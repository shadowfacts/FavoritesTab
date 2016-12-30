package net.shadowfacts.favoritestab

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.gui.inventory.GuiContainerCreative
import net.minecraft.client.settings.KeyBinding
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.init.Items
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompressedStreamTools
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.util.NonNullList
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.client.settings.KeyConflictContext
import net.minecraftforge.client.settings.KeyModifier
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.Constants
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.shadowfacts.forgelin.extensions.containsStack
import net.shadowfacts.forgelin.extensions.forEach
import net.shadowfacts.forgelin.extensions.set
import net.shadowfacts.mirror.Mirror
import net.shadowfacts.mirror.MirrorMethod
import org.lwjgl.input.Keyboard
import java.io.File

/**
 * @author shadowfacts
 */
@Mod(modid = MOD_ID, name = NAME, version = "@VERSION@", dependencies = "required-after:shadowmc;", modLanguageAdapter = "net.shadowfacts.forgelin.KotlinAdapter", clientSideOnly = true)
object FavoritesTab {

	val file by lazy {
		File(Minecraft.getMinecraft().mcDataDir, "favorites-tab.nbt")
	}
	lateinit var keyBinding: KeyBinding
		private set
	val favorites = mutableListOf<ItemStack>()
	val creativeInv by lazy {
		val field = Mirror.of(GuiContainerCreative::class.java).declaredField("basicInventory", "field_147060_v").get()
		field.setAccessible(true)
		field.get(null) as IInventory
	}
	val setCurrentCreativeTab: MirrorMethod by lazy {
		val method = Mirror.of(GuiContainerCreative::class.java).declaredMethod(arrayOf("setCurrentCreativeTab", "func_147050_b"), CreativeTabs::class.java).get()
		method.setAccessible(true)
		method
	}
	var getJEIStack = {
		ItemStack.EMPTY
	}

	@Mod.EventHandler
	fun preInit(event: FMLPreInitializationEvent) {
		load()
		TabFavorites
		MinecraftForge.EVENT_BUS.register(EventHandler)
		keyBinding = KeyBinding("keybinding.favorite", KeyConflictContext.GUI, KeyModifier.ALT, Keyboard.KEY_F, "key.categories.misc")
		ClientRegistry.registerKeyBinding(keyBinding)
	}

	fun load() {
		if (file.exists()) {
			val tag = CompressedStreamTools.read(file)
			val list = tag.getTagList("favorites", Constants.NBT.TAG_COMPOUND)
			favorites.clear()
			list.forEach {
				if (it is NBTTagCompound) {
					favorites.add(ItemStack(it))
				}
			}
		} else {
			save()
		}
	}

	fun save() {
		if (!file.exists()) {
			file.createNewFile()
		}
		val tag = NBTTagCompound()
		val list = NBTTagList()
		favorites.forEach {
			val stackTag = NBTTagCompound()
			it.writeToNBT(stackTag)
			list.appendTag(stackTag)
		}
		tag["favorites"] = list
		CompressedStreamTools.write(tag, file)
	}

	object TabFavorites: CreativeTabs("favorites") {

		override fun getTabIconItem(): ItemStack {
			return ItemStack(Items.NETHER_STAR)
		}

		override fun displayAllRelevantItems(list: NonNullList<ItemStack>) {
			list.addAll(favorites)
		}

		override fun hasSearchBar(): Boolean {
			return false
		}

	}

	object EventHandler {

		private var lastOp: Long = 0

		@SubscribeEvent
		fun onKeyInput(event: GuiScreenEvent.KeyboardInputEvent.Post) {
			if (!keyBinding.isActiveAndMatches(Keyboard.getEventKey())) return
			if (System.currentTimeMillis() - lastOp < 150) return

			var mode = OpMode.ADD
			var stack: ItemStack = ItemStack.EMPTY

			val gui = event.gui
			var onComplete: () -> Unit = {}

			if (gui is GuiContainer) {
				val favoritesTabOpen = gui is GuiContainerCreative && gui.selectedTabIndex == TabFavorites.tabIndex
				mode = if (favoritesTabOpen && gui.slotUnderMouse?.inventory == creativeInv) OpMode.REMOVE else OpMode.ADD
				stack = gui.slotUnderMouse?.stack?.copy()?.apply {
					count = 1
				} ?: ItemStack.EMPTY
				if (favoritesTabOpen) {
					onComplete = {
						setCurrentCreativeTab.invoke(gui, TabFavorites)
					}
				} else {
					onComplete = {}
				}
			}
			if (stack.isEmpty) {
				stack = getJEIStack()
			}

			if (!stack.isEmpty) {
				when (mode) {
					OpMode.ADD -> {
						if (favorites.containsStack(stack)) return
						favorites.add(stack)
					}
					OpMode.REMOVE -> {
						favorites.removeAll {
							ItemStack.areItemStacksEqual(stack, it)
						}
					}
				}

				onComplete()
				save()
				event.isCanceled = true
				lastOp = System.currentTimeMillis()
			}
		}

	}

	enum class OpMode {
		ADD,
		REMOVE
	}

}