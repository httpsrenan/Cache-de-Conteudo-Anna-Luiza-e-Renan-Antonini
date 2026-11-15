package com.exemplo.conteudos.repository;

import com.exemplo.conteudos.model.Conteudo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConteudoRepository extends JpaRepository<Conteudo, Long> {
}
