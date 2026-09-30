package com.demo.banking.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Caso "Transferencia duplicada": verifica con evidencia real que el bug sembrado
 * se manifiesta como falla (escenarios 2 y 3) antes de usar el caso en una demo.
 */
class PagoServiceTest {

    private PagoService pagoService;

    @BeforeEach
    void setUp() {
        pagoService = new PagoService();
        pagoService.abrirCuenta("cta-cliente", new BigDecimal("10000.00"));
    }

    // Escenario 1: un pago normal debita el monto una vez.
    @Test
    void pagoNormalDebitaUnaVez() {
        pagoService.pagar("clave-001", "cta-cliente", new BigDecimal("1500.00"));

        assertThat(pagoService.saldo("cta-cliente")).isEqualByComparingTo("8500.00");
    }

    // Escenario 2: la app reintenta el MISMO pago (misma clave) — no debe cobrar dos veces.
    // BUG SEMBRADO: hoy este test FALLA — el reintento debita de nuevo.
    @Test
    void reintentoConLaMismaClaveNoDebitaDosVeces_bugIdempotencia() {
        pagoService.pagar("clave-001", "cta-cliente", new BigDecimal("1500.00"));
        pagoService.pagar("clave-001", "cta-cliente", new BigDecimal("1500.00"));

        assertThat(pagoService.saldo("cta-cliente"))
                .as("un reintento con la misma clave de idempotencia no debería volver a debitar")
                .isEqualByComparingTo("8500.00");
    }

    // Escenario 3: el reintento devuelve el mismo comprobante que el pago original.
    // BUG SEMBRADO: hoy este test FALLA — genera un comprobante nuevo.
    @Test
    void reintentoDevuelveElMismoComprobante_bugIdempotencia() {
        var original = pagoService.pagar("clave-001", "cta-cliente", new BigDecimal("1500.00"));
        var reintento = pagoService.pagar("clave-001", "cta-cliente", new BigDecimal("1500.00"));

        assertThat(reintento.numero())
                .as("el reintento debería devolver el comprobante del pago original")
                .isEqualTo(original.numero());
    }

    // Escenario 4: dos pagos distintos (claves distintas) sí debitan los dos.
    @Test
    void pagosConClavesDistintasDebitanAmbos() {
        pagoService.pagar("clave-001", "cta-cliente", new BigDecimal("1500.00"));
        pagoService.pagar("clave-002", "cta-cliente", new BigDecimal("1500.00"));

        assertThat(pagoService.saldo("cta-cliente")).isEqualByComparingTo("7000.00");
    }
}
