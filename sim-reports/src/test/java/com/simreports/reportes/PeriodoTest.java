package com.simreports.reportes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PeriodoTest {

    @Test
    void nombraElArchivoComoElUsuario() {
        assertThat(new Periodo(2026, 10).nombreArchivo()).isEqualTo("CONSUMOS_OCTUBRE_2026.xlsx");
        assertThat(new Periodo(2027, 1).nombreArchivo()).isEqualTo("CONSUMOS_ENERO_2027.xlsx");
        assertThat(new Periodo(2026, 12).nombreArchivo()).isEqualTo("CONSUMOS_DICIEMBRE_2026.xlsx");
    }

    @Test
    void rechazaMesesYAniosFueraDeRango() {
        assertThatThrownBy(() -> new Periodo(2026, 13)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Periodo(2026, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Periodo(26, 10)).isInstanceOf(IllegalArgumentException.class);
    }
}
