package garden.hestia.hoofprint;

import com.google.common.collect.Multimap;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.WorldLandmarks;
import folk.sisby.surveyor.terrain.LayerSummary;
import folk.sisby.surveyor.terrain.RegionSummary;
import folk.sisby.surveyor.terrain.WorldTerrainSummary;
import folk.sisby.surveyor.util.RegistryPalette;
import garden.hestia.hoofprint.util.ColorUtil;
import garden.hestia.hoofprint.util.ConstantLightMap;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HoofprintMapStorage {
	private static final Map<RegistryKey<World>, HoofprintMapStorage> INSTANCES = new HashMap<>();
	public static final String TEXTURE_PREFIX = "hoofprint/map";

	Map<ChunkPos, Identifier> regionTextures = new ConcurrentHashMap<>();
	Map<ChunkPos, Identifier> caveRegionTextures = new ConcurrentHashMap<>();
	Map<ChunkPos, BitSet> terrainFilled = new ConcurrentHashMap<>();
	Map<ChunkPos, BitSet> terrainQueue = new ConcurrentHashMap<>();
	Map<UUID, Map<Identifier, Landmark>> landmarks = new ConcurrentHashMap<>();

	public static HoofprintMapStorage get(RegistryKey<World> dim) {
		return HoofprintMapStorage.INSTANCES.computeIfAbsent(dim, (key) -> new HoofprintMapStorage());
	}

	public static void disconnect(ClientPlayNetworkHandler handler, MinecraftClient client) {
		INSTANCES.clear();
	}

	public void worldLoad(ClientWorld world, WorldSummary summary, ClientPlayerEntity player, Map<ChunkPos, BitSet> terrain, Multimap<RegistryKey<Structure>, ChunkPos> structures, Multimap<UUID, Identifier> landmarks) {
		terrainUpdated(world, summary.terrain(), WorldTerrainSummary.toKeys(terrain));
		landmarksAdded(world, summary.landmarks(), landmarks);
	}

	public void terrainUpdated(World world, WorldTerrainSummary worldTerrainSummary, Collection<ChunkPos> chunks) {
		for (ChunkPos chunkPos : chunks) {
			ChunkPos rPos = new ChunkPos(RegionSummary.chunkToRegion(chunkPos.x), RegionSummary.chunkToRegion(chunkPos.z));
			terrainQueue.computeIfAbsent(rPos, s -> new BitSet(RegionSummary.BITSET_SIZE)).set(RegionSummary.bitForChunk(chunkPos));
		}
	}

	public void landmarksAdded(World world, WorldLandmarks worldLandmarks, Multimap<UUID, Identifier> landmarks) {
		landmarks.forEach((uuid, id) -> {
			this.landmarks.computeIfAbsent(uuid, t -> new HashMap<>()).put(id, worldLandmarks.get(uuid, id));
		});
	}

	public void landmarksRemoved(World world, WorldLandmarks worldLandmarks, Multimap<UUID, Identifier> landmarks) {
		landmarks.forEach((type, pos) -> {
			this.landmarks.computeIfAbsent(type, t -> new HashMap<>()).remove(pos);
			if (this.landmarks.get(type).isEmpty()) this.landmarks.remove(type);
		});
	}

	public void tick(World world) {
		ChunkPos rPos = terrainQueue.keySet().stream().findFirst().orElse(null);
		if (rPos != null) {
			bake(world, rPos, terrainQueue.remove(rPos));
		}
	}

	private void bake(World world, ChunkPos rPos, BitSet changes) {
		WorldTerrainSummary terrain = WorldSummary.of(world).terrain();
		if (terrain == null) return;
		RegionSummary region = terrain.getRegion(rPos);
		BitSet filledArea = terrainFilled.computeIfAbsent(rPos, s -> new BitSet(RegionSummary.BITSET_SIZE));
		changes.andNot(filledArea); // Don't live update the existing map.
		if (changes.isEmpty()) return;
		filledArea.or(changes);
		if (Hoofprint.CONFIG.logMapBaking) Hoofprint.LOGGER.info("[Hoofprint] Baking {} chunks to the map texture for region {}", changes.cardinality(), rPos);
		ConstantLightMap lightMap = Hoofprint.CONFIG.dimensionLightMaps.getOrDefault(world.getRegistryKey().getValue().toString(), Hoofprint.CONFIG.lightMap);
		ChunkPos regionChunkOrigin = new ChunkPos(RegionSummary.regionToChunk(rPos.x), RegionSummary.regionToChunk(rPos.z));

		Integer maxY = Hoofprint.CONFIG.dimensionMaxYValues.getOrDefault(world.getRegistryKey().getValue().toString(), null);

		Identifier textureId = regionTextures.computeIfAbsent(rPos, r -> MinecraftClient.getInstance().getTextureManager().registerDynamicTexture(TEXTURE_PREFIX, new NativeImageBackedTexture(512, 512, true)));
		NativeImageBackedTexture terrainTexture = (NativeImageBackedTexture) MinecraftClient.getInstance().getTextureManager().getTexture(textureId);
		NativeImage image = terrainTexture.getImage();
		if (image == null) throw new IllegalStateException("[Hoofprint] WHO THREW OUT MY %s DYNAMIC TEXTURE".formatted(textureId));
		LayerSummary.Raw[][] chunkSummaries = new LayerSummary.Raw[34][34];
		for (int chunkX = 0; chunkX < 32; chunkX++) {
			for (int chunkZ = 0; chunkZ < 32; chunkZ++) {
				if (!changes.get(RegionSummary.bitForXZ(chunkX, chunkZ))) continue;
				ChunkPos chunkPos = new ChunkPos(regionChunkOrigin.x + chunkX, regionChunkOrigin.z + chunkZ);
				LayerSummary.Raw layer = terrain.get(chunkPos).toSingleLayer(null, maxY, world.getHeight());
				chunkSummaries[chunkX + 1][chunkZ + 1] = layer;
				RegistryPalette<Biome>.ValueView biomePalette = region.getBiomePalette();
				RegistryPalette<Block>.ValueView blockPalette = region.getBlockPalette();
				if (layer != null && biomePalette != null && blockPalette != null) {
					if (chunkSummaries[chunkX + 1][chunkZ] == null) { // Above Layer
						chunkSummaries[chunkX + 1][chunkZ] = terrain.get(new ChunkPos(chunkPos.x, chunkPos.z - 1)).toSingleLayer(null, maxY, world.getHeight());
					}
					int[][] colors = this.getColors(layer, chunkSummaries[chunkX + 1][chunkZ], biomePalette, blockPalette, lightMap);
					for (int x = 0; x < colors.length; x++) {
						for (int z = 0; z < colors[x].length; z++) {
							image.setColor(16 * chunkX + x, 16 * chunkZ + z, ColorUtil.argbToABGR(colors[x][z]));
						}
					}
				}
			}
		}
		terrainTexture.upload();
	}

	int[][] getColors(LayerSummary.Raw layer, @Nullable LayerSummary.Raw aboveLayer, RegistryPalette<Biome>.ValueView biomePalette, RegistryPalette<Block>.ValueView blockPalette, ConstantLightMap lightMap) {
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
					color = ColorUtil.tint(color, lightMap.getMap()[skyLight][blockLight]);
				}
				if (Hoofprint.CONFIG.transparentWater && layer.waterDepths()[i] > 0) {
					waterColor = ColorUtil.getWaterColor(biomePalette.get(layer.biomes()[i]));
					if (Hoofprint.CONFIG.lighting) {
						int blockLight = layer.waterLights()[i];
						int skyLight = ColorUtil.SKY_LIGHT;
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
