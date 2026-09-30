package com.demo.banking.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Fixture de demo — caso "Transferencia duplicada".
 *
 * Cuando la app del cliente no recibe respuesta (se cortó la conexión, timeout),
 * reintenta el mismo pago con la MISMA clave de idempotencia. El servicio tiene
 * que reconocer que ese pago ya se procesó y devolver el mismo comprobante, sin
 * volver a debitar.
 *
 * BUG SEMBRADO: el servicio ignora la clave de idempotencia — cada reintento
 * debita de nuevo, así que un solo pago del cliente puede cobrarse dos veces.
 *
 * No corregir este bug a mano — es el fixture que van a encontrar y arreglar
 * los agentes (Arquitecto/Developer/QA) en la corrida real.
 */
@Service
public class PagoService {

    private final Map<String, BigDecimal> saldos = new ConcurrentHashMap<>();

    public void abrirCuenta(String cuentaId, BigDecimal saldoInicial) {
        saldos.put(cuentaId, saldoInicial);
    }

    public BigDecimal saldo(String cuentaId) {
        return saldos.get(cuentaId);
    }

    public Comprobante pagar(String claveIdempotencia, String cuentaId, BigDecimal monto) {
        BigDecimal saldoActual = saldos.get(cuentaId);
        if (saldoActual == null) {
            throw new IllegalArgumentException("Cuenta inexistente: " + cuentaId);
        }
        if (saldoActual.compareTo(monto) < 0) {
            throw new IllegalStateException("Saldo insuficiente en cuenta " + cuentaId);
        }

        // --- BUG SEMBRADO (idempotencia) ---
        // Falta acá: si esta claveIdempotencia ya se procesó, devolver el mismo
        // comprobante de esa vez y NO volver a debitar.
        saldos.put(cuentaId, saldoActual.subtract(monto));
        return new Comprobante(UUID.randomUUID().toString(), cuentaId, monto);
    }

    public record Comprobante(String numero, String cuentaId, BigDecimal monto) {
    }
}
