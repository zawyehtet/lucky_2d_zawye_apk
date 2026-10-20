@file:OptIn(ExperimentalFoundationApi::class)
package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.Session
import com.example.ui.theme.*
import com.example.util.SoundManager
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Shared light-mode OutlinedTextField wrapper used across the entire app.
 * Guarantees solid black typed text (Color.Black, full opacity, >= 16sp, Medium weight),
 * pure white field background, dark grey labels, black cursor & selection handles, and red error border.
 */
@Composable
fun AppOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = AppInputTextStyle,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    val resolvedFontSize = if (textStyle.fontSize != androidx.compose.ui.unit.TextUnit.Unspecified && textStyle.fontSize.value >= 16f) {
        textStyle.fontSize
    } else {
        16.sp
    }
    val resolvedStyle = textStyle.copy(
        color = Color.Black,
        fontSize = resolvedFontSize,
        fontWeight = textStyle.fontWeight ?: FontWeight.Medium
    )
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = resolvedStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        shape = shape,
        colors = appOutlinedTextFieldColors()
    )
}

/**
 * Compact filled Button variant (height 40dp, 14sp text, wrap-content width, configurable horizontal padding and shape,
 * minimum touch target 40dp, and clear disabled style).
 */
@Composable
fun CompactFilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = LuckyHeader,
    contentColor: Color = Color.White,
    disabledContainerColor: Color = LuckyBorder.copy(alpha = 0.75f),
    disabledContentColor: Color = LuckyTextMuted,
    horizontalPadding: Dp = 16.dp,
    cornerRadius: Dp = 10.dp
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(cornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        ),
        contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 0.dp),
        modifier = modifier
            .height(40.dp)
            .defaultMinSize(minWidth = 1.dp, minHeight = 40.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun CompactPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = LuckyHeader,
    contentColor: Color = Color.White,
    disabledContainerColor: Color = LuckyBorder.copy(alpha = 0.75f),
    disabledContentColor: Color = LuckyTextMuted,
    horizontalPadding: Dp = 16.dp,
    cornerRadius: Dp = 10.dp
) {
    CompactFilledButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
        horizontalPadding = horizontalPadding,
        cornerRadius = cornerRadius
    )
}

/**
 * Compact TextButton variant (height 40dp, 14sp text, wrap-content width, configurable horizontal padding,
 * minimum touch target 40dp).
 */
@Composable
fun CompactTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = LuckyTextSecondary,
    horizontalPadding: Dp = 16.dp,
    cornerRadius: Dp = 10.dp
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(cornerRadius),
        colors = ButtonDefaults.textButtonColors(
            contentColor = contentColor,
            disabledContentColor = LuckyTextMuted.copy(alpha = 0.6f)
        ),
        contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 0.dp),
        modifier = modifier
            .height(40.dp)
            .defaultMinSize(minWidth = 1.dp, minHeight = 40.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Plays the short click sound whenever a text/number input field is tapped or receives focus.
 */
fun Modifier.soundOnInputFocus(): Modifier = composed {
    var wasFocused by remember { mutableStateOf(false) }
    this
        .onFocusChanged { state ->
            if (state.isFocused && !wasFocused) {
                SoundManager.playClick(debounceMs = 80L)
            }
            wasFocused = state.isFocused
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                SoundManager.playClick(debounceMs = 80L)
            }
        }
}

/**
 * Long-press + normal tap modifier using Modifier.combinedClickable with HapticFeedbackType.LongPress
 * and TalkBack custom accessibility actions for "ပြင်မည်" (Edit) and "ဖျက်မည်" (Delete).
 */
fun Modifier.entryCombinedClickable(
    enabled: Boolean = true,
    itemDescription: String = "",
    onClick: () -> Unit = {},
    onShowMenu: () -> Unit,
    onEditAction: () -> Unit,
    onDeleteAction: () -> Unit
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val editLabel = stringResource(R.string.action_edit)
    val deleteLabel = stringResource(R.string.action_delete)

    this
        .semantics {
            if (itemDescription.isNotBlank()) {
                contentDescription = itemDescription
            }
            customActions = if (enabled) {
                listOf(
                    CustomAccessibilityAction(label = editLabel) {
                        onEditAction()
                        true
                    },
                    CustomAccessibilityAction(label = deleteLabel) {
                        onDeleteAction()
                        true
                    }
                )
            } else {
                emptyList()
            }
        }
        .combinedClickable(
            enabled = enabled,
            onClick = onClick,
            onLongClick = if (enabled) {
                {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onShowMenu()
                }
            } else null
        )
}

/**
 * Dropdown menu with optional header title and exclusively two actions: "ပြင်မည်" (Edit) and "ဖျက်မည်" (Delete).
 */
@Composable
fun EntryLongPressDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    title: String? = null
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .background(LuckyCard)
            .testTag("entry_long_press_menu")
    ) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                color = LuckyTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("entry_long_press_menu_title")
            )
            HorizontalDivider(color = LuckyBorder.copy(alpha = 0.6f), thickness = 0.5.dp)
        }
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(R.string.action_edit),
                    color = LuckyTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.action_edit),
                    tint = LuckyHeader,
                    modifier = Modifier.size(18.dp)
                )
            },
            onClick = {
                onDismiss()
                onEdit()
            },
            modifier = Modifier.testTag("menu_action_edit")
        )
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(R.string.action_delete),
                    color = LuckyDanger,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = LuckyDanger,
                    modifier = Modifier.size(18.dp)
                )
            },
            onClick = {
                onDismiss()
                onDelete()
            },
            modifier = Modifier.testTag("menu_action_delete")
        )
    }
}

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
    var expanded by rememberSaveable { mutableStateOf(false) }
    val noRoundsText = stringResource(R.string.no_rounds_yet)

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
                text = if (sessions.isEmpty() || selectedSession.isEmpty()) noRoundsText else selectedSession,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                softWrap = false
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = stringResource(R.string.select_round_content_desc),
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
                            text = noRoundsText,
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
fun RoundRequiredHelperBanner(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = visible) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(LuckyWarning.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                .border(1.dp, LuckyWarning.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("add_round_first_helper"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = LuckyWarning,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = stringResource(R.string.add_round_first),
                color = LuckyTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun SearchPill(
    query: String,
    onQueryChange: (String) -> Unit,
    label: String = "",
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val resolvedLabel = label.ifEmpty { stringResource(R.string.label_search) }
    AppOutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        enabled = enabled,
        label = { Text(resolvedLabel, fontSize = 12.sp, maxLines = 1, color = LuckyLabelDarkGrey) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = resolvedLabel,
                tint = LuckyTextSecondary,
                modifier = Modifier.size(18.dp)
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .soundOnInputFocus()
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
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    this.pointerInput(onLongPress, onClick) {
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
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
}

@Composable
fun DeleteConfirmDialog(
    itemName: String = "",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String = "ဖျက်မည်လား?",
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
                text = message ?: if (itemName.isNotBlank()) "$itemName ကို ဖျက်မည်လား?" else "ဖျက်မည်လား?",
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
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("confirm_delete_btn")
            ) {
                Text(stringResource(R.string.action_delete), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_delete_btn")
            ) {
                Text(stringResource(R.string.action_delete_cancel), color = LuckyTextSecondary)
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
                            Icon(Icons.Default.Edit, contentDescription = "ပြင်မည်", tint = LuckyHeader, modifier = Modifier.size(20.dp))
                            Text("ပြင်မည်", color = LuckyTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
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
        confirmButton = {}
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

fun fmtThousands(n: Number): String {
    val value = when (n) {
        is Long, is Int, is Short, is Byte -> n.toLong()
        else -> kotlin.math.round(n.toDouble()).toLong()
    }
    return java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(value)
}

fun formatWinningNumberTwoDigits(winningNumber: String?): String {
    val clean = winningNumber?.trim().orEmpty()
    if (clean.isEmpty()) return "-"
    val num = clean.toIntOrNull()
    return if (num != null && num in 0..99) {
        "%02d".format(java.util.Locale.US, num)
    } else {
        clean
    }
}

/**
 * Formats a date string ("yyyy-MM-dd" or "yyyy.MM.dd") into "dd.MM.yy".
 */
fun formatDailyRecordDate(dateStr: String): String {
    val clean = dateStr.trim()
    val parts = clean.split("-", ".", "/")
    if (parts.size == 3) {
        if (parts[0].length == 4) {
            val yy = parts[0].takeLast(2)
            val mm = parts[1].padStart(2, '0')
            val dd = parts[2].padStart(2, '0')
            return "$dd.$mm.$yy"
        } else if (parts[2].length == 4) {
            val dd = parts[0].padStart(2, '0')
            val mm = parts[1].padStart(2, '0')
            val yy = parts[2].takeLast(2)
            return "$dd.$mm.$yy"
        }
    }
    return clean
}

/**
 * Formats profit/loss with thousands separators and sign:
 * "+" for profit (> 0), "-" for loss (< 0), "0" for zero.
 */
fun formatSignedProfitLoss(amount: Long): String {
    return when {
        amount > 0L -> "+${fmtThousands(amount)}"
        amount < 0L -> "-${fmtThousands(kotlin.math.abs(amount))}"
        else -> "0"
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4F7F6, name = "Light Mode AppOutlinedTextField Preview")
@Composable
fun AppOutlinedTextFieldLightModePreview() {
    MyApplicationTheme {
        Column(
            modifier = Modifier
                .background(LuckyBg)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppOutlinedTextField(
                value = "47 = 15000",
                onValueChange = {},
                label = { Text("ပမာဏ") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
