package com.demo.banking.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

/**
 * Fixture de demo — caso "Comisión mal calculada".
 *
 * Cada transferencia paga una comisión del 0,6% del monto, redondeada al
 * centavo (2 decimales, redondeo comercial: 0,5 o más sube).
 *
 * BUG SEMBRADO: el cálculo usa double y corta los decimales en vez de
 * redondear. Resultado: comisiones que pierden centavos (7,40 en lugar de
 * 7,41) y montos que no quedan expresados en centavos. En un banco, esa
 * diferencia repetida en millones de operaciones descuadra la contabilidad.
 *
 * No corregir este bug a mano — es el fixture que van a encontrar y arreglar
 * los agentes (Arquitecto/Developer/QA) en la corrida real.
 */
@Service
public class ComisionService {

    private static final BigDecimal TASA_COMISION = new BigDecimal("0.006");

    public BigDecimal calcularComision(BigDecimal monto) {
        BigDecimal comision = monto.multiply(TASA_COMISION)
                .divide(BigDecimal.ONE, 2, RoundingMode.HALF_UP);
        return comision.setScale(2, RoundingMode.HALF_UP);
    }
}
