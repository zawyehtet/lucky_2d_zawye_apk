package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
                text = selectedSession,
                color = LuckyLime,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select Session",
                tint = LuckyLime,
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(LuckyCard)
        ) {
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
        // Fallback for native Android print without external printer: copy and inform
        copyToClipboard(context, text, "Print Receipt")
        Toast.makeText(context, "ပရင့်ထုတ်ရန် အချက်အလက် ကူးယူပြီးပါပြီ", Toast.LENGTH_LONG).show()
    }
}
