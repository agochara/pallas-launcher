package com.pallas.launcher.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pallas.launcher.domain.search.AppSearchResult
import com.pallas.launcher.ui.theme.LauncherTypography
import com.pallas.launcher.ui.theme.LocalLauncherColors

@Composable
fun SearchResultItemView(
    result: AppSearchResult,
    isFirst: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalLauncherColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = result.app.label,
            style = LauncherTypography.appLabel.copy(
                fontSize = 18.sp,
                fontWeight = if (isFirst) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (isFirst) colors.textPrimary else colors.textSecondary
        )
    }
}
