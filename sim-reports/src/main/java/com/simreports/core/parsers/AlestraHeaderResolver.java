package com.simreports.core.parsers;

import com.simreports.core.HeaderResolver;

import java.util.Map;
import java.util.Optional;

/**
 * {@link HeaderResolver} para el archivo de Alestra — ver spec 02,
 * sección "Alestra".
 *
 * <p>Los encabezados reales confirmados con el archivo real de Fase 0
 * son {@code LINEA} y {@code Tráfico MB} — no {@code "Consumo (MB)"}
 * como se documentaba genéricamente antes de tener el archivo real. Se
 * agrega también la variante sin acento como sinónimo defensivo, por si
 * algún mes el export llega sin el carácter acentuado.</p>
 */
public class AlestraHeaderResolver implements HeaderResolver {

    private static final Map<String, String> SINONIMOS = Map.of(
            "LINEA", "LINEA",
            "TRÁFICOMB", "CONSUMO",
            "TRAFICOMB", "CONSUMO"
    );

    @Override
    public Optional<String> resolver(String headerReal) {
        String normalizado = HeaderResolver.normalizar(headerReal);
        return Optional.ofNullable(SINONIMOS.get(normalizado));
    }
}
