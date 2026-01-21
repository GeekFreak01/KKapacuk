package com.example.scheduler

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.material3.rememberModalBottomSheetState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
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
                        onThemeModeChange = { themeMode = it },
                        onTimeZoneModeChange = viewModel::setTimeZoneMode,
                        onGpsPermissionChange = viewModel::setGpsPermissionGranted,
                        onLocationChange = viewModel::setSelectedLocation
                    )
                }
            }
        }
    }
}

data class ScheduleUiState(
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.now(),
    val snapshot: ScheduleSnapshot = scheduleSnapshot(LocalDate.now(), LocalTime.now()),
    val timeZoneMode: TimeZoneMode = TimeZoneMode.AUTO,
    val gpsPermissionGranted: Boolean = true,
    val selectedLocation: TimeZoneLocation = timeZoneLocations.first()
)

class ScheduleViewModel : ViewModel() {
    private val _state = MutableStateFlow(ScheduleUiState())
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                val current = _state.value
                val zoneId = resolveZoneId(current)
                val now = ZonedDateTime.now(zoneId)
                val date = now.toLocalDate()
                val time = now.toLocalTime()
                _state.value = current.copy(
                    date = date,
                    time = time,
                    snapshot = scheduleSnapshot(date, time)
                )
                delay(1000)
            }
        }
    }

    fun setTimeZoneMode(mode: TimeZoneMode) {
        _state.value = _state.value.copy(timeZoneMode = mode)
    }

    fun setGpsPermissionGranted(granted: Boolean) {
        _state.value = _state.value.copy(gpsPermissionGranted = granted)
    }

    fun setSelectedLocation(location: TimeZoneLocation) {
        _state.value = _state.value.copy(selectedLocation = location)
    }
}

@Composable
fun ScheduleScreen(
    state: ScheduleUiState,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onTimeZoneModeChange: (TimeZoneMode) -> Unit,
    onGpsPermissionChange: (Boolean) -> Unit,
    onLocationChange: (TimeZoneLocation) -> Unit
) {
    val dimens = LocalDimens.current
    var settingsOpen by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onGpsPermissionChange(granted)
    }
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
                    IconButton(onClick = { settingsOpen = true }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Настройки"
                        )
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
                Spacer(modifier = Modifier.height(dimens.small.dp))
                TimeZoneSettings(
                    state = state,
                    onTimeZoneModeChange = onTimeZoneModeChange,
                    onGpsPermissionChange = onGpsPermissionChange,
                    onLocationChange = onLocationChange
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

    if (settingsOpen) {
        ModalBottomSheet(
            onDismissRequest = { settingsOpen = false },
            sheetState = bottomSheetState
        ) {
            SettingsSheet(
                state = state,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                onTimeZoneModeChange = onTimeZoneModeChange,
                onLocationChange = onLocationChange,
                onRequestGpsPermission = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
            )
        }
    }
}

@Composable
private fun SettingsSheet(
    state: ScheduleUiState,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onTimeZoneModeChange: (TimeZoneMode) -> Unit,
    onLocationChange: (TimeZoneLocation) -> Unit,
    onRequestGpsPermission: () -> Unit
) {
    val dimens = LocalDimens.current
    var themeExpanded by remember { mutableStateOf(false) }
    var locationExpanded by remember { mutableStateOf(false) }
    val isAuto = state.timeZoneMode == TimeZoneMode.AUTO
    val manualEnabled = !isAuto

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.medium.dp)
            .padding(bottom = dimens.large.dp),
        verticalArrangement = Arrangement.spacedBy(dimens.small.dp)
    ) {
        Text(
            text = "Настройки",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(dimens.medium.dp)) {
                Text(
                    text = "Выбор темы",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(dimens.tiny.dp))
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { themeExpanded = true }
                            .padding(dimens.small.dp)
                    ) {
                        Text(
                            text = themeModeLabel(themeMode),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    DropdownMenu(
                        expanded = themeExpanded,
                        onDismissRequest = { themeExpanded = false }
                    ) {
                        ThemeMenuItem(
                            label = "Как на устройстве",
                            selected = themeMode == ThemeMode.SYSTEM,
                            onClick = {
                                onThemeModeChange(ThemeMode.SYSTEM)
                                themeExpanded = false
                            }
                        )
                        ThemeMenuItem(
                            label = "День",
                            selected = themeMode == ThemeMode.LIGHT,
                            onClick = {
                                onThemeModeChange(ThemeMode.LIGHT)
                                themeExpanded = false
                            }
                        )
                        ThemeMenuItem(
                            label = "Ночь",
                            selected = themeMode == ThemeMode.DARK,
                            onClick = {
                                onThemeModeChange(ThemeMode.DARK)
                                themeExpanded = false
                            }
                        )
                    }
                }
            }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(dimens.medium.dp)) {
                Text(
                    text = "Определить часовой пояс",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(dimens.tiny.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Автоопределение (GPS)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = isAuto,
                        onCheckedChange = { enabled ->
                            onTimeZoneModeChange(if (enabled) TimeZoneMode.AUTO else TimeZoneMode.MANUAL)
                            if (enabled && !state.gpsPermissionGranted) {
                                onRequestGpsPermission()
                            }
                        }
                    )
                }
                if (isAuto && !state.gpsPermissionGranted) {
                    Spacer(modifier = Modifier.height(dimens.tiny.dp))
                    Text(
                        text = "Нужен доступ к GPS для автоопределения.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(dimens.small.dp))
                Box {
                    val manualAlpha = if (manualEnabled) 1f else 0.5f
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable(enabled = manualEnabled) { locationExpanded = true }
                            .padding(dimens.small.dp)
                    ) {
                        Text(
                            text = state.selectedLocation.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = manualAlpha)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = manualAlpha)
                        )
                    }
                    DropdownMenu(
                        expanded = locationExpanded && manualEnabled,
                        onDismissRequest = { locationExpanded = false }
                    ) {
                        timeZoneLocations.forEach { location ->
                            DropdownMenuItem(
                                text = { Text(text = location.label) },
                                onClick = {
                                    onLocationChange(location)
                                    locationExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeZoneSettings(
    state: ScheduleUiState,
    onTimeZoneModeChange: (TimeZoneMode) -> Unit,
    onGpsPermissionChange: (Boolean) -> Unit,
    onLocationChange: (TimeZoneLocation) -> Unit
) {
    val dimens = LocalDimens.current
    var locationExpanded by remember { mutableStateOf(false) }
    val isAuto = state.timeZoneMode == TimeZoneMode.AUTO
    val showManualSelection = !state.gpsPermissionGranted || !isAuto

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(dimens.medium.dp)) {
            Text(
                text = "Часовой пояс",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(dimens.tiny.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Автоопределение (GPS)",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = isAuto,
                    onCheckedChange = { enabled ->
                        onTimeZoneModeChange(if (enabled) TimeZoneMode.AUTO else TimeZoneMode.MANUAL)
                    }
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Разрешение GPS",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = state.gpsPermissionGranted,
                    onCheckedChange = onGpsPermissionChange
                )
            }
            if (showManualSelection) {
                Spacer(modifier = Modifier.height(dimens.small.dp))
                Text(
                    text = if (state.gpsPermissionGranted) {
                        "Выберите местоположение вручную."
                    } else {
                        "GPS недоступен. Выберите местоположение вручную."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(dimens.tiny.dp))
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { locationExpanded = true }
                            .padding(dimens.small.dp)
                    ) {
                        Text(
                            text = state.selectedLocation.label,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    DropdownMenu(
                        expanded = locationExpanded,
                        onDismissRequest = { locationExpanded = false }
                    ) {
                        timeZoneLocations.forEach { location ->
                            DropdownMenuItem(
                                text = { Text(text = location.label) },
                                onClick = {
                                    onLocationChange(location)
                                    locationExpanded = false
                                }
                            )
                        }
                    }
                }
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

enum class TimeZoneMode {
    AUTO,
    MANUAL
}

data class TimeZoneLocation(
    val label: String,
    val zoneId: ZoneId
)

private val timeZoneLocations = listOf(
    TimeZoneLocation("Камчатка (UTC/GMT +12:00)", ZoneId.of("Asia/Kamchatka")),
    TimeZoneLocation("Калининград (UTC+2)", ZoneId.of("Europe/Kaliningrad")),
    TimeZoneLocation("Москва (UTC+3)", ZoneId.of("Europe/Moscow")),
    TimeZoneLocation("Екатеринбург (UTC+5)", ZoneId.of("Asia/Yekaterinburg")),
    TimeZoneLocation("Новосибирск (UTC+7)", ZoneId.of("Asia/Novosibirsk")),
    TimeZoneLocation("Владивосток (UTC+10)", ZoneId.of("Asia/Vladivostok"))
)

private fun resolveZoneId(state: ScheduleUiState): ZoneId {
    return if (state.timeZoneMode == TimeZoneMode.AUTO && state.gpsPermissionGranted) {
        ZoneId.systemDefault()
    } else {
        state.selectedLocation.zoneId
    }
}

private fun themeModeLabel(themeMode: ThemeMode): String = when (themeMode) {
    ThemeMode.SYSTEM -> "Как на устройстве"
    ThemeMode.LIGHT -> "День"
    ThemeMode.DARK -> "Ночь"
}
