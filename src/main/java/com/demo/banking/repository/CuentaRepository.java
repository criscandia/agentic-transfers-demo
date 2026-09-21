package com.demo.banking.repository;

import com.demo.banking.model.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CuentaRepository extends JpaRepository<Cuenta, String> {
}
