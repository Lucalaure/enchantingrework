package lucalaure1007.enchantingrework;

import com.mojang.serialization.Codec;
import lucalaure1007.enchantingrework.catalyst.Catalyst;
import lucalaure1007.enchantingrework.config.EnchantingConfig;
import lucalaure1007.enchantingrework.network.ConfigSyncPayload;
import lucalaure1007.enchantingrework.table.ReworkedEnchantmentMenu;
import lucalaure1007.enchantingrework.template.EnchantingTemplates;
import lucalaure1007.enchantingrework.template.InscribeTemplateRecipe;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnchantingRework implements ModInitializer {
	public static final String MOD_ID = "enchantingrework";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static EnchantingConfig CONFIG = new EnchantingConfig();

	/** How many times an item has been through the enchanting table. */
	public static final DataComponentType<Integer> TABLE_PASSES = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		id("table_passes"),
		DataComponentType.<Integer>builder().persistent(Codec.intRange(0, 255)).networkSynchronized(ByteBufCodecs.VAR_INT).build()
	);

	public static final MenuType<ReworkedEnchantmentMenu> ENCHANTMENT_MENU = Registry.register(
		BuiltInRegistries.MENU,
		id("enchanting_table"),
		new MenuType<>(ReworkedEnchantmentMenu::new, FeatureFlags.VANILLA_SET)
	);

	@Override
	public void onInitialize() {
		CONFIG = EnchantingConfig.load();
		EnchantingTemplates.register();
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id("inscribe_template"), InscribeTemplateRecipe.SERIALIZER);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output ->
			output.insertAfter(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, EnchantingTemplates.BY_THEME.values().toArray(new ItemLike[0])));
		ServerLifecycleEvents.SERVER_STARTING.register(InscribeTemplateRecipe::setServer);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> InscribeTemplateRecipe.setServer(null));
		DynamicRegistries.registerSynced(Catalyst.REGISTRY_KEY, Catalyst.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ConfigSyncPayload.TYPE, ConfigSyncPayload.CODEC);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sender.sendPacket(new ConfigSyncPayload(CONFIG.toJson())));
		ServerLifecycleEvents.SERVER_STARTED.register(server -> LOGGER.info(
			"Loaded {} enchanting catalysts",
			server.registryAccess().lookup(Catalyst.REGISTRY_KEY).map(registry -> registry.size()).orElse(0)
		));
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
