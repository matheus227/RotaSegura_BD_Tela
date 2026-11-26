package org.concessionaria.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Sedan: acréscimo de 10% de taxa de conforto sobre o total. */
public class Sedan extends Veiculo {
    private static final BigDecimal TAXA_CONFORTO = new BigDecimal("1.10");

    @Override
    public CategoriaVeiculo getCategoria() {
        return CategoriaVeiculo.SEDAN;
    }

    @Override
    public BigDecimal calcularValorBase(BigDecimal diaria, int dias) {
        return diaria.multiply(BigDecimal.valueOf(dias))
                .multiply(TAXA_CONFORTO)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    protected BigDecimal percentualMultaPorDia() {
        return new BigDecimal("0.30");
    }

    @Override
    public String descricaoRegras() {
        return "Sedan: diária + 10% de taxa de conforto; multa de 30% da diária por dia de atraso.";
    }
}
