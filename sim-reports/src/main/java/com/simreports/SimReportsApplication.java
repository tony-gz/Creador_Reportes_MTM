package com.simreports;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la capa Spring Boot (Fase 2, spec 04). El motor de
 * parsing puro (com.simreports.core.*, Fase 1) no depende de esta clase
 * ni de Spring en absoluto -- sigue siendo testeable con JUnit puro.
 */
@SpringBootApplication
public class SimReportsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimReportsApplication.class, args);
    }
}