package garden.hestia.hoofprint;

import folk.sisby.surveyor.terrain.LayerSummary;
import folk.sisby.surveyor.util.RegistryPalette;
import garden.hestia.hoofprint.util.FillBatcher;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class HoofprintScreen extends Screen {

    HoofprintMapStorage mapStorage;

    public HoofprintScreen() {
        super(Text.of("Hoofprint World Map"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        ChunkPos originChunk = MinecraftClient.getInstance().player.getChunkPos();
        if (this.mapStorage != null) {
            try (FillBatcher batcher = new FillBatcher(context))
            {
                int xSize = (int) Math.ceil((float) this.width / 16);
                int zSize = (int) Math.ceil((float) this.height / 16);
                for (int chunkX = 0; chunkX < xSize; chunkX++)
                {
                    for (int chunkZ = 0; chunkZ < zSize; chunkZ++)
                    {
                        ChunkPos pos = new ChunkPos(chunkX + originChunk.x - (xSize / 2), chunkZ + originChunk.z - (zSize / 2));
                        LayerSummary.Raw layer = mapStorage.bakedTerrain.get(pos);
                        RegistryPalette<Biome>.ValueView biomePalette = mapStorage.biomePalettes.get(pos);
                        RegistryPalette<Block>.ValueView blockPalette = mapStorage.blockPalettes.get(pos);

                        if (layer != null) {
                            int[][] colors = this.getColors(layer, biomePalette, blockPalette);
                            for (int x = 0; x < colors.length; x++) {
                                for (int z = 0; z < colors[x].length; z++) {
                                    batcher.add(chunkX * 16 + x, chunkZ * 16 + z, chunkX * 16 + x+1, chunkZ * 16 + z+1, 0, colors[x][z]);
                                }
                            }
                        }
                    }
                }
            }

        }
        super.render(context, mouseX, mouseY, delta);
    }

    int[][] getColors(LayerSummary.Raw layer, RegistryPalette<Biome>.ValueView biomePalette, RegistryPalette<Block>.ValueView blockPalette) {
        int[][] colors = new int[16][16];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                colors[x][z] = blockPalette.get(layer.blocks()[x * 16 + z]).getDefaultMapColor().color | 0xff000000;
            }
        }
        return colors;
    }

    @Override
    protected void init() {
        RegistryKey<World> dim = MinecraftClient.getInstance().world.getRegistryKey();
        this.mapStorage = HoofprintMapStorage.get(dim);
        super.init();
    }
}
