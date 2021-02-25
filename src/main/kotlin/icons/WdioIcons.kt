package icons

import com.intellij.ui.IconManager
import org.jetbrains.annotations.NotNull
import javax.swing.Icon

object WdioIcons
{
	@JvmStatic
	@NotNull
	private fun load(@NotNull path: String): Icon
	{
		@Suppress("UnstableApiUsage")
		return IconManager.getInstance().getIcon(
		  path,
		  WdioIcons::class.java
		)
	}

	@JvmStatic
	@NotNull
	val wdio: Icon = load("icons/wdio.svg")
}
