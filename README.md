# Agentic Transfers Demo

Fixture pequeño de una app bancaria (Java 17 + Spring Boot + H2 en memoria) con **dos bugs reales sembrados a propósito**, pensado para mostrar a un equipo de agentes de IA — no guionados — encontrándolos y arreglándolos de punta a punta: diseño, código, tests, y un Pull Request real en GitHub.

## Qué hace la app

Dos casos de uso chicos sobre cuentas y transferencias:

- **Validar límite diario** — antes de ejecutar una transferencia, verifica que no se supere el límite diario configurado para la cuenta origen.
- **Ejecutar transferencia** — mueve fondos de una cuenta a otra.

## Los 2 bugs sembrados

1. **Condición de carrera en el límite diario.** El chequeo de "cuánto se transfirió hoy" y la escritura de la nueva transferencia no son atómicos — bajo transferencias concurrentes sobre la misma cuenta, varias pueden pasar el chequeo antes de que ninguna se persista, superando el límite diario real en conjunto.
2. **Control de acceso roto (BOLA — Broken Object Level Authorization) al ejecutar una transferencia.** El servicio no valida que la cuenta origen pertenezca al usuario autenticado que hace el pedido — cualquier usuario puede mover fondos de una cuenta ajena con solo conocer su ID.

Ninguno de los dos está corregido a propósito — son el fixture que un equipo de agentes debe encontrar y arreglar.

## Verificación de línea base

`TransferenciaServiceTest` corre 4 escenarios reales contra el código tal como está, sin ningún agente de por medio:

| Escenario | Resultado esperado hoy |
|---|---|
| Transferencia normal | ✅ pasa |
| Transferencia en el límite exacto | ✅ pasa |
| Transferencias concurrentes que en conjunto superan el límite | ❌ falla (bug 1) |
| Transferencia desde una cuenta ajena | ❌ falla (bug 2) |

```bash
./mvnw test
```

Los 2 fallos son la evidencia de que los bugs son reales, no supuestos — confirmado corriendo la suite de verdad, no solo leyendo el código.

## Stack

- Java 17, Spring Boot, Spring Data JPA
- H2 embebido en memoria (sin infraestructura externa)
- JUnit 5 + AssertJ

## Reglas del repo

- `main` está protegida: todo cambio entra por Pull Request, sin excepción para administradores.
- Los bugs sembrados no se corrigen a mano en `main` — el objetivo es que los encuentre y resuelva un flujo de agentes de IA, con evidencia real (tests) antes de abrir el PR.
