package org.zhangwenqing.jetbrains.execution

import com.intellij.execution.RunManager
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.ide.plugins.PluginManager
import com.intellij.javascript.testFramework.JsTestElementPath
import com.intellij.javascript.testFramework.PreferableRunConfiguration
import com.intellij.javascript.testFramework.interfaces.mochaTdd.MochaTddFileStructureBuilder
import com.intellij.javascript.testFramework.jasmine.JasmineFileStructureBuilder
import com.intellij.javascript.testing.JsTestRunConfigurationProducer
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.ObjectUtils
import com.intellij.util.SmartList
import com.intellij.util.containers.SmartHashSet
import com.jetbrains.nodejs.mocha.execution.MochaRunConfiguration
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.zhangwenqing.jetbrains.WdioUtil

const val MAX_DEPTH = 5

class WdioRunConfigurationProducer : JsTestRunConfigurationProducer<WdioRunConfiguration>(
  WdioUtil.PACKAGE_DESCRIPTOR,
  getStopPackageNames()
)
{
	override fun getConfigurationFactory(): ConfigurationFactory = WdioConfigurationType.getInstance()

	override fun setupConfigurationFromCompatibleContext(
	  configuration: WdioRunConfiguration,
	  context: ConfigurationContext,
	  sourceElement: Ref<PsiElement?>
	): Boolean
	{
		val element = context.psiLocation ?: return false
		if (!isActiveFor(element, context))
		{
			return false
		}
		val elementRunInfo: TestElementInfo = createTestElementRunInfo(element, configuration.getRunSettings())
		  ?: return false
		var runSettings: WdioRunSettings = elementRunInfo.runSettings

		if (runSettings.wdioPackage?.isValid != true)
		{
			runSettings = runSettings.builder()
			  .setWdioPackage(configuration.getWdioPackage())
			  .build()

			if (runSettings.wdioPackage?.isValid != true)
			{
				return false
			}
		}

		if (StringUtil.isEmptyOrSpaces(runSettings.wdioConfigFilePath))
		{
			var compilerWdioOption: String? = getWdioConfigFilePath(runSettings, getOriginalPsiFile(element))
			if (compilerWdioOption == null)
			{
				compilerWdioOption = "wdio.conf.js"
			}
			runSettings = runSettings.builder()
			  .setWdioConfigFilePath(compilerWdioOption)
			  .build()
		}

		configuration.setRunSettings(runSettings)
		sourceElement.set(elementRunInfo.enclosingTestElement)
		configuration.setGeneratedName()
		return true
	}

	private fun isActiveFor(element: PsiElement, context: ConfigurationContext): Boolean
	{
		val file = PsiUtilCore.getVirtualFile(element) ?: return false
		if (isTestRunnerPackageAvailableFor(element, context))
		{
			return true
		}
		val roots: List<VirtualFile> = collectWdioTestRoots(element.project)
		if (roots.isEmpty())
		{
			return false
		}
		val testSet = SmartHashSet<VirtualFile?>()
		for (root in roots)
		{
			if (root.isDirectory)
			{
				testSet.add(root)
				continue
			}
			if (root == file)
			{
				return true
			}
		}
		return VfsUtilCore.isUnder(file, testSet)
	}

	override fun isConfigurationFromCompatibleContext(
	  configuration: WdioRunConfiguration,
	  context: ConfigurationContext
	): Boolean
	{
		val element = context.psiLocation ?: return false
		val elementRunInfo = createTestElementRunInfo(element, configuration.getRunSettings()) ?: return false
		val thisRunSettings: WdioRunSettings = elementRunInfo.runSettings
		val thatRunSettings = configuration.getRunSettings()

		return thisRunSettings.testFilePath == thatRunSettings.testFilePath &&
		  thisRunSettings.testNames == thatRunSettings.testNames
	}


	override fun isPreferredConfiguration(self: ConfigurationFromContext, other: ConfigurationFromContext?): Boolean
	{
		if (other != null)
		{
			val otherRc = ObjectUtils.tryCast(
			  other.configuration,
			  PreferableRunConfiguration::class.java
			)

			return (otherRc == null
			  || otherRc is MochaRunConfiguration
			  || !otherRc.isPreferredOver(self.configuration, self.sourceElement))
		}
		return true
	}

	companion object
	{
		private fun createSuiteOrTestData(element: PsiElement): Pair<String, JsTestElementPath>?
		{
			if (element is PsiFileSystemItem)
			{
				return null
			}
			val jsFile = ObjectUtils.tryCast(element.containingFile, JSFile::class.java)
			val textRange = element.textRange
			if (jsFile == null || textRange == null)
			{
				return null
			}
			var path = JasmineFileStructureBuilder.getInstance()
			  .fetchCachedTestFileStructure(jsFile)
			  .findTestElementPath(textRange)
			if (path != null)
			{
				return Pair("bdd", path)
			}
			val tddStructure = MochaTddFileStructureBuilder.getInstance()
			  .fetchCachedTestFileStructure(jsFile)
			path = tddStructure.findTestElementPath(textRange)
			return if (path != null)
			{
				if (tddStructure.hasMochaTypeScriptDeclarations())
				{
					Pair("mocha-typescript", path)
				}
				else Pair("tdd", path)
			}
			else null
		}

		@NotNull
		private fun getStopPackageNames(): List<String?>
		{
			val karma = PluginManager.getInstance().findEnabledPlugin(PluginId.getId("Karma"))
			if (karma != null && karma.isEnabled)
			{
				return listOf("karma")
			}
			return emptyList()
		}

		@NotNull
		private fun collectWdioTestRoots(project: Project): List<VirtualFile>
		{
			val list = RunManager.getInstance(project)
			  .getConfigurationsList((WdioConfigurationType.getInstance() as ConfigurationType))
			val smartList: SmartList<VirtualFile> = SmartList()
			for (configuration in list)
			{
				if (configuration is WdioRunConfiguration)
				{
					val settings = configuration.getRunSettings()
					val path: String = settings.testFilePath
					if (!StringUtil.isEmptyOrSpaces(path))
					{
						val dir = LocalFileSystem.getInstance().findFileByPath(path)
						if (dir != null)
						{
							smartList.add(dir)
						}
					}
				}
			}
			return smartList
		}

		@Nullable
		private fun getOriginalPsiFile(@Nullable element: PsiElement?): PsiFile? =
		  element?.containingFile?.originalFile

		@Nullable
		private fun getWdioConfigFilePath(runSettings: WdioRunSettings, psiFile: PsiFile?): String?
		{
			if (psiFile == null)
			{
				return null
			}
			val workingDir = LocalFileSystem.getInstance().findFileByPath(runSettings.workingDir)
			if (workingDir == null || !workingDir.isValid)
			{
				return null
			}

			return findConfigFileRecursive(workingDir)?.path
		}

		private fun findConfigFileRecursive(folder: VirtualFile, depth: Int = 0): VirtualFile?
		{
			if (depth >= MAX_DEPTH)
			{
				return null
			}

			val wdioConfigFilePaths = arrayOf(
			  "wdio.local.conf.js",
			  "wdio.ios.conf.js",
			  "wdio.conf.js",
			)

			for (configFilePath in wdioConfigFilePaths)
			{
				val wdioConfigFile = folder.findFileByRelativePath(configFilePath)
				if (wdioConfigFile != null && wdioConfigFile.isValid && !wdioConfigFile.isDirectory)
				{
					return wdioConfigFile
				}
			}

			for (subFolder in folder.children)
			{
				if (subFolder.isDirectory)
				{
					val config = findConfigFileRecursive(subFolder, depth + 1)
					if (config != null && config.isValid && !config.isDirectory)
					{
						return config
					}
				}
			}

			return null
		}

		private fun createTestElementRunInfo(
		  element: PsiElement,
		  templateRunSettings: WdioRunSettings
		): TestElementInfo?
		{
			var wdioRunSettings = templateRunSettings
			val virtualFile = PsiUtilCore.getVirtualFile(element) ?: return null
			val pair: Pair<String, JsTestElementPath>? = createSuiteOrTestData(element)
			if (StringUtil.isEmptyOrSpaces(wdioRunSettings.workingDir))
			{
				val workingDir: String = guessWorkingDirectory(element.project, virtualFile.path)?.path ?: ""
				wdioRunSettings = wdioRunSettings.builder().setWorkingDir(workingDir).build()
			}

			if (pair == null)
			{
				return createFileInfo(element, virtualFile, wdioRunSettings)
			}

			val builder = wdioRunSettings.builder()
			builder.setTestFilePath(virtualFile.path)
			val testElementPath = pair.second
			val testName = testElementPath.testName
			if (testName != null)
			{
				val names: MutableList<String> = ArrayList(testElementPath.suiteNames)
				names.add(testName)
				builder.setTestNames(names)

				val psiFile = ObjectUtils.tryCast(element.containingFile, JSFile::class.java)
				if (psiFile != null)
				{
					val lineNumbers: MutableList<Int> = ArrayList()
					PsiDocumentManager.getInstance(element.project)
					  .getDocument(psiFile)?.getLineNumber(element.textOffset)?.let {
						  lineNumbers.add(it + 1)
					  }
					builder.setTestLineNumbers(lineNumbers)
				}

			}
			return TestElementInfo(builder.build(), testElementPath.testElement)
		}

		private fun createFileInfo(
		  element: PsiElement,
		  virtualFile: VirtualFile,
		  templateRunSettings: WdioRunSettings
		): TestElementInfo?
		{
			if (virtualFile.isDirectory)
			{
				val builder = templateRunSettings.builder()
				return TestElementInfo(builder.build(), element)
			}

			val psiFile = ObjectUtils.tryCast(element.containingFile, JSFile::class.java)
			val testFileType = psiFile?.testFileType
			if (psiFile != null && testFileType != null)
			{
				val builder = templateRunSettings.builder()
				builder.setTestFilePath(virtualFile.path)
				return TestElementInfo(builder.build(), (psiFile as PsiElement?)!!)
			}

			return null
		}
	}

	private class TestElementInfo(
	  private val myRunSettings: WdioRunSettings,
	  private val myEnclosingTestElement: PsiElement
	)
	{
		val runSettings: WdioRunSettings
			get()
			{
				return myRunSettings
			}
		val enclosingTestElement: PsiElement
			get()
			{
				return myEnclosingTestElement
			}

	}
}
