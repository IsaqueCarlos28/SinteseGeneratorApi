package com.example.sintese_api.extractor;

import com.example.sintese_api.exception.DocumentoNaoProcessavelException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

@Component
public class TxtExtractor implements DocumentoExtractor {

    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    @Override
    public boolean suporta(String contentType) {
        return TipoDocumento.TXT.contentType().equalsIgnoreCase(contentType);
    }

    @Override
    public String extrair(MultipartFile arquivo) {

        String nome = NomeArquivo.seguro(arquivo.getOriginalFilename());

        byte[] bytes;

        try {
            bytes = arquivo.getBytes();
        } catch (IOException exception) {
            throw new DocumentoNaoProcessavelException(
                    "Não foi possível ler o arquivo '" + nome + "'.",
                    exception
            );
        }

        // UTF-16 com BOM (opção "Unicode" do Bloco de Notas)
        if (temBom(bytes, (byte) 0xFF, (byte) 0xFE)
                || temBom(bytes, (byte) 0xFE, (byte) 0xFF)) {
            return new String(bytes, StandardCharsets.UTF_16);
        }

        // Byte nulo em texto sem BOM UTF-16 indica arquivo binário
        // com extensão .txt.
        for (byte b : bytes) {
            if (b == 0) {
                throw new DocumentoNaoProcessavelException(
                        "O arquivo '" + nome + "' não parece ser um texto válido."
                );
            }
        }

        try {
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            // Comum em .txt antigos gerados no Windows.
            return new String(bytes, WINDOWS_1252);
        }
    }

    private boolean temBom(byte[] bytes, byte primeiro, byte segundo) {
        return bytes.length >= 2
                && bytes[0] == primeiro
                && bytes[1] == segundo;
    }
}
