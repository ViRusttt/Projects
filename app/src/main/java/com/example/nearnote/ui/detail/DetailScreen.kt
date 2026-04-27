package com.example.nearnote.ui.detail

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nearnote.domain.model.Reminder
import com.example.nearnote.service.GeofenceBroadcastReceiver
import com.example.nearnote.ui.add.GoogleMapPicker
import com.google.android.gms.maps.model.LatLng
import com.example.nearnote.ui.theme.*

fun triggerTestNotification(context: Context, reminder: Reminder) {
    val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
        putExtra("test_reminder_id", reminder.id)
        putExtra("test_title", reminder.title)
        putExtra("test_note", reminder.noteText)
    }
    context.sendBroadcast(intent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    reminder: Reminder,
    onBack: () -> Unit,
    onSave: (Reminder) -> Unit,
    onDelete: () -> Unit,
    onSnooze: () -> Unit
) {
    val context = LocalContext.current

    // View mode state
    val noteLines = remember(reminder.noteText) { reminder.noteText.lines().filter { it.isNotBlank() } }
    val checkedStates = remember(reminder.noteText) { mutableStateListOf(*Array(noteLines.size) { false }) }

    // Edit mode state
    var isEditMode by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf(reminder.title) }
    val editItems = remember { mutableStateListOf<String>() }

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete reminder?") },
            text = { Text("Are you sure you want to delete \"${reminder.title}\"?") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Delete", color = RedDelete)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isEditMode) {
                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = { editTitle = it },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 4.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green),
                            shape = RoundedCornerShape(8.dp),
                            label = { Text("ชื่อสถานที่", fontSize = 11.sp) }
                        )
                    } else {
                        Text(reminder.title, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isEditMode) {
                            // ยกเลิก edit — reset กลับค่าเดิม
                            editTitle = reminder.title
                            editItems.clear()
                            editItems.addAll(noteLines)
                            isEditMode = false
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            if (isEditMode) Icons.Default.Close else Icons.Default.ArrowBack,
                            contentDescription = if (isEditMode) "Cancel" else "Back",
                            tint = Green
                        )
                    }
                },
                actions = {
                    if (isEditMode) {
                        TextButton(onClick = {
                            val newNoteText = editItems.filter { it.isNotBlank() }.joinToString("\n")
                            val updated = reminder.copy(
                                title = editTitle.trim().ifBlank { reminder.title },
                                noteText = newNoteText
                            )
                            onSave(updated)
                            isEditMode = false
                        }) {
                            Text("Save", color = Green, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        IconButton(onClick = {
                            editTitle = reminder.title
                            editItems.clear()
                            editItems.addAll(noteLines)
                            isEditMode = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Green)
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Map
            item {
                Spacer(Modifier.height(4.dp))
                Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)) {
                    GoogleMapPicker(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        point = LatLng(reminder.latitude, reminder.longitude),
                        radius = reminder.radiusMeters,
                        onPointSelected = {}
                    )
                }
            }

            // Distance badge
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = GreenLight, shape = RoundedCornerShape(20.dp)) {
                        Text(
                            "Alert Radius ${reminder.radiusMeters.toInt()} m",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            color = GreenDark, fontSize = 13.sp
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${reminder.latitude.format(4)}, ${reminder.longitude.format(4)}",
                        fontSize = 11.sp, color = GrayText
                    )
                }
            }

            // Section label
            item {
                Text(
                    if (isEditMode) "แก้ไข Checklist" else "Checklist",
                    fontSize = 12.sp, color = GrayText, fontWeight = FontWeight.Medium
                )
            }

            if (isEditMode) {
                // ── EDIT MODE ──────────────────────────────────────────────
                itemsIndexed(editItems) { i, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = item,
                            onValueChange = { editItems[i] = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            placeholder = { Text("รายการ...", color = GrayText) }
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { editItems.removeAt(i) }) {
                            Icon(Icons.Default.Delete, contentDescription = "ลบ", tint = RedDelete)
                        }
                    }
                }

                // ปุ่มเพิ่ม item
                item {
                    OutlinedButton(
                        onClick = { editItems.add("") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Green)
                    ) {
                        Text("+ เพิ่มรายการ", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(40.dp))
                }

            } else {
                // ── VIEW MODE ──────────────────────────────────────────────
                if (noteLines.isEmpty()) {
                    item {
                        Text(
                            "ยังไม่มีรายการ — กด ✏️ เพื่อเพิ่ม",
                            fontSize = 13.sp, color = GrayText
                        )
                    }
                } else {
                    itemsIndexed(noteLines) { i, line ->
                        HorizontalDivider(color = Color(0xFFEEEEEE))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { checkedStates[i] = !checkedStates[i] }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (checkedStates[i]) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (checkedStates[i]) Green else GrayText,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                line,
                                fontSize = 15.sp,
                                color = if (checkedStates[i]) GrayText else MaterialTheme.colorScheme.onBackground,
                                textDecoration = if (checkedStates[i]) TextDecoration.LineThrough else null
                            )
                        }
                    }
                    item { HorizontalDivider(color = Color(0xFFEEEEEE)) }
                }

                // Action buttons
                item {
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onSnooze,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GrayText)
                        ) {
                            Text("Snooze 2 hrs", fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RedDelete)
                        ) {
                            Text("Delete", fontSize = 13.sp, color = RedDelete)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TextButton(onClick = { triggerTestNotification(context, reminder) }) {
                            Text("Test Notification", fontSize = 11.sp, color = GrayText)
                        }
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

private fun Double.format(digits: Int) = "%.${digits}f".format(this)
