package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryInfo

@Composable
fun CategoryFilterDialog(
    categories: List<CategoryInfo>,
    onDismiss: () -> Unit,
    onConfirmSelection: (Set<String>) -> Unit,
    primaryColor: Color = Color(0xFFCCFF00)
) {
    val selectionMap = remember(categories) {
        mutableStateMapOf<String, Boolean>().apply {
            categories.forEach { this[it.name] = it.isSelected }
        }
    }

    val selectedCount = selectionMap.values.count { it }
    val totalChannels = categories.filter { selectionMap[it.name] == true }.sumOf { it.itemCount }

    AdaptiveDialogFrame(
        onDismiss = onDismiss,
        primaryColor = primaryColor,
        maxWidth = 680.dp,
        maxHeight = 720.dp,
        testTag = "category_filter_dialog"
    ) {
        val compact = maxWidth < 460.dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .padding(if (compact) 14.dp else 18.dp)
        ) {
            // Header (compact, never steals the list's height)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(if (compact) 32.dp else 38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(primaryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterAlt,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(if (compact) 18.dp else 21.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Filtro de Categorías",
                        color = Color.White,
                        fontSize = if (compact) 15.sp else 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Elige solo lo que deseas sincronizar.",
                        color = Color(0xFF8F9EAE),
                        fontSize = if (compact) 10.sp else 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (compact) 10.dp else 14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { categories.forEach { selectionMap[it.name] = true } },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                ) {
                    Text("Todo (${categories.size})", fontSize = 11.sp, maxLines = 1)
                }

                OutlinedButton(
                    onClick = { categories.forEach { selectionMap[it.name] = false } },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC0CAD5)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                ) {
                    Text("Ninguno", fontSize = 11.sp, maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable list with a guaranteed minimum height so at least a few
            // categories remain visible even on short landscape phone screens.
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(min = 72.dp)
                    .fillMaxWidth()
                    .background(Color(0xFF0A0F15), RoundedCornerShape(8.dp))
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(categories, key = { it.name }) { category ->
                    val isChecked = selectionMap[category.name] ?: true

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isChecked) Color(0xFF162330) else Color.Transparent)
                            .clickable { selectionMap[category.name] = !isChecked }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { selectionMap[category.name] = it },
                            modifier = Modifier.size(32.dp),
                            colors = CheckboxDefaults.colors(
                                checkedColor = primaryColor,
                                checkmarkColor = Color.Black,
                                uncheckedColor = Color(0xFF536473)
                            )
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = category.name,
                            color = if (isChecked) Color.White else Color(0xFF8291A0),
                            fontSize = 12.sp,
                            fontWeight = if (isChecked) FontWeight.Medium else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (category.itemCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF223242))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${category.itemCount}",
                                    color = primaryColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (compact) 8.dp else 12.dp))

            // Sticky footer: ALWAYS visible, never pushed out by the list.
            if (compact) {
                Text(
                    text = "$selectedCount de ${categories.size} categorías • $totalChannels canales",
                    color = Color(0xFFA2B4C5),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FooterButtons(
                        primaryColor = primaryColor,
                        onDismiss = onDismiss,
                        onConfirm = {
                            onConfirmSelection(selectionMap.filter { it.value }.keys.toSet())
                        }
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$selectedCount de ${categories.size} categorías elegidas ($totalChannels canales)",
                        color = Color(0xFFA2B4C5),
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    FooterButtons(
                        primaryColor = primaryColor,
                        onDismiss = onDismiss,
                        onConfirm = {
                            onConfirmSelection(selectionMap.filter { it.value }.keys.toSet())
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FooterButtons(
    primaryColor: Color,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onDismiss,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC0CAD5)),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text("Cancelar", fontSize = 11.sp, maxLines = 1)
        }

        Button(
            onClick = onConfirm,
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryColor,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            modifier = Modifier
                .height(36.dp)
                .testTag("confirm_import_categories_button")
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Importar", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
        }
    }
}
