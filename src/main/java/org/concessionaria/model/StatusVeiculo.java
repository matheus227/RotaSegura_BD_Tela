package org.concessionaria.model;

public enum StatusVeiculo {
    DISPONIVEL("Disponível"),
    LOCADO("Locado");

    private final String rotulo;

    StatusVeiculo(String rotulo) {
        this.rotulo = rotulo;
    }

    @Override
    public String toString() {
        return rotulo;
    }
}
