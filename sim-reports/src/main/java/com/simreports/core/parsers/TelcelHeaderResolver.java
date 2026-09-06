package com.simreports.core.parsers;

import com.simreports.core.HeaderResolver;

import java.util.Map;
import java.util.Optional;

/**
 * {@link HeaderResolver} para el archivo de Telcel — ver spec 02,
 * sección "Telcel". Columnas reales confirmadas con archivo de muestra:
 * {@code DURACIÓN (UTC), ICCID, MSISDN, IMSI, USO (KB), USO (MB), USO
 * (BYTES)}. El parser solo necesita {@code ICCID} y {@code USO (MB)};
 * el resto de columnas se ignoran (no se agregan como sinónimos porque
 * no hace falta reconocerlas).
 */
public class TelcelHeaderResolver implements HeaderResolver {

    private static final Map<String, String> SINONIMOS = Map.of(
            "ICCID", "ICCID",
            "USO(MB)", "CONSUMO"
    );

    @Override
    public Optional<String> resolver(String headerReal) {
        return Optional.ofNullable(SINONIMOS.get(HeaderResolver.normalizar(headerReal)));
    }
}
