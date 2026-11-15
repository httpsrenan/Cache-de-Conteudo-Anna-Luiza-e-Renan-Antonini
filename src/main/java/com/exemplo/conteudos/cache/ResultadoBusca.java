package com.exemplo.conteudos.cache;

import com.exemplo.conteudos.model.Conteudo;

public record ResultadoBusca(Conteudo conteudo, boolean hit) {
}
