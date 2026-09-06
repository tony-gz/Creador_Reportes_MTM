package com.simreports.core.parsers;

import com.simreports.core.ArchivoFormatoInvalidoException;
import com.simreports.core.ResultadoParseo;
import com.simreports.core.TipoAviso;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttParserTest {

    private final AttParser parser = new AttParser();

    @Test
    void parseaArchivoYaConsolidadoDeDosColumnas() {
        // Spec 02: archivo ya consolidado por el usuario, sin filas de
        // metadata, exactamente ICCID + Consumo_MB.
        String csv = String.join("\n",
                "ICCID,Consumo_MB",
                "8952030000000001,10.5",
                "8952030000000002,5.25",
                ""
        );

        InputStream archivo = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        ResultadoParseo resultado = parser.parsear(archivo);

        assertEquals(2, resultado.consumos().size());
        assertEquals(10.5, resultado.consumos().get("8952030000000001"));
        assertEquals(5.25, resultado.consumos().get("8952030000000002"));
        assertTrue(resultado.avisos().isEmpty());
    }

    @Test
    void excluyeFilaConIccidVacioYLoReportaComoAviso() {
        String csv = String.join("\n",
                "ICCID,Consumo_MB",
                "8952030000000001,10.5",
                ",99.0",
                ""
        );

        InputStream archivo = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        ResultadoParseo resultado = parser.parsear(archivo);

        assertEquals(1, resultado.consumos().size());
        assertTrue(resultado.avisos().stream().anyMatch(a -> a.tipo() == TipoAviso.CLAVE_VACIA_O_MALFORMADA));
    }

    @Test
    void fallaSiNoTieneLasColumnasObligatorias() {
        String csv = String.join("\n",
                "ICCID,OTRA_COLUMNA",
                "8952030000000001,algo",
                ""
        );

        InputStream archivo = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        assertThrows(ArchivoFormatoInvalidoException.class, () -> parser.parsear(archivo));
    }
}
