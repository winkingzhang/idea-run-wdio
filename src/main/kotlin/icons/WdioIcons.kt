package icons

import com.intellij.ui.IconManager
import org.jetbrains.annotations.NotNull
import javax.swing.Icon

object WdioIcons
{
	@JvmStatic
	@NotNull
	private fun getIcon(@NotNull path: String) =
	  IconManager.getInstance().getIcon(path, WdioIcons::class.java)

	@JvmStatic
	@NotNull
	val wdio: Icon = getIcon("icons/wdio.svg")
}
