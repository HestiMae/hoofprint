package garden.hestia.hoofprint;

import com.google.common.collect.Multimap;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.landmark.LandmarkType;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class HoofprintMapStorage {
    public static final Map<RegistryKey<World>, HoofprintMapStorage> INSTANCES = new HashMap<>();

    Map<ChunkPos, int[][]> bakedTerrain;

    public static void worldLoad(ClientWorld clientWorld, WorldSummary worldSummary, ClientPlayerEntity clientPlayerEntity, Map<ChunkPos, BitSet> chunkPosBitSetMap, Multimap<RegistryKey<Structure>, ChunkPos> registryKeyChunkPosMultimap, Multimap<LandmarkType<?>, BlockPos> landmarkTypeBlockPosMultimap) {
    }
}
