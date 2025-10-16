package com.niki914.breeno.ui.navigation

import android.app.Activity
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.niki914.breeno.aboutMe
import com.niki914.breeno.issueReport
import com.niki914.breeno.ui.compose.ChatTestingScreen
import com.niki914.breeno.ui.compose.MainSettingsScreen
import com.niki914.breeno.ui.compose.OtherSettingsScreen
import com.niki914.breeno.ui.compose.ShellCmdSettingsScreen
import com.niki914.breeno.viewmodel.ChatTestingViewModel
import com.niki914.core.MainMenuChoices

@Composable
fun Activity.MainNav() {
    val nav = rememberNavController()
    val chatTestingViewModel: ChatTestingViewModel = hiltViewModel() // 想要保留记录

    // 创建 NavHost，作为导航容器
    NavHost(
        navController = nav,
        startDestination = Screens.MainSettings.route // 设置起始页
    ) {
        // 定义 "main" 路由的目标 Composable
        animComposable(route = Screens.MainSettings.route) {
            MainSettingsScreen(
                onMenuItemClicked = { choice ->
                    when (choice) {
                        MainMenuChoices.About ->
                            aboutMe()

                        MainMenuChoices.OtherSettings ->
                            nav.navigate(Screens.OtherSettings.route)

                        MainMenuChoices.Report ->
                            issueReport("", "")

                        MainMenuChoices.Test ->
                            nav.navigate(Screens.ChatTest.route)
                    }
                }
            )
        }

        animComposable(Screens.OtherSettings.route) {
            OtherSettingsScreen(
                onNav = { route ->
                    nav.navigate(route)
                },
                onBack = {
                    nav.popBackStack() // 从后退栈中弹出，实现返回效果
                }
            )
        }

        animComposable(Screens.ShellCmdSettings.route) {
            ShellCmdSettingsScreen(
                onBack = {
                    nav.popBackStack() // 从后退栈中弹出，实现返回效果
                }
            )
        }

        animComposable(Screens.ChatTest.route) {
            ChatTestingScreen(
                chatTestingViewModel,
                onBack = {
                    nav.popBackStack() // 从后退栈中弹出，实现返回效果
                }
            )
        }
    }
}

private fun NavGraphBuilder.animComposable(
    route: String,
    content: @Composable (AnimatedContentScope.(NavBackStackEntry) -> Unit)
) {
    composable(
        route = route,
        enterTransition = { slideIn },
        exitTransition = { scaleOut + fadeOut },
        popEnterTransition = { scaleIn + fadeIn },
        popExitTransition = { slideOut },
        content = content
    )
}

// 统一动画规格，让动画看起来更协调
private val animationSpec = tween<Float>(durationMillis = 400)
private val animationSpecInt = tween<IntOffset>(durationMillis = 400)
private const val scale = 0.97F
private const val fadeAlpha = 0.6F

private val slideIn = slideInHorizontally(
    initialOffsetX = { fullWidth -> fullWidth },
    animationSpec = animationSpecInt
)

private val slideOut = slideOutHorizontally(
    targetOffsetX = { fullWidth -> fullWidth },
    animationSpec = animationSpecInt
)

private val scaleOut = scaleOut(animationSpec = animationSpec, targetScale = scale)
private val scaleIn = scaleIn(animationSpec = animationSpec, initialScale = scale)

private val fadeIn = fadeIn(animationSpec = animationSpec, initialAlpha = fadeAlpha)
private val fadeOut = fadeOut(animationSpec = animationSpec, targetAlpha = fadeAlpha)
