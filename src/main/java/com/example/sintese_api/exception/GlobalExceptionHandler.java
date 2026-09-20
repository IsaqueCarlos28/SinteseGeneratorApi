package com.example.sintese_api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(
            MethodArgumentNotValidException exception
    ) {

        String mensagem = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ProblemDetail problem = ProblemDetail.forStatus(
                HttpStatus.BAD_REQUEST
        );

        problem.setTitle("Requisição inválida");
        problem.setDetail(mensagem);

        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleInvalidJson(
            HttpMessageNotReadableException exception
    ) {

        ProblemDetail problem = ProblemDetail.forStatus(
                HttpStatus.BAD_REQUEST
        );

        problem.setTitle("JSON inválido");
        problem.setDetail(
                "O corpo da requisição não possui um formato JSON válido."
        );

        return problem;
    }

    @ExceptionHandler(SinteseEntradaMuitoGrandeException.class)
    public ProblemDetail handleSinteseEntradaMuitoGrande(
            SinteseEntradaMuitoGrandeException exception
    ) {

        ProblemDetail problem = ProblemDetail.forStatus(
                HttpStatus.BAD_REQUEST
        );

        problem.setTitle("Entrada muito grande");
        problem.setDetail(exception.getMessage());

        return problem;
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception
    ) {

        ProblemDetail problem = ProblemDetail.forStatus(
                HttpStatus.METHOD_NOT_ALLOWED
        );

        problem.setTitle("Método não permitido");
        problem.setDetail(
                "O método HTTP utilizado não é permitido para este recurso."
        );

        return problem;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleResourceNotFound(
            NoResourceFoundException exception
    ) {

        ProblemDetail problem = ProblemDetail.forStatus(
                HttpStatus.NOT_FOUND
        );

        problem.setTitle("Recurso não encontrado");
        problem.setDetail(
                "O recurso solicitado não foi encontrado."
        );

        return problem;
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception
    ) {

        ProblemDetail problem = ProblemDetail.forStatus(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE
        );

        problem.setTitle("Tipo de conteúdo não suportado");
        problem.setDetail(
                "O tipo de conteúdo enviado não é suportado pela API."
        );

        return problem;
    }

    @ExceptionHandler(GeminiRateLimitException.class)
    public ProblemDetail handleGeminiRateLimit(
            GeminiRateLimitException exception
    ) {

        ProblemDetail problem = ProblemDetail.forStatus(
                HttpStatus.TOO_MANY_REQUESTS
        );

        problem.setTitle("Limite de requisições atingido");
        problem.setDetail(
                "A quantidade de requisições à Gemini excedeu o limite permitido."
        );

        return problem;
    }

    @ExceptionHandler(GeminiTimeoutException.class)
    public ProblemDetail handleGeminiTimeout(
            GeminiTimeoutException exception
    ) {

        ProblemDetail problem = ProblemDetail.forStatus(
                HttpStatus.GATEWAY_TIMEOUT
        );

        problem.setTitle("Timeout na Gemini");
        problem.setDetail(
                "A Gemini demorou demais para responder."
        );

        return problem;
    }

    @ExceptionHandler(GeminiException.class)
    public ProblemDetail handleGeminiException(
            GeminiException exception
    ) {

        ProblemDetail problem = ProblemDetail.forStatus(
                HttpStatus.BAD_GATEWAY
        );

        problem.setTitle("Erro na Gemini");
        problem.setDetail(
                "Não foi possível processar a síntese através da Gemini."
        );

        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(
            Exception exception
    ) {

        ProblemDetail problem = ProblemDetail.forStatus(
                HttpStatus.INTERNAL_SERVER_ERROR
        );

        problem.setTitle("Erro interno do servidor");
        problem.setDetail(
                "Ocorreu um erro inesperado ao processar a requisição."
        );

        return problem;
    }
}