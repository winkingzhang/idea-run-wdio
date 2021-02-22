package icons

import com.intellij.ui.IconManager
import org.jetbrains.annotations.NotNull
import javax.swing.Icon

object WdioIcons
{
	@JvmStatic
	@NotNull
	private fun load(@NotNull path: String, cacheKey: Long, flags: Int): Icon
	{
		@Suppress("UnstableApiUsage")
		return IconManager.getInstance().loadRasterizedIcon(
		  path,
		  WdioIcons::class.java.classLoader,
		  cacheKey,
		  flags
		)
	}

	@JvmStatic
	@NotNull
	val wdio: Icon = load("icons/wdio.svg", Long.MAX_VALUE / 2, 0)
}
