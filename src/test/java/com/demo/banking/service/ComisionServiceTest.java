package com.demo.banking.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Caso "Comisión mal calculada": verifica con evidencia real que el bug sembrado
 * se manifiesta como falla (escenarios 2 y 3) antes de usar el caso en una demo.
 */
class ComisionServiceTest {

    private final ComisionService comisionService = new ComisionService();

    // Escenario 1: monto redondo — 0,6% de 10.000 = 60.
    @Test
    void comisionDeMontoRedondo() {
        assertThat(comisionService.calcularComision(new BigDecimal("10000.00"))).isEqualByComparingTo("60.00");
    }

    // Escenario 2: el resultado tiene más de 2 decimales y hay que redondear al centavo.
    // 0,6% de 1.234,56 = 7,40736 → 7,41. BUG SEMBRADO: hoy este test FALLA (da 7,40).
    @Test
    void comisionSeRedondeaAlCentavo_bugPrecision() {
        assertThat(comisionService.calcularComision(new BigDecimal("1234.56")))
                .as("0,6% de 1.234,56 = 7,40736, redondeado al centavo debería ser 7,41")
                .isEqualByComparingTo("7.41");
    }

    // Escenario 3: la comisión siempre se expresa en centavos (2 decimales).
    // BUG SEMBRADO: hoy este test FALLA (devuelve 60.0, con 1 decimal).
    @Test
    void comisionSiempreTieneDosDecimales_bugPrecision() {
        assertThat(comisionService.calcularComision(new BigDecimal("10000.00")).scale())
                .as("la comisión debería expresarse en centavos (2 decimales)")
                .isEqualTo(2);
    }
}
