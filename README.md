# Creador_Reportes_MTM

App para automatizar los reportes mensuales de consumo de datos SIM de MTM.
`sim-reports/` es un proyecto Maven/Java 21 (Spring Boot en Fase 2) que:

1. Lee las bases de datos de consumo de los operadores (Alestra, Telcel y ATT).
2. Resuelve las tablas de la plantilla de 18 hojas y hace el match de líneas/ICCID.
3. Escribe el reporte mensual (.xlsx) y permite consultarlo vía API/Web.

## Estado

- **Fase 1 — Motor de parsing puro**: completa (checklist spec 06, 8/8, 113 tests verdes).
  Solo JVM + Apache POI + JUnit 5, sin Spring.
- **Fase 2 — Capa Spring Boot (REST + almacenamiento)**: pendiente (spec 04).
- **Fase 3 — Web UI**: pendiente.

## Cómo compilar y probar

```bash
cd sim-reports
mvn test      # 113 tests, 0 fallos
```

## Documentación

Los specs `00-*` a `06-*` describen visión, mapeo de columnas, parsers,
motor de coincidencia, modelo de datos/API y el flujo de trabajo usado.
`07-api-fase1.md` es el contrato público de la Fase 1 para la Fase 2.