package dev.domus.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.domus.shared.DesignTokens
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val body: String,
)

private val PAGES = listOf(
    OnboardingPage(
        icon = Icons.Filled.Home,
        title = "Welcome to Domus",
        body = "A native Material 3 client for Home Assistant — built for speed and " +
            "real Android integration instead of a wrapped web dashboard.",
    ),
    OnboardingPage(
        icon = Icons.AutoMirrored.Filled.Login,
        title = "Connect your instance",
        body = "Sign in with your Home Assistant username and password (2FA included), " +
            "or paste a long-lived access token if you'd rather skip the login form.",
    ),
    OnboardingPage(
        icon = Icons.Filled.Tune,
        title = "Choose what to show",
        body = "Pick exactly which entities show up on your dashboard, grouped by " +
            "type, with live updates and controls — not a dump of everything.",
    ),
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()
    val isLastPage by remember { derivedStateOf { pagerState.currentPage == PAGES.lastIndex } }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.md.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onFinished) { Text("Skip") }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { page ->
            val onboardingPage = PAGES[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(DesignTokens.Spacing.lg.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = onboardingPage.icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                )
                Text(
                    text = onboardingPage.title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = DesignTokens.Spacing.lg.dp),
                )
                Text(
                    text = onboardingPage.body,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = DesignTokens.Spacing.md.dp),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.md.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(PAGES.size) { index ->
                val isSelected = index == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 10.dp else 8.dp)
                        .background(
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = CircleShape,
                        ),
                )
            }
        }

        Button(
            onClick = {
                if (isLastPage) {
                    onFinished()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.Spacing.lg.dp),
        ) {
            Text(if (isLastPage) "Get started" else "Next")
        }
    }
}
