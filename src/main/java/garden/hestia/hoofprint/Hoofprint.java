package garden.hestia.hoofprint;

import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.client.SurveyorClientEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hoofprint implements ClientModInitializer {
    public static final String ID = "hoofprint";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    public static final KeyBinding OPEN_MAP = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.hoofprint.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_M, "category.hoofprint"));

    @Override
    public void onInitializeClient() {
        WorldSummary.enableTerrain();
        WorldSummary.enableLandmarks();
        WorldSummary.enableStructures();
        ClientTickEvents.END_CLIENT_TICK.register((c) -> {
            while (OPEN_MAP.wasPressed())
            {
                c.setScreen(new HoofprintScreen());
            }
        });

        SurveyorClientEvents.Register.worldLoad(new Identifier(ID, "world_load"), (world, summary, player, terrain, structures, landmarks) -> {
            HoofprintMapStorage.get(world.getRegistryKey()).worldLoad(world, summary, player, terrain, structures, landmarks);
        });

        SurveyorClientEvents.Register.terrainUpdated(new Identifier(ID, "terrain_updated"), (world, summary, chunks) -> {
            HoofprintMapStorage.get(world.getRegistryKey()).terrainUpdated(world, summary, chunks);
        });
        SurveyorClientEvents.Register.landmarksAdded(new Identifier(ID, "landmarks_added"), (world, worldLandmarks, landmarks) -> {
            HoofprintMapStorage.get(world.getRegistryKey()).landmarksAdded(world, worldLandmarks, landmarks);
        });

        LOGGER.info("[Hoofprint] They went thatta-way!");
    }
}
