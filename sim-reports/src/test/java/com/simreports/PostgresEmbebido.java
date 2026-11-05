package com.simreports;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Base de los tests que tocan la base de datos: levanta un PostgreSQL real
 * (binarios descargados como dependencia Maven, sin Docker) una sola vez
 * por ejecución de tests y apunta el datasource de Spring a él.
 *
 * <p>Se usa Postgres real en vez de H2 porque H2 ocultaba diferencias que
 * sí importan en producción: el mapeo de {@code @Lob String} a {@code oid},
 * y que H2 no soporta la restricción de exclusión parcial que garantiza una
 * sola plantilla activa. Así los tests ejecutan exactamente las mismas
 * migraciones de Flyway que producción.</p>
 */
public abstract class PostgresEmbebido {

    private static final EmbeddedPostgres POSTGRES = iniciar();

    private static EmbeddedPostgres iniciar() {
        try {
            EmbeddedPostgres postgres = EmbeddedPostgres.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    postgres.close();
                } catch (IOException ignorada) {
                    // La JVM ya está terminando; no hay nada más que hacer.
                }
            }));
            return postgres;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo iniciar PostgreSQL embebido para los tests", e);
        }
    }

    @DynamicPropertySource
    static void configurarDatasource(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registro.add("spring.datasource.username", () -> "postgres");
        registro.add("spring.datasource.password", () -> "postgres");
    }
}
