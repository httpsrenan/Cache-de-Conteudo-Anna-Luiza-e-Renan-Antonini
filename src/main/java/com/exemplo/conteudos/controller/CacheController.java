package com.exemplo.conteudos.controller;

import com.exemplo.conteudos.cache.EstatisticasCache;
import com.exemplo.conteudos.service.ConteudoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cache")
public class CacheController {

    private final ConteudoService service;

    public CacheController(ConteudoService service) {
        this.service = service;
    }

    @GetMapping("/estatisticas")
    public EstatisticasCache estatisticas() {
        return service.estatisticasCache();
    }

    @DeleteMapping
    public ResponseEntity<Void> limpar() {
        service.limparCache();
        return ResponseEntity.noContent().build();
    }
}
