package com.simreports.almacenamiento;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Archivos de la app en el filesystem (spec 04): en Postgres solo se guarda la
 * ruta. Las rutas que devuelve son <b>relativas</b> a la carpeta base
 * ({@code simreports.almacenamiento.directorio}), para que mover la carpeta
 * completa no rompa las rutas guardadas.
 *
 * <p>Estructura: {@code plantillas/v<version>/plantilla.xlsx}.</p>
 *
 * <p>La escritura es atómica (archivo temporal + mover): nunca queda un
 * {@code .xlsx} a medias. Si la transacción que registra la versión falla
 * después de escribir, el archivo queda huérfano en {@code v<version>}; el
 * siguiente intento usa el mismo número de versión y lo reemplaza.</p>
 */
@Component
public class AlmacenamientoArchivos {

    private final Path base;

    public AlmacenamientoArchivos(@Value("${simreports.almacenamiento.directorio}") Path base) {
        this.base = Objects.requireNonNull(base, "base no puede ser null").toAbsolutePath().normalize();
    }

    /** Guarda el archivo de una versión de la plantilla y devuelve su ruta relativa. */
    public String guardarPlantilla(int version, byte[] contenido) {
        Objects.requireNonNull(contenido, "contenido no puede ser null");
        Path relativa = Path.of("plantillas", "v" + version, "plantilla.xlsx");
        escribirAtomico(base.resolve(relativa), contenido);
        return relativa.toString();
    }

    /** Lee un archivo por su ruta relativa (la que devolvió este componente). */
    public byte[] leer(String rutaRelativa) {
        try {
            return Files.readAllBytes(resolver(rutaRelativa));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo '" + rutaRelativa + "'", e);
        }
    }

    /** Ruta absoluta de una ruta relativa, sin permitir salir de la carpeta base. */
    public Path resolver(String rutaRelativa) {
        Path ruta = base.resolve(Objects.requireNonNull(rutaRelativa, "rutaRelativa no puede ser null")).normalize();
        if (!ruta.startsWith(base)) {
            throw new IllegalArgumentException("La ruta '" + rutaRelativa + "' sale de la carpeta de almacenamiento");
        }
        return ruta;
    }

    private static void escribirAtomico(Path destino, byte[] contenido) {
        try {
            Files.createDirectories(destino.getParent());
            Path temporal = Files.createTempFile(destino.getParent(), ".subiendo-", ".tmp");
            try {
                Files.write(temporal, contenido);
                Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(temporal);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el archivo en '" + destino + "'", e);
        }
    }
}
