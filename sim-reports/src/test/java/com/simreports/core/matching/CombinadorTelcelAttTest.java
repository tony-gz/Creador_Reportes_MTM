package com.simreports.core.matching;

import com.simreports.core.AvisoParseo;
import com.simreports.core.ResultadoParseo;
import com.simreports.core.TipoAviso;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CombinadorTelcelAttTest {

    @Test
    void combinaClavesDistintasSinAvisos() {
        ResultadoParseo telcel = new ResultadoParseo(Map.of("111", 10.0, "222", 20.0), List.of());
        ResultadoParseo att = new ResultadoParseo(Map.of("333", 30.0), List.of());

        ResultadoParseo combinado = CombinadorTelcelAtt.combinar(telcel, att);

        assertEquals(Map.of("111", 10.0, "222", 20.0, "333", 30.0), combinado.consumos());
        assertTrue(combinado.avisos().isEmpty());
    }

    @Test
    void sumaConsumoCuandoElMismoIccidApareceEnAmbasFuentesYRegistraAviso() {
        ResultadoParseo telcel = new ResultadoParseo(Map.of("111", 10.0), List.of());
        ResultadoParseo att = new ResultadoParseo(Map.of("111", 5.0), List.of());

        ResultadoParseo combinado = CombinadorTelcelAtt.combinar(telcel, att);

        assertEquals(15.0, combinado.consumos().get("111"));
        assertEquals(1, combinado.avisos().size());
        AvisoParseo aviso = combinado.avisos().get(0);
        assertEquals(TipoAviso.CLAVE_DUPLICADA, aviso.tipo());
        assertEquals("111", aviso.clave());
    }

    @Test
    void preservaAvisosOriginalesDeAmbasFuentes() {
        AvisoParseo avisoTelcel = new AvisoParseo(TipoAviso.CLAVE_VACIA_O_MALFORMADA, "", "iccid vacio en telcel");
        AvisoParseo avisoAtt = new AvisoParseo(TipoAviso.CLAVE_VACIA_O_MALFORMADA, "", "iccid vacio en att");
        ResultadoParseo telcel = new ResultadoParseo(Map.of("111", 10.0), List.of(avisoTelcel));
        ResultadoParseo att = new ResultadoParseo(Map.of("222", 20.0), List.of(avisoAtt));

        ResultadoParseo combinado = CombinadorTelcelAtt.combinar(telcel, att);

        assertEquals(2, combinado.avisos().size());
        assertTrue(combinado.avisos().contains(avisoTelcel));
        assertTrue(combinado.avisos().contains(avisoAtt));
    }
}