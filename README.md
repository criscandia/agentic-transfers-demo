# Agentic Transfers Demo

Fixture pequeño de una app bancaria (Java 17 + Spring Boot + H2 en memoria) con **bugs reales sembrados a propósito**, pensado para mostrar a un equipo de agentes de IA —no guionados— encontrándolos y arreglándolos de punta a punta: diseño, código, tests y un Pull Request real en GitHub.

Incluye **3 casos independientes**. Cada corrida de la demo toma uno, así dos corridas seguidas muestran escenarios distintos.

## Los 3 casos

### 1. Límite diario y control de acceso — `TransferenciaService`

Antes de ejecutar una transferencia, el servicio verifica que no se supere el límite diario de la cuenta origen y mueve los fondos. Tiene dos bugs:

- **Condición de carrera en el límite diario.** El chequeo de "cuánto se transfirió hoy" y la escritura de la nueva transferencia no son atómicos. Con transferencias simultáneas sobre la misma cuenta, varias pasan el chequeo antes de que ninguna se guarde y, en conjunto, superan el límite.
- **Control de acceso roto (BOLA, *Broken Object Level Authorization*).** No valida que la cuenta origen sea del usuario que hace el pedido: cualquiera puede mover fondos de una cuenta ajena con solo conocer su ID.

### 2. Transferencia duplicada — `PagoService`

Cuando la app del cliente no recibe respuesta (timeout, conexión cortada), reintenta el mismo pago con la **misma clave de idempotencia**. El servicio debería reconocer que ese pago ya se hizo y devolver el mismo comprobante.

- **Bug:** ignora la clave de idempotencia. Cada reintento vuelve a debitar, así que un solo pago se cobra dos veces.

### 3. Comisión mal calculada — `ComisionService`

Cada transferencia paga una comisión del 0,6% del monto, redondeada al centavo.

- **Bug:** el cálculo usa `double` y corta los decimales en vez de redondear. Resultado: comisiones que pierden centavos (7,40 en lugar de 7,41) y montos que no quedan expresados en centavos. Repetido en millones de operaciones, descuadra la contabilidad.

Ningún bug está corregido: son el fixture que un equipo de agentes tiene que encontrar y arreglar.

## Verificación de línea base

Cada caso tiene su clase de tests, que corre escenarios reales contra el código tal como está, sin ningún agente de por medio. Los fallos son la evidencia de que los bugs son reales, no supuestos.

| Caso | Clase de tests | Hoy |
|---|---|---|
| 1. Límite diario y acceso | `TransferenciaServiceTest` | 2 de 4 fallan (carrera y acceso) |
| 2. Transferencia duplicada | `PagoServiceTest` | 2 de 4 fallan (reintento debita de nuevo y cambia el comprobante) |
| 3. Comisión mal calculada | `ComisionServiceTest` | 2 de 3 fallan (redondeo y centavos) |

Cada caso se corre por separado, porque los otros dos siguen con sus bugs:

```bash
./mvnw test -Dtest=TransferenciaServiceTest
./mvnw test -Dtest=PagoServiceTest
./mvnw test -Dtest=ComisionServiceTest
```

Cada caso se puede resolver modificando **un solo archivo** (el servicio): verificado con un arreglo de referencia que deja su suite en verde y que después se revirtió.

## Stack

- Java 17, Spring Boot, Spring Data JPA
- H2 embebido en memoria (sin infraestructura externa)
- JUnit 5 + AssertJ

## Reglas del repo

- `main` está protegida: todo cambio entra por Pull Request, sin excepción para administradores.
- Los bugs sembrados no se corrigen a mano en `main`. Los resuelve un flujo de agentes de IA, con evidencia real (tests) antes de abrir el PR, en una rama nueva por corrida. Esos PR no se mergean, así los casos se pueden repetir.
