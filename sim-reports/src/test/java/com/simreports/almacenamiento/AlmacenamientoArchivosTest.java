package com.simreports.almacenamiento;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlmacenamientoArchivosTest {

    @TempDir
    Path base;

    @Test
    void guardaLaPlantillaPorVersionConRutaRelativaYLaLee() throws IOException {
        AlmacenamientoArchivos almacenamiento = new AlmacenamientoArchivos(base);

        String ruta = almacenamiento.guardarPlantilla(3, new byte[]{1, 2, 3});

        assertThat(ruta).isEqualTo(Path.of("plantillas", "v3", "plantilla.xlsx").toString());
        assertThat(Files.readAllBytes(base.resolve(ruta))).containsExactly(1, 2, 3);
        assertThat(almacenamiento.leer(ruta)).containsExactly(1, 2, 3);
        // Sin temporales sobrantes.
        try (var archivos = Files.list(base.resolve("plantillas/v3"))) {
            assertThat(archivos).hasSize(1);
        }
    }

    @Test
    void reemplazaElArchivoSiLaVersionYaExistia() {
        // Caso: una transacción anterior falló después de escribir el archivo.
        AlmacenamientoArchivos almacenamiento = new AlmacenamientoArchivos(base);
        almacenamiento.guardarPlantilla(1, new byte[]{9});

        String ruta = almacenamiento.guardarPlantilla(1, new byte[]{7, 7});

        assertThat(almacenamiento.leer(ruta)).containsExactly(7, 7);
    }

    @Test
    void noPermiteRutasQueSalenDeLaCarpetaBase() {
        AlmacenamientoArchivos almacenamiento = new AlmacenamientoArchivos(base);

        assertThatThrownBy(() -> almacenamiento.leer("../fuera.xlsx"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
