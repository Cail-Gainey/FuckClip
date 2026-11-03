package app.fuckclip.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.fuckclip.R
import org.koin.androidx.compose.koinViewModel

/**
 * Screen providing comprehensive appearance and dynamic theme configuration.
 *
 * 外观与动态主题配置界面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ThemeViewModel = koinViewModel()
) {
    val colorMode by viewModel.themeColorMode.collectAsStateWithLifecycle()
    val keyColor by viewModel.themeKeyColor.collectAsStateWithLifecycle()
    val paletteStyle by viewModel.themePaletteStyle.collectAsStateWithLifecycle()
    val isFloatingNav by viewModel.isFloatingNavEnabled.collectAsStateWithLifecycle()

    var showStyleDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_theme)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Theme Preview Card
            ThemePreviewCard()

            // Display / Dark Mode Selection
            ThemeModeSelector(
                currentMode = colorMode,
                onModeSelected = viewModel::setThemeColorMode
            )

            // Color Palette Selection (Dynamic Monet + Preset Colors)
            ThemePaletteSelector(
                selectedKeyColor = keyColor,
                currentPaletteStyle = paletteStyle,
                onKeyColorSelected = viewModel::setThemeKeyColor
            )

            // Color Style Variant Picker
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showStyleDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Style,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.theme_style),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = getPaletteStyleLabel(paletteStyle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Bottom Navigation Style
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.theme_floating_nav),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.theme_floating_nav_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isFloatingNav,
                        onCheckedChange = viewModel::setFloatingNavEnabled
                    )
                }
            }
        }
    }

    if (showStyleDialog) {
        PaletteStyleDialog(
            currentStyle = paletteStyle,
            onStyleSelected = {
                viewModel.setThemePaletteStyle(it)
                showStyleDialog = false
            },
            onDismiss = { showStyleDialog = false }
        )
    }
}
