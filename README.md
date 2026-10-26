# BloodersTv II

Cliente Android (telÃƒÆ’Ã‚Â©fono, tablet, TV Box y Android TV) para listas IPTV propias y para el catÃƒÆ’Ã‚Â¡logo de **NSRPlay**. La prioridad del proyecto es una navegaciÃƒÆ’Ã‚Â³n fluida incluso en equipos de gama baja: listas ligeras, sin animaciones costosas, sin servicios que no se usan y un reproductor web enfocado.

## QuÃƒÆ’Ã‚Â© incluye

- **TV en vivo** desde listas M3U/M3U8 y Xtream Codes, con zapping y favoritos.
- **PelÃƒÆ’Ã‚Â­culas y series** de tus listas IPTV y de NSRPlay, en la misma pestaÃƒÆ’Ã‚Â±a.
- **NSRPlay integrado** (sin pestaÃƒÆ’Ã‚Â±a propia):
  - sus tÃƒÆ’Ã‚Â­tulos aparecen junto a los de IPTV en **PelÃƒÆ’Ã‚Â­culas**, **Series** y la **bÃƒÆ’Ã‚Âºsqueda global**;
  - cada tarjeta NSRPlay lleva una insignia `NSRPLAY`;
  - bÃƒÆ’Ã‚Âºsqueda explÃƒÆ’Ã‚Â­cita con la lupa (no se dispara una peticiÃƒÆ’Ã‚Â³n por cada tecla);
  - detalle con sinopsis, servidores y, en series, temporadas/episodios;
  - cada servidor abre el **reproductor web unificado** basado en Serchwebview.
- **Reproductor web** (`PlayerWebActivity`): WebView con bloqueo de anuncios y pop-ups, pantalla completa HTML5, control de velocidad, modo escritorio, Picture-in-Picture y pantalla siempre encendida.
- **Extensiones Chrome/Edge** en el reproductor: paquetes `.zip` o `.crx` con content scripts (JS/CSS), background/service worker y reglas estÃƒÆ’Ã‚Â¡ticas `declarativeNetRequest` (block, redirect, upgradeScheme). Se gestionan desde **Ajustes ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ Extensiones**.
- **Vimeus**: catÃƒÆ’Ã‚Â¡logo de pelÃƒÆ’Ã‚Â­culas, series y anime con paginaciÃƒÆ’Ã‚Â³n, bÃƒÆ’Ã‚Âºsqueda por tÃƒÆ’Ã‚Â­tulo, detalle con temporadas/episodios y reproducciÃƒÆ’Ã‚Â³n embed. Se configura desde **Ajustes ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ Vimeus**.
- **Unlimplay**: catÃƒÆ’Ã‚Â¡logo de pelÃƒÆ’Ã‚Â­culas, series y anime usando TMDB para metadatos/bÃƒÆ’Ã‚Âºsqueda; reproduce mediante los iframes `/f/embed/{tipo}?tmdb=...`. Se configura desde **Ajustes ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ Unlimplay**.
- **BÃƒÆ’Ã‚Âºsqueda global** con vista tipo cover-flow: pÃƒÆ’Ã‚Â³ster central grande, laterales inclinados, borde de acento, reflejo y tÃƒÆ’Ã‚Â­tulo/progreso sobre la carÃƒÆ’Ã‚Â¡tula. Orden: IPTV ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ Vimeus ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ NSRPlay ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ **Unlimplay al final**.
- **Deslizador de tamaÃƒÆ’Ã‚Â±o de pÃƒÆ’Ã‚Â³sters** a la derecha de los filtros de bÃƒÆ’Ã‚Âºsqueda; el tamaÃƒÆ’Ã‚Â±o elegido se guarda y se aplica a los tres carruseles (IPTV, Vimeus y NSRPlay).
- **La barra de bÃƒÆ’Ã‚Âºsqueda se oculta al pulsar Enter/OK o la lupa**, para dejar mÃƒÆ’Ã‚Â¡s espacio a los resultados. Un icono de lupa a la izquierda de los filtros la vuelve a mostrar y enfoca el campo.
- **Bloqueo reforzado de anuncios de apuestas/casino** en NSRPlay, Vimeus y Unlimplay: incluye SapphireBet, 1xBet, Melbet, Mostbet, 22Bet, BC.Game, Stake, 1Win, Vulkan Vegas y variantes con subdominios numÃƒÆ’Ã‚Â©ricos.
- **Extensiones en todos los frames**: los scripts de extensiones se inyectan al inicio del documento en el reproductor y en iframes de NSRPlay/Vimeus/Unlimplay mediante `WebViewCompat`; las reglas de red ya se aplicaban a todas las peticiones.
- **Sin lÃƒÂ­mite de peso de extensiÃƒÂ³n**: se eliminaron los topes de 25 MB por paquete y 4 MB por archivo; el nÃƒÂºmero de reglas DNR sigue limitado para evitar bloquear el dispositivo.
- **Acelerar anuncios**: al detectar Ã¢â‚¬Å“Saltar anuncio en NÃ¢â‚¬Â, acelera el video del preroll y los temporizadores de la pÃƒÂ¡gina para que el anuncio termine en menos de un segundo; en iframes toca repetidamente la esquina del botÃƒÂ³n.
- **NSRPlay sin extracciÃƒÂ³n de servidores**: resuelve el tÃƒÂ­tulo en TMDB y abre `/embed/movie/{tmdbId}`; cada episodio abre `/embed/tv/{tmdbId}/{temporada}/{episodio}` en su reproductor web.
- **Reproductor web para TV Box**: autoplay al cargar, botÃƒÂ³n central y teclas multimedia para play/pausa, retroceder/avanzar 10 s; si el iframe no permite JavaScript se simula un toque central.
- **APK de rendimiento**: la variante `perf` usa R8, eliminaciÃƒÆ’Ã‚Â³n de recursos y firma de depuraciÃƒÆ’Ã‚Â³n; reduce el APK de ~22 MB a ~3 MB y el arranque medido en BlueStacks de ~1.9 s a ~0.4 s.
- **Historial de series resumido**: ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œContinuar viendoÃƒÂ¢Ã¢â€šÂ¬Ã‚Â muestra solo el ÃƒÆ’Ã‚Âºltimo episodio de cada serie, no todos los capÃƒÆ’Ã‚Â­tulos; las etiquetas `VISTO` se conservan por separado.
- **Episodios vistos**: el detalle de la serie y la lista lateral del reproductor marcan con `VISTO` los capÃƒÆ’Ã‚Â­tulos ya reproducidos.
- **Reproductor de series**: muestra una lista lateral de episodios y al terminar un capÃƒÆ’Ã‚Â­tulo pasa automÃƒÆ’Ã‚Â¡ticamente al siguiente.
- **Series IPTV agrupadas**: una portada por serie (todas las temporadas juntas); al abrirla se ven temporadas y episodios. El agrupado usa un solo recorrido rÃƒÆ’Ã‚Â¡pido y se ejecuta fuera del hilo principal.
- La bÃƒÆ’Ã‚Âºsqueda IPTV tambiÃƒÆ’Ã‚Â©n agrupa todos los capÃƒÆ’Ã‚Â­tulos en una sola tarjeta.
- **Series IPTV corregidas**: el importador ya no corta en 20 000 elementos (sube a 120 000) y reconoce episodios `1x01`, `S01E01`, ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œtemporada/capÃƒÆ’Ã‚Â­tulo/episodioÃƒÂ¢Ã¢â€šÂ¬Ã‚Â, ademÃƒÆ’Ã‚Â¡s de categorÃƒÆ’Ã‚Â­as Series/Anime/Novela. Los canales ya importados se reclasifican una vez automÃƒÆ’Ã‚Â¡ticamente.
- **BÃƒÆ’Ã‚Âºsqueda mÃƒÆ’Ã‚Â¡s estable en TV Box**: DNS con preferencia IPv4, reintento automÃƒÆ’Ã‚Â¡tico de peticiones GET y bÃƒÆ’Ã‚Âºsquedas de proveedores escalonadas para evitar timeouts por saturaciÃƒÆ’Ã‚Â³n.
- **BÃƒÆ’Ã‚Âºsqueda solo al confirmar**: los resultados de IPTV y proveedores aparecen al pulsar OK/Enter o la lupa, no mientras se escribe.
- **Inicio mÃƒÆ’Ã‚Â¡s ligero**: las secciones generales de PelÃƒÆ’Ã‚Â­culas y Series solo cargan sugerencias de NSRPlay; Vimeus y Unlimplay siguen disponibles desde la bÃƒÆ’Ã‚Âºsqueda.
- **Respaldo ÃƒÆ’Ã‚Âºnico**: la primera exportaciÃƒÆ’Ã‚Â³n elige la ubicaciÃƒÆ’Ã‚Â³n; las siguientes reemplazan ese mismo archivo en vez de crear copias con fecha.
- El respaldo incluye la **ediciÃƒÆ’Ã‚Â³n de categorÃƒÆ’Ã‚Â­as**: nombres de lista, categorÃƒÆ’Ã‚Â­as renombradas, reclasificaciÃƒÆ’Ã‚Â³n TV/PelÃƒÆ’Ã‚Â­culas/Series/Adultos y categorÃƒÆ’Ã‚Â­as ocultas.
- **Editor de listas IPTV**: renombrar la lista, copiar su enlace M3U/Xtream, reclasificar toda la lista o una categorÃƒÆ’Ã‚Â­a entre TV/PelÃƒÆ’Ã‚Â­culas/Series/**Adultos**, ocultar/mostrar categorÃƒÆ’Ã‚Â­as y renombrarlas.
- **PestaÃƒÆ’Ã‚Â±a Adultos** protegida con PIN, separada de TV/PelÃƒÆ’Ã‚Â­culas/Series y de la bÃƒÆ’Ã‚Âºsqueda general; el PIN se pide una sola vez por sesiÃƒÆ’Ã‚Â³n de la app.
- **Nuevo icono BloodersTV II** en todas las densidades, con fondo adaptativo negro y versiÃƒÆ’Ã‚Â³n redonda.
- **PelÃƒÆ’Ã‚Â­culas como pestaÃƒÆ’Ã‚Â±a inicial**: al abrir la app se entra directamente a PelÃƒÆ’Ã‚Â­culas en lugar de TelevisiÃƒÆ’Ã‚Â³n.
- **ÃƒÆ’Ã…Â¡ltimo canal TV**: al abrir la app se restaura el ÃƒÆ’Ã‚Âºltimo canal en vivo y su categorÃƒÆ’Ã‚Â­a; tambiÃƒÆ’Ã‚Â©n se recuerda al cambiar de canal con el control dentro del reproductor.
- **Continuar viendo**: cuadrÃƒÆ’Ã‚Â­cula dinÃƒÆ’Ã‚Â¡mica basada en el historial de IPTV **y NSRPlay/Vimeus/Unlimplay**, con carÃƒÆ’Ã‚Â¡tula, progreso, tiempo restante y reanudaciÃƒÆ’Ã‚Â³n desde el punto guardado.
- **Respaldo**: exporta, comparte o importa listas M3U/Xtream y ajustes entre dispositivos desde **Ajustes ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ Respaldo**.
- **Etiquetas `VISTO`** en tarjetas de pelÃƒÆ’Ã‚Â­culas/series y en la bÃƒÆ’Ã‚Âºsqueda. Se basan en el historial IPTV y en un registro persistente para NSRPlay.
- **Ajustes**: tema, cachÃƒÆ’Ã‚Â©/buffer para conexiones lentas, PIN parental, historial, NSRPlay y extensiones.
- **Sin Firebase, Room, Retrofit, Moshi ni KSP**: se retiraron dependencias que el cÃƒÆ’Ã‚Â³digo no usaba para reducir tamaÃƒÆ’Ã‚Â±o, arranque y tiempo de compilaciÃƒÆ’Ã‚Â³n.

## Configurar NSRPlay

### Desde la app (recomendado)

Abre **Ajustes ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ NSRPlay** y pega la API Key ahÃƒÆ’Ã‚Â­. TambiÃƒÆ’Ã‚Â©n puedes cambiar la URL del servicio si usas un proxy. La configuraciÃƒÆ’Ã‚Â³n se guarda en el dispositivo y se aplica al volver a PelÃƒÆ’Ã‚Â­culas, Series o la bÃƒÆ’Ã‚Âºsqueda; **no hay que editar cÃƒÆ’Ã‚Â³digo ni recompilar**.

La pestaÃƒÆ’Ã‚Â±a incluye:

- campo de API Key con mostrar/ocultar;
- URL del servicio (por defecto `https://nsrplay.space`);
- **Guardar configuraciÃƒÆ’Ã‚Â³n**;
- **Probar conexiÃƒÆ’Ã‚Â³n**, que hace una peticiÃƒÆ’Ã‚Â³n real y muestra cuÃƒÆ’Ã‚Â¡ntos tÃƒÆ’Ã‚Â­tulos respondiÃƒÆ’Ã‚Â³ y cuÃƒÆ’Ã‚Â¡nto tardÃƒÆ’Ã‚Â³;
- **Restablecer**, para volver a los valores incluidos en el APK.

Los valores de `.env` son solo los valores por defecto. Si el campo de clave se deja vacÃƒÆ’Ã‚Â­o, se usa la clave compilada en la app.

### Desde `.env` (para compilar)

La API Key se lee del archivo `.env` (ignorado por git) y se inyecta en `BuildConfig` al compilar. **No se escribe en el cÃƒÆ’Ã‚Â³digo fuente**.

1. Copia `.env.example` a `.env`.
2. Reemplaza el valor:

```properties
NSRPLAY_API_KEY=tu_clave_real
# Opcional, para usar tu propio proxy:
# NSRPLAY_BASE_URL=https://tu-proxy.example.com
```

3. Recompila la app.

Si no hay clave, las filas de NSRPlay muestran un aviso en lugar de romper la navegaciÃƒÆ’Ã‚Â³n.

> **Importante para distribuir la app:** cualquier clave incluida en un APK es recuperable por terceros. La documentaciÃƒÆ’Ã‚Â³n de NSRPlay prohÃƒÆ’Ã‚Â­be publicar la clave en el cliente. Para una versiÃƒÆ’Ã‚Â³n pÃƒÆ’Ã‚Âºblica, coloca un proxy propio (una funciÃƒÆ’Ã‚Â³n serverless basta) que aÃƒÆ’Ã‚Â±ada el header `X-API-Key`, y apunta la app a ese proxy.

Los endpoints usados son:

| Uso | Endpoint |
| --- | --- |
| CatÃƒÆ’Ã‚Â¡logo | `GET /api/v1/content/catalog?type=movie\|series&page=N&genre=slug` |
| BÃƒÆ’Ã‚Âºsqueda | `GET /api/v1/content/search?s=texto` |
| Detalle | `GET /api/v1/content/info/{slug}?type=movie\|series` |
| Servidores de episodio | `GET /api/v1/content/servers?slug=...&season=N&episode=N` |

El reproductor abre el `embedUrl` del servidor elegido. La app no resuelve ni almacena enlaces de video, y no guarda el catÃƒÆ’Ã‚Â¡logo en disco, respetando los tÃƒÆ’Ã‚Â©rminos de NSRPlay.

## Extensiones del reproductor

Abre **Ajustes ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ Extensiones** y pulsa **Instalar extensiÃƒÆ’Ã‚Â³n desde archivo**. Se aceptan paquetes `.zip` y `.crx` que contengan `manifest.json`.

Soporte actual:

- `content_scripts` con JS y CSS y patrones `matches`;
- `background.scripts` (MV2) y `background.service_worker` (MV3), inyectados como scripts de pÃƒÆ’Ã‚Â¡gina;
- reglas estÃƒÆ’Ã‚Â¡ticas de `declarative_net_request` (`block`, `redirect`, `upgradeScheme`);
- bloqueo de dominios publicitarios y redirecciones, mÃƒÆ’Ã‚Â¡s el guard de `window.open`/pop-ups del reproductor;
- extensiones integradas: uBlock Video Shield, Anti-Popup y Redirecciones, SponsorBlock, Dark Reader, Video Speed Controller y Force PiP.

Limitaciones: no hay acceso a Chrome Web Store dentro de la app, no se ejecuta un runtime completo de Chrome (algunas APIs `chrome.*`, reglas dinÃƒÆ’Ã‚Â¡micas y el bloqueo `webRequest` de MV2 no estÃƒÆ’Ã‚Â¡n disponibles) y los recursos binarios de las extensiones no se sirven. El bloqueo de red funciona con reglas estÃƒÆ’Ã‚Â¡ticas `declarativeNetRequest` (MV3) y con el filtro de dominios propio de la app. Para instalar, descarga el paquete `.zip`/`.crx` en el dispositivo y selecciÃƒÆ’Ã‚Â³nalo desde el selector de archivos.

## Compilar

Requisitos: JDK 17+ y Android SDK con `platforms;android-36` y `build-tools;36.0.0`.

```bash
./gradlew :app:assembleDebug       # Linux / macOS
gradlew.bat :app:assembleDebug     # Windows
# APK: app/build/outputs/apk/debug/app-debug.apk

gradlew.bat :app:assemblePerf      # APK final rÃƒÆ’Ã‚Â¡pido, R8 + recursos reducidos
# APK: app/build/outputs/apk/perf/app-perf.apk
```

El wrapper usa Gradle 9.3.1. TambiÃƒÆ’Ã‚Â©n puedes abrir el proyecto directamente en Android Studio. El proyecto fija `compileSdk 36.1`, `minSdk 24` y `targetSdk 36`.

## Cambios de esta versiÃƒÆ’Ã‚Â³n

### MenÃƒÆ’Ã‚Âºs flotantes y proporciones

- Nuevo `AdaptiveDialogFrame`: todos los diÃƒÆ’Ã‚Â¡logos se miden contra la ventana real, con margen seguro, ancho mÃƒÆ’Ã‚Â¡ximo y alto mÃƒÆ’Ã‚Â¡ximo. Ya no usan porcentajes del diÃƒÆ’Ã‚Â¡logo de plataforma (que en TV quedaban enormes y en telÃƒÆ’Ã‚Â©fonos pequeÃƒÆ’Ã‚Â±os se salÃƒÆ’Ã‚Â­an).
- `AddPlaylistDialog`: formulario con scroll y **footer fijo**; los botones Cancelar/Importar siempre estÃƒÆ’Ã‚Â¡n visibles. Campos de una lÃƒÆ’Ã‚Â­nea y control segmentado compacto en lugar de `TabRow`.
- `PlaylistsScreen`: en pantallas angostas el tÃƒÆ’Ã‚Â­tulo y el botÃƒÆ’Ã‚Â³n "Agregar Lista" se apilan; antes el botÃƒÆ’Ã‚Â³n quedaba comprimido a una franja vertical de 10 dp al borde de la pantalla.
- `CategoryFilterDialog`: cabecera compacta, lista con altura mÃƒÆ’Ã‚Â­nima garantizada y footer que se apila en pantallas angostas.
- `ParentalPinDialog`: teclado y tipografÃƒÆ’Ã‚Â­a adaptativos; en horizontal muy bajo se compacta y todo el contenido puede desplazarse.
- `TopHeaderBar`: **se eliminaron el reloj y la fecha**; de paso desaparece la recomposiciÃƒÆ’Ã‚Â³n periÃƒÆ’Ã‚Â³dica que provocaban. TamaÃƒÆ’Ã‚Â±os reducidos y `statusBars` respetadas.
- **Eliminada la pestaÃƒÆ’Ã‚Â±a y la tarjeta Premium**, que no aportaba funcionalidad.
- Chips de bÃƒÆ’Ã‚Âºsqueda en `LazyRow` para que nunca se corten.
- Hero banner con altura proporcional al ancho (170ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“320 dp) en lugar de 230 dp fijos.
- Campos de bÃƒÆ’Ã‚Âºsqueda con ancho mÃƒÆ’Ã‚Â¡ximo en pantallas grandes.

### Continuar viendo

- El banner promocional se reemplazÃƒÆ’Ã‚Â³ por una **cuadrÃƒÆ’Ã‚Â­cula adaptativa** (1ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“5 columnas segÃƒÆ’Ã‚Âºn el ancho) con los ÃƒÆ’Ã‚Âºltimos 8 tÃƒÆ’Ã‚Â­tulos del historial.
- Mezcla el historial IPTV (con progreso y reanudaciÃƒÆ’Ã‚Â³n) y el historial NSRPlay/Vimeus (sin minuto, porque sus reproductores web no lo exponen). Los tÃƒÆ’Ã‚Â­tulos de proveedor llevan su etiqueta (`NSRPLAY` cian / `VIMEUS` naranja) y al pulsarlos se abre su detalle para elegir servidor.
- **Borrar historial** en Ajustes elimina los historiales IPTV, NSRPlay y Vimeus.
- Cada tarjeta muestra carÃƒÆ’Ã‚Â¡tula 16:9, tipo, progreso, porcentaje y tiempo restante cuando existe.
- Al pulsar una tarjeta IPTV, el reproductor arranca en la **posiciÃƒÆ’Ã‚Â³n guardada**; no vuelve al inicio.
- La posiciÃƒÆ’Ã‚Â³n se actualiza cada 500 ms en memoria y el historial se persiste cada 1,5 s para no castigar el almacenamiento en equipos de gama baja.
- Al cambiar de canal/pelÃƒÆ’Ã‚Â­cula dentro del reproductor tambiÃƒÆ’Ã‚Â©n se reanuda el tÃƒÆ’Ã‚Â­tulo si tenÃƒÆ’Ã‚Â­a progreso.

### Etiquetas de contenido visto

- Las tarjetas IPTV se marcan como `VISTO` cuando el tÃƒÆ’Ã‚Â­tulo aparece en el historial de reproducciÃƒÆ’Ã‚Â³n.
- Los tÃƒÆ’Ã‚Â­tulos NSRPlay y Vimeus se marcan al abrir un servidor de reproducciÃƒÆ’Ã‚Â³n; las claves `nsr:<tipo>:<slug>` y `vimeus:<tipo>:<id>` se guardan en las preferencias de la app.
- La etiqueta aparece en PelÃƒÆ’Ã‚Â­culas, Series, la bÃƒÆ’Ã‚Âºsqueda global, el historial y los rankings.
- **Borrar historial** en Ajustes elimina tambiÃƒÆ’Ã‚Â©n las marcas de proveedores.

### Rendimiento y fluidez

- Las listas y canales guardados se leen en segundo plano: la primera pantalla aparece sin esperar a parsear miles de canales. Television, PelÃƒÆ’Ã‚Â­culas, Series y Listas muestran un estado de carga mientras llegan.
- Las pestaÃƒÆ’Ã‚Â±as cambian de forma directa, sin `Crossfade`; antes se mantenÃƒÆ’Ã‚Â­an dos ÃƒÆ’Ã‚Â¡rboles de UI y se recargaban imÃƒÆ’Ã‚Â¡genes durante la transiciÃƒÆ’Ã‚Â³n.
- La bÃƒÆ’Ã‚Âºsqueda precalcula las tarjetas de pÃƒÆ’Ã‚Â³sters con `remember` y el carrusel mueve escala/rotaciÃƒÆ’Ã‚Â³n dentro de `graphicsLayer`, sin recomponer todas las pÃƒÆ’Ã‚Â¡ginas en cada frame.
- El reflejo solo se dibuja en las tarjetas cercanas al centro y el Pager mantiene una sola pÃƒÆ’Ã‚Â¡gina extra a cada lado.
- Los grupos de categorÃƒÆ’Ã‚Â­as se calculan una sola vez (`groupBy`) en lugar de filtrar toda la lista por categorÃƒÆ’Ã‚Â­a en cada composiciÃƒÆ’Ã‚Â³n.
- El reproductor actualiza la posiciÃƒÆ’Ã‚Â³n cada 1 s y guarda el historial cada 3 s, reduciendo recomposiciones y escrituras en equipos lentos.
- Los episodios de Vimeus se cachean 5 minutos para no repetir la paginaciÃƒÆ’Ã‚Â³n al volver al detalle.
- Todos los proveedores comparten dos `OkHttpClient` (API y sugerencias IMDb) en vez de crear pools de conexiones por pantalla.
- Coil usa cachÃƒÆ’Ã‚Â© de memoria al 15 %, cachÃƒÆ’Ã‚Â© de disco de 128 MB, sin crossfade y respetando menos las cabeceras para reutilizar pÃƒÆ’Ã‚Â³sters.
- Las filas de proveedores cargan escalonadas (250ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“300 ms) para no lanzar 3ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“5 peticiones a la vez al entrar a PelÃƒÆ’Ã‚Â­culas/Series.
- Las listas usan claves estables (`key`) para evitar recomponer pÃƒÆ’Ã‚Â³sters al actualizarse el historial o los favoritos.

### NavegaciÃƒÆ’Ã‚Â³n con control remoto / teclado

- Los pÃƒÆ’Ã‚Â³sters del carrusel son enfocables: borde blanco al enfocar, izquierda/derecha para recorrerlos y Enter/OK para abrir.
- En la bÃƒÆ’Ã‚Âºsqueda, **Abajo** desde el campo de texto lleva a los filtros y otro **Abajo** al carrusel; asÃƒÆ’Ã‚Â­ no hace falta tocar la pantalla.
- PestaÃƒÆ’Ã‚Â±as de Ajustes, temporadas/episodios de Vimeus y chips/episodios de NSRPlay tambiÃƒÆ’Ã‚Â©n muestran foco y responden al D-pad.
- El foco se centra automÃƒÆ’Ã‚Â¡ticamente en el pÃƒÆ’Ã‚Â³ster seleccionado.

### BÃƒÆ’Ã‚Âºsqueda visual

- Los resultados se muestran como un carrusel de pÃƒÆ’Ã‚Â³sters (Pager) en lugar de filas de lista.
- La tarjeta central se agranda, recibe el color del proveedor, muestra el tÃƒÆ’Ã‚Â­tulo y un reflejo debajo; las laterales se reducen e inclinan.
- Tocar una tarjeta lateral la centra; tocar la central abre el detalle IPTV/Vimeus/NSRPlay.
- El orden de las secciones es **IPTV ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ Vimeus ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ NSRPlay ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ Unlimplay**.
- Las filas de Unlimplay tambiÃƒÆ’Ã‚Â©n quedan al final de PelÃƒÆ’Ã‚Â­culas y Series, despuÃƒÆ’Ã‚Â©s del contenido local.
- A la derecha de los filtros hay un deslizador con escala **A pequeÃƒÆ’Ã‚Â±a / A grande** para ajustar el ancho de los pÃƒÆ’Ã‚Â³sters (112ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“210 dp, limitado al 55 % del ancho de pantalla). El valor se guarda en las preferencias y tambiÃƒÆ’Ã‚Â©n se incluye en el respaldo.
- El deslizador de tamaÃƒÆ’Ã‚Â±o se guarda en preferencias y tambiÃƒÆ’Ã‚Â©n en el respaldo.
- Tras buscar, la barra desaparece; la lupa compacta de la fila de filtros la restaura.
- La vista es estÃƒÆ’Ã‚Â¡tica (sin animaciones de fondo ni blur) para no perder fluidez en gama baja.

### Extensiones Chrome/Edge

- PestaÃƒÆ’Ã‚Â±a **Extensiones** en Ajustes: instalar `.zip`/`.crx`, activar/desactivar y eliminar.
- Lector de paquetes con soporte de `manifest.json` en raÃƒÆ’Ã‚Â­z o subcarpeta y cabecera CRX2/CRX3.
- Motor de content scripts + shim de `chrome.runtime`, `chrome.storage`, `GM_addStyle`, `GM_setValue` y `GM_getValue`.
- Filtro de red a partir de reglas `declarativeNetRequest`: dominios, `urlFilter`, `regexFilter`, block/allow/redirect/upgradeScheme.
- InyecciÃƒÆ’Ã‚Â³n al confirmar la pÃƒÆ’Ã‚Â¡gina (`onPageCommitVisible`) y al terminar, con guard para no ejecutar dos veces.

### NSRPlay + Serchwebview

- Sin pestaÃƒÆ’Ã‚Â±a NSRPlay: sus tÃƒÆ’Ã‚Â­tulos se muestran dentro de **PelÃƒÆ’Ã‚Â­culas**, **Series** y la **bÃƒÆ’Ã‚Âºsqueda global**, con insignia `NSRPLAY` en cada tarjeta.
- CachÃƒÆ’Ã‚Â© en memoria de 5 minutos para el catÃƒÆ’Ã‚Â¡logo (compartida entre pestaÃƒÆ’Ã‚Â±as) y carga de mÃƒÆ’Ã‚Â¡s tÃƒÆ’Ã‚Â­tulos bajo demanda; asÃƒÆ’Ã‚Â­ cambiar de pestaÃƒÆ’Ã‚Â±a no vuelve a pedir el catÃƒÆ’Ã‚Â¡logo ni muestra un spinner innecesario.
- BÃƒÆ’Ã‚Âºsqueda global unificada: resultados de IPTV y NSRPlay en la misma lista. La bÃƒÆ’Ã‚Âºsqueda local sigue siendo instantÃƒÆ’Ã‚Â¡nea; NSRPlay se consulta al pulsar la **lupa** o **Enter** (asÃƒÆ’Ã‚Â­ respetamos su lÃƒÆ’Ã‚Â­mite de 5 bÃƒÆ’Ã‚Âºsquedas/minuto).
- Cuando el endpoint de bÃƒÆ’Ã‚Âºsqueda de NSRPlay devuelve vacÃƒÆ’Ã‚Â­o, la app usa las sugerencias pÃƒÆ’Ã‚Âºblicas de IMDb para convertir una bÃƒÆ’Ã‚Âºsqueda parcial en el tÃƒÆ’Ã‚Â­tulo completo (ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œsaulÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œBetter Call SaulÃƒÂ¢Ã¢â€šÂ¬Ã‚Â), prueba el slug en `/content/info` y, si tampoco hay resultado, busca en la primera pÃƒÆ’Ã‚Â¡gina del catÃƒÆ’Ã‚Â¡logo. IMDb tambiÃƒÆ’Ã‚Â©n aporta el pÃƒÆ’Ã‚Â³ster cuando NSRPlay lo devuelve vacÃƒÆ’Ã‚Â­o.
- CachÃƒÆ’Ã‚Â© de bÃƒÆ’Ã‚Âºsquedas de 5 minutos: repetir la misma consulta es instantÃƒÆ’Ã‚Â¡neo.
- Ajustes con pestaÃƒÆ’Ã‚Â±as **General / NSRPlay / Vimeus / Extensiones / Respaldo**: claves, URL, guardar, probar conexiÃƒÆ’Ã‚Â³n y restablecer sin tocar el cÃƒÆ’Ã‚Â³digo.
- Cliente HTTP propio y liviano (`NsrPlayRepository`) con mensajes de error claros para 401/404/429/5xx.
- Detalle de pelÃƒÆ’Ã‚Â­cula/serie y selector de servidores por episodio.
- `PlayerWebActivity` porta el motor de Serchwebview: `PlayerAdBlocker`, script de auto-skip, fullscreen nativo, velocidad, UA de escritorio, PiP y bloqueo de ventanas emergentes.

### Vimeus

- Cliente `VimeusRepository` para `/api/listing/movies`, `/series`, `/animes` y `/episodes` con header `X-API-Key`.
- Filas **Vimeus Ãƒâ€šÃ‚Â· PelÃƒÆ’Ã‚Â­culas**, **Vimeus Ãƒâ€šÃ‚Â· Series** y **Vimeus Ãƒâ€šÃ‚Â· Anime** dentro de las pestaÃƒÆ’Ã‚Â±as correspondientes, con paginaciÃƒÆ’Ã‚Â³n y cachÃƒÆ’Ã‚Â© de 5 minutos.
- BÃƒÆ’Ã‚Âºsqueda parcial por IMDb (sin endpoint de bÃƒÆ’Ã‚Âºsqueda en Vimeus) que construye los embeds `/e/movie`, `/e/serie` y `/e/anime` con `view_key`.
- Detalle con pÃƒÆ’Ã‚Â³ster, sinopsis, temporadas y episodios; cada episodio abre el embed con `se` y `ep`. Si la API entrega `embed_url`, se usa su `view_key` oficial.
- Historial y etiqueta `VISTO` con la clave `vimeus:<tipo>:<id>`.

### Unlimplay

- `UnlimplayRepository` usa la API v3 de TMDB para tendencias, anime y bÃƒÆ’Ã‚Âºsqueda multi-tÃƒÆ’Ã‚Â­tulo.
- Filas **Unlimplay Ãƒâ€šÃ‚Â· PelÃƒÆ’Ã‚Â­culas**, **Unlimplay Ãƒâ€šÃ‚Â· Series** y **Unlimplay Ãƒâ€šÃ‚Â· Anime** con paginaciÃƒÆ’Ã‚Â³n y cachÃƒÆ’Ã‚Â© de 5 minutos.
- BÃƒÆ’Ã‚Âºsqueda global con badge `UNLIMPLAY` y detalle con pÃƒÆ’Ã‚Â³ster, sinopsis y aÃƒÆ’Ã‚Â±o.
- ReproducciÃƒÆ’Ã‚Â³n con el iframe completo del ejemplo: `https://unlimplay.com/f/embed/movie/{tmdb_id}` y `/f/embed/tv/{tmdb_id}/{temporada}/{episodio}`. El reproductor se carga dentro de un `<iframe>` con `allowfullscreen` y `referrerpolicy="origin"`, no como URL principal.
- Unlimplay se abre con un User-Agent mÃƒÆ’Ã‚Â³vil de Chrome porque su verificaciÃƒÆ’Ã‚Â³n rechaza el WebView detectado; esto evita la pantalla `ERR_TOKEN_EXPIRED` y muestra el reproductor/iframe real.
- Los servidores aparecen dentro del iframe, no se extraen ni se guardan.
- El detalle de series/anime consulta temporadas y episodios en TMDB y actualiza el embed al elegir temporada/episodio.
- Las pelÃƒÆ’Ã‚Â­culas usan la ruta del ejemplo (`/f/embed/movie/{id}`). Si Unlimplay devuelve su pantalla de token/Cloudflare, es una restricciÃƒÆ’Ã‚Â³n del proveedor; la app no puede saltarla sin extraer servidores.
- Historial y etiqueta `VISTO` con la clave `unlimplay:<tipo>:<id>`.

### Respaldo entre dispositivos

- PestaÃƒÆ’Ã‚Â±a **Respaldo** en Ajustes con **Exportar**, **Compartir** e **Importar**.
- Exporta `JSON` con listas M3U por URL, cuentas Xtream, tema, buffer, PIN, categorÃƒÆ’Ã‚Â­as bloqueadas, favoritos y claves de NSRPlay/Vimeus/Unlimplay.
- Al importar, las listas M3U/Xtream se vuelven a descargar y se omite cualquier lista duplicada. Las listas pegadas como texto se avisan porque no se pueden reconstruir.
- El archivo contiene credenciales y API keys: se recomienda compartirlo solo por canales seguros.

## Estructura relevante

```
app/src/main/java/com/example/
ÃƒÂ¢Ã¢â‚¬ÂÃ…â€œÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ data/nsrplay/           # modelos, cliente, configuraciÃƒÆ’Ã‚Â³n y cachÃƒÆ’Ã‚Â© de NSRPlay
ÃƒÂ¢Ã¢â‚¬ÂÃ…â€œÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ data/vimeus/            # modelos, cliente, configuraciÃƒÆ’Ã‚Â³n y cachÃƒÆ’Ã‚Â© de Vimeus
ÃƒÂ¢Ã¢â‚¬ÂÃ…â€œÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ data/backup/            # exportaciÃƒÆ’Ã‚Â³n/importaciÃƒÆ’Ã‚Â³n de listas y ajustes
ÃƒÂ¢Ã¢â‚¬ÂÃ…â€œÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ ui/components/          # AdaptiveDialog, PIN, filtros, cabecera, pÃƒÆ’Ã‚Â³sters y filas de proveedores
ÃƒÂ¢Ã¢â‚¬ÂÃ…â€œÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ ui/screens/NsrPlayDetailScreen.kt
ÃƒÂ¢Ã¢â‚¬ÂÃ…â€œÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ ui/screens/VimeusDetailScreen.kt
ÃƒÂ¢Ã¢â‚¬ÂÃ…â€œÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ ui/screens/BackupSettings.kt
ÃƒÂ¢Ã¢â‚¬ÂÃ…â€œÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ webplayer/              # PlayerWebActivity + PlayerAdBlocker (Serchwebview)
ÃƒÂ¢Ã¢â‚¬ÂÃ¢â‚¬ÂÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ MainActivity.kt
```

## VerificaciÃƒÆ’Ã‚Â³n realizada

- `:app:assembleDebug` compila y genera APK.
- 35 pruebas unitarias pasan (`ExampleUnitTest`, `ExampleRobolectricTest`, `NsrPlayTextTest`, `NsrPlayConfigStoreTest`, `ExtensionEngineTest`, `WatchedContentTest`, `VimeusBackupTest`).
- UI probada en 1920ÃƒÆ’Ã¢â‚¬â€1080 (tablet/TV), 1080ÃƒÆ’Ã¢â‚¬â€1920 a 420 dpi (telÃƒÆ’Ã‚Â©fono vertical) y 1920ÃƒÆ’Ã¢â‚¬â€1080 a 420 dpi (telÃƒÆ’Ã‚Â©fono horizontal): cabecera, botÃƒÆ’Ã‚Â³n de listas, diÃƒÆ’Ã‚Â¡logo de alta y reproductor web.
- NSRPlay verificado en producciÃƒÆ’Ã‚Â³n dentro de PelÃƒÆ’Ã‚Â­culas (badges `NSRPLAY`) y en la bÃƒÆ’Ã‚Âºsqueda global.
- Vimeus verificado en producciÃƒÆ’Ã‚Â³n: catÃƒÆ’Ã‚Â¡logo de pelÃƒÆ’Ã‚Â­culas, series y anime, detalle de serie con temporadas/episodios, reproducciÃƒÆ’Ã‚Â³n embed y bÃƒÆ’Ã‚Âºsqueda ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œsaulÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œBetter Call SaulÃƒÂ¢Ã¢â€šÂ¬Ã‚Â.
- Unlimplay verificado con TMDB real: filas de pelÃƒÆ’Ã‚Â­culas/series/anime, bÃƒÆ’Ã‚Âºsqueda ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œsaulÃƒÂ¢Ã¢â€šÂ¬Ã‚Â, temporadas/episodios y embed dentro de iframe. Con User-Agent mÃƒÆ’Ã‚Â³vil se mostrÃƒÆ’Ã‚Â³ el reproductor en lugar del error de token.
- BÃƒÆ’Ã‚Âºsqueda visual verificada con ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œtitanicÃƒÂ¢Ã¢â€šÂ¬Ã‚Â y ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œsaulÃƒÂ¢Ã¢â€šÂ¬Ã‚Â: carrusel de pÃƒÆ’Ã‚Â³sters, orden IPTV ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ Vimeus ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ NSRPlay, borde de acento y reflejo.
- Deslizador de tamaÃƒÆ’Ã‚Â±o verificado: cambia el pÃƒÆ’Ã‚Â³ster en vivo y conserva el tamaÃƒÆ’Ã‚Â±o tras cerrar y abrir la app.
- Barra de bÃƒÆ’Ã‚Âºsqueda oculta/restaurada verificada con toque y D-pad; al ocultarse, el foco pasa a los filtros.
- ÃƒÆ’Ã…Â¡ltimo canal TV verificado tras cerrar y abrir la app; el canal y su categorÃƒÆ’Ã‚Â­a se restauran.
- NavegaciÃƒÆ’Ã‚Â³n D-pad verificada en la bÃƒÆ’Ã‚Âºsqueda: campo ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ filtros ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ carrusel, izquierda/derecha entre pÃƒÆ’Ã‚Â³sters y Enter abre el detalle.
- Arranque en frÃƒÆ’Ã‚Â­o medido en el emulador con listas IPTV cargadas: ~1,7 s (`am start -W`), con canales cargados en segundo plano.
- Respaldo verificado exportando a `Download`, leyendo el JSON e importÃƒÆ’Ã‚Â¡ndolo de vuelta.
- BÃƒÆ’Ã‚Âºsqueda por slug y por sugerencias verificada con ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œChappieÃƒÂ¢Ã¢â€šÂ¬Ã‚Â y ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œsaulÃƒÂ¢Ã¢â€šÂ¬Ã‚Â (encuentra ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œBetter Call SaulÃƒÂ¢Ã¢â€šÂ¬Ã‚Â con pÃƒÆ’Ã‚Â³ster y episodios).
- La configuraciÃƒÆ’Ã‚Â³n NSRPlay se probÃƒÆ’Ã‚Â³ de extremo a extremo: URL de mock, "Probar conexiÃƒÆ’Ã‚Â³n" (respuesta real), guardado y apertura del catÃƒÆ’Ã‚Â¡logo con esa configuraciÃƒÆ’Ã‚Â³n.

## Aviso

BloodersTv no aloja, sube ni distribuye contenido. Las listas IPTV son responsabilidad del usuario y NSRPlay indexa enlaces publicados por terceros. Verifica que tu uso cumpla las leyes de tu paÃƒÆ’Ã‚Â­s y las condiciones de cada proveedor.

- **Vimeus con episodios**: usa `imdb_id` cuando el tÃ­tulo viene de la bÃºsqueda por IMDb, por lo que las series muestran temporadas y capÃ­tulos como NSRPlay y Unlimplay.
- **BÃºsqueda NSRPlay mÃ¡s rÃ¡pida**: los resultados de IMDb se muestran directamente como tarjetas y el detalle se carga solo al abrir el tÃ­tulo, eliminando varias llamadas lentas a `/content/info` durante la bÃºsqueda.
- **Ad Accelerator predeterminada**: extensiÃ³n activa por defecto que acelera el video del anuncio, lo silencia y pulsa Â«Saltar anuncioÂ» en cuanto aparece; se eliminÃ³ Dark Reader.
- **CorrecciÃ³n de crash al abrir el reproductor**: `shouldInterceptRequest` usaba `WebView.getUrl()` desde un hilo de red; ahora usa la URL cacheada en el hilo principal.
- **TV Box sin carga infinita**: no se inyectan extensiones en todos los frames en equipos de poca RAM ni Android antiguo, el auto-skip solo escanea el DOM cuando hay un anuncio y la superposiciÃ³n de carga se retira a los 20 s.
- **Siguiente episodio automÃ¡tico**: NSRPlay, Vimeus y Unlimplay reciben la lista de capÃ­tulos; al terminar el video se carga el siguiente en el mismo reproductor, tambiÃ©n dentro de iframes.
- **Velocidad del contenido corregida**: el acelerador solo toca videos de anuncio de hasta 2 minutos y restaura la velocidad original; nunca acelera un episodio completo.
- **VISTO en series web**: NSRPlay, Vimeus y Unlimplay guardan cada episodio reproducido y muestran la etiqueta `VISTO`, tambiÃ©n al avanzar automÃ¡ticamente.
