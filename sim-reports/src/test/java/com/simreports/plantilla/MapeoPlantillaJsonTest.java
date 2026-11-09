package com.simreports.plantilla;

import com.simreports.core.plantilla.MapeoPlantilla;
import com.simreports.core.plantilla.MapeoTabla;
import com.simreports.core.plantilla.TipoTablaPlantilla;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MapeoPlantillaJsonTest {

    private final MapeoPlantillaJson json = new MapeoPlantillaJson(JsonMapper.builder().build());

    private static final MapeoPlantilla MAPEO = new MapeoPlantilla(List.of(
            new MapeoTabla("PETCO ", "Tabla7", "B6:H7", TipoTablaPlantilla.SIMPLE_CASCADA,
                    Map.of("LINEA", "LINEA", "ICCID", "ICCID", "CONSUMO(MB)", "CONSUMO (MB)"), List.of("SERIE")),
            new MapeoTabla("BIMBO", "Tabla1", "A11:L15", TipoTablaPlantilla.DUAL_SIM,
                    Map.of("LINEA_1", "LINEA_1", "LINEA_2", "LINEA_2",
                            "CONSUMO(MB)_1", "CONSUMO(MB)_1", "CONSUMO(MB)_2", "CONSUMO(MB)_2", "SIM", "SIM"),
                    List.of())));

    @Test
    void idaYVueltaConservaElMapeo() {
        String texto = json.escribir(MAPEO);

        assertThat(texto).startsWith("{\"formato\":1,");
        assertThat(texto).doesNotContain("identificador");
        assertThat(json.leer(texto)).isEqualTo(MAPEO);
    }

    @Test
    void rechazaUnFormatoDesconocido() {
        assertThatThrownBy(() -> json.leer("{\"formato\":99,\"tablas\":[]}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("99");
    }
}
