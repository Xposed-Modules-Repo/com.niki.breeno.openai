package com.niki914.core.repository

/**
 * 可写的
 */
interface IEditableSettingsRepository :
    IMainSettingsRepository,
    IOtherSettingsRepository,
    IShellCmdSettingsRepository