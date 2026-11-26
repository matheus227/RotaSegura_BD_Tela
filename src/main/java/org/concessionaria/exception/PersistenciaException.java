package org.concessionaria.exception;

/** Falha técnica de acesso ao banco de dados (encapsula SQLException). */
public class PersistenciaException extends RuntimeException {
    public PersistenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
