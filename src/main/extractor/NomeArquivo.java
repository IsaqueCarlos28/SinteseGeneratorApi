package com.example.sintese_api.extractor;

/**
 * O nome enviado pelo cliente não é confiável. Ele só é usado em mensagens
 * de erro, nunca para acessar o sistema de arquivos.
 */
public final class NomeArquivo {

    private static final int TAMANHO_MAXIMO = 100;

    private NomeArquivo() {
    }

    public static String seguro(String nome) {

        if (nome == null || nome.isBlank()) {
            return "sem-nome";
        }

        String semCaminho = nome.substring(
                Math.max(nome.lastIndexOf('/'), nome.lastIndexOf('\\')) + 1
        );

        // remove caracteres de controle e limita o tamanho
        String limpo = semCaminho.replaceAll("\\p{Cntrl}", "").trim();

        if (limpo.isEmpty()) {
            return "sem-nome";
        }

        return limpo.length() > TAMANHO_MAXIMO
                ? limpo.substring(0, TAMANHO_MAXIMO)
                : limpo;
    }

    public static String extensao(String nome) {

        String seguro = seguro(nome);
        int ponto = seguro.lastIndexOf('.');

        if (ponto < 0 || ponto == seguro.length() - 1) {
            return "";
        }

        return seguro.substring(ponto + 1);
    }
}
