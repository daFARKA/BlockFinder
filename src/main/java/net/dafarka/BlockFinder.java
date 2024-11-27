package net.dafarka;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.registry.Registries;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class BlockFinder implements ClientModInitializer {
	public static final String MOD_ID = "block_finder";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		ModConfig.init();

		if (!ModConfig.INSTANCE.disableMod) {
			ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(ClientCommandManager.literal("find")
					.then(ClientCommandManager.argument("range", IntegerArgumentType.integer())
						.then(ClientCommandManager.argument("block_name", StringArgumentType.string())
							.executes(context -> {
								detectBlocks(IntegerArgumentType.getInteger(context, "range"), StringArgumentType.getString(context, "block_name"));
								return 1;
							})))
			));
		}
	}

	private void detectBlocks(int range, String blockName) {
		Identifier targetBlock = Identifier.ofVanilla(blockName);

		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		ClientWorld world = client.world;

		if (player == null || world == null) return;

		BlockPos playerPos = player.getBlockPos();

		Box searchArea = new Box(playerPos).expand(range);

		AtomicBoolean found = new AtomicBoolean(false);
		BlockPos.iterate(
			(int) searchArea.minX, (int) searchArea.minY, (int) searchArea.minZ,
			(int) searchArea.maxX, (int) searchArea.maxY, (int) searchArea.maxZ
		).forEach(pos -> {
			if (world.getBlockState(pos).isOf(Registries.BLOCK.get(targetBlock))) {
				String posText = "X: " + pos.getX() + ", Y: " + pos.getY() + ", Z: " + pos.getZ();
				LOGGER.info("Target found at " + posText);
				player.sendMessage(Text.of("Target at " + posText), true);
				// Create a clickable text message
				Text message = Text.literal("Target at " + posText)
					.setStyle(Style.EMPTY
						.withColor(Formatting.GOLD) // Set the text color
						.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @p " + pos.getX() + " " + (pos.getY() + 1) + " " + pos.getZ()))
						.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to teleport on top of the block.")))
					);

				// Send the message to the player
				player.sendMessage(message, false);
				found.set(true);
			}
		});

		if (!found.get()) {
			player.sendMessage(Text.of("Target not found."), true);
		}
	}
}