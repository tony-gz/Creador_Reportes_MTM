package com.simreports.plantilla;

import com.simreports.PostgresEmbebido;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
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
class PlantillaConfigRepositoryTest extends PostgresEmbebido {

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

    // ---------- Restricciones que solo existen en PostgreSQL real ----------

    @Autowired
    private EntityManager entityManager;

    /** Fuerza a comprobar ya las restricciones diferidas (normalmente se comprueban al commit). */
    private void comprobarRestriccionesDiferidas() {
        entityManager.flush();
        entityManager.createNativeQuery("SET CONSTRAINTS ALL IMMEDIATE").executeUpdate();
    }

    @Test
    void laBaseDeDatosNoPermiteDosVersionesActivas() {
        repository.save(new PlantillaConfig(1, "/data/plantillas/v1.xlsx", "{}", true));
        repository.save(new PlantillaConfig(2, "/data/plantillas/v2.xlsx", "{}", true));

        assertThatThrownBy(this::comprobarRestriccionesDiferidas)
                .isInstanceOf(PersistenceException.class)
                .hasStackTraceContaining("ex_plantilla_config_una_activa");
    }

    @Test
    void permiteReemplazarLaVersionActivaEnUnaMismaTransaccionSinImportarElOrden() {
        // Mismo orden en que Hibernate emitiría las sentencias al reemplazar
        // la plantilla: primero el INSERT de la nueva (activa) y después el
        // UPDATE de la anterior. Con la restricción diferida, el estado
        // intermedio con dos activas no falla; solo cuenta el final.
        PlantillaConfig v1 = repository.saveAndFlush(new PlantillaConfig(1, "/data/plantillas/v1.xlsx", "{}", true));
        repository.saveAndFlush(new PlantillaConfig(2, "/data/plantillas/v2.xlsx", "{}", true));
        v1.setActiva(false);

        comprobarRestriccionesDiferidas();

        assertThat(repository.findByActivaTrue()).get().extracting(PlantillaConfig::getVersion).isEqualTo(2);
    }

    @Test
    void elMapeoSeGuardaEnUnaColumnaTextYAdmiteJsonGrande() {
        String tipo = (String) entityManager.createNativeQuery(
                "select data_type from information_schema.columns "
                        + "where table_name = 'plantilla_config' and column_name = 'mapeo_columnas'")
                .getSingleResult();
        assertThat(tipo).isEqualTo("text");

        String jsonGrande = "{\"x\":\"" + "a".repeat(2_000_000) + "\"}";
        PlantillaConfig config = repository.saveAndFlush(
                new PlantillaConfig(1, "/data/plantillas/v1.xlsx", jsonGrande, true));
        entityManager.clear();

        assertThat(repository.findById(config.getId()).orElseThrow().getMapeoColumnas()).isEqualTo(jsonGrande);
    }
}
