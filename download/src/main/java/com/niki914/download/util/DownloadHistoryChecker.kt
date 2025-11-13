package com.niki914.download.util

import com.niki914.download.protocol.DownloadChunkState
import com.niki914.download.protocol.DownloadTask
import com.niki914.download.protocol.DownloadTaskState
import com.niki914.download.room.DownloadDao
import com.niki914.download.room.DownloadRepository

class DownloadHistoryChecker(dao: DownloadDao) {
    private val repo = DownloadRepository(dao)

    private val resumeHandler = ResumeHandler(repo, 2.MB)

    sealed interface State {
        data object NoHistory : State
        data class Interrupted(
            val taskState: DownloadTaskState,
            val chunkStates: List<DownloadChunkState>
        ) : State

        data object Downloaded : State
    }

    suspend fun check(task: DownloadTask): State {
        val existingTask = repo.findTask(task.url)
        val isValid = resumeHandler.isValidResumeTask(existingTask, task)

        if (!isValid)
            return State.NoHistory

        val handledData = resumeHandler.createHandledData(existingTask, task)

        val taskState: DownloadTaskState = handledData.taskState
        val chunkStates: List<DownloadChunkState> = handledData.chunkStates // 必须是此列表，因为它包含进度

        if (taskState.totalDownloadedBytes == task.totalSize)
            return State.Downloaded

        return State.Interrupted(taskState, chunkStates)
    }
}