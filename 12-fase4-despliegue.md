# 12 — Fase 4: en línea, con usuarios y sin guardar datos de clientes

> Mismo formato que los checklists 06, 08 y 11. Decisiones D-023 a D-027 en
> la spec 10.

## Objetivo
Usar la app desde cualquier computadora:
- app en **Render**, base de datos en **Neon**;
- varias personas, cada una con su usuario y contraseña;
- el usuario principal es **superadmin**.

## Decisiones (2026-11-09)
- **D-023 — La app no guarda archivos ni datos de clientes.**
  - Cada corrida recibe la plantilla, los 3 archivos de los operadores y el
    periodo. Devuelve el detalle y el `.xlsx` en la misma respuesta. Nada
    de eso queda en disco ni en la base de datos.
  - El usuario guarda la plantilla, las BD y el reporte en su computadora o
    en la nube.
  - Se quitan:
    - las versiones de plantilla y el mapeo guardado;
    - la corrección del mapeo desde la app (si una tabla no se detecta
      bien, se corrige en el Excel);
    - el historial de reportes con descarga;
    - la carpeta `SIMREPORTS_DATOS_DIR`.
  - Queda **"Revisar plantilla"**: subirla y ver cómo la lee la app, sin
    guardarla.
- **D-024 — Usuarios con dos roles.**
  - **SUPERADMIN** (el usuario principal) puede:
    - crear usuarios;
    - activarlos y desactivarlos;
    - restablecer contraseñas (temporal, que obliga a cambiarla al entrar);
    - ver la actividad de todos.
  - **USUARIO**: genera reportes y cambia su propia contraseña.
  - El primer superadmin se crea al arrancar, desde variables de entorno,
    solo si no existe ningún usuario. Debe cambiar la contraseña la primera
    vez que entra.
- **D-025 — Registro de actividad** (solo lo ve el superadmin):
  - quién, cuándo, desde qué IP y qué hizo:
    - entró, falló al entrar o salió;
    - generó un reporte (periodo y si cuadró) o falló al generarlo;
    - revisó una plantilla;
    - cambió un usuario o una contraseña;
  - **nada de los archivos**: ni líneas, ni ICCID, ni unidades, ni consumos.
- **D-026 — Seguridad.**
  - Spring Security con formulario de inicio de sesión y sesión en memoria
    (si la app se reinicia, hay que volver a entrar).
  - Cookie `HttpOnly`, `Secure` y `SameSite=Lax`; protección CSRF.
  - Contraseñas con BCrypt; mínimo 10 caracteres.
  - Bloqueo de 15 minutos tras 5 intentos fallidos seguidos del mismo
    usuario.
  - La sesión expira tras 30 minutos sin uso.
- **D-027 — Despliegue.**
  - Render (Docker) + Neon (PostgreSQL con SSL).
  - Un servicio externo del usuario hace ping cada ≤ 5 min a `/salud` para
    que Render gratis no se duerma.
  - `/salud` no toca la base y el pool cierra las conexiones sin uso, para
    que Neon sí se suspenda y no gaste sus horas de cómputo gratis.

## Checklist
- [x] 1. **Generación sin estado:**
      - `POST /api/reportes` recibe plantilla + 3 archivos + periodo y
        devuelve el detalle con el `.xlsx`;
      - `POST /api/plantilla/revisar`;
      - quitar versiones, historial y almacenamiento (migración V3);
      - pantallas Generar y Revisar plantilla.

      *Hecho el 2026-11-09. `./mvnw test`: 243/243. Probado con la app
      corriendo y los archivos reales de octubre: la respuesta tarda unos
      2 s, los totales cuadran y la base queda solo con la tabla de
      Flyway.*
- [x] 2. **Usuarios e inicio de sesión:**
      - tabla de usuarios, Spring Security, pantalla de entrada, salir;
      - cambio de contraseña obligatorio, bloqueo por intentos, CSRF en el
        cliente;
      - superadmin inicial.

      *Hecho el 2026-11-09. `./mvnw test`: 266/266. Detalle abajo, en
      "Notas del punto 2".*
- [ ] 3. **Pantallas del superadmin:** usuarios (crear, activar o desactivar,
      restablecer contraseña) y actividad.
- [ ] 4. **Registro de actividad** en cada acción.
- [ ] 5. **Despliegue:**
      - `Dockerfile`;
      - `/salud`;
      - configuración para Neon (SSL, pool) y memoria para 512 MB.
- [ ] 6. **Guía** paso a paso: Neon, Render, variables de entorno y ping;
      PR de `correcciones-revision` a `main`.
- [ ] 7. **Prueba en línea** con los archivos reales.

## Notas del punto 2 (2026-11-09)
- **Base:** migración V4, tabla `usuario`:
  - nombre en minúsculas y único;
  - hash BCrypt;
  - rol `SUPERADMIN` o `USUARIO`;
  - activo y contraseña temporal;
  - intentos fallidos y bloqueo;
  - `version_credenciales`, que sube con cada cambio de contraseña y cierra
    las demás sesiones.
- **Endpoints:**
  - `GET /api/sesion/csrf`: deja la cookie `XSRF-TOKEN`; no pide sesión.
  - `POST /api/sesion/entrar`, con los campos `usuario` y `password`:
    - `200` con `{"usuario","nombreCompleto","rol","debeCambiarPassword"}`;
    - `401` si no entra. El mensaje es el mismo si el usuario no existe,
      está desactivado o la contraseña es incorrecta; el bloqueo sí se
      avisa.
  - `GET /api/sesion`: quién soy.
  - `POST /api/sesion/password`, con `{"actual","nueva"}`.
  - `POST /api/sesion/salir`: `204`.
- **Filtro `UsuarioVigenteFiltro`:** en cada llamada a la API revisa al
  usuario en la base.
  - Si lo desactivaron, le cambiaron la contraseña o el rol, cierra la
    sesión (`401`).
  - Con contraseña temporal solo deja cambiarla (`403` en lo demás).
- **CSRF para páginas con JavaScript (`csrf.spa()`):** cookie `XSRF-TOKEN`
  y cabecera `X-XSRF-TOKEN`.
- **Política de contenido estricta:** solo scripts y estilos propios, sin
  estilos en línea. `PaginaWebTest` lo vigila.
- **Superadmin inicial:** `SIMREPORTS_ADMIN_USUARIO`,
  `SIMREPORTS_ADMIN_PASSWORD` y, opcional, `SIMREPORTS_ADMIN_NOMBRE`.
  - Solo se usan si no hay ningún usuario.
  - Una contraseña de menos de 10 caracteres detiene el arranque con el
    motivo.
- **Pantallas:**
  - `entrar.html`;
  - "Mi cuenta" (`#cuenta`), con cambio de contraseña;
  - en la barra: nombre, "Mi cuenta" y "Salir".
- **Probado con el `.jar` real** (curl con cookies):
  - sin sesión, la raíz manda a la pantalla de entrada;
  - entrar sin token CSRF da `403`;
  - contraseña incorrecta, `401`;
  - con contraseña temporal, revisar la plantilla da `403`;
  - después de cambiarla, generar octubre da `200`;
  - salir da `204` y, después, `401`.
- **Riesgo aceptado:** alguien que conozca un nombre de usuario puede
  bloquearlo 15 minutos fallando 5 veces. El superadmin lo desbloquea
  restableciendo la contraseña (punto 3).

