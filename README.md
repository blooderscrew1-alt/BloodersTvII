# BloodersTv II

Cliente Android (teléfono, tablet, TV Box y Android TV) para listas IPTV propias y para el catálogo de **NSRPlay**. La prioridad del proyecto es una navegación fluida incluso en equipos de gama baja: listas ligeras, sin animaciones costosas, sin servicios que no se usan y un reproductor web enfocado.

## Qué incluye

- **TV en vivo** desde listas M3U/M3U8 y Xtream Codes, con zapping y favoritos.
- **Películas y series** de tus listas IPTV y de NSRPlay, en la misma pestaña.
- **NSRPlay integrado** (sin pestaña propia):
  - sus títulos aparecen junto a los de IPTV en **Películas**, **Series** y la **búsqueda global**;
  - cada tarjeta NSRPlay lleva una insignia `NSRPLAY`;
  - búsqueda explícita con la lupa (no se dispara una petición por cada tecla);
  - detalle con sinopsis, servidores y, en series, temporadas/episodios;
  - cada servidor abre el **reproductor web unificado** basado en Serchwebview.
- **Reproductor web** (`PlayerWebActivity`): WebView con bloqueo de anuncios y pop-ups, pantalla completa HTML5, control de velocidad, modo escritorio, Picture-in-Picture y pantalla siempre encendida.
- **Extensiones Chrome/Edge** en el reproductor: paquetes `.zip` o `.crx` con content scripts (JS/CSS), background/service worker y reglas estáticas `declarativeNetRequest` (block, redirect, upgradeScheme). Se gestionan desde **Ajustes → Extensiones**.
- **Ajustes**: tema, caché/buffer para conexiones lentas, PIN parental, historial, NSRPlay y extensiones.
- **Sin Firebase, Room, Retrofit, Moshi ni KSP**: se retiraron dependencias que el código no usaba para reducir tamaño, arranque y tiempo de compilación.

## Configurar NSRPlay

### Desde la app (recomendado)

Abre **Ajustes → NSRPlay** y pega la API Key ahí. También puedes cambiar la URL del servicio si usas un proxy. La configuración se guarda en el dispositivo y se aplica al volver a Películas, Series o la búsqueda; **no hay que editar código ni recompilar**.

La pestaña incluye:

- campo de API Key con mostrar/ocultar;
- URL del servicio (por defecto `https://nsrplay.space`);
- **Guardar configuración**;
- **Probar conexión**, que hace una petición real y muestra cuántos títulos respondió y cuánto tardó;
- **Restablecer**, para volver a los valores incluidos en el APK.

Los valores de `.env` son solo los valores por defecto. Si el campo de clave se deja vacío, se usa la clave compilada en la app.

### Desde `.env` (para compilar)

La API Key se lee del archivo `.env` (ignorado por git) y se inyecta en `BuildConfig` al compilar. **No se escribe en el código fuente**.

1. Copia `.env.example` a `.env`.
2. Reemplaza el valor:

```properties
NSRPLAY_API_KEY=tu_clave_real
# Opcional, para usar tu propio proxy:
# NSRPLAY_BASE_URL=https://tu-proxy.example.com
```

3. Recompila la app.

Si no hay clave, las filas de NSRPlay muestran un aviso en lugar de romper la navegación.

> **Importante para distribuir la app:** cualquier clave incluida en un APK es recuperable por terceros. La documentación de NSRPlay prohíbe publicar la clave en el cliente. Para una versión pública, coloca un proxy propio (una función serverless basta) que añada el header `X-API-Key`, y apunta la app a ese proxy.

Los endpoints usados son:

| Uso | Endpoint |
| --- | --- |
| Catálogo | `GET /api/v1/content/catalog?type=movie\|series&page=N&genre=slug` |
| Búsqueda | `GET /api/v1/content/search?s=texto` |
| Detalle | `GET /api/v1/content/info/{slug}?type=movie\|series` |
| Servidores de episodio | `GET /api/v1/content/servers?slug=...&season=N&episode=N` |

El reproductor abre el `embedUrl` del servidor elegido. La app no resuelve ni almacena enlaces de video, y no guarda el catálogo en disco, respetando los términos de NSRPlay.

## Extensiones del reproductor

Abre **Ajustes → Extensiones** y pulsa **Instalar extensión desde archivo**. Se aceptan paquetes `.zip` y `.crx` que contengan `manifest.json`.

Soporte actual:

- `content_scripts` con JS y CSS y patrones `matches`;
- `background.scripts` (MV2) y `background.service_worker` (MV3), inyectados como scripts de página;
- reglas estáticas de `declarative_net_request` (`block`, `redirect`, `upgradeScheme`);
- bloqueo de dominios publicitarios y redirecciones, más el guard de `window.open`/pop-ups del reproductor;
- extensiones integradas: uBlock Video Shield, Anti-Popup y Redirecciones, SponsorBlock, Dark Reader, Video Speed Controller y Force PiP.

Limitaciones: no hay acceso a Chrome Web Store dentro de la app, no se ejecuta un runtime completo de Chrome (algunas APIs `chrome.*`, reglas dinámicas y el bloqueo `webRequest` de MV2 no están disponibles) y los recursos binarios de las extensiones no se sirven. El bloqueo de red funciona con reglas estáticas `declarativeNetRequest` (MV3) y con el filtro de dominios propio de la app. Para instalar, descarga el paquete `.zip`/`.crx` en el dispositivo y selecciónalo desde el selector de archivos.

## Compilar

Requisitos: JDK 17+ y Android SDK con `platforms;android-36` y `build-tools;36.0.0`.

```bash
./gradlew :app:assembleDebug       # Linux / macOS
gradlew.bat :app:assembleDebug     # Windows
# APK: app/build/outputs/apk/debug/app-debug.apk
```

El wrapper usa Gradle 9.3.1. También puedes abrir el proyecto directamente en Android Studio. El proyecto fija `compileSdk 36.1`, `minSdk 24` y `targetSdk 36`.

## Cambios de esta versión

### Menús flotantes y proporciones

- Nuevo `AdaptiveDialogFrame`: todos los diálogos se miden contra la ventana real, con margen seguro, ancho máximo y alto máximo. Ya no usan porcentajes del diálogo de plataforma (que en TV quedaban enormes y en teléfonos pequeños se salían).
- `AddPlaylistDialog`: formulario con scroll y **footer fijo**; los botones Cancelar/Importar siempre están visibles. Campos de una línea y control segmentado compacto en lugar de `TabRow`.
- `PlaylistsScreen`: en pantallas angostas el título y el botón "Agregar Lista" se apilan; antes el botón quedaba comprimido a una franja vertical de 10 dp al borde de la pantalla.
- `CategoryFilterDialog`: cabecera compacta, lista con altura mínima garantizada y footer que se apila en pantallas angostas.
- `ParentalPinDialog`: teclado y tipografía adaptativos; en horizontal muy bajo se compacta y todo el contenido puede desplazarse.
- `TopHeaderBar`: **se eliminaron el reloj y la fecha**; de paso desaparece la recomposición periódica que provocaban. Tamaños reducidos y `statusBars` respetadas.
- **Eliminada la pestaña y la tarjeta Premium**, que no aportaba funcionalidad.
- Chips de búsqueda en `LazyRow` para que nunca se corten.
- Hero banner con altura proporcional al ancho (170–320 dp) en lugar de 230 dp fijos.
- Campos de búsqueda con ancho máximo en pantallas grandes.

### Extensiones Chrome/Edge

- Pestaña **Extensiones** en Ajustes: instalar `.zip`/`.crx`, activar/desactivar y eliminar.
- Lector de paquetes con soporte de `manifest.json` en raíz o subcarpeta y cabecera CRX2/CRX3.
- Motor de content scripts + shim de `chrome.runtime`, `chrome.storage`, `GM_addStyle`, `GM_setValue` y `GM_getValue`.
- Filtro de red a partir de reglas `declarativeNetRequest`: dominios, `urlFilter`, `regexFilter`, block/allow/redirect/upgradeScheme.
- Inyección al confirmar la página (`onPageCommitVisible`) y al terminar, con guard para no ejecutar dos veces.

### NSRPlay + Serchwebview

- Sin pestaña NSRPlay: sus títulos se muestran dentro de **Películas**, **Series** y la **búsqueda global**, con insignia `NSRPLAY` en cada tarjeta.
- Caché en memoria de 5 minutos para el catálogo (compartida entre pestañas) y carga de más títulos bajo demanda; así cambiar de pestaña no vuelve a pedir el catálogo ni muestra un spinner innecesario.
- Búsqueda global unificada: resultados de IPTV y NSRPlay en la misma lista. La búsqueda local sigue siendo instantánea; NSRPlay se consulta al pulsar la **lupa** o **Enter** (así respetamos su límite de 5 búsquedas/minuto).
- Cuando el endpoint de búsqueda de NSRPlay devuelve vacío, la app usa las sugerencias públicas de IMDb para convertir una búsqueda parcial en el título completo (“saul” → “Better Call Saul”), prueba el slug en `/content/info` y, si tampoco hay resultado, busca en la primera página del catálogo. IMDb también aporta el póster cuando NSRPlay lo devuelve vacío.
- Caché de búsquedas de 5 minutos: repetir la misma consulta es instantáneo.
- Pestañas **General / NSRPlay** en Ajustes: clave, URL, guardar, probar conexión y restablecer sin tocar el código.
- Cliente HTTP propio y liviano (`NsrPlayRepository`) con mensajes de error claros para 401/404/429/5xx.
- Detalle de película/serie y selector de servidores por episodio.
- `PlayerWebActivity` porta el motor de Serchwebview: `PlayerAdBlocker`, script de auto-skip, fullscreen nativo, velocidad, UA de escritorio, PiP y bloqueo de ventanas emergentes.

## Estructura relevante

```
app/src/main/java/com/example/
├─ data/nsrplay/           # modelos, cliente, configuración y caché de NSRPlay
├─ ui/components/          # AdaptiveDialog, PIN, filtros, cabecera, pósters y fila NSRPlay
├─ ui/screens/NsrPlayDetailScreen.kt
├─ webplayer/              # PlayerWebActivity + PlayerAdBlocker (Serchwebview)
└─ MainActivity.kt
```

## Verificación realizada

- `:app:assembleDebug` compila y genera APK.
- 18 pruebas unitarias pasan (`ExampleUnitTest`, `ExampleRobolectricTest`, `NsrPlayTextTest`, `NsrPlayConfigStoreTest`, `ExtensionEngineTest`).
- UI probada en 1920×1080 (tablet/TV), 1080×1920 a 420 dpi (teléfono vertical) y 1920×1080 a 420 dpi (teléfono horizontal): cabecera, botón de listas, diálogo de alta y reproductor web.
- NSRPlay verificado en producción dentro de Películas (badges `NSRPLAY`) y en la búsqueda global.
- Búsqueda por slug y por sugerencias verificada con “Chappie” y “saul” (encuentra “Better Call Saul” con póster y episodios).
- La configuración NSRPlay se probó de extremo a extremo: URL de mock, "Probar conexión" (respuesta real), guardado y apertura del catálogo con esa configuración.

## Aviso

BloodersTv no aloja, sube ni distribuye contenido. Las listas IPTV son responsabilidad del usuario y NSRPlay indexa enlaces publicados por terceros. Verifica que tu uso cumpla las leyes de tu país y las condiciones de cada proveedor.
