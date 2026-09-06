# 05 — Flujo de trabajo: Claude (autor) / opencode (ejecutor)

> Spec operativa, no de dominio. Documenta cómo se produce el código de
> este proyecto a partir de Fase 1 en adelante. Léela junto con
> `00-vision-general.md` a `04-modelo-datos-y-api.md` al retomar el
> proyecto en una sesión nueva.

## Decisión
Claude (en esta conversación de chat) es el único que **escribe y decide**
el código: lógica, estructura de clases, nombres, tests. **opencode no
escribe ni modifica lógica** — su rol se limita a:

1. Crear/editar archivos con el contenido **exacto** que Claude entrega
   (copiar-pegar, sin "mejoras", sin refactors espontáneos, sin
   autocompletar lo que Claude dejó incompleto).
2. Ejecutar comandos de build/test (`mvn compile`, `mvn test`, etc.).
3. Devolver la salida **cruda** (stack traces, output de consola) para que
   Claude decida cómo corregir.

## Motivo
opencode corre con modelos de menor capacidad de razonamiento en esta
config. Dejarle decisiones de diseño o "arreglos" de compilación por su
cuenta introduce el mismo tipo de riesgo que las specs 01–03 ya
identificaron para el dominio (bugs silenciosos, deriva respecto a la
spec) — pero ahora en la capa de implementación. Concentrar la autoría en
Claude (razonamiento más fuerte, con las specs completas en contexto)
minimiza ese riesgo. opencode como "manos" que pegan y ejecutan es
barato y no requiere que razone sobre el dominio.

## Mecánica por unidad de trabajo
Para cada pieza de código (una clase, una interfaz, un test):

1. Claude entrega: **ruta completa del archivo** + **contenido íntegro del
   archivo** (no un diff parcial, salvo que el archivo ya sea grande y el
   cambio sea muy acotado — en ese caso, un bloque de reemplazo con
   suficiente contexto para ubicarlo sin ambigüedad).
2. El usuario pasa eso a opencode con instrucción explícita de tipo
   "crea/reemplaza este archivo con este contenido exacto, no cambies
   nada".
3. opencode ejecuta el build/tests correspondientes.
4. El usuario copia la salida de vuelta a Claude (éxito o error).
5. Si hay error: Claude lo corrige explícitamente y vuelve a entregar el
   archivo completo corregido — nunca se le pide a opencode que "arregle
   esto".

## Regla dura para opencode
Si algo no compila, falta un import, o el archivo referencia una clase que
aún no existe: opencode **no debe inventar ni completar nada por su
cuenta**. Reporta el error tal cual y espera la siguiente entrega de
Claude. Esto es intencional aunque parezca ineficiente — es la manera de
evitar que aparezca lógica no revisada por Claude en el proyecto.

## Orden de entrega de código sugerido para Fase 1
Motor de parsing puro (sin Spring), en este orden, cada uno con sus tests
JUnit antes de pasar al siguiente:
1. Modelo de dominio compartido: `ResultadoParseo`, `AvisoParseo`,
   excepciones (`ArchivoFormatoInvalidoException`, `HojaSinTablaException`).
2. `HeaderResolver` (interfaz + normalización + implementación para
   plantilla).
3. Resolver de tablas de la plantilla (clasificación simple/dual/cascada,
   spec 01).
4. Parser de Alestra (spec 02).
5. Parser de Telcel (`.csv` y `.xlsx`, spec 02).
6. Parser de ATT (reutiliza lógica de Telcel post-agregación).
7. Motor de coincidencia (spec 03): cascada, dual SIM, no-encontrados,
   consistencia de totales.
8. Interfaz `ReglaAmbigua` con implementación default.

## Nota de continuidad entre sesiones
Si esta conversación se corta o se retoma en otra sesión de chat:
- Releer specs 00–04 (dominio) y esta spec 05 (flujo) antes de continuar.
- Preguntar en qué punto del orden de la sección anterior se quedó el
  proyecto, y pedir al usuario los archivos ya generados (si los tiene)
  para no reescribir desde cero lo que ya está validado.
