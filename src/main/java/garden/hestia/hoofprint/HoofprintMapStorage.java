package garden.hestia.hoofprint;

import com.google.common.collect.Multimap;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.landmark.LandmarkType;
import folk.sisby.surveyor.landmark.WorldLandmarks;
import folk.sisby.surveyor.terrain.LayerSummary;
import folk.sisby.surveyor.terrain.WorldTerrainSummary;
import folk.sisby.surveyor.util.RegistryPalette;
import net.minecraft.block.Block;
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

    Map<ChunkPos, LayerSummary.Raw> bakedTerrain = new HashMap<>();
    Map<ChunkPos, RegistryPalette<Biome>.ValueView> biomePalettes = new HashMap<>();
    Map<ChunkPos, RegistryPalette<Block>.ValueView> blockPalettes = new HashMap<>();

    public void worldLoad(ClientWorld world, WorldSummary summary, ClientPlayerEntity player, Map<ChunkPos, BitSet> terrain, Multimap<RegistryKey<Structure>, ChunkPos> structures, Multimap<LandmarkType<?>, BlockPos> landmarks) {
        for (ChunkPos chunkPos : WorldTerrainSummary.toKeys(terrain)) {
            bakedTerrain.put(chunkPos, summary.terrain().get(chunkPos).toSingleLayer(null, null, world.getHeight()));
            biomePalettes.put(chunkPos, summary.terrain().getBiomePalette(chunkPos));
            blockPalettes.put(chunkPos, summary.terrain().getBlockPalette(chunkPos));
        }
    }

    public void terrainUpdated(World world, WorldTerrainSummary worldTerrainSummary, Collection<ChunkPos> chunks) {
        for (ChunkPos chunk : chunks) {
            bakedTerrain.put(chunk, worldTerrainSummary.get(chunk).toSingleLayer(null, null, world.getHeight()));
            biomePalettes.put(chunk, worldTerrainSummary.getBiomePalette(chunk));
            blockPalettes.put(chunk, worldTerrainSummary.getBlockPalette(chunk));
        }
    }


    public void landmarksAdded(World world, WorldLandmarks worldLandmarks, Multimap<LandmarkType<?>, BlockPos> landmarks) {
        // handle later
    }

    public static HoofprintMapStorage get(RegistryKey<World> dim) {
        return HoofprintMapStorage.INSTANCES.computeIfAbsent(dim, (key) -> new HoofprintMapStorage());
    }

}
