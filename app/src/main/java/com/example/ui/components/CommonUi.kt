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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Session
import com.example.ui.theme.*
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun AppHeader(
    title: String = "Lucky2D",
    selectedSession: String,
    sessions: List<Session>,
    onSessionSelected: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    showSessionPill: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        color = LuckyHeader,
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "နောက်သို့",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                CloverLogo(size = 30.dp)
                Text(
                    text = title,
                    color = Color.White,
                    fontFamily = LuckySansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (showSessionPill) {
                SessionPill(
                    selectedSession = selectedSession,
                    sessions = sessions,
                    onSessionSelected = onSessionSelected
                )
            }
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
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.2f))
                .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (sessions.isEmpty()) "အကြိမ် မရှိသေးပါ" else selectedSession.ifEmpty { "12.AM" },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                softWrap = false
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "အကြိမ် ရွေးရန်",
                tint = Color.White,
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
                                color = if (s.name == selectedSession) LuckyHeader else LuckyTextPrimary,
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
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder, color = LuckyTextMuted, fontSize = 13.sp, maxLines = 1) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "ရှာဖွေရန်",
                tint = LuckyTextSecondary,
                modifier = Modifier.size(18.dp)
            )
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = LuckyCard,
            unfocusedContainerColor = LuckyCard,
            focusedTextColor = LuckyTextPrimary,
            unfocusedTextColor = LuckyTextPrimary,
            focusedBorderColor = LuckyHeader,
            unfocusedBorderColor = LuckyBorder
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    )
}

@Composable
fun CircularActionButton(
    icon: ImageVector? = null,
    text: String? = null,
    backgroundColor: Color = LuckyHeader,
    contentColor: Color = Color.White,
    size: Int = 52,
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
                contentDescription = text ?: "လုပ်ဆောင်ချက်",
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
        shape = RoundedCornerShape(14.dp),
        title = {
            Text(
                text = title,
                color = LuckyTextPrimary,
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
        shape = RoundedCornerShape(14.dp),
        title = {
            Text(
                text = title,
                color = LuckyTextPrimary,
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
                            Icon(Icons.Default.Edit, contentDescription = "ပြင်ဆင်မည်", tint = LuckyHeader, modifier = Modifier.size(20.dp))
                            Text("ပြင်ဆင်မည်", color = LuckyTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
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
                        Icon(Icons.Default.Delete, contentDescription = "ဖျက်မည်", tint = LuckyDanger, modifier = Modifier.size(20.dp))
                        Text("ဖျက်မည်", color = LuckyDanger, fontSize = 15.sp, fontWeight = FontWeight.Medium)
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
    fun copyToClipboard(context: Context, text: String, label: String = "Lucky2D") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "ကူးယူပြီးပါပြီ", Toast.LENGTH_SHORT).show()
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

fun fmt(n: Number): String = when (n) {
    is Long, is Int, is Short, is Byte -> n.toLong().toString()
    else -> kotlin.math.round(n.toDouble()).toLong().toString()
}
