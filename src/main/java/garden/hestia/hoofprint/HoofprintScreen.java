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
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class HoofprintScreen extends Screen {

    HoofprintMapStorage mapStorage;

    public HoofprintScreen() {
        super(Text.of("Hoofprint World Map"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Vec3d origin = MinecraftClient.getInstance().player.getPos();
        int playerX = (int) Math.floor(origin.x);
        int playerZ = (int) Math.floor(origin.z);
        ChunkPos originChunk = new ChunkPos(playerX / 16, playerZ / 16);
        int chunkRelativeX = playerX - (originChunk.x * 16);
        int chunkRelativeZ = playerZ - (originChunk.z * 16);
        if (this.mapStorage != null) {
            try (FillBatcher batcher = new FillBatcher(context))
            {
                int xSize = (int) Math.ceil((float) this.width / 16) + 4;
                int zSize = (int) Math.ceil((float) this.height / 16) + 4;
                for (int chunkX = -2; chunkX < xSize; chunkX++)
                {
                    for (int chunkZ = -2; chunkZ < zSize; chunkZ++)
                    {
                        ChunkPos pos = new ChunkPos(chunkX + originChunk.x - Math.round(xSize / 2.0f) + 2, chunkZ + originChunk.z - Math.round(zSize / 2.0f) + 2);
                        LayerSummary.Raw layer = mapStorage.bakedTerrain.get(pos);
                        RegistryPalette<Biome>.ValueView biomePalette = mapStorage.biomePalettes.get(pos);
                        RegistryPalette<Block>.ValueView blockPalette = mapStorage.blockPalettes.get(pos);

                        if (layer != null) {
                            int[][] colors = this.getColors(layer, biomePalette, blockPalette);
                            for (int x = 0; x < colors.length; x++) {
                                for (int z = 0; z < colors[x].length; z++) {
                                    int x1 = chunkX * 16 + x - chunkRelativeX;
                                    int y1 = chunkZ * 16 + z - chunkRelativeZ;

                                    if (x1 <= width && y1 <= height) batcher.add(x1, y1, x1 + 1, y1+1, 0, colors[x][z]);
                                }
                            }
                        }
                    }
                }
            }
            context.fill(width / 2, height / 2 , (width / 2) + 1, (height / 2) + 1, 0xff39ff14);
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

    @Override
    public boolean shouldPause() {
        return false;
    }
}
