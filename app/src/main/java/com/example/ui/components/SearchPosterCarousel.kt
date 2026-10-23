package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlin.math.abs

data class SearchPosterItem(
    val key: String,
    val title: String,
    val posterUrl: String?,
    val typeLabel: String,
    val subtitle: String? = null,
    val isWatched: Boolean = false
)

/**
 * Cover-flow style search results: one large central poster, smaller angled
 * neighbours and a soft reflection. It is a normal Pager, not an infinite
 * animation, so it stays fluid on low-end phones.
 */
@Composable
fun SearchPosterCarousel(
    items: List<SearchPosterItem>,
    accentColor: Color,
    onOpen: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { items.size })
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val cardWidth = (maxWidth * 0.44f).coerceIn(132.dp, 160.dp)
        val cardHeight = cardWidth * 1.42f
        val reflectionHeight = cardWidth * 0.32f

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = (maxWidth - cardWidth) / 2),
            pageSpacing = 10.dp,
            pageSize = PageSize.Fixed(cardWidth),
            beyondViewportPageCount = 2,
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight + reflectionHeight - 8.dp)
        ) { page ->
            val relative = (page - pagerState.currentPage) - pagerState.currentPageOffsetFraction
            val distance = abs(relative).coerceAtMost(1.6f)
            val selected = distance < 0.55f
            val interactionSource = remember(page) { MutableInteractionSource() }
            val isFocused by interactionSource.collectIsFocusedAsState()

            LaunchedEffect(isFocused, page) {
                if (isFocused) {
                    pagerState.animateScrollToPage(page)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex((1f - distance).coerceAtLeast(0.1f))
                    .graphicsLayer {
                        val scale = 1f - (distance * 0.22f)
                        scaleX = scale
                        scaleY = scale
                        alpha = (1f - distance * 0.40f).coerceIn(0.32f, 1f)
                        rotationY = (-relative * 26f).coerceIn(-30f, 30f)
                        translationY = distance * 10.dp.toPx()
                        cameraDistance = 8f * density.density
                    }
                    .focusable(interactionSource = interactionSource)
                    .onKeyEvent { event ->
                        val isSelect = event.type == KeyEventType.KeyDown &&
                            (event.key == Key.DirectionCenter ||
                                event.key == Key.Enter ||
                                event.key == Key.NumPadEnter)
                        if (isSelect) {
                            onOpen(page)
                            true
                        } else {
                            false
                        }
                    }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        if (selected) {
                            onOpen(page)
                        } else {
                            scope.launch { pagerState.animateScrollToPage(page) }
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cardHeight)
                        .shadow(
                            elevation = if (selected) 24.dp else 4.dp,
                            shape = RoundedCornerShape(10.dp),
                            ambientColor = accentColor.copy(alpha = 0.65f),
                            spotColor = accentColor.copy(alpha = 0.65f)
                        )
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF14202C))
                        .then(
                            if (isFocused) {
                                Modifier.border(3.dp, Color.White, RoundedCornerShape(10.dp))
                            } else if (selected) {
                                Modifier.border(3.dp, accentColor, RoundedCornerShape(10.dp))
                            } else {
                                Modifier.border(1.dp, Color(0xFF243343), RoundedCornerShape(10.dp))
                            }
                        )
                ) {
                    if (!items[page].posterUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = items[page].posterUrl,
                            contentDescription = items[page].title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = items[page].title.take(1).uppercase(),
                                color = Color(0xFF53677A),
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color(0x22000000),
                                        Color(0xF2000000)
                                    )
                                )
                            )
                    )

                    if (items[page].isWatched) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(7.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(accentColor)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "VISTO",
                                color = Color.Black,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(9.dp)
                    ) {
                        Text(
                            text = items[page].title,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = listOfNotNull(items[page].typeLabel, items[page].subtitle)
                                .joinToString(" · "),
                            color = Color(0xFFB8C6D3),
                            fontSize = 8.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(reflectionHeight)
                        .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                        .background(Color(0xFF090E13))
                ) {
                    if (!items[page].posterUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = items[page].posterUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(cardHeight)
                                .graphicsLayer {
                                    scaleY = -1f
                                    alpha = 0.32f
                                }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0x33090E13),
                                        Color(0xE6090E13),
                                        Color(0xFF090E13)
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}
