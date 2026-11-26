package org.concessionaria.util;

import java.util.regex.Pattern;

/** Validações de formato reutilizáveis (CPF, placa, e-mail...). */
public final class Validador {
    private static final Pattern PLACA = Pattern.compile("[A-Z]{3}[0-9][A-Z0-9][0-9]{2}");
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private Validador() {}

    public static String somenteDigitos(String texto) {
        return texto == null ? "" : texto.replaceAll("\\D", "");
    }

    public static boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }

    public static boolean cpfValido(String cpf) {
        String d = somenteDigitos(cpf);
        if (d.length() != 11 || d.chars().distinct().count() == 1) {
            return false;
        }
        return digitoCpf(d, 9) == d.charAt(9) - '0' && digitoCpf(d, 10) == d.charAt(10) - '0';
    }

    private static int digitoCpf(String d, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += (d.charAt(i) - '0') * (quantidade + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    /** Aceita placa antiga (ABC1234) e Mercosul (ABC1D23), sem hífen e em maiúsculas. */
    public static boolean placaValida(String placaNormalizada) {
        return placaNormalizada != null && PLACA.matcher(placaNormalizada).matches();
    }

    public static String normalizarPlaca(String placa) {
        return placa == null ? "" : placa.replaceAll("[\\s-]", "").toUpperCase();
    }

    public static boolean emailValido(String email) {
        return email != null && EMAIL.matcher(email.trim()).matches();
    }
}
