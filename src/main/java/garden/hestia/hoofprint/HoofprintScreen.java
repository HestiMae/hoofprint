package garden.hestia.hoofprint;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public class HoofprintScreen extends Screen {

    HoofprintMapStorage mapStorage;
    public HoofprintScreen() {
        super(Text.of("Hoofprint World Map"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.mapStorage != null)
        {
            // screen goes here. Load from mapStorage, colour pixels, etc.
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    protected void init() {
        RegistryKey<World> dim = MinecraftClient.getInstance().world.getRegistryKey();
        this.mapStorage = HoofprintMapStorage.INSTANCES.computeIfAbsent(dim, (key) -> new HoofprintMapStorage());
        super.init();
    }
}
