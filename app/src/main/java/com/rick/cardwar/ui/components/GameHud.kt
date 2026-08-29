package com.rick.cardwar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rick.cardwar.R
import com.rick.cardwar.ui.theme.HudScrim

@Composable
fun GameHud(
    cpuEnabled: Boolean,
    soundEnabled: Boolean,
    onStart: () -> Unit,
    onCpuChange: (Boolean) -> Unit,
    onRules: () -> Unit,
    onSoundToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(HudScrim)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HudSlot {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = cpuEnabled,
                    onCheckedChange = onCpuChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurface,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
                Text(
                    text = stringResource(R.string.cpu),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        HudSlot {
            CompactButton(text = stringResource(R.string.start), onClick = onStart)
        }
        HudSlot {
            CompactButton(text = stringResource(R.string.rules), onClick = onRules)
        }
        HudSlot {
            IconButton(onClick = onSoundToggle, modifier = Modifier.size(40.dp)) {
                Icon(
                    painter = painterResource(
                        if (soundEnabled) R.drawable.ic_volume_up else R.drawable.ic_volume_off,
                    ),
                    contentDescription = stringResource(R.string.sound),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun RowScope.HudSlot(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun CompactButton(text: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.height(36.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Text(text)
    }
}
