package com.niki914.tool.call.models

import android.app.Application
import androidx.annotation.Keep
import com.niki.cmd.RootShell
import com.niki.cmd.model.bean.ShellResult
import com.niki914.chat.beans.PropertyDefinition
import com.niki914.core.ToolsNames
import com.niki914.core.getAs
import com.niki914.core.logD
import com.niki914.core.logV
import com.niki914.core.parseToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Keep
internal data object ShellModel : BaseToolModel() {
    override val name: String = ToolsNames.SHELL
    override val description: String =
        "Run a shell command with root access on the Android device. Note: " +
                "This command execution is stateless. Each call runs in a new, " +
                "isolated environment, and no state from previous commands " +
                "(like the current working directory) is preserved. Any multi-step operation, " +
                "such as navigating to a directory and listing its contents, " +
                "must be combined into a single command line (e.g., `cd /some_folder && ls -l`)."

    override val properties: Map<String, PropertyDefinition>
        get() = mapOf(
            "command" to PropertyDefinition(
                type = "string",
                description = "the command to run. (e.g., `input text 'hello world'`)"
            )
        )

    override val required: List<String> = listOf("command")

    private val rootShell = RootShell()
    private val userShell = RootShell()

    override suspend fun callInternal(
        args: JsonObject,
        extras: JsonObject,
        application: Application?
    ): JsonObject {
        val command = args.getAs<String>("command") ?: return illegalArgumentJson()

        val enableRootAccess = extras.getAs<Boolean>("enable_root_access") ?: false
        val isBlackList = extras.getAs<Boolean>("is_black_list") ?: true
        val list = extras.getAs<String>("list") ?: ""
//        val askBeforeExec = extras.getAs<Boolean>("enable_root_access") ?: false

        logV(extras.parseToString())
        logD("\nenable root: $enableRootAccess\nis black: $isBlackList\nlist: $list")

        fun safeInBlacklistMode(): Boolean {
            val commandWords = command.words()
            val (listWords, listParts) = list.lines().classifyLines()

            // 检查命令中是否出现黑名单关键字
            listWords.forEach { word ->
                logV("blacklist word: $word")
                if (commandWords.contains(word)) {
                    logD("发现黑名单关键字，禁止")
                    return false
                }
            }

            // 检查命令中是否出现黑名单片段
            return !listParts.any { part ->
                logV("blacklist part: $part")
                command.contains(part)
            }
        }

        fun safeInWhitelistMode(): Boolean {
            val cmds = list.lines()

            // 白名单直接激进一点
            return cmds.any { cmd ->
                logV("whitelist lines: $cmd")
                command == cmd
            }
        }

        val canExec = if (isBlackList) {
            safeInBlacklistMode()
        } else {
            safeInWhitelistMode()
        }

        logD("运行调用: $canExec")

        if (!canExec) {
            return if (isBlackList) {
                illegalArgumentJson("try to execute commands that is in blacklist($list)")
            } else {
                illegalArgumentJson("try to execute commands that is not in whitelist($list)")
            }
        }

        val shell = if (enableRootAccess) {
            rootShell.isAvailable() // 提前检查可用性
            rootShell
        } else {
            userShell
        }

        val result: ShellResult = shell.exec(command)

        return buildJsonObject {
            put("exit_code", result.exitCode)
            put("output", result.output)
        }
    }

    private fun String.words(): List<String> {
        return split(Regex("\\s+"))
//        return splitToSequence(' ', '\n')
            .filter { it.isNotBlank() }
            .map { it.trim() }
            .toList()
    }

    // a: words
    // b: others
    private fun List<String>.classifyLines(): Pair<List<String>, List<String>> {
        val wordLines = mutableListOf<String>()
        val otherLines = mutableListOf<String>()

        filter { it.isNotBlank() }.forEach { line ->
            if (line.trim().contains(" ")) {
                otherLines.add(line)
            } else {
                wordLines.add(line)
            }
        }
        return Pair(wordLines, otherLines)
    }
}