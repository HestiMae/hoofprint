package garden.hestia.hoofprint;

import folk.sisby.surveyor.terrain.LayerSummary;
import folk.sisby.surveyor.util.RegistryPalette;
import garden.hestia.hoofprint.util.FillBatcher;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.lwjgl.glfw.GLFW;

public class HoofprintScreen extends Screen {

    HoofprintMapStorage mapStorage;
    private double centreX = 0;
    private double centreZ = 0;

    public HoofprintScreen() {
        super(Text.of("Hoofprint World Map"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int roundCentreX = (int) Math.round(centreX);
        int roundCentreZ = (int) Math.round(centreZ);
        ChunkPos originChunk = new ChunkPos(roundCentreX / 16, roundCentreZ / 16);
        int chunkRelativeX = roundCentreX - (originChunk.x * 16);
        int chunkRelativeZ = roundCentreZ - (originChunk.z * 16);
        if (this.mapStorage != null) {
            try (FillBatcher batcher = new FillBatcher(context)) {
                int xSize = (int) Math.ceil((float) this.width / 16) + 4;
                int zSize = (int) Math.ceil((float) this.height / 16) + 4;
                for (int chunkX = -2; chunkX < xSize; chunkX++) {
                    for (int chunkZ = -2; chunkZ < zSize; chunkZ++) {
                        ChunkPos pos = new ChunkPos(chunkX + originChunk.x - Math.round(xSize / 2.0f) + 2, chunkZ + originChunk.z - Math.round(zSize / 2.0f) + 2);
                        LayerSummary.Raw layer = mapStorage.bakedTerrain.get(pos);
                        RegistryPalette<Biome>.ValueView biomePalette = mapStorage.biomePalettes.get(pos);
                        RegistryPalette<Block>.ValueView blockPalette = mapStorage.blockPalettes.get(pos);

                        if (layer != null) {
                            int[][] colors = this.getColors(layer, biomePalette, blockPalette);
                            for (int x = 0; x < colors.length; x++) {
                                for (int z = 0; z < colors[x].length; z++) {
                                    int x1 = chunkX * 16 + x - chunkRelativeX;
                                    int y1 = chunkZ * 16 + z - chunkRelativeZ;

                                    if (x1 <= width && y1 <= height)
                                        batcher.add(x1, y1, x1 + 1, y1 + 1, 0, colors[x][z]);
                                }
                            }
                        }
                    }
                }
            }
            Vec3d origin = MinecraftClient.getInstance().player.getPos();
            int playerX = (int) Math.floor(origin.x);
            int playerZ = (int) Math.floor(origin.z);
            int playerScreenX = width / 2 + playerX - roundCentreX;
            int playerScreenZ = height / 2 + playerZ - roundCentreZ;
            context.fill(playerScreenX, playerScreenZ, playerScreenX + 1, playerScreenZ + 1, 0xff39ff14);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    int[][] getColors(LayerSummary.Raw layer, RegistryPalette<Biome>.ValueView biomePalette, RegistryPalette<Block>.ValueView blockPalette) {
        int[][] colors = new int[16][16];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                colors[x][z] = blockPalette.get(layer.blocks()[x * 16 + z]).getDefaultMapColor().color | 0xff000000;
            }
        }
        return colors;
    }

    @Override
    protected void init() {
        RegistryKey<World> dim = MinecraftClient.getInstance().world.getRegistryKey();
        this.mapStorage = HoofprintMapStorage.get(dim);
        this.centreX = MinecraftClient.getInstance().player.getBlockX();
        this.centreZ = MinecraftClient.getInstance().player.getBlockZ();
        super.init();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_UP -> centreZ--;
            case GLFW.GLFW_KEY_DOWN -> centreZ++;
            case GLFW.GLFW_KEY_LEFT -> centreX--;
            case GLFW.GLFW_KEY_RIGHT -> centreX++;
            default -> {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        centreX -= deltaX;
        centreZ -= deltaY;
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
}