package org.concessionaria.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.concessionaria.model.Locacao;
import org.concessionaria.model.ResumoFechamento;
import org.concessionaria.model.StatusLocacao;
import org.concessionaria.exception.NegocioException;
import org.concessionaria.service.LocacaoService;
import org.concessionaria.service.RelatorioService;
import org.concessionaria.service.Servicos;
import org.concessionaria.util.Formatos;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;

/**
 * Tela de fechamento de contrato. Com a locação ABERTA mostra uma prévia que se recalcula
 * a cada mudança na data de devolução; depois de confirmado (ou para locações já encerradas),
 * mostra o relatório final, que pode ser salvo em arquivo.
 */
public class FechamentoController {
    private final LocacaoService servico = Servicos.locacoes();
    private final RelatorioService relatorios = Servicos.relatorios();
    private Locacao locacao;
    private boolean modoPrevia;

    @FXML private Label lblContrato, lblCliente, lblVeiculo, lblPeriodo, lblTotal, lblSituacao;
    @FXML private DatePicker dpDevolucao;
    @FXML private TextArea txtRelatorio;
    @FXML private Button btnConfirmar, btnSalvar;

    @FXML
    private void initialize() {
        dpDevolucao.valueProperty().addListener((obs, antigo, novo) -> {
            if (modoPrevia) {
                atualizarPrevia();
            }
        });
    }

    public void setLocacao(Locacao locacao) {
        this.locacao = locacao;
        lblContrato.setText("Contrato nº " + locacao.getId());
        lblCliente.setText(locacao.getCliente().getNome() + "  |  CPF " + Formatos.cpf(locacao.getCliente().getCpf()));
        lblVeiculo.setText(locacao.getVeiculo().getPlaca() + " - " + locacao.getVeiculo().getMarca() + " "
                + locacao.getVeiculo().getModelo() + " (" + locacao.getVeiculo().getCategoria() + ")");
        lblPeriodo.setText("Retirada " + Formatos.data(locacao.getDataRetirada())
                + "  |  Previsão de devolução " + Formatos.data(locacao.getDataPrevistaDevolucao()));

        if (locacao.getStatus() == StatusLocacao.ABERTA) {
            dpDevolucao.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate data, boolean vazio) {
                    super.updateItem(data, vazio);
                    setDisable(vazio || data.isAfter(LocalDate.now()) || data.isBefore(locacao.getDataRetirada()));
                }
            });
            modoPrevia = true;
            dpDevolucao.setValue(LocalDate.now());
            atualizarPrevia();
        } else {
            modoPrevia = false;
            dpDevolucao.setValue(locacao.getDataDevolucao());
            mostrarEncerrada(servico.resumoDeLocacaoEncerrada(locacao));
        }
    }

    @FXML
    private void onConfirmar() {
        try {
            LocalDate devolucao = dpDevolucao.getValue();
            ResumoFechamento previa = servico.simularFechamento(locacao, devolucao);
            boolean confirmado = Alertas.confirmar("Confirmar encerramento",
                    "Encerrar o contrato nº " + locacao.getId() + "?\n\n"
                            + "Devolução: " + Formatos.data(devolucao) + "\n"
                            + "Total a cobrar: " + Formatos.moeda(previa.total()));
            if (!confirmado) {
                return;
            }
            modoPrevia = false;
            ResumoFechamento resumo = servico.encerrar(locacao, devolucao);
            mostrarEncerrada(resumo);
            Alertas.info("Locação encerrada",
                    "Contrato encerrado e veículo " + locacao.getVeiculo().getPlaca() + " liberado.\n"
                            + "Total cobrado: " + Formatos.moeda(resumo.total()));
        } catch (RuntimeException e) {
            modoPrevia = locacao.getStatus() == StatusLocacao.ABERTA;
            Alertas.tratar(e);
        }
    }

    @FXML
    private void onSalvarRelatorio() {
        FileChooser escolhedor = new FileChooser();
        escolhedor.setTitle("Salvar relatório de encerramento");
        escolhedor.setInitialFileName("relatorio_contrato_" + locacao.getId() + ".txt");
        escolhedor.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arquivo de texto (*.txt)", "*.txt"));
        File arquivo = escolhedor.showSaveDialog(txtRelatorio.getScene().getWindow());
        if (arquivo == null) {
            return;
        }
        try {
            relatorios.salvar(txtRelatorio.getText(), arquivo.toPath());
            Alertas.info("Relatório salvo", "Arquivo salvo em:\n" + arquivo.getAbsolutePath());
        } catch (IOException e) {
            Alertas.erro("Erro ao salvar", "Não foi possível salvar o relatório: " + e.getMessage());
        }
    }

    @FXML
    private void onFechar() {
        ((Stage) txtRelatorio.getScene().getWindow()).close();
    }

    private void atualizarPrevia() {
        lblSituacao.setText("EM ABERTO - prévia do fechamento");
        btnSalvar.setDisable(true);
        try {
            ResumoFechamento resumo = servico.simularFechamento(locacao, dpDevolucao.getValue());
            txtRelatorio.setText(relatorios.gerar(locacao, resumo, false));
            lblTotal.setText(Formatos.moeda(resumo.total()));
            btnConfirmar.setDisable(false);
        } catch (NegocioException e) {
            txtRelatorio.setText(e.getMessage());
            lblTotal.setText("-");
            btnConfirmar.setDisable(true);
        }
    }

    private void mostrarEncerrada(ResumoFechamento resumo) {
        lblSituacao.setText("ENCERRADA");
        txtRelatorio.setText(relatorios.gerar(locacao, resumo, true));
        lblTotal.setText(Formatos.moeda(resumo.total()));
        dpDevolucao.setDisable(true);
        btnConfirmar.setDisable(true);
        btnSalvar.setDisable(false);
    }
}
