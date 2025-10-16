//package com.niki914.breeno.ui.compose
//
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.tooling.preview.Preview
//import com.niki914.breeno.repository.MainSettingsRepository
//import com.niki914.breeno.repository.OtherSettingsRepository
//import com.niki914.breeno.repository.ShellCmdSettingsRepository
//import com.niki914.breeno.ui.compose.theme.BreenoTheme
//
//
//@Preview(showBackground = true, showSystemUi = true)
//@Composable
//fun MainScreenPreview() {
//    BreenoTheme {
//        MainSettingsScreen {}
//    }
//}
//
//@Preview(showBackground = true, showSystemUi = true)
//@Composable
//fun OtherSettingsScreenPreview() {
//    val mockOnBack: () -> Unit = {
//        println("Back button clicked in preview!")
//    }
//
//    BreenoTheme {
//        OtherSettingsScreen(
//            onBack = mockOnBack
//        )
//    }
//}
//
//@Preview(showBackground = true, showSystemUi = true)
//@Composable
//fun ShellSmdSettingsScreenPreview() {
//    val mockOnBack: () -> Unit = {
//        println("Back button clicked in preview!")
//    }
//
//    BreenoTheme {
//        ShellCmdSettingsScreen(
//            onBack = mockOnBack
//        )
//    }
//}
//
//@Preview(showBackground = true, showSystemUi = true)
//@Composable
//fun ChatTestScreenPreview() {
//    val context = composableContext.applicationContext
//
//    MainSettingsRepository.getInstance(context)
//    ShellCmdSettingsRepository.getInstance(context)
//    OtherSettingsRepository.getInstance(context)
//
//    val mockOnBack: () -> Unit = {
//        println("Back button clicked in preview!")
//    }
//
//    BreenoTheme {
//        ChatTestScreen(
//            onBack = mockOnBack
//        )
//    }
//}