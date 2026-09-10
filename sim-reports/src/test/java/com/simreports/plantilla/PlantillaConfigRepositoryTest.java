package com.simreports.plantilla;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @Transactional aqui es el de Spring Test: cada @Test corre en su propia
 * transaccion, que se revierte automaticamente al terminar -- no hace
 * falta limpiar datos entre tests a mano.
 */
@SpringBootTest
@Transactional
class PlantillaConfigRepositoryTest {

    @Autowired
    private PlantillaConfigRepository repository;

    @Test
    void guardaYRecuperaUnaVersion() {
        PlantillaConfig config = new PlantillaConfig(1, "/data/plantillas/v1.xlsx", "{}", true);

        PlantillaConfig guardada = repository.save(config);

        assertThat(guardada.getId()).isNotNull();
        assertThat(guardada.getFechaCreacion()).isNotNull();
        assertThat(guardada.getVersion()).isEqualTo(1);
        assertThat(guardada.isActiva()).isTrue();
    }

    @Test
    void noPermiteDosVersionesConElMismoNumero() {
        repository.saveAndFlush(new PlantillaConfig(1, "/data/plantillas/v1.xlsx", "{}", false));

        assertThatThrownBy(() ->
                repository.saveAndFlush(new PlantillaConfig(1, "/data/plantillas/v1-otra.xlsx", "{}", true))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByActivaTrueDevuelveSoloLaFilaActiva() {
        repository.save(new PlantillaConfig(1, "/data/plantillas/v1.xlsx", "{}", false));
        PlantillaConfig activa = repository.save(new PlantillaConfig(2, "/data/plantillas/v2.xlsx", "{}", true));

        Optional<PlantillaConfig> encontrada = repository.findByActivaTrue();

        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getId()).isEqualTo(activa.getId());
    }

    @Test
    void findActivaParaActualizarDevuelveLaMismaFilaActiva() {
        repository.save(new PlantillaConfig(1, "/data/plantillas/v1.xlsx", "{}", true));

        Optional<PlantillaConfig> encontrada = repository.findActivaParaActualizar();

        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().isActiva()).isTrue();
    }

    @Test
    void findAllByOrderByVersionDescOrdenaDescendente() {
        repository.save(new PlantillaConfig(1, "/data/plantillas/v1.xlsx", "{}", false));
        repository.save(new PlantillaConfig(3, "/data/plantillas/v3.xlsx", "{}", true));
        repository.save(new PlantillaConfig(2, "/data/plantillas/v2.xlsx", "{}", false));

        List<PlantillaConfig> versiones = repository.findAllByOrderByVersionDesc();

        assertThat(versiones).extracting(PlantillaConfig::getVersion).containsExactly(3, 2, 1);
    }

    @Test
    void obtenerMaxVersionDevuelveCeroSiNoHayFilas() {
        assertThat(repository.obtenerMaxVersion()).isZero();
    }

    @Test
    void obtenerMaxVersionDevuelveElMayorNumeroDeVersion() {
        repository.save(new PlantillaConfig(1, "/data/plantillas/v1.xlsx", "{}", false));
        repository.save(new PlantillaConfig(5, "/data/plantillas/v5.xlsx", "{}", true));

        assertThat(repository.obtenerMaxVersion()).isEqualTo(5);
    }

    @Test
    void setMapeoColumnasActualizaSinCrearFilaNueva() {
        PlantillaConfig config = repository.save(new PlantillaConfig(1, "/data/plantillas/v1.xlsx", "{}", true));

        config.setMapeoColumnas("{\"ALSEA\":{}}");
        repository.saveAndFlush(config);

        PlantillaConfig releida = repository.findById(config.getId()).orElseThrow();
        assertThat(releida.getMapeoColumnas()).isEqualTo("{\"ALSEA\":{}}");
        assertThat(repository.count()).isEqualTo(1);
    }
}