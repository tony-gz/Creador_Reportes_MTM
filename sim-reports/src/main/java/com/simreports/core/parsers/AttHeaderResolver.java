package com.simreports.core.parsers;

import com.simreports.core.HeaderResolver;

import java.util.Map;
import java.util.Optional;

/**
 * {@link HeaderResolver} para el archivo de ATT — ver spec 02, sección
 * "ATT". Formato de entrada: un solo archivo ya consolidado por el
 * usuario con columnas ICCID + consumo (uno o varios bloques lado a lado).
 *
 * <p>Encabezado de consumo: {@code Uso de ciclo hasta la fecha (MB)} es el
 * nombre real de la columna en los exports de Jasper (confirmado con la
 * hoja BD_ATT del reporte de octubre 2026); {@code Consumo_MB} se conserva
 * por compatibilidad con el formato documentado originalmente.</p>
 */
public class AttHeaderResolver implements HeaderResolver {

    private static final Map<String, String> SINONIMOS = Map.of(
            "ICCID", "ICCID",
            "CONSUMO_MB", "CONSUMO",
            "USODECICLOHASTALAFECHA(MB)", "CONSUMO"
    );

    @Override
    public Optional<String> resolver(String headerReal) {
        return Optional.ofNullable(SINONIMOS.get(HeaderResolver.normalizar(headerReal)));
    }
}
