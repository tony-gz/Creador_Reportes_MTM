package com.simreports.usuarios;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reglas de contraseña — D-026 (spec 12). Mínimo 10 caracteres; máximo 72
 * bytes, el límite de BCrypt (lo que pase de ahí se ignoraría).
 */
public final class PoliticaPassword {

    public static final int MINIMO = 10;
    public static final int MAXIMO_BYTES = 72;

    private PoliticaPassword() {
    }

    /** Problemas de la contraseña; vacía si es válida. */
    public static List<String> problemas(String password, String nombreUsuario) {
        List<String> problemas = new ArrayList<>();
        if (password == null || password.isBlank()) {
            problemas.add("La contraseña no puede estar vacía.");
            return problemas;
        }
        if (password.codePointCount(0, password.length()) < MINIMO) {
            problemas.add("La contraseña debe tener al menos " + MINIMO + " caracteres.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES) {
            problemas.add("La contraseña es demasiado larga (máximo " + MAXIMO_BYTES + " bytes; unos 70 caracteres).");
        }
        if (nombreUsuario != null && password.strip().equalsIgnoreCase(nombreUsuario.strip())) {
            problemas.add("La contraseña no puede ser igual al nombre de usuario.");
        }
        return problemas;
    }
}
