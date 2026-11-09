package com.simreports.core.matching;

/**
 * Regla que decide qué hacer cuando el motor detecta que una clave
 * (línea o ICCID) ya fue consultada antes durante la misma corrida —
 * spec 03, "Reglas ambiguas".
 *
 * <ul>
 *   <li><b>Ya cableada al motor:</b> una línea/ICCID aparece duplicada
 *       dentro de la misma hoja de la plantilla, o en dos hojas
 *       distintas. {@link MotorCoincidencia} invoca {@link #resolver}
 *       cada vez que una clave se consulta por segunda vez o más (ver
 *       {@link ContextoFila#numeroDeAparicion()} y
 *       {@link ContextoFila#yaAparecioEnEstaHoja()}). La regla confirmada
 *       por el usuario (D-001, D-006) es la default:
 *       {@link PrimeraEnLaHojaGanaReglaAmbigua}.</li>
 *   <li><b>Todavía no cableada:</b> un router mixto resuelve una SIM por
 *       línea y la otra por ICCID (hoy la spec asume que ambas SIMs de
 *       un router siempre se resuelven por línea —
 *       {@code TipoTablaPlantilla.DUAL_SIM}). {@link FilaPlantilla} no
 *       tiene forma de expresar un router mixto hoy, así que no hay
 *       ningún punto del motor que invoque esta regla para ese caso.
 *       Cuando se confirme que existe en la práctica, se extiende
 *       {@code FilaPlantilla} y el caso {@code DUAL_SIM} de
 *       {@code MotorCoincidencia} para delegarlo aquí también, sin
 *       tocar el resto del motor ya probado.</li>
 * </ul>
 *
 * <p>Ajustar el comportamiento ante datos reales de Fase 0 se limita a
 * escribir una nueva implementación de esta interfaz — el resto del
 * motor de coincidencia no cambia.</p>
 */
public interface ReglaAmbigua {

    ResolucionAmbigua resolver(ContextoFila contexto);
}