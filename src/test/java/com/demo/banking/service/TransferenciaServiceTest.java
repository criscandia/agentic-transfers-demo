package com.demo.banking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.demo.banking.model.Cuenta;
import com.demo.banking.repository.CuentaRepository;
import com.demo.banking.repository.TransferenciaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Slice 0 — verificación de línea base: corre estos 4 escenarios contra el
 * código de fábrica (sin ningún agente de por medio) para confirmar con
 * evidencia real que los 2 bugs sembrados se manifiestan como falla, antes
 * de apoyarse en esta demo para cualquier presentación.
 *
 * A propósito, sin @Transactional a nivel clase/método: cada llamada a
 * ejecutarTransferencia() debe correr en su propia transacción real para
 * que la condición de carrera del escenario 3 pueda manifestarse de verdad
 * (si el test envolviera todo en una sola transacción, serializaría las
 * escrituras y taparía el bug).
 */
@SpringBootTest
class TransferenciaServiceTest {

    @Autowired
    private TransferenciaService transferenciaService;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private TransferenciaRepository transferenciaRepository;

    private static final String USUARIO_TITULAR = "user-cristian";
    private static final String USUARIO_ATACANTE = "user-otro";

    @BeforeEach
    void limpiarDatos() {
        transferenciaRepository.deleteAll();
        cuentaRepository.deleteAll();
    }

    private Cuenta crearCuenta(String id, String titular, BigDecimal saldo, BigDecimal limiteDiario) {
        return cuentaRepository.save(new Cuenta(id, titular, saldo, limiteDiario));
    }

    // Escenario 1: transferencia normal — debe ejecutarse sin problema.
    @Test
    void transferenciaNormalSeEjecutaOk() {
        crearCuenta("cta-origen", USUARIO_TITULAR, new BigDecimal("10000.00"), new BigDecimal("5000.00"));
        crearCuenta("cta-destino", "user-destino", new BigDecimal("0.00"), new BigDecimal("5000.00"));

        var transferencia = transferenciaService.ejecutarTransferencia(
                USUARIO_TITULAR, "cta-origen", "cta-destino", new BigDecimal("1000.00"));

        assertThat(transferencia.getId()).isNotNull();
        assertThat(cuentaRepository.findById("cta-origen").orElseThrow().getSaldo())
                .isEqualByComparingTo("9000.00");
    }

    // Escenario 2: transferencia justo en el límite exacto — debe ejecutarse ok (caso borde, no bug).
    @Test
    void transferenciaEnElLimiteExactoSeEjecutaOk() {
        crearCuenta("cta-origen", USUARIO_TITULAR, new BigDecimal("10000.00"), new BigDecimal("5000.00"));
        crearCuenta("cta-destino", "user-destino", new BigDecimal("0.00"), new BigDecimal("5000.00"));

        var transferencia = transferenciaService.ejecutarTransferencia(
                USUARIO_TITULAR, "cta-origen", "cta-destino", new BigDecimal("5000.00"));

        assertThat(transferencia.getId()).isNotNull();
    }

    // Escenario 3: transferencias CONCURRENTES sobre la misma cuenta — el total
    // transferido en el día nunca debería superar el límite diario real.
    // BUG SEMBRADO 1 (condición de carrera): hoy este test FALLA — el total
    // transferido termina superando el límite porque el chequeo y la escritura
    // no son atómicos.
    @Test
    void transferenciasConcurrentesNoDeberianSuperarElLimiteDiario_bugDeCarrera() throws InterruptedException {
        BigDecimal limiteDiario = new BigDecimal("1000.00");
        crearCuenta("cta-origen", USUARIO_TITULAR, new BigDecimal("100000.00"), limiteDiario);
        crearCuenta("cta-destino", "user-destino", new BigDecimal("0.00"), limiteDiario);

        int hilos = 10;
        BigDecimal montoPorTransferencia = new BigDecimal("200.00"); // 10 x 200 = 2000, el doble del límite (1000)
        ExecutorService executor = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        CountDownLatch listos = new CountDownLatch(hilos);
        AtomicInteger exitosas = new AtomicInteger();

        for (int i = 0; i < hilos; i++) {
            executor.submit(() -> {
                listos.countDown();
                try {
                    salida.await();
                    transferenciaService.ejecutarTransferencia(
                            USUARIO_TITULAR, "cta-origen", "cta-destino", montoPorTransferencia);
                    exitosas.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (TransferenciaService.LimiteDiarioExcedidoException e) {
                    // rechazo esperado para las que sí detecten el límite correctamente
                }
            });
        }

        listos.await(5, TimeUnit.SECONDS); // todos los hilos listos en la línea de largada
        salida.countDown(); // largan todos al mismo tiempo, a propósito
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        Instant inicioDelDia = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        BigDecimal totalTransferidoReal = transferenciaRepository.sumaTransferidaDesde("cta-origen", inicioDelDia);

        // Esta es la aserción real de negocio: el total transferido en el día
        // nunca debería superar el límite diario configurado, sin importar
        // cuántas transferencias concurrentes se hayan intentado.
        assertThat(totalTransferidoReal)
                .as("total transferido en el día (%s exitosas de %s intentos) no debería superar el límite %s",
                        exitosas.get(), hilos, limiteDiario)
                .isLessThanOrEqualTo(limiteDiario);
    }

    // Escenario 4: un usuario intenta transferir desde una cuenta que NO es suya.
    // BUG SEMBRADO 2 (BOLA): hoy este test FALLA — la transferencia se ejecuta
    // igual, sin rechazar al usuario que no es titular de la cuenta origen.
    @Test
    void noSePuedeTransferirDesdeUnaCuentaQueNoEsPropia_bugBola() {
        crearCuenta("cta-origen", USUARIO_TITULAR, new BigDecimal("10000.00"), new BigDecimal("5000.00"));
        crearCuenta("cta-destino", "user-destino", new BigDecimal("0.00"), new BigDecimal("5000.00"));

        assertThatThrownBy(() -> transferenciaService.ejecutarTransferencia(
                USUARIO_ATACANTE, "cta-origen", "cta-destino", new BigDecimal("500.00")))
                .as("un usuario distinto del titular de cta-origen no debería poder transferir desde ella")
                .isInstanceOf(SecurityException.class);
    }
}
