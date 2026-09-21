package com.demo.banking.service;

import com.demo.banking.model.Cuenta;
import com.demo.banking.model.Transferencia;
import com.demo.banking.repository.CuentaRepository;
import com.demo.banking.repository.TransferenciaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Fixture de demo para un flujo de agentes de IA trabajando sobre un dominio
 * bancario chico.
 *
 * Contiene DOS bugs reales sembrados a propósito:
 *
 * 1. "Validar límite diario" — condición de carrera: el chequeo de saldo
 *    disponible del día y la escritura de la transferencia no son atómicos,
 *    así que transferencias concurrentes pueden pasar el chequeo antes de
 *    que ninguna se persista, superando el límite diario real en conjunto.
 *
 * 2. "Ejecutar transferencia" — BOLA (Broken Object Level Authorization):
 *    no valida que la cuenta origen pertenezca al usuario autenticado que
 *    hace el pedido.
 *
 * No corregir estos bugs a mano — son el fixture que van a encontrar y
 * arreglar los agentes (Arquitecto/Developer/QA) en la corrida real.
 */
@Service
public class TransferenciaService {

    private final CuentaRepository cuentaRepository;
    private final TransferenciaRepository transferenciaRepository;
    private final Map<String, Object> locksPorCuenta = new HashMap<>();

    public TransferenciaService(CuentaRepository cuentaRepository, TransferenciaRepository transferenciaRepository) {
        this.cuentaRepository = cuentaRepository;
        this.transferenciaRepository = transferenciaRepository;
    }

    private Object obtenerLockParaCuenta(String cuentaOrigenId) {
        synchronized (locksPorCuenta) {
            return locksPorCuenta.computeIfAbsent(cuentaOrigenId, k -> new Object());
        }
    }

    public Transferencia ejecutarTransferencia(String usuarioAutenticadoId, String cuentaOrigenId,
            String cuentaDestinoId, BigDecimal monto) {

        Cuenta cuentaOrigen = cuentaRepository.findById(cuentaOrigenId)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta origen inexistente: " + cuentaOrigenId));

        // --- CORRECCIÓN BUG 2 (BOLA) ---
        // Validar que la cuenta origen pertenezca al usuario autenticado
        if (!cuentaOrigen.getTitularUsuarioId().equals(usuarioAutenticadoId)) {
            throw new SecurityException("No estás autorizado para transferir desde la cuenta " + cuentaOrigenId);
        }

        cuentaRepository.findById(cuentaDestinoId)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta destino inexistente: " + cuentaDestinoId));

        Instant inicioDelDia = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();

        // --- CORRECCIÓN BUG 1 (condición de carrera) ---
        // Sincronizar el acceso a nivel de método usando un lock por cuenta origen
        Object lock = obtenerLockParaCuenta(cuentaOrigenId);
        synchronized (lock) {
            BigDecimal transferidoHoy = transferenciaRepository.sumaTransferidaDesde(cuentaOrigenId, inicioDelDia);
            BigDecimal totalConEstaTransferencia = transferidoHoy.add(monto);
            if (totalConEstaTransferencia.compareTo(cuentaOrigen.getLimiteDiario()) > 0) {
                throw new LimiteDiarioExcedidoException(cuentaOrigenId, cuentaOrigen.getLimiteDiario(),
                        totalConEstaTransferencia);
            }

            cuentaOrigen.setSaldo(cuentaOrigen.getSaldo().subtract(monto));
            cuentaRepository.save(cuentaOrigen);

            Transferencia transferencia = new Transferencia(cuentaOrigenId, cuentaDestinoId, monto, Instant.now());
            return transferenciaRepository.save(transferencia);
        }
    }

    public static class LimiteDiarioExcedidoException extends RuntimeException {
        public LimiteDiarioExcedidoException(String cuentaId, BigDecimal limite, BigDecimal totalIntentado) {
            super("Límite diario excedido para cuenta " + cuentaId + ": límite=" + limite
                    + " totalIntentado=" + totalIntentado);
        }
    }
}
