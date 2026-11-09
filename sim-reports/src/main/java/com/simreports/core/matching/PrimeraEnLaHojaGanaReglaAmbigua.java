package com.simreports.core.matching;

/**
 * Implementación default de {@link ReglaAmbigua}: la regla confirmada por
 * el usuario el 2026-11-05 (spec 10, D-001 y D-006).
 *
 * <ul>
 *   <li><b>Misma SIM en hojas distintas</b> (D-001): se cuenta en cada
 *       hoja, porque cada hoja se entrega por separado a su destinatario.
 *       La aparición recibe el consumo completo y queda un aviso
 *       informativo. El motor descuenta esos MB en la consistencia de
 *       totales ({@link TotalesFuente#totalRepetidoEntreHojas()}).</li>
 *   <li><b>Misma SIM repetida dentro de una hoja</b> (D-003 a D-006): la
 *       <b>primera</b> aparición de esa hoja conserva el consumo y las
 *       demás van en 0 MB, con aviso.</li>
 * </ul>
 *
 * <p>Cuando la fila válida no es la primera, o una línea está mal
 * capturada, el usuario corrige la plantilla (D-002, D-007, D-009); la app
 * no adivina. Todas las repeticiones salen en el reporte final de la
 * corrida ({@link ResultadoMatching#simsRepetidas()}).</p>
 */
public class PrimeraEnLaHojaGanaReglaAmbigua implements ReglaAmbigua {

    @Override
    public ResolucionAmbigua resolver(ContextoFila contexto) {
        if (contexto.yaAparecioEnEstaHoja()) {
            String advertencia = "La clave '" + contexto.clave() + "' (" + contexto.fuente()
                    + ") está repetida en la hoja '" + contexto.hoja() + "'. En " + contexto.ubicacion()
                    + " se dejó en 0 MB: la primera aparición de la hoja conserva el consumo ("
                    + contexto.consumoMb() + " MB). Si la fila válida es otra, corregir la plantilla.";
            return new ResolucionAmbigua(Asignacion.sinMatch(), advertencia);
        }
        String advertencia = "La clave '" + contexto.clave() + "' (" + contexto.fuente()
                + ") también aparece en otra hoja; en " + contexto.ubicacion()
                + " se cuenta completa (" + contexto.consumoMb() + " MB), como en cada hoja donde aparece.";
        return new ResolucionAmbigua(new Asignacion(contexto.consumoMb(), contexto.fuente()), advertencia);
    }
}
