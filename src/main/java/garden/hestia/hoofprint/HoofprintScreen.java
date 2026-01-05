package garden.hestia.hoofprint;

import com.google.common.primitives.Ints;
import folk.sisby.surveyor.PlayerSummary;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.client.SurveyorClient;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.WorldLandmarks;
import folk.sisby.surveyor.landmark.component.LandmarkComponentMap;
import folk.sisby.surveyor.landmark.component.LandmarkComponentTypes;
import folk.sisby.surveyor.terrain.ChunkSummary;
import folk.sisby.surveyor.terrain.LayerSummary;
import folk.sisby.surveyor.terrain.WorldTerrain;
import folk.sisby.surveyor.util.RegionPos;
import garden.hestia.hoofprint.util.ColorUtil;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.border.WorldBorder;
import org.apache.commons.lang3.text.WordUtils;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class HoofprintScreen extends Screen {
	public static final Identifier BACKGROUND = Identifier.of("hoofprint", "map_background_checkerboard");
	public static final Identifier BACKGROUND_DARK = Identifier.of("hoofprint", "map_background_checkerboard_dark");
	private static final float PLAYER_ROTATION_STEPS = 16;
	private double centreX = 0;
	private double centreZ = 0;
	private Landmark hoveredLandmark = null;
	private Landmark editingLandmark = null;
	private StringBuilder landmarkName = null;
	private StringBuilder landmarkStyle = null;
	private boolean editingStyle = false;
	private boolean styleValid = false;
	private double hoveredScreenX = 0;
	private double hoveredScreenY = 0;
	private int hoveredWorldX = 0;
	private int hoveredWorldZ = 0;
	private int guiScale = 1;
	private boolean inspectMode = false;
	private boolean caveMode = false;
	private boolean hideDecorations = false;
	private PlayerSummary hoveredPlayer;
	private int cursorFrame = 0;
	private RegistryKey<World> dim;
	private float switchFade = 0.0F;

	public HoofprintScreen() {
		super(Text.of("Hoofprint World Map"));
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		switchFade = Math.max(0, switchFade - delta);
		HoofprintMapStorage mapStorage = HoofprintMapStorage.get(dim);
		context.getMatrices().pushMatrix();
		float scaleFactor = getScaleFactor();
		context.getMatrices().scale(scaleFactor, scaleFactor);

		WorldBorder worldBorder = client.world.getWorldBorder();
		double size = worldBorder.getSize();
		int borderX1 = Math.max((int) Math.floor(worldBorder.getCenterX() - size / 2.0), mapStorage.minBlockX);
		int borderX2 = Math.min((int) Math.ceil(worldBorder.getCenterX() + size / 2.0), mapStorage.maxBlockX);
		int borderZ1 = Math.max((int) Math.floor(worldBorder.getCenterZ() - size / 2.0), mapStorage.minBlockZ);
		int borderZ2 = Math.min((int) Math.ceil(worldBorder.getCenterZ() + size / 2.0), mapStorage.maxBlockZ);


		double areaX1 = Math.max(worldXToRenderX(borderX1) - 8, -256 + (worldXToRenderX(borderX1) - 8) % 256);
		double areaX2 = Math.min(worldXToRenderX(borderX2) + 8, screenToRender(width) + (worldXToRenderX(borderX2) + 8) % 256);
		double areaY1 = Math.max(worldZToRenderY(borderZ1) - 8, -256 + (worldZToRenderY(borderZ1) - 8) % 256);
		double areaY2 = Math.min(worldZToRenderY(borderZ2) + 8, screenToRender(height) + (worldZToRenderY(borderZ2) + 8) % 256);
		if (Hoofprint.CONFIG.style.mapBackground && (areaX2 - areaX1) > 0 && (areaY2 - areaY1) > 0) {
			context.getMatrices().pushMatrix();
			context.getMatrices().translate((float) areaX1, (float) areaY1);
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, caveMode ? BACKGROUND_DARK : BACKGROUND, 0, 0, (int) (areaX2 - areaX1), (int) (areaY2 - areaY1));
			context.getMatrices().popMatrix();
		}

		int renderX1 = Math.max((int) Math.floor(screenXToWorldX(0.0)), borderX1);
		int renderX2 = Math.min((int) Math.ceil(screenXToWorldX(width)), borderX2);
		int renderZ1 = Math.max((int) Math.floor(screenYToWorldZ(0.0)), borderZ1);
		int renderZ2 = Math.min((int) Math.ceil(screenYToWorldZ(height)), borderZ2);

		for (Map.Entry<RegionPos, Identifier> entry : (caveMode ? mapStorage.caveRegionTextures : mapStorage.regionTextures).entrySet()) {
			RegionPos regionPos = entry.getKey();
			Identifier texture = entry.getValue();
			int regionX1 = regionPos.x() * 32 * 16;
			int regionZ1 = regionPos.z() * 32 * 16;
			int u = Math.max(0, renderX1 - regionX1);
			int v = Math.max(0, renderZ1 - regionZ1);
			int drawWidth = Math.min(512, renderX2 - regionX1) - u;
			int drawHeight = Math.min(512, renderZ2 - regionZ1) - v;
			if (drawHeight <= 0 || drawWidth <= 0) continue;
			context.getMatrices().pushMatrix();
			context.getMatrices().translate((float) worldXToRenderX(regionX1 + u), (float) worldZToRenderY(regionZ1 + v));
			context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, 0, 0, u, v, drawWidth, drawHeight, drawWidth, drawHeight, 512, 512);
			context.getMatrices().popMatrix();
		}

		context.getMatrices().popMatrix();

		hoveredLandmark = null;
		double bestDistance = Double.MAX_VALUE;
		for (Landmark landmark : mapStorage.landmarks.values()) {
			if (landmark == editingLandmark) continue;
			BlockPos pos = landmark.get(LandmarkComponentTypes.POS);
			if (hideDecorations) continue;
			if (pos == null) {
				Set<ChunkPos> chunks = RegionPos.regionsToChunks(landmark.getOrDefault(LandmarkComponentTypes.CHUNKS, new HashMap<>()));
				for (ChunkPos chunk : chunks) {
					double screenX = renderToScreen(worldXToRenderX(chunk.getStartX()));
					double screenY = renderToScreen(worldZToRenderY(chunk.getStartZ()));
					boolean isInside = hoveredScreenX >= screenX && hoveredScreenX < screenX + 16 * scaleFactor && hoveredScreenY >= screenY && hoveredScreenY < screenY + 16 * scaleFactor;
					if (!inspectMode && isInside && 10 < bestDistance) {
						hoveredLandmark = landmark;
						bestDistance = 10;
					}
				}
				continue;
			}
			double landmarkCenterX = renderToScreen(worldXToRenderX(pos.getX()));
			double landmarkCenterY = renderToScreen(worldZToRenderY(pos.getZ()));
			double mouseDistance = (hoveredScreenX - landmarkCenterX) * (hoveredScreenX - landmarkCenterX) + (hoveredScreenY - landmarkCenterY) * (hoveredScreenY - landmarkCenterY);
			if (!inspectMode && mouseDistance < (6 * 6 * client.getWindow().getScaleFactor()) && mouseDistance < bestDistance) {
				hoveredLandmark = landmark;
				bestDistance = mouseDistance;
			}
		}

		hoveredPlayer = null;
		for (Map.Entry<UUID, PlayerSummary> entry : SurveyorClient.getFriends().entrySet()) {
			UUID uuid = entry.getKey();
			PlayerSummary player = entry.getValue();
			boolean friend = !SurveyorClient.getClientUuid().equals(uuid);
			boolean inDim = player.dimension().equals(dim);
			if ((friend && !inDim) || (!player.online() && !Hoofprint.CONFIG.style.offlinePlayers) || hideDecorations) continue;
			double dimX = player.pos().getX();
			double dimZ = player.pos().getZ();
			if (!inDim) {
				Map<RegistryKey<World>, Integer> scales = Hoofprint.CONFIG.dimensions.getScales(MinecraftClient.getInstance().getNetworkHandler());
				int newScale = scales.getOrDefault(dim, 0);
				int oldScale = scales.getOrDefault(player.dimension(), 0);
				if (newScale * oldScale == 0) continue;
				double mult = newScale / (double) oldScale;
				dimX = mult * dimX;
				dimZ = mult * dimZ;
			}
			double playerCenterX = renderToScreen(worldXToRenderX(dimX));
			double playerCenterY = renderToScreen(worldZToRenderY(dimZ));
			double mouseDistance = (hoveredScreenX - playerCenterX) * (hoveredScreenX - playerCenterX) + (hoveredScreenY - playerCenterY) * (hoveredScreenY - playerCenterY);
			if (mouseDistance < (4 * 4 * client.getWindow().getScaleFactor()) && mouseDistance < bestDistance) {
				hoveredLandmark = null;
				hoveredPlayer = player;
				bestDistance = mouseDistance;
			}
		}

		SurveyorClient.getFriends().forEach((uuid, player) -> renderPlayer(context, player, uuid));

		mapStorage.landmarks.values().stream().filter(landmark -> editingLandmark == null || !landmark.id().equals(editingLandmark.id())).forEach(landmark -> renderLandmark(context, landmark, scaleFactor));

		// Tooltips
		if (editingLandmark != null) {
			renderLandmark(context, editingLandmark, scaleFactor);
			double landmarkScreenX = renderToScreen(worldXToRenderX(editingLandmark.get(LandmarkComponentTypes.POS).getX()));
			double landmarkScreenY = renderToScreen(worldZToRenderY(editingLandmark.get(LandmarkComponentTypes.POS).getZ()));
			String cursor = List.of("|", "/", "-", "\\").get(cursorFrame / 10);
			context.drawTooltip(this.textRenderer, List.of(
				Text.empty().append(Text.literal(landmarkName.toString())).append(Text.literal(editingStyle ? "" : cursor).formatted(Formatting.GRAY)),
				Text.empty().append(Text.literal(landmarkStyle.toString()).formatted(styleValid ? Formatting.WHITE : Formatting.RED)).append(Text.literal(editingStyle ? cursor : "").formatted(Formatting.GRAY))
			), (int) landmarkScreenX, (int) landmarkScreenY);
		} else {
			if (inspectMode) {
				List<Text> tooltipLines = new ArrayList<>();
				if (!ifTerrainUnderCursor((block, biome, biomeId, y, lightLevel, waterDepth, waterLight) -> {
					tooltipLines.add(Text.of("x: %d, y: %d, z: %d".formatted(hoveredWorldX, y, hoveredWorldZ)));
					tooltipLines.add(block.getName());
					if (biomeId != null) tooltipLines.add(Text.translatable("biome.%s.%s".formatted(biomeId.getNamespace(), biomeId.getPath())));
					if (waterDepth > 0) tooltipLines.add(Text.of("Water: %d blocks".formatted(waterDepth)));
					if (lightLevel > 0) tooltipLines.add(Text.of("Block Light: %d".formatted(lightLevel)));
					if (waterLight > 0) tooltipLines.add(Text.of("Water Surface Light: %d".formatted(waterLight)));
				})) {
					tooltipLines.add(Text.of("x: %d, z: %d".formatted(hoveredWorldX, hoveredWorldZ)));
				}
				context.drawTooltip(this.textRenderer, tooltipLines, (int) hoveredScreenX, (int) hoveredScreenY);
			} else if (hoveredPlayer != null && hoveredPlayer.username() != null) {
				context.drawTooltip(this.textRenderer, Text.of(hoveredPlayer.username()), (int) hoveredScreenX, (int) hoveredScreenY);
			} else if (hoveredLandmark != null) {
				List<Text> tooltipLines = new ArrayList<>();
				if (hoveredLandmark.contains(LandmarkComponentTypes.NAME)) tooltipLines.add(hoveredLandmark.get(LandmarkComponentTypes.NAME));
				if (hoveredLandmark.contains(LandmarkComponentTypes.LORE)) tooltipLines.addAll(hoveredLandmark.get(LandmarkComponentTypes.LORE).stream().map(t -> t.copy().formatted(Formatting.GRAY)).toList());
				if (!tooltipLines.isEmpty()) {
					context.drawTooltip(this.textRenderer, tooltipLines, (int) hoveredScreenX, (int) hoveredScreenY);
				}
			}
		}
		if (!mapStorage.terrainQueue.isEmpty()) {
			context.drawText(this.textRenderer, Text.literal("Loading" + ".".repeat((cursorFrame / 8) % 4)).formatted(Formatting.GRAY), width - this.textRenderer.getWidth(Text.of("Loading...")), height - 10, 0xFF_FFFFFF, false);
		}
		if (switchFade > 0) {
			context.drawText(this.textRenderer, Text.literal(WordUtils.capitalizeFully(dim.getValue().getPath().replaceAll("[/_-]", " "))).formatted(Formatting.WHITE), 0, height - 10,  ColorHelper.getArgb(Math.min(255, (int) (255 * switchFade / 5.0)), 255, 255, 255), true);
		}
	}

	private void renderPlayer(DrawContext context, PlayerSummary player, UUID uuid) {
		boolean friend = !SurveyorClient.getClientUuid().equals(uuid);
		boolean inDim = player.dimension().equals(dim);
		if ((friend && !inDim) || (!player.online() && !Hoofprint.CONFIG.style.offlinePlayers) || hideDecorations) return;
		double dimX = player.pos().getX();
		double dimZ = player.pos().getZ();
		if (!inDim) {
			Map<RegistryKey<World>, Integer> scales = Hoofprint.CONFIG.dimensions.getScales(MinecraftClient.getInstance().getNetworkHandler());
			int newScale = scales.getOrDefault(dim, 0);
			int oldScale = scales.getOrDefault(player.dimension(), 0);
			if (newScale * oldScale == 0) return;
			double mult = newScale / (double) oldScale;
			dimX = mult * dimX;
			dimZ = mult * dimZ;
		}
		double playerScreenX = renderToScreen(worldXToRenderX(dimX));
		double playerScreenY = renderToScreen(worldZToRenderY(dimZ));
		boolean mouseOver = player == hoveredPlayer;
		context.getMatrices().pushMatrix();
		context.getMatrices().translate((float) playerScreenX, (float) playerScreenY);
		float playerRotation = ((float) Math.round(player.yaw() / 360f * PLAYER_ROTATION_STEPS) / PLAYER_ROTATION_STEPS) * 360f;
		context.getMatrices().rotate((float) Math.toRadians(180 + playerRotation));
		context.getMatrices().translate(-2.5F, -3.5F);
		int tint = !player.online() ? 77 : mouseOver ? 204 : 255;
		context.drawTexture(RenderPipelines.GUI_TEXTURED, Identifier.tryParse("textures/map/decorations/player.png"), 0, 0, 2, 0, 5, 7, 5, 7, 8, 8, ColorHelper.getArgb(255, (friend ? 0 : 255) * tint / 255, (inDim ? 255 : 204) * tint / 255, (friend ? 76 : 255) * tint / 255));
		context.getMatrices().popMatrix();
	}

	private void renderLandmark(DrawContext context, Landmark landmark, float scaleFactor) {
		BlockPos pos = landmark.get(LandmarkComponentTypes.POS);
		if (hideDecorations) return;
		if (pos == null) {
			Set<ChunkPos> chunks = RegionPos.regionsToChunks(landmark.getOrDefault(LandmarkComponentTypes.CHUNKS, new HashMap<>()));
			context.getMatrices().pushMatrix();
			context.getMatrices().scale(scaleFactor, scaleFactor);
			for (ChunkPos chunk : chunks) {
				context.getMatrices().pushMatrix();
				context.getMatrices().translate((float) worldXToRenderX(chunk.getStartX()), (float) worldZToRenderY(chunk.getStartZ()));
				int color = 0xFF_000000 | ColorUtil.applyBrightnessRGB(hoveredLandmark == landmark ? ColorUtil.Brightness.HIGH : ColorUtil.Brightness.NORMAL, landmark.getOrDefault(LandmarkComponentTypes.COLOR, 0xFFFFFF));
				context.fill(0, 0, 16, 16, 0x44FFFFFF & color);
				if (!chunks.contains(new ChunkPos(chunk.x - 1, chunk.z))) context.fill(0, 0, 1, 16, color);
				if (!chunks.contains(new ChunkPos(chunk.x, chunk.z - 1))) context.fill(0, 0, 16, 1, color);
				if (!chunks.contains(new ChunkPos(chunk.x + 1, chunk.z))) context.fill(15, 0, 16, 16, color);
				if (!chunks.contains(new ChunkPos(chunk.x, chunk.z + 1))) context.fill(0, 15, 16, 16, color);
				context.getMatrices().popMatrix();
			}
			context.getMatrices().popMatrix();
			return;
		}
		double landmarkScreenX = renderToScreen(worldXToRenderX(pos.getX()));
		double landmarkScreenY = renderToScreen(worldZToRenderY(pos.getZ()));
		int landmarkColor = landmark.getOrDefault(LandmarkComponentTypes.COLOR, 0xFFFFFF);
		boolean mouseOver = landmark == hoveredLandmark;
		int tint = mouseOver ? 0xB2B2B2 : 0xFFFFFF;
		context.getMatrices().pushMatrix();
		context.getMatrices().translate((float) landmarkScreenX, (float) landmarkScreenY);
		if (Hoofprint.CONFIG.style.itemOutlines && landmark.contains(LandmarkComponentTypes.STACK) && !landmark.get(LandmarkComponentTypes.STACK).isEmpty()) {
			// FIXME: Find some new way to do item outlines on 1.21.7
		}
		if (landmark.contains(LandmarkComponentTypes.STACK) && !landmark.get(LandmarkComponentTypes.STACK).isEmpty()) {
			ItemStack stack = landmark.get(LandmarkComponentTypes.STACK);
			if (mouseOver) {
				stack = stack.copy();
				stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
			}
			context.drawItemWithoutEntity(stack, -8, -8);
		} else {
			context.drawTexture(RenderPipelines.GUI_TEXTURED, Identifier.tryParse("textures/map/decorations/white_banner.png"), -4, -8, 0, 0, 8, 8, 8, 8, 8, 8, 0xFF000000 | ColorUtil.tint(landmarkColor, tint));
		}
		if (inspectMode && landmark.contains(LandmarkComponentTypes.NAME)) {
			// Draw Text Below Marker
			int textX = -this.textRenderer.getWidth(landmark.get(LandmarkComponentTypes.NAME)) / 2;
			context.drawText(this.textRenderer, landmark.get(LandmarkComponentTypes.NAME), textX, 12, 0xFFFFFFFF, true);
		}
		context.getMatrices().popMatrix();
	}

	interface FloorConsumer {
		void accept(Block block, Biome biome, Identifier biomeId, int y, int lightLevel, int waterDepth, int waterLight);
	}

	private boolean ifTerrainUnderCursor(FloorConsumer consumer) {
		ChunkPos cp = new ColumnPos(hoveredWorldX, hoveredWorldZ).toChunkPos();
		WorldSummary summary = SurveyorClient.tryGetSummary(dim);
		if (summary == null) return false;
		WorldTerrain terrain = summary.terrain();
		if (terrain == null) return false;
		Integer maxY = Hoofprint.CONFIG.dimensions.ceilings.getOrDefault(dim.getValue().toString(), null);
		ChunkSummary chunk = terrain.get(cp);
		if (chunk == null) return false;
		LayerSummary.Raw layer = chunk.toSingleLayer(null, maxY, 999);
		if (caveMode) layer = layer == null ? null : chunk.toSingleLayerBelow(null, layer.depths(), 999);
		if (layer == null) return false;
		int blockIndex = (hoveredWorldX - cp.getStartX()) * 16 + (hoveredWorldZ - cp.getStartZ());
		if (!layer.exists().get(blockIndex)) return false;
		Block block = terrain.getBlockPalette(cp).get(layer.blocks()[blockIndex]);
		Biome biome = terrain.getBiomePalette(cp).get(layer.biomes()[blockIndex]);
		Identifier biomeId = terrain.getBiomePalette(cp).registry().getId(biome);
		if (block == null || biome == null) return false;
		consumer.accept(block, biome, biomeId, 999 - layer.depths()[blockIndex], layer.lightLevels()[blockIndex], layer.waterDepths()[blockIndex], layer.waterLights()[blockIndex]);
		return true;
	}

	public HoofprintMapStorage mapStorage() {
		return HoofprintMapStorage.get(dim);
	}

	@Override
	protected void init() {
		this.dim = client.world.getRegistryKey();
		this.centreX = client.player.getBlockX();
		this.centreZ = client.player.getBlockZ();
		this.guiScale = Hoofprint.CONFIG.defaultScale < 1 ? (int) (Math.ceil(client.getWindow().getScaleFactor() / (Hoofprint.CONFIG.defaultScale == -1 ? 2.0 : 1.0))) : Hoofprint.CONFIG.defaultScale;
		client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ITEM_BOOK_PAGE_TURN, caveMode ? 0.8F : 1.0F));
		super.init();
	}

	@Override
	public void tick() {
		cursorFrame = (cursorFrame + 1) % 40;
		super.tick();
	}

	@Override
	public boolean shouldPause() {
		// todo. move bake ticking off-thread and remove this
		return false;
	}

	public static Landmark copyLandmarkWith(Landmark landmark, Identifier id, Consumer<LandmarkComponentMap> modifier) {
		LandmarkComponentMap copy = LandmarkComponentMap.builder().build();
		landmark.components().keySet().forEach(t -> copy.set(t, landmark.components().get(t)));
		modifier.accept(copy);
		return new Landmark(landmark.owner(), id, copy);
	}

	private void updateEdited() {
		String name = landmarkName.toString();
		editingLandmark = copyLandmarkWith(editingLandmark, editingLandmark.id(), b -> {
			if (name.isBlank()) {
				b.remove(LandmarkComponentTypes.NAME);
			} else {
				b.set(LandmarkComponentTypes.NAME, Text.literal(name));
			}
		});
		String style = landmarkStyle.toString();
		DyeColor dye = DyeColor.CODEC.byId(style);
		Item item = Registries.ITEM.getOptionalValue(Identifier.tryParse(style)).orElse(null);
		Integer color = style.startsWith("#") ? Ints.tryParse(style.substring(1), 16) : null;
		styleValid = true;
		if (dye != null) {
			editingLandmark = copyLandmarkWith(editingLandmark, editingLandmark.id(), b -> {
				b.remove(LandmarkComponentTypes.STACK);
				b.set(LandmarkComponentTypes.COLOR, dye.getFireworkColor());
			});
		} else if (item != null) {
			editingLandmark = copyLandmarkWith(editingLandmark, editingLandmark.id(), b -> {
				b.set(LandmarkComponentTypes.STACK, item.getDefaultStack().copy());
				b.remove(LandmarkComponentTypes.COLOR);
			});
		} else if (color != null && color > 0 && color <= 0xFFFFFF) {
			editingLandmark = copyLandmarkWith(editingLandmark, editingLandmark.id(), b -> {
				b.remove(LandmarkComponentTypes.STACK);
				b.set(LandmarkComponentTypes.COLOR, color);
			});
		} else {
			styleValid = false;
		}
	}

	private void changeDim(RegistryKey<World> newDim) {
		Map<RegistryKey<World>, Integer> scales = Hoofprint.CONFIG.dimensions.getScales(MinecraftClient.getInstance().getNetworkHandler());
		int newScale = scales.getOrDefault(newDim, 0);
		int oldScale = scales.getOrDefault(this.dim, 0);
		dim = newDim;
		if (newScale * oldScale > 0) {
			double mult = newScale / (double) oldScale;
			centreX = mult * centreX;
			centreZ = mult * centreZ;
			guiScale = (int) MathHelper.clamp(guiScale / mult, 1, 10);
		}
		switchFade = 20;
		client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.1F));
	}

	@Override
	public boolean charTyped(char chr, int modifiers) {
		if (editingLandmark != null) {
			StringBuilder editing = editingStyle ? landmarkStyle : landmarkName;
			editing.append(chr);
			updateEdited();
		}
		return super.charTyped(chr, modifiers);
	}

	@Override
	public void close() {
		client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ITEM_BOOK_PAGE_TURN, caveMode ? 0.8F : 1.0F));
		super.close();
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (editingLandmark != null) {
			switch (keyCode) {
				case GLFW.GLFW_KEY_ESCAPE -> { // discard landmark
					client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 2.0F));
					editingLandmark = null;
				}
				case GLFW.GLFW_KEY_ENTER -> { // save landmark
					client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.0F));
					saveLandmark();
				}
				case GLFW.GLFW_KEY_TAB, GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_UP -> {
					client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 2.0F));
					editingStyle = !editingStyle;
				}
				case GLFW.GLFW_KEY_BACKSPACE -> {
					StringBuilder editing = editingStyle ? landmarkStyle : landmarkName;
					if (!editing.isEmpty()) editing.deleteCharAt((editingStyle ? landmarkStyle : landmarkName).length() - 1);
					updateEdited();
				}
				default -> {
					return super.keyPressed(keyCode, scanCode, modifiers);
				}
			}
			return true;
		}
		if (Hoofprint.OPEN_MAP.matchesKey(keyCode, scanCode)) {
			close();
			return true;
		}
		switch (keyCode) {
			case GLFW.GLFW_KEY_LEFT_ALT -> {
				if (!inspectMode) {
					client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ITEM_SPYGLASS_USE, 1.0F));
					inspectMode = true;
				}
			}
			case GLFW.GLFW_KEY_H -> {
				client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ITEM_BRUSH_BRUSHING_GENERIC, 1.0F));
				hideDecorations = !hideDecorations;
			}
			case GLFW.GLFW_KEY_TAB -> {
				caveMode = !caveMode;
				client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ITEM_BOOK_PAGE_TURN, caveMode ? 0.8F : 1.0F));
			}
			case GLFW.GLFW_KEY_UP -> centreZ--;
			case GLFW.GLFW_KEY_DOWN -> centreZ++;
			case GLFW.GLFW_KEY_LEFT -> centreX--;
			case GLFW.GLFW_KEY_RIGHT -> centreX++;
			case GLFW.GLFW_KEY_SPACE -> {
				if (this.centreX != client.player.getBlockX() || this.centreZ != client.player.getBlockZ()) {
					client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_STONE_BUTTON_CLICK_OFF, 1.0F));
					this.centreX = client.player.getBlockX();
					this.centreZ = client.player.getBlockZ();
				}
			}
			case GLFW.GLFW_KEY_DELETE -> {
				if (hoveredLandmark == null || !SurveyorClient.canModify(hoveredLandmark.owner())) return true;
				WorldSummary summary = SurveyorClient.tryGetSummary(dim);
				if (summary == null) return true;
				WorldLandmarks landmarks = summary.landmarks();
				if (landmarks == null) return true;
				client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_LAVA_POP, 2.0F));
				landmarks.remove(hoveredLandmark.owner(), hoveredLandmark.id());
			}
			case GLFW.GLFW_KEY_LEFT_BRACKET -> {
				List<RegistryKey<World>> regKeys = Hoofprint.CONFIG.dimensions.getOrder(client.getNetworkHandler());
				if (regKeys.contains(dim)) changeDim(regKeys.get((regKeys.size() + regKeys.indexOf(dim) - 1) % regKeys.size()));
			}
			case GLFW.GLFW_KEY_RIGHT_BRACKET -> {
				List<RegistryKey<World>> regKeys = Hoofprint.CONFIG.dimensions.getOrder(client.getNetworkHandler());
				if (regKeys.contains(dim)) changeDim(regKeys.get((regKeys.indexOf(dim) + 1) % regKeys.size()));
			}
			default -> {
				return super.keyPressed(keyCode, scanCode, modifiers);
			}
		}
		return true;
	}

	private void saveLandmark() {
		if (editingLandmark == null || !SurveyorClient.canModify(editingLandmark.owner())) return;
		WorldSummary summary = SurveyorClient.tryGetSummary(dim);
		if (summary == null) return;
		WorldLandmarks landmarks = summary.landmarks();
		if (landmarks == null) return;
		landmarks.put(editingLandmark);
		editingLandmark = null;
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		switch (keyCode) {
			case GLFW.GLFW_KEY_LEFT_ALT -> {
				client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ITEM_SPYGLASS_USE, 0.8F));
				inspectMode = false;
			}
			default -> {
				return super.keyReleased(keyCode, scanCode, modifiers);
			}
		}
		return true;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == GLFW.GLFW_MOUSE_BUTTON_3) { // easter egg: knock
			if (!ifTerrainUnderCursor((block, biome, biomeId, y, lightLevel, waterDepth, waterLight) -> {
				SoundEvent hit = block.getDefaultState().getFluidState().getFluid().getBucketFillSound().orElse(block.getDefaultState().getSoundGroup().getHitSound());
				client.getSoundManager().play(PositionedSoundInstance.master(hit, 1.0F, 0.3F));
			})) {
				client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_HANGING_ROOTS_HIT, 1.0F, 0.3F));
			}
		} else if (button == GLFW.GLFW_MOUSE_BUTTON_2) {
			if (editingLandmark != null) { // discard
				editingLandmark = null;
				return true;
			} else if (hoveredLandmark != null && SurveyorClient.canModify(hoveredLandmark.owner()) && hoveredLandmark.contains(LandmarkComponentTypes.POS)) {
				editingLandmark = hoveredLandmark;
			} else if (hoveredLandmark == null) {
				if (!ifTerrainUnderCursor((block, biome, biomeId, y, lightLevel, waterDepth, waterLight) -> editingLandmark = Landmark.create(SurveyorClient.getClientUuid(), Identifier.of("hoofprint", "block/%s/%s/%s".formatted(hoveredWorldX, y, hoveredWorldZ)), builder -> {
					builder.add(LandmarkComponentTypes.POS, new BlockPos(hoveredWorldX, y, hoveredWorldZ))
						.add(LandmarkComponentTypes.NAME, block.getName())
						.add(LandmarkComponentTypes.COLOR, ColorUtil.argbToABGR(block.getDefaultMapColor().getRenderColor(MapColor.Brightness.NORMAL)));
					if (!block.asItem().getDefaultStack().isEmpty()) {
						builder.add(LandmarkComponentTypes.STACK, block.asItem().getDefaultStack());
					} else {
						Item bucket = block.getDefaultState().getFluidState().getFluid().getBucketItem();
						if (!bucket.getDefaultStack().isEmpty()) builder.add(LandmarkComponentTypes.STACK, bucket.getDefaultStack());
					}
					return builder;
				}))) {
					editingLandmark = Landmark.create(SurveyorClient.getClientUuid(), Identifier.of(Hoofprint.ID, "custom/%s/%s".formatted(hoveredWorldX, hoveredWorldZ)), b -> b.add(LandmarkComponentTypes.POS, new BlockPos(hoveredWorldX, 0, hoveredWorldZ)));
				}
			} else {
				return true;
			}
			landmarkName = new StringBuilder(editingLandmark.getOrDefault(LandmarkComponentTypes.NAME, Text.empty()).getString());
			landmarkStyle = new StringBuilder(editingLandmark.contains(LandmarkComponentTypes.STACK) ? Registries.ITEM.getId(editingLandmark.get(LandmarkComponentTypes.STACK).getItem()).toString().replace("minecraft:", "") :
				editingLandmark.contains(LandmarkComponentTypes.COLOR) ? Optional.ofNullable(DyeColor.byFireworkColor(editingLandmark.get(LandmarkComponentTypes.COLOR))).map(d -> d.name().toLowerCase()).orElse("#" + Integer.toHexString(0xFFFFFF & editingLandmark.get(LandmarkComponentTypes.COLOR)).toUpperCase()) : "white");
			editingStyle = false;
			updateEdited();
			SoundEvent placeSound = Optional.ofNullable(editingLandmark.get(LandmarkComponentTypes.STACK)).filter(s -> s.getItem() instanceof BlockItem).map(s -> ((BlockItem) s.getItem()).getBlock()).map(b -> b.getDefaultState().getFluidState().getFluid().getBucketFillSound().orElse(b.getDefaultState().getSoundGroup().getPlaceSound())).orElse(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT);
			client.getSoundManager().play(PositionedSoundInstance.master(placeSound, 1.2F));
			if (hasShiftDown()) saveLandmark();
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		hoveredScreenX = mouseX;
		hoveredScreenY = mouseY;
		hoveredWorldX = MathHelper.floor(screenXToWorldX(mouseX));
		hoveredWorldZ = MathHelper.floor(screenYToWorldZ(mouseY));
		super.mouseMoved(mouseX, mouseY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double hz, double amount) {
		if (amount >= 1 && guiScale < 10) {
			centreX += (screenXToWorldX(mouseX) - centreX) / (guiScale + 1);
			centreZ += (screenYToWorldZ(mouseY) - centreZ) / (guiScale + 1);
			guiScale++;
			return true;
		}
		if (amount <= -1 && guiScale > 1) {
			guiScale--;
			centreX -= (screenXToWorldX(mouseX) - centreX) / (guiScale + 1);
			centreZ -= (screenYToWorldZ(mouseY) - centreZ) / (guiScale + 1);
			return true;
		}

		return false;
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
		return guiScale / (float) client.getWindow().getScaleFactor();
	}

	float getWidth() {
		return width / getScaleFactor();
	}

	float getHeight() {
		return height / getScaleFactor();
	}
}
