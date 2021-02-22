package org.zhangwenqing.jetbrains.execution

import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.javascript.testFramework.JsTestFileByTestNameIndex
import com.intellij.javascript.testFramework.exports.ExportsTestFileStructureBuilder
import com.intellij.javascript.testFramework.interfaces.mochaTdd.MochaTddFileStructureBuilder
import com.intellij.javascript.testFramework.jasmine.JasmineFileStructureBuilder
import com.intellij.javascript.testFramework.qunit.QUnitFileStructureBuilder
import com.intellij.javascript.testFramework.util.EscapeUtils
import com.intellij.javascript.testFramework.util.JsTestFqn
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.lang.javascript.psi.JSTestFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.ObjectUtils
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.annotations.Nullable


private const val TEST_PROTOCOL_ID = "test"
private const val SPLIT_CHAR = '.'

class WdioTestLocationProvider : SMTestLocator
{
	override fun getLocation(
	  protocol: String,
	  path: String,
	  project: Project,
	  scope: GlobalSearchScope
	): List<Location<PsiElement>>
	{
		throw IllegalStateException("Should not be called")
	}

	override fun getLocation(
	  protocol: String,
	  path: String,
	  @Nullable metaInfo: String?,
	  project: Project,
	  scope: GlobalSearchScope
	): List<Location<PsiElement>>
	{
		if (TEST_PROTOCOL_ID == protocol)
		{
			val location: Location<PsiElement>? = getTestLocation(project, path, metaInfo)
			return ContainerUtil.createMaybeSingletonList(location)
		}
		return emptyList()
	}

	private fun getTestLocation(
	  project: Project,
	  locationData: String,
	  testFilePath: String?
	): Location<PsiElement>?
	{
		var psiElement: PsiElement?
		val path = EscapeUtils.split(locationData, SPLIT_CHAR)
		if (path.isEmpty())
		{
			return null
		}

		psiElement = findJasmineElement(project, path, testFilePath)

		if (psiElement == null)
		{
			psiElement = findQUnitElement(project, path, testFilePath)
		}

		if (psiElement == null)
		{
			psiElement = findExportsElement(project, path, testFilePath)
		}

		if (psiElement == null)
		{
			psiElement = findTddElement(project, path, testFilePath)
		}

		return if (psiElement != null)
		{
			PsiLocation.fromPsiElement(psiElement)
		}
		else null
	}

	companion object
	{
		private fun findJasmineElement(project: Project, location: List<String>, testFilePath: String?): PsiElement?
		{
			val executedFile = findFile(testFilePath!!)
			val testFqn = JsTestFqn(JSTestFileType.JASMINE, location)
			val scope = GlobalSearchScope.projectScope(project)
			val jsTestVirtualFiles = JsTestFileByTestNameIndex.findFiles(testFqn, scope, executedFile)
			for (file in jsTestVirtualFiles)
			{
				val psiFile = PsiManager.getInstance(project).findFile(file)
				if (psiFile is JSFile)
				{
					val builder = JasmineFileStructureBuilder.getInstance()
					val element = builder.fetchCachedTestFileStructure(psiFile).findPsiElement(testFqn.names, null)
					if (element != null && element.isValid)
					{
						return element
					}
				}
			}
			return null
		}

		private fun findQUnitElement(project: Project, location: List<String>, testFilePath: String?): PsiElement?
		{
			val moduleName: String
			val testName: String?
			val executedFile = findFile(testFilePath!!)
			when
			{
				location.size > 1 ->
				{
					moduleName = location[0]
					testName = location[1]
				}
				else ->
				{
					moduleName = "Default Module"
					testName = location[0]
				}
			}
			val key = JsTestFileByTestNameIndex.createQUnitKey(moduleName, testName)
			val scope = GlobalSearchScope.projectScope(project)
			val jsTestVirtualFiles = JsTestFileByTestNameIndex.findFilesByKey(key, scope, executedFile)
			for (file in jsTestVirtualFiles)
			{
				val psiFile = PsiManager.getInstance(project).findFile(file)
				if (psiFile is JSFile)
				{
					val builder = QUnitFileStructureBuilder.getInstance()
					val element = builder.fetchCachedTestFileStructure(psiFile)
					  .findPsiElement(moduleName, testName)
					if (element != null && element.isValid)
					{
						return element
					}
				}
			}
			return null
		}

		private fun findExportsElement(project: Project, location: List<String>, testFilePath: String?): PsiElement?
		{
			val file = findJSFile(project, testFilePath) ?: return null
			return ExportsTestFileStructureBuilder.getInstance()
			  .fetchCachedTestFileStructure(file)
			  .findPsiElement(location)
		}

		private fun findTddElement(project: Project, location: List<String>, testFilePath: String?): PsiElement?
		{
			val executedFile = findFile(testFilePath!!)
			val scope = GlobalSearchScope.projectScope(project)
			val testFqn = JsTestFqn(JSTestFileType.TDD, location)
			val jsTestVirtualFiles = JsTestFileByTestNameIndex.findFiles(testFqn, scope, executedFile)
			for (file in jsTestVirtualFiles)
			{
				val psiFile = PsiManager.getInstance(project).findFile(file)
				if (psiFile is JSFile)
				{
					val suiteNames = location.subList(0, location.size - 1)
					val testName = ContainerUtil.getLastItem(location) as String
					val element = MochaTddFileStructureBuilder.getInstance()
					  .fetchCachedTestFileStructure(psiFile)
					  .findPsiElement(suiteNames, testName)
					if (element != null && element.isValid)
					{
						return element
					}
				}
			}
			return null
		}

		private fun findJSFile(project: Project, testFilePath: String?): JSFile?
		{
			val file: VirtualFile? = findFile(testFilePath!!)
			if (file == null || !file.isValid)
			{
				return null
			}
			val psiFile = PsiManager.getInstance(project).findFile(file)
			return ObjectUtils.tryCast(psiFile, JSFile::class.java)
		}

		private fun findFile(filePath: String): VirtualFile?
		{
			return if (StringUtil.isEmptyOrSpaces(filePath)) null
			else LocalFileSystem.getInstance().findFileByPath(FileUtil.toSystemIndependentName(filePath))
		}
	}
}
