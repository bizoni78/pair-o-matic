package com.pairomatic.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.pairomatic.AppContainer
import com.pairomatic.PairOMaticApp

/** Wygodny dostęp do kontenera zależności z poziomu funkcji @Composable. */
@Composable
fun rememberAppContainer(): AppContainer {
    val context = LocalContext.current
    return (context.applicationContext as PairOMaticApp).container
}
