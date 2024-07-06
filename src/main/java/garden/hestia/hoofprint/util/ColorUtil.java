package garden.hestia.hoofprint.util;

import garden.hestia.hoofprint.Hoofprint;
import net.minecraft.world.biome.Biome;

import static garden.hestia.hoofprint.util.ColorConstants.WATER_MAP_COLOR;
import static garden.hestia.hoofprint.util.ColorConstants.WATER_TEXTURE_COLOR;

public class ColorUtil {
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

    public static int getWaterColor(Biome biome) {
        return Hoofprint.CONFIG.biomeWater ? tint(WATER_TEXTURE_COLOR, biome.getWaterColor()) : applyBrightnessRGB(Brightness.LOWEST, WATER_MAP_COLOR);
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
