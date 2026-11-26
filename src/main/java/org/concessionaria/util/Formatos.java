package org.concessionaria.util;

import org.concessionaria.exception.ValidacaoException;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatação para exibição e conversão de texto digitado nos formulários. */
public final class Formatos {
    private static final Locale PT_BR = Locale.of("pt", "BR");
    public static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Formatos() {}

    public static String moeda(BigDecimal valor) {
        return valor == null ? "" : NumberFormat.getCurrencyInstance(PT_BR).format(valor);
    }

    public static String data(LocalDate data) {
        return data == null ? "" : DATA.format(data);
    }

    public static String cpf(String digitos) {
        if (digitos != null && digitos.length() == 11) {
            return digitos.substring(0, 3) + "." + digitos.substring(3, 6) + "."
                    + digitos.substring(6, 9) + "-" + digitos.substring(9);
        }
        return digitos == null ? "" : digitos;
    }

    public static String telefone(String digitos) {
        if (digitos == null) {
            return "";
        }
        if (digitos.length() == 11) {
            return "(" + digitos.substring(0, 2) + ") " + digitos.substring(2, 7) + "-" + digitos.substring(7);
        }
        if (digitos.length() == 10) {
            return "(" + digitos.substring(0, 2) + ") " + digitos.substring(2, 6) + "-" + digitos.substring(6);
        }
        return digitos;
    }

    /** Converte "1.234,56", "89,90" ou "89.90" em BigDecimal. */
    public static BigDecimal lerDecimal(String texto, String campo) {
        if (Validador.vazio(texto)) {
            throw new ValidacaoException("Informe o campo " + campo + ".");
        }
        String limpo = texto.trim().replace("R$", "").replace(" ", "");
        if (limpo.contains(",")) {
            limpo = limpo.replace(".", "").replace(",", ".");
        }
        try {
            return new BigDecimal(limpo);
        } catch (NumberFormatException e) {
            throw new ValidacaoException("Valor inválido no campo " + campo + ".");
        }
    }

    public static int lerInteiro(String texto, String campo) {
        if (Validador.vazio(texto)) {
            throw new ValidacaoException("Informe o campo " + campo + ".");
        }
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            throw new ValidacaoException("Valor inválido no campo " + campo + ".");
        }
    }

    public static String decimalParaCampo(BigDecimal valor) {
        return valor == null ? "" : valor.setScale(2).toPlainString().replace('.', ',');
    }
}
