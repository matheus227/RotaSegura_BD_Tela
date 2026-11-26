package org.concessionaria.gui;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.concessionaria.model.CategoriaVeiculo;
import org.concessionaria.model.StatusVeiculo;
import org.concessionaria.model.Veiculo;
import org.concessionaria.model.VeiculoFactory;
import org.concessionaria.service.Servicos;
import org.concessionaria.service.VeiculoService;
import org.concessionaria.util.Formatos;
import org.concessionaria.util.Validador;

public class VeiculosController {
    private final VeiculoService servico = Servicos.veiculos();
    private final ObservableList<Veiculo> dados = FXCollections.observableArrayList();
    private FilteredList<Veiculo> filtrados;
    private Integer idEmEdicao;

    @FXML private Label lblModo, lblRegras;
    @FXML private ComboBox<CategoriaVeiculo> cmbCategoria, cmbFiltroCategoria;
    @FXML private TextField txtPlaca, txtMarca, txtModelo, txtAno, txtDiaria, txtBusca;
    @FXML private Button btnSalvar, btnExcluir;
    @FXML private TableView<Veiculo> tabela;
    @FXML private TableColumn<Veiculo, Number> colId, colAno;
    @FXML private TableColumn<Veiculo, String> colPlaca, colMarca, colModelo, colDiaria;
    @FXML private TableColumn<Veiculo, CategoriaVeiculo> colCategoria;
    @FXML private TableColumn<Veiculo, StatusVeiculo> colStatus;

    @FXML
    private void initialize() {
        cmbCategoria.setItems(FXCollections.observableArrayList(CategoriaVeiculo.values()));
        cmbFiltroCategoria.setItems(FXCollections.observableArrayList(CategoriaVeiculo.values()));
        cmbCategoria.valueProperty().addListener((obs, antigo, novo) ->
                lblRegras.setText(novo == null ? "" : VeiculoFactory.criar(novo).descricaoRegras()));

        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()));
        colPlaca.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPlaca()));
        colMarca.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMarca()));
        colModelo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getModelo()));
        colAno.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getAno()));
        colCategoria.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getCategoria()));
        colDiaria.setCellValueFactory(c -> new SimpleStringProperty(Formatos.moeda(c.getValue().getValorDiaria())));
        colStatus.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(StatusVeiculo status, boolean vazio) {
                super.updateItem(status, vazio);
                getStyleClass().removeAll("status-locado", "status-disponivel");
                if (vazio || status == null) {
                    setText(null);
                } else {
                    setText(status.toString());
                    getStyleClass().add(status == StatusVeiculo.LOCADO ? "status-locado" : "status-disponivel");
                }
            }
        });

        // Restrições de digitação (a validação definitiva é feita em VeiculoService)
        Mascaras.permitir(txtPlaca, "A-Za-z0-9\\-", 8);
        Mascaras.limitarTamanho(txtMarca, 40);
        Mascaras.limitarTamanho(txtModelo, 40);
        Mascaras.permitir(txtAno, "0-9", 4);
        Mascaras.permitir(txtDiaria, "0-9.,", 10);

        filtrados = new FilteredList<>(dados, v -> true);
        SortedList<Veiculo> ordenados = new SortedList<>(filtrados);
        ordenados.comparatorProperty().bind(tabela.comparatorProperty());
        tabela.setItems(ordenados);

        txtBusca.textProperty().addListener((obs, antigo, novo) -> aplicarFiltro());
        cmbFiltroCategoria.valueProperty().addListener((obs, antigo, novo) -> aplicarFiltro());
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            if (novo != null) {
                editar(novo);
            }
        });

        onNovo();
        recarregar();
    }

    public void recarregar() {
        try {
            dados.setAll(servico.listar());
        } catch (RuntimeException e) {
            Alertas.tratar(e);
        }
    }

    @FXML
    private void onNovo() {
        tabela.getSelectionModel().clearSelection();
        idEmEdicao = null;
        lblModo.setText("Novo veículo");
        txtPlaca.clear();
        txtMarca.clear();
        txtModelo.clear();
        txtAno.clear();
        txtDiaria.clear();
        cmbCategoria.setValue(CategoriaVeiculo.POPULAR);
        btnSalvar.setDisable(false);
        btnExcluir.setDisable(true);
        txtPlaca.requestFocus();
    }

    @FXML
    private void onSalvar() {
        try {
            CategoriaVeiculo categoria = cmbCategoria.getValue();
            if (categoria == null) {
                Alertas.aviso("Atenção", "Selecione a categoria do veículo.");
                return;
            }
            Veiculo v = VeiculoFactory.criar(categoria);
            v.setId(idEmEdicao);
            v.setPlaca(txtPlaca.getText());
            v.setMarca(txtMarca.getText());
            v.setModelo(txtModelo.getText());
            v.setAno(Formatos.lerInteiro(txtAno.getText(), "Ano"));
            v.setValorDiaria(Formatos.lerDecimal(txtDiaria.getText(), "Diária"));

            servico.salvar(v);
            recarregar();
            onNovo();
            Alertas.info("Sucesso", "Veículo salvo com sucesso.");
        } catch (RuntimeException e) {
            Alertas.tratar(e);
        }
    }

    @FXML
    private void onExcluir() {
        Veiculo selecionado = tabela.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            Alertas.aviso("Atenção", "Selecione um veículo na tabela para excluir.");
            return;
        }
        if (!Alertas.confirmar("Excluir veículo", "Excluir o veículo " + selecionado.getPlaca() + "?")) {
            return;
        }
        try {
            servico.remover(selecionado);
            recarregar();
            onNovo();
        } catch (RuntimeException e) {
            Alertas.tratar(e);
        }
    }

    @FXML
    private void onLimparFiltros() {
        txtBusca.clear();
        cmbFiltroCategoria.setValue(null);
    }

    private void editar(Veiculo v) {
        idEmEdicao = v.getId();
        lblModo.setText("Editando veículo nº " + v.getId());
        txtPlaca.setText(v.getPlaca());
        txtMarca.setText(v.getMarca());
        txtModelo.setText(v.getModelo());
        txtAno.setText(String.valueOf(v.getAno()));
        txtDiaria.setText(Formatos.decimalParaCampo(v.getValorDiaria()));
        cmbCategoria.setValue(v.getCategoria());
        boolean locado = v.getStatus() == StatusVeiculo.LOCADO;
        btnSalvar.setDisable(locado);   // veículo locado é somente leitura
        btnExcluir.setDisable(locado);
    }

    private void aplicarFiltro() {
        String t = txtBusca.getText() == null ? "" : txtBusca.getText().trim().toLowerCase();
        String placa = Validador.normalizarPlaca(t).toLowerCase();
        CategoriaVeiculo categoria = cmbFiltroCategoria.getValue();
        filtrados.setPredicate(v ->
                (categoria == null || v.getCategoria() == categoria)
                        && (t.isEmpty()
                        || v.getMarca().toLowerCase().contains(t)
                        || v.getModelo().toLowerCase().contains(t)
                        || (!placa.isEmpty() && v.getPlaca().toLowerCase().contains(placa))));
    }
}
