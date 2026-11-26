package org.concessionaria.model;

public enum CategoriaVeiculo {
    POPULAR("Popular"),
    SEDAN("Sedan"),
    SUV("SUV");

    private final String rotulo;

    CategoriaVeiculo(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    @Override
    public String toString() {
        return rotulo;
    }
}
