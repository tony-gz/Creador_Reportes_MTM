package com.simreports.usuarios;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PoliticaPasswordTest {

    @Test
    void aceptaUnaContrasenaDeDiezCaracteresOMas() {
        assertThat(PoliticaPassword.problemas("diez-chars", "ana")).isEmpty();
        assertThat(PoliticaPassword.problemas("contraseña larga con acentos áéí", "ana")).isEmpty();
    }

    @Test
    void rechazaVaciaCortaIgualAlUsuarioODemasiadoLarga() {
        assertThat(PoliticaPassword.problemas("", "ana")).containsExactly("La contraseña no puede estar vacía.");
        assertThat(PoliticaPassword.problemas("nueve1234", "ana")).anyMatch(p -> p.contains("al menos 10"));
        assertThat(PoliticaPassword.problemas("Usuario.largo", "usuario.largo")).anyMatch(p -> p.contains("nombre de usuario"));
        assertThat(PoliticaPassword.problemas("ñ".repeat(40), "ana")).anyMatch(p -> p.contains("demasiado larga"));
    }
}
