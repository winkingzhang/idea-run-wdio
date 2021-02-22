package org.zhangwenqing.jetbrains.execution

import com.google.common.collect.ImmutableList
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.annotations.NotNull


class WdioRunSettings constructor(@NotNull builder: Builder)
{
	val interpreterRef: NodeJsInterpreterRef = builder.myInterpreterRef
	val nodeOptions: String = builder.myNodeOptions
	val wdioPackage: NodePackage? = builder.myWdioPackage
	val workingDir: String = FileUtil.toSystemIndependentName(builder.myWorkingDir)
	val envData: EnvironmentVariablesData = builder.myEnvData
	val wdioConfigFilePath: String = builder.myWdioConfigFilePath
	val testFilePath: String = FileUtil.toSystemIndependentName(builder.myTestFilePath)
	val testNames: List<String> = ImmutableList.copyOf(builder.myTestNames) as List<String>
	var testLineNumbers: List<Int> = ImmutableList.copyOf(builder.myTestLineNumbers) as List<Int>

	companion object
	{
		@JvmStatic
		fun builder(runSettings: WdioRunSettings): Builder = Builder(runSettings)
	}

	fun builder(): Builder = Builder(this)

	class Builder
	{
		internal var myInterpreterRef: NodeJsInterpreterRef
		internal var myNodeOptions: String
		internal var myWdioPackage: NodePackage?
		internal var myWorkingDir: String
		internal var myEnvData: EnvironmentVariablesData
		internal var myWdioConfigFilePath: String
		internal var myTestFilePath: String
		internal var myTestNames: List<String>
		internal var myTestLineNumbers: List<Int>

		constructor()
		{
			myInterpreterRef = NodeJsInterpreterRef.createProjectRef()
			myNodeOptions = ""
			myWdioPackage = null
			myWorkingDir = ""
			myEnvData = EnvironmentVariablesData.DEFAULT
			myWdioConfigFilePath = ""
			myTestFilePath = ""
			myTestNames = ImmutableList.of<String>() as List<String>
			myTestLineNumbers = ImmutableList.of<Int>() as List<Int>
		}

		constructor(@NotNull runSettings: WdioRunSettings)
		{
			this.myInterpreterRef = runSettings.interpreterRef
			this.myNodeOptions = runSettings.nodeOptions
			this.myWdioPackage = runSettings.wdioPackage
			this.myWorkingDir = runSettings.workingDir
			this.myEnvData = runSettings.envData
			this.myWdioConfigFilePath = runSettings.wdioConfigFilePath
			this.myTestFilePath = runSettings.testFilePath
			this.myTestNames = runSettings.testNames
			this.myTestLineNumbers = runSettings.testLineNumbers
		}

		fun build(): WdioRunSettings = WdioRunSettings(this)

		fun setInterpreterRef(nodeJsInterpreterRef: NodeJsInterpreterRef): Builder
		{
			myInterpreterRef = nodeJsInterpreterRef
			return this
		}

		fun setNodeOptions(nodeOptions: String): Builder
		{
			myNodeOptions = nodeOptions
			return this
		}

		fun setWdioPackage(wdioPackage: NodePackage?): Builder
		{
			myWdioPackage = wdioPackage
			return this
		}

		fun setWorkingDir(workingDir: String): Builder
		{
			myWorkingDir = workingDir
			return this
		}

		fun setEnvData(envData: EnvironmentVariablesData): Builder
		{
			myEnvData = envData
			return this
		}

		fun setWdioConfigFilePath(wdioConfigFilePath: String): Builder
		{
			myWdioConfigFilePath = wdioConfigFilePath
			return this
		}

		fun setTestFilePath(testFilePath: String): Builder
		{
			myTestFilePath = testFilePath
			return this
		}

		fun setTestNames(testNames: List<String>): Builder
		{
			myTestNames = testNames
			return this
		}

		fun setTestLineNumbers(lineNumbers: List<Int>): Builder
		{
			myTestLineNumbers = lineNumbers
			return this
		}
	}
}
