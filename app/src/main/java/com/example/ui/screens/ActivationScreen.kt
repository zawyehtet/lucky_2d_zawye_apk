package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.security.LicenseManager
import com.example.ui.components.CloverLogo
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * VisualTransformation that formats raw 12-character uppercase input as `XXXX-XXXX-XXXX`
 * with accurate cursor offset mapping.
 */
class ProductKeyVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.take(12)
        val out = StringBuilder()
        for (i in raw.indices) {
            out.append(raw[i])
            if ((i == 3 || i == 7) && i != raw.lastIndex) {
                out.append('-')
            }
        }
        val transformed = out.toString()

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, raw.length)
                return when {
                    clamped <= 4 -> clamped
                    clamped <= 8 -> clamped + 1
                    else -> clamped + 2
                }.coerceAtMost(transformed.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, transformed.length)
                return when {
                    clamped <= 4 -> clamped
                    clamped <= 9 -> clamped - 1
                    else -> clamped - 2
                }.coerceIn(0, raw.length)
            }
        }

        return TransformedText(AnnotatedString(transformed), offsetMapping)
    }
}

@Composable
fun ActivationScreen(
    onActivated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val deviceId = remember(context) { LicenseManager.getDeviceId(context) }

    // Raw 12-char alphanumeric key state (displayed as XXXX-XXXX-XXXX via VisualTransformation)
    var rawKeyInput by rememberSaveable { mutableStateOf("") }
    var isVerifying by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showSuccessDialog by rememberSaveable { mutableStateOf(false) }

    val visualTransformation = remember { ProductKeyVisualTransformation() }

    fun triggerActivation() {
        focusManager.clearFocus()
        val normalized = LicenseManager.normalizeKeyInput(rawKeyInput)
        if (normalized.length < 12) {
            errorMessage = "အသက်သွင်းကုဒ် ၁၂ လုံး အပြည့်ထည့်သွင်းပါ (XXXX-XXXX-XXXX)"
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            return
        }

        isVerifying = true
        errorMessage = null

        coroutineScope.launch {
            delay(180)
            val success = LicenseManager.verifyAndSaveLicense(context, normalized)
            isVerifying = false
            if (success) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showSuccessDialog = true
                delay(700)
                showSuccessDialog = false
                onActivated()
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                errorMessage = "အသက်သွင်းကုဒ် မှားယွင်းနေပါသည်။ ပြန်လည်စစ်ဆေးပါ။"
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LuckyBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("activation_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. App Logo & Brand Title
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEFF9D1))
                    .border(2.5.dp, LuckyHeader, CircleShape)
                    .testTag("activation_brand_logo"),
                contentAlignment = Alignment.Center
            ) {
                CloverLogo(size = 80.dp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.app_name),
                color = LuckyHeader,
                fontFamily = LuckySansFontFamily,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("activation_brand_name")
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "ဆော့ဝဲလ် အသက်သွင်းခြင်း",
                color = LuckyTextPrimary,
                fontSize = 20.sp,
                lineHeight = 1.5.em,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("activation_title_mm")
            )

            Text(
                text = "တစ်ကြိမ်သာ အသက်သွင်းရန် လိုအပ်ပါသည်",
                color = LuckyTextSecondary,
                fontSize = 13.sp,
                lineHeight = 1.5.em,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Main Activation Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
                    .border(1.dp, LuckyBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LuckyCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Device ID Display Section
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = LuckyHeader,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "သင့်စက်အမှတ်",
                                color = LuckyTextPrimary,
                                fontSize = 14.sp,
                                lineHeight = 1.5.em,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            color = LuckyCardDark,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, LuckyBorder, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = deviceId,
                                    color = LuckyHeader,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("activation_device_id_text")
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        clipboard?.setPrimaryClip(ClipData.newPlainText("Device ID", deviceId))
                                        Toast.makeText(
                                            context,
                                            "စက်အမှတ် ကူးယူပြီးပါပြီ ($deviceId)",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LuckyHeader,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .height(38.dp)
                                        .testTag("activation_copy_id_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "စက်အမှတ် ကူးယူရန်",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ကူးယူမည်",
                                        fontSize = 12.sp,
                                        lineHeight = 1.5.em,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Text(
                            text = "သင့်စက်အမှတ်ကို Admin ထံပေးပို့ပြီး အသက်သွင်းကုဒ် ရယူပါ",
                            color = LuckyTextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 1.5.em,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("activation_helper_text")
                        )
                    }

                    HorizontalDivider(color = LuckyBorder)

                    // Formatted Product Key Input Field
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = LuckyHeader,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "အသက်သွင်းကုဒ် ထည့်သွင်းရန်",
                                    color = LuckyTextPrimary,
                                    fontSize = 14.sp,
                                    lineHeight = 1.5.em,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            TextButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val pasted = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                                    if (pasted.isNotBlank()) {
                                        rawKeyInput = LicenseManager.normalizeKeyInput(pasted)
                                        errorMessage = null
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    } else {
                                        Toast.makeText(context, "Clipboard ထဲတွင် စာသားမရှိပါ", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("activation_paste_key_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "ကူးထည့်ရန်",
                                    tint = LuckyHeader,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ကူးထည့်မည်",
                                    color = LuckyHeader,
                                    fontSize = 12.sp,
                                    lineHeight = 1.5.em,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        OutlinedTextField(
                            value = rawKeyInput,
                            onValueChange = { newValue ->
                                rawKeyInput = LicenseManager.normalizeKeyInput(newValue)
                                errorMessage = null
                            },
                            placeholder = {
                                Text(
                                    text = "XXXX-XXXX-XXXX",
                                    fontFamily = FontFamily.Monospace,
                                    color = LuckyTextMuted
                                )
                            },
                            visualTransformation = visualTransformation,
                            singleLine = true,
                            isError = errorMessage != null,
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                letterSpacing = 1.5.sp,
                                color = LuckyTextPrimary
                            ),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                autoCorrectEnabled = false,
                                keyboardType = KeyboardType.Ascii,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { triggerActivation() }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LuckyHeader,
                                unfocusedBorderColor = LuckyBorder,
                                errorBorderColor = LuckyDanger,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("activation_key_input")
                        )

                        AnimatedVisibility(visible = errorMessage != null) {
                            Text(
                                text = errorMessage.orEmpty(),
                                color = LuckyDanger,
                                fontSize = 12.sp,
                                lineHeight = 1.5.em,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag("activation_error_text")
                            )
                        }
                    }

                    // Primary "Activate App" Action Button
                    Button(
                        onClick = { triggerActivation() },
                        enabled = !isVerifying && rawKeyInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LuckyHeader,
                            contentColor = Color.White,
                            disabledContainerColor = LuckyBorder,
                            disabledContentColor = LuckyTextMuted
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("activation_submit_btn")
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "စစ်ဆေးနေပါသည်...",
                                fontSize = 15.sp,
                                lineHeight = 1.5.em,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "အသက်သွင်းမည်",
                                fontSize = 15.sp,
                                lineHeight = 1.5.em,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.home_footer_branding),
                color = LuckyTextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("activation_footer_branding")
            )
        }

        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = {},
                containerColor = LuckyCard,
                shape = RoundedCornerShape(16.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = LuckySuccess,
                        modifier = Modifier.size(48.dp)
                    )
                },
                title = {
                    Text(
                        text = "အသက်သွင်းမှု အောင်မြင်ပါသည်",
                        color = LuckyTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        lineHeight = 1.5.em,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Text(
                        text = "သင့်ဖုန်းအတွက် လိုင်စင်အတည်ပြုပြီးပါပြီ။ နောက်တစ်ကြိမ် ပြန်ဖွင့်လျှင် ကုဒ်ပြန်ထည့်ရန် မလိုအပ်တော့ပါ။",
                        color = LuckyTextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 1.5.em,
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {}
            )
        }
    }
}
