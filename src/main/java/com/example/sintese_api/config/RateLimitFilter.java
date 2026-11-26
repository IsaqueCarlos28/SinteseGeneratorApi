package com.example.sintese_api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int LIMPEZA_A_CADA_REQUISICOES = 100;

    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final int maxRequests;
    private final long windowSeconds;
    private final Set<String> trustedProxies;
    private final LongSupplier relogio;
    private final Map<String, Janela> janelas = new ConcurrentHashMap<>();
    private final AtomicInteger contadorRequisicoes = new AtomicInteger(0);

    @Autowired
    public RateLimitFilter(
            ObjectMapper objectMapper,
            @Value("${app.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.rate-limit.max-requests:10}") int maxRequests,
            @Value("${app.rate-limit.window-seconds:60}") long windowSeconds,
            @Value("${app.rate-limit.trusted-proxies:}") String trustedProxies
    ) {
        this(objectMapper, enabled, maxRequests, windowSeconds, trustedProxies, () -> Instant.now().getEpochSecond());
    }

    RateLimitFilter(
            ObjectMapper objectMapper,
            boolean enabled,
            int maxRequests,
            long windowSeconds,
            LongSupplier relogio
    ) {
        this(objectMapper, enabled, maxRequests, windowSeconds, "", relogio);
    }

    RateLimitFilter(
            ObjectMapper objectMapper,
            boolean enabled,
            int maxRequests,
            long windowSeconds,
            String trustedProxies,
            LongSupplier relogio
    ) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
        this.trustedProxies = parseTrustedProxies(trustedProxies);
        this.relogio = relogio;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        String uri = request.getRequestURI();
        return uri == null || !uri.startsWith("/api/sinteses");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        long agora = relogio.getAsLong();
        limparJanelasExpiradasSeNecessario(agora);

        String chave = chaveCliente(request);
        Janela janela = janelas.computeIfAbsent(chave, k -> new Janela(agora));

        ResultadoRateLimit resultado = janela.tentarConsumir(agora, windowSeconds, maxRequests);

        if (!resultado.permitido()) {
            escreverResposta429(request, response, resultado.retryAfterSegundos());
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Identifica o cliente pelo endereço remoto da conexão TCP.
     * <p>
     * Não confia em {@code X-Forwarded-For} diretamente. O cabeçalho só é
     * considerado quando o remetente TCP ({@code remoteAddr}) está na lista
     * explícita de proxies confiáveis ({@code app.rate-limit.trusted-proxies}).
     * Nesse caso, o IP do cliente é resolvido caminhando do fim para o início
     * da cadeia XFF e selecionando o primeiro IP que não seja proxy confiável.
     * <p>
     * Se a cadeia de proxies não puder ser confirmada, a lista deve ficar vazia
     * e o rate limit passa a usar apenas {@code remoteAddr}.
     */
    private String chaveCliente(HttpServletRequest request) {
        String remoto = normalizarIp(request.getRemoteAddr());
        if (remoto.isEmpty()) {
            return "desconhecido";
        }

        if (!trustedProxies.isEmpty() && trustedProxies.contains(remoto)) {
            String encaminhado = request.getHeader("X-Forwarded-For");
            String ipCliente = resolverIpCliente(encaminhado, remoto);
            if (!ipCliente.isEmpty()) {
                return ipCliente;
            }
        }

        return remoto;
    }

    private String resolverIpCliente(String forwardedFor, String remoto) {
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return remoto;
        }
        String[] partes = forwardedFor.split(",");
        for (int i = partes.length - 1; i >= 0; i--) {
            String candidato = normalizarIp(partes[i]);
            if (candidato.isEmpty()) {
                continue;
            }
            if (!trustedProxies.contains(candidato)) {
                return candidato;
            }
        }
        return remoto;
    }

    private static Set<String> parseTrustedProxies(String trustedProxies) {
        if (trustedProxies == null || trustedProxies.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(trustedProxies.split(","))
                .map(RateLimitFilter::normalizarIp)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String normalizarIp(String ip) {
        if (ip == null) {
            return "";
        }
        String normalizado = ip.trim();
        if (normalizado.startsWith("[") && normalizado.endsWith("]")) {
            normalizado = normalizado.substring(1, normalizado.length() - 1);
        }
        return normalizado;
    }

    private void limparJanelasExpiradasSeNecessario(long agora) {
        int contagem = contadorRequisicoes.incrementAndGet();
        if (contagem % LIMPEZA_A_CADA_REQUISICOES != 0) {
            return;
        }
        janelas.forEach((chave, janela) -> janelas.computeIfPresent(
                chave,
                (k, atual) -> atual.expirada(agora, windowSeconds) ? null : atual
        ));
    }

    private void escreverResposta429(
            HttpServletRequest request,
            HttpServletResponse response,
            long retryAfter
    ) throws IOException {

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.TOO_MANY_REQUESTS);
        problem.setTitle("Limite de requisições atingido");
        problem.setDetail(
                "Muitas requisições foram feitas a partir deste cliente. "
                        + "Tente novamente em instantes."
        );
        problem.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        objectMapper.writeValue(response.getWriter(), problem);
    }

    private record ResultadoRateLimit(boolean permitido, long retryAfterSegundos) {
    }

    static final class Janela {
        private final AtomicLong inicioJanela;
        private final AtomicInteger contador;

        Janela(long agora) {
            this.inicioJanela = new AtomicLong(agora);
            this.contador = new AtomicInteger(0);
        }

        synchronized ResultadoRateLimit tentarConsumir(
                long agora,
                long windowSeconds,
                int maxRequests
        ) {
            long inicio = inicioJanela.get();
            if (agora - inicio >= windowSeconds) {
                inicioJanela.set(agora);
                contador.set(0);
                inicio = agora;
            }

            int atual = contador.incrementAndGet();
            if (atual > maxRequests) {
                long retryAfter = Math.max(1, windowSeconds - (agora - inicio));
                return new ResultadoRateLimit(false, retryAfter);
            }

            return new ResultadoRateLimit(true, 0);
        }

        synchronized boolean expirada(long agora, long windowSeconds) {
            return agora - inicioJanela.get() >= windowSeconds;
        }

        long inicio() {
            return inicioJanela.get();
        }
    }

    // visibilidade de pacote para testes
    Map<String, Janela> janelas() {
        return janelas;
    }
}
