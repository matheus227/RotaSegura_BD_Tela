package org.concessionaria.service;

import org.concessionaria.model.Locacao;
import org.concessionaria.model.ResumoFechamento;
import org.concessionaria.util.Formatos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

/** Gera o texto do relatório de encerramento e o salva em arquivo. */
public class RelatorioService {
    private static final String LINHA = "=".repeat(56);
    private static final String DIVISOR = "-".repeat(56);

    public String gerar(Locacao l, ResumoFechamento r, boolean encerrada) {
        StringBuilder sb = new StringBuilder();
        sb.append(LINHA).append('\n');
        sb.append(" ROTA SEGURA - LOCADORA DE VEÍCULOS\n");
        sb.append(encerrada ? " RELATÓRIO DE ENCERRAMENTO DE LOCAÇÃO\n"
                            : " PRÉVIA DE FECHAMENTO (contrato ainda em aberto)\n");
        sb.append(LINHA).append('\n');
        sb.append(String.format("Contrato nº ...: %d%n", l.getId()));
        sb.append(String.format("Emitido em ....: %s%n", Formatos.DATA_HORA.format(LocalDateTime.now())));
        sb.append(DIVISOR).append('\n');

        sb.append("CLIENTE\n");
        sb.append(String.format("  Nome ........: %s%n", l.getCliente().getNome()));
        sb.append(String.format("  CPF .........: %s%n", Formatos.cpf(l.getCliente().getCpf())));
        sb.append(String.format("  CNH .........: %s%n", l.getCliente().getCnh()));
        sb.append(DIVISOR).append('\n');

        sb.append("VEÍCULO\n");
        sb.append(String.format("  Placa .......: %s%n", l.getVeiculo().getPlaca()));
        sb.append(String.format("  Modelo ......: %s %s (%d)%n",
                l.getVeiculo().getMarca(), l.getVeiculo().getModelo(), l.getVeiculo().getAno()));
        sb.append(String.format("  Categoria ...: %s%n", l.getVeiculo().getCategoria()));
        sb.append(String.format("  Diária ......: %s%n", Formatos.moeda(l.getValorDiariaContratada())));
        sb.append("  Regra .......: ").append(l.getVeiculo().descricaoRegras()).append('\n');
        sb.append(DIVISOR).append('\n');

        sb.append("PERÍODO\n");
        sb.append(String.format("  Retirada ....: %s%n", Formatos.data(l.getDataRetirada())));
        sb.append(String.format("  Prevista ....: %s (%d dia(s))%n",
                Formatos.data(l.getDataPrevistaDevolucao()), r.diasPrevistos()));
        sb.append(String.format("  Devolução ...: %s%n", Formatos.data(r.dataDevolucao())));
        sb.append(String.format("  Dias usados .: %d%n", r.diasUtilizados()));
        sb.append(String.format("  Dias atraso .: %d%n", r.diasAtraso()));
        sb.append(DIVISOR).append('\n');

        sb.append("VALORES\n");
        sb.append(String.format("  Valor base ..: %s%n", Formatos.moeda(r.valorBase())));
        sb.append(String.format("  Multa .......: %s%n", Formatos.moeda(r.multa())));
        sb.append(String.format("  TOTAL .......: %s%n", Formatos.moeda(r.total())));
        sb.append(LINHA).append('\n');
        return sb.toString();
    }

    public void salvar(String relatorio, Path destino) throws IOException {
        Files.writeString(destino, relatorio, StandardCharsets.UTF_8);
    }
}
