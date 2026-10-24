# BloodersTv II

Cliente Android (telÃ©fono, tablet, TV Box y Android TV) para listas IPTV propias y para el catÃ¡logo de **NSRPlay**. La prioridad del proyecto es una navegaciÃ³n fluida incluso en equipos de gama baja: listas ligeras, sin animaciones costosas, sin servicios que no se usan y un reproductor web enfocado.

## QuÃ© incluye

- **TV en vivo** desde listas M3U/M3U8 y Xtream Codes, con zapping y favoritos.
- **PelÃ­culas y series** de tus listas IPTV y de NSRPlay, en la misma pestaÃ±a.
- **NSRPlay integrado** (sin pestaÃ±a propia):
  - sus tÃ­tulos aparecen junto a los de IPTV en **PelÃ­culas**, **Series** y la **bÃºsqueda global**;
  - cada tarjeta NSRPlay lleva una insignia `NSRPLAY`;
  - bÃºsqueda explÃ­cita con la lupa (no se dispara una peticiÃ³n por cada tecla);
  - detalle con sinopsis, servidores y, en series, temporadas/episodios;
  - cada servidor abre el **reproductor web unificado** basado en Serchwebview.
- **Reproductor web** (`PlayerWebActivity`): WebView con bloqueo de anuncios y pop-ups, pantalla completa HTML5, control de velocidad, modo escritorio, Picture-in-Picture y pantalla siempre encendida.
- **Extensiones Chrome/Edge** en el reproductor: paquetes `.zip` o `.crx` con content scripts (JS/CSS), background/service worker y reglas estÃ¡ticas `declarativeNetRequest` (block, redirect, upgradeScheme). Se gestionan desde **Ajustes â†’ Extensiones**.
- **Vimeus**: catÃ¡logo de pelÃ­culas, series y anime con paginaciÃ³n, bÃºsqueda por tÃ­tulo, detalle con temporadas/episodios y reproducciÃ³n embed. Se configura desde **Ajustes â†’ Vimeus**.
- **Unlimplay**: catÃ¡logo de pelÃ­culas, series y anime usando TMDB para metadatos/bÃºsqueda; reproduce mediante los iframes `/f/embed/{tipo}?tmdb=...`. Se configura desde **Ajustes â†’ Unlimplay**.
- **BÃºsqueda global** con vista tipo cover-flow: pÃ³ster central grande, laterales inclinados, borde de acento, reflejo y tÃ­tulo/progreso sobre la carÃ¡tula. Orden: IPTV â†’ Vimeus â†’ NSRPlay â†’ **Unlimplay al final**.
- **Deslizador de tamaÃ±o de pÃ³sters** a la derecha de los filtros de bÃºsqueda; el tamaÃ±o elegido se guarda y se aplica a los tres carruseles (IPTV, Vimeus y NSRPlay).
- **La barra de bÃºsqueda se oculta al pulsar Enter/OK o la lupa**, para dejar mÃ¡s espacio a los resultados. Un icono de lupa a la izquierda de los filtros la vuelve a mostrar y enfoca el campo.
- **Bloqueo reforzado de anuncios de apuestas/casino** en NSRPlay, Vimeus y Unlimplay: incluye SapphireBet, 1xBet, Melbet, Mostbet, 22Bet, BC.Game, Stake, 1Win, Vulkan Vegas y variantes con subdominios numÃ©ricos.
- **Reproductor web para TV Box**: autoplay al cargar, botón central y teclas multimedia para play/pausa, retroceder/avanzar 10 s; si el iframe no permite JavaScript se simula un toque central.
- **APK de rendimiento**: la variante `perf` usa R8, eliminaciÃ³n de recursos y firma de depuraciÃ³n; reduce el APK de ~22 MB a ~3 MB y el arranque medido en BlueStacks de ~1.9 s a ~0.4 s.
- **Historial de series resumido**: â€œContinuar viendoâ€ muestra solo el Ãºltimo episodio de cada serie, no todos los capÃ­tulos; las etiquetas `VISTO` se conservan por separado.
- **Episodios vistos**: el detalle de la serie y la lista lateral del reproductor marcan con `VISTO` los capÃ­tulos ya reproducidos.
- **Reproductor de series**: muestra una lista lateral de episodios y al terminar un capÃ­tulo pasa automÃ¡ticamente al siguiente.
- **Series IPTV agrupadas**: una portada por serie (todas las temporadas juntas); al abrirla se ven temporadas y episodios. El agrupado usa un solo recorrido rÃ¡pido y se ejecuta fuera del hilo principal.
- La bÃºsqueda IPTV tambiÃ©n agrupa todos los capÃ­tulos en una sola tarjeta.
- **Series IPTV corregidas**: el importador ya no corta en 20 000 elementos (sube a 120 000) y reconoce episodios `1x01`, `S01E01`, â€œtemporada/capÃ­tulo/episodioâ€, ademÃ¡s de categorÃ­as Series/Anime/Novela. Los canales ya importados se reclasifican una vez automÃ¡ticamente.
- **BÃºsqueda mÃ¡s estable en TV Box**: DNS con preferencia IPv4, reintento automÃ¡tico de peticiones GET y bÃºsquedas de proveedores escalonadas para evitar timeouts por saturaciÃ³n.
- **BÃºsqueda solo al confirmar**: los resultados de IPTV y proveedores aparecen al pulsar OK/Enter o la lupa, no mientras se escribe.
- **Inicio mÃ¡s ligero**: las secciones generales de PelÃ­culas y Series solo cargan sugerencias de NSRPlay; Vimeus y Unlimplay siguen disponibles desde la bÃºsqueda.
- **Respaldo Ãºnico**: la primera exportaciÃ³n elige la ubicaciÃ³n; las siguientes reemplazan ese mismo archivo en vez de crear copias con fecha.
- El respaldo incluye la **ediciÃ³n de categorÃ­as**: nombres de lista, categorÃ­as renombradas, reclasificaciÃ³n TV/PelÃ­culas/Series/Adultos y categorÃ­as ocultas.
- **Editor de listas IPTV**: renombrar la lista, copiar su enlace M3U/Xtream, reclasificar toda la lista o una categorÃ­a entre TV/PelÃ­culas/Series/**Adultos**, ocultar/mostrar categorÃ­as y renombrarlas.
- **PestaÃ±a Adultos** protegida con PIN, separada de TV/PelÃ­culas/Series y de la bÃºsqueda general; el PIN se pide una sola vez por sesiÃ³n de la app.
- **Nuevo icono BloodersTV II** en todas las densidades, con fondo adaptativo negro y versiÃ³n redonda.
- **PelÃ­culas como pestaÃ±a inicial**: al abrir la app se entra directamente a PelÃ­culas en lugar de TelevisiÃ³n.
- **Ãšltimo canal TV**: al abrir la app se restaura el Ãºltimo canal en vivo y su categorÃ­a; tambiÃ©n se recuerda al cambiar de canal con el control dentro del reproductor.
- **Continuar viendo**: cuadrÃ­cula dinÃ¡mica basada en el historial de IPTV **y NSRPlay/Vimeus/Unlimplay**, con carÃ¡tula, progreso, tiempo restante y reanudaciÃ³n desde el punto guardado.
- **Respaldo**: exporta, comparte o importa listas M3U/Xtream y ajustes entre dispositivos desde **Ajustes â†’ Respaldo**.
- **Etiquetas `VISTO`** en tarjetas de pelÃ­culas/series y en la bÃºsqueda. Se basan en el historial IPTV y en un registro persistente para NSRPlay.
- **Ajustes**: tema, cachÃ©/buffer para conexiones lentas, PIN parental, historial, NSRPlay y extensiones.
- **Sin Firebase, Room, Retrofit, Moshi ni KSP**: se retiraron dependencias que el cÃ³digo no usaba para reducir tamaÃ±o, arranque y tiempo de compilaciÃ³n.

## Configurar NSRPlay

### Desde la app (recomendado)

Abre **Ajustes â†’ NSRPlay** y pega la API Key ahÃ­. TambiÃ©n puedes cambiar la URL del servicio si usas un proxy. La configuraciÃ³n se guarda en el dispositivo y se aplica al volver a PelÃ­culas, Series o la bÃºsqueda; **no hay que editar cÃ³digo ni recompilar**.

La pestaÃ±a incluye:

- campo de API Key con mostrar/ocultar;
- URL del servicio (por defecto `https://nsrplay.space`);
- **Guardar configuraciÃ³n**;
- **Probar conexiÃ³n**, que hace una peticiÃ³n real y muestra cuÃ¡ntos tÃ­tulos respondiÃ³ y cuÃ¡nto tardÃ³;
- **Restablecer**, para volver a los valores incluidos en el APK.

Los valores de `.env` son solo los valores por defecto. Si el campo de clave se deja vacÃ­o, se usa la clave compilada en la app.

### Desde `.env` (para compilar)

La API Key se lee del archivo `.env` (ignorado por git) y se inyecta en `BuildConfig` al compilar. **No se escribe en el cÃ³digo fuente**.

1. Copia `.env.example` a `.env`.
2. Reemplaza el valor:

```properties
NSRPLAY_API_KEY=tu_clave_real
# Opcional, para usar tu propio proxy:
# NSRPLAY_BASE_URL=https://tu-proxy.example.com
```

3. Recompila la app.

Si no hay clave, las filas de NSRPlay muestran un aviso en lugar de romper la navegaciÃ³n.

> **Importante para distribuir la app:** cualquier clave incluida en un APK es recuperable por terceros. La documentaciÃ³n de NSRPlay prohÃ­be publicar la clave en el cliente. Para una versiÃ³n pÃºblica, coloca un proxy propio (una funciÃ³n serverless basta) que aÃ±ada el header `X-API-Key`, y apunta la app a ese proxy.

Los endpoints usados son:

| Uso | Endpoint |
| --- | --- |
| CatÃ¡logo | `GET /api/v1/content/catalog?type=movie\|series&page=N&genre=slug` |
| BÃºsqueda | `GET /api/v1/content/search?s=texto` |
| Detalle | `GET /api/v1/content/info/{slug}?type=movie\|series` |
| Servidores de episodio | `GET /api/v1/content/servers?slug=...&season=N&episode=N` |

El reproductor abre el `embedUrl` del servidor elegido. La app no resuelve ni almacena enlaces de video, y no guarda el catÃ¡logo en disco, respetando los tÃ©rminos de NSRPlay.

## Extensiones del reproductor

Abre **Ajustes â†’ Extensiones** y pulsa **Instalar extensiÃ³n desde archivo**. Se aceptan paquetes `.zip` y `.crx` que contengan `manifest.json`.

Soporte actual:

- `content_scripts` con JS y CSS y patrones `matches`;
- `background.scripts` (MV2) y `background.service_worker` (MV3), inyectados como scripts de pÃ¡gina;
- reglas estÃ¡ticas de `declarative_net_request` (`block`, `redirect`, `upgradeScheme`);
- bloqueo de dominios publicitarios y redirecciones, mÃ¡s el guard de `window.open`/pop-ups del reproductor;
- extensiones integradas: uBlock Video Shield, Anti-Popup y Redirecciones, SponsorBlock, Dark Reader, Video Speed Controller y Force PiP.

Limitaciones: no hay acceso a Chrome Web Store dentro de la app, no se ejecuta un runtime completo de Chrome (algunas APIs `chrome.*`, reglas dinÃ¡micas y el bloqueo `webRequest` de MV2 no estÃ¡n disponibles) y los recursos binarios de las extensiones no se sirven. El bloqueo de red funciona con reglas estÃ¡ticas `declarativeNetRequest` (MV3) y con el filtro de dominios propio de la app. Para instalar, descarga el paquete `.zip`/`.crx` en el dispositivo y selecciÃ³nalo desde el selector de archivos.

## Compilar

Requisitos: JDK 17+ y Android SDK con `platforms;android-36` y `build-tools;36.0.0`.

```bash
./gradlew :app:assembleDebug       # Linux / macOS
gradlew.bat :app:assembleDebug     # Windows
# APK: app/build/outputs/apk/debug/app-debug.apk

gradlew.bat :app:assemblePerf      # APK final rÃ¡pido, R8 + recursos reducidos
# APK: app/build/outputs/apk/perf/app-perf.apk
```

El wrapper usa Gradle 9.3.1. TambiÃ©n puedes abrir el proyecto directamente en Android Studio. El proyecto fija `compileSdk 36.1`, `minSdk 24` y `targetSdk 36`.

## Cambios de esta versiÃ³n

### MenÃºs flotantes y proporciones

- Nuevo `AdaptiveDialogFrame`: todos los diÃ¡logos se miden contra la ventana real, con margen seguro, ancho mÃ¡ximo y alto mÃ¡ximo. Ya no usan porcentajes del diÃ¡logo de plataforma (que en TV quedaban enormes y en telÃ©fonos pequeÃ±os se salÃ­an).
- `AddPlaylistDialog`: formulario con scroll y **footer fijo**; los botones Cancelar/Importar siempre estÃ¡n visibles. Campos de una lÃ­nea y control segmentado compacto en lugar de `TabRow`.
- `PlaylistsScreen`: en pantallas angostas el tÃ­tulo y el botÃ³n "Agregar Lista" se apilan; antes el botÃ³n quedaba comprimido a una franja vertical de 10 dp al borde de la pantalla.
- `CategoryFilterDialog`: cabecera compacta, lista con altura mÃ­nima garantizada y footer que se apila en pantallas angostas.
- `ParentalPinDialog`: teclado y tipografÃ­a adaptativos; en horizontal muy bajo se compacta y todo el contenido puede desplazarse.
- `TopHeaderBar`: **se eliminaron el reloj y la fecha**; de paso desaparece la recomposiciÃ³n periÃ³dica que provocaban. TamaÃ±os reducidos y `statusBars` respetadas.
- **Eliminada la pestaÃ±a y la tarjeta Premium**, que no aportaba funcionalidad.
- Chips de bÃºsqueda en `LazyRow` para que nunca se corten.
- Hero banner con altura proporcional al ancho (170â€“320 dp) en lugar de 230 dp fijos.
- Campos de bÃºsqueda con ancho mÃ¡ximo en pantallas grandes.

### Continuar viendo

- El banner promocional se reemplazÃ³ por una **cuadrÃ­cula adaptativa** (1â€“5 columnas segÃºn el ancho) con los Ãºltimos 8 tÃ­tulos del historial.
- Mezcla el historial IPTV (con progreso y reanudaciÃ³n) y el historial NSRPlay/Vimeus (sin minuto, porque sus reproductores web no lo exponen). Los tÃ­tulos de proveedor llevan su etiqueta (`NSRPLAY` cian / `VIMEUS` naranja) y al pulsarlos se abre su detalle para elegir servidor.
- **Borrar historial** en Ajustes elimina los historiales IPTV, NSRPlay y Vimeus.
- Cada tarjeta muestra carÃ¡tula 16:9, tipo, progreso, porcentaje y tiempo restante cuando existe.
- Al pulsar una tarjeta IPTV, el reproductor arranca en la **posiciÃ³n guardada**; no vuelve al inicio.
- La posiciÃ³n se actualiza cada 500 ms en memoria y el historial se persiste cada 1,5 s para no castigar el almacenamiento en equipos de gama baja.
- Al cambiar de canal/pelÃ­cula dentro del reproductor tambiÃ©n se reanuda el tÃ­tulo si tenÃ­a progreso.

### Etiquetas de contenido visto

- Las tarjetas IPTV se marcan como `VISTO` cuando el tÃ­tulo aparece en el historial de reproducciÃ³n.
- Los tÃ­tulos NSRPlay y Vimeus se marcan al abrir un servidor de reproducciÃ³n; las claves `nsr:<tipo>:<slug>` y `vimeus:<tipo>:<id>` se guardan en las preferencias de la app.
- La etiqueta aparece en PelÃ­culas, Series, la bÃºsqueda global, el historial y los rankings.
- **Borrar historial** en Ajustes elimina tambiÃ©n las marcas de proveedores.

### Rendimiento y fluidez

- Las listas y canales guardados se leen en segundo plano: la primera pantalla aparece sin esperar a parsear miles de canales. Television, PelÃ­culas, Series y Listas muestran un estado de carga mientras llegan.
- Las pestaÃ±as cambian de forma directa, sin `Crossfade`; antes se mantenÃ­an dos Ã¡rboles de UI y se recargaban imÃ¡genes durante la transiciÃ³n.
- La bÃºsqueda precalcula las tarjetas de pÃ³sters con `remember` y el carrusel mueve escala/rotaciÃ³n dentro de `graphicsLayer`, sin recomponer todas las pÃ¡ginas en cada frame.
- El reflejo solo se dibuja en las tarjetas cercanas al centro y el Pager mantiene una sola pÃ¡gina extra a cada lado.
- Los grupos de categorÃ­as se calculan una sola vez (`groupBy`) en lugar de filtrar toda la lista por categorÃ­a en cada composiciÃ³n.
- El reproductor actualiza la posiciÃ³n cada 1 s y guarda el historial cada 3 s, reduciendo recomposiciones y escrituras en equipos lentos.
- Los episodios de Vimeus se cachean 5 minutos para no repetir la paginaciÃ³n al volver al detalle.
- Todos los proveedores comparten dos `OkHttpClient` (API y sugerencias IMDb) en vez de crear pools de conexiones por pantalla.
- Coil usa cachÃ© de memoria al 15 %, cachÃ© de disco de 128 MB, sin crossfade y respetando menos las cabeceras para reutilizar pÃ³sters.
- Las filas de proveedores cargan escalonadas (250â€“300 ms) para no lanzar 3â€“5 peticiones a la vez al entrar a PelÃ­culas/Series.
- Las listas usan claves estables (`key`) para evitar recomponer pÃ³sters al actualizarse el historial o los favoritos.

### NavegaciÃ³n con control remoto / teclado

- Los pÃ³sters del carrusel son enfocables: borde blanco al enfocar, izquierda/derecha para recorrerlos y Enter/OK para abrir.
- En la bÃºsqueda, **Abajo** desde el campo de texto lleva a los filtros y otro **Abajo** al carrusel; asÃ­ no hace falta tocar la pantalla.
- PestaÃ±as de Ajustes, temporadas/episodios de Vimeus y chips/episodios de NSRPlay tambiÃ©n muestran foco y responden al D-pad.
- El foco se centra automÃ¡ticamente en el pÃ³ster seleccionado.

### BÃºsqueda visual

- Los resultados se muestran como un carrusel de pÃ³sters (Pager) en lugar de filas de lista.
- La tarjeta central se agranda, recibe el color del proveedor, muestra el tÃ­tulo y un reflejo debajo; las laterales se reducen e inclinan.
- Tocar una tarjeta lateral la centra; tocar la central abre el detalle IPTV/Vimeus/NSRPlay.
- El orden de las secciones es **IPTV â†’ Vimeus â†’ NSRPlay â†’ Unlimplay**.
- Las filas de Unlimplay tambiÃ©n quedan al final de PelÃ­culas y Series, despuÃ©s del contenido local.
- A la derecha de los filtros hay un deslizador con escala **A pequeÃ±a / A grande** para ajustar el ancho de los pÃ³sters (112â€“210 dp, limitado al 55 % del ancho de pantalla). El valor se guarda en las preferencias y tambiÃ©n se incluye en el respaldo.
- El deslizador de tamaÃ±o se guarda en preferencias y tambiÃ©n en el respaldo.
- Tras buscar, la barra desaparece; la lupa compacta de la fila de filtros la restaura.
- La vista es estÃ¡tica (sin animaciones de fondo ni blur) para no perder fluidez en gama baja.

### Extensiones Chrome/Edge

- PestaÃ±a **Extensiones** en Ajustes: instalar `.zip`/`.crx`, activar/desactivar y eliminar.
- Lector de paquetes con soporte de `manifest.json` en raÃ­z o subcarpeta y cabecera CRX2/CRX3.
- Motor de content scripts + shim de `chrome.runtime`, `chrome.storage`, `GM_addStyle`, `GM_setValue` y `GM_getValue`.
- Filtro de red a partir de reglas `declarativeNetRequest`: dominios, `urlFilter`, `regexFilter`, block/allow/redirect/upgradeScheme.
- InyecciÃ³n al confirmar la pÃ¡gina (`onPageCommitVisible`) y al terminar, con guard para no ejecutar dos veces.

### NSRPlay + Serchwebview

- Sin pestaÃ±a NSRPlay: sus tÃ­tulos se muestran dentro de **PelÃ­culas**, **Series** y la **bÃºsqueda global**, con insignia `NSRPLAY` en cada tarjeta.
- CachÃ© en memoria de 5 minutos para el catÃ¡logo (compartida entre pestaÃ±as) y carga de mÃ¡s tÃ­tulos bajo demanda; asÃ­ cambiar de pestaÃ±a no vuelve a pedir el catÃ¡logo ni muestra un spinner innecesario.
- BÃºsqueda global unificada: resultados de IPTV y NSRPlay en la misma lista. La bÃºsqueda local sigue siendo instantÃ¡nea; NSRPlay se consulta al pulsar la **lupa** o **Enter** (asÃ­ respetamos su lÃ­mite de 5 bÃºsquedas/minuto).
- Cuando el endpoint de bÃºsqueda de NSRPlay devuelve vacÃ­o, la app usa las sugerencias pÃºblicas de IMDb para convertir una bÃºsqueda parcial en el tÃ­tulo completo (â€œsaulâ€ â†’ â€œBetter Call Saulâ€), prueba el slug en `/content/info` y, si tampoco hay resultado, busca en la primera pÃ¡gina del catÃ¡logo. IMDb tambiÃ©n aporta el pÃ³ster cuando NSRPlay lo devuelve vacÃ­o.
- CachÃ© de bÃºsquedas de 5 minutos: repetir la misma consulta es instantÃ¡neo.
- Ajustes con pestaÃ±as **General / NSRPlay / Vimeus / Extensiones / Respaldo**: claves, URL, guardar, probar conexiÃ³n y restablecer sin tocar el cÃ³digo.
- Cliente HTTP propio y liviano (`NsrPlayRepository`) con mensajes de error claros para 401/404/429/5xx.
- Detalle de pelÃ­cula/serie y selector de servidores por episodio.
- `PlayerWebActivity` porta el motor de Serchwebview: `PlayerAdBlocker`, script de auto-skip, fullscreen nativo, velocidad, UA de escritorio, PiP y bloqueo de ventanas emergentes.

### Vimeus

- Cliente `VimeusRepository` para `/api/listing/movies`, `/series`, `/animes` y `/episodes` con header `X-API-Key`.
- Filas **Vimeus Â· PelÃ­culas**, **Vimeus Â· Series** y **Vimeus Â· Anime** dentro de las pestaÃ±as correspondientes, con paginaciÃ³n y cachÃ© de 5 minutos.
- BÃºsqueda parcial por IMDb (sin endpoint de bÃºsqueda en Vimeus) que construye los embeds `/e/movie`, `/e/serie` y `/e/anime` con `view_key`.
- Detalle con pÃ³ster, sinopsis, temporadas y episodios; cada episodio abre el embed con `se` y `ep`. Si la API entrega `embed_url`, se usa su `view_key` oficial.
- Historial y etiqueta `VISTO` con la clave `vimeus:<tipo>:<id>`.

### Unlimplay

- `UnlimplayRepository` usa la API v3 de TMDB para tendencias, anime y bÃºsqueda multi-tÃ­tulo.
- Filas **Unlimplay Â· PelÃ­culas**, **Unlimplay Â· Series** y **Unlimplay Â· Anime** con paginaciÃ³n y cachÃ© de 5 minutos.
- BÃºsqueda global con badge `UNLIMPLAY` y detalle con pÃ³ster, sinopsis y aÃ±o.
- ReproducciÃ³n con el iframe completo del ejemplo: `https://unlimplay.com/f/embed/movie/{tmdb_id}` y `/f/embed/tv/{tmdb_id}/{temporada}/{episodio}`. El reproductor se carga dentro de un `<iframe>` con `allowfullscreen` y `referrerpolicy="origin"`, no como URL principal.
- Unlimplay se abre con un User-Agent mÃ³vil de Chrome porque su verificaciÃ³n rechaza el WebView detectado; esto evita la pantalla `ERR_TOKEN_EXPIRED` y muestra el reproductor/iframe real.
- Los servidores aparecen dentro del iframe, no se extraen ni se guardan.
- El detalle de series/anime consulta temporadas y episodios en TMDB y actualiza el embed al elegir temporada/episodio.
- Las pelÃ­culas usan la ruta del ejemplo (`/f/embed/movie/{id}`). Si Unlimplay devuelve su pantalla de token/Cloudflare, es una restricciÃ³n del proveedor; la app no puede saltarla sin extraer servidores.
- Historial y etiqueta `VISTO` con la clave `unlimplay:<tipo>:<id>`.

### Respaldo entre dispositivos

- PestaÃ±a **Respaldo** en Ajustes con **Exportar**, **Compartir** e **Importar**.
- Exporta `JSON` con listas M3U por URL, cuentas Xtream, tema, buffer, PIN, categorÃ­as bloqueadas, favoritos y claves de NSRPlay/Vimeus/Unlimplay.
- Al importar, las listas M3U/Xtream se vuelven a descargar y se omite cualquier lista duplicada. Las listas pegadas como texto se avisan porque no se pueden reconstruir.
- El archivo contiene credenciales y API keys: se recomienda compartirlo solo por canales seguros.

## Estructura relevante

```
app/src/main/java/com/example/
â”œâ”€ data/nsrplay/           # modelos, cliente, configuraciÃ³n y cachÃ© de NSRPlay
â”œâ”€ data/vimeus/            # modelos, cliente, configuraciÃ³n y cachÃ© de Vimeus
â”œâ”€ data/backup/            # exportaciÃ³n/importaciÃ³n de listas y ajustes
â”œâ”€ ui/components/          # AdaptiveDialog, PIN, filtros, cabecera, pÃ³sters y filas de proveedores
â”œâ”€ ui/screens/NsrPlayDetailScreen.kt
â”œâ”€ ui/screens/VimeusDetailScreen.kt
â”œâ”€ ui/screens/BackupSettings.kt
â”œâ”€ webplayer/              # PlayerWebActivity + PlayerAdBlocker (Serchwebview)
â””â”€ MainActivity.kt
```

## VerificaciÃ³n realizada

- `:app:assembleDebug` compila y genera APK.
- 33 pruebas unitarias pasan (`ExampleUnitTest`, `ExampleRobolectricTest`, `NsrPlayTextTest`, `NsrPlayConfigStoreTest`, `ExtensionEngineTest`, `WatchedContentTest`, `VimeusBackupTest`).
- UI probada en 1920Ã—1080 (tablet/TV), 1080Ã—1920 a 420 dpi (telÃ©fono vertical) y 1920Ã—1080 a 420 dpi (telÃ©fono horizontal): cabecera, botÃ³n de listas, diÃ¡logo de alta y reproductor web.
- NSRPlay verificado en producciÃ³n dentro de PelÃ­culas (badges `NSRPLAY`) y en la bÃºsqueda global.
- Vimeus verificado en producciÃ³n: catÃ¡logo de pelÃ­culas, series y anime, detalle de serie con temporadas/episodios, reproducciÃ³n embed y bÃºsqueda â€œsaulâ€ â†’ â€œBetter Call Saulâ€.
- Unlimplay verificado con TMDB real: filas de pelÃ­culas/series/anime, bÃºsqueda â€œsaulâ€, temporadas/episodios y embed dentro de iframe. Con User-Agent mÃ³vil se mostrÃ³ el reproductor en lugar del error de token.
- BÃºsqueda visual verificada con â€œtitanicâ€ y â€œsaulâ€: carrusel de pÃ³sters, orden IPTV â†’ Vimeus â†’ NSRPlay, borde de acento y reflejo.
- Deslizador de tamaÃ±o verificado: cambia el pÃ³ster en vivo y conserva el tamaÃ±o tras cerrar y abrir la app.
- Barra de bÃºsqueda oculta/restaurada verificada con toque y D-pad; al ocultarse, el foco pasa a los filtros.
- Ãšltimo canal TV verificado tras cerrar y abrir la app; el canal y su categorÃ­a se restauran.
- NavegaciÃ³n D-pad verificada en la bÃºsqueda: campo â†’ filtros â†’ carrusel, izquierda/derecha entre pÃ³sters y Enter abre el detalle.
- Arranque en frÃ­o medido en el emulador con listas IPTV cargadas: ~1,7 s (`am start -W`), con canales cargados en segundo plano.
- Respaldo verificado exportando a `Download`, leyendo el JSON e importÃ¡ndolo de vuelta.
- BÃºsqueda por slug y por sugerencias verificada con â€œChappieâ€ y â€œsaulâ€ (encuentra â€œBetter Call Saulâ€ con pÃ³ster y episodios).
- La configuraciÃ³n NSRPlay se probÃ³ de extremo a extremo: URL de mock, "Probar conexiÃ³n" (respuesta real), guardado y apertura del catÃ¡logo con esa configuraciÃ³n.

## Aviso

BloodersTv no aloja, sube ni distribuye contenido. Las listas IPTV son responsabilidad del usuario y NSRPlay indexa enlaces publicados por terceros. Verifica que tu uso cumpla las leyes de tu paÃ­s y las condiciones de cada proveedor.
