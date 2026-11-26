package org.concessionaria.gui;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.concessionaria.App;
import org.concessionaria.model.Cliente;
import org.concessionaria.model.Locacao;
import org.concessionaria.model.StatusLocacao;
import org.concessionaria.model.Veiculo;
import org.concessionaria.service.LocacaoService;
import org.concessionaria.service.Servicos;
import org.concessionaria.util.Formatos;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public class LocacoesController {
    private final LocacaoService servico = Servicos.locacoes();
    private final ObservableList<Locacao> dados = FXCollections.observableArrayList();
    private FilteredList<Locacao> filtradas;

    @FXML private ComboBox<Cliente> cmbCliente;
    @FXML private ComboBox<Veiculo> cmbVeiculo;
    @FXML private DatePicker dpRetirada;
    @FXML private Spinner<Integer> spnDias;
    @FXML private Label lblEstimativa, lblRegras;
    @FXML private CheckBox chkSomenteAbertas;
    @FXML private Button btnEncerrar, btnRelatorio, btnEditar, btnCancelar;
    @FXML private TableView<Locacao> tabela;
    @FXML private TableColumn<Locacao, Number> colId;
    @FXML private TableColumn<Locacao, String> colCliente, colVeiculo, colCategoria, colRetirada,
            colPrevista, colDevolucao, colValor;
    @FXML private TableColumn<Locacao, StatusLocacao> colStatus;

    @FXML
    private void initialize() {
        // --- formulário de nova locação ---
        dpRetirada.setValue(LocalDate.now());
        dpRetirada.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate data, boolean vazio) {
                super.updateItem(data, vazio);
                setDisable(vazio || data.isAfter(LocalDate.now()));
            }
        });
        spnDias.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, LocacaoService.MAXIMO_DIAS, 1));
        spnDias.getEditor().focusedProperty().addListener((obs, tinha, tem) -> {
            if (!tem) { // confirma o texto digitado ao sair do campo
                try {
                    spnDias.increment(0);
                } catch (RuntimeException e) {
                    spnDias.getEditor().setText(String.valueOf(spnDias.getValue()));
                }
            }
        });
        cmbVeiculo.valueProperty().addListener((obs, antigo, novo) -> atualizarEstimativa());
        spnDias.valueProperty().addListener((obs, antigo, novo) -> atualizarEstimativa());
        atualizarEstimativa();

        // --- tabela ---
        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()));
        colCliente.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCliente().getNome()));
        colVeiculo.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getVeiculo().getPlaca() + " - " + c.getValue().getVeiculo().getModelo()));
        colCategoria.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getVeiculo().getCategoria().toString()));
        colRetirada.setCellValueFactory(c -> new SimpleStringProperty(Formatos.data(c.getValue().getDataRetirada())));
        colPrevista.setCellValueFactory(c -> new SimpleStringProperty(
                Formatos.data(c.getValue().getDataPrevistaDevolucao())));
        colDevolucao.setCellValueFactory(c -> new SimpleStringProperty(Formatos.data(c.getValue().getDataDevolucao())));
        colValor.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getValorTotal() == null ? "em aberto" : Formatos.moeda(c.getValue().getValorTotal())));
        colStatus.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(StatusLocacao status, boolean vazio) {
                super.updateItem(status, vazio);
                getStyleClass().removeAll("status-locado", "status-disponivel");
                if (vazio || status == null) {
                    setText(null);
                } else {
                    setText(status.toString());
                    getStyleClass().add(status == StatusLocacao.ABERTA ? "status-locado" : "status-disponivel");
                }
            }
        });

        filtradas = new FilteredList<>(dados, l -> true);
        SortedList<Locacao> ordenadas = new SortedList<>(filtradas);
        ordenadas.comparatorProperty().bind(tabela.comparatorProperty());
        tabela.setItems(ordenadas);
        chkSomenteAbertas.selectedProperty().addListener((obs, antigo, novo) ->
                filtradas.setPredicate(l -> !novo || l.getStatus() == StatusLocacao.ABERTA));

        var selecionada = tabela.getSelectionModel().selectedItemProperty();
        var naoEstaAberta = Bindings.createBooleanBinding(
                () -> selecionada.get() == null || selecionada.get().getStatus() != StatusLocacao.ABERTA, selecionada);
        btnEncerrar.disableProperty().bind(naoEstaAberta);
        btnEditar.disableProperty().bind(naoEstaAberta);
        btnCancelar.disableProperty().bind(naoEstaAberta);
        btnRelatorio.disableProperty().bind(Bindings.createBooleanBinding(
                () -> selecionada.get() == null || selecionada.get().getStatus() != StatusLocacao.ENCERRADA, selecionada));

        recarregar();
    }

    /** Recarrega combos e tabela do banco, preservando a seleção do formulário quando possível. */
    public void recarregar() {
        try {
            Cliente clienteSelecionado = cmbCliente.getValue();
            Veiculo veiculoSelecionado = cmbVeiculo.getValue();

            cmbCliente.setItems(FXCollections.observableArrayList(Servicos.clientes().listar()));
            cmbVeiculo.setItems(FXCollections.observableArrayList(Servicos.veiculos().listarDisponiveis()));
            dados.setAll(servico.listar());

            if (clienteSelecionado != null) {
                cmbCliente.getItems().stream()
                        .filter(c -> c.getId().equals(clienteSelecionado.getId()))
                        .findFirst().ifPresent(cmbCliente::setValue);
            }
            if (veiculoSelecionado != null) {
                cmbVeiculo.getItems().stream()
                        .filter(v -> v.getId().equals(veiculoSelecionado.getId()))
                        .findFirst().ifPresent(cmbVeiculo::setValue);
            }
        } catch (RuntimeException e) {
            Alertas.tratar(e);
        }
    }

    @FXML
    private void onAbrir() {
        try {
            Locacao locacao = servico.abrir(cmbCliente.getValue(), cmbVeiculo.getValue(),
                    dpRetirada.getValue(), spnDias.getValue());
            Alertas.info("Locação aberta",
                    "Contrato nº " + locacao.getId() + " aberto com sucesso.\n"
                            + "Previsão de devolução: " + Formatos.data(locacao.getDataPrevistaDevolucao()) + "\n"
                            + "Valor estimado: " + Formatos.moeda(servico.estimarValor(locacao)));
            cmbCliente.setValue(null);
            cmbVeiculo.setValue(null);
            dpRetirada.setValue(LocalDate.now());
            spnDias.getValueFactory().setValue(1);
            recarregar();
        } catch (RuntimeException e) {
            Alertas.tratar(e);
            recarregar(); // a tela pode estar desatualizada (ex.: veículo locado em outro atendimento)
        }
    }

    @FXML
    private void onEncerrar() {
        abrirFechamento(tabela.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void onRelatorio() {
        abrirFechamento(tabela.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void onEditar() {
        Locacao selecionada = tabela.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            Alertas.aviso("Atenção", "Selecione uma locação em aberto na tabela.");
            return;
        }
        try {
            Optional<EdicaoLocacaoDialog.Dados> dados = EdicaoLocacaoDialog.mostrar(
                    tabela.getScene().getWindow(), selecionada, Servicos.clientes().listar());
            if (dados.isEmpty()) {
                return;
            }
            servico.editar(selecionada, dados.get().cliente(), dados.get().retirada(), dados.get().dias());
            Alertas.info("Locação atualizada",
                    "Contrato nº " + selecionada.getId() + " atualizado.\n"
                            + "Previsão de devolução: " + Formatos.data(selecionada.getDataPrevistaDevolucao()) + "\n"
                            + "Valor estimado: " + Formatos.moeda(servico.estimarValor(selecionada)));
            recarregar();
        } catch (RuntimeException e) {
            Alertas.tratar(e);
            recarregar();
        }
    }

    @FXML
    private void onCancelar() {
        Locacao selecionada = tabela.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            Alertas.aviso("Atenção", "Selecione uma locação em aberto na tabela.");
            return;
        }
        if (!Alertas.confirmar("Cancelar locação",
                "Cancelar o contrato nº " + selecionada.getId() + " (" + selecionada.getVeiculo().getPlaca() + ")?\n\n"
                        + "O contrato será removido e o veículo voltará a ficar disponível.")) {
            return;
        }
        try {
            servico.cancelar(selecionada);
            recarregar();
        } catch (RuntimeException e) {
            Alertas.tratar(e);
            recarregar();
        }
    }

    @FXML
    private void onAtualizar() {
        recarregar();
    }

    private void abrirFechamento(Locacao locacao) {
        if (locacao == null) {
            Alertas.aviso("Atenção", "Selecione uma locação na tabela.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    getClass().getResource("/view/fechamento.fxml")));
            Parent raiz = loader.load();
            FechamentoController controller = loader.getController();
            controller.setLocacao(locacao);

            Scene cena = new Scene(raiz);
            cena.getStylesheets().add(Objects.requireNonNull(App.class.getResource(App.CSS)).toExternalForm());
            Stage janela = new Stage();
            janela.initOwner(tabela.getScene().getWindow());
            janela.initModality(Modality.APPLICATION_MODAL);
            janela.setTitle("Contrato nº " + locacao.getId());
            janela.setScene(cena);
            janela.showAndWait();
            recarregar();
        } catch (IOException e) {
            Alertas.erro("Erro", "Não foi possível abrir a tela de fechamento: " + e.getMessage());
        } catch (RuntimeException e) {
            Alertas.tratar(e);
            recarregar();
        }
    }

    private void atualizarEstimativa() {
        Veiculo v = cmbVeiculo.getValue();
        Integer dias = spnDias.getValue();
        if (v == null || dias == null) {
            lblEstimativa.setText("Valor estimado: selecione um veículo");
            lblRegras.setText("");
            return;
        }
        lblEstimativa.setText("Valor estimado (" + dias + " dia(s)): " + Formatos.moeda(servico.estimarValor(v, dias)));
        lblRegras.setText(v.descricaoRegras());
    }
}
