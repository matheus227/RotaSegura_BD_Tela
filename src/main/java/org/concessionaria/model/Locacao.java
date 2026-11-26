package org.concessionaria.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Locacao {
    private Integer id;
    private Cliente cliente;
    private Veiculo veiculo;
    private LocalDate dataRetirada;
    private LocalDate dataPrevistaDevolucao;
    private LocalDate dataDevolucao;          // null enquanto aberta
    private BigDecimal valorDiariaContratada; // "foto" da diária no momento da abertura
    private BigDecimal valorTotal;            // null enquanto aberta
    private StatusLocacao status = StatusLocacao.ABERTA;

    public int getDiasPrevistos() {
        return (int) Math.max(1, ChronoUnit.DAYS.between(dataRetirada, dataPrevistaDevolucao));
    }

    private BigDecimal diaria() {
        return valorDiariaContratada != null ? valorDiariaContratada : veiculo.getValorDiaria();
    }

    public BigDecimal calcularValorEstimado() {
        return veiculo.calcularValorBase(diaria(), getDiasPrevistos());
    }

    /**
     * Calcula o fechamento usando as regras polimórficas da categoria do veículo.
     * Dias utilizados = dias entre retirada e devolução (mínimo 1).
     * Atraso = dias após a data prevista; gera multa além do valor base.
     */
    public ResumoFechamento calcularResumo(LocalDate devolucao) {
        int diasUtilizados = (int) Math.max(1, ChronoUnit.DAYS.between(dataRetirada, devolucao));
        int diasAtraso = (int) Math.max(0, ChronoUnit.DAYS.between(dataPrevistaDevolucao, devolucao));
        BigDecimal base = veiculo.calcularValorBase(diaria(), diasUtilizados);
        BigDecimal multa = veiculo.calcularMulta(diaria(), diasAtraso);
        return new ResumoFechamento(devolucao, getDiasPrevistos(), diasUtilizados, diasAtraso,
                base, multa, base.add(multa));
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public Veiculo getVeiculo() { return veiculo; }
    public void setVeiculo(Veiculo veiculo) { this.veiculo = veiculo; }
    public LocalDate getDataRetirada() { return dataRetirada; }
    public void setDataRetirada(LocalDate d) { this.dataRetirada = d; }
    public LocalDate getDataPrevistaDevolucao() { return dataPrevistaDevolucao; }
    public void setDataPrevistaDevolucao(LocalDate d) { this.dataPrevistaDevolucao = d; }
    public LocalDate getDataDevolucao() { return dataDevolucao; }
    public void setDataDevolucao(LocalDate d) { this.dataDevolucao = d; }
    public BigDecimal getValorDiariaContratada() { return valorDiariaContratada; }
    public void setValorDiariaContratada(BigDecimal v) { this.valorDiariaContratada = v; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }
    public StatusLocacao getStatus() { return status; }
    public void setStatus(StatusLocacao status) { this.status = status; }
}
