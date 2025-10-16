package com.niki914.breeno.ui.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

fun MutableState<ToastInfo?>.showToast(newInfo: ToastInfo) {
    value = newInfo
}

fun MutableState<ToastInfo?>.showToast(
    message: String,
    icon: ImageVector? = null,
    duration: Long = 1500L,
    backgroundColor: Color,
    contentColor: Color
) {
    value = ToastInfo(
        message = message,
        icon = icon,
        duration = duration,
        backgroundColor = backgroundColor,
        contentColor = contentColor
    )
}

data class ToastInfo(
    val message: String,
    val icon: ImageVector? = null,
    val duration: Long = 3500L,
    val backgroundColor: Color = Color.Black.copy(alpha = 0.85f),
    val contentColor: Color = Color.White
)

/**
 * 可高度自定义、带动画的 Toast Composable
 */
@Composable
fun Toast(
    toastInfoState: MutableState<ToastInfo?>,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.BottomCenter
) {
    val currentToastInfo = toastInfoState.value
    LaunchedEffect(currentToastInfo) {
        if (currentToastInfo != null) {
            delay(currentToastInfo.duration)
            if (toastInfoState.value == currentToastInfo) {
                toastInfoState.value = null
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 80.dp, start = 16.dp, end = 16.dp),
        contentAlignment = contentAlignment
    ) {
        AnimatedContent(
            targetState = toastInfoState.value,
            label = "CustomToastAnimation",
            transitionSpec = {
                enterAnimation() togetherWith exitAnimation()
            },
            modifier = Modifier
                .wrapContentSize(align = Alignment.Center),
            contentAlignment = contentAlignment
        ) { info ->
            if (info != null) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = info.backgroundColor,
                    modifier = Modifier
//                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(28.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (info.icon != null) {
                            Icon(
                                imageVector = info.icon,
                                contentDescription = "Toast Icon",
                                tint = info.contentColor
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Text(
                            text = info.message,
                            color = info.contentColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

private fun enterAnimation(): EnterTransition {
    return fadeIn(animationSpec = tween(300)) +
            slideInVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                initialOffsetY = { it / 2 }
            ) +
            expandHorizontally(
                animationSpec = spring(
                    dampingRatio = 0.65f,
                    stiffness = Spring.StiffnessMediumLow
                ),
                expandFrom = Alignment.CenterHorizontally,
                initialWidth = { 0 }
            )
}

private fun exitAnimation(): ExitTransition {
    return fadeOut(animationSpec = tween(250)) +
            slideOutVertically(
                animationSpec = tween(300),
                targetOffsetY = { it }
            ) +
            shrinkHorizontally(
                animationSpec = tween(300),
                shrinkTowards = Alignment.CenterHorizontally,
                targetWidth = { 0 }
            )
}