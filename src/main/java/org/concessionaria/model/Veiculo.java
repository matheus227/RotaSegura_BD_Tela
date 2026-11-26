package org.concessionaria.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Veículo da frota. As regras de cobrança variam por categoria (polimorfismo):
 * cada subclasse define como calcular o valor base e qual a multa por dia de atraso.
 */
public abstract class Veiculo {
    private Integer id;
    private String placa;
    private String marca;
    private String modelo;
    private int ano;
    private BigDecimal valorDiaria;
    private StatusVeiculo status = StatusVeiculo.DISPONIVEL;

    public abstract CategoriaVeiculo getCategoria();

    /** Valor base da locação para a quantidade de dias e a diária informadas. */
    public abstract BigDecimal calcularValorBase(BigDecimal diaria, int dias);

    /** Percentual da diária cobrado por dia de atraso (ex.: 0.20 = 20%). */
    protected abstract BigDecimal percentualMultaPorDia();

    /** Texto curto explicando a regra de cobrança da categoria. */
    public abstract String descricaoRegras();

    public BigDecimal calcularValorBase(int dias) {
        return calcularValorBase(valorDiaria, dias);
    }

    public BigDecimal calcularMulta(BigDecimal diaria, int diasAtraso) {
        if (diasAtraso <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return diaria.multiply(percentualMultaPorDia())
                .multiply(BigDecimal.valueOf(diasAtraso))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public int getAno() { return ano; }
    public void setAno(int ano) { this.ano = ano; }
    public BigDecimal getValorDiaria() { return valorDiaria; }
    public void setValorDiaria(BigDecimal valorDiaria) { this.valorDiaria = valorDiaria; }
    public StatusVeiculo getStatus() { return status; }
    public void setStatus(StatusVeiculo status) { this.status = status; }

    public boolean isDisponivel() {
        return status == StatusVeiculo.DISPONIVEL;
    }

    @Override
    public String toString() {
        return placa + " - " + marca + " " + modelo + " [" + getCategoria() + "]";
    }
}
