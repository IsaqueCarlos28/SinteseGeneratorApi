package com.example.sintese_api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final int maxRequests;
    private final long windowSeconds;
    private final Map<String, Janela> janelas = new ConcurrentHashMap<>();

    public RateLimitFilter(
            ObjectMapper objectMapper,
            @Value("${app.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.rate-limit.max-requests:10}") int maxRequests,
            @Value("${app.rate-limit.window-seconds:60}") long windowSeconds
    ) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
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

        String chave = chaveCliente(request);
        long agora = Instant.now().getEpochSecond();
        Janela janela = janelas.computeIfAbsent(chave, k -> new Janela(agora));

        ResultadoRateLimit resultado = janela.tentarConsumir(agora, windowSeconds, maxRequests);

        if (!resultado.permitido()) {
            escreverResposta429(request, response, resultado.retryAfterSegundos());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String chaveCliente(HttpServletRequest request) {
        String encaminhado = request.getHeader("X-Forwarded-For");
        if (encaminhado != null && !encaminhado.isBlank()) {
            int virgula = encaminhado.indexOf(',');
            String ip = virgula >= 0 ? encaminhado.substring(0, virgula) : encaminhado;
            ip = ip.trim();
            if (!ip.isEmpty()) {
                return ip;
            }
        }
        String remoto = request.getRemoteAddr();
        return remoto == null || remoto.isBlank() ? "desconhecido" : remoto;
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

    private static final class Janela {
        private final AtomicLong inicioJanela;
        private final AtomicInteger contador;

        private Janela(long agora) {
            this.inicioJanela = new AtomicLong(agora);
            this.contador = new AtomicInteger(0);
        }

        private synchronized ResultadoRateLimit tentarConsumir(
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
    }
}
