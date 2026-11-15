package com.exemplo.conteudos.cache;

import com.exemplo.conteudos.model.Conteudo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * Cache "light": ConcurrentHashMap + TTL + remoção LRU + estatísticas.
 */
@Component
public class ConteudoCache {

    private static final Logger log = LoggerFactory.getLogger(ConteudoCache.class);

    private final Map<Long, EntradaCache> mapa = new ConcurrentHashMap<>();
    private final int maxItens;
    private final Duration ttl;

    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    private final AtomicLong evictions = new AtomicLong();

    public ConteudoCache(@Value("${app.cache.max-itens:5}") int maxItens,
                         @Value("${app.cache.ttl-segundos:60}") long ttlSegundos) {
        this.maxItens = maxItens;
        this.ttl = Duration.ofSeconds(ttlSegundos);
    }

    /**
     * Busca no cache; se não houver (ou estiver expirado), carrega da fonte e armazena.
     * Registra HIT/MISS nas estatísticas e no log, com o tempo da operação.
     */
    public ResultadoBusca obterOuCarregar(Long id, Function<Long, Conteudo> fonte) {
        long inicio = System.nanoTime();

        Conteudo emCache = buscarValido(id);
        if (emCache != null) {
            hits.incrementAndGet();
            log.info("Cache HIT  - id={} - {} ms", id, ms(inicio));
            return new ResultadoBusca(emCache, true);
        }

        misses.incrementAndGet();
        Conteudo carregado = fonte.apply(id); // lenta; fica fora de qualquer lock
        armazenar(carregado);
        log.info("Cache MISS - id={} - {} ms", id, ms(inicio));
        return new ResultadoBusca(carregado, false);
    }

    /** Remove a entrada de um conteúdo (usado em PUT e DELETE). */
    public void invalidar(Long id) {
        if (mapa.remove(id) != null) {
            log.info("Cache INVALIDATE - id={}", id);
        }
    }

    /** Limpa todas as entradas. As estatísticas acumuladas são mantidas. */
    public void limpar() {
        int removidos = mapa.size();
        mapa.clear();
        log.info("Cache CLEAR - {} itens removidos", removidos);
    }

    public EstatisticasCache estatisticas() {
        long h = hits.get();
        long m = misses.get();
        double ratio = (h + m) == 0 ? 0.0 : (double) h / (h + m);
        return new EstatisticasCache(mapa.size(), maxItens, h, m, evictions.get(), ratio);
    }

    /** Retorna o conteúdo se existir e não tiver expirado; atualiza o último acesso. */
    private Conteudo buscarValido(Long id) {
        EntradaCache entrada = mapa.get(id);
        if (entrada == null) {
            return null;
        }
        Instant agora = Instant.now();
        if (entrada.expirou(agora, ttl)) {
            mapa.remove(id, entrada);
            log.info("Cache EXPIRED - id={} (TTL de {} s)", id, ttl.toSeconds());
            return null;
        }
        entrada.registrarAcesso(agora);
        return entrada.getConteudo();
    }

    /** Escritas são serializadas para que "checar tamanho + remover LRU + inserir" seja atômico. */
    private synchronized void armazenar(Conteudo conteudo) {
        Long id = conteudo.getId();
        if (!mapa.containsKey(id) && mapa.size() >= maxItens) {
            removerMenosRecentementeUsado();
        }
        mapa.put(id, new EntradaCache(conteudo, Instant.now()));
    }

    private void removerMenosRecentementeUsado() {
        mapa.entrySet().stream()
                .min(Comparator.comparing(e -> e.getValue().getUltimoAcesso()))
                .ifPresent(e -> {
                    mapa.remove(e.getKey());
                    evictions.incrementAndGet();
                    log.info("Cache EVICT - id={} (limite de {} itens atingido)", e.getKey(), maxItens);
                });
    }

    private static long ms(long inicioNanos) {
        return (System.nanoTime() - inicioNanos) / 1_000_000;
    }
}
