package com.niki914.core.repository

/**
 * 只读的
 */
interface ISettingsRepository :
    IMainSettingsRepository_ReadOnly,
    IOtherSettingsRepository_ReadOnly,
    IShellCmdSettingsRepository_ReadOnly