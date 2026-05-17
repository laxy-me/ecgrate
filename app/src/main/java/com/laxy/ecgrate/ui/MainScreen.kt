package com.laxy.ecgrate.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laxy.ecgrate.MainViewModel
import com.laxy.ecgrate.RateState
import com.laxy.ecgrate.entity.CurrencyRate

@Composable
fun MainScreen(viewModel: MainViewModel, onRequestAlarmPermission: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editMode by remember { mutableStateOf(false) }
    var tempSelected by remember { mutableStateOf<String?>(null) }
    var intervalText by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        RateHeader(
            state = state,
            editMode = editMode,
            intervalText = intervalText,
            onIntervalChange = { intervalText = it },
            onEditClick = { editMode = true; tempSelected = null },
            onCompleteClick = {
                editMode = false
                tempSelected?.let { viewModel.setSelectedCurrency(it) }
            },
            onSaveInterval = {
                intervalText.toIntOrNull()?.let {
                    viewModel.setInterval(it)
                    intervalText = ""
                }
            },
            onRefreshClick = { viewModel.refresh() },
            onAlarmPermission = onRequestAlarmPermission
        )

        if (state.isLoading && state.rates.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.rates, key = { it.ccyNbr }) { body ->
                    val isSelected = body.ccyNbr == (tempSelected ?: state.selectedCurrency)
                    RateItemCard(
                        body = body,
                        isSelected = isSelected && editMode,
                        editMode = editMode,
                        onClick = { if (editMode) tempSelected = body.ccyNbr }
                    )
                }
            }
        }
    }
}

@Composable
private fun RateHeader(
    state: RateState,
    editMode: Boolean,
    intervalText: String,
    onIntervalChange: (String) -> Unit,
    onEditClick: () -> Unit,
    onCompleteClick: () -> Unit,
    onSaveInterval: () -> Unit,
    onRefreshClick: () -> Unit,
    onAlarmPermission: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF1565C0), Color(0xFF42A5F5)))
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ECGRate", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!editMode) {
                        IconButton(onClick = onAlarmPermission, modifier = Modifier.size(36.dp)) {
                            Text("⚡", fontSize = 16.sp)
                        }
                    }
                    TextButton(
                        onClick = if (editMode) onCompleteClick else onEditClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                    ) {
                        Text(if (editMode) "完成" else "编辑")
                    }
                }
            }

            Text(
                text = state.lastUpdated.ifEmpty { if (state.isLoading) "加载中..." else "点击刷新" },
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                modifier = Modifier.clickable { onRefreshClick() }
            )

            if (editMode) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = intervalText,
                        onValueChange = onIntervalChange,
                        label = { Text("刷新间隔(秒)", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            cursorColor = Color.White
                        ),
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onSaveInterval,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        )
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

@Composable
fun RateItemCard(
    body: CurrencyRate.Body,
    isSelected: Boolean,
    editMode: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = editMode, onClick = onClick),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = body.ccyNbrEng,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            RateRow(label = "现汇买入", value = body.rthBid)
            RateRow(label = "现汇卖出", value = body.rthOfr)
            RateRow(label = "现钞买入", value = body.rtcBid)
            RateRow(label = "现钞卖出", value = body.rtcOfr)
        }
    }
}

@Composable
private fun RateRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = Color.Gray)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
