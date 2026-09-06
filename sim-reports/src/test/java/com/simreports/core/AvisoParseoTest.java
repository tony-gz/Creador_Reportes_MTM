package com.simreports.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AvisoParseoTest {

    @Test
    void guardaTipoClaveYDetalle() {
        AvisoParseo aviso = new AvisoParseo(
                TipoAviso.CLAVE_VACIA_O_MALFORMADA,
                "",
                "Fila 14: ICCID vacío, se excluyó de la suma.");

        assertEquals(TipoAviso.CLAVE_VACIA_O_MALFORMADA, aviso.tipo());
        assertEquals("", aviso.clave());
        assertEquals("Fila 14: ICCID vacío, se excluyó de la suma.", aviso.detalle());
    }

    @Test
    void ningunCampoPuedeSerNull() {
        assertThrows(NullPointerException.class,
                () -> new AvisoParseo(null, "algo", "detalle"));
        assertThrows(NullPointerException.class,
                () -> new AvisoParseo(TipoAviso.CLAVE_DUPLICADA, null, "detalle"));
        assertThrows(NullPointerException.class,
                () -> new AvisoParseo(TipoAviso.CLAVE_DUPLICADA, "algo", null));
    }
}
