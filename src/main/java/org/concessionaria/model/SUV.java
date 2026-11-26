package org.concessionaria.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** SUV: seguro obrigatório de R$ 40,00 por dia somado à diária. */
public class SUV extends Veiculo {
    private static final BigDecimal SEGURO_POR_DIA = new BigDecimal("40.00");

    @Override
    public CategoriaVeiculo getCategoria() {
        return CategoriaVeiculo.SUV;
    }

    @Override
    public BigDecimal calcularValorBase(BigDecimal diaria, int dias) {
        return diaria.add(SEGURO_POR_DIA)
                .multiply(BigDecimal.valueOf(dias))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    protected BigDecimal percentualMultaPorDia() {
        return new BigDecimal("0.50");
    }

    @Override
    public String descricaoRegras() {
        return "SUV: diária + seguro obrigatório de R$ 40,00/dia; multa de 50% da diária por dia de atraso.";
    }
}
