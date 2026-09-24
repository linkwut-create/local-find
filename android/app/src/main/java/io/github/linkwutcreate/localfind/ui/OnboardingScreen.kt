package io.github.linkwutcreate.localfind.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.linkwutcreate.localfind.onboarding.OnboardingStep
import io.github.linkwutcreate.localfind.onboarding.SelfCheckResult

/**
 * First-run (and reopenable-anytime) walkthrough for OEM auto-start / battery settings.
 * Purely a presentation layer: vendor detection, step content and the self-check result
 * are all computed by the caller (Activity layer, where Context/PackageManager live) and
 * passed in, keeping this composable and [io.github.linkwutcreate.localfind.onboarding]
 * independently unit-testable.
 */
@Composable
fun OnboardingGuideDialog(
    steps: List<OnboardingStep>,
    onLaunchStep: (OnboardingStep) -> Unit,
    onSelfCheck: () -> SelfCheckResult,
    onDismiss: () -> Unit,
) {
    var lastResult by remember { mutableStateOf<SelfCheckResult?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    LFS.str("onboard_dialog_title"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    LFS.str("onboard_intro"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                steps.forEachIndexed { index, step ->
                    OnboardingStepCard(
                        index = index + 1,
                        step = step,
                        onLaunch = { onLaunchStep(step) },
                    )
                }

                Divider()

                Button(
                    onClick = { lastResult = onSelfCheck() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(LFS.str("onboard_selfcheck_button"))
                }

                lastResult?.let { result -> SelfCheckBanner(result) }

                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(LFS.str("onboard_close"))
                }
            }
        }
    }
}

@Composable
private fun OnboardingStepCard(index: Int, step: OnboardingStep, onLaunch: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "$index. ${LFS.str(step.titleKey)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                LFS.str(step.descriptionKey),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onLaunch, shape = RoundedCornerShape(8.dp)) {
                Text(LFS.str("onboard_jump"), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SelfCheckBanner(result: SelfCheckResult) {
    val bannerColor = if (result.allGood) Color(0xFF4CAF50) else Color(0xFFF44336)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bannerColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (result.allGood) {
            Text(LFS.str("onboard_selfcheck_pass"), style = MaterialTheme.typography.bodySmall, color = bannerColor, fontWeight = FontWeight.Bold)
        } else {
            if (!result.serviceRunning) {
                Text(LFS.str("onboard_selfcheck_fail_service"), style = MaterialTheme.typography.bodySmall, color = bannerColor)
            }
            if (!result.batteryUnrestricted) {
                Text(LFS.str("onboard_selfcheck_fail_battery"), style = MaterialTheme.typography.bodySmall, color = bannerColor)
            }
        }
    }
}
