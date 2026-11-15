package com.exemplo.conteudos.cache;

public record EstatisticasCache(
        int tamanhoAtual,
        int limite,
        long hits,
        long misses,
        long evictions,
        double hitRatio) {
}
