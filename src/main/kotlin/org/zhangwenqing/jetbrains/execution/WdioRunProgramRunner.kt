package org.zhangwenqing.jetbrains.execution

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.runners.*
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.fileEditor.FileDocumentManager
import org.jetbrains.annotations.NotNull


class WdioRunProgramRunner<Settings : RunnerSettings> : GenericProgramRunner<Settings>()
{
	@NotNull
	override fun getRunnerId(): String = "RunnerForWdioJavaScript"

	override fun canRun(executorId: String, profile: RunProfile): Boolean
	{
		return "Run" == executorId && profile is WdioRunConfiguration
	}

	@Throws(ExecutionException::class)
	override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor?
	{
		FileDocumentManager.getInstance().saveAllDocuments()
		val executionResult = state.execute(
		  environment.executor,
		  (this as ProgramRunner<Settings>)
		) ?: return null
		val descriptor = RunContentBuilder(executionResult, environment)
		  .showRunContent(environment.contentToReuse)
		RerunTestsNotification.showRerunNotification(
		  environment.contentToReuse,
		  executionResult.executionConsole
		)
		RerunTestsAction.register(descriptor)
		return descriptor
	}
}
