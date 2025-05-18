package garden.hestia.hoofprint;

import folk.sisby.kaleido.api.WrappedConfig;
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
	public boolean renderBorder = true;
	public boolean renderOutsideBorder = true;
	public boolean logMapBaking = false;
	public Map<String, Integer> dimensionMaxYValues = ValueMap.builder(0)
		.put("minecraft:the_nether", 126)
		.build();
	public ConstantLightMap lightMap = ConstantLightMap.DAY;
	public Map<String, ConstantLightMap> dimensionLightMaps = ValueMap.builder(ConstantLightMap.DAY)
		.put("minecraft:the_nether", ConstantLightMap.NETHER)
		.put("minecraft:the_end", ConstantLightMap.END)
		.build();
}
