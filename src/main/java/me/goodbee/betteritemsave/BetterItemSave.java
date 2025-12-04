package me.goodbee.betteritemsave;

import com.mojang.brigadier.arguments.StringArgumentType;
import me.goodbee.betteritemsave.ui.Config;
import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BetterItemSave implements ClientModInitializer {
	public static final Path BASE_PATH = FabricLoader.getInstance().getGameDir().resolve("better-item-save");
	public static final String MOD_ID = "better-item-save";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Config CONFIG = Config.createAndLoad();

	public static ItemSaver itemSaver;
	@Override
	public void onInitializeClient() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		try {
			Files.createDirectories(BASE_PATH);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		itemSaver = new ItemSaver(BASE_PATH);

		ClientCommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommandManager.literal("save-item-in-hand")
					.then(ClientCommandManager.argument("location", StringArgumentType.greedyString())
					.executes(context -> {
						String location = StringArgumentType.getString(context, "location");

						if(!location.endsWith(".nbt")) {
							location += ".nbt";
						}

						try {
							itemSaver.saveItem(location, context.getSource().getPlayer().getMainHandItem());
							context.getSource().sendFeedback(Component.literal("Successfully saved to " + location));
						} catch (RuntimeException e) {
							context.getSource().sendError(Component.literal("An error occurred while saving the item. Check the logs for more information."));
							LOGGER.error("An error occured while trying to save an item", e);
						}

						return 1;
					})));
		}));
	}
}