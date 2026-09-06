package com.simreports.core.matching;

/**
 * Implementación default de {@link ReglaAmbigua} — spec 03: "la primera
 * coincidencia gana y el resto se marca como advertencia".
 *
 * <p>La primera aparición de una clave nunca pasa por aquí (la resuelve
 * {@link MotorCoincidencia} de forma normal). A partir de la segunda
 * aparición, esta regla decide <b>no</b> volver a escribir el consumo:
 * asigna {@code 0} MB, igual que una clave sin match, para no inflar el
 * total escrito de esa fuente y disparar una falsa alarma en el reporte
 * de consistencia de totales (punto 7 / spec 03). En su lugar, genera un
 * aviso explícito con la clave, la tabla donde se repitió, y el consumo
 * real que le hubiera correspondido, para que el usuario revise a mano
 * si es un duplicado real en la plantilla (típicamente un error de
 * captura).</p>
 *
 * <p>Es una decisión razonable para arrancar, no la definitiva: spec 00
 * deja pendiente confirmar con datos reales si este caso ocurre en la
 * práctica y qué comportamiento se espera. Si la confirmación cambia el
 * criterio, el ajuste se limita a esta clase.</p>
 */
public class PrimeraCoincidenciaGanaReglaAmbigua implements ReglaAmbigua {

    @Override
    public ResolucionAmbigua resolver(ContextoFila contexto) {
        String advertencia = "La clave '" + contexto.clave() + "' (" + contexto.fuente()
                + ") ya había sido asignada antes en esta corrida; en '" + contexto.tablaIdentificador()
                + "' es la aparición #" + contexto.numeroDeAparicion() + ". Se dejó en 0 MB en esta fila "
                + "para no duplicar el consumo (habría correspondido " + contexto.consumoMb()
                + " MB) — revisar manualmente si es un duplicado real en la plantilla.";
        return new ResolucionAmbigua(Asignacion.sinMatch(), advertencia);
    }
}