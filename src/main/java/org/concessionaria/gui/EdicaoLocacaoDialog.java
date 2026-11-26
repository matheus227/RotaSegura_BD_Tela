package org.concessionaria.gui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;
import org.concessionaria.model.Cliente;
import org.concessionaria.model.Locacao;
import org.concessionaria.service.LocacaoService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Caixa de diálogo para editar cliente, retirada e prazo de uma locação em aberto. */
final class EdicaoLocacaoDialog {
    /** Valores informados pelo atendente (ainda não validados: a validação é do serviço). */
    record Dados(Cliente cliente, LocalDate retirada, int dias) {}

    private EdicaoLocacaoDialog() {}

    static Optional<Dados> mostrar(Window dono, Locacao locacao, List<Cliente> clientes) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.initOwner(dono);
        dialogo.setTitle("Editar contrato nº " + locacao.getId());
        dialogo.setHeaderText("Veículo: " + locacao.getVeiculo()
                + "\nPara trocar o veículo, cancele a locação e abra outra.");

        ComboBox<Cliente> cmbCliente = new ComboBox<>(FXCollections.observableArrayList(clientes));
        cmbCliente.setPrefWidth(280);
        clientes.stream()
                .filter(c -> c.getId().equals(locacao.getCliente().getId()))
                .findFirst()
                .ifPresent(cmbCliente::setValue);

        DatePicker dpRetirada = new DatePicker(locacao.getDataRetirada());
        dpRetirada.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate data, boolean vazio) {
                super.updateItem(data, vazio);
                setDisable(vazio || data.isAfter(LocalDate.now()));
            }
        });

        Spinner<Integer> spnDias = new Spinner<>(1, LocacaoService.MAXIMO_DIAS, locacao.getDiasPrevistos());
        spnDias.setEditable(true);
        spnDias.setPrefWidth(100);

        GridPane grade = new GridPane();
        grade.setHgap(12);
        grade.setVgap(10);
        grade.setPadding(new Insets(12));
        grade.addRow(0, new Label("Cliente *"), cmbCliente);
        grade.addRow(1, new Label("Retirada *"), dpRetirada);
        grade.addRow(2, new Label("Dias previstos *"), spnDias);

        dialogo.getDialogPane().setContent(grade);
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> resposta = dialogo.showAndWait();
        if (resposta.isEmpty() || resposta.get() != ButtonType.OK) {
            return Optional.empty();
        }
        try {
            spnDias.increment(0); // confirma o texto digitado no campo, se ainda não confirmado
        } catch (RuntimeException e) {
            // texto inválido: mantém o último valor válido do spinner
        }
        return Optional.of(new Dados(cmbCliente.getValue(), dpRetirada.getValue(), spnDias.getValue()));
    }
}
