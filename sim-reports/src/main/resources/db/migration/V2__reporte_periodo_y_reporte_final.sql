-- Fase 2 punto 4 (spec 08): periodo del reporte (mes/año, para el nombre
-- CONSUMOS_<MES>_<AÑO>.xlsx), nombre del archivo generado y el texto del
-- reporte final de la corrida (ReporteFinalCorrida), que se muestra en la app
-- y no va dentro del .xlsx.
-- Los DEFAULT solo sirven para filas que ya existieran; se quitan enseguida.
ALTER TABLE reporte_generado
    ADD COLUMN periodo_anio   INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN periodo_mes    INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN nombre_archivo VARCHAR(255) NOT NULL DEFAULT '',
    ADD COLUMN reporte_final  TEXT         NOT NULL DEFAULT '';

ALTER TABLE reporte_generado
    ALTER COLUMN periodo_anio DROP DEFAULT,
    ALTER COLUMN periodo_mes DROP DEFAULT,
    ALTER COLUMN nombre_archivo DROP DEFAULT,
    ALTER COLUMN reporte_final DROP DEFAULT;

CREATE INDEX ix_reporte_generado_periodo ON reporte_generado (periodo_anio, periodo_mes);
