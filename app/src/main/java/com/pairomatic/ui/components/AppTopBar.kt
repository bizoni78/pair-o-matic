package com.pairomatic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pairomatic.ui.theme.brandGradient

/**
 * Gradientowa (markowa) belka górna — przejście indygo → fiolet → róż, sięgające pod pasek statusu.
 * Biały tytuł i akcje.
 */
@Composable
fun AppTopBar(
    title: String,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxWidth().background(brandGradient())) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            // Ikony akcji na biało, żeby były czytelne na gradiencie.
            CompositionLocalProvider(LocalContentColor provides Color.White) {
                actions()
            }
        }
    }
}
