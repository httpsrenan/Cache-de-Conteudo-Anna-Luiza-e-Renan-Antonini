package com.exemplo.conteudos.service;

import com.exemplo.conteudos.cache.ConteudoCache;
import com.exemplo.conteudos.cache.EstatisticasCache;
import com.exemplo.conteudos.cache.ResultadoBusca;
import com.exemplo.conteudos.exception.ConteudoNaoEncontradoException;
import com.exemplo.conteudos.model.Conteudo;
import com.exemplo.conteudos.repository.ConteudoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ConteudoService {

    private final ConteudoRepository repository;
    private final ConteudoCache cache;
    private final long atrasoFonteMs;

    public ConteudoService(ConteudoRepository repository,
                           ConteudoCache cache,
                           @Value("${app.fonte.atraso-ms:2000}") long atrasoFonteMs) {
        this.repository = repository;
        this.cache = cache;
        this.atrasoFonteMs = atrasoFonteMs;
    }

    /** Passa pelo cache; só vai à fonte lenta em caso de MISS. */
    public ResultadoBusca buscarPorId(Long id) {
        return cache.obterOuCarregar(id, this::buscarNaFonte);
    }

    public Conteudo criar(Conteudo conteudo) {
        conteudo.setId(null);
        conteudo.setAtualizadoEm(LocalDateTime.now());
        return repository.save(conteudo);
    }

    public Conteudo atualizar(Long id, Conteudo dados) {
        Conteudo existente = repository.findById(id)
                .orElseThrow(() -> new ConteudoNaoEncontradoException(id));
        existente.setTitulo(dados.getTitulo());
        existente.setCorpo(dados.getCorpo());
        existente.setAutor(dados.getAutor());
        existente.setAtualizadoEm(LocalDateTime.now());
        Conteudo salvo = repository.save(existente);
        cache.invalidar(id);
        return salvo;
    }

    public void excluir(Long id) {
        if (!repository.existsById(id)) {
            throw new ConteudoNaoEncontradoException(id);
        }
        repository.deleteById(id);
        cache.invalidar(id);
    }

    public EstatisticasCache estatisticasCache() {
        return cache.estatisticas();
    }

    public void limparCache() {
        cache.limpar();
    }

    private Conteudo buscarNaFonte(Long id) {
        try {
            Thread.sleep(atrasoFonteMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Busca interrompida", e);
        }
        return repository.findById(id)
                .orElseThrow(() -> new ConteudoNaoEncontradoException(id));
    }
}
