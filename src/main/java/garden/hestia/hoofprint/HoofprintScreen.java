package garden.hestia.hoofprint;

import com.mojang.blaze3d.systems.RenderSystem;
import folk.sisby.surveyor.PlayerSummary;
import folk.sisby.surveyor.client.SurveyorClient;
import folk.sisby.surveyor.landmark.Landmark;
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
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class HoofprintScreen extends Screen {

    HoofprintMapStorage mapStorage;
    private double centreX = 0;
    private double centreZ = 0;

    public HoofprintScreen() {
        super(Text.of("Hoofprint World Map"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int roundCentreX = (int) Math.round(centreX);
        int roundCentreZ = (int) Math.round(centreZ);
        ChunkPos originChunk = new ChunkPos(roundCentreX / 16, roundCentreZ / 16);
        int chunkRelativeX = roundCentreX - (originChunk.x * 16);
        int chunkRelativeZ = roundCentreZ - (originChunk.z * 16);
        if (this.mapStorage != null) {
            try (FillBatcher batcher = new FillBatcher(context)) {
                int xSize = (int) Math.ceil((float) this.width / 16) + 4;
                int zSize = (int) Math.ceil((float) this.height / 16) + 4;
                for (int chunkX = -xSize / 2; chunkX < xSize / 2; chunkX++) {
                    for (int chunkZ = -zSize; chunkZ < zSize / 2; chunkZ++) {
                        ChunkPos pos = new ChunkPos(originChunk.x + chunkX, originChunk.z + chunkZ);
                        LayerSummary.Raw layer = mapStorage.bakedTerrain.get(pos);
                        LayerSummary.Raw aboveLayer = mapStorage.bakedTerrain.get(new ChunkPos(pos.x, pos.z - 1));
                        RegistryPalette<Biome>.ValueView biomePalette = mapStorage.biomePalettes.get(pos);
                        RegistryPalette<Block>.ValueView blockPalette = mapStorage.blockPalettes.get(pos);

                        if (layer != null && biomePalette != null && blockPalette != null) {
                            int[][] colors = this.getColors(layer, aboveLayer, biomePalette, blockPalette);
                            for (int x = 0; x < colors.length; x++) {
                                for (int z = 0; z < colors[x].length; z++) {
                                    int x1 = this.width / 2 + 16 * chunkX  + x - chunkRelativeX;
                                    int y1 = this.height / 2 + 16 * chunkZ +  z - chunkRelativeZ;

                                    if (x1 <= width && y1 <= height)
                                        batcher.add(x1, y1, x1 + 1, y1 + 1, 0, colors[x][z]);
                                }
                            }
                        }
                    }
                }
            }
            List<Landmark<?>> hoveredLandmarks = new ArrayList<>();
            this.mapStorage.landmarks.forEach((type, map) -> map.forEach((pos, landmark) -> {
                int landmarkCenterX = width / 2 + pos.getX() - roundCentreX;
                int landmarkCenterY = height / 2 + pos.getZ() - roundCentreZ - 4;
                boolean mouseOver = mouseX > landmarkCenterX - 5 && mouseX < landmarkCenterX + 5 && mouseY > landmarkCenterY - 5 && mouseY < landmarkCenterY + 5;
                if (mouseOver) hoveredLandmarks.add(landmark);
            }));

            List<PlayerSummary> hoveredPlayers = new ArrayList<>();
            SurveyorClient.getFriends().forEach((uuid, player) -> {
                int playerCenterX = (int) Math.round(width / 2.0f + player.pos().getX() - roundCentreX);
                int playerCenterY = (int) Math.round(height / 2.0f + player.pos().getZ() - roundCentreZ);
                boolean mouseOver = mouseX > playerCenterX - 4 && mouseX < playerCenterX + 4 && mouseY > playerCenterY - 4 && mouseY < playerCenterY + 4;
                if (player.username() != null && mouseOver && hoveredLandmarks.isEmpty())
                {
                    hoveredPlayers.add(player);
                }
            });

            SurveyorClient.getFriends().forEach((uuid, player) -> {
                if (player.online() || Hoofprint.CONFIG.showOffline)
                {
                    int playerScreenX = (int) Math.round(width / 2.0f + player.pos().getX() - roundCentreX);
                    int playerScreenY = (int) Math.round(height / 2.0f + player.pos().getZ() - roundCentreZ);
                    boolean mouseOver = hoveredPlayers.contains(player);
                    context.getMatrices().push();
                    context.getMatrices().translate(playerScreenX, playerScreenY, 0);
                    context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180 + player.yaw()));
                    context.getMatrices().translate(-2.5, -3.5, 0);
                    boolean friend = !SurveyorClient.getClientUuid().equals(uuid);
                    float tint = !player.online() ? 0.3f : mouseOver ? 0.8f : 1f;
                    RenderSystem.setShaderColor(tint * (friend ? 0.0f : 1.0f), tint, tint * (friend ? 0.3f : 1.0f), 1.0F);
                    context.drawTexture(Identifier.tryParse("textures/map/decorations/player.png"), 0, 0, 5, 7, 2, 0, 5, 7, 8, 8);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    context.getMatrices().pop();
                }
            });

            this.mapStorage.landmarks.forEach((type, map) -> map.forEach((pos, landmark) -> {
                int landmarkScreenX = width / 2 + pos.getX() - roundCentreX;
                int landmarkScreenY = height / 2 + pos.getZ() - roundCentreZ;
                float[] landmarkColors = landmark.color() == null ? null : ColorUtil.getColorFromArgb(landmark.color().getMapColor().color);
                boolean mouseOver = hoveredLandmarks.contains(landmark);
                float tint = mouseOver ? 0.7F : 1.0F;
                if (landmarkColors != null) RenderSystem.setShaderColor(landmarkColors[0] * tint, landmarkColors[1] * tint, landmarkColors[2] * tint, 1.0F);
                context.drawTexture(Identifier.tryParse("textures/map/decorations/white_banner.png"), landmarkScreenX - 4, landmarkScreenY, 8, 8, 0, 0, 8, 8, 8, 8);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }));

            if (!hoveredLandmarks.isEmpty()) context.drawTooltip(this.textRenderer, hoveredLandmarks.get(0).name(), mouseX, mouseY);
            else if (hoveredLandmarks.isEmpty() && !hoveredPlayers.isEmpty()) context.drawTooltip(this.textRenderer, Text.of(hoveredPlayers.get(0).username()), mouseX, mouseY);
        }
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
