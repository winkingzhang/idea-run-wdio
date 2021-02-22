package org.zhangwenqing.jetbrains.execution

import com.intellij.execution.configurations.ConfigurationTypeUtil.findConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationSingletonPolicy
import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import icons.WdioIcons
import org.jetbrains.annotations.NotNull
import org.zhangwenqing.jetbrains.WdioBundle


class WdioConfigurationType :
  SimpleConfigurationType(
	"WdioConfigurationType",
	WdioBundle.message("wdio.name"),
	WdioBundle.message("wdio.run.configuration.description"),
	NotNullLazyValue.createConstantValue(WdioIcons.wdio)
  ),
  DumbAware
{

	override fun getTag(): String = "wdio"

	@NotNull
	override fun createTemplateConfiguration(project: Project): RunConfiguration
	{
		return WdioRunConfiguration(project, this, "Wdio")
	}

	override fun getSingletonPolicy(): RunConfigurationSingletonPolicy =
	  RunConfigurationSingletonPolicy.SINGLE_INSTANCE_ONLY

	override fun isEditableInDumbMode(): Boolean = true

	companion object
	{
		@JvmStatic
		fun getInstance(): WdioConfigurationType
		{
			return findConfigurationType(WdioConfigurationType::class.java)
		}
	}
}
