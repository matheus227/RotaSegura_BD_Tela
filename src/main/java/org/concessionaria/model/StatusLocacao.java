package org.concessionaria.model;

public enum StatusLocacao {
    ABERTA("Aberta"),
    ENCERRADA("Encerrada");

    private final String rotulo;

    StatusLocacao(String rotulo) {
        this.rotulo = rotulo;
    }

    @Override
    public String toString() {
        return rotulo;
    }
}
