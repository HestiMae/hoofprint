package garden.hestia.hoofprint;

import com.google.common.collect.Multimap;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.LandmarkType;
import folk.sisby.surveyor.landmark.WorldLandmarks;
import folk.sisby.surveyor.terrain.ChunkSummary;
import folk.sisby.surveyor.terrain.LayerSummary;
import folk.sisby.surveyor.terrain.RegionSummary;
import folk.sisby.surveyor.terrain.WorldTerrainSummary;
import folk.sisby.surveyor.util.RegistryPalette;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.Structure;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class HoofprintMapStorage {
    private static final Map<RegistryKey<World>, HoofprintMapStorage> INSTANCES = new HashMap<>();

    int minChunkX = 0;
    int maxChunkX = -1;
    int minChunkZ = 0;
    int maxChunkZ = -1;
    Map<ChunkPos, LayerSummary.Raw[][]> bakedTerrain = new HashMap<>();
    Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> landmarks = new HashMap<>();
    Map<ChunkPos, RegistryPalette<Biome>.ValueView> biomePalettes = new HashMap<>();
    Map<ChunkPos, RegistryPalette<Block>.ValueView> blockPalettes = new HashMap<>();

    public static HoofprintMapStorage get(RegistryKey<World> dim) {
        return HoofprintMapStorage.INSTANCES.computeIfAbsent(dim, (key) -> new HoofprintMapStorage());
    }

    public void worldLoad(ClientWorld world, WorldSummary summary, ClientPlayerEntity player, Map<ChunkPos, BitSet> terrain, Multimap<RegistryKey<Structure>, ChunkPos> structures, Multimap<LandmarkType<?>, BlockPos> landmarks) {
        terrainUpdated(world, summary.terrain(), WorldTerrainSummary.toKeys(terrain));
        landmarksAdded(world, summary.landmarks(), landmarks);
    }

    public void terrainUpdated(World world, WorldTerrainSummary worldTerrainSummary, Collection<ChunkPos> chunks) {
        for (ChunkPos chunkPos : chunks) {
            minChunkX = Math.min(chunkPos.x, minChunkX);
            maxChunkX = Math.max(chunkPos.x, maxChunkX);
            minChunkZ = Math.min(chunkPos.z, minChunkZ);
            maxChunkZ = Math.max(chunkPos.z, maxChunkZ);
            ChunkSummary chunk = worldTerrainSummary.get(chunkPos);
            if (chunk == null) return;
            bakedTerrain.computeIfAbsent(new ChunkPos(RegionSummary.chunkToRegion(chunkPos.x), RegionSummary.chunkToRegion(chunkPos.z)), c -> new LayerSummary.Raw[32][32])[RegionSummary.regionRelative(chunkPos.x)][RegionSummary.regionRelative(chunkPos.z)] = chunk.toSingleLayer(null, null, world.getHeight());
            biomePalettes.put(chunkPos, worldTerrainSummary.getBiomePalette(chunkPos));
            blockPalettes.put(chunkPos, worldTerrainSummary.getBlockPalette(chunkPos));
        }
    }

    public void landmarksAdded(World world, WorldLandmarks worldLandmarks, Multimap<LandmarkType<?>, BlockPos> landmarks) {
        landmarks.forEach((type, pos) -> {
            this.landmarks.computeIfAbsent(type, t -> new HashMap<>()).put(pos, worldLandmarks.get(type, pos));
        });
    }

    public void landmarksRemoved(World world, WorldLandmarks worldLandmarks, Multimap<LandmarkType<?>, BlockPos> landmarks) {
        landmarks.forEach((type, pos) -> {
            this.landmarks.computeIfAbsent(type, t -> new HashMap<>()).remove(pos);
            if (this.landmarks.get(type).isEmpty()) this.landmarks.remove(type);
        });
    }

	public static void disconnect(ClientPlayNetworkHandler handler, MinecraftClient client) {
		INSTANCES.clear();
	}
}
