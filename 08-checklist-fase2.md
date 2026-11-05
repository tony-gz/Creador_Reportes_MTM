# 08 — Checklist de progreso (Fase 2: capa Spring Boot)

> Mismo formato que 06-checklist-fase1.md. Se actualiza al cerrar cada
> punto; regla de bookkeeping de spec 06 aplica igual aqui (opencode
> puede marcar [x] con fecha y resultado de `mvn test` una vez que pase,
> sin esperar a que Claude dicte el texto).

- [x] 1. Entidades JPA `PlantillaConfig` y `ReporteGenerado` + repositorios
      Spring Data (`PlantillaConfigRepository`, `ReporteGeneradoRepository`).
      **Entregado:** 2026-09-06. **`mvn test`: 123/123 OK** (113 Fase 1 + 10
      en verde: 8 de Fase 2).
- [ ] 2. Servicio de plantilla: lectura de tablas nativas con POI
      (`XSSFSheet.getTables()`) -> `ResolverTablaPlantilla` -> persistir
      `mapeoColumnas` en la version activa (cableado pendiente segun spec 07).
- [ ] 3. Endpoints de plantilla (`POST /api/plantilla`, `GET`/`PUT
      /api/plantilla/mapeo`, `GET /api/plantilla/versiones`) + lock
      `SELECT ... FOR UPDATE` sobre la fila activa.
- [ ] 4. Servicio de reportes: orquesta los 3 parsers + `MotorCoincidencia`,
      escribe el .xlsx de salida, guarda `ReporteGenerado`.
- [ ] 5. Endpoints de reportes (`POST /api/reportes`, `GET
      /api/reportes/{id}/descargar`, `GET /api/reportes`).
- [ ] 6. `@ControllerAdvice` global: traduce `ArchivoFormatoInvalidoException`,
      `HojaSinTablaException`, `TablaPlantillaNoReconocidaException` a HTTP 400.
- [ ] 7. Config de datasource (perfiles local/servidor) + almacenamiento de
      archivos en filesystem.

## Notas de esta entrega (punto 1)
- Spring Boot **4.1.1** como parent (la `4.1.2` original no existe en
  Maven Central, HTTP 404; verificada la última 4.1.x estable). `mvn test`
  en verde con 123 tests.
- Tests con `@SpringBootTest` + `@Transactional` (no `@DataJpaTest`):
  Boot 4 reorganizo paquetes de varias anotaciones de test slice
  (`@EntityScan`, `@WebMvcTest`), y no se pudo confirmar si `@DataJpaTest`
  tambien cambio de paquete. Se prefirio la anotacion nucleo, mas lenta
  pero sin riesgo de import roto que opencode no pueda arreglar (spec 05).
  Revisar si vale la pena volver a `@DataJpaTest` cuando lleguen los
  controllers (punto 3).
- `spring.jpa.hibernate.ddl-auto=update` es temporal (sin Flyway/Liquibase
  todavia) -- revisar antes de Fase 4.
- `mapeoColumnas`, `archivosEntrada`, `lineasNoEncontradas` y `avisos` se
  guardan como `@Lob` de texto plano (JSON serializado por el servicio,
  no por la entidad) -- no jsonb nativo de Postgres, porque no se
  consulta dentro del JSON, solo se lee/escribe entero.
## Correcciones al motor de Fase 1 (2026-11-05, antes del punto 2)
Detectadas en una revisión de código y reproducidas con casos concretos.
Cada una tiene su test de regresión; los 11 tests nuevos fallan contra el
código anterior y pasan con el corregido.
- **Cascada + línea repetida** (`MotorCoincidencia.resolverCascada`): si la
  línea existía en Alestra pero estaba repetida en la plantilla, la
  `ReglaAmbigua` default devolvía una asignación sin fuente y la cascada
  caía a buscar el ICCID en Telcel+ATT, con lo que podía asignar el consumo
  de otro operador. Ahora se cae a ICCID solo si la línea **no existe** en
  Alestra.
- **Fila de totales en Alestra** (`AlestraParser`): se leía como si fuera
  una línea llamada "Total" con el total completo. Ahora se descuenta
  `getTotalsRowCount()`.
- **`.xlsx` de Telcel/ATT** (`ParserIccidConsumoGenerico`): el consumo se
  leía con `DataFormatter` (valor mostrado, redondeado por el formato de
  celda; las fórmulas devolvían su texto). Ahora se usa el valor numérico
  real. Un ICCID guardado como número se excluye con aviso (antes entraba
  como `8.95202E+18`).
- **`.csv` de Telcel/ATT**: lector RFC 4180 en vez de `split(",")`, con
  soporte de comillas dobles y descarte del BOM UTF-8 (antes un CSV de ATT
  guardado como "UTF-8 con BOM" fallaba con `ArchivoFormatoInvalidoException`).
- **`HeaderResolver.normalizar`** y la limpieza de valores ahora eliminan
  NBSP, BOM y espacios de ancho cero.

Tests del motor (`com.simreports.core`): 124/124 en verde (113 + 11
nuevos). Esta vez no se corrió `mvn test`: no había Maven en la máquina y
se usó un runner mínimo por reflexión. Con `mvn test` se esperan 134 tests
(123 + 11).
