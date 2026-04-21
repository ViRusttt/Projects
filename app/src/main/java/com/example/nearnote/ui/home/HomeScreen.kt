package com.example.nearnote.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nearnote.domain.model.Reminder
import com.example.nearnote.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    reminders: List<Reminder>,
    isDarkMode: Boolean,
    onAddClick: () -> Unit,
    onReminderClick: (Long) -> Unit,
    onToggle: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit,
    onToggleTheme: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("NearNote", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        val activeCount = reminders.count { it.isActive }
                        if (activeCount > 0) {
                            Surface(color = GreenLight, shape = RoundedCornerShape(20.dp)) {
                                Text(
                                    "$activeCount active",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    color = GreenDark,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            if (isDarkMode) Icons.Default.WbSunny else Icons.Default.NightsStay,
                            contentDescription = "Toggle Dark Mode",
                            tint = Green
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = Green,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        if (reminders.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = GrayText, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No reminders yet", color = GrayText, fontSize = 16.sp)
                    Text("Tap + to add a location", color = GrayText, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("All Reminders", fontSize = 12.sp, color = GrayText, fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 4.dp))
                }
                items(reminders, key = { it.id }) { reminder ->
                    SwipeToDeleteCard(
                        reminder = reminder,
                        onClick = { onReminderClick(reminder.id) },
                        onToggle = { onToggle(reminder) },
                        onDelete = { onDelete(reminder) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteCard(
    reminder: Reminder,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateColorAsState(
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart)
                    RedDelete else Color.Transparent,
                label = "swipe_color"
            )
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(color),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete",
                    tint = Color.White, modifier = Modifier.padding(end = 20.dp))
            }
        }
    ) {
        ReminderCard(reminder = reminder, onClick = onClick, onToggle = onToggle)
    }
}

@Composable
fun ReminderCard(reminder: Reminder, onClick: () -> Unit, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(reminder.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(
                reminder.noteText.lines().take(2).joinToString(", "),
                fontSize = 13.sp, color = GrayText, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                DistanceBadge(reminder.radiusMeters)
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = reminder.isActive,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Green)
                )
            }
        }
    }
}

@Composable
fun DistanceBadge(radius: Float) {
    val near = radius <= 500f
    Surface(
        color = if (near) GreenLight else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            "Radius ${radius.toInt()} m",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = if (near) GreenDark else GrayText,
            fontSize = 12.sp
        )
    }
}
