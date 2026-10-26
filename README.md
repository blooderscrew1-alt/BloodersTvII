# BloodersTv II

Cliente Android (telÃƒÂ©fono, tablet, TV Box y Android TV) para listas IPTV propias y para el catÃƒÂ¡logo de **NSRPlay**. La prioridad del proyecto es una navegaciÃƒÂ³n fluida incluso en equipos de gama baja: listas ligeras, sin animaciones costosas, sin servicios que no se usan y un reproductor web enfocado.

## QuÃƒÂ© incluye

- **TV en vivo** desde listas M3U/M3U8 y Xtream Codes, con zapping y favoritos.
- **PelÃƒÂ­culas y series** de tus listas IPTV y de NSRPlay, en la misma pestaÃƒÂ±a.
- **NSRPlay integrado** (sin pestaÃƒÂ±a propia):
  - sus tÃƒÂ­tulos aparecen junto a los de IPTV en **PelÃƒÂ­culas**, **Series** y la **bÃƒÂºsqueda global**;
  - cada tarjeta NSRPlay lleva una insignia `NSRPLAY`;
  - bÃƒÂºsqueda explÃƒÂ­cita con la lupa (no se dispara una peticiÃƒÂ³n por cada tecla);
  - detalle con sinopsis, servidores y, en series, temporadas/episodios;
  - cada servidor abre el **reproductor web unificado** basado en Serchwebview.
- **Reproductor web** (`PlayerWebActivity`): WebView con bloqueo de anuncios y pop-ups, pantalla completa HTML5, control de velocidad, modo escritorio, Picture-in-Picture y pantalla siempre encendida.
- **Extensiones Chrome/Edge** en el reproductor: paquetes `.zip` o `.crx` con content scripts (JS/CSS), background/service worker y reglas estÃƒÂ¡ticas `declarativeNetRequest` (block, redirect, upgradeScheme). Se gestionan desde **Ajustes Ã¢â€ â€™ Extensiones**.
- **Vimeus**: catÃƒÂ¡logo de pelÃƒÂ­culas, series y anime con paginaciÃƒÂ³n, bÃƒÂºsqueda por tÃƒÂ­tulo, detalle con temporadas/episodios y reproducciÃƒÂ³n embed. Se configura desde **Ajustes Ã¢â€ â€™ Vimeus**.
- **Unlimplay**: catÃƒÂ¡logo de pelÃƒÂ­culas, series y anime usando TMDB para metadatos/bÃƒÂºsqueda; reproduce mediante los iframes `/f/embed/{tipo}?tmdb=...`. Se configura desde **Ajustes Ã¢â€ â€™ Unlimplay**.
- **BÃƒÂºsqueda global** con vista tipo cover-flow: pÃƒÂ³ster central grande, laterales inclinados, borde de acento, reflejo y tÃƒÂ­tulo/progreso sobre la carÃƒÂ¡tula. Orden: IPTV Ã¢â€ â€™ Vimeus Ã¢â€ â€™ NSRPlay Ã¢â€ â€™ **Unlimplay al final**.
- **Deslizador de tamaÃƒÂ±o de pÃƒÂ³sters** a la derecha de los filtros de bÃƒÂºsqueda; el tamaÃƒÂ±o elegido se guarda y se aplica a los tres carruseles (IPTV, Vimeus y NSRPlay).
- **La barra de bÃƒÂºsqueda se oculta al pulsar Enter/OK o la lupa**, para dejar mÃƒÂ¡s espacio a los resultados. Un icono de lupa a la izquierda de los filtros la vuelve a mostrar y enfoca el campo.
- **Bloqueo reforzado de anuncios de apuestas/casino** en NSRPlay, Vimeus y Unlimplay: incluye SapphireBet, 1xBet, Melbet, Mostbet, 22Bet, BC.Game, Stake, 1Win, Vulkan Vegas y variantes con subdominios numÃƒÂ©ricos.
- **Extensiones en todos los frames**: los scripts de extensiones se inyectan al inicio del documento en el reproductor y en iframes de NSRPlay/Vimeus/Unlimplay mediante `WebViewCompat`; las reglas de red ya se aplicaban a todas las peticiones.
- **Sin lÃ­mite de peso de extensiÃ³n**: se eliminaron los topes de 25 MB por paquete y 4 MB por archivo; el nÃºmero de reglas DNR sigue limitado para evitar bloquear el dispositivo.
- **Acelerar anuncios**: al detectar â€œSaltar anuncio en Nâ€, acelera el video del preroll y los temporizadores de la pÃ¡gina para que el anuncio termine en menos de un segundo; en iframes toca repetidamente la esquina del botÃ³n.
- **NSRPlay sin extracciÃ³n de servidores**: resuelve el tÃ­tulo en TMDB y abre `/embed/movie/{tmdbId}`; cada episodio abre `/embed/tv/{tmdbId}/{temporada}/{episodio}` en su reproductor web.
- **Reproductor web para TV Box**: autoplay al cargar, botÃ³n central y teclas multimedia para play/pausa, retroceder/avanzar 10 s; si el iframe no permite JavaScript se simula un toque central.
- **APK de rendimiento**: la variante `perf` usa R8, eliminaciÃƒÂ³n de recursos y firma de depuraciÃƒÂ³n; reduce el APK de ~22 MB a ~3 MB y el arranque medido en BlueStacks de ~1.9 s a ~0.4 s.
- **Historial de series resumido**: Ã¢â‚¬Å“Continuar viendoÃ¢â‚¬Â muestra solo el ÃƒÂºltimo episodio de cada serie, no todos los capÃƒÂ­tulos; las etiquetas `VISTO` se conservan por separado.
- **Episodios vistos**: el detalle de la serie y la lista lateral del reproductor marcan con `VISTO` los capÃƒÂ­tulos ya reproducidos.
- **Reproductor de series**: muestra una lista lateral de episodios y al terminar un capÃƒÂ­tulo pasa automÃƒÂ¡ticamente al siguiente.
- **Series IPTV agrupadas**: una portada por serie (todas las temporadas juntas); al abrirla se ven temporadas y episodios. El agrupado usa un solo recorrido rÃƒÂ¡pido y se ejecuta fuera del hilo principal.
- La bÃƒÂºsqueda IPTV tambiÃƒÂ©n agrupa todos los capÃƒÂ­tulos en una sola tarjeta.
- **Series IPTV corregidas**: el importador ya no corta en 20 000 elementos (sube a 120 000) y reconoce episodios `1x01`, `S01E01`, Ã¢â‚¬Å“temporada/capÃƒÂ­tulo/episodioÃ¢â‚¬Â, ademÃƒÂ¡s de categorÃƒÂ­as Series/Anime/Novela. Los canales ya importados se reclasifican una vez automÃƒÂ¡ticamente.
- **BÃƒÂºsqueda mÃƒÂ¡s estable en TV Box**: DNS con preferencia IPv4, reintento automÃƒÂ¡tico de peticiones GET y bÃƒÂºsquedas de proveedores escalonadas para evitar timeouts por saturaciÃƒÂ³n.
- **BÃƒÂºsqueda solo al confirmar**: los resultados de IPTV y proveedores aparecen al pulsar OK/Enter o la lupa, no mientras se escribe.
- **Inicio mÃƒÂ¡s ligero**: las secciones generales de PelÃƒÂ­culas y Series solo cargan sugerencias de NSRPlay; Vimeus y Unlimplay siguen disponibles desde la bÃƒÂºsqueda.
- **Respaldo ÃƒÂºnico**: la primera exportaciÃƒÂ³n elige la ubicaciÃƒÂ³n; las siguientes reemplazan ese mismo archivo en vez de crear copias con fecha.
- El respaldo incluye la **ediciÃƒÂ³n de categorÃƒÂ­as**: nombres de lista, categorÃƒÂ­as renombradas, reclasificaciÃƒÂ³n TV/PelÃƒÂ­culas/Series/Adultos y categorÃƒÂ­as ocultas.
- **Editor de listas IPTV**: renombrar la lista, copiar su enlace M3U/Xtream, reclasificar toda la lista o una categorÃƒÂ­a entre TV/PelÃƒÂ­culas/Series/**Adultos**, ocultar/mostrar categorÃƒÂ­as y renombrarlas.
- **PestaÃƒÂ±a Adultos** protegida con PIN, separada de TV/PelÃƒÂ­culas/Series y de la bÃƒÂºsqueda general; el PIN se pide una sola vez por sesiÃƒÂ³n de la app.
- **Nuevo icono BloodersTV II** en todas las densidades, con fondo adaptativo negro y versiÃƒÂ³n redonda.
- **PelÃƒÂ­culas como pestaÃƒÂ±a inicial**: al abrir la app se entra directamente a PelÃƒÂ­culas en lugar de TelevisiÃƒÂ³n.
- **ÃƒÅ¡ltimo canal TV**: al abrir la app se restaura el ÃƒÂºltimo canal en vivo y su categorÃƒÂ­a; tambiÃƒÂ©n se recuerda al cambiar de canal con el control dentro del reproductor.
- **Continuar viendo**: cuadrÃƒÂ­cula dinÃƒÂ¡mica basada en el historial de IPTV **y NSRPlay/Vimeus/Unlimplay**, con carÃƒÂ¡tula, progreso, tiempo restante y reanudaciÃƒÂ³n desde el punto guardado.
- **Respaldo**: exporta, comparte o importa listas M3U/Xtream y ajustes entre dispositivos desde **Ajustes Ã¢â€ â€™ Respaldo**.
- **Etiquetas `VISTO`** en tarjetas de pelÃƒÂ­culas/series y en la bÃƒÂºsqueda. Se basan en el historial IPTV y en un registro persistente para NSRPlay.
- **Ajustes**: tema, cachÃƒÂ©/buffer para conexiones lentas, PIN parental, historial, NSRPlay y extensiones.
- **Sin Firebase, Room, Retrofit, Moshi ni KSP**: se retiraron dependencias que el cÃƒÂ³digo no usaba para reducir tamaÃƒÂ±o, arranque y tiempo de compilaciÃƒÂ³n.

## Configurar NSRPlay

### Desde la app (recomendado)

Abre **Ajustes Ã¢â€ â€™ NSRPlay** y pega la API Key ahÃƒÂ­. TambiÃƒÂ©n puedes cambiar la URL del servicio si usas un proxy. La configuraciÃƒÂ³n se guarda en el dispositivo y se aplica al volver a PelÃƒÂ­culas, Series o la bÃƒÂºsqueda; **no hay que editar cÃƒÂ³digo ni recompilar**.

La pestaÃƒÂ±a incluye:

- campo de API Key con mostrar/ocultar;
- URL del servicio (por defecto `https://nsrplay.space`);
- **Guardar configuraciÃƒÂ³n**;
- **Probar conexiÃƒÂ³n**, que hace una peticiÃƒÂ³n real y muestra cuÃƒÂ¡ntos tÃƒÂ­tulos respondiÃƒÂ³ y cuÃƒÂ¡nto tardÃƒÂ³;
- **Restablecer**, para volver a los valores incluidos en el APK.

Los valores de `.env` son solo los valores por defecto. Si el campo de clave se deja vacÃƒÂ­o, se usa la clave compilada en la app.

### Desde `.env` (para compilar)

La API Key se lee del archivo `.env` (ignorado por git) y se inyecta en `BuildConfig` al compilar. **No se escribe en el cÃƒÂ³digo fuente**.

1. Copia `.env.example` a `.env`.
2. Reemplaza el valor:

```properties
NSRPLAY_API_KEY=tu_clave_real
# Opcional, para usar tu propio proxy:
# NSRPLAY_BASE_URL=https://tu-proxy.example.com
```

3. Recompila la app.

Si no hay clave, las filas de NSRPlay muestran un aviso en lugar de romper la navegaciÃƒÂ³n.

> **Importante para distribuir la app:** cualquier clave incluida en un APK es recuperable por terceros. La documentaciÃƒÂ³n de NSRPlay prohÃƒÂ­be publicar la clave en el cliente. Para una versiÃƒÂ³n pÃƒÂºblica, coloca un proxy propio (una funciÃƒÂ³n serverless basta) que aÃƒÂ±ada el header `X-API-Key`, y apunta la app a ese proxy.

Los endpoints usados son:

| Uso | Endpoint |
| --- | --- |
| CatÃƒÂ¡logo | `GET /api/v1/content/catalog?type=movie\|series&page=N&genre=slug` |
| BÃƒÂºsqueda | `GET /api/v1/content/search?s=texto` |
| Detalle | `GET /api/v1/content/info/{slug}?type=movie\|series` |
| Servidores de episodio | `GET /api/v1/content/servers?slug=...&season=N&episode=N` |

El reproductor abre el `embedUrl` del servidor elegido. La app no resuelve ni almacena enlaces de video, y no guarda el catÃƒÂ¡logo en disco, respetando los tÃƒÂ©rminos de NSRPlay.

## Extensiones del reproductor

Abre **Ajustes Ã¢â€ â€™ Extensiones** y pulsa **Instalar extensiÃƒÂ³n desde archivo**. Se aceptan paquetes `.zip` y `.crx` que contengan `manifest.json`.

Soporte actual:

- `content_scripts` con JS y CSS y patrones `matches`;
- `background.scripts` (MV2) y `background.service_worker` (MV3), inyectados como scripts de pÃƒÂ¡gina;
- reglas estÃƒÂ¡ticas de `declarative_net_request` (`block`, `redirect`, `upgradeScheme`);
- bloqueo de dominios publicitarios y redirecciones, mÃƒÂ¡s el guard de `window.open`/pop-ups del reproductor;
- extensiones integradas: uBlock Video Shield, Anti-Popup y Redirecciones, SponsorBlock, Dark Reader, Video Speed Controller y Force PiP.

Limitaciones: no hay acceso a Chrome Web Store dentro de la app, no se ejecuta un runtime completo de Chrome (algunas APIs `chrome.*`, reglas dinÃƒÂ¡micas y el bloqueo `webRequest` de MV2 no estÃƒÂ¡n disponibles) y los recursos binarios de las extensiones no se sirven. El bloqueo de red funciona con reglas estÃƒÂ¡ticas `declarativeNetRequest` (MV3) y con el filtro de dominios propio de la app. Para instalar, descarga el paquete `.zip`/`.crx` en el dispositivo y selecciÃƒÂ³nalo desde el selector de archivos.

## Compilar

Requisitos: JDK 17+ y Android SDK con `platforms;android-36` y `build-tools;36.0.0`.

```bash
./gradlew :app:assembleDebug       # Linux / macOS
gradlew.bat :app:assembleDebug     # Windows
# APK: app/build/outputs/apk/debug/app-debug.apk

gradlew.bat :app:assemblePerf      # APK final rÃƒÂ¡pido, R8 + recursos reducidos
# APK: app/build/outputs/apk/perf/app-perf.apk
```

El wrapper usa Gradle 9.3.1. TambiÃƒÂ©n puedes abrir el proyecto directamente en Android Studio. El proyecto fija `compileSdk 36.1`, `minSdk 24` y `targetSdk 36`.

## Cambios de esta versiÃƒÂ³n

### MenÃƒÂºs flotantes y proporciones

- Nuevo `AdaptiveDialogFrame`: todos los diÃƒÂ¡logos se miden contra la ventana real, con margen seguro, ancho mÃƒÂ¡ximo y alto mÃƒÂ¡ximo. Ya no usan porcentajes del diÃƒÂ¡logo de plataforma (que en TV quedaban enormes y en telÃƒÂ©fonos pequeÃƒÂ±os se salÃƒÂ­an).
- `AddPlaylistDialog`: formulario con scroll y **footer fijo**; los botones Cancelar/Importar siempre estÃƒÂ¡n visibles. Campos de una lÃƒÂ­nea y control segmentado compacto en lugar de `TabRow`.
- `PlaylistsScreen`: en pantallas angostas el tÃƒÂ­tulo y el botÃƒÂ³n "Agregar Lista" se apilan; antes el botÃƒÂ³n quedaba comprimido a una franja vertical de 10 dp al borde de la pantalla.
- `CategoryFilterDialog`: cabecera compacta, lista con altura mÃƒÂ­nima garantizada y footer que se apila en pantallas angostas.
- `ParentalPinDialog`: teclado y tipografÃƒÂ­a adaptativos; en horizontal muy bajo se compacta y todo el contenido puede desplazarse.
- `TopHeaderBar`: **se eliminaron el reloj y la fecha**; de paso desaparece la recomposiciÃƒÂ³n periÃƒÂ³dica que provocaban. TamaÃƒÂ±os reducidos y `statusBars` respetadas.
- **Eliminada la pestaÃƒÂ±a y la tarjeta Premium**, que no aportaba funcionalidad.
- Chips de bÃƒÂºsqueda en `LazyRow` para que nunca se corten.
- Hero banner con altura proporcional al ancho (170Ã¢â‚¬â€œ320 dp) en lugar de 230 dp fijos.
- Campos de bÃƒÂºsqueda con ancho mÃƒÂ¡ximo en pantallas grandes.

### Continuar viendo

- El banner promocional se reemplazÃƒÂ³ por una **cuadrÃƒÂ­cula adaptativa** (1Ã¢â‚¬â€œ5 columnas segÃƒÂºn el ancho) con los ÃƒÂºltimos 8 tÃƒÂ­tulos del historial.
- Mezcla el historial IPTV (con progreso y reanudaciÃƒÂ³n) y el historial NSRPlay/Vimeus (sin minuto, porque sus reproductores web no lo exponen). Los tÃƒÂ­tulos de proveedor llevan su etiqueta (`NSRPLAY` cian / `VIMEUS` naranja) y al pulsarlos se abre su detalle para elegir servidor.
- **Borrar historial** en Ajustes elimina los historiales IPTV, NSRPlay y Vimeus.
- Cada tarjeta muestra carÃƒÂ¡tula 16:9, tipo, progreso, porcentaje y tiempo restante cuando existe.
- Al pulsar una tarjeta IPTV, el reproductor arranca en la **posiciÃƒÂ³n guardada**; no vuelve al inicio.
- La posiciÃƒÂ³n se actualiza cada 500 ms en memoria y el historial se persiste cada 1,5 s para no castigar el almacenamiento en equipos de gama baja.
- Al cambiar de canal/pelÃƒÂ­cula dentro del reproductor tambiÃƒÂ©n se reanuda el tÃƒÂ­tulo si tenÃƒÂ­a progreso.

### Etiquetas de contenido visto

- Las tarjetas IPTV se marcan como `VISTO` cuando el tÃƒÂ­tulo aparece en el historial de reproducciÃƒÂ³n.
- Los tÃƒÂ­tulos NSRPlay y Vimeus se marcan al abrir un servidor de reproducciÃƒÂ³n; las claves `nsr:<tipo>:<slug>` y `vimeus:<tipo>:<id>` se guardan en las preferencias de la app.
- La etiqueta aparece en PelÃƒÂ­culas, Series, la bÃƒÂºsqueda global, el historial y los rankings.
- **Borrar historial** en Ajustes elimina tambiÃƒÂ©n las marcas de proveedores.

### Rendimiento y fluidez

- Las listas y canales guardados se leen en segundo plano: la primera pantalla aparece sin esperar a parsear miles de canales. Television, PelÃƒÂ­culas, Series y Listas muestran un estado de carga mientras llegan.
- Las pestaÃƒÂ±as cambian de forma directa, sin `Crossfade`; antes se mantenÃƒÂ­an dos ÃƒÂ¡rboles de UI y se recargaban imÃƒÂ¡genes durante la transiciÃƒÂ³n.
- La bÃƒÂºsqueda precalcula las tarjetas de pÃƒÂ³sters con `remember` y el carrusel mueve escala/rotaciÃƒÂ³n dentro de `graphicsLayer`, sin recomponer todas las pÃƒÂ¡ginas en cada frame.
- El reflejo solo se dibuja en las tarjetas cercanas al centro y el Pager mantiene una sola pÃƒÂ¡gina extra a cada lado.
- Los grupos de categorÃƒÂ­as se calculan una sola vez (`groupBy`) en lugar de filtrar toda la lista por categorÃƒÂ­a en cada composiciÃƒÂ³n.
- El reproductor actualiza la posiciÃƒÂ³n cada 1 s y guarda el historial cada 3 s, reduciendo recomposiciones y escrituras en equipos lentos.
- Los episodios de Vimeus se cachean 5 minutos para no repetir la paginaciÃƒÂ³n al volver al detalle.
- Todos los proveedores comparten dos `OkHttpClient` (API y sugerencias IMDb) en vez de crear pools de conexiones por pantalla.
- Coil usa cachÃƒÂ© de memoria al 15 %, cachÃƒÂ© de disco de 128 MB, sin crossfade y respetando menos las cabeceras para reutilizar pÃƒÂ³sters.
- Las filas de proveedores cargan escalonadas (250Ã¢â‚¬â€œ300 ms) para no lanzar 3Ã¢â‚¬â€œ5 peticiones a la vez al entrar a PelÃƒÂ­culas/Series.
- Las listas usan claves estables (`key`) para evitar recomponer pÃƒÂ³sters al actualizarse el historial o los favoritos.

### NavegaciÃƒÂ³n con control remoto / teclado

- Los pÃƒÂ³sters del carrusel son enfocables: borde blanco al enfocar, izquierda/derecha para recorrerlos y Enter/OK para abrir.
- En la bÃƒÂºsqueda, **Abajo** desde el campo de texto lleva a los filtros y otro **Abajo** al carrusel; asÃƒÂ­ no hace falta tocar la pantalla.
- PestaÃƒÂ±as de Ajustes, temporadas/episodios de Vimeus y chips/episodios de NSRPlay tambiÃƒÂ©n muestran foco y responden al D-pad.
- El foco se centra automÃƒÂ¡ticamente en el pÃƒÂ³ster seleccionado.

### BÃƒÂºsqueda visual

- Los resultados se muestran como un carrusel de pÃƒÂ³sters (Pager) en lugar de filas de lista.
- La tarjeta central se agranda, recibe el color del proveedor, muestra el tÃƒÂ­tulo y un reflejo debajo; las laterales se reducen e inclinan.
- Tocar una tarjeta lateral la centra; tocar la central abre el detalle IPTV/Vimeus/NSRPlay.
- El orden de las secciones es **IPTV Ã¢â€ â€™ Vimeus Ã¢â€ â€™ NSRPlay Ã¢â€ â€™ Unlimplay**.
- Las filas de Unlimplay tambiÃƒÂ©n quedan al final de PelÃƒÂ­culas y Series, despuÃƒÂ©s del contenido local.
- A la derecha de los filtros hay un deslizador con escala **A pequeÃƒÂ±a / A grande** para ajustar el ancho de los pÃƒÂ³sters (112Ã¢â‚¬â€œ210 dp, limitado al 55 % del ancho de pantalla). El valor se guarda en las preferencias y tambiÃƒÂ©n se incluye en el respaldo.
- El deslizador de tamaÃƒÂ±o se guarda en preferencias y tambiÃƒÂ©n en el respaldo.
- Tras buscar, la barra desaparece; la lupa compacta de la fila de filtros la restaura.
- La vista es estÃƒÂ¡tica (sin animaciones de fondo ni blur) para no perder fluidez en gama baja.

### Extensiones Chrome/Edge

- PestaÃƒÂ±a **Extensiones** en Ajustes: instalar `.zip`/`.crx`, activar/desactivar y eliminar.
- Lector de paquetes con soporte de `manifest.json` en raÃƒÂ­z o subcarpeta y cabecera CRX2/CRX3.
- Motor de content scripts + shim de `chrome.runtime`, `chrome.storage`, `GM_addStyle`, `GM_setValue` y `GM_getValue`.
- Filtro de red a partir de reglas `declarativeNetRequest`: dominios, `urlFilter`, `regexFilter`, block/allow/redirect/upgradeScheme.
- InyecciÃƒÂ³n al confirmar la pÃƒÂ¡gina (`onPageCommitVisible`) y al terminar, con guard para no ejecutar dos veces.

### NSRPlay + Serchwebview

- Sin pestaÃƒÂ±a NSRPlay: sus tÃƒÂ­tulos se muestran dentro de **PelÃƒÂ­culas**, **Series** y la **bÃƒÂºsqueda global**, con insignia `NSRPLAY` en cada tarjeta.
- CachÃƒÂ© en memoria de 5 minutos para el catÃƒÂ¡logo (compartida entre pestaÃƒÂ±as) y carga de mÃƒÂ¡s tÃƒÂ­tulos bajo demanda; asÃƒÂ­ cambiar de pestaÃƒÂ±a no vuelve a pedir el catÃƒÂ¡logo ni muestra un spinner innecesario.
- BÃƒÂºsqueda global unificada: resultados de IPTV y NSRPlay en la misma lista. La bÃƒÂºsqueda local sigue siendo instantÃƒÂ¡nea; NSRPlay se consulta al pulsar la **lupa** o **Enter** (asÃƒÂ­ respetamos su lÃƒÂ­mite de 5 bÃƒÂºsquedas/minuto).
- Cuando el endpoint de bÃƒÂºsqueda de NSRPlay devuelve vacÃƒÂ­o, la app usa las sugerencias pÃƒÂºblicas de IMDb para convertir una bÃƒÂºsqueda parcial en el tÃƒÂ­tulo completo (Ã¢â‚¬Å“saulÃ¢â‚¬Â Ã¢â€ â€™ Ã¢â‚¬Å“Better Call SaulÃ¢â‚¬Â), prueba el slug en `/content/info` y, si tampoco hay resultado, busca en la primera pÃƒÂ¡gina del catÃƒÂ¡logo. IMDb tambiÃƒÂ©n aporta el pÃƒÂ³ster cuando NSRPlay lo devuelve vacÃƒÂ­o.
- CachÃƒÂ© de bÃƒÂºsquedas de 5 minutos: repetir la misma consulta es instantÃƒÂ¡neo.
- Ajustes con pestaÃƒÂ±as **General / NSRPlay / Vimeus / Extensiones / Respaldo**: claves, URL, guardar, probar conexiÃƒÂ³n y restablecer sin tocar el cÃƒÂ³digo.
- Cliente HTTP propio y liviano (`NsrPlayRepository`) con mensajes de error claros para 401/404/429/5xx.
- Detalle de pelÃƒÂ­cula/serie y selector de servidores por episodio.
- `PlayerWebActivity` porta el motor de Serchwebview: `PlayerAdBlocker`, script de auto-skip, fullscreen nativo, velocidad, UA de escritorio, PiP y bloqueo de ventanas emergentes.

### Vimeus

- Cliente `VimeusRepository` para `/api/listing/movies`, `/series`, `/animes` y `/episodes` con header `X-API-Key`.
- Filas **Vimeus Ã‚Â· PelÃƒÂ­culas**, **Vimeus Ã‚Â· Series** y **Vimeus Ã‚Â· Anime** dentro de las pestaÃƒÂ±as correspondientes, con paginaciÃƒÂ³n y cachÃƒÂ© de 5 minutos.
- BÃƒÂºsqueda parcial por IMDb (sin endpoint de bÃƒÂºsqueda en Vimeus) que construye los embeds `/e/movie`, `/e/serie` y `/e/anime` con `view_key`.
- Detalle con pÃƒÂ³ster, sinopsis, temporadas y episodios; cada episodio abre el embed con `se` y `ep`. Si la API entrega `embed_url`, se usa su `view_key` oficial.
- Historial y etiqueta `VISTO` con la clave `vimeus:<tipo>:<id>`.

### Unlimplay

- `UnlimplayRepository` usa la API v3 de TMDB para tendencias, anime y bÃƒÂºsqueda multi-tÃƒÂ­tulo.
- Filas **Unlimplay Ã‚Â· PelÃƒÂ­culas**, **Unlimplay Ã‚Â· Series** y **Unlimplay Ã‚Â· Anime** con paginaciÃƒÂ³n y cachÃƒÂ© de 5 minutos.
- BÃƒÂºsqueda global con badge `UNLIMPLAY` y detalle con pÃƒÂ³ster, sinopsis y aÃƒÂ±o.
- ReproducciÃƒÂ³n con el iframe completo del ejemplo: `https://unlimplay.com/f/embed/movie/{tmdb_id}` y `/f/embed/tv/{tmdb_id}/{temporada}/{episodio}`. El reproductor se carga dentro de un `<iframe>` con `allowfullscreen` y `referrerpolicy="origin"`, no como URL principal.
- Unlimplay se abre con un User-Agent mÃƒÂ³vil de Chrome porque su verificaciÃƒÂ³n rechaza el WebView detectado; esto evita la pantalla `ERR_TOKEN_EXPIRED` y muestra el reproductor/iframe real.
- Los servidores aparecen dentro del iframe, no se extraen ni se guardan.
- El detalle de series/anime consulta temporadas y episodios en TMDB y actualiza el embed al elegir temporada/episodio.
- Las pelÃƒÂ­culas usan la ruta del ejemplo (`/f/embed/movie/{id}`). Si Unlimplay devuelve su pantalla de token/Cloudflare, es una restricciÃƒÂ³n del proveedor; la app no puede saltarla sin extraer servidores.
- Historial y etiqueta `VISTO` con la clave `unlimplay:<tipo>:<id>`.

### Respaldo entre dispositivos

- PestaÃƒÂ±a **Respaldo** en Ajustes con **Exportar**, **Compartir** e **Importar**.
- Exporta `JSON` con listas M3U por URL, cuentas Xtream, tema, buffer, PIN, categorÃƒÂ­as bloqueadas, favoritos y claves de NSRPlay/Vimeus/Unlimplay.
- Al importar, las listas M3U/Xtream se vuelven a descargar y se omite cualquier lista duplicada. Las listas pegadas como texto se avisan porque no se pueden reconstruir.
- El archivo contiene credenciales y API keys: se recomienda compartirlo solo por canales seguros.

## Estructura relevante

```
app/src/main/java/com/example/
Ã¢â€Å“Ã¢â€â‚¬ data/nsrplay/           # modelos, cliente, configuraciÃƒÂ³n y cachÃƒÂ© de NSRPlay
Ã¢â€Å“Ã¢â€â‚¬ data/vimeus/            # modelos, cliente, configuraciÃƒÂ³n y cachÃƒÂ© de Vimeus
Ã¢â€Å“Ã¢â€â‚¬ data/backup/            # exportaciÃƒÂ³n/importaciÃƒÂ³n de listas y ajustes
Ã¢â€Å“Ã¢â€â‚¬ ui/components/          # AdaptiveDialog, PIN, filtros, cabecera, pÃƒÂ³sters y filas de proveedores
Ã¢â€Å“Ã¢â€â‚¬ ui/screens/NsrPlayDetailScreen.kt
Ã¢â€Å“Ã¢â€â‚¬ ui/screens/VimeusDetailScreen.kt
Ã¢â€Å“Ã¢â€â‚¬ ui/screens/BackupSettings.kt
Ã¢â€Å“Ã¢â€â‚¬ webplayer/              # PlayerWebActivity + PlayerAdBlocker (Serchwebview)
Ã¢â€â€Ã¢â€â‚¬ MainActivity.kt
```

## VerificaciÃƒÂ³n realizada

- `:app:assembleDebug` compila y genera APK.
- 35 pruebas unitarias pasan (`ExampleUnitTest`, `ExampleRobolectricTest`, `NsrPlayTextTest`, `NsrPlayConfigStoreTest`, `ExtensionEngineTest`, `WatchedContentTest`, `VimeusBackupTest`).
- UI probada en 1920Ãƒâ€”1080 (tablet/TV), 1080Ãƒâ€”1920 a 420 dpi (telÃƒÂ©fono vertical) y 1920Ãƒâ€”1080 a 420 dpi (telÃƒÂ©fono horizontal): cabecera, botÃƒÂ³n de listas, diÃƒÂ¡logo de alta y reproductor web.
- NSRPlay verificado en producciÃƒÂ³n dentro de PelÃƒÂ­culas (badges `NSRPLAY`) y en la bÃƒÂºsqueda global.
- Vimeus verificado en producciÃƒÂ³n: catÃƒÂ¡logo de pelÃƒÂ­culas, series y anime, detalle de serie con temporadas/episodios, reproducciÃƒÂ³n embed y bÃƒÂºsqueda Ã¢â‚¬Å“saulÃ¢â‚¬Â Ã¢â€ â€™ Ã¢â‚¬Å“Better Call SaulÃ¢â‚¬Â.
- Unlimplay verificado con TMDB real: filas de pelÃƒÂ­culas/series/anime, bÃƒÂºsqueda Ã¢â‚¬Å“saulÃ¢â‚¬Â, temporadas/episodios y embed dentro de iframe. Con User-Agent mÃƒÂ³vil se mostrÃƒÂ³ el reproductor en lugar del error de token.
- BÃƒÂºsqueda visual verificada con Ã¢â‚¬Å“titanicÃ¢â‚¬Â y Ã¢â‚¬Å“saulÃ¢â‚¬Â: carrusel de pÃƒÂ³sters, orden IPTV Ã¢â€ â€™ Vimeus Ã¢â€ â€™ NSRPlay, borde de acento y reflejo.
- Deslizador de tamaÃƒÂ±o verificado: cambia el pÃƒÂ³ster en vivo y conserva el tamaÃƒÂ±o tras cerrar y abrir la app.
- Barra de bÃƒÂºsqueda oculta/restaurada verificada con toque y D-pad; al ocultarse, el foco pasa a los filtros.
- ÃƒÅ¡ltimo canal TV verificado tras cerrar y abrir la app; el canal y su categorÃƒÂ­a se restauran.
- NavegaciÃƒÂ³n D-pad verificada en la bÃƒÂºsqueda: campo Ã¢â€ â€™ filtros Ã¢â€ â€™ carrusel, izquierda/derecha entre pÃƒÂ³sters y Enter abre el detalle.
- Arranque en frÃƒÂ­o medido en el emulador con listas IPTV cargadas: ~1,7 s (`am start -W`), con canales cargados en segundo plano.
- Respaldo verificado exportando a `Download`, leyendo el JSON e importÃƒÂ¡ndolo de vuelta.
- BÃƒÂºsqueda por slug y por sugerencias verificada con Ã¢â‚¬Å“ChappieÃ¢â‚¬Â y Ã¢â‚¬Å“saulÃ¢â‚¬Â (encuentra Ã¢â‚¬Å“Better Call SaulÃ¢â‚¬Â con pÃƒÂ³ster y episodios).
- La configuraciÃƒÂ³n NSRPlay se probÃƒÂ³ de extremo a extremo: URL de mock, "Probar conexiÃƒÂ³n" (respuesta real), guardado y apertura del catÃƒÂ¡logo con esa configuraciÃƒÂ³n.

## Aviso

BloodersTv no aloja, sube ni distribuye contenido. Las listas IPTV son responsabilidad del usuario y NSRPlay indexa enlaces publicados por terceros. Verifica que tu uso cumpla las leyes de tu paÃƒÂ­s y las condiciones de cada proveedor.

- **Vimeus con episodios**: usa `imdb_id` cuando el título viene de la búsqueda por IMDb, por lo que las series muestran temporadas y capítulos como NSRPlay y Unlimplay.
- **Búsqueda NSRPlay más rápida**: los resultados de IMDb se muestran directamente como tarjetas y el detalle se carga solo al abrir el título, eliminando varias llamadas lentas a `/content/info` durante la búsqueda.
- **Ad Accelerator predeterminada**: extensión activa por defecto que acelera el video del anuncio, lo silencia y pulsa «Saltar anuncio» en cuanto aparece; se eliminó Dark Reader.
- **Vimeus en Android nuevo**: en Vimeus/vimeos no se ocultan sus scripts ni se bloquean sus popups, porque el proveedor muestra “Disable ADBlock” y no reproduce; el auto-skip sigue acelerando y saltando el anuncio.
- **Corrección de crash al abrir el reproductor**: `shouldInterceptRequest` usaba `WebView.getUrl()` desde un hilo de red; ahora usa la URL cacheada en el hilo principal.
- **TV Box sin carga infinita**: no se inyectan extensiones en todos los frames en equipos de poca RAM ni Android antiguo, el auto-skip solo escanea el DOM cuando hay un anuncio y la superposición de carga se retira a los 20 s.
- **Siguiente episodio automático**: NSRPlay, Vimeus y Unlimplay reciben la lista de capítulos; al terminar el video se carga el siguiente en el mismo reproductor, también dentro de iframes.
