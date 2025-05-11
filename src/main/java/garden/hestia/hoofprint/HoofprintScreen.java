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
	private boolean hideDecorations = false;

	public HoofprintScreen() {
		super(Text.of("Hoofprint World Map"));
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		context.getMatrices().push();
		float scaleFactor = getScaleFactor();
		context.getMatrices().scale(scaleFactor, scaleFactor, 1.0f);

		WorldBorder worldBorder = client.world.getWorldBorder();
		double size = worldBorder.getSize();
		int borderX1 = (int) Math.floor(worldBorder.getCenterX() - size / 2.0);
		int borderX2 = (int) Math.ceil(worldBorder.getCenterX() + size / 2.0);
		int borderZ1 = (int) Math.floor(worldBorder.getCenterZ() - size / 2.0);
		int borderZ2 = (int) Math.ceil(worldBorder.getCenterZ() + size / 2.0);
		int renderX1 = Math.max((int) Math.floor(screenXToWorldX(0.0)), Hoofprint.CONFIG.renderOutsideBorder ? Integer.MIN_VALUE : borderX1);
		int renderX2 = Math.min((int) Math.ceil(screenXToWorldX(width)), Hoofprint.CONFIG.renderOutsideBorder ? Integer.MAX_VALUE : borderX2);
		int renderZ1 = Math.max((int) Math.floor(screenYToWorldZ(0.0)), Hoofprint.CONFIG.renderOutsideBorder ? Integer.MIN_VALUE : borderZ1);
		int renderZ2 = Math.min((int) Math.ceil(screenYToWorldZ(height)), Hoofprint.CONFIG.renderOutsideBorder ? Integer.MAX_VALUE : borderZ2);

		for (Map.Entry<ChunkPos, Identifier> entry : (caveMode ? mapStorage.caveRegionTextures : mapStorage.regionTextures).entrySet()) {
			ChunkPos regionPos = entry.getKey();
			Identifier texture = entry.getValue();
			int regionX1 = regionPos.x * 32 * 16;
			int regionZ1 = regionPos.z * 32 * 16;
			int u = Math.max(0, renderX1 - regionX1);
			int v = Math.max(0, renderZ1 - regionZ1);
			int drawWidth = Math.min(512, renderX2 - regionX1) - u;
			int drawHeight = Math.min(512, renderZ2 - regionZ1) - v;
			if (drawHeight <= 0 || drawWidth <= 0) continue;
			context.getMatrices().push();
			context.getMatrices().translate(worldXToRenderX(regionX1 + u), worldZToRenderY(regionZ1 + v), 0);
			context.drawTexture(texture, 0, 0, drawWidth, drawHeight, u, v, drawWidth, drawHeight, 512, 512);
			context.getMatrices().pop();
		}
		if (Hoofprint.CONFIG.renderBorder && !hideDecorations) {
			int color = worldBorder.getStage().getColor() | 0xff000000;

			double clampedx1 = Math.max(worldXToRenderX(borderX1), -1);
			double clampedx2 = Math.min(worldXToRenderX(borderX2), Math.ceil(getWidth() + 1));
			double clampedy1 = Math.max(worldZToRenderY(borderZ1), -1);
			double clampedy2 = Math.min(worldZToRenderY(borderZ2), Math.ceil(getHeight() + 1));

			context.getMatrices().push();
			context.getMatrices().translate(clampedx1, clampedy1, 0);
			context.drawBorder(0, 0, (int) (clampedx2 - clampedx1), (int) (clampedy2 - clampedy1), color);
			context.getMatrices().pop();
		}

		context.getMatrices().pop();

		hoveredLandmark = null;
		double bestDistance = Double.MAX_VALUE;
		for (Map<Identifier, Landmark> map : this.mapStorage.landmarks.values()) {
			for (Landmark landmark : map.values()) {
				BlockPos pos = landmark.get(LandmarkComponentTypes.POS);
				if (pos == null || hideDecorations) continue;
				double landmarkCenterX = renderToScreen(worldXToRenderX(pos.getX()));
				double landmarkCenterY = renderToScreen(worldZToRenderY(pos.getZ()));
				double mouseDistance = (mouseX - landmarkCenterX) * (mouseX - landmarkCenterX) + (mouseY - landmarkCenterY) * (mouseY - landmarkCenterY);
				if (!hasShiftDown() && mouseDistance < (6 * 6 * client.getWindow().getScaleFactor()) && mouseDistance < bestDistance) {
					hoveredLandmark = landmark;
					bestDistance = mouseDistance;
				}
			}
		}

		RegistryKey<World> dim = client.world != null ? client.world.getRegistryKey() : null;

		PlayerSummary hoveredPlayer = null;
		for (PlayerSummary player : SurveyorClient.getFriends().values()) {
			if (!player.dimension().equals(dim) || (!player.online() && !Hoofprint.CONFIG.showOffline) || hideDecorations) continue;
			double playerCenterX = renderToScreen(worldXToRenderX(player.pos().getX()));
			double playerCenterY = renderToScreen(worldZToRenderY(player.pos().getZ()));
			double mouseDistance = (mouseX - playerCenterX) * (mouseX - playerCenterX) + (mouseY - playerCenterY) * (mouseY - playerCenterY);
			if (mouseDistance < (4 * 4 * client.getWindow().getScaleFactor()) && mouseDistance < bestDistance) {
				hoveredLandmark = null;
				hoveredPlayer = player;
				bestDistance = mouseDistance;
			}
		}

		for (Map.Entry<UUID, PlayerSummary> e : SurveyorClient.getFriends().entrySet()) {
			UUID uuid = e.getKey();
			PlayerSummary player = e.getValue();
			if (!player.dimension().equals(dim) || (!player.online() && !Hoofprint.CONFIG.showOffline) || hideDecorations) continue;
			double playerScreenX = renderToScreen(worldXToRenderX(player.pos().getX()));
			double playerScreenY = renderToScreen(worldZToRenderY(player.pos().getZ()));
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

		for (Map<Identifier, Landmark> map : this.mapStorage.landmarks.values()) {
			for (Landmark landmark : map.values()) {
				BlockPos pos = landmark.get(LandmarkComponentTypes.POS);
				if (pos == null || hideDecorations) continue;
				double landmarkScreenX = renderToScreen(worldXToRenderX(pos.getX()));
				double landmarkScreenY = renderToScreen(worldZToRenderY(pos.getZ()));
				float[] landmarkColors = (landmark.contains(LandmarkComponentTypes.COLOR) && !landmark.contains(LandmarkComponentTypes.STACK)) ? ColorUtil.getColorFromArgb(landmark.get(LandmarkComponentTypes.COLOR)) : new float[]{1.0f, 1.0f, 1.0f};
				boolean mouseOver = landmark == hoveredLandmark;
				float tint = mouseOver ? 0.7F : 1.0F;
				RenderSystem.setShaderColor(landmarkColors[0] * tint, landmarkColors[1] * tint, landmarkColors[2] * tint, 1.0F);
				context.getMatrices().push();
				context.getMatrices().translate(landmarkScreenX, landmarkScreenY, 0);
				if (landmark.contains(LandmarkComponentTypes.STACK) && !landmark.get(LandmarkComponentTypes.STACK).isEmpty()) {
					ItemStack stack = landmark.get(LandmarkComponentTypes.STACK);
					context.drawItem(stack, -8, -8);
				} else {
					context.drawTexture(new Identifier("textures/map/map_icons.png"), -4, -8, 8, 8, 80, 0, 8, 8, 128, 128);
				}
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
				if (hasShiftDown() && landmark.contains(LandmarkComponentTypes.NAME)) {
					// Draw Text Below Marker
					int textX = -this.textRenderer.getWidth(landmark.get(LandmarkComponentTypes.NAME)) / 2;
					context.drawText(this.textRenderer, landmark.get(LandmarkComponentTypes.NAME), textX, 0, 0xFFFFFF, true);
				}
				context.getMatrices().pop();
			}
		}


		if (hoveredPlayer != null && hoveredPlayer.username() != null) {
			context.drawTooltip(this.textRenderer, Text.of(hoveredPlayer.username()), mouseX, mouseY);
		} else if (hoveredLandmark != null) {
			List<Text> tooltipLines = new ArrayList<>();
			if (hoveredLandmark.contains(LandmarkComponentTypes.NAME)) tooltipLines.add(hoveredLandmark.get(LandmarkComponentTypes.NAME));
			if (hoveredLandmark.contains(LandmarkComponentTypes.LORE)) tooltipLines.addAll(hoveredLandmark.get(LandmarkComponentTypes.LORE).stream().map(t -> t.copy().formatted(Formatting.GRAY)).toList());
			if (!tooltipLines.isEmpty()) {
				context.drawTooltip(this.textRenderer, tooltipLines, mouseX, mouseY);
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
			context.drawTooltip(this.textRenderer, tooltipLines, mouseX, mouseY);
		}

		super.render(context, mouseX, mouseY, delta);
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
		if (Hoofprint.OPEN_MAP.matchesKey(keyCode, scanCode)) {
			close();
			return true;
		}
		switch (keyCode) {
			case GLFW.GLFW_KEY_I -> inspectMode = true;
			case GLFW.GLFW_KEY_H -> hideDecorations = !hideDecorations;
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
				landmarks.put(client.world, Landmark.createIncremental(landmarks, SurveyorClient.getClientUuid(), new Identifier("hoofprint", "block"), builder -> {
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
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
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
		return (getWidth() / 2.0) + worldX - centreX;
	}

	double worldZToRenderY(double worldZ) {
		return (getHeight() / 2.0) + worldZ - centreZ;
	}

	double screenXToWorldX(double screenX) {
		return (screenX / getScaleFactor()) + centreX - getWidth() / 2.0;
	}

	double screenYToWorldZ(double screenY) {
		return (screenY / getScaleFactor()) + centreZ - getHeight() / 2.0;
	}

	double screenToRender(double screenPixels) {
		return screenPixels / getScaleFactor();
	}

	double renderToScreen(double screenPixels) {
		return screenPixels * getScaleFactor();
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
