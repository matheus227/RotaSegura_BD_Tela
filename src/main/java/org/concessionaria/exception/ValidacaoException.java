package org.concessionaria.exception;

/** Dados inválidos informados pelo usuário. Pode conter várias mensagens, uma por linha. */
public class ValidacaoException extends NegocioException {
    public ValidacaoException(String mensagem) {
        super(mensagem);
    }
}
