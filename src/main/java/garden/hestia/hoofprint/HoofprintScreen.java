package garden.hestia.hoofprint;

import com.mojang.blaze3d.systems.RenderSystem;
import folk.sisby.surveyor.PlayerSummary;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.client.SurveyorClient;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.WorldLandmarks;
import folk.sisby.surveyor.landmark.component.LandmarkComponentTypes;
import folk.sisby.surveyor.terrain.ChunkSummary;
import folk.sisby.surveyor.terrain.LayerSummary;
import folk.sisby.surveyor.terrain.WorldTerrainSummary;
import garden.hestia.hoofprint.util.ColorUtil;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.border.WorldBorder;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HoofprintScreen extends Screen {
	HoofprintMapStorage mapStorage;
	private double centreX = 0;
	private double centreZ = 0;
	private Landmark hoveredLandmark = null;
	private int hoveredWorldX = 0;
	private int hoveredWorldZ = 0;
	private double guiScale = 1;
	private boolean inspectMode = false;
	private boolean caveMode = false;

	public HoofprintScreen() {
		super(Text.of("Hoofprint World Map"));
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.getMatrices().push();
		float scaleFactor = getScaleFactor();
		context.getMatrices().scale(scaleFactor, scaleFactor, 1.0f);
		int scaledMouseX = (int) screenXtoRenderX(mouseX);
		int scaledMouseY = (int) screenYtoRenderY(mouseY);

		WorldBorder worldBorder = client.world.getWorldBorder();
		double size = worldBorder.getSize();
		double borderX1 = worldXToRenderX(worldBorder.getCenterX() - size / 2.0);
		double borderX2 = worldXToRenderX(worldBorder.getCenterX() + size / 2.0);
		double borderY1 = worldZToRenderY(worldBorder.getCenterZ() - size / 2.0);
		double borderY2 = worldZToRenderY(worldBorder.getCenterZ() + size / 2.0);

		for (Map.Entry<ChunkPos, Identifier> entry : (caveMode ? mapStorage.caveRegionTextures : mapStorage.regionTextures).entrySet()) {
			int drawWidth = 512;
			int drawHeight = 512;
			ChunkPos regionPos = entry.getKey();
			Identifier texture = entry.getValue();
			int minBlockX = regionPos.x * 32 * 16;
			int minBlockZ = regionPos.z * 32 * 16;
			int x = (int) worldXToRenderX(minBlockX);
			int y = (int) worldZToRenderY(minBlockZ);
			int drawX = x;
			int drawY = y;
			if (x > getWidth() || x < -512 || y > getWidth() || y < -512) continue;
			if (!Hoofprint.CONFIG.renderOutsideBorder) {
				drawX = (int) Math.max(x, borderX1);
				drawY = (int) Math.max(y, borderY1);
				double drawX2 = Math.min(x + 512, borderX2);
				double drawY2 = Math.min(y + 512, borderY2);
				drawWidth = (int) (drawX2 - drawX);
				drawHeight = (int) (drawY2 - drawY);
				if (drawHeight == 0 || drawWidth == 0) continue;
			}
			context.drawTexture(texture, drawX, drawY, drawWidth, drawHeight, drawX - x, drawY - y, drawWidth, drawHeight, 512, 512);
		}
		if (Hoofprint.CONFIG.renderBorder) {
			int color = worldBorder.getStage().getColor() | 0xff000000;

			int clampedx1 = (int) Math.max(borderX1, -1);
			int clampedx2 = (int) Math.min(borderX2, Math.ceil(getWidth() + 1));
			int clampedy1 = (int) Math.max(borderY1, -1);
			int clampedy2 = (int) Math.min(borderY2, Math.ceil(getHeight() + 1));

			context.drawBorder(clampedx1, clampedy1, clampedx2 - clampedx1, clampedy2 - clampedy1, color);
		}

		hoveredLandmark = null;
		double bestDistance = Double.MAX_VALUE;
		for (Map<Identifier, Landmark> map : this.mapStorage.landmarks.values()) {
			for (Map.Entry<Identifier, Landmark> entry : map.entrySet()) {
				Identifier id = entry.getKey();
				Landmark landmark = entry.getValue();
				if (!landmark.contains(LandmarkComponentTypes.POS)) continue;
				BlockPos pos = landmark.get(LandmarkComponentTypes.POS);
				int landmarkCenterX = (int) worldXToRenderX(pos.getX());
				int landmarkCenterY = (int) worldZToRenderY(pos.getZ());
				double mouseDistance = (scaledMouseX - landmarkCenterX) * (scaledMouseX - landmarkCenterX) + (scaledMouseY - landmarkCenterY) * (scaledMouseY - landmarkCenterY);
				if (!hasShiftDown() && mouseDistance < 25 && mouseDistance < bestDistance) {
					hoveredLandmark = landmark;
					bestDistance = mouseDistance;
				}
			}
		}

		RegistryKey<World> dim = client.world != null ? client.world.getRegistryKey() : null;

		PlayerSummary hoveredPlayer = null;
		for (PlayerSummary player : SurveyorClient.getFriends().values()) {
			if (!player.dimension().equals(dim) || (!player.online() && !Hoofprint.CONFIG.showOffline)) continue;
			int playerCenterX = (int) worldXToRenderX(player.pos().getX());
			int playerCenterY = (int) worldZToRenderY(player.pos().getZ());
			double mouseDistance = (scaledMouseX - playerCenterX) * (scaledMouseX - playerCenterX) + (scaledMouseY - playerCenterY) * (scaledMouseY - playerCenterY);
			if (mouseDistance < 16 && mouseDistance < bestDistance) {
				hoveredLandmark = null;
				hoveredPlayer = player;
				bestDistance = mouseDistance;
			}
		}

		for (Map.Entry<UUID, PlayerSummary> e : SurveyorClient.getFriends().entrySet()) {
			UUID uuid = e.getKey();
			PlayerSummary player = e.getValue();
			if (!player.dimension().equals(dim) || (!player.online() && !Hoofprint.CONFIG.showOffline)) continue;
			int playerScreenX = (int) worldXToRenderX(player.pos().getX());
			int playerScreenY = (int) worldZToRenderY(player.pos().getZ());
			boolean mouseOver = player == hoveredPlayer;
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

		for (Map<Identifier, Landmark> map : this.mapStorage.landmarks.values()) {
			for (Map.Entry<Identifier, Landmark> entry : map.entrySet()) {
				Identifier id = entry.getKey();
				Landmark landmark = entry.getValue();
				if (!landmark.contains(LandmarkComponentTypes.POS)) continue;
				BlockPos pos = landmark.get(LandmarkComponentTypes.POS);
				int landmarkScreenX = (int) worldXToRenderX(pos.getX());
				int landmarkScreenY = (int) worldZToRenderY(pos.getZ());
				float[] landmarkColors = (landmark.contains(LandmarkComponentTypes.COLOR) && !landmark.contains(LandmarkComponentTypes.STACK)) ? ColorUtil.getColorFromArgb(landmark.get(LandmarkComponentTypes.COLOR)) : new float[]{1.0f, 1.0f, 1.0f};
				boolean mouseOver = landmark == hoveredLandmark;
				float tint = mouseOver ? 0.7F : 1.0F;
				RenderSystem.setShaderColor(landmarkColors[0] * tint, landmarkColors[1] * tint, landmarkColors[2] * tint, 1.0F);
				if (landmark.contains(LandmarkComponentTypes.STACK) && !landmark.get(LandmarkComponentTypes.STACK).isEmpty()) {
					ItemStack stack = landmark.get(LandmarkComponentTypes.STACK);
					context.drawItem(stack, landmarkScreenX - 8, landmarkScreenY - 8);
				} else {
					context.drawTexture(Identifier.tryParse("textures/map/decorations/white_banner.png"), landmarkScreenX - 4, landmarkScreenY - 8, 8, 8, 0, 0, 8, 8, 8, 8);
				}
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
				if (hasShiftDown() && landmark.contains(LandmarkComponentTypes.NAME)) {
					// Draw Text Below Marker
					int textX = landmarkScreenX - this.textRenderer.getWidth(landmark.get(LandmarkComponentTypes.NAME)) / 2;
					context.drawText(this.textRenderer, landmark.get(LandmarkComponentTypes.NAME), textX, landmarkScreenY, 0xFFFFFF, true);
				}
			}
		}

		if (hoveredPlayer != null && hoveredPlayer.username() != null) {
			context.drawTooltip(this.textRenderer, Text.of(hoveredPlayer.username()), scaledMouseX, scaledMouseY);
		} else if (hoveredLandmark != null) {
			List<Text> tooltipLines = new ArrayList<>();
			if (hoveredLandmark.contains(LandmarkComponentTypes.NAME)) tooltipLines.add(hoveredLandmark.get(LandmarkComponentTypes.NAME));
			if (hoveredLandmark.contains(LandmarkComponentTypes.LORE)) tooltipLines.addAll(hoveredLandmark.get(LandmarkComponentTypes.LORE).stream().map(t -> t.copy().formatted(Formatting.GRAY)).toList());
			if (!tooltipLines.isEmpty()) {
				context.drawTooltip(this.textRenderer, tooltipLines, scaledMouseX, scaledMouseY);
			}
		} else if (inspectMode) {
			List<Text> tooltipLines = new ArrayList<>();

			if (!ifTerrainUnderCursor(((block, biome, biomeId, y, lightLevel, waterDepth, waterLight) -> {
				tooltipLines.add(Text.of("x: %d, y: %d, z: %d".formatted(hoveredWorldX, y, hoveredWorldZ)));
				tooltipLines.add(block.getName());
				if (biomeId != null) tooltipLines.add(Text.translatable("biome.%s.%s".formatted(biomeId.getNamespace(), biomeId.getPath())));
				if (waterDepth > 0) tooltipLines.add(Text.of("Water: %d blocks".formatted(waterDepth)));
				if (lightLevel > 0) tooltipLines.add(Text.of("Block Light: %d".formatted(lightLevel)));
			}))) {
				tooltipLines.add(Text.of("x: %d, z: %d".formatted(hoveredWorldX, hoveredWorldZ)));
			}
			context.drawTooltip(this.textRenderer, tooltipLines, scaledMouseX, scaledMouseY);
		}

		context.getMatrices().pop();
	}

	interface FloorConsumer {
		void accept(Block block, Biome biome, Identifier biomeId, int y, int lightLevel, int waterDepth, int waterLight);
	}

	private boolean ifTerrainUnderCursor(FloorConsumer consumer) {
		ChunkPos cp = new ColumnPos(hoveredWorldX, hoveredWorldZ).toChunkPos();
		if (client == null || client.world == null) return false;
		WorldTerrainSummary terrain = WorldSummary.of(client.world).terrain();
		if (terrain == null) return false;
		Integer maxY = Hoofprint.CONFIG.dimensionMaxYValues.getOrDefault(client.world.getRegistryKey().getValue().toString(), null);
		ChunkSummary summary = terrain.get(cp);
		if (summary == null) return false;
		LayerSummary.Raw layer = summary.toSingleLayer(null, maxY, client.world.getHeight());
		if (layer == null) return false;
		int blockIndex = (hoveredWorldX - cp.getStartX()) * 16 + (hoveredWorldZ - cp.getStartZ());
		Block block = terrain.getBlockPalette(cp).get(layer.blocks()[blockIndex]);
		Biome biome = terrain.getBiomePalette(cp).get(layer.biomes()[blockIndex]);
		Identifier biomeId = terrain.getBiomePalette(cp).registry().getId(biome);
		if (block == null || biome == null) return false;
		consumer.accept(block, biome, biomeId, client.world.getHeight() - layer.depths()[blockIndex], layer.lightLevels()[blockIndex], layer.waterDepths()[blockIndex], layer.waterLights()[blockIndex]);
		return true;
	}

	@Override
	protected void init() {
		RegistryKey<World> dim = client.world.getRegistryKey();
		this.mapStorage = HoofprintMapStorage.get(dim);
		this.centreX = client.player.getBlockX();
		this.centreZ = client.player.getBlockZ();
		this.guiScale = client.getWindow().getScaleFactor();
		super.init();
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		switch (keyCode) {
			case GLFW.GLFW_KEY_I -> inspectMode = true;
			case GLFW.GLFW_KEY_PAGE_DOWN, GLFW.GLFW_KEY_PAGE_UP -> caveMode = !caveMode;
			case GLFW.GLFW_KEY_UP -> centreZ--;
			case GLFW.GLFW_KEY_DOWN -> centreZ++;
			case GLFW.GLFW_KEY_LEFT -> centreX--;
			case GLFW.GLFW_KEY_RIGHT -> centreX++;
			case GLFW.GLFW_KEY_DELETE -> {
				if (client == null || client.world == null || client.player == null) return true;
				if (hoveredLandmark != null && (SurveyorClient.getClientUuid().equals(hoveredLandmark.owner()) || (hoveredLandmark.owner().equals(WorldLandmarks.GLOBAL) && client.player.hasPermissionLevel(2)))) {
					WorldTerrainSummary terrain = WorldSummary.of(client.world).terrain();
					WorldLandmarks landmarks = WorldSummary.of(client.world).landmarks();
					if (terrain == null || landmarks == null) return true;
					landmarks.remove(client.world, hoveredLandmark.owner(), hoveredLandmark.id());
				}
			}
			case GLFW.GLFW_KEY_INSERT -> ifTerrainUnderCursor(((block, biome, biomeId, y, lightLevel, waterDepth, waterLight) -> {
				WorldLandmarks landmarks = WorldSummary.of(client.world).landmarks();
				if (landmarks == null) return;
				landmarks.put(client.world, Landmark.createIncremental(landmarks, SurveyorClient.getClientUuid(), Identifier.of("hoofprint", "block"), builder -> {
						builder.add(LandmarkComponentTypes.POS, new BlockPos(hoveredWorldX, y, hoveredWorldZ))
							.add(LandmarkComponentTypes.NAME, block.getName())
							.add(LandmarkComponentTypes.COLOR, ColorUtil.argbToABGR(block.getDefaultMapColor().getRenderColor(MapColor.Brightness.NORMAL)));
						if (!block.asItem().getDefaultStack().isEmpty()) builder.add(LandmarkComponentTypes.STACK, block.asItem().getDefaultStack());
						return builder;
					}
				));
			}));
			default -> {
				return super.keyPressed(keyCode, scanCode, modifiers);
			}
		}
		return true;
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		switch (keyCode) {
			case GLFW.GLFW_KEY_I -> inspectMode = false;
			default -> {
				return super.keyReleased(keyCode, scanCode, modifiers);
			}
		}
		return true;
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		hoveredWorldX = (int) Math.floor(screenXToWorldX(mouseX) + (screenXToWorldX(mouseX) < 0 ? 0.5 : -0.5)); // Dunno
		hoveredWorldZ = (int) Math.floor(screenYToWorldZ(mouseY));
		super.mouseMoved(mouseX, mouseY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double hz, double amount) {
		guiScale = (int) MathHelper.clamp(guiScale + amount, 1, 10);
		return true;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		centreX -= deltaX / getScaleFactor();
		centreZ -= deltaY / getScaleFactor();
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	double worldXToRenderX(double worldX) {
		return (getWidth() / 2.0) + worldX - Math.round(centreX);
	}

	double worldZToRenderY(double worldZ) {
		return (getHeight() / 2.0) + worldZ - Math.round(centreZ);
	}

	double screenXToWorldX(double screenX) {
		return (screenX / getScaleFactor()) + Math.round(centreX) - getWidth() / 2.0;
	}

	double screenYToWorldZ(double screenY) {
		return (screenY / getScaleFactor()) + Math.round(centreZ) - getHeight() / 2.0;
	}

	double screenXtoRenderX(double screenX) {
		return screenX / getScaleFactor();
	}

	double screenYtoRenderY(double screenY) {
		return screenY / getScaleFactor();
	}

	float getScaleFactor() {
		return (float) (guiScale / client.getWindow().getScaleFactor());
	}

	float getWidth() {
		return width / getScaleFactor();
	}

	float getHeight() {
		return height / getScaleFactor();
	}
}
