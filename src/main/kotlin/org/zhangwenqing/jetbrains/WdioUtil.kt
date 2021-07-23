package org.zhangwenqing.jetbrains

import com.intellij.ide.util.PropertiesComponent
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.javascript.nodejs.util.NodePackageDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.annotations.NotNull


object WdioUtil
{
	const val NODE_PACKAGE_NAME = "@wdio/cli"

	val PACKAGE_DESCRIPTOR: NodePackageDescriptor = NodePackageDescriptor(NODE_PACKAGE_NAME)

	const val WDIO_PACKAGE_DIR__KEY: String = "nodejs.wdio.wdio_node_package_dir"

	const val FRAMEWORK_MOCHA = "Mocha"
	const val FRAMRWORK_JASMINE = "Jasmine"
	const val FRAMRWORK_CUCUMBER = "Cucumber"

	@NotNull
	fun getWdioPackage(@NotNull project: Project): NodePackage
	{
		val packageDir = PropertiesComponent.getInstance(project).getValue(WDIO_PACKAGE_DIR__KEY)
		return PACKAGE_DESCRIPTOR.createPackage(StringUtil.notNullize(packageDir))
	}

	fun setWdioPackage(project: Project, wdioPackage: NodePackage)
	{
		PropertiesComponent.getInstance(project)
		  .setValue(WDIO_PACKAGE_DIR__KEY, wdioPackage.systemIndependentPath)
	}
}
