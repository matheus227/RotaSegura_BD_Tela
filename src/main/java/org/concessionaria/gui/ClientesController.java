package org.concessionaria.gui;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.concessionaria.model.Cliente;
import org.concessionaria.service.ClienteService;
import org.concessionaria.service.Servicos;
import org.concessionaria.util.Formatos;
import org.concessionaria.util.Validador;

public class ClientesController {
    private final ClienteService servico = Servicos.clientes();
    private final ObservableList<Cliente> dados = FXCollections.observableArrayList();
    private FilteredList<Cliente> filtrados;
    private Integer idEmEdicao;

    @FXML private Label lblModo;
    @FXML private TextField txtNome, txtCpf, txtCnh, txtTelefone, txtEmail, txtBusca;
    @FXML private Button btnExcluir;
    @FXML private TableView<Cliente> tabela;
    @FXML private TableColumn<Cliente, Number> colId;
    @FXML private TableColumn<Cliente, String> colNome, colCpf, colCnh, colTelefone, colEmail;

    @FXML
    private void initialize() {
        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()));
        colNome.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNome()));
        colCpf.setCellValueFactory(c -> new SimpleStringProperty(Formatos.cpf(c.getValue().getCpf())));
        colCnh.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCnh()));
        colTelefone.setCellValueFactory(c -> new SimpleStringProperty(Formatos.telefone(c.getValue().getTelefone())));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));

        // Restrições de digitação (a validação definitiva é feita em ClienteService)
        Mascaras.limitarTamanho(txtNome, 100);
        Mascaras.permitir(txtCpf, "0-9.\\-", 14);
        Mascaras.permitir(txtCnh, "0-9", 11);
        Mascaras.permitir(txtTelefone, "0-9()\\- ", 15);
        Mascaras.limitarTamanho(txtEmail, 100);

        filtrados = new FilteredList<>(dados, c -> true);
        SortedList<Cliente> ordenados = new SortedList<>(filtrados);
        ordenados.comparatorProperty().bind(tabela.comparatorProperty());
        tabela.setItems(ordenados);

        txtBusca.textProperty().addListener((obs, antigo, novo) -> filtrar(novo));
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            if (novo != null) {
                editar(novo);
            }
        });

        onNovo();
        recarregar();
    }

    /** Recarrega a lista a partir do banco (chamado ao abrir a aba). */
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
        lblModo.setText("Novo cliente");
        txtNome.clear();
        txtCpf.clear();
        txtCnh.clear();
        txtTelefone.clear();
        txtEmail.clear();
        btnExcluir.setDisable(true);
        txtNome.requestFocus();
    }

    @FXML
    private void onSalvar() {
        try {
            Cliente c = new Cliente();
            c.setId(idEmEdicao);
            c.setNome(txtNome.getText());
            c.setCpf(txtCpf.getText());
            c.setCnh(txtCnh.getText());
            c.setTelefone(txtTelefone.getText());
            c.setEmail(txtEmail.getText());

            servico.salvar(c);
            recarregar();
            onNovo();
            Alertas.info("Sucesso", "Cliente salvo com sucesso.");
        } catch (RuntimeException e) {
            Alertas.tratar(e);
        }
    }

    @FXML
    private void onExcluir() {
        Cliente selecionado = tabela.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            Alertas.aviso("Atenção", "Selecione um cliente na tabela para excluir.");
            return;
        }
        if (!Alertas.confirmar("Excluir cliente", "Excluir o cliente \"" + selecionado.getNome() + "\"?")) {
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

    private void editar(Cliente c) {
        idEmEdicao = c.getId();
        lblModo.setText("Editando cliente nº " + c.getId());
        txtNome.setText(c.getNome());
        txtCpf.setText(Formatos.cpf(c.getCpf()));
        txtCnh.setText(c.getCnh());
        txtTelefone.setText(Formatos.telefone(c.getTelefone()));
        txtEmail.setText(c.getEmail());
        btnExcluir.setDisable(false);
    }

    private void filtrar(String texto) {
        String t = texto == null ? "" : texto.trim().toLowerCase();
        String digitos = Validador.somenteDigitos(t);
        filtrados.setPredicate(c -> t.isEmpty()
                || c.getNome().toLowerCase().contains(t)
                || (!digitos.isEmpty() && c.getCpf().contains(digitos)));
    }
}
