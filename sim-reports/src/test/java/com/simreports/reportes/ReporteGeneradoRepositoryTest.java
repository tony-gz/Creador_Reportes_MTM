package com.simreports.reportes;

import com.simreports.plantilla.PlantillaConfig;
import com.simreports.plantilla.PlantillaConfigRepository;
import com.simreports.PostgresEmbebido;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ReporteGeneradoRepositoryTest extends PostgresEmbebido {

    @Autowired
    private ReporteGeneradoRepository reporteRepository;

    @Autowired
    private PlantillaConfigRepository plantillaRepository;

    @Test
    void guardaYRecuperaUnReporteConSuVersionDePlantilla() {
        PlantillaConfig plantilla = plantillaRepository.save(
                new PlantillaConfig(1, "/data/plantillas/v1.xlsx", "{}", true)
        );

        ReporteGenerado reporte = new ReporteGenerado(
                plantilla,
                "{\"alestra\":\"a.xlsx\",\"telcel\":\"t.csv\",\"att\":\"att.xlsx\"}",
                "/data/reportes/2026-09.xlsx",
                "[]",
                "[]"
        );

        ReporteGenerado guardado = reporteRepository.save(reporte);

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getFecha()).isNotNull();
        assertThat(guardado.getPlantillaVersionId()).isEqualTo(plantilla.getId());
    }

    @Test
    void findAllByOrderByFechaDescDevuelveElHistorial() {
        PlantillaConfig plantilla = plantillaRepository.save(
                new PlantillaConfig(1, "/data/plantillas/v1.xlsx", "{}", true)
        );

        reporteRepository.save(new ReporteGenerado(plantilla, "{}", "/data/reportes/r1.xlsx", "[]", "[]"));
        reporteRepository.save(new ReporteGenerado(plantilla, "{}", "/data/reportes/r2.xlsx", "[]", "[]"));

        List<ReporteGenerado> historial = reporteRepository.findAllByOrderByFechaDesc();

        assertThat(historial).hasSize(2);
    }
}