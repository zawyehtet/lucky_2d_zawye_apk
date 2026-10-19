package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Session
import com.example.ui.theme.*

@Composable
fun AppHeader(
    title: String = "Lucky2d",
    selectedSession: String,
    sessions: List<Session>,
    onSessionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = LuckyHeader,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CloverLogo(size = 36.dp)
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            SessionPill(
                selectedSession = selectedSession,
                sessions = sessions,
                onSessionSelected = onSessionSelected
            )
        }
    }
}

@Composable
fun SessionPill(
    selectedSession: String,
    sessions: List<Session>,
    onSessionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(LuckyCard)
                .border(1.dp, LuckyBorder, RoundedCornerShape(20.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (sessions.isEmpty()) "အကြိမ် မရှိသေးပါ" else selectedSession,
                color = if (sessions.isEmpty()) LuckyTextMuted else LuckyLime,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select Session",
                tint = if (sessions.isEmpty()) LuckyTextMuted else LuckyLime,
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(LuckyCard)
        ) {
            if (sessions.isEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "အကြိမ် မရှိသေးပါ",
                            color = LuckyTextMuted,
                            fontSize = 13.sp
                        )
                    },
                    onClick = { expanded = false }
                )
            } else {
                sessions.forEach { s ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = s.name,
                                color = if (s.name == selectedSession) LuckyLime else Color.White,
                                fontWeight = if (s.name == selectedSession) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onSessionSelected(s.name)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchPill(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "ရှာဖွေရန်...",
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder, color = LuckyTextMuted, fontSize = 13.sp) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = LuckyTextSecondary,
                modifier = Modifier.size(18.dp)
            )
        },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = LuckyCard,
            unfocusedContainerColor = LuckyCard,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .border(1.dp, LuckyBorder, RoundedCornerShape(20.dp))
    )
}

@Composable
fun CircularActionButton(
    icon: ImageVector? = null,
    text: String? = null,
    backgroundColor: Color = LuckyHeader,
    contentColor: Color = Color.White,
    size: Int = 56,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = text ?: "Action",
                tint = contentColor,
                modifier = Modifier.size((size * 0.45).dp)
            )
        } else if (text != null) {
            Text(
                text = text,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

fun Modifier.recordLongPress(
    onLongPress: () -> Unit,
    onClick: (() -> Unit)? = null
): Modifier = this.pointerInput(onLongPress, onClick) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val initialPosition = down.position
        var longPressTriggered = false
        val timeout = withTimeoutOrNull(550) {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id }
                if (change == null || !change.pressed) {
                    break
                }
                val diff = (change.position - initialPosition).getDistance()
                if (diff > viewConfiguration.touchSlop) {
                    break
                }
            }
        }
        if (timeout == null) {
            longPressTriggered = true
            down.consume()
            onLongPress()
        } else {
            val change = currentEvent.changes.firstOrNull { it.id == down.id }
            if (change != null && !change.pressed && !longPressTriggered) {
                val diff = (change.position - initialPosition).getDistance()
                if (diff <= viewConfiguration.touchSlop) {
                    onClick?.invoke()
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmDialog(
    itemName: String = "",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String = "ဖျက်ရန် သေချာပါသလား?",
    message: String? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LuckyCard,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Text(
                text = message ?: if (itemName.isNotBlank()) "$itemName ကို ဖျက်ရန် သေချာပါသလား?" else "ဖျက်ရန် သေချာပါသလား?",
                color = LuckyTextSecondary,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LuckyDanger,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("သေချာသည်", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("မဖျက်ပါ", color = LuckyTextSecondary)
            }
        }
    )
}

@Composable
fun RecordActionMenuDialog(
    title: String,
    onDismiss: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LuckyCard,
        title = {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onEdit != null) {
                    TextButton(
                        onClick = {
                            onDismiss()
                            onEdit()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = LuckyLime)
                            Text("ပြင်ဆင်မည် (Edit)", color = Color.White, fontSize = 15.sp)
                        }
                    }
                }
                TextButton(
                    onClick = {
                        onDismiss()
                        onDelete()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = LuckyDanger)
                        Text("ဖျက်မည် (Delete)", color = LuckyDanger, fontSize = 15.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ပိတ်မည်", color = LuckyTextSecondary)
            }
        }
    )
}

@Composable
fun EmptyStateView(
    message: String = "အချက်အလက် မရှိသေးပါ။ + ကိုနှိပ်ပြီး ထည့်ပါ",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = LuckyTextMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EmptyListPlaceholder(
    text: String = "အချက်အလက် မရှိသေးပါ။ + ကိုနှိပ်ပြီး ထည့်ပါ",
    modifier: Modifier = Modifier
) {
    EmptyStateView(message = text, modifier = modifier)
}

object ShareHelper {
    fun copyToClipboard(context: Context, text: String, label: String = "Lucky2d") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "ကူးယူပြီးပါပြီ (Copied)", Toast.LENGTH_SHORT).show()
    }

    fun shareText(context: Context, text: String, title: String = "ဘောင်ချာ မျှဝေမည်") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(intent, title)
        context.startActivity(chooser)
    }

    fun printText(context: Context, text: String) {
        copyToClipboard(context, text, "Print Receipt")
        Toast.makeText(context, "ပရင့်ထုတ်ရန် အချက်အလက် ကူးယူပြီးပါပြီ", Toast.LENGTH_LONG).show()
    }
}
