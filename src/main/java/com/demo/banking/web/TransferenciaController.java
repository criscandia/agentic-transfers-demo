package com.demo.banking.web;

import com.demo.banking.model.Transferencia;
import com.demo.banking.service.TransferenciaService;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simula el usuario autenticado con el header X-Usuario-Id — no hay
 * infraestructura de auth real en este fixture de demo (fuera de alcance).
 */
@RestController
public class TransferenciaController {

    private final TransferenciaService transferenciaService;

    public TransferenciaController(TransferenciaService transferenciaService) {
        this.transferenciaService = transferenciaService;
    }

    public record TransferenciaRequest(String cuentaOrigenId, String cuentaDestinoId, BigDecimal monto) {
    }

    @PostMapping("/transferencias")
    @ResponseStatus(HttpStatus.CREATED)
    public Transferencia transferir(@RequestHeader("X-Usuario-Id") String usuarioAutenticadoId,
            @RequestBody TransferenciaRequest request) {
        return transferenciaService.ejecutarTransferencia(usuarioAutenticadoId, request.cuentaOrigenId(),
                request.cuentaDestinoId(), request.monto());
    }
}
