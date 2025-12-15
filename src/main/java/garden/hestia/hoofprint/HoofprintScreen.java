package garden.hestia.hoofprint;

import com.google.common.primitives.Ints;
import com.mojang.blaze3d.systems.RenderSystem;
import folk.sisby.surveyor.PlayerSummary;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.client.SurveyorClient;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.WorldLandmarks;
import folk.sisby.surveyor.landmark.component.LandmarkComponentMap;
import folk.sisby.surveyor.landmark.component.LandmarkComponentTypes;
import folk.sisby.surveyor.terrain.ChunkSummary;
import folk.sisby.surveyor.terrain.LayerSummary;
import folk.sisby.surveyor.terrain.WorldTerrainSummary;
import folk.sisby.surveyor.util.RegionPos;
import garden.hestia.hoofprint.util.ColorUtil;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.BlockSoundGroup;
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
	HoofprintMapStorage mapStorage;
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

	public HoofprintScreen() {
		super(Text.of("Hoofprint World Map"));
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.getMatrices().push();
		float scaleFactor = getScaleFactor();
		context.getMatrices().scale(scaleFactor, scaleFactor, 1.0f);

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
		if (Hoofprint.CONFIG.renderBackground && (areaX2 - areaX1) > 0 && (areaY2 - areaY1) > 0) {
			context.getMatrices().push();
			context.getMatrices().translate(areaX1, areaY1, 0);
			context.drawGuiTexture(caveMode ? BACKGROUND_DARK : BACKGROUND, 0, 0, (int) (areaX2 - areaX1), (int) (areaY2 - areaY1));
			context.getMatrices().pop();
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
			context.getMatrices().push();
			context.getMatrices().translate(worldXToRenderX(regionX1 + u), worldZToRenderY(regionZ1 + v), 0);
			context.drawTexture(texture, 0, 0, drawWidth, drawHeight, u, v, drawWidth, drawHeight, 512, 512);
			context.getMatrices().pop();
		}

		context.getMatrices().pop();

		hoveredLandmark = null;
		double bestDistance = Double.MAX_VALUE;
		for (Map<Identifier, Landmark> map : this.mapStorage.landmarks.values()) {
			for (Landmark landmark : map.values()) {
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
		}

		RegistryKey<World> dim = client.world != null ? client.world.getRegistryKey() : null;

		hoveredPlayer = null;
		for (PlayerSummary player : SurveyorClient.getFriends().values()) {
			if (!player.dimension().equals(dim) || (!player.online() && !Hoofprint.CONFIG.showOffline) || hideDecorations) continue;
			double playerCenterX = renderToScreen(worldXToRenderX(player.pos().getX()));
			double playerCenterY = renderToScreen(worldZToRenderY(player.pos().getZ()));
			double mouseDistance = (hoveredScreenX - playerCenterX) * (hoveredScreenX - playerCenterX) + (hoveredScreenY - playerCenterY) * (hoveredScreenY - playerCenterY);
			if (mouseDistance < (4 * 4 * client.getWindow().getScaleFactor()) && mouseDistance < bestDistance) {
				hoveredLandmark = null;
				hoveredPlayer = player;
				bestDistance = mouseDistance;
			}
		}

		SurveyorClient.getFriends().forEach((uuid, player) -> renderPlayer(context, player, dim, uuid));

		this.mapStorage.landmarks.values().stream().flatMap(map -> map.values().stream()).filter(landmark -> editingLandmark == null || !landmark.id().equals(editingLandmark.id())).forEach(landmark -> renderLandmark(context, landmark, scaleFactor));

		// Tooltips
		if (editingLandmark != null) {
			renderLandmark(context, editingLandmark, scaleFactor);
			double landmarkScreenX = renderToScreen(worldXToRenderX(editingLandmark.get(LandmarkComponentTypes.POS).getX()));
			double landmarkScreenY = renderToScreen(worldZToRenderY(editingLandmark.get(LandmarkComponentTypes.POS).getZ()));
			String cursor = List.of("|", "/", "-", "\\").get(cursorFrame / 10);
			context.getMatrices().push();
			context.getMatrices().translate(landmarkScreenX, landmarkScreenY, 0);
			context.drawTooltip(this.textRenderer, List.of(
				Text.empty().append(Text.literal(landmarkName.toString())).append(Text.literal(editingStyle ? "" : cursor).formatted(Formatting.GRAY)),
				Text.empty().append(Text.literal(landmarkStyle.toString()).formatted(styleValid ? Formatting.WHITE : Formatting.RED)).append(Text.literal(editingStyle ? cursor : "").formatted(Formatting.GRAY))
			), 0, 0);
			context.getMatrices().pop();
		} else {
			context.getMatrices().push();
			context.getMatrices().translate(hoveredScreenX, hoveredScreenY, 0);
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
				context.drawTooltip(this.textRenderer, tooltipLines, 0, 0);
			} else if (hoveredPlayer != null && hoveredPlayer.username() != null) {
				context.drawTooltip(this.textRenderer, Text.of(hoveredPlayer.username()), 0, 0);
			} else if (hoveredLandmark != null) {
				List<Text> tooltipLines = new ArrayList<>();
				if (hoveredLandmark.contains(LandmarkComponentTypes.NAME)) tooltipLines.add(hoveredLandmark.get(LandmarkComponentTypes.NAME));
				if (hoveredLandmark.contains(LandmarkComponentTypes.LORE)) tooltipLines.addAll(hoveredLandmark.get(LandmarkComponentTypes.LORE).stream().map(t -> t.copy().formatted(Formatting.GRAY)).toList());
				if (!tooltipLines.isEmpty()) {
					context.drawTooltip(this.textRenderer, tooltipLines, 0, 0);
				}
			}
			context.getMatrices().pop();
		}
		if (!mapStorage.terrainQueue.isEmpty()) {
			context.drawText(this.textRenderer, Text.literal("Loading" + ".".repeat((cursorFrame / 8) % 4)).formatted(Formatting.GRAY), width-this.textRenderer.getWidth(Text.of("Loading...")), height - 10, 0xFFFFFF, false);
		}
	}

	private void renderPlayer(DrawContext context, PlayerSummary player, RegistryKey<World> dim, UUID uuid) {
		if (!player.dimension().equals(dim) || (!player.online() && !Hoofprint.CONFIG.showOffline) || hideDecorations) return;
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
		context.drawTexture(Identifier.tryParse("textures/map/decorations/player.png"), 0, 0, 5, 7, 2, 0, 5, 7, 8, 8);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		context.getMatrices().pop();
	}

	private void renderLandmark(DrawContext context, Landmark landmark, float scaleFactor) {
		BlockPos pos = landmark.get(LandmarkComponentTypes.POS);
		if (hideDecorations) return;
		if (pos == null) {
			Set<ChunkPos> chunks = RegionPos.regionsToChunks(landmark.getOrDefault(LandmarkComponentTypes.CHUNKS, new HashMap<>()));
			context.getMatrices().push();
			context.getMatrices().scale(scaleFactor, scaleFactor, 1.0f);
			for (ChunkPos chunk : chunks) {
				context.getMatrices().push();
				context.getMatrices().translate(worldXToRenderX(chunk.getStartX()), worldZToRenderY(chunk.getStartZ()), 0);
				int color = 0xFF_000000 | ColorUtil.applyBrightnessRGB(hoveredLandmark == landmark ? ColorUtil.Brightness.HIGH : ColorUtil.Brightness.NORMAL, landmark.getOrDefault(LandmarkComponentTypes.COLOR, 0xFFFFFF));
				context.fill(0, 0, 16, 16, 0x44FFFFFF & color);
				if (!chunks.contains(new ChunkPos(chunk.x - 1, chunk.z))) context.fill(0, 0, 1, 16, color);
				if (!chunks.contains(new ChunkPos(chunk.x , chunk.z - 1))) context.fill(0, 0, 16, 1, color);
				if (!chunks.contains(new ChunkPos(chunk.x + 1, chunk.z))) context.fill(15, 0, 16, 16, color);
				if (!chunks.contains(new ChunkPos(chunk.x, chunk.z + 1))) context.fill(0, 15, 16, 16, color);
				context.getMatrices().pop();
			}
			context.getMatrices().pop();
			return;
		}
		double landmarkScreenX = renderToScreen(worldXToRenderX(pos.getX()));
		double landmarkScreenY = renderToScreen(worldZToRenderY(pos.getZ()));
		float[] landmarkColors = (landmark.contains(LandmarkComponentTypes.COLOR) && !landmark.contains(LandmarkComponentTypes.STACK)) ? ColorUtil.getColorFromArgb(landmark.get(LandmarkComponentTypes.COLOR)) : new float[]{1.0f, 1.0f, 1.0f};
		boolean mouseOver = landmark == hoveredLandmark;
		float tint = mouseOver ? 0.7F : 1.0F;
		context.getMatrices().push();
		context.getMatrices().translate(landmarkScreenX, landmarkScreenY, 0);
		if (Hoofprint.CONFIG.itemOutlines && landmark.contains(LandmarkComponentTypes.STACK) && !landmark.get(LandmarkComponentTypes.STACK).isEmpty()) {
			RenderSystem.setShaderColor(0, 0, 0, 1);
			ItemStack stack = landmark.get(LandmarkComponentTypes.STACK);
			context.getMatrices().push();
			context.getMatrices().translate(0, 0, -5);
			context.drawItem(stack, -9, -9);
			context.drawItem(stack, -9, -8);
			context.drawItem(stack, -9, -7);
			context.drawItem(stack, -8, -9);
			context.drawItem(stack, -8, -7);
			context.drawItem(stack, -7, -9);
			context.drawItem(stack, -7, -8);
			context.drawItem(stack, -7, -7);
			context.getMatrices().pop();
		}
		RenderSystem.setShaderColor(landmarkColors[0] * tint, landmarkColors[1] * tint, landmarkColors[2] * tint, 1.0F);
		if (landmark.contains(LandmarkComponentTypes.STACK) && !landmark.get(LandmarkComponentTypes.STACK).isEmpty()) {
			ItemStack stack = landmark.get(LandmarkComponentTypes.STACK);
			context.drawItem(stack, -8, -8);
		} else {
			context.drawTexture(Identifier.tryParse("textures/map/decorations/white_banner.png"), -4, -8, 8, 8, 0, 0, 8, 8, 8, 8);
		}
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		if (inspectMode && landmark.contains(LandmarkComponentTypes.NAME)) {
			// Draw Text Below Marker
			int textX = -this.textRenderer.getWidth(landmark.get(LandmarkComponentTypes.NAME)) / 2;
			context.drawText(this.textRenderer, landmark.get(LandmarkComponentTypes.NAME), textX, 12, 0xFFFFFF, true);
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
		if (caveMode) layer = layer == null ? null : summary.toSingleLayerBelow(null, layer.depths(), client.world.getHeight());
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
		this.guiScale = (int) client.getWindow().getScaleFactor();
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
		Item item = Registries.ITEM.getOrEmpty(Identifier.tryParse(style)).orElse(null);
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
				if (client == null || client.world == null || client.player == null || hoveredLandmark == null || !WorldLandmarks.canModify (hoveredLandmark.owner(), client.world, null)) return true;
				WorldLandmarks landmarks = WorldSummary.of(client.world).landmarks();
				if (landmarks == null) return true;
				client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_LAVA_POP, 2.0F));
				landmarks.remove(client.world, hoveredLandmark.owner(), hoveredLandmark.id());
			}
			default -> {
				return super.keyPressed(keyCode, scanCode, modifiers);
			}
		}
		return true;
	}

	private void saveLandmark() {
		if (editingLandmark == null || client == null || client.world == null || client.player == null || !WorldLandmarks.canModify(editingLandmark.owner(), client.world, null)) return;
		WorldLandmarks landmarks = WorldSummary.of(client.world).landmarks();
		if (landmarks == null) return;
		landmarks.put(client.world, editingLandmark);
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
			ifTerrainUnderCursor((block, biome, biomeId, y, lightLevel, waterDepth, waterLight) -> {
				BlockSoundGroup group = block.getDefaultState().getSoundGroup();
				client.getSoundManager().play(PositionedSoundInstance.master(group.getHitSound(), 1.0F, 0.3F));
			});
		} else if (button == GLFW.GLFW_MOUSE_BUTTON_2) {
			if (editingLandmark != null) { // discard
				editingLandmark = null;
				return true;
			} else if (hoveredLandmark != null && WorldLandmarks.canModify(hoveredLandmark.owner(), client.world, null) && hoveredLandmark.contains(LandmarkComponentTypes.POS)) {
				editingLandmark = hoveredLandmark;
			} else if (hoveredLandmark == null) {
				if (!ifTerrainUnderCursor((block, biome, biomeId, y, lightLevel, waterDepth, waterLight) -> editingLandmark = Landmark.create(SurveyorClient.getClientUuid(), Identifier.of("hoofprint", "block/%s/%s/%s".formatted(hoveredWorldX, y, hoveredWorldZ)), builder -> {
					builder.add(LandmarkComponentTypes.POS, new BlockPos(hoveredWorldX, y, hoveredWorldZ))
						.add(LandmarkComponentTypes.NAME, block.getName())
						.add(LandmarkComponentTypes.COLOR, ColorUtil.argbToABGR(block.getDefaultMapColor().getRenderColor(MapColor.Brightness.NORMAL)));
					if (!block.asItem().getDefaultStack().isEmpty()) builder.add(LandmarkComponentTypes.STACK, block.asItem().getDefaultStack());
					return builder;
				}))) {
					editingLandmark = Landmark.create(SurveyorClient.getClientUuid(), Identifier.of(Hoofprint.ID, "custom/%s/%s".formatted(hoveredWorldX, hoveredWorldZ)), b -> b.add(LandmarkComponentTypes.POS, new BlockPos(hoveredWorldX, 0, hoveredWorldZ)));
				}
			}
			landmarkName = new StringBuilder(editingLandmark.getOrDefault(LandmarkComponentTypes.NAME, Text.empty()).getString());
			landmarkStyle = new StringBuilder(editingLandmark.contains(LandmarkComponentTypes.STACK) ? Registries.ITEM.getId(editingLandmark.get(LandmarkComponentTypes.STACK).getItem()).toString().replace("minecraft:", "") :
				editingLandmark.contains(LandmarkComponentTypes.COLOR) ? Optional.ofNullable(DyeColor.byFireworkColor(editingLandmark.get(LandmarkComponentTypes.COLOR))).map(d -> d.getName().toLowerCase()).orElse("#" + Integer.toHexString(0xFFFFFF & editingLandmark.get(LandmarkComponentTypes.COLOR)).toUpperCase()) : "white");
			editingStyle = false;
			updateEdited();
			SoundEvent placeSound = Optional.ofNullable(editingLandmark.get(LandmarkComponentTypes.STACK)).filter(s -> s.getItem() instanceof BlockItem).map(s -> ((BlockItem) s.getItem()).getBlock().getDefaultState().getSoundGroup().getPlaceSound()).orElse(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT);
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
		hoveredWorldX = (int) Math.floor(screenXToWorldX(mouseX) + (screenXToWorldX(mouseX) < 0 ? 0.5 : -0.5)); // Dunno
		hoveredWorldZ = (int) Math.floor(screenYToWorldZ(mouseY));
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
		return (float) (guiScale / client.getWindow().getScaleFactor());
	}

	float getWidth() {
		return width / getScaleFactor();
	}

	float getHeight() {
		return height / getScaleFactor();
	}
}
