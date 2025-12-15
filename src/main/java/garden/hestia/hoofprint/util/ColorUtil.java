package garden.hestia.hoofprint.util;

import garden.hestia.hoofprint.Hoofprint;
import net.minecraft.block.Block;
import net.minecraft.client.color.world.FoliageColors;
import net.minecraft.util.math.ColorHelper;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class ColorUtil {
	public static final int SKY_LIGHT = 15;
	public static final Map<Predicate<Block>, Function<Integer, Integer>> BLOCK_COLOR_PROVIDERS = Map.of(
		BlockConstants.FOLIAGE_BLOCKS::contains, foliage -> ColorUtil.tint(ColorConstants.FOLIAGE_TEXTURE_COLOR, foliage),
		BlockConstants.GRASS_BLOCKS::contains, foliage -> ColorUtil.tint(ColorConstants.GRASS_TEXTURE_COLOR, foliage),
		BlockConstants.GRASS_BLOCK_BLOCKS::contains, foliage -> ColorUtil.tint(ColorConstants.GRASS_BLOCK_TEXTURE_COLOR, foliage)
	);
	public static final Map<Predicate<Block>, Integer> CONSTANT_BLOCK_COLOR_PROVIDERS = Map.of(
		BlockConstants.STONE_BLOCKS::contains, ColorConstants.STONE_MAP_COLOR,
		BlockConstants.ICE_BLOCKS::contains, ColorConstants.ICE_MAP_COLOR,
		BlockConstants.SPRUCE_BLOCKS::contains, ColorUtil.tint(ColorConstants.FOLIAGE_TEXTURE_COLOR, FoliageColors.getSpruceColor()),
		BlockConstants.BIRCH_BLOCKS::contains, ColorUtil.tint(ColorConstants.FOLIAGE_TEXTURE_COLOR, FoliageColors.getBirchColor()),
		BlockConstants.MANGROVE_BLOCKS::contains, ColorUtil.tint(ColorConstants.FOLIAGE_TEXTURE_COLOR, FoliageColors.getMangroveColor())
	);

	public static int tint(int base, int tint) {
		int a1 = (base >>> 24);
		int r1 = ((base & 0xff0000) >> 16);
		int g1 = ((base & 0xff00) >> 8);
		int b1 = (base & 0xff);

		int a2 = (tint >>> 24);
		int r2 = ((tint & 0xff0000) >> 16);
		int g2 = ((tint & 0xff00) >> 8);
		int b2 = (tint & 0xff);

		int a = (a1 * a2) / 256;
		int r = (r1 * r2) / 256;
		int g = (g1 * g2) / 256;
		int b = (b1 * b2) / 256;

		return a << 24 | r << 16 | g << 8 | b;
	}

	public static int blend(int c1, int c2, float ratio) {
		float iRatio = 1.0f - ratio;

		int a1 = (c1 >>> 24);
		int r1 = ((c1 & 0xff0000) >> 16);
		int g1 = ((c1 & 0xff00) >> 8);
		int b1 = (c1 & 0xff);

		int a2 = (c2 >>> 24);
		int r2 = ((c2 & 0xff0000) >> 16);
		int g2 = ((c2 & 0xff00) >> 8);
		int b2 = (c2 & 0xff);

		int a = (int) ((a1 * iRatio) + (a2 * ratio));
		int r = (int) ((r1 * iRatio) + (r2 * ratio));
		int g = (int) ((g1 * iRatio) + (g2 * ratio));
		int b = (int) ((b1 * iRatio) + (b2 * ratio));

		return a << 24 | r << 16 | g << 8 | b;
	}

	public static int applyBrightnessRGB(Brightness brightness, int color) {
		int i = brightness.brightness;
		int r = (color >> 16 & 0xFF) * i / 255;
		int g = (color >> 8 & 0xFF) * i / 255;
		int b = (color & 0xFF) * i / 255;
		return r << 16 | g << 8 | b;
	}

	/**
	 * @author ampflower
	 */
	public static Brightness getBrightnessFromDepth(int depth, int x, int z) {
		if (depth == 7) { // Emulate floating point error in vanilla code
			depth = 8;
		}
		int ditheredDepth = depth + (((x ^ z) & 1) << 1);
		if (ditheredDepth > 9) {
			return Brightness.LOW;
		} else if (ditheredDepth >= 5) {
			return Brightness.NORMAL;
		} else {
			return Brightness.HIGH;
		}
	}

	public static int blendColors(int[][] colors, int x, int z, int radius) {
		if (radius == 0) return colors[x][z];
		long r = 0;
		long g = 0;
		long b = 0;
		int num = 0;
		for (int i = x - radius; i < x + radius; i++) {
			for (int j = z - radius; j < z + radius; j++) {
				// if ((x - i) * (x - i) + (z - j) * (z - j) > radius * radius) continue;
				r += (colors[i][j] & 0xFF0000) >> 16;
				g += (colors[i][j] & 0xFF00) >> 8;
				b += colors[i][j] & 0xFF;
				num += Math.min(Math.abs(colors[i][j]), 1);
			}
		}
		return Math.toIntExact((r / num & 0xFF) << 16 | (g / num & 0xFF) << 8 | b / num & 0xFF);
	}

	public static Function<Integer, Integer> getBiomeColorProvider(Block block) {
		if (!Hoofprint.CONFIG.style.biomeFoliage) return null;
		for (Predicate<Block> predicate : BLOCK_COLOR_PROVIDERS.keySet()) {
			if (predicate.test(block)) {
				return BLOCK_COLOR_PROVIDERS.get(predicate);
			}
		}
		return null;
	}

	public static int getStaticBlockColor(Block block) {
		if (Hoofprint.CONFIG.style.accurateColors) {
			for (Predicate<Block> predicate : CONSTANT_BLOCK_COLOR_PROVIDERS.keySet()) {
				if (predicate.test(block)) {
					return CONSTANT_BLOCK_COLOR_PROVIDERS.get(predicate);
				}
			}
		}
		return block.getDefaultMapColor().color;
	}

	public static int argbToABGR(int argbColor) {
		int r = (argbColor >> 16) & 0xFF;
		int b = argbColor & 0xFF;
		return (argbColor & 0xFF00FF00) | (b << 16) | r;
	}

	public static float[] getColorFromArgb(int color) {
		return new float[]{ColorHelper.Argb.getRed(color) / 255f, ColorHelper.Argb.getGreen(color) / 255f, ColorHelper.Argb.getBlue(color) / 255f};
	}

	public enum Brightness {
		LOW(180),
		NORMAL(220),
		HIGH(255),
		LOWEST(135);

		public final int brightness;

		Brightness(int brightness) {
			this.brightness = brightness;

		}
	}
}
