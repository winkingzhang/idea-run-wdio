package org.zhangwenqing.jetbrains.execution

import com.intellij.execution.configuration.EnvironmentVariablesData
import org.junit.Test
import org.zhangwenqing.jetbrains.Common

class WdioRunSettingsTest : Common<WdioRunSettings>()
{
	@Test
	fun testWdioRunSettingDefaultValue()
	{
		val builder = WdioRunSettings.Builder()
		val settings = builder.build()
		assertNotNull(settings.interpreterRef)
		assertEmpty(settings.nodeOptions)
		assertNull(settings.wdioPackage)
		assertEmpty(settings.workingDir)
		assertEquals(settings.envData, EnvironmentVariablesData.DEFAULT)
		assertEmpty(settings.wdioConfigFilePath)
		assertEmpty(settings.testFilePath)
		assertEmpty(settings.testNames)
		assertEmpty(settings.testLineNumbers)
	}
}
