package com.simreports.core.parsers;

import com.simreports.core.HeaderResolver;

import java.util.Map;
import java.util.Optional;

/**
 * {@link HeaderResolver} para el archivo de ATT — ver spec 02, sección
 * "ATT". Formato de entrada: un solo archivo ya consolidado por el
 * usuario, con exactamente 2 columnas: {@code ICCID, Consumo_MB}.
 */
public class AttHeaderResolver implements HeaderResolver {

    private static final Map<String, String> SINONIMOS = Map.of(
            "ICCID", "ICCID",
            "CONSUMO_MB", "CONSUMO"
    );

    @Override
    public Optional<String> resolver(String headerReal) {
        return Optional.ofNullable(SINONIMOS.get(HeaderResolver.normalizar(headerReal)));
    }
}
