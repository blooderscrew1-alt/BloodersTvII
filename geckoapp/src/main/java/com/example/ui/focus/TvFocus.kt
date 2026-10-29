package com.example.ui.focus

import android.view.KeyEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.tvFocusable(
    focusBorderColor: Color = Color(0xFFCCFF00),
    shape: Shape = RoundedCornerShape(8.dp),
    scaleUpFactor: Float = 1.04f,
    borderWidth: Dp = 2.5.dp,
    elevation: Dp = 8.dp,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) scaleUpFactor else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "tv_scale"
    )

    return this
        .scale(scale)
        .then(
            if (isFocused) {
                Modifier
                    .shadow(elevation, shape, spotColor = focusBorderColor, ambientColor = focusBorderColor)
                    .border(BorderStroke(borderWidth, focusBorderColor), shape)
            } else {
                Modifier
            }
        )
        .focusable(interactionSource = interactionSource)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            role = Role.Button,
            onClick = onClick
        )
        .onKeyEvent { keyEvent ->
            if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                when (keyEvent.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_NUMPAD_ENTER,
                    KeyEvent.KEYCODE_BUTTON_A -> {
                        onClick()
                        true
                    }
                    else -> false
                }
            } else {
                false
            }
        }
}
