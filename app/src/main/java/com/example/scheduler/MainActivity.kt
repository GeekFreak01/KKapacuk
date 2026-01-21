package com.example.scheduler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var themeMode by rememberSaveable { mutableStateOf(ThemeMode.SYSTEM) }
            ScheduleTheme(themeMode = themeMode) {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    val viewModel: ScheduleViewModel = viewModel()
                    val state by viewModel.state.collectAsState()
                    ScheduleScreen(
                        state = state,
                        themeMode = themeMode,
                        onThemeModeChange = { themeMode = it }
                    )
                }
            }
        }
    }
}

data class ScheduleUiState(
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.now(),
    val snapshot: ScheduleSnapshot = scheduleSnapshot(LocalDate.now(), LocalTime.now())
)

class ScheduleViewModel : ViewModel() {
    private val _state = MutableStateFlow(ScheduleUiState())
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                val now = LocalTime.now()
                val date = LocalDate.now()
                _state.value = ScheduleUiState(
                    date = date,
                    time = now,
                    snapshot = scheduleSnapshot(date, now)
                )
                delay(1000)
            }
        }
    }
}

@Composable
fun ScheduleScreen(
    state: ScheduleUiState,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    val dimens = LocalDimens.current
    var settingsExpanded by remember { mutableStateOf(false) }
    val header = buildString {
        append(
            when (state.snapshot.variant) {
                ScheduleVariant.SEP_NOV_APR_JUN -> "Сентябрь–Ноябрь / Апрель–Июнь"
                ScheduleVariant.DEC_MAR -> "Декабрь–Март"
            }
        )
        append(" • ")
        append(
            when (state.snapshot.dayType) {
                DayType.WEEKDAY -> "Будний день"
                DayType.SATURDAY -> "Суббота"
            }
        )
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.padding(dimens.medium.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Расписание",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Box {
                        IconButton(onClick = { settingsExpanded = true }) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Настройки"
                            )
                        }
                        DropdownMenu(
                            expanded = settingsExpanded,
                            onDismissRequest = { settingsExpanded = false }
                        ) {
                            ThemeMenuItem(
                                label = "Как на устройстве",
                                selected = themeMode == ThemeMode.SYSTEM,
                                onClick = {
                                    onThemeModeChange(ThemeMode.SYSTEM)
                                    settingsExpanded = false
                                }
                            )
                            ThemeMenuItem(
                                label = "День",
                                selected = themeMode == ThemeMode.LIGHT,
                                onClick = {
                                    onThemeModeChange(ThemeMode.LIGHT)
                                    settingsExpanded = false
                                }
                            )
                            ThemeMenuItem(
                                label = "Ночь",
                                selected = themeMode == ThemeMode.DARK,
                                onClick = {
                                    onThemeModeChange(ThemeMode.DARK)
                                    settingsExpanded = false
                                }
                            )
                        }
                    }
                }
                Text(
                    text = header,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "Сейчас: ${state.time.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = dimens.medium.dp),
            verticalArrangement = Arrangement.spacedBy(dimens.small.dp)
        ) {
            itemsIndexed(state.snapshot.lessons) { index, lesson ->
                LessonRow(
                    lesson = lesson,
                    isActive = state.snapshot.activeIndex == index,
                    now = state.time
                )
            }
        }
    }
}

@Composable
private fun ThemeMenuItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(text = label) },
        leadingIcon = {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null
                )
            }
        },
        onClick = onClick
    )
}

@Composable
fun LessonRow(lesson: Lesson, isActive: Boolean, now: LocalTime) {
    val dimens = LocalDimens.current
    val progress = lesson.progress(now)
    val passed = lesson.passedMinutes(now)
    val total = lesson.durationMinutes()
    val remaining = lesson.remainingMinutes(now)

    val cardColor = if (isActive) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(dimens.medium.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${lesson.start.format(timeFormatter)} — ${lesson.end.format(timeFormatter)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${passed}/${total} мин",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(dimens.small.dp))
            ProgressWithHalfMarker(progress = progress)
            Spacer(modifier = Modifier.height(dimens.tiny.dp))
            Text(
                text = "Осталось: ${remaining} мин",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun ProgressWithHalfMarker(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(3.dp))
        )
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(10.dp)
                .align(Alignment.Center)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        )
    }
}

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
