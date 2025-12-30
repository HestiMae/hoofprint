package garden.hestia.hoofprint;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.mojang.datafixers.util.Function3;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.terrain.ChunkSummary;
import folk.sisby.surveyor.terrain.LayerSummary;
import folk.sisby.surveyor.terrain.RegionSummary;
import folk.sisby.surveyor.terrain.WorldTerrain;
import folk.sisby.surveyor.util.RegionPos;
import folk.sisby.surveyor.util.RegistryPalette;
import garden.hestia.hoofprint.util.ColorUtil;
import garden.hestia.hoofprint.util.ConstantLightMap;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static garden.hestia.hoofprint.util.ColorConstants.WATER_MAP_COLOR;
import static garden.hestia.hoofprint.util.ColorConstants.WATER_TEXTURE_COLOR;
import static garden.hestia.hoofprint.util.ColorUtil.applyBrightnessRGB;

public class HoofprintMapStorage {
	private static final Map<RegistryKey<World>, HoofprintMapStorage> INSTANCES = new HashMap<>();
	public static final String TEXTURE_PREFIX = "hoofprint/map";

	public final Map<RegionPos, Identifier> regionTextures = new ConcurrentHashMap<>();
	public final Map<RegionPos, Identifier> caveRegionTextures = new ConcurrentHashMap<>();
	public final Map<RegionPos, BitSet> terrainFilled = new ConcurrentHashMap<>();
	public final Map<RegionPos, BitSet> terrainQueue = Collections.synchronizedMap(new LinkedHashMap<>());
	public final Table<UUID, Identifier, Landmark> landmarks = Tables.synchronizedTable(HashBasedTable.create());
	public int minBlockX = 0;
	public int maxBlockX = 0;
	public int minBlockZ = 0;
	public int maxBlockZ = 0;

	public static HoofprintMapStorage get(RegistryKey<World> dim) {
		return HoofprintMapStorage.INSTANCES.computeIfAbsent(dim, (key) -> new HoofprintMapStorage());
	}

	public static void disconnect(ClientPlayNetworkHandler handler, MinecraftClient client) {
		INSTANCES.clear();
	}

	public boolean isEmpty() {
		return (regionTextures.isEmpty() && terrainQueue.isEmpty() && landmarks.isEmpty());
	}

	public void terrainUpdated(WorldSummary summary, Map<RegionPos, BitSet> chunks) {
		for (ChunkPos chunkPos : RegionPos.regionsToChunks(chunks)) {
			minBlockX = Math.min(minBlockX, chunkPos.getStartX());
			maxBlockX = Math.max(maxBlockX, chunkPos.getEndX());
			minBlockZ = Math.min(minBlockZ, chunkPos.getStartZ());
			maxBlockZ = Math.max(maxBlockZ, chunkPos.getEndZ());
			terrainQueue.computeIfAbsent(RegionPos.of(chunkPos), s -> new BitSet(RegionPos.CHUNK_AREA)).set(RegionPos.chunkToBit(chunkPos));
		}
	}

	public void landmarksAdded(WorldSummary summary, Multimap<UUID, Identifier> landmarks) {
		landmarks.forEach((uuid, id) -> {
			Landmark landmark = summary.landmarks().get(uuid, id);
			if (landmark != null) this.landmarks.put(uuid, id, landmark);
		});
	}

	public void landmarksRemoved(WorldSummary summary, Multimap<UUID, Identifier> landmarks) {
		landmarks.forEach(this.landmarks::remove);
	}

	public void tick(WorldSummary summary, long time) {
		if (time % Hoofprint.CONFIG.debug.ticksPerBake != 0) return;
		RegionPos rPos = terrainQueue.keySet().stream().findFirst().orElse(null);
		if (rPos != null) {
			bake(summary, rPos, terrainQueue.remove(rPos));
		}
	}

	private void bake(WorldSummary summary, RegionPos rPos, BitSet changes) {
		WorldTerrain terrain = summary.terrain();
		if (terrain == null) return;
		RegionSummary region = terrain.getRegion(rPos);
		BitSet filledArea = terrainFilled.computeIfAbsent(rPos, s -> new BitSet(RegionPos.CHUNK_AREA));
		changes.andNot(filledArea); // Don't live update the existing map.
		if (changes.isEmpty()) return;
		filledArea.or(changes);
		if (Hoofprint.CONFIG.debug.logBaking) Hoofprint.LOGGER.info("[Hoofprint] Baking {} chunks to the map texture for region {}", changes.cardinality(), rPos);
		ConstantLightMap lightMap = Hoofprint.CONFIG.dimensions.lightmaps.getOrDefault(summary.dimension().getValue().toString(), Hoofprint.CONFIG.dimensions.defaultLightmap);
		ChunkPos regionChunkOrigin = rPos.toChunk();
		Integer maxY = Hoofprint.CONFIG.dimensions.ceilings.getOrDefault(summary.dimension().getValue().toString(), null);

		LayerSummary.Raw[][] chunkSummaries = new LayerSummary.Raw[34][34];
		LayerSummary.Raw[][] chunkBelowSummaries = new LayerSummary.Raw[34][34];

		for (LayerConfiguration config : List.of(
			new LayerConfiguration(chunkSummaries, new int[544][544], new int[544][544], getNativeTexture(rPos, summary, "surface", regionTextures), (s, x, z) -> s == null ? null : s.toSingleLayer(null, maxY, 999), maxY == null),
			new LayerConfiguration(chunkBelowSummaries, new int[544][544], new int[544][544], getNativeTexture(rPos, summary, "cave", caveRegionTextures), (s, x, z) -> this.belowLayerUsingCache(chunkSummaries, s, x, z, maxY, 999), false)
		)) {
			for (int chunkX = 0; chunkX < 32; chunkX++) {
				for (int chunkZ = 0; chunkZ < 32; chunkZ++) {
					if (!changes.get(RegionPos.chunkToBit(chunkX, chunkZ))) continue;
					for (int x = -1; x <= 1; x++) {
						for (int z = -1; z <= 1; z++) {
							if (config.cache[chunkX + 1 + x][chunkZ + 1 + z] == null) { // Surrounding layers
								ChunkPos layerPos = new ChunkPos(regionChunkOrigin.x + chunkX + x, regionChunkOrigin.z + chunkZ + z);
								LayerSummary.Raw layer = config.flattener.apply(terrain.get(layerPos), chunkX + x, chunkZ + z);
								if (layer == null) continue;
								config.cache[chunkX + 1 + x][chunkZ + 1 + z] = layer;
								RegistryPalette<Biome>.ValueView biomePalette = terrain.getRegion(RegionPos.of(layerPos)).getBiomePalette();
								for (int i = 0; i < 16; i++) {
									for (int j = 0; j < 16; j++) {
										Biome biome = biomePalette.get(layer.biomes()[i * 16 + j]);
										if (biome == null) continue;
										config.waterColors[(chunkX + 1 + x) * 16 + i][(chunkZ + 1 + z) * 16 + j] = biome.getWaterColor();
										config.foliageColors[(chunkX + 1 + x) * 16 + i][(chunkZ + 1 + z) * 16 + j] = biome.getFoliageColor();
									}
								}
							}
						}
					}
					RegistryPalette<Block>.ValueView blockPalette = region.getBlockPalette();
					if (config.cache[chunkX + 1][chunkZ + 1] == null || blockPalette == null) continue;
					int[][] colors = this.getColors(config.cache, config.waterColors, config.foliageColors, chunkX + 1, chunkZ + 1, blockPalette, lightMap, config.skyLight);
					for (int x = 0; x < colors.length; x++) {
						for (int z = 0; z < colors[x].length; z++) {
							config.texture.getImage().setColor(16 * chunkX + x, 16 * chunkZ + z, ColorUtil.argbToABGR(colors[x][z]));
						}
					}
				}
			}
			config.texture.upload();
		}
	}

	LayerSummary.Raw belowLayerUsingCache(LayerSummary.Raw[][] cache, ChunkSummary s, int x, int z, Integer maxY, int worldHeight) {
		if (s == null) return null;
		if (cache[x + 1][z + 1] == null) return s.toSingleLayer(null, maxY, worldHeight);
		return s.toSingleLayerBelow(null, cache[x + 1][z + 1].depths(), worldHeight);
	}

	record LayerConfiguration(LayerSummary.Raw[][] cache, int[][] waterColors, int[][] foliageColors, NativeImageBackedTexture texture, Function3<ChunkSummary, Integer, Integer, LayerSummary.Raw> flattener, boolean skyLight) {}

	NativeImageBackedTexture getNativeTexture(RegionPos rPos, WorldSummary summary, String qualifier, Map<RegionPos, Identifier> regionTextures) {
		Identifier textureId = regionTextures.computeIfAbsent(rPos, r -> {
			Identifier inId = Identifier.of(Hoofprint.ID, "%s/%s/%s/%s/%s".formatted(TEXTURE_PREFIX, summary.dimension().getValue().toString().replace(":", "/"), qualifier, rPos.x(), rPos.z()));
			MinecraftClient.getInstance().getTextureManager().registerTexture(inId, new NativeImageBackedTexture(inId.toString(), 512, 512, true));
			return inId;
		});
		NativeImageBackedTexture terrainTexture = (NativeImageBackedTexture) MinecraftClient.getInstance().getTextureManager().getTexture(textureId);
		NativeImage image = terrainTexture.getImage();
		if (image == null) throw new IllegalStateException("[Hoofprint] WHO THREW OUT MY %s DYNAMIC TEXTURE".formatted(textureId));
		return terrainTexture;
	}

	int[][] getColors(LayerSummary.Raw[][] chunks, int[][] waterColors, int[][] foliageColors, int chunkX, int chunkZ, RegistryPalette<Block>.ValueView blockPalette, ConstantLightMap lightMap, boolean hasSky) {
		LayerSummary.Raw layer = chunks[chunkX][chunkZ];
		LayerSummary.Raw aboveLayer = chunks[chunkX][chunkZ - 1];
		int[][] colors = new int[16][16];
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int i = x * 16 + z;
				if (!layer.exists().get(i)) continue;
				int color;
				int waterColor;
				if (!Hoofprint.CONFIG.style.transparentWater && layer.waterDepths()[i] > 0) {
					color = Hoofprint.CONFIG.style.biomeWater ? ColorUtil.tint(WATER_TEXTURE_COLOR, ColorUtil.blendColors(waterColors, 16 * chunkX + x, 16 * chunkZ + z, Hoofprint.CONFIG.style.blendRadius)) : WATER_MAP_COLOR;
				} else {
					Block block = blockPalette.get(layer.blocks()[i]);
					Function<Integer, Integer> foliageFunction = ColorUtil.getBiomeColorProvider(block);
					if (foliageFunction != null) {
						color = foliageFunction.apply(ColorUtil.blendColors(foliageColors, 16 * chunkX + x, 16 * chunkZ + z, Hoofprint.CONFIG.style.blendRadius));
					} else {
						color = ColorUtil.getStaticBlockColor(block);
					}
				}
				if (Hoofprint.CONFIG.style.topography) {
					ColorUtil.Brightness brightness = ColorUtil.Brightness.NORMAL;
					if (!Hoofprint.CONFIG.style.transparentWater && layer.waterDepths()[i] > 0) {
						brightness = ColorUtil.getBrightnessFromDepth(layer.waterDepths()[i], x, z);
					} else if (z > 0) {
						if (layer.depths()[i - 1] < layer.depths()[i]) brightness = ColorUtil.Brightness.LOW;
						if (layer.depths()[i - 1] > layer.depths()[i]) brightness = ColorUtil.Brightness.HIGH;
					} else if (aboveLayer != null) {
						if (aboveLayer.depths()[x * 16 + 15] < layer.depths()[i]) brightness = ColorUtil.Brightness.LOW;
						if (aboveLayer.depths()[x * 16 + 15] > layer.depths()[i])
							brightness = ColorUtil.Brightness.HIGH;
					}
					color = applyBrightnessRGB(brightness, color);
				}
				if (Hoofprint.CONFIG.style.lighting && (Hoofprint.CONFIG.style.transparentWater || layer.waterDepths()[i] == 0)) {
					int blockLight = layer.lightLevels()[i];
					int skyLight = hasSky ? Math.max(ColorUtil.SKY_LIGHT - layer.waterDepths()[i], 0) : Hoofprint.CONFIG.style.ambientLight;
					color = ColorUtil.tint(color, lightMap.getMap()[skyLight][blockLight]);
				}
				if (Hoofprint.CONFIG.style.transparentWater && layer.waterDepths()[i] > 0) {
					waterColor = Hoofprint.CONFIG.style.biomeWater ? ColorUtil.tint(WATER_TEXTURE_COLOR, ColorUtil.blendColors(waterColors, 16 * chunkX + x, 16 * chunkZ + z, Hoofprint.CONFIG.style.blendRadius)) : applyBrightnessRGB(ColorUtil.Brightness.LOW, WATER_MAP_COLOR);
					if (Hoofprint.CONFIG.style.lighting) {
						int blockLight = layer.waterLights()[i];
						int skyLight = hasSky ? ColorUtil.SKY_LIGHT : Hoofprint.CONFIG.style.ambientLight;
						waterColor = ColorUtil.tint(waterColor, lightMap.getMap()[skyLight][blockLight]);
					}
					color = ColorUtil.blend(color, waterColor, 0.6F);
				}
				colors[x][z] = color | 0xff000000;
			}
		}
		return colors;
	}
}
