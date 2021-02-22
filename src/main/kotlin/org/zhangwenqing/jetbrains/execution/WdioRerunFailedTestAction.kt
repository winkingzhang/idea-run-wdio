package org.zhangwenqing.jetbrains.execution

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.AbstractTestProxy
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.javascript.testFramework.util.EscapeUtils
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.util.*


class WdioRerunFailedTestAction constructor(
  @NotNull consoleView: SMTRunnerConsoleView,
  @NotNull consoleProperties: WdioConsoleProperties
) : AbstractRerunFailedTestsAction(consoleView)
{
	init
	{
		init(consoleProperties)
		model = consoleView.resultsViewer
	}

	@Nullable
	override fun getRunProfile(environment: ExecutionEnvironment): MyRunProfile?
	{
		val configuration = myConsoleProperties.configuration as WdioRunConfiguration
		val state = WdioRunProfileState(
		  configuration.project,
		  configuration,
		  environment,
		  configuration.getWdioPackage(),
		  configuration.getRunSettings()
		)
		state.setFailedTests(convertToTestFqns(getFailedTests(configuration.project)))
		return object : MyRunProfile((configuration as RunConfigurationBase<*>))
		{
			override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState = state
		}
	}

	private fun convertToTestFqns(tests: List<AbstractTestProxy>): List<List<String>>
	{
		val result: MutableList<List<String>> = ArrayList()
		for (test in tests)
		{
			val fqn: List<String>? = convertToTestFqn(test)
			if (fqn != null)
			{
				result.add(fqn)
			}
		}
		return result
	}

	private fun convertToTestFqn(test: AbstractTestProxy): List<String>?
	{
		val url = test.locationUrl
		if (test.isLeaf && url != null)
		{
			val testFqn = EscapeUtils.split(VirtualFileManager.extractPath(url), '.')
			if (testFqn.isNotEmpty())
			{
				return testFqn
			}
		}
		return null
	}
}
