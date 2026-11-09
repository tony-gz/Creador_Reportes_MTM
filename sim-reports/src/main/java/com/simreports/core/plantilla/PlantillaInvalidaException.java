package com.simreports.core.plantilla;

import java.util.List;
import java.util.Objects;

/**
 * La plantilla no se puede usar. Lleva <b>todos</b> los problemas
 * encontrados, no solo el primero, para que el usuario los corrija de una vez
 * (p. ej. varias hojas sin tabla nativa). Cada problema nombra la hoja y, si
 * aplica, la tabla, la fila o la columna.
 *
 * <p>Casos: hoja de clientes sin ninguna tabla nativa; fila de encabezados
 * fuera de las tablas nativas (spec 09, ALSEA bloque 2); tabla que no se puede
 * clasificar; ICCID guardado como número; y, al leer con un mapeo guardado,
 * una tabla o columna que ya no existe.</p>
 */
public class PlantillaInvalidaException extends RuntimeException {

    private final List<String> problemas;

    public PlantillaInvalidaException(List<String> problemas) {
        super(mensaje(problemas));
        this.problemas = List.copyOf(problemas);
    }

    public List<String> getProblemas() {
        return problemas;
    }

    private static String mensaje(List<String> problemas) {
        Objects.requireNonNull(problemas, "problemas no puede ser null");
        if (problemas.isEmpty()) {
            throw new IllegalArgumentException("una PlantillaInvalidaException necesita al menos un problema");
        }
        StringBuilder sb = new StringBuilder("La plantilla tiene ").append(problemas.size())
                .append(problemas.size() == 1 ? " problema:" : " problemas:");
        for (String p : problemas) {
            sb.append("\n- ").append(p);
        }
        return sb.toString();
    }
}
