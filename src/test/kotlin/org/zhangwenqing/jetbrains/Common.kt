package org.zhangwenqing.jetbrains

import com.intellij.openapi.util.text.StringUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier
import com.intellij.openapi.project.Project

const val NEWLINE = "\n"

@Suppress("UnnecessaryAbstractClass")
abstract class Common<T> : BasePlatformTestCase()
{
	@Throws(
	  NoSuchMethodException::class,
	  IllegalAccessException::class,
	  InvocationTargetException::class,
	  InstantiationException::class
	)
	protected fun privateConstructor(clz: Class<T>)
	{
		val constructor = clz.getDeclaredConstructor()
		TestCase.assertTrue(Modifier.isPrivate(constructor.modifiers))
		constructor.isAccessible = true
		constructor.newInstance()
	}

	protected fun createIgnoreContent(vararg entries: String?) =
	  StringUtil.join(entries, NEWLINE)

	protected val fixtureRootFile
		get() = myFixture.file.containingDirectory.virtualFile

	protected val fixtureProject: Project
		get() = myFixture.project
}
