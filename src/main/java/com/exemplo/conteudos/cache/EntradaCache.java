package com.exemplo.conteudos.cache;

import com.exemplo.conteudos.model.Conteudo;
import java.time.Duration;
import java.time.Instant;

/** Uma entrada do cache: o conteúdo, quando entrou e quando foi acessado pela última vez. */
public class EntradaCache {

    private final Conteudo conteudo;
    private final Instant armazenadoEm;
    private volatile Instant ultimoAcesso;

    public EntradaCache(Conteudo conteudo, Instant agora) {
        this.conteudo = conteudo;
        this.armazenadoEm = agora;
        this.ultimoAcesso = agora;
    }

    public boolean expirou(Instant agora, Duration ttl) {
        return Duration.between(armazenadoEm, agora).compareTo(ttl) > 0;
    }

    public void registrarAcesso(Instant agora) {
        this.ultimoAcesso = agora;
    }

    public Conteudo getConteudo() { return conteudo; }
    public Instant getArmazenadoEm() { return armazenadoEm; }
    public Instant getUltimoAcesso() { return ultimoAcesso; }
}
