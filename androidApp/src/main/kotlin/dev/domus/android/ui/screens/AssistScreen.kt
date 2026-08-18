package dev.domus.android.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.domus.android.R
import dev.domus.shared.DesignTokens
import dev.domus.shared.data.HaSession
import kotlinx.coroutines.launch
import java.util.Locale

private data class ChatMessage(
    val id: Long,
    val text: String,
    val isUser: Boolean,
    val isError: Boolean = false,
)

/** A chat-style Assist screen: type or speak to Home Assistant's conversation agent
 *  (`conversation.process`) and see its reply. Voice input delegates to the system's own
 *  speech-recognition activity (`RecognizerIntent`) instead of driving the microphone
 *  in-process, so no RECORD_AUDIO permission is needed on our side. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistScreen(session: HaSession, onBack: () -> Unit) {
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var input by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var conversationId by remember { mutableStateOf<String?>(null) }
    var nextId by remember { mutableLongStateOf(0L) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val micUnavailableMessage = stringResource(R.string.assist_mic_unavailable)
    val errorMessage = stringResource(R.string.assist_error_generic)

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || isThinking) return
        messages = messages + ChatMessage(id = nextId++, text = trimmed, isUser = true)
        input = ""
        isThinking = true
        scope.launch {
            val response = session.repository.converse(trimmed, conversationId, Locale.getDefault().language)
            conversationId = response.conversationId ?: conversationId
            messages = messages + ChatMessage(
                id = nextId++,
                text = response.speech ?: errorMessage,
                isUser = false,
                isError = response.speech == null,
            )
            isThinking = false
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spoken.isNullOrBlank()) send(spoken)
    }

    fun launchMic() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            scope.launch { snackbarHostState.showSnackbar(micUnavailableMessage) }
        }
    }

    LaunchedEffect(messages.size, isThinking) {
        val lastIndex = messages.size - 1 + if (isThinking) 1 else 0
        if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BrandBlobBackdrop(modifier = Modifier.fillMaxWidth().height(200.dp))
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.assist_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(DesignTokens.Spacing.md.dp),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm.dp),
                ) {
                    if (messages.isEmpty()) {
                        item {
                            AssistWelcome()
                        }
                    }
                    items(messages, key = { it.id }) { message ->
                        ChatBubble(message)
                    }
                    if (isThinking) {
                        item { ThinkingBubble() }
                    }
                }
                AssistInputBar(
                    input = input,
                    onInputChange = { input = it },
                    onSend = { send(input) },
                    onMic = ::launchMic,
                )
            }
        }
    }
}

@Composable
private fun AssistWelcome() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = DesignTokens.Spacing.xl.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(64.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Text(
            text = stringResource(R.string.assist_welcome),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = DesignTokens.Spacing.xl.dp).padding(top = DesignTokens.Spacing.md.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val visibleState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }
    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 6 },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
        ) {
            val containerColor = when {
                message.isError -> MaterialTheme.colorScheme.errorContainer
                message.isUser -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceContainerHigh
            }
            val contentColor = when {
                message.isError -> MaterialTheme.colorScheme.onErrorContainer
                message.isUser -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            }
            Card(
                shape = RoundedCornerShape(
                    topStart = DesignTokens.Shape.cornerLarge.dp,
                    topEnd = DesignTokens.Shape.cornerLarge.dp,
                    bottomStart = if (message.isUser) DesignTokens.Shape.cornerLarge.dp else DesignTokens.Shape.cornerExtraSmall.dp,
                    bottomEnd = if (message.isUser) DesignTokens.Shape.cornerExtraSmall.dp else DesignTokens.Shape.cornerLarge.dp,
                ),
                colors = CardDefaults.cardColors(containerColor = containerColor),
                modifier = Modifier.widthIn(max = 300.dp),
            ) {
                Text(
                    text = message.text,
                    color = contentColor,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = DesignTokens.Spacing.md.dp, vertical = DesignTokens.Spacing.sm.dp),
                )
            }
        }
    }
}

@Composable
private fun ThinkingBubble() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Card(
            shape = RoundedCornerShape(
                topStart = DesignTokens.Shape.cornerLarge.dp,
                topEnd = DesignTokens.Shape.cornerLarge.dp,
                bottomStart = DesignTokens.Shape.cornerExtraSmall.dp,
                bottomEnd = DesignTokens.Shape.cornerLarge.dp,
            ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = DesignTokens.Spacing.md.dp, vertical = DesignTokens.Spacing.md.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val transition = rememberInfiniteTransition(label = "thinking")
                repeat(3) { index ->
                    val alpha by transition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = index * 150, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "dot$index",
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                                CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistInputBar(
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onMic: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.sm.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs.dp),
        ) {
            IconButton(onClick = onMic) {
                Icon(Icons.Filled.Mic, contentDescription = stringResource(R.string.assist_mic_content_description))
            }
            TextField(
                value = input,
                onValueChange = onInputChange,
                placeholder = { Text(stringResource(R.string.assist_input_placeholder)) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.weight(1f),
            )
            FilledIconButton(
                onClick = onSend,
                enabled = input.isNotBlank(),
                colors = IconButtonDefaults.filledIconButtonColors(),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.assist_send_content_description))
            }
        }
    }
}
