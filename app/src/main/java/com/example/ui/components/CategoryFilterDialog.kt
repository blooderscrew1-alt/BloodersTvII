package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CategoryInfo

@Composable
fun CategoryFilterDialog(
    categories: List<CategoryInfo>,
    onDismiss: () -> Unit,
    onConfirmSelection: (Set<String>) -> Unit,
    primaryColor: Color = Color(0xFFCCFF00)
) {
    val selectionMap = remember {
        mutableStateMapOf<String, Boolean>().apply {
            categories.forEach { this[it.name] = it.isSelected }
        }
    }

    val selectedCount = selectionMap.values.count { it }
    val totalChannels = categories.filter { selectionMap[it.name] == true }.sumOf { it.itemCount }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F1722),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .border(1.5.dp, primaryColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .testTag("category_filter_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(primaryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterAlt,
                            contentDescription = null,
                            tint = primaryColor
                        )
                    }

                    Column {
                        Text(
                            text = "Filtro Inteligente de Categorías",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Selecciona solo lo que deseas sincronizar para ahorrar memoria y datos.",
                            color = Color(0xFF8F9EAE),
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action buttons: Select All / Deselect All
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            categories.forEach { selectionMap[it.name] = true }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Todo (${categories.size})", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            categories.forEach { selectionMap[it.name] = false }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC0CAD5)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Ninguno", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Categories list with count
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF0A0F15), RoundedCornerShape(8.dp))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(categories) { category ->
                        val isChecked = selectionMap[category.name] ?: true

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isChecked) Color(0xFF162330) else Color.Transparent)
                                .clickable {
                                    selectionMap[category.name] = !isChecked
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { selectionMap[category.name] = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = primaryColor,
                                    checkmarkColor = Color.Black,
                                    uncheckedColor = Color(0xFF536473)
                                )
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = category.name,
                                color = if (isChecked) Color.White else Color(0xFF8291A0),
                                fontSize = 13.sp,
                                fontWeight = if (isChecked) FontWeight.Medium else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )

                            if (category.itemCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF223242))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${category.itemCount} canales",
                                        color = primaryColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Summary & Confirm
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$selectedCount de ${categories.size} categorías elegidas ($totalChannels canales)",
                        color = Color(0xFFA2B4C5),
                        fontSize = 11.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC0CAD5))
                        ) {
                            Text("Cancelar", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val selected = selectionMap.filter { it.value }.keys.toSet()
                                onConfirmSelection(selected)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("confirm_import_categories_button")
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Importar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
