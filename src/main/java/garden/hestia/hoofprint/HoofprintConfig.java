package garden.hestia.hoofprint;

import folk.sisby.kaleido.api.WrappedConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.IntegerRange;
import folk.sisby.kaleido.lib.quiltconfig.api.values.ValueMap;
import garden.hestia.hoofprint.util.ConstantLightMap;

import java.util.Map;

public class HoofprintConfig extends WrappedConfig {
	public boolean biomeWater = true;
	public boolean topography = true;
	public boolean transparentWater = true;
	public boolean lighting = true;
	public boolean itemOutlines = true;
	public boolean showOffline = true;
	public boolean logMapBaking = false;
	public boolean renderBackground = true;
	@IntegerRange(min = 0, max = 15)
	public int ambientSkyLight = 8;
	@IntegerRange(min = 0, max = 16)
	public int blendRadius = 9;
	@IntegerRange(min = 1, max = 200)
	public int ticksPerRegion = 20;
	public Map<String, Integer> dimensionMaxYValues = ValueMap.builder(0)
		.put("minecraft:the_nether", 126)
		.build();
	public ConstantLightMap lightMap = ConstantLightMap.DAY;
	public Map<String, ConstantLightMap> dimensionLightMaps = ValueMap.builder(ConstantLightMap.DAY)
		.put("minecraft:the_nether", ConstantLightMap.NETHER)
		.put("minecraft:the_end", ConstantLightMap.END)
		.build();
	@Comment("Try clicking the scroll wheel!")
	public boolean redHerring = false;
}
