package org.concessionaria.model;

/** Cria a subclasse correta de Veículo a partir da categoria. */
public final class VeiculoFactory {
    private VeiculoFactory() {}

    public static Veiculo criar(CategoriaVeiculo categoria) {
        return switch (categoria) {
            case POPULAR -> new Popular();
            case SEDAN -> new Sedan();
            case SUV -> new SUV();
        };
    }
}
