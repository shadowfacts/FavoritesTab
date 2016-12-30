package net.shadowfacts.favoritestab

import mezz.jei.api.BlankModPlugin
import mezz.jei.api.IJeiRuntime
import mezz.jei.api.JEIPlugin
import net.minecraft.item.ItemStack

/**
 * @author shadowfacts
 */
@JEIPlugin
class CompatJEI: BlankModPlugin() {

	override fun onRuntimeAvailable(jeiRuntime: IJeiRuntime) {
		FavoritesTab.getJEIStack = {
			jeiRuntime.itemListOverlay.stackUnderMouse?.apply {
				count = 1
			} ?: ItemStack.EMPTY
		}
	}

}