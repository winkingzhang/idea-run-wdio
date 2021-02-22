package org.zhangwenqing.jetbrains.execution

import com.google.common.collect.ImmutableList
import com.intellij.execution.configuration.EnvironmentVariablesTextFieldWithBrowseButton
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterField
import com.intellij.javascript.nodejs.util.NodePackageField
import com.intellij.lang.javascript.JavaScriptBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.options.ex.SingleConfigurableEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.components.fields.CommaSeparatedIntegersField
import com.intellij.ui.components.fields.ExpandableTextField
import com.intellij.util.ui.ComponentWithEmptyText
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.SwingHelper
import com.intellij.webcore.ui.PathShortener
import org.jetbrains.annotations.NotNull
import org.zhangwenqing.jetbrains.WdioBundle
import org.zhangwenqing.jetbrains.WdioUtil
import javax.swing.JComponent
import javax.swing.JPanel

const val PANEL_TOP = 8

class WdioRunConfigurationEditor constructor(
  @NotNull private val project: Project
) : SettingsEditor<WdioRunConfiguration>()
{
	private val myNodeInterpreterField = NodeJsInterpreterField(project, false)
	private val myNodeOptions = RawCommandLineEditor()
	private val myWorkingDirTextFieldWithBrowseButton = createWorkingDirTextField(project)
	private val myEnvironmentVariablesTextFieldWithBrowseButton = EnvironmentVariablesTextFieldWithBrowseButton()
	private val myWdioPackageField = NodePackageField(myNodeInterpreterField, WdioUtil.PACKAGE_DESCRIPTOR, null)
	private val myWdioConfigFiledWithBrowseButton = createWdioConfigFileTextField(project)
	private val myTestFileTextFieldWithBrowseButton = createTestFileTextField(project)
	private val myTestLineNumbersEditor = CommaSeparatedIntegersField()
	private val myComponent = FormBuilder()
	  .setAlignLabelOnRight(false)
	  .addLabeledComponent(
		NodeJsInterpreterField.getLabelTextForComponent(),
		(myNodeInterpreterField as JComponent)
	  )
	  .addLabeledComponent(
		JavaScriptBundle.message("rc.nodeOptions.label"),
		(myNodeOptions as JComponent)
	  )
	  .addLabeledComponent(
		JavaScriptBundle.message("rc.workingDirectory.label"),
		(myWorkingDirTextFieldWithBrowseButton as JComponent)
	  )
	  .addLabeledComponent(
		JavaScriptBundle.message("rc.environmentVariables.label"),
		(myEnvironmentVariablesTextFieldWithBrowseButton as JComponent)
	  )
	  .addLabeledComponent(
		WdioBundle.message("wdio.run.package.label"),
		(myWdioPackageField as JComponent)
	  )
	  .addLabeledComponent(
		WdioBundle.message("wdio.run.config.label"),
		(myWdioConfigFiledWithBrowseButton as JComponent)
	  )
	  .addLabeledComponent(
		JavaScriptBundle.message("rc.testRunScope.testFile.label"),
		(myTestFileTextFieldWithBrowseButton as JComponent)
	  )
	  .addLabeledComponent(
		WdioBundle.message("wdio.run.test.line.numbers.label"),
		(myTestLineNumbersEditor as JComponent)
	  )
	  .addSeparator(PANEL_TOP)
	  .addComponentFillVertically(JPanel(), 0)
	  .panel

	override fun resetEditorFrom(configuration: WdioRunConfiguration)
	{
		val runSettings = configuration.getRunSettings()
		myNodeInterpreterField.interpreterRef = runSettings.interpreterRef
		myNodeOptions.text = runSettings.nodeOptions
		myWorkingDirTextFieldWithBrowseButton.text = FileUtil.toSystemDependentName(runSettings.workingDir)
		myEnvironmentVariablesTextFieldWithBrowseButton.data = runSettings.envData
		myWdioPackageField.selected = configuration.getWdioPackage()
		myWdioConfigFiledWithBrowseButton.text = FileUtil.toSystemDependentName(runSettings.wdioConfigFilePath)
		myTestFileTextFieldWithBrowseButton.text = FileUtil.toSystemDependentName(runSettings.testFilePath)
		myTestLineNumbersEditor.value = ImmutableList.copyOf(runSettings.testLineNumbers)
		updatePreferredWidth()
	}

	override fun applyEditorTo(configuration: WdioRunConfiguration)
	{
		val builder = WdioRunSettings.Builder()
		val interpreterRef = myNodeInterpreterField.interpreterRef
		builder.setInterpreterRef(interpreterRef)
		builder.setNodeOptions(myNodeOptions.text)
		builder.setWorkingDir(PathShortener.getAbsolutePath(myWorkingDirTextFieldWithBrowseButton.textField))
		builder.setEnvData(myEnvironmentVariablesTextFieldWithBrowseButton.data)
		builder.setWdioPackage(myWdioPackageField.selected)
		builder.setWdioConfigFilePath(PathShortener.getAbsolutePath(myWdioConfigFiledWithBrowseButton.textField))
		builder.setTestFilePath(PathShortener.getAbsolutePath(myTestFileTextFieldWithBrowseButton.textField))
		builder.setTestLineNumbers(myTestLineNumbersEditor.value)
		configuration.setRunSettings(builder.build())
	}

	override fun createEditor(): JComponent = myComponent

	private fun updatePreferredWidth()
	{
		val dialogWrapper = DialogWrapper.findInstance(myComponent)
		if (dialogWrapper is SingleConfigurableEditor)
		{
			myNodeInterpreterField.setPreferredWidthToFitText()
			myWdioPackageField.setPreferredWidthToFitText()
			SwingHelper.setPreferredWidthToFitText(myWorkingDirTextFieldWithBrowseButton)
			SwingHelper.setPreferredWidthToFitText(myTestFileTextFieldWithBrowseButton)
			ApplicationManager.getApplication().invokeLater({
				SwingHelper.adjustDialogSizeToFitPreferredSize(dialogWrapper)
			}, ModalityState.any())
		}
	}

	companion object
	{
		private fun createWorkingDirTextField(project: Project): TextFieldWithBrowseButton =
		  createTextFieldWithBrowseButton(project, JavaScriptBundle.message("rc.workingDirectory.browseDialogTitle"))

		private fun createWdioConfigFileTextField(project: Project): TextFieldWithBrowseButton
		{
			val textFieldWithBrowseButton = createTextFieldWithBrowseButton(
			  project,
			  WdioBundle.message("wdio.run.config.workingDirectory.browseDialogTitle")
			)
			val field = textFieldWithBrowseButton.textField
			(field as? ExpandableTextField)?.putClientProperty("monospaced", false)
			if (field is ComponentWithEmptyText)
			{
				(field as ComponentWithEmptyText).emptyText.text = WdioBundle.message("wdio.run.config.emptyText")
			}
			return textFieldWithBrowseButton
		}

		private fun createTestFileTextField(project: Project): TextFieldWithBrowseButton =
		  createTextFieldWithBrowseButton(project, JavaScriptBundle.message("rc.testRunScope.testFile.browseTitle"))

		private fun createTextFieldWithBrowseButton(project: Project, dialogTitle: String): TextFieldWithBrowseButton
		{
			val textFieldWithBrowseButton = TextFieldWithBrowseButton()
			SwingHelper.installFileCompletionAndBrowseDialog(
			  project,
			  textFieldWithBrowseButton,
			  dialogTitle,
			  FileChooserDescriptorFactory.createSingleFolderDescriptor()
			)
			PathShortener.enablePathShortening(textFieldWithBrowseButton.textField, null)
			return textFieldWithBrowseButton
		}
	}
}
