package com.simreports;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Reloj que los tests pueden adelantar (bloqueos de 15 minutos, etc.). */
public class RelojDePrueba extends Clock {

    private volatile Instant ahora = Instant.now();

    public void adelantar(Duration d) {
        ahora = ahora.plus(d);
    }

    public void reiniciar() {
        ahora = Instant.now();
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return ahora;
    }
}
