package com.exemplo.conteudos.mapper;

import com.exemplo.conteudos.dto.ConteudoRequest;
import com.exemplo.conteudos.dto.ConteudoResponse;
import com.exemplo.conteudos.model.Conteudo;
import org.springframework.stereotype.Component;

@Component
public class ConteudoMapper {

    public Conteudo toEntity(ConteudoRequest request) {
        Conteudo c = new Conteudo();
        c.setTitulo(request.titulo());
        c.setCorpo(request.corpo());
        c.setAutor(request.autor());
        return c;
    }

    public ConteudoResponse toResponse(Conteudo c) {
        return new ConteudoResponse(c.getId(), c.getTitulo(), c.getCorpo(),
                c.getAutor(), c.getAtualizadoEm());
    }
}
