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
- **Vimeus**: catálogo de películas, series y anime con paginación, búsqueda por título, detalle con temporadas/episodios y reproducción embed. Se configura desde **Ajustes → Vimeus**.
- **Búsqueda global** con vista tipo cover-flow: póster central grande, laterales inclinados, borde de acento, reflejo y título/progreso sobre la carátula. IPTV, **Vimeus** y NSRPlay aparecen en ese orden.
- **Continuar viendo**: cuadrícula dinámica basada en el historial de IPTV **y NSRPlay/Vimeus**, con carátula, progreso, tiempo restante y reanudación desde el punto guardado. Sustituye al banner promocional.
- **Respaldo**: exporta, comparte o importa listas M3U/Xtream y ajustes entre dispositivos desde **Ajustes → Respaldo**.
- **Etiquetas `VISTO`** en tarjetas de películas/series y en la búsqueda. Se basan en el historial IPTV y en un registro persistente para NSRPlay.
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

### Continuar viendo

- El banner promocional se reemplazó por una **cuadrícula adaptativa** (1–5 columnas según el ancho) con los últimos 8 títulos del historial.
- Mezcla el historial IPTV (con progreso y reanudación) y el historial NSRPlay/Vimeus (sin minuto, porque sus reproductores web no lo exponen). Los títulos de proveedor llevan su etiqueta (`NSRPLAY` cian / `VIMEUS` naranja) y al pulsarlos se abre su detalle para elegir servidor.
- **Borrar historial** en Ajustes elimina los historiales IPTV, NSRPlay y Vimeus.
- Cada tarjeta muestra carátula 16:9, tipo, progreso, porcentaje y tiempo restante cuando existe.
- Al pulsar una tarjeta IPTV, el reproductor arranca en la **posición guardada**; no vuelve al inicio.
- La posición se actualiza cada 500 ms en memoria y el historial se persiste cada 1,5 s para no castigar el almacenamiento en equipos de gama baja.
- Al cambiar de canal/película dentro del reproductor también se reanuda el título si tenía progreso.

### Etiquetas de contenido visto

- Las tarjetas IPTV se marcan como `VISTO` cuando el título aparece en el historial de reproducción.
- Los títulos NSRPlay y Vimeus se marcan al abrir un servidor de reproducción; las claves `nsr:<tipo>:<slug>` y `vimeus:<tipo>:<id>` se guardan en las preferencias de la app.
- La etiqueta aparece en Películas, Series, la búsqueda global, el historial y los rankings.
- **Borrar historial** en Ajustes elimina también las marcas de proveedores.

### Navegación con control remoto / teclado

- Los pósters del carrusel son enfocables: borde blanco al enfocar, izquierda/derecha para recorrerlos y Enter/OK para abrir.
- En la búsqueda, **Abajo** desde el campo de texto lleva a los filtros y otro **Abajo** al carrusel; así no hace falta tocar la pantalla.
- Pestañas de Ajustes, temporadas/episodios de Vimeus y chips/episodios de NSRPlay también muestran foco y responden al D-pad.
- El foco se centra automáticamente en el póster seleccionado.

### Búsqueda visual

- Los resultados se muestran como un carrusel de pósters (Pager) en lugar de filas de lista.
- La tarjeta central se agranda, recibe el color del proveedor, muestra el título y un reflejo debajo; las laterales se reducen e inclinan.
- Tocar una tarjeta lateral la centra; tocar la central abre el detalle IPTV/Vimeus/NSRPlay.
- El orden de las secciones es **IPTV → Vimeus → NSRPlay**.
- La vista es estática (sin animaciones de fondo ni blur) para no perder fluidez en gama baja.

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
- Ajustes con pestañas **General / NSRPlay / Vimeus / Extensiones / Respaldo**: claves, URL, guardar, probar conexión y restablecer sin tocar el código.
- Cliente HTTP propio y liviano (`NsrPlayRepository`) con mensajes de error claros para 401/404/429/5xx.
- Detalle de película/serie y selector de servidores por episodio.
- `PlayerWebActivity` porta el motor de Serchwebview: `PlayerAdBlocker`, script de auto-skip, fullscreen nativo, velocidad, UA de escritorio, PiP y bloqueo de ventanas emergentes.

### Vimeus

- Cliente `VimeusRepository` para `/api/listing/movies`, `/series`, `/animes` y `/episodes` con header `X-API-Key`.
- Filas **Vimeus · Películas**, **Vimeus · Series** y **Vimeus · Anime** dentro de las pestañas correspondientes, con paginación y caché de 5 minutos.
- Búsqueda parcial por IMDb (sin endpoint de búsqueda en Vimeus) que construye los embeds `/e/movie`, `/e/serie` y `/e/anime` con `view_key`.
- Detalle con póster, sinopsis, temporadas y episodios; cada episodio abre el embed con `se` y `ep`. Si la API entrega `embed_url`, se usa su `view_key` oficial.
- Historial y etiqueta `VISTO` con la clave `vimeus:<tipo>:<id>`.

### Respaldo entre dispositivos

- Pestaña **Respaldo** en Ajustes con **Exportar**, **Compartir** e **Importar**.
- Exporta `JSON` con listas M3U por URL, cuentas Xtream, tema, buffer, PIN, categorías bloqueadas, favoritos y claves de NSRPlay/Vimeus.
- Al importar, las listas M3U/Xtream se vuelven a descargar y se omite cualquier lista duplicada. Las listas pegadas como texto se avisan porque no se pueden reconstruir.
- El archivo contiene credenciales y API keys: se recomienda compartirlo solo por canales seguros.

## Estructura relevante

```
app/src/main/java/com/example/
├─ data/nsrplay/           # modelos, cliente, configuración y caché de NSRPlay
├─ data/vimeus/            # modelos, cliente, configuración y caché de Vimeus
├─ data/backup/            # exportación/importación de listas y ajustes
├─ ui/components/          # AdaptiveDialog, PIN, filtros, cabecera, pósters y filas de proveedores
├─ ui/screens/NsrPlayDetailScreen.kt
├─ ui/screens/VimeusDetailScreen.kt
├─ ui/screens/BackupSettings.kt
├─ webplayer/              # PlayerWebActivity + PlayerAdBlocker (Serchwebview)
└─ MainActivity.kt
```

## Verificación realizada

- `:app:assembleDebug` compila y genera APK.
- 23 pruebas unitarias pasan (`ExampleUnitTest`, `ExampleRobolectricTest`, `NsrPlayTextTest`, `NsrPlayConfigStoreTest`, `ExtensionEngineTest`, `WatchedContentTest`, `VimeusBackupTest`).
- UI probada en 1920×1080 (tablet/TV), 1080×1920 a 420 dpi (teléfono vertical) y 1920×1080 a 420 dpi (teléfono horizontal): cabecera, botón de listas, diálogo de alta y reproductor web.
- NSRPlay verificado en producción dentro de Películas (badges `NSRPLAY`) y en la búsqueda global.
- Vimeus verificado en producción: catálogo de películas, series y anime, detalle de serie con temporadas/episodios, reproducción embed y búsqueda “saul” → “Better Call Saul”.
- Búsqueda visual verificada con “titanic” y “saul”: carrusel de pósters, orden IPTV → Vimeus → NSRPlay, borde de acento y reflejo.
- Navegación D-pad verificada en la búsqueda: campo → filtros → carrusel, izquierda/derecha entre pósters y Enter abre el detalle.
- Respaldo verificado exportando a `Download`, leyendo el JSON e importándolo de vuelta.
- Búsqueda por slug y por sugerencias verificada con “Chappie” y “saul” (encuentra “Better Call Saul” con póster y episodios).
- La configuración NSRPlay se probó de extremo a extremo: URL de mock, "Probar conexión" (respuesta real), guardado y apertura del catálogo con esa configuración.

## Aviso

BloodersTv no aloja, sube ni distribuye contenido. Las listas IPTV son responsabilidad del usuario y NSRPlay indexa enlaces publicados por terceros. Verifica que tu uso cumpla las leyes de tu país y las condiciones de cada proveedor.
