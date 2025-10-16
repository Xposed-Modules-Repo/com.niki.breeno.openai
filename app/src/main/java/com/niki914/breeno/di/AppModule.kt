package com.niki914.breeno.di

import android.content.Context
import com.niki914.breeno.repository.MainSettingsRepository
import com.niki914.breeno.repository.OtherSettingsRepository
import com.niki914.breeno.repository.ShellCmdSettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // 表示这个模块提供的依赖是应用全局的（单例）
object AppModule {

    @Provides
    @Singleton
    fun provideMainSettingsRepository(@ApplicationContext context: Context): MainSettingsRepository {
        // Hilt 会自动传入 ApplicationContext
        return MainSettingsRepository.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideOtherSettingsRepository(@ApplicationContext context: Context): OtherSettingsRepository {
        return OtherSettingsRepository.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideShellCmdSettingsRepository(@ApplicationContext context: Context): ShellCmdSettingsRepository {
        return ShellCmdSettingsRepository.getInstance(context)
    }
}