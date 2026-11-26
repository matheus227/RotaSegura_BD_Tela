package org.concessionaria.dao;

import java.util.List;
import java.util.Optional;

/** Contrato básico de CRUD do padrão DAO. */
public interface GenericDAO<T> {
    void inserir(T entidade);

    void atualizar(T entidade);

    void remover(int id);

    Optional<T> buscarPorId(int id);

    List<T> listarTodos();
}
