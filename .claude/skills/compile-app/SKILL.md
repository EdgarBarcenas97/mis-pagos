---
name: compile-app
description: Compila la app Android, la instala en un emulador o teléfono (enciende el emulador si no hay ninguno conectado) y la abre. Úsala cuando el usuario escriba /compile-app, pida "corre la app", "ábrela en el emulador", "quiero verla funcionando", o en la fase 6 (Demo) de /feature.
---

# /compile-app [--avd <nombre>] [--no-emulator] [--screenshot <ruta.png>]

Ejecuta **`scripts/run-app.sh`** pasándole los mismos argumentos. No hagas los
pasos a mano: el script es la única forma de compilar, instalar y abrir la app.

## Qué hace el script

1. **Dispositivo:** si hay un teléfono o emulador conectado, lo usa. Si no,
   enciende el primer emulador creado (o el de `--avd`) y espera a que arranque.
   Con `--no-emulator` no enciende nada y falla si no hay dispositivo.
2. **Compilar e instalar:** `./gradlew installDebug`.
3. **Abrir** la app en el dispositivo.
4. **Captura** (opcional): con `--screenshot <ruta.png>` guarda cómo quedó la pantalla.

## Al terminar

Responde en una línea: en qué dispositivo quedó abierta la app y la ruta de la
captura, si se pidió.

Si lo pidió `/feature` (fase 6), sigue con el MCP `mobile` para recorrer los
criterios de aceptación en la app abierta.

## Si falla

- Muestra el error del script tal cual y para.
- ❌ No cambies código ni configuración de Gradle para "hacerla compilar": eso
  es trabajo de `/feature`.
- ❌ No borres ni crees emuladores sin preguntar.
