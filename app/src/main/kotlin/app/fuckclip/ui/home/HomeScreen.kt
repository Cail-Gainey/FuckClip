package app.fuckclip.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

/**
 * Redesigned dashboard home screen providing real-time status banners, metrics cards, quick shortcuts, and device environment specs.
 *
 * 重新设计的首页仪表盘，提供实时状态横幅、指标看板、快捷功能入口以及设备运行环境详情。
 */
@Composable
fun HomeScreen(
    onNavigateToTheme: () -> Unit = {},
    onNavigateToWhitelist: () -> Unit = {},
    onNavigateToLog: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel()
) {
    val isActivated by viewModel.isActivated.collectAsStateWithLifecycle()
    val frameworkDesc by viewModel.frameworkDescription.collectAsStateWithLifecycle()
    val masterEnabled by viewModel.masterEnabled.collectAsStateWithLifecycle()
    val whitelistCount by viewModel.whitelistCount.collectAsStateWithLifecycle()
    val deniedCount by viewModel.deniedCount.collectAsStateWithLifecycle()
    val grantedCount by viewModel.grantedCount.collectAsStateWithLifecycle()
    val deviceInfo by viewModel.deviceInfo.collectAsStateWithLifecycle()

    val rogueThrottleEnabled by viewModel.rogueThrottleEnabled.collectAsStateWithLifecycle()
    val denyCooldownSeconds by viewModel.denyCooldownSeconds.collectAsStateWithLifecycle()
    val grantBurstSeconds by viewModel.grantBurstSeconds.collectAsStateWithLifecycle()
    val maxReadFrequency by viewModel.maxReadFrequency.collectAsStateWithLifecycle()
    val readTolerance by viewModel.readToleranceCount.collectAsStateWithLifecycle()
    var showThrottleDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Status Banner
        HeroStatusCard(
            isActivated = isActivated,
            masterEnabled = masterEnabled,
            frameworkDesc = frameworkDesc
        )

        // 2. Global Protection Master Switch
        MasterSwitchCard(
            masterEnabled = masterEnabled,
            onToggle = { viewModel.setMasterEnabled(it) }
        )

        // 3. Metrics Overview Grid (3 columns)
        MetricsOverviewGrid(
            whitelistCount = whitelistCount,
            deniedCount = deniedCount,
            grantedCount = grantedCount,
            onRulesClick = onNavigateToWhitelist,
            onDeniedClick = onNavigateToLog,
            onGrantedClick = onNavigateToLog
        )

        // 4. Rogue App Limiter Card
        RogueThrottleCard(
            enabled = rogueThrottleEnabled,
            denyCooldownSeconds = denyCooldownSeconds,
            maxReadFrequency = maxReadFrequency,
            readTolerance = readTolerance,
            onToggle = { viewModel.setRogueThrottleEnabled(it) },
            onConfigure = { showThrottleDialog = true }
        )

        // 5. Quick Access Shortcuts
        QuickActionsCard(
            onNavigateToWhitelist = onNavigateToWhitelist,
            onNavigateToLog = onNavigateToLog,
            onNavigateToTheme = onNavigateToTheme
        )

        // 6. Device Environment Info
        DeviceInfoCard(
            deviceInfo = deviceInfo,
            onCopy = { label, value -> copyTextToClipboard(context, label, value) }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showThrottleDialog) {
        RogueThrottleConfigDialog(
            denyCooldownSeconds = denyCooldownSeconds,
            grantBurstSeconds = grantBurstSeconds,
            maxReadFrequency = maxReadFrequency,
            readTolerance = readTolerance,
            onSelectDenyCooldown = { viewModel.setDenyCooldownSeconds(it) },
            onSelectGrantBurst = { viewModel.setGrantBurstSeconds(it) },
            onSelectMaxFrequency = { viewModel.setMaxReadFrequency(it) },
            onSelectReadTolerance = { viewModel.setReadToleranceCount(it) },
            onDismiss = { showThrottleDialog = false }
        )
    }
}
