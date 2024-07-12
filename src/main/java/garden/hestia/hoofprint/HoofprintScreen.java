package garden.hestia.hoofprint;

import com.mojang.blaze3d.systems.RenderSystem;
import folk.sisby.surveyor.client.SurveyorClient;
import folk.sisby.surveyor.terrain.LayerSummary;
import folk.sisby.surveyor.util.RegistryPalette;
import garden.hestia.hoofprint.util.ColorUtil;
import garden.hestia.hoofprint.util.FillBatcher;
import garden.hestia.hoofprint.util.LightMapUtil;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;
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
                        LayerSummary.Raw aboveLayer = mapStorage.bakedTerrain.get(new ChunkPos(pos.x, pos.z - 1));
                        RegistryPalette<Biome>.ValueView biomePalette = mapStorage.biomePalettes.get(pos);
                        RegistryPalette<Block>.ValueView blockPalette = mapStorage.blockPalettes.get(pos);

                        if (layer != null && biomePalette != null && blockPalette != null) {
                            int[][] colors = this.getColors(layer, aboveLayer, biomePalette, blockPalette);
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
            SurveyorClient.getFriends().forEach((uuid, player) -> {
                if (player.online() || Hoofprint.CONFIG.showOffline)
                {
                    Vec3d origin = player.pos();
                    int playerX = (int) Math.floor(origin.x);
                    int playerZ = (int) Math.floor(origin.z);
                    int playerScreenX = width / 2 + playerX - roundCentreX;
                    int playerScreenY = height / 2 + playerZ - roundCentreZ;
                    context.getMatrices().push();
                    context.getMatrices().translate(playerScreenX, playerScreenY, 0);
                    context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180 + player.yaw()));
                    context.getMatrices().translate(-5/2.0f, -7/2.0f, 0);
                    boolean friend = !SurveyorClient.getClientUuid().equals(uuid);
                    context.drawTexture(new Identifier("textures/map/map_icons.png"), 0, 0, 5, 7, !player.online() ? 19 : friend ? 10 : 2, 0, 5, 7, 128, 128);
                    context.getMatrices().pop();
                    boolean mouseOver = mouseX > playerScreenX - 5 && mouseX < playerScreenX + 5 && mouseY > playerScreenY - 5 && mouseY < playerScreenY + 5;
                    if (player.username() != null && mouseOver)
                    {
                        context.drawTooltip(this.textRenderer, Text.of(player.username()), mouseX, mouseY);
                    }
                }
            });
            this.mapStorage.landmarks.forEach((type, map) -> map.forEach((pos, landmark) -> {
                Vec3d origin = pos.toCenterPos();
                int landmarkX = (int) Math.floor(origin.x);
                int landmarkZ = (int) Math.floor(origin.z);
                int landmarkScreenX = width / 2 + landmarkX - roundCentreX;
                int landmarkScreenY = height / 2 + landmarkZ - roundCentreZ;
                float[] landmarkColors = landmark.color() == null ? null : landmark.color().getColorComponents();
                boolean mouseOver = mouseX > landmarkScreenX - 5 && mouseX < landmarkScreenX + 5 && mouseY > landmarkScreenY - 5 && mouseY < landmarkScreenY + 5;
                float tint = mouseOver ? 0.7F : 1.0F;
                context.getMatrices().push();
                context.getMatrices().translate(landmarkScreenX, landmarkScreenY, 0);
                context.getMatrices().translate(-5/2.0f, -7/2.0f, 0);
                if (landmarkColors != null) RenderSystem.setShaderColor(landmarkColors[0] * tint, landmarkColors[1] * tint, landmarkColors[2] * tint, 1.0F);
                context.drawTexture(new Identifier("textures/map/map_icons.png"), 0, 0, 6, 8, 81, 0, 6, 8, 128, 128);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                context.getMatrices().pop();
                if (landmark.name() != null && mouseOver)
                {
                    context.drawTooltip(this.textRenderer, landmark.name(), mouseX, mouseY);
                }
            }));

        }
        super.render(context, mouseX, mouseY, delta);
    }

    int[][] getColors(LayerSummary.Raw layer, @Nullable LayerSummary.Raw aboveLayer, RegistryPalette<Biome>.ValueView biomePalette, RegistryPalette<Block>.ValueView blockPalette) {
        int[][] colors = new int[16][16];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int i = x * 16 + z;
                if (!layer.exists().get(i)) continue;
                int color;
                int waterColor;
                if (!Hoofprint.CONFIG.transparentWater && layer.waterDepths()[i] > 0)
                {
                    color = ColorUtil.getWaterColor(biomePalette.get(layer.biomes()[i]));
                }
                else {
                    color = ColorUtil.getBlockColour(blockPalette.get(layer.blocks()[i]), biomePalette.get(layer.biomes()[i]));
                }
                if (Hoofprint.CONFIG.topography) {
                    ColorUtil.Brightness brightness = ColorUtil.Brightness.NORMAL;
                    if (!Hoofprint.CONFIG.transparentWater && layer.waterDepths()[i] > 0) {
                        brightness = ColorUtil.getBrightnessFromDepth(layer.waterDepths()[i], x, z);
                    }
                    else if (z > 0) {
                        if (layer.depths()[i - 1] < layer.depths()[i]) brightness = ColorUtil.Brightness.LOW;
                        if (layer.depths()[i - 1] > layer.depths()[i]) brightness = ColorUtil.Brightness.HIGH;
                    }
                    else if (aboveLayer != null) {
                        if (aboveLayer.depths()[x * 16 + 15] < layer.depths()[i]) brightness = ColorUtil.Brightness.LOW;
                        if (aboveLayer.depths()[x * 16 + 15] > layer.depths()[i]) brightness = ColorUtil.Brightness.HIGH;
                    }
                    color = ColorUtil.applyBrightnessRGB(brightness, color);
                }
                if (Hoofprint.CONFIG.lighting && (Hoofprint.CONFIG.transparentWater || layer.waterDepths()[i] == 0)) {
                    int blockLight = layer.lightLevels()[i];
                    int skyLight = Math.max(ColorUtil.SKY_LIGHT - layer.waterDepths()[i], 0);
                    color = ColorUtil.tint(color, LightMapUtil.DAY[skyLight][blockLight]);
                }
                if (Hoofprint.CONFIG.transparentWater && layer.waterDepths()[i] > 0)
                {
                    waterColor = ColorUtil.getWaterColor(biomePalette.get(layer.biomes()[i]));
                    if (Hoofprint.CONFIG.lighting) {
                        int blockLight = layer.waterLights()[i];
                        int skyLight = ColorUtil.SKY_LIGHT;
                        waterColor = ColorUtil.tint(waterColor, LightMapUtil.DAY[skyLight][blockLight]);
                    }
                    color = ColorUtil.blend(color, waterColor, 0.6F);
                }
                colors[x][z] = color | 0xff000000;
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
