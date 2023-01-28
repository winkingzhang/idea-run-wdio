package org.zhangwenqing.jetbrains.execution

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.filters.Filter
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ExecutionConsole
import com.intellij.javascript.debugger.CommandLineDebugConfigurator
import com.intellij.javascript.nodejs.NodeCommandLineUtil
import com.intellij.javascript.nodejs.NodeConsoleAdditionalFilter
import com.intellij.javascript.nodejs.NodeStackTraceFilter
import com.intellij.javascript.nodejs.debug.NodeLocalDebuggableRunProfileStateSync
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.execution.ParametersListUtil
import com.jetbrains.nodejs.mocha.execution.MochaRunProfileState.getMochaMainJsFile
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.zhangwenqing.jetbrains.WdioUtil.FRAMEWORK_MOCHA
import org.zhangwenqing.jetbrains.WdioUtil.FRAMRWORK_CUCUMBER
import org.zhangwenqing.jetbrains.WdioUtil.FRAMRWORK_JASMINE
import java.io.File
import java.nio.charset.StandardCharsets


class WdioRunProfileState constructor(
  @NotNull private val project: Project,
  @NotNull private val runConfiguration: WdioRunConfiguration,
  @NotNull private val env: ExecutionEnvironment,
  @NotNull private val wdioPackage: NodePackage,
  @NotNull val runSettings: WdioRunSettings
) : NodeLocalDebuggableRunProfileStateSync()
{
	private var myRerunActionFailedTests: List<List<String>>? = null

	@Throws(ExecutionException::class)
	override fun executeSync(@Nullable configurator: CommandLineDebugConfigurator?): ExecutionResult
	{
		val interpreter: NodeJsInterpreter = this.runSettings.interpreterRef.resolveNotNull(this.project)
		val commandLine = NodeCommandLineUtil.createCommandLineForTestTools()
		NodeCommandLineUtil.configureCommandLine(commandLine, configurator, interpreter) {
			configureCommandLine(commandLine, interpreter, it)
		}
		val processHandler = NodeCommandLineUtil.createProcessHandler(commandLine, false)
		val consoleProperties: WdioConsoleProperties = this.runConfiguration.createTestConsoleProperties(
		  this.env.executor,
		  NodeCommandLineUtil.shouldUseTerminalConsole(processHandler as ProcessHandler)
		)
		val consoleView: ConsoleView = createSMTRunnerConsoleView(commandLine.workDirectory, consoleProperties)
		ProcessTerminatedListener.attach(processHandler as ProcessHandler)
		consoleView.attachToProcess(processHandler as ProcessHandler)
		val executionResult = DefaultExecutionResult(consoleView as ExecutionConsole, processHandler as ProcessHandler)
		executionResult.setRestartActions(consoleProperties.createRerunFailedTestsAction(consoleView) as AnAction?)
		return executionResult
	}

	private fun createSMTRunnerConsoleView(workingDirectory: File, consoleProperties: WdioConsoleProperties): ConsoleView
	{
		val baseTestsOutputConsoleView = SMTestRunnerConnectionUtil.createConsole(
		  consoleProperties.testFrameworkName,
		  (consoleProperties as TestConsoleProperties)
		)
		consoleProperties.addStackTraceFilter(NodeStackTraceFilter(this.project, workingDirectory) as Filter)
		for (filter in consoleProperties.stackTrackFilters)
		{
			baseTestsOutputConsoleView.addMessageFilter(filter)
		}
		baseTestsOutputConsoleView.addMessageFilter(
		  NodeConsoleAdditionalFilter(this.project, workingDirectory) as Filter
		)
		return baseTestsOutputConsoleView
	}

	@Throws(ExecutionException::class)
	private fun configureCommandLine(
	  commandLine: GeneralCommandLine,
	  interpreter: NodeJsInterpreter,
	  debugMode: Boolean
	)
	{
		val nodeOptions: List<String> = ArrayList(commandLine.parametersList.parameters)
		commandLine.parametersList.clearAll()
		commandLine.charset = StandardCharsets.UTF_8
		if (!StringUtil.isEmptyOrSpaces(this.runSettings.workingDir))
		{
			commandLine.withWorkDirectory(this.runSettings.workingDir)
		}
		NodeCommandLineUtil.configureUsefulEnvironment(commandLine)
		NodeCommandLineUtil.prependNodeDirToPATH(commandLine, interpreter)
		this.runSettings.envData.configureCommandLine(commandLine, true)

		commandLine.addParameter(getMochaMainJsFile(interpreter, this.wdioPackage).absolutePath)
		commandLine.addParameters(nodeOptions)
		commandLine.addParameters(ParametersListUtil.parse(this.runSettings.nodeOptions.trim()))

		commandLine.addParameter("run")
		var wdioConfigFilePath = this.runSettings.wdioConfigFilePath.trim()
		if (wdioConfigFilePath.isEmpty())
		{
			wdioConfigFilePath = "wdio.conf.js"
		}
		val extraWdioOptionList = ParametersListUtil.parse(wdioConfigFilePath)
		commandLine.addParameters(extraWdioOptionList)

		commandLine.addParameter("--framework")
		commandLine.addParameter(this.runSettings.framework)

		if (debugMode) {
			when (this.runSettings.framework) {
				FRAMEWORK_MOCHA ->
				{
					commandLine.addParameter("--mochaOpts.timeout")
				}
				FRAMRWORK_JASMINE ->
				{
					commandLine.addParameter("--jasmineOpts.defaultTimeoutInterval")
				}
				FRAMRWORK_CUCUMBER ->
				{
					commandLine.addParameter("--cucumberOpts.timeout")
				}
			}
			commandLine.addParameter("0")
		}

		if (!StringUtil.isEmptyOrSpaces(this.runSettings.testFilePath))
		{
			commandLine.addParameter("--spec")
			if (this.runSettings.testLineNumbers.isEmpty())
			{
				commandLine.addParameter(FileUtil.toSystemDependentName(this.runSettings.testFilePath))
			}
			else
			{
				commandLine.addParameter(
				  "${
					  FileUtil.toSystemDependentName(this.runSettings.testFilePath)
				  }:${runSettings.testLineNumbers[0]}"
				)
			}
		}
	}

	fun setFailedTests(rerunActionFailedTests: List<List<String>>?)
	{
		myRerunActionFailedTests = rerunActionFailedTests
	}
}
