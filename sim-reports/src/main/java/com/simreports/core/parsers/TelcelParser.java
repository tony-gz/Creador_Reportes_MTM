package com.simreports.core.parsers;

import com.simreports.core.HeaderResolver;
import com.simreports.core.OrigenParser;
import com.simreports.core.ResultadoParseo;

import java.io.InputStream;
import java.util.Objects;

/**
 * Parser del archivo de Telcel — ver spec 02, sección "Telcel". Soporta
 * tanto {@code .csv} como {@code .xlsx} (confirmado con archivo real de
 * Fase 0: el de muestra llegó como {@code .csv}), detectando el formato
 * por contenido, nunca por el nombre del archivo.
 *
 * <p>Delega toda la lógica de búsqueda de encabezado, lectura de filas y
 * agregación en {@link ParserIccidConsumoGenerico}, compartida con el
 * parser de ATT (spec 02: "El parser [ATT] es idéntico al de Telcel
 * post-agregación").</p>
 */
public class TelcelParser implements OrigenParser {

    private final ParserIccidConsumoGenerico motor;

    public TelcelParser() {
        this(new TelcelHeaderResolver());
    }

    public TelcelParser(HeaderResolver headerResolver) {
        Objects.requireNonNull(headerResolver, "headerResolver no puede ser null");
        this.motor = new ParserIccidConsumoGenerico(headerResolver, "Telcel");
    }

    @Override
    public ResultadoParseo parsear(InputStream archivo) {
        return motor.parsear(archivo);
    }
}
