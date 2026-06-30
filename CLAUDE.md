# CLAUDE.md — Reglas de trabajo para Educhronos

Aplicación de gestión de horarios escolares. Solver CP-SAT (OR-Tools) +
backend Spring Boot + persistencia SQLite/Hibernate + UI Angular (futura).
Proyecto de larga duración guiado por fases. Vas por la Fase 6 (persistencia).

## Jerarquía de autoridad (no negociable)
1. El CÓDIGO es la autoridad última sobre el estado real.
2. Documentación autoritativa en `docs/`:
   - `plan_trabajo_horarios.md`: estado, fases, criterios, deuda consciente,
     decisiones permanentes, registro de sesiones. FUENTE del estado del proyecto.
   - `modelo_datos_fase1.md`: esquema de datos (entidades, invariantes I1-I7,
     S1-S9, particiones). Referencia autoritativa del dominio.
   - `referencia-codigo-solver.md`: índice de API del módulo `solver/`,
     generado SOLO desde el código fuente.
   - `bitacora-sesiones.md`: histórico detallado (solo lectura).
3. Para datos del centro de referencia: PDF → volcado fiel
   (`docs/horario-referencia/`) → modelo §6.x. NUNCA inferir datos del OCR de
   los PDF; los volcados fieles son la fuente de verdad para fixtures.

Antes de proponer o construir nada, LEE el estado en `plan_trabajo_horarios.md`
(no lo asumas). No resumas el estado de memoria: dos copias se desincronizan.

## Arquitectura: frontera dura solver ↔ persistencia
- El módulo `solver/` (paquete `domain/`) son POJOs puros (records). NO LLEVAN
  anotaciones JPA NI dependencias de Spring/Hibernate. Esta frontera es
  permanente y no se cruza por conveniencia.
- Las entidades JPA viven en el módulo `app/` y tienen su propia forma (Id
  sintético, relaciones perezosas). Un MAPPER explícito convierte entidad JPA
  → modelo del solver (y solución → entidad). El solver no sabe que JPA existe.
- El solver usa `codigo` (clave natural String) como identidad. Las entidades
  JPA usan Id sintético. El mapper es quien reconcilia ambos mundos.
- Dependencias de módulo unidireccionales: `app` depende de `solver`; `solver`
  NUNCA depende de `app`.

## Disciplina de commits
- Mensajes de commit de UNA sola línea.
- Código y documentación en commits SEPARADOS.
- Si tocas `solver/src/main`, regenera `docs/referencia-codigo-solver.md`
  (ver `docs/Prompt_generar_referencia_codigo.txt`) en su propio commit de doc.
- El modelo (`modelo_datos_fase1.md`) se toca SOLO si el cambio añade una
  entidad o una invariante nueva. No por cambios de implementación.

## Fixtures y tests
- Fixtures de test en `solver/src/test/resources/fixtures/`.
- Fixtures construidos desde volcados fieles, nunca del OCR de los PDF.
- Tests pesados de escala llevan `@Tag("escala")` y se excluyen de `mvn test`
  por defecto; se corren con `mvn test -Pescala` (ver deuda D24/D25).
- D25: los tests de optimización a escala SOLO son fiables corridos AISLADOS
  (`-Dtest=NombreTest`), no en bloque, por contención de CPU.

## Modo de trabajo por bloques
- Fase a fase, y dentro de cada fase, bloque a bloque. "Una capa por bloque".
- No trocear en exceso (aprendizaje S31) ni meter varias capas en un bloque.
- En decisiones con impacto, exponer opciones y consecuencias ANTES de construir.
- No ceder por insistencia: defender lo correcto con argumentos.
- Si no sabes algo con certeza, dilo; no inventes.

## Rutina de cierre de bloque/sesión
Antes de dar por cerrado:
- Suite rápida verde (`mvn test`) + BUILD SUCCESS.
- Índice `referencia-codigo-solver.md` regenerado si se tocó `solver/src/main`.
- Modelo tocado solo si se añadió entidad/invariante.
- Commits de código y doc separados, de una línea.
- Working tree limpio.

## Idioma
Responde siempre en español.
