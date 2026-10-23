package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * One adaptive frame for every floating menu/dialog of the app.
 *
 * The old dialogs used percentages of the platform dialog window (which is already
 * inset on phones and can be huge on TV), so the same form ended up cramped on a
 * small phone and absurdly wide on a TV. This frame measures the real window once,
 * keeps a comfortable margin, caps the width/height and lets each dialog decide how
 * to use the remaining space (scroll, list, or sticky footer).
 */
@Composable
fun AdaptiveDialogFrame(
    onDismiss: () -> Unit,
    primaryColor: Color,
    maxWidth: Dp = 560.dp,
    maxHeight: Dp = 760.dp,
    testTag: String = "adaptive_dialog",
    content: @Composable androidx.compose.foundation.layout.BoxWithConstraintsScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            val dialogWidth = (maxWidth * 0.94f).coerceAtMost(maxWidth)
            val dialogHeight = (maxHeight * 0.92f).coerceAtMost(maxHeight)

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0F1722),
                modifier = Modifier
                    .width(dialogWidth)
                    .heightIn(max = dialogHeight)
                    .border(
                        1.5.dp,
                        primaryColor.copy(alpha = 0.45f),
                        RoundedCornerShape(16.dp)
                    )
                    .testTag(testTag)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    content()
                }
            }
        }
    }
}
