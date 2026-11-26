package org.concessionaria;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.concessionaria.dao.BancoDeDados;
import org.concessionaria.exception.PersistenciaException;
import org.concessionaria.gui.Alertas;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class App extends Application {
    public static final String CSS = "/css/estilo.css";
    private static final AtomicBoolean EXIBINDO_ERRO = new AtomicBoolean(false);

    @Override
    public void start(Stage stage) {
        instalarTratadorGlobalDeErros();

        try {
            BancoDeDados.inicializar();
        } catch (PersistenciaException e) {
            Alertas.tratar(e);
            Platform.exit();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(App.class.getResource("/view/main.fxml")));
            Scene scene = new Scene(loader.load(), 1180, 720);
            scene.getStylesheets().add(Objects.requireNonNull(App.class.getResource(CSS)).toExternalForm());

            stage.setTitle("Rota Segura - Gestão de Locações");
            stage.setMinWidth(1000);
            stage.setMinHeight(640);
            stage.setScene(scene);
            stage.show();
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            Alertas.erro("Erro ao iniciar", "Não foi possível abrir a janela principal:\n" + e);
            Platform.exit();
        }
    }

    /**
     * Rede de segurança: qualquer exceção não prevista vira uma caixa de diálogo em vez de
     * um erro silencioso no console, e o sistema continua aberto.
     */
    private static void instalarTratadorGlobalDeErros() {
        Thread.setDefaultUncaughtExceptionHandler((thread, erro) -> {
            erro.printStackTrace();
            if (EXIBINDO_ERRO.compareAndSet(false, true)) {
                Platform.runLater(() -> {
                    try {
                        Alertas.erro("Erro inesperado",
                                "Ocorreu um erro inesperado. O sistema continua aberto.\n\n" + erro);
                    } finally {
                        EXIBINDO_ERRO.set(false);
                    }
                });
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
