package garden.hestia.hoofprint;

import folk.sisby.kaleido.api.WrappedConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.IntegerRange;
import folk.sisby.kaleido.lib.quiltconfig.api.values.ValueMap;
import garden.hestia.hoofprint.util.ConstantLightMap;

import java.util.Map;

public class HoofprintConfig extends WrappedConfig {
	@Comment("Options to change the visuals of the map to be more or less vanilla-style.")
	public Style style = new Style();

	public static class Style implements Section {
		@Comment("Whether to color the water based on biome.")
		@Comment("Disable for a vanilla style.")
		public boolean biomeWater = true;

		@Comment("Whether to color grass and foliage blocks based on biome.")
		@Comment("Disable for a vanilla style.")
		public boolean biomeFoliage = true;

		@Comment("How large of an area to blend biome colors for water and foliage.")
		@Comment("Set to 0 for no blending. Higher is more performance intensive.")
		@IntegerRange(min = 0, max = 16)
		public int blendRadius = 9;

		@Comment("Whether to override the base color of odd-colored blocks like stone, ice, and leaf variants.")
		@Comment("Disable for a vanilla style.")
		public boolean accurateColors = true;

		@Comment("Whether to render water as transparent, becoming more opaque in deep waters.")
		@Comment("Disable for a vanilla style.")
		public boolean transparentWater = true;

		@Comment("Whether to consider sky and block lighting when coloring map pixels.")
		@Comment("Disable for a vanilla style.")
		public boolean lighting = true;

		@Comment("What level of skylight to use when in cave mode.")
		@IntegerRange(min = 0, max = 15)
		public int ambientLight = 8;

		@Comment("Whether to shade the map based on relative elevation and water depth.")
		@Comment("Enable for a vanilla style. Disable for a very flat map.")
		public boolean topography = true;

		@Comment("Whether to draw stacks tinted black underneath themselves to create an outline.")
		@Comment("Noticeable performance cost with many stacks on screen.")
		public boolean itemOutlines = true;

		@Comment("Whether to show disconnected players and offline group members on the map.")
		public boolean offlinePlayers = true;

		@Comment("Whether to render the map-textured background to help show valid pan areas.")
		public boolean mapBackground = true;
	}

	@Comment("Options to adjust map behaviour for custom or modified dimensions.")
	public Dimensions dimensions = new Dimensions();

	public static class Dimensions implements Section {
		@Comment("Which lightmap to use for dimensions not specified below.")
		@Comment("This changes the color tone of the lighting, when lighting is enabled.")
		public ConstantLightMap defaultLightmap = ConstantLightMap.DAY;

		@Comment("Which lightmap to use for each dimension.")
		@Comment("This changes the color tone of the lighting, when lighting is enabled.")
		public Map<String, ConstantLightMap> lightmaps = ValueMap.builder(ConstantLightMap.DAY)
			.put("minecraft:the_nether", ConstantLightMap.NETHER)
			.put("minecraft:the_end", ConstantLightMap.END)
			.build();

		@Comment("What Y co-ordinate to start the map at, in non-cave mode.")
		public Map<String, Integer> ceilings = ValueMap.builder(0)
			.put("minecraft:the_nether", 126)
			.build();
	}

	@Comment("Options for debugging issues with hoofprint.")
	public Debug debug = new Debug();

	public static class Debug implements Section {
		@Comment("Whether to log every time map area baking is started.")
		public boolean logBaking = false;

		@Comment("How many ticks to wait between baking regions.")
		@IntegerRange(min = 1, max = 200)
		public int ticksPerBake = 20;

		private enum KnockHint {
			TRY,
			MIDDLE,
			CLICKING,
			THE,
			MAP
		}

		@Comment("I wonder what this could be?")
		public KnockHint redHerring = KnockHint.TRY;
	}
}
