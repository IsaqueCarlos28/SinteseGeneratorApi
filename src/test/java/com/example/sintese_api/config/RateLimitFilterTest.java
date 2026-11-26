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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void naoDeveConfiarEmXForwardedForEnviadoPeloCliente() throws ServletException, IOException {
        AtomicLong agora = new AtomicLong(1_000);
        RateLimitFilter filter = new RateLimitFilter(objectMapper, true, 1, 60, agora::get);

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
    void deveRemoverJanelasExpiradasSemCorromperContadoresAtivos() throws ServletException, IOException {
        AtomicLong agora = new AtomicLong(1_000);
        RateLimitFilter filter = new RateLimitFilter(objectMapper, true, 2, 60, agora::get);

        // cria entradas que vão expirar
        for (int i = 0; i < 50; i++) {
            executar(filter, requisicao("192.168.0." + i));
        }

        // cliente ativo
        executar(filter, requisicao("10.0.0.1"));
        agora.set(1_061); // expira janelas antigas

        // força limpeza periódica (a cada 100 requisições)
        for (int i = 0; i < 60; i++) {
            executar(filter, requisicao("172.16.0." + i));
        }

        // cliente ativo ainda deve estar dentro da sua nova janela/estado coerente
        MockHttpServletResponse ativa = executar(filter, requisicao("10.0.0.1"));
        assertEquals(HttpStatus.OK.value(), ativa.getStatus());
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
