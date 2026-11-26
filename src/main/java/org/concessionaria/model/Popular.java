package org.concessionaria.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Popular: diária simples, com 10% de desconto a partir de 7 dias. */
public class Popular extends Veiculo {
    private static final BigDecimal DESCONTO_LONGA_DURACAO = new BigDecimal("0.90");

    @Override
    public CategoriaVeiculo getCategoria() {
        return CategoriaVeiculo.POPULAR;
    }

    @Override
    public BigDecimal calcularValorBase(BigDecimal diaria, int dias) {
        BigDecimal total = diaria.multiply(BigDecimal.valueOf(dias));
        if (dias >= 7) {
            total = total.multiply(DESCONTO_LONGA_DURACAO);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    protected BigDecimal percentualMultaPorDia() {
        return new BigDecimal("0.20");
    }

    @Override
    public String descricaoRegras() {
        return "Popular: diária simples; 10% de desconto a partir de 7 dias; multa de 20% da diária por dia de atraso.";
    }
}
