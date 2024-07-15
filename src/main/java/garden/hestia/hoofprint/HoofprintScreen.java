package garden.hestia.hoofprint;

import com.mojang.blaze3d.systems.RenderSystem;
import folk.sisby.surveyor.PlayerSummary;
import folk.sisby.surveyor.client.SurveyorClient;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.terrain.LayerSummary;
import folk.sisby.surveyor.terrain.RegionSummary;
import folk.sisby.surveyor.util.RegistryPalette;
import garden.hestia.hoofprint.util.ColorUtil;
import garden.hestia.hoofprint.util.LightMapUtil;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HoofprintScreen extends Screen {
    public static final String TEXTURE_PREFIX = "hoofprint/map";

    HoofprintMapStorage mapStorage;
    private double centreX = 0;
    private double centreZ = 0;
    private final Map<ChunkPos, Identifier> regionTextures = new HashMap<>();

    public HoofprintScreen() {
        super(Text.of("Hoofprint World Map"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int roundCentreX = (int) Math.round(centreX);
        int roundCentreZ = (int) Math.round(centreZ);
        for (Map.Entry<ChunkPos, Identifier> entry : regionTextures.entrySet()) {
            ChunkPos regionPos = entry.getKey();
            Identifier texture = entry.getValue();
            int minBlockX = regionPos.x * 32 * 16;
            int minBlockZ = regionPos.z * 32 * 16;
            int x = minBlockX - roundCentreX + width / 2;
            int y = minBlockZ - roundCentreZ + height / 2;
            if (x > width || x < -512 || y > width || y < -512) continue;
            context.drawTexture(texture, x, y, 512, 512, 0, 0, 512, 512, 512, 512);
        }

        Landmark<?> hoveredLandmark = null;
        double bestDistance = Double.MAX_VALUE;
        for (Map<BlockPos, Landmark<?>> map : this.mapStorage.landmarks.values()) {
            for (Map.Entry<BlockPos, Landmark<?>> entry : map.entrySet()) {
                BlockPos pos = entry.getKey();
                Landmark<?> landmark = entry.getValue();
                int landmarkCenterX = width / 2 + pos.getX() - roundCentreX;
                int landmarkCenterY = height / 2 + pos.getZ() - roundCentreZ - 4;
                double mouseDistance = (mouseX - landmarkCenterX) * (mouseX - landmarkCenterX) + (mouseY - landmarkCenterY) * (mouseY - landmarkCenterY);
                if (mouseDistance < 25 && mouseDistance < bestDistance) {
                    hoveredLandmark = landmark;
                    bestDistance = mouseDistance;
                }
            }
        }

        RegistryKey<World> dim = MinecraftClient.getInstance().world != null ? MinecraftClient.getInstance().world.getRegistryKey() : null;

        PlayerSummary hoveredPlayer = null;
        for (PlayerSummary player : SurveyorClient.getFriends().values()) {
            if (player.dimension().equals(dim) || (!player.online() && !Hoofprint.CONFIG.showOffline)) continue;
            int playerCenterX = (int) Math.round(width / 2.0f + player.pos().getX() - roundCentreX);
            int playerCenterY = (int) Math.round(height / 2.0f + player.pos().getZ() - roundCentreZ);
            double mouseDistance = (mouseX - playerCenterX) * (mouseX - playerCenterX) + (mouseY - playerCenterY) * (mouseY - playerCenterY);
            if (mouseDistance < 16 && mouseDistance < bestDistance) {
                hoveredLandmark = null;
                hoveredPlayer = player;
                bestDistance = mouseDistance;
            }
        }

        for (Map.Entry<UUID, PlayerSummary> e : SurveyorClient.getFriends().entrySet()) {
            UUID uuid = e.getKey();
            PlayerSummary player = e.getValue();
            if (player.dimension().equals(dim) || (!player.online() && !Hoofprint.CONFIG.showOffline)) continue;
            int playerScreenX = (int) Math.round(width / 2.0f + player.pos().getX() - roundCentreX);
            int playerScreenY = (int) Math.round(height / 2.0f + player.pos().getZ() - roundCentreZ);
            boolean mouseOver = player == hoveredPlayer;
            context.getMatrices().push();
            context.getMatrices().translate(playerScreenX, playerScreenY, 0);
            context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180 + player.yaw()));
            context.getMatrices().translate(-2.5, -3.5, 0);
            boolean friend = !SurveyorClient.getClientUuid().equals(uuid);
            float tint = !player.online() ? 0.3f : mouseOver ? 0.8f : 1f;
            RenderSystem.setShaderColor(tint * (friend ? 0.0f : 1.0f), tint, tint * (friend ? 0.3f : 1.0f), 1.0F);
            context.drawTexture(new Identifier("textures/map/map_icons.png"), 0, 0, 5, 7, 2, 0, 5, 7, 128, 128);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            context.getMatrices().pop();
        }

        for (Map<BlockPos, Landmark<?>> map : this.mapStorage.landmarks.values()) {
            for (Map.Entry<BlockPos, Landmark<?>> entry : map.entrySet()) {
                BlockPos pos = entry.getKey();
                Landmark<?> landmark = entry.getValue();
                int landmarkScreenX = width / 2 + pos.getX() - roundCentreX;
                int landmarkScreenY = height / 2 + pos.getZ() - roundCentreZ;
                float[] landmarkColors = landmark.color() == null ? null : landmark.color().getColorComponents();
                boolean mouseOver = landmark == hoveredLandmark;
                float tint = mouseOver ? 0.7F : 1.0F;
                if (landmarkColors != null) RenderSystem.setShaderColor(landmarkColors[0] * tint, landmarkColors[1] * tint, landmarkColors[2] * tint, 1.0F);
                context.drawTexture(new Identifier("textures/map/map_icons.png"), landmarkScreenX - 4, landmarkScreenY - 8, 8, 8, 80, 0, 8, 8, 128, 128);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }

        if (hoveredPlayer != null && hoveredPlayer.username() != null) context.drawTooltip(this.textRenderer, Text.of(hoveredPlayer.username()), mouseX, mouseY);
        if (hoveredLandmark != null) context.drawTooltip(this.textRenderer, hoveredLandmark.name(), mouseX, mouseY);
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
                if (!Hoofprint.CONFIG.transparentWater && layer.waterDepths()[i] > 0) {
                    color = ColorUtil.getWaterColor(biomePalette.get(layer.biomes()[i]));
                } else {
                    color = ColorUtil.getBlockColour(blockPalette.get(layer.blocks()[i]), biomePalette.get(layer.biomes()[i]));
                }
                if (Hoofprint.CONFIG.topography) {
                    ColorUtil.Brightness brightness = ColorUtil.Brightness.NORMAL;
                    if (!Hoofprint.CONFIG.transparentWater && layer.waterDepths()[i] > 0) {
                        brightness = ColorUtil.getBrightnessFromDepth(layer.waterDepths()[i], x, z);
                    } else if (z > 0) {
                        if (layer.depths()[i - 1] < layer.depths()[i]) brightness = ColorUtil.Brightness.LOW;
                        if (layer.depths()[i - 1] > layer.depths()[i]) brightness = ColorUtil.Brightness.HIGH;
                    } else if (aboveLayer != null) {
                        if (aboveLayer.depths()[x * 16 + 15] < layer.depths()[i]) brightness = ColorUtil.Brightness.LOW;
                        if (aboveLayer.depths()[x * 16 + 15] > layer.depths()[i])
                            brightness = ColorUtil.Brightness.HIGH;
                    }
                    color = ColorUtil.applyBrightnessRGB(brightness, color);
                }
                if (Hoofprint.CONFIG.lighting && (Hoofprint.CONFIG.transparentWater || layer.waterDepths()[i] == 0)) {
                    int blockLight = layer.lightLevels()[i];
                    int skyLight = Math.max(ColorUtil.SKY_LIGHT - layer.waterDepths()[i], 0);
                    color = ColorUtil.tint(color, LightMapUtil.DAY[skyLight][blockLight]);
                }
                if (Hoofprint.CONFIG.transparentWater && layer.waterDepths()[i] > 0) {
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

    private void bake() {
        mapStorage.bakedTerrain.forEach((rPos, flatChunks) -> {
            NativeImageBackedTexture terrainTexture = new NativeImageBackedTexture(512, 512, true);
            regionTextures.put(rPos, MinecraftClient.getInstance().getTextureManager().registerDynamicTexture(TEXTURE_PREFIX, terrainTexture));
            ChunkPos regionChunkOrigin = new ChunkPos(RegionSummary.regionToChunk(rPos.x), RegionSummary.regionToChunk(rPos.z));
            for (int chunkX = 0; chunkX < 32; chunkX++) {
                for (int chunkZ = 0; chunkZ < 32; chunkZ++) {
                    ChunkPos pos = new ChunkPos(regionChunkOrigin.x + chunkX, regionChunkOrigin.z + chunkZ);
                    LayerSummary.Raw layer = mapStorage.bakedTerrain.get(rPos)[chunkX][chunkZ];
                    LayerSummary.Raw aboveLayer = chunkZ > 0 ? mapStorage.bakedTerrain.get(rPos)[chunkX][chunkZ - 1] : mapStorage.bakedTerrain.containsKey(new ChunkPos(rPos.x, rPos.z - 1)) ? mapStorage.bakedTerrain.get(new ChunkPos(rPos.x, rPos.z - 1))[chunkX][31] : null;
                    RegistryPalette<Biome>.ValueView biomePalette = mapStorage.biomePalettes.get(pos);
                    RegistryPalette<Block>.ValueView blockPalette = mapStorage.blockPalettes.get(pos);
                    if (layer != null && biomePalette != null && blockPalette != null) {
                        int[][] colors = this.getColors(layer, aboveLayer, biomePalette, blockPalette);
                        for (int x = 0; x < colors.length; x++) {
                            for (int z = 0; z < colors[x].length; z++) {
                                int imageX = 16 * chunkX + x;
                                int imageY = 16 * chunkZ + z;
                                terrainTexture.getImage().setColor(imageX, imageY, ColorUtil.argbToABGR(colors[x][z]));
                            }
                        }
                    }
                }
            }
            terrainTexture.upload();
        });
    }

    @Override
    protected void init() {
        RegistryKey<World> dim = MinecraftClient.getInstance().world.getRegistryKey();
        this.mapStorage = HoofprintMapStorage.get(dim);
        this.centreX = MinecraftClient.getInstance().player.getBlockX();
        this.centreZ = MinecraftClient.getInstance().player.getBlockZ();
        super.init();
        bake();
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
