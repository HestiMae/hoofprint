package garden.hestia.hoofprint.util;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

import java.util.List;

public class BlockConstants {
    public static final List<Block> FOLIAGE_BLOCKS = List.of(Blocks.OAK_LEAVES, Blocks.JUNGLE_LEAVES,
        Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.MANGROVE_LEAVES, Blocks.VINE);
    public static final List<Block> GRASS_BLOCKS = List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS,
        Blocks.FERN, Blocks.POTTED_FERN, Blocks.LARGE_FERN, Blocks.SUGAR_CANE);
    public static final List<Block> GRASS_BLOCK_BLOCKS = List.of(Blocks.GRASS_BLOCK);
    public static final List<Block> STONE_BLOCKS = List.of(Blocks.STONE, Blocks.ANDESITE);
}
