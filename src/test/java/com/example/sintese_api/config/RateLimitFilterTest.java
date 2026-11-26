package com.example.sintese_api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void devePermitirRequisicoesDentroDoLimite() throws ServletException, IOException {
        AtomicLong agora = new AtomicLong(1_000);
        RateLimitFilter filter = new RateLimitFilter(objectMapper, true, 2, 60, agora::get);

        MockHttpServletResponse r1 = executar(filter, requisicao("10.0.0.1"));
        MockHttpServletResponse r2 = executar(filter, requisicao("10.0.0.1"));

        assertEquals(HttpStatus.OK.value(), r1.getStatus());
        assertEquals(HttpStatus.OK.value(), r2.getStatus());
    }

    @Test
    void deveRetornar429QuandoLimiteForExcedido() throws Exception {
        AtomicLong agora = new AtomicLong(1_000);
        RateLimitFilter filter = new RateLimitFilter(objectMapper, true, 2, 60, agora::get);

        executar(filter, requisicao("10.0.0.1"));
        executar(filter, requisicao("10.0.0.1"));
        MockHttpServletResponse bloqueada = executar(filter, requisicao("10.0.0.1"));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), bloqueada.getStatus());
        assertEquals("60", bloqueada.getHeader("Retry-After"));
        assertTrue(bloqueada.getContentAsString().contains("\"status\":429"));
        assertTrue(bloqueada.getContentAsString().contains("Limite de requisições atingido"));
    }

    @Test
    void deveRetornarRetryAfterCorreto() throws Exception {
        AtomicLong agora = new AtomicLong(1_000);
        RateLimitFilter filter = new RateLimitFilter(objectMapper, true, 1, 60, agora::get);

        executar(filter, requisicao("10.0.0.1"));
        agora.set(1_045);
        MockHttpServletResponse bloqueada = executar(filter, requisicao("10.0.0.1"));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), bloqueada.getStatus());
        assertEquals("15", bloqueada.getHeader("Retry-After"));
    }

    @Test
    void devePermitirRequisicoesAposExpiracaoDaJanela() throws ServletException, IOException {
        AtomicLong agora = new AtomicLong(1_000);
        RateLimitFilter filter = new RateLimitFilter(objectMapper, true, 1, 60, agora::get);

        executar(filter, requisicao("10.0.0.1"));
        MockHttpServletResponse bloqueada = executar(filter, requisicao("10.0.0.1"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), bloqueada.getStatus());

        agora.set(1_060);
        MockHttpServletResponse aposJanela = executar(filter, requisicao("10.0.0.1"));
        assertEquals(HttpStatus.OK.value(), aposJanela.getStatus());
    }

    @Test
    void deveManterContadoresIndependentesPorCliente() throws ServletException, IOException {
        AtomicLong agora = new AtomicLong(1_000);
        RateLimitFilter filter = new RateLimitFilter(objectMapper, true, 1, 60, agora::get);

        MockHttpServletResponse clienteA1 = executar(filter, requisicao("10.0.0.1"));
        MockHttpServletResponse clienteB1 = executar(filter, requisicao("10.0.0.2"));
        MockHttpServletResponse clienteA2 = executar(filter, requisicao("10.0.0.1"));

        assertEquals(HttpStatus.OK.value(), clienteA1.getStatus());
        assertEquals(HttpStatus.OK.value(), clienteB1.getStatus());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), clienteA2.getStatus());
    }

    @Test
    void naoDeveConfiarEmXForwardedForSemProxyConfiavel() throws ServletException, IOException {
        AtomicLong agora = new AtomicLong(1_000);
        RateLimitFilter filter = new RateLimitFilter(objectMapper, true, 1, 60, "", agora::get);

        MockHttpServletRequest primeira = requisicao("10.0.0.1");
        primeira.addHeader("X-Forwarded-For", "1.2.3.4");
        MockHttpServletRequest segunda = requisicao("10.0.0.1");
        segunda.addHeader("X-Forwarded-For", "5.6.7.8");

        MockHttpServletResponse r1 = executar(filter, primeira);
        MockHttpServletResponse r2 = executar(filter, segunda);

        assertEquals(HttpStatus.OK.value(), r1.getStatus());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), r2.getStatus());
    }

    @Test
    void deveUsarXForwardedForApenasComProxyConfiavel() throws ServletException, IOException {
        AtomicLong agora = new AtomicLong(1_000);
        RateLimitFilter filter = new RateLimitFilter(objectMapper, true, 1, 60, "10.0.0.254", agora::get);

        MockHttpServletRequest primeira = requisicao("10.0.0.254");
        primeira.addHeader("X-Forwarded-For", "203.0.113.10");
        MockHttpServletRequest segunda = requisicao("10.0.0.254");
        segunda.addHeader("X-Forwarded-For", "203.0.113.11");

        MockHttpServletResponse r1 = executar(filter, primeira);
        MockHttpServletResponse r2 = executar(filter, segunda);

        assertEquals(HttpStatus.OK.value(), r1.getStatus());
        assertEquals(HttpStatus.OK.value(), r2.getStatus());
        assertTrue(filter.janelas().containsKey("203.0.113.10"));
        assertTrue(filter.janelas().containsKey("203.0.113.11"));
    }

    @Test
    void naoDeveConfiarEmXForwardedForQuandoRemotoNaoEProxyConfiavel() throws ServletException, IOException {
        AtomicLong agora = new AtomicLong(1_000);
        RateLimitFilter filter = new RateLimitFilter(objectMapper, true, 1, 60, "10.0.0.254", agora::get);

        MockHttpServletRequest primeira = requisicao("198.51.100.20");
        primeira.addHeader("X-Forwarded-For", "203.0.113.10");
        MockHttpServletRequest segunda = requisicao("198.51.100.20");
        segunda.addHeader("X-Forwarded-For", "203.0.113.11");

        MockHttpServletResponse r1 = executar(filter, primeira);
        MockHttpServletResponse r2 = executar(filter, segunda);

        assertEquals(HttpStatus.OK.value(), r1.getStatus());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), r2.getStatus());
        assertTrue(filter.janelas().containsKey("198.51.100.20"));
        assertFalse(filter.janelas().containsKey("203.0.113.10"));
    }

    @Test
    void deveRemoverJanelasExpiradasDoMapa() throws ServletException, IOException {
        AtomicLong agora = new AtomicLong(1_000);
        RateLimitFilter filter = new RateLimitFilter(objectMapper, true, 10, 60, agora::get);

        for (int i = 0; i < 50; i++) {
            executar(filter, requisicao("192.168.0." + i));
        }
        assertEquals(50, filter.janelas().size());

        agora.set(1_061); // expira todas
        // força limpeza periódica
        for (int i = 0; i < 100; i++) {
            executar(filter, requisicao("172.16.0." + (i % 10)));
        }

        assertFalse(filter.janelas().keySet().stream().anyMatch(k -> k.startsWith("192.168.0.")));
    }

    @Test
    void naoDeveRemoverJanelaAtivaDuranteLimpeza() throws ServletException, IOException {
        AtomicLong agora = new AtomicLong(1_000);
        RateLimitFilter filter = new RateLimitFilter(objectMapper, true, 10, 60, agora::get);

        executar(filter, requisicao("10.0.0.1"));
        agora.set(1_030); // ainda ativa
        for (int i = 0; i < 120; i++) {
            executar(filter, requisicao("172.16.0." + (i % 5)));
        }
        assertTrue(filter.janelas().containsKey("10.0.0.1"));
    }

    @Test
    void janelaRenovadaAposExpiracaoNaoDeveSerRemovidaComoExpirada() throws ServletException, IOException {
        AtomicLong agora = new AtomicLong(1_000);
        RateLimitFilter filter = new RateLimitFilter(objectMapper, true, 10, 60, agora::get);

        executar(filter, requisicao("10.0.0.1"));
        assertTrue(filter.janelas().containsKey("10.0.0.1"));

        agora.set(1_061); // expirada
        // renova a janela com uma requisição antes da limpeza
        executar(filter, requisicao("10.0.0.1"));
        assertEquals(1_061, filter.janelas().get("10.0.0.1").inicio());

        // dispara limpeza periódica
        for (int i = 0; i < 120; i++) {
            executar(filter, requisicao("172.16.0." + (i % 5)));
        }

        assertTrue(filter.janelas().containsKey("10.0.0.1"));
        assertEquals(1_061, filter.janelas().get("10.0.0.1").inicio());
    }

    @Test
    void naoDevePerderContadorAtivo() throws ServletException, IOException {
        AtomicLong agora = new AtomicLong(1_000);
        RateLimitFilter filter = new RateLimitFilter(objectMapper, true, 2, 60, agora::get);

        executar(filter, requisicao("10.0.0.1")); // 1
        agora.set(1_030);
        for (int i = 0; i < 120; i++) {
            executar(filter, requisicao("172.16.0." + (i % 20)));
        }
        // cliente ativo já usou 1 de 2 dentro da mesma janela
        MockHttpServletResponse segunda = executar(filter, requisicao("10.0.0.1"));
        MockHttpServletResponse terceira = executar(filter, requisicao("10.0.0.1"));
        assertEquals(HttpStatus.OK.value(), segunda.getStatus());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), terceira.getStatus());
    }

    @Test
    void naoDeveReiniciarContadorQuandoConsumoEConcorrenteComLimpeza() throws Exception {
        AtomicLong agora = new AtomicLong(1_000);
        int limite = 2;
        AtomicBoolean pausar = new AtomicBoolean(true);
        CountDownLatch cleanupPodeTentar = new CountDownLatch(1);
        CountDownLatch continuarConsumo = new CountDownLatch(1);
        CountDownLatch cleanupConcluida = new CountDownLatch(1);

        RateLimitFilter filter = new RateLimitFilter(objectMapper, true, limite, 60, agora::get) {
            @Override
            protected void antesDeConsumir(String chave, long agoraAtual) {
                if ("10.0.0.1".equals(chave) && agoraAtual == 1_061L && pausar.compareAndSet(true, false)) {
                    cleanupPodeTentar.countDown();
                    try {
                        assertTrue(continuarConsumo.await(5, TimeUnit.SECONDS), "consumo não liberado");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
            }
        };

        // consome 1 da janela inicial
        assertEquals(HttpStatus.OK.value(), executar(filter, requisicao("10.0.0.1")).getStatus());
        agora.set(1_061); // janela expirada

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Future<MockHttpServletResponse> reqA = pool.submit(() -> executar(filter, requisicao("10.0.0.1")));

            assertTrue(cleanupPodeTentar.await(5, TimeUnit.SECONDS), "hook não alcançado");

            pool.submit(() -> {
                filter.limparJanelasExpiradas(1_061);
                cleanupConcluida.countDown();
            });

            // limpeza não deve concluir enquanto compute da mesma chave está em andamento
            assertFalse(cleanupConcluida.await(300, TimeUnit.MILLISECONDS), "limpeza concluiu indevidamente durante consumo");

            continuarConsumo.countDown();

            MockHttpServletResponse respostaA = reqA.get(5, TimeUnit.SECONDS);
            assertEquals(HttpStatus.OK.value(), respostaA.getStatus());
            assertTrue(cleanupConcluida.await(5, TimeUnit.SECONDS), "limpeza não concluiu");

            MockHttpServletResponse respostaB = executar(filter, requisicao("10.0.0.1"));
            MockHttpServletResponse respostaC = executar(filter, requisicao("10.0.0.1"));

            assertEquals(HttpStatus.OK.value(), respostaB.getStatus());
            assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), respostaC.getStatus());
            assertTrue(filter.janelas().containsKey("10.0.0.1"));
            assertEquals(1_061, filter.janelas().get("10.0.0.1").inicio());
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void deveManterCorretudeSobAcessoConcorrente() throws Exception {
        AtomicLong agora = new AtomicLong(1_000);
        int limite = 20;
        RateLimitFilter filter = new RateLimitFilter(objectMapper, true, limite, 60, agora::get);

        int total = 100;
        ExecutorService pool = Executors.newFixedThreadPool(total);
        CountDownLatch pronto = new CountDownLatch(total);
        CountDownLatch inicio = new CountDownLatch(1);
        AtomicInteger permitidas = new AtomicInteger();
        AtomicInteger bloqueadas = new AtomicInteger();

        for (int i = 0; i < total; i++) {
            pool.submit(() -> {
                pronto.countDown();
                try {
                    inicio.await();
                    MockHttpServletResponse resp = executar(filter, requisicao("10.0.0.1"));
                    if (resp.getStatus() == HttpStatus.OK.value()) {
                        permitidas.incrementAndGet();
                    } else if (resp.getStatus() == HttpStatus.TOO_MANY_REQUESTS.value()) {
                        bloqueadas.incrementAndGet();
                    }
                } catch (Exception ignored) {
                    // falha contabilizada pela ausência no total
                }
            });
        }

        assertTrue(pronto.await(10, TimeUnit.SECONDS), "tarefas não ficaram prontas");
        inicio.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(15, TimeUnit.SECONDS), "pool não terminou");

        assertEquals(limite, permitidas.get());
        assertEquals(total - limite, bloqueadas.get());
    }

    private MockHttpServletRequest requisicao(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/sinteses");
        request.setRemoteAddr(ip);
        request.setContentType("application/json");
        return request;
    }

    private MockHttpServletResponse executar(RateLimitFilter filter, MockHttpServletRequest request)
            throws ServletException, IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> ((MockHttpServletResponse) res).setStatus(HttpStatus.OK.value());
        filter.doFilter(request, response, chain);
        return response;
    }
}
