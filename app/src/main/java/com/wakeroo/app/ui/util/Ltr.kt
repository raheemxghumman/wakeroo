package com.wakeroo.app.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * Lays [content] out left to right even in Arabic and Urdu. For things whose order is not
 * language: the wordmark, keypads, and hour:minute clocks, which Android keeps LTR too.
 */
@Composable
fun Ltr(content: @Composable () -> Unit) = CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr, content = content)
