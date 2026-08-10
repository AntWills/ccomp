package com.ccomp.br.domain.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados de retorno do upload do arquivo")
public record UploadFileResponse(
        @Schema(description = "URL completa de acesso ao arquivo", example = "http://localhost:9000/images/71c629f9-0fe1-4d23-bc0a-a7158769fe8b_imagem.jpeg")
        String url,

        @Schema(description = "Nome único gerado com UUID salvo no storage", example = "71c629f9-0fe1-4d23-bc0a-a7158769fe8b_imagem.jpeg")
        String fileName
) {
}
