package dev.domus.android.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import dev.domus.android.R
import dev.domus.shared.DesignTokens
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val titleRes: Int,
    val bodyRes: Int,
)

private val PAGES = listOf(
    OnboardingPage(Icons.Filled.Home, R.string.onboarding_page1_title, R.string.onboarding_page1_body),
    OnboardingPage(
        Icons.AutoMirrored.Filled.Login,
        R.string.onboarding_page2_title,
        R.string.onboarding_page2_body,
    ),
    OnboardingPage(Icons.Filled.Tune, R.string.onboarding_page3_title, R.string.onboarding_page3_body),
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
            TextButton(onClick = onFinished) { Text(stringResource(R.string.onboarding_skip)) }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { page ->
            val onboardingPage = PAGES[page]
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val proximity = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(DesignTokens.Spacing.lg.dp)
                    .graphicsLayer {
                        val scale = lerp(0.82f, 1f, proximity)
                        scaleX = scale
                        scaleY = scale
                        alpha = lerp(0.35f, 1f, proximity)
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(96.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = onboardingPage.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                }
                Text(
                    text = stringResource(onboardingPage.titleRes),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = DesignTokens.Spacing.lg.dp),
                )
                Text(
                    text = stringResource(onboardingPage.bodyRes),
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
                val dotWidth by animateDpAsState(if (isSelected) 24.dp else 8.dp, label = "onboardingDotWidth")
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .height(8.dp)
                        .width(dotWidth)
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
            Text(stringResource(if (isLastPage) R.string.onboarding_get_started else R.string.onboarding_next))
        }
    }
}
