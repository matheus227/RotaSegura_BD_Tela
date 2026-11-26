package org.concessionaria.gui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.concessionaria.exception.NegocioException;
import org.concessionaria.exception.PersistenciaException;

/** Caixas de diálogo padronizadas para feedback ao usuário. */
public final class Alertas {
    private Alertas() {}

    public static void info(String titulo, String mensagem) {
        exibir(Alert.AlertType.INFORMATION, titulo, mensagem);
    }

    public static void aviso(String titulo, String mensagem) {
        exibir(Alert.AlertType.WARNING, titulo, mensagem);
    }

    public static void erro(String titulo, String mensagem) {
        exibir(Alert.AlertType.ERROR, titulo, mensagem);
    }

    public static boolean confirmar(String titulo, String mensagem) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, mensagem, ButtonType.YES, ButtonType.NO);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        return alerta.showAndWait().filter(b -> b == ButtonType.YES).isPresent();
    }

    /** Traduz qualquer exceção das camadas inferiores em uma caixa de diálogo adequada. */
    public static void tratar(RuntimeException e) {
        if (e instanceof NegocioException) {
            aviso("Atenção", e.getMessage());
        } else if (e instanceof PersistenciaException) {
            String detalhe = e.getCause() != null ? "\n\nDetalhe técnico: " + e.getCause().getMessage() : "";
            erro("Erro de banco de dados", e.getMessage() + detalhe);
        } else {
            e.printStackTrace();
            erro("Erro inesperado", String.valueOf(e));
        }
    }

    private static void exibir(Alert.AlertType tipo, String titulo, String mensagem) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensagem);
        alerta.showAndWait();
    }
}
