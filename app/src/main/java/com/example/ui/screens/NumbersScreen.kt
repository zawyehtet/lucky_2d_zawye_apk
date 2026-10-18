package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.AppHeader
import com.example.ui.components.SearchPill
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun NumbersScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSession by viewModel.selectedSession.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    val vouchers by viewModel.vouchers.collectAsState()
    val layoffs by viewModel.layoffs.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    // Aggregated numbers for selectedDate and selectedSession
    val summaries = remember(vouchers, layoffs, selectedDate, selectedSession) {
        viewModel.getAggregatedNumbers()
    }

    val filteredSummaries = summaries.filter {
        searchQuery.isBlank() || it.n.contains(searchQuery.trim())
    }

    val grandTotal = filteredSummaries.sumOf { it.netAmount }
    val numFormat = NumberFormat.getNumberInstance(Locale.US)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LuckyBg)
    ) {
        AppHeader(
            title = "ဂဏန်းများ (Numbers)",
            selectedSession = selectedSession,
            sessions = sessions,
            onSessionSelected = { viewModel.setSession(it) }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "နေ့စွဲ : $selectedDate",
                    color = LuckyTextSecondary,
                    fontSize = 13.sp
                )
                Text(
                    text = "စုစုပေါင်း ဂဏန်း : ${filteredSummaries.size} လုံး",
                    color = LuckyLime,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            SearchPill(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "ဂဏန်း ရိုက်ထည့်၍ ရှာရန်..."
            )
        }

        // 2-Column Table (ဂဏန်းများ | ပမာဏ)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = LuckyCard),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .border(1.dp, LuckyBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LuckyHeader)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ဂဏန်းများ (2D)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "ပမာဏ (Net)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.End
                    )
                }

                if (filteredSummaries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ထိုးထားသော ဂဏန်း မရှိသေးပါ",
                            color = LuckyTextMuted,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        itemsIndexed(filteredSummaries, key = { _, item -> item.n }) { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (index % 2 == 0) LuckyCard else LuckyCardDark)
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "${index + 1}.",
                                        color = LuckyTextMuted,
                                        fontSize = 12.sp,
                                        modifier = Modifier.width(26.dp)
                                    )
                                    Text(
                                        text = item.n,
                                        color = LuckyLime,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    if (item.layoffAmount > 0) {
                                        Text(
                                            text = "(တင်: -${numFormat.format(item.layoffAmount)})",
                                            color = LuckyDanger,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Text(
                                    text = numFormat.format(item.netAmount),
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }

                // Table Footer: စုစုပေါင်း
                Divider(color = LuckyBorder, thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LuckyHeader)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "စုစုပေါင်း",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = numFormat.format(grandTotal),
                        color = LuckyLime,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(90.dp))
    }
}
