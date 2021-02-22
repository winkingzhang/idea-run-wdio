package org.zhangwenqing.jetbrains.execution

import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef
import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import org.jdom.Element
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.zhangwenqing.jetbrains.WdioUtil

const val NODE_INTERPRETER__KEY = "node-interpreter"
const val NODE_OPTIONS__KEY = "node-options"
const val WDIO_PACKAGE___KEY = "wdio-package"
const val WORKING_DIRECTORY__KEY = "working-directory"
const val PASS_PARENT_ENV__KEY = "pass-parent-env"
const val WDIO_CONFIG_FILE_PATH__KEY = "wdio-config-file-path"
const val TEST_FILE__KEY = "test-file"
const val TEST_NAMES__KEY = "test-names"
const val TEST_LINE_NUMBERS__KEY = "test-line-numbers"
const val TEST_NAME__KEY = "name"

object WdioRunSettingsSerializationUtil
{
	@JvmStatic
	@NotNull
	fun readFromXml(@NotNull parent: Element): WdioRunSettings
	{
		val builder = WdioRunSettings.Builder()

		val interpreterRefName: String? = readTagNullable(parent, NODE_INTERPRETER__KEY)
		builder.setInterpreterRef(NodeJsInterpreterRef.create(StringUtil.notNullize(interpreterRefName)))

		val nodeOptions: String = readTag(parent, NODE_OPTIONS__KEY)
		builder.setNodeOptions(nodeOptions)

		val pkg: String? = readTagNullable(parent, WDIO_PACKAGE___KEY)
		if (pkg != null)
		{
			builder.setWdioPackage(WdioUtil.PACKAGE_DESCRIPTOR.createPackage(pkg))
		}
		val workingDirPath: String = readTag(parent, WORKING_DIRECTORY__KEY)
		builder.setWorkingDir(FileUtil.toSystemDependentName(workingDirPath))

		var envData = EnvironmentVariablesData.readExternal(parent)
		val passParentEnvStr: String = readTag(parent, PASS_PARENT_ENV__KEY)
		if (StringUtil.isNotEmpty(passParentEnvStr))
		{
			envData = EnvironmentVariablesData.create(envData.envs, passParentEnvStr.toBoolean())
		}
		builder.setEnvData(envData)

		val extraWdioOptions: String = readTag(parent, WDIO_CONFIG_FILE_PATH__KEY)
		builder.setWdioConfigFilePath(extraWdioOptions)

		builder.setTestFilePath(
		  FileUtil.toSystemDependentName(
			StringUtil.notNullize(readTag(parent, TEST_FILE__KEY))
		  )
		)
		builder.setTestNames(readTestNames(parent))

		builder.setTestLineNumbers(readTestLineNumbers(parent))

		return builder.build()
	}

	private fun readTestNames(parent: Element): List<String>
	{
		val testNamesElement = parent.getChild(TEST_NAMES__KEY) ?: return emptyList()
		return JDOMExternalizerUtil.getChildrenValueAttributes(testNamesElement, TEST_NAME__KEY)
	}

	private fun readTestLineNumbers(parent: Element): List<Int>
	{
		val testLineNumberElement = parent.getChild(TEST_LINE_NUMBERS__KEY) ?: return emptyList()
		return JDOMExternalizerUtil.getChildrenValueAttributes(testLineNumberElement, TEST_NAME__KEY).map {
			it.toInt()
		}
	}

	private fun readTag(parent: Element, tagName: String): String =
	  StringUtil.notNullize(readTagNullable(parent, tagName))

	@Nullable
	private fun readTagNullable(@NotNull parent: Element, @NotNull tagName: String): String?
	{
		val child = parent.getChild(tagName)
		return child?.text
	}

	@JvmStatic
	fun writeToXml(@NotNull parent: Element, @NotNull runSettings: WdioRunSettings)
	{
		val interpreterRef = runSettings.interpreterRef
		writeTag(parent, NODE_INTERPRETER__KEY, interpreterRef.referenceName)
		writeTag(parent, NODE_OPTIONS__KEY, runSettings.nodeOptions)
		if (runSettings.wdioPackage != null)
		{
			writeTag(parent, WDIO_PACKAGE___KEY, runSettings.wdioPackage.systemIndependentPath)
		}
		val workingDirPath = FileUtil.toSystemIndependentName(runSettings.workingDir)
		writeTag(parent, WORKING_DIRECTORY__KEY, workingDirPath)
		writeTag(parent, PASS_PARENT_ENV__KEY, runSettings.envData.isPassParentEnvs.toString())
		EnvironmentVariablesComponent.writeExternal(parent, runSettings.envData.envs)
		writeTag(parent, WDIO_CONFIG_FILE_PATH__KEY, runSettings.wdioConfigFilePath)

		writeTag(parent, TEST_FILE__KEY, FileUtil.toSystemIndependentName(runSettings.testFilePath))
		writeTestNames(parent, runSettings.testNames)
		writeTestLineNumber(parent, runSettings.testLineNumbers)
	}

	private fun writeTestNames(parent: Element, testNames: List<String>)
	{
		if (testNames.isNotEmpty())
		{
			val testNamesElement = Element(TEST_NAMES__KEY)
			JDOMExternalizerUtil.addChildrenWithValueAttribute(testNamesElement, TEST_NAME__KEY, testNames)
			parent.addContent(testNamesElement)
		}
	}

	private fun writeTestLineNumber(parent: Element, testLineNumbers: List<Int>)
	{
		if (testLineNumbers.isNotEmpty())
		{
			val testNamesElement = Element(TEST_LINE_NUMBERS__KEY)
			JDOMExternalizerUtil.addChildrenWithValueAttribute(
			  testNamesElement,
			  TEST_NAME__KEY,
			  testLineNumbers.map { it.toString() })
			parent.addContent(testNamesElement)
		}
	}

	private fun writeTag(parent: Element, tagName: String, value: String)
	{
		val element = Element(tagName)
		element.text = value
		parent.addContent(element)
	}
}
