package org.concessionaria.gui;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.util.regex.Pattern;

/**
 * Restrições de digitação para os campos de texto: impedem, já na interface, caracteres
 * inesperados e textos longos demais. A validação definitiva continua nos serviços.
 */
final class Mascaras {
    private Mascaras() {}

    /**
     * Aceita apenas os caracteres da classe informada (sintaxe de classe de caracteres de regex,
     * ex.: {@code "0-9.\\-"}) e no máximo {@code maximo} caracteres.
     */
    static void permitir(TextField campo, String classeDeCaracteres, int maximo) {
        Pattern padrao = Pattern.compile("[" + classeDeCaracteres + "]*");
        campo.setTextFormatter(new TextFormatter<String>(mudanca -> {
            String novoTexto = mudanca.getControlNewText();
            return novoTexto.length() <= maximo && padrao.matcher(novoTexto).matches() ? mudanca : null;
        }));
    }

    /** Qualquer caractere, mas no máximo {@code maximo} caracteres. */
    static void limitarTamanho(TextField campo, int maximo) {
        campo.setTextFormatter(new TextFormatter<String>(mudanca ->
                mudanca.getControlNewText().length() <= maximo ? mudanca : null));
    }
}
