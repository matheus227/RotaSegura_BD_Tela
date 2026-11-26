package org.concessionaria.gui;

import javafx.fxml.FXML;
import javafx.scene.control.TabPane;

/** Janela principal: recarrega os dados da aba sempre que ela é selecionada. */
public class MainController {
    @FXML private TabPane tabPane;

    // Injetados pelo JavaFX: nome do fx:id do <fx:include> + "Controller"
    @FXML private ClientesController clientesController;
    @FXML private VeiculosController veiculosController;
    @FXML private LocacoesController locacoesController;

    @FXML
    private void initialize() {
        tabPane.getSelectionModel().selectedIndexProperty().addListener((obs, antigo, novo) -> {
            switch (novo.intValue()) {
                case 0 -> clientesController.recarregar();
                case 1 -> veiculosController.recarregar();
                case 2 -> locacoesController.recarregar();
                default -> { }
            }
        });
    }
}
