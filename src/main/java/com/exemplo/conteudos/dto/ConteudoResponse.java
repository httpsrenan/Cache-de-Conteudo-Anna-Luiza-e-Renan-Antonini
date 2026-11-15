package com.exemplo.conteudos.dto;

import java.time.LocalDateTime;

public record ConteudoResponse(
        Long id,
        String titulo,
        String corpo,
        String autor,
        LocalDateTime atualizadoEm) {
}
