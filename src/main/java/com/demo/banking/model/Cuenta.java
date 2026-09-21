package com.demo.banking.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;

@Entity
public class Cuenta {

    @Id
    private String id;

    private String titularUsuarioId;

    private BigDecimal saldo;

    private BigDecimal limiteDiario;

    protected Cuenta() {
    }

    public Cuenta(String id, String titularUsuarioId, BigDecimal saldo, BigDecimal limiteDiario) {
        this.id = id;
        this.titularUsuarioId = titularUsuarioId;
        this.saldo = saldo;
        this.limiteDiario = limiteDiario;
    }

    public String getId() {
        return id;
    }

    public String getTitularUsuarioId() {
        return titularUsuarioId;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }

    public BigDecimal getLimiteDiario() {
        return limiteDiario;
    }
}
