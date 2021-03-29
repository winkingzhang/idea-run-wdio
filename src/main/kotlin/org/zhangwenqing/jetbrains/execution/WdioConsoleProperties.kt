package org.zhangwenqing.jetbrains.execution

import com.intellij.execution.Executor
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.ui.ConsoleView
import com.intellij.javascript.testing.JsTestConsoleProperties
import com.intellij.terminal.TerminalExecutionConsole
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable


const val FRAMEWORK_NAME = "WdioJavaScriptTestRunner";

class WdioConsoleProperties constructor(
  @NotNull configuration: WdioRunConfiguration,
  @NotNull executor: Executor,
  @NotNull locator: SMTestLocator,
  withTerminalConsole: Boolean
) : JsTestConsoleProperties(configuration, FRAMEWORK_NAME, executor)
{
	private val myLocator: SMTestLocator = locator
	private val myWithTerminalConsole: Boolean = withTerminalConsole

	init
	{
		isUsePredefinedMessageFilter = false
		setIfUndefined(HIDE_PASSED_TESTS, false)
		setIfUndefined(HIDE_IGNORED_TEST, true)
		setIfUndefined(SCROLL_TO_SOURCE, true)
		setIfUndefined(SELECT_FIRST_DEFECT, true)
		isIdBasedTestTree = true
		isPrintTestingStartedTime = false
	}

	override fun createConsole(): ConsoleView
	{
		if (myWithTerminalConsole)
		{
			return object : TerminalExecutionConsole(project, null)
			{
				override fun attachToProcess(processHandler: ProcessHandler)
				{
					attachToProcess(processHandler, false)
				}
			}
		}
		return super.createConsole()
	}

	override fun getTestLocator(): SMTestLocator = this.myLocator

	@Nullable
	override fun createRerunFailedTestsAction(consoleView: ConsoleView?): AbstractRerunFailedTestsAction
	{
		return WdioRerunFailedTestAction((consoleView as SMTRunnerConsoleView?)!!, this)
	}
}
