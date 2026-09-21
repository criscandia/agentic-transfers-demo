package com.demo.banking.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
public class Transferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cuentaOrigenId;

    private String cuentaDestinoId;

    private BigDecimal monto;

    private Instant fecha;

    protected Transferencia() {
    }

    public Transferencia(String cuentaOrigenId, String cuentaDestinoId, BigDecimal monto, Instant fecha) {
        this.cuentaOrigenId = cuentaOrigenId;
        this.cuentaDestinoId = cuentaDestinoId;
        this.monto = monto;
        this.fecha = fecha;
    }

    public Long getId() {
        return id;
    }

    public String getCuentaOrigenId() {
        return cuentaOrigenId;
    }

    public String getCuentaDestinoId() {
        return cuentaDestinoId;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public Instant getFecha() {
        return fecha;
    }
}
