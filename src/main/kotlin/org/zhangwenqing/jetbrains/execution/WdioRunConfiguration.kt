package org.zhangwenqing.jetbrains.execution

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.TestRunnerBundle
import com.intellij.execution.testframework.sm.runner.SMRunnerConsolePropertiesProvider
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.javascript.JSRunProfileWithCompileBeforeLaunchOption
import com.intellij.javascript.nodejs.debug.NodeDebugRunConfiguration
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.javascript.testFramework.PreferableRunConfiguration
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.WriteExternalException
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.util.containers.ContainerUtil
import org.jdom.Element
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.io.LocalFileFinder
import org.zhangwenqing.jetbrains.WdioUtil
import java.io.File


open class WdioRunConfiguration constructor(
  @NotNull project: Project,
  @NotNull factory: ConfigurationFactory,
  @NotNull name: String
) :
  LocatableConfigurationBase<WdioRunConfiguration>(project, factory, name),
  JSRunProfileWithCompileBeforeLaunchOption,
  NodeDebugRunConfiguration,
  PreferableRunConfiguration,
  SMRunnerConsolePropertiesProvider
{
	private var myRunSettings = WdioRunSettings.Builder().build()

	override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
	  WdioRunConfigurationEditor(project)

	@Throws(InvalidDataException::class)
	override fun readExternal(@NotNull element: Element)
	{
		super.readExternal(element)
		this.myRunSettings = WdioRunSettingsSerializationUtil.readFromXml(element)
	}

	@Throws(WriteExternalException::class)
	override fun writeExternal(@NotNull element: Element)
	{
		super.writeExternal(element)
		WdioRunSettingsSerializationUtil.writeToXml(element, this.myRunSettings)
	}

	@Nullable
	override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState?
	{
		return WdioRunProfileState(
		  project,
		  this,
		  environment,
		  getWdioPackage(),
		  myRunSettings
		)
	}

	override val interpreter: NodeJsInterpreter?
		get() = myRunSettings.interpreterRef.resolve(project)

	fun getWdioPackage(): NodePackage
	{
		var pkg: NodePackage? = myRunSettings.wdioPackage
		if (pkg == null)
		{
			val project = project
			val interpreter = myRunSettings.interpreterRef.resolve(project)
			pkg = WdioUtil.PACKAGE_DESCRIPTOR.findFirstDirectDependencyPackage(
			  project,
			  interpreter,
			  getContextFile()
			)
			if (pkg.isEmptyPath)
			{
				pkg = WdioUtil.getWdioPackage(project)
			}
			else
			{
				WdioUtil.setWdioPackage(project, pkg)
			}
			myRunSettings = myRunSettings.builder().setWdioPackage(pkg).build()
		}
		return pkg
	}

	private fun getContextFile(): VirtualFile?
	{
		var f: VirtualFile? = findFile(myRunSettings.testFilePath)
		if (f == null)
		{
			f = findFile(myRunSettings.workingDir)
		}
		return f
	}

	@Nullable
	private fun findFile(path: String): VirtualFile?
	{
		return if (FileUtil.isAbsolute(path))
		{
			LocalFileSystem.getInstance().findFileByPath(path)
		}
		else null
	}

	@NotNull
	override fun createTestConsoleProperties(executor: Executor): SMTRunnerConsoleProperties
	{
		return createTestConsoleProperties(executor, true)
	}

	fun createTestConsoleProperties(executor: Executor, withTerminalConsole: Boolean): WdioConsoleProperties
	{
		return WdioConsoleProperties(this, executor, WdioTestLocationProvider(), withTerminalConsole)
	}

	@Nullable
	override fun getActionName(): String
	{
		if (myRunSettings.testNames.isNotEmpty())
		{
			return StringUtil.notNullize(ContainerUtil.getLastItem(myRunSettings.testNames))
		}
		if (myRunSettings.testFilePath.isNotEmpty())
		{
			return StringUtil.notNullize(getLastPathComponent(myRunSettings.testFilePath))
		}
		return TestRunnerBundle.message("all.tests.scope.presentable.text")
	}

	override fun suggestedName(): String?
	{
		val runSettings: WdioRunSettings = myRunSettings

		if (runSettings.testNames.isNotEmpty())
		{
			return StringUtil.join(runSettings.testNames, ".")
		}

		return if (runSettings.testFilePath.isNotEmpty()) getRelativePath(project, runSettings.testFilePath)
		else TestRunnerBundle.message("all.tests.scope.presentable.text")
	}

	fun getRunSettings(): WdioRunSettings = myRunSettings

	fun setRunSettings(@NotNull wdioRunSettings: WdioRunSettings)
	{
		val pkg = wdioRunSettings.wdioPackage

		myRunSettings = wdioRunSettings
		if (pkg != null)
		{
			WdioUtil.setWdioPackage(project, pkg)
		}
	}

	override fun isPreferredOver(p0: RunConfiguration, p1: PsiElement): Boolean
	{
		return myRunSettings.wdioPackage?.isValid(null, null)!!
	}

	override fun onNewConfigurationCreated()
	{
		val builder: WdioRunSettings.Builder = myRunSettings.builder()
		if (myRunSettings.workingDir.trim { it <= ' ' }.isEmpty())
		{
			val basePath = project.basePath
			if (basePath != null)
			{
				builder.setWorkingDir(basePath)
			}
		}
		myRunSettings = builder.build()
	}

	companion object
	{
		@NotNull
		private fun getRelativePath(project: Project, path: String): String
		{
			val file: VirtualFile? = LocalFileFinder.findFile(path)
			if (file != null && file.isValid)
			{
				val root = ProjectFileIndex.getInstance(project).getContentRootForFile(file)
				if (root != null && root.isValid)
				{
					val relativePath = VfsUtilCore.getRelativePath(file, root, File.separatorChar)
					if (StringUtil.isNotEmpty(relativePath))
					{
						return relativePath!!
					}
				}
			}
			return getLastPathComponent(path)
		}

		@NotNull
		private fun getLastPathComponent(path: String): String
		{
			val lastIndex = path.lastIndexOf('/')
			return if (lastIndex >= 0)
			{
				path.substring(lastIndex + 1)
			}
			else path
		}
	}
}
