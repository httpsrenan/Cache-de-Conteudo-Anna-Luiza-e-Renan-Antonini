package com.exemplo.conteudos.controller;

import com.exemplo.conteudos.cache.ResultadoBusca;
import com.exemplo.conteudos.dto.ConteudoRequest;
import com.exemplo.conteudos.dto.ConteudoResponse;
import com.exemplo.conteudos.mapper.ConteudoMapper;
import com.exemplo.conteudos.model.Conteudo;
import com.exemplo.conteudos.service.ConteudoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/conteudos")
public class ConteudoController {

    private final ConteudoService service;
    private final ConteudoMapper mapper;

    public ConteudoController(ConteudoService service, ConteudoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConteudoResponse> buscar(@PathVariable Long id) {
        ResultadoBusca resultado = service.buscarPorId(id);
        return ResponseEntity.ok()
                .header("X-Cache", resultado.hit() ? "HIT" : "MISS")
                .body(mapper.toResponse(resultado.conteudo()));
    }

    @PostMapping
    public ResponseEntity<ConteudoResponse> criar(@Valid @RequestBody ConteudoRequest request) {
        Conteudo salvo = service.criar(mapper.toEntity(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(salvo.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(salvo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConteudoResponse> atualizar(@PathVariable Long id,
                                                      @Valid @RequestBody ConteudoRequest request) {
        Conteudo atualizado = service.atualizar(id, mapper.toEntity(request));
        return ResponseEntity.ok(mapper.toResponse(atualizado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
