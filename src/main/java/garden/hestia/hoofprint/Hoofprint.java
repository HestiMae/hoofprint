package garden.hestia.hoofprint;

import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.client.SurveyorClient;
import folk.sisby.surveyor.client.SurveyorClientEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hoofprint implements ClientModInitializer {
	public static final String ID = "hoofprint";
	public static final Logger LOGGER = LoggerFactory.getLogger(ID);
	public static final HoofprintConfig CONFIG = HoofprintConfig.createToml(FabricLoader.getInstance().getConfigDir(), "", ID, HoofprintConfig.class);
	public static final KeyBinding OPEN_MAP = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.hoofprint.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_M, "category.hoofprint"));

	@Override
	public void onInitializeClient() {
		WorldSummary.enableTerrain();
		WorldSummary.enableLandmarks();
		WorldSummary.enableStructures();
		ClientTickEvents.END_CLIENT_TICK.register((c) -> {
			while (OPEN_MAP.wasPressed()) {
				c.setScreen(new HoofprintScreen());
			}
		});
		ClientTickEvents.END_WORLD_TICK.register((c) -> {
			ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
			if (handler != null) SurveyorClient.getSummaries(handler).forEach((dim, summary) -> HoofprintMapStorage.get(dim).tick(summary, c.getTime()));
		});
		ClientPlayConnectionEvents.DISCONNECT.register(HoofprintMapStorage::disconnect);

		SurveyorClientEvents.Register.terrainUpdated(new Identifier(ID, "terrain_updated"), (summary, chunks) -> HoofprintMapStorage.get(summary.dimension()).terrainUpdated(summary, chunks));
		SurveyorClientEvents.Register.landmarksAdded(new Identifier(ID, "landmarks_added"), (summary, landmarks) -> HoofprintMapStorage.get(summary.dimension()).landmarksAdded(summary, landmarks));
		SurveyorClientEvents.Register.landmarksRemoved(new Identifier(ID, "landmarks_removed"), (summary, landmarks) -> HoofprintMapStorage.get(summary.dimension()).landmarksRemoved(summary, landmarks));

		LOGGER.info("[Hoofprint] They went thatta-way!");
	}
}
