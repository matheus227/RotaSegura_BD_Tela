package org.concessionaria.exception;

/** Violação de regra de negócio (ex.: veículo já locado). A mensagem é exibida ao usuário. */
public class NegocioException extends RuntimeException {
    public NegocioException(String mensagem) {
        super(mensagem);
    }
}
