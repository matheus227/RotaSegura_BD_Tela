package org.concessionaria.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Resultado do cálculo de fechamento de uma locação. */
public record ResumoFechamento(
        LocalDate dataDevolucao,
        int diasPrevistos,
        int diasUtilizados,
        int diasAtraso,
        BigDecimal valorBase,
        BigDecimal multa,
        BigDecimal total) {
}
