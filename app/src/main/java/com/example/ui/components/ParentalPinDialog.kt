package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun ParentalPinDialog(
    correctPin: String,
    title: String = "Control Parental Activo",
    subtitle: String = "Ingresa el PIN de 4 dígitos para desbloquear este contenido protegido.",
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
    primaryColor: Color = Color(0xFFCCFF00)
) {
    var enteredPin by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    fun addDigit(digit: String) {
        if (enteredPin.length < 4) {
            hasError = false
            enteredPin += digit
            if (enteredPin.length == 4) {
                if (enteredPin == correctPin) {
                    onSuccess()
                } else {
                    hasError = true
                    enteredPin = ""
                }
            }
        }
    }

    fun removeDigit() {
        if (enteredPin.isNotEmpty()) {
            hasError = false
            enteredPin = enteredPin.dropLast(1)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F1722),
            modifier = Modifier
                .width(360.dp)
                .border(1.5.dp, if (hasError) Color(0xFFFF334B) else primaryColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .testTag("parental_pin_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (hasError) Color(0x33FF334B) else primaryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (hasError) Color(0xFFFF334B) else primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = subtitle,
                    color = Color(0xFF8F9EAE),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // 4 PIN indicator dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    repeat(4) { idx ->
                        val isFilled = idx < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        hasError -> Color(0xFFFF334B)
                                        isFilled -> primaryColor
                                        else -> Color(0xFF1F2C3A)
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (hasError) Color(0xFFFF334B) else primaryColor.copy(alpha = 0.3f),
                                    CircleShape
                                )
                        )
                    }
                }

                if (hasError) {
                    Text(
                        text = "PIN incorrecto. Intenta de nuevo.",
                        color = Color(0xFFFF334B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Numeric Keypad Grid
                val keypadRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "DEL")
                )

                keypadRows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        row.forEach { key ->
                            val interactionSource = remember { MutableInteractionSource() }
                            val isFocused by interactionSource.collectIsFocusedAsState()

                            Box(
                                modifier = Modifier
                                    .size(54.dp, 44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isFocused) primaryColor.copy(alpha = 0.25f) else Color(0xFF162330))
                                    .border(
                                        width = if (isFocused) 2.dp else 1.dp,
                                        color = if (isFocused) primaryColor else Color(0xFF263748),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .focusable(interactionSource = interactionSource)
                                    .clickable(interactionSource = interactionSource, indication = null) {
                                        when (key) {
                                            "C" -> enteredPin = ""
                                            "DEL" -> removeDigit()
                                            else -> addDigit(key)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "DEL") {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Borrar",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Text(
                                        text = key,
                                        color = if (isFocused) primaryColor else Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC0CAD5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancelar", fontSize = 12.sp)
                }
            }
        }
    }
}
