package com.simreports.core.parsers;

import com.simreports.core.HeaderResolver;
import com.simreports.core.OrigenParser;
import com.simreports.core.ResultadoParseo;

import java.io.InputStream;
import java.util.Objects;

/**
 * Parser del archivo de ATT — ver spec 02, sección "ATT": un solo
 * archivo, ya consolidado por el usuario a partir de varios exports de
 * Jasper, con exactamente 2 columnas ({@code ICCID, Consumo_MB}).
 *
 * <p>Idéntico al de Telcel post-agregación (spec 02) — no necesita
 * lógica de agregación propia, por eso delega en el mismo {@link
 * ParserIccidConsumoGenerico}, solo con su propio {@link
 * AttHeaderResolver}.</p>
 */
public class AttParser implements OrigenParser {

    private final ParserIccidConsumoGenerico motor;

    public AttParser() {
        this(new AttHeaderResolver());
    }

    public AttParser(HeaderResolver headerResolver) {
        Objects.requireNonNull(headerResolver, "headerResolver no puede ser null");
        this.motor = new ParserIccidConsumoGenerico(headerResolver, "ATT");
    }

    @Override
    public ResultadoParseo parsear(InputStream archivo) {
        return motor.parsear(archivo);
    }
}
