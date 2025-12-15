<!--suppress HtmlDeprecatedTag, XmlDeprecatedElement -->
<center>
<img alt="mod preview" src="https://cdn.modrinth.com/data/8O6iJpuJ/images/8be556ecaff911fe16eb86561f13d40c7fcc9433.png"/><br/>
A minimalist client-side world map.<br/>
<b>Requires <a href="https://modrinth.com/mod/surveyor">Surveyor Map Framework</a>.</b>
<b>Requires <a href="https://modrinth.com/mod/connector">Connector</a> and <a href="https://modrinth.com/mod/forgified-fabric-api">FFAPI</a> on forge.</b><br/>
</center>

---

**Hoofprint** is an enhanced-vanilla-style world map with:
- Fancy rendering features like biome colors, block lighting, and game-accurate transparent water
- Waypoints in three styles: vanilla-style banner (recolorable), item, and block
- Basic cave mapping, and the ability to view details about blocks on the map.
- Global player positions and map/waypoint sharing via `/surveyor share` when surveyor is on the server
- Automatic waypoint migration from existing Xaero's Minimap saves
- Support for Waystones ([Surveystones](https://modrinth.com/mod/surveystones)) and OPAC ([Surveyalot](https://modrinth.com/mod/surveyalot))
- Shared save format with [Antique Atlas 4](https://modrinth.com/mod/antique-atlas-4)

**Keybinds**:
- `M` to open the hoofprint world map screen
- `Left Click` to pan the map around
- `Mouse Scroll` to zoom the map in and out
- `Right Click` to edit the hovered waypoint, or place a new one
  - `TAB` to edit the waypoint style (accepts colors, item IDs, and #hex)
  - `ENTER` to save your changes, or `ESC` to discard them
- `DEL` to remove the hovered waypoint
- `H` to hide all map decorations
- `ALT` to view info about the block under the cursor (also shows waypoint names)
- `TAB` to view the cave layer

### Configuration

Hoofprint's configuration can be edited in `config/hoofprint.toml`, or in-game using [McQoy](https://modrinth.com/mod/mcqoy). This includes:
- Toggles for vanilla-style rendering (e.g. opaque water, no biome colors, no lighting)
- Lightmap selection, allowing you to render the map as if it's night.
- Dimension overrides for maximum Y (e.g. nether) and for lightmap.
- Handling for the world border.

### Troubleshooting / Suggestions

Hoofprint is a **clientside map frontend** for [Surveyor Map Framework](https://modrinth.com/mod/surveyor).<br/>
It renders surveyor save data in a vanilla-enhanced style, and allows editing surveyor waypoints.<br/>
Issues and suggestions regarding the screen, terrain, keybinds, and waypoint icons are [Hoofprint Issues](https://github.com/sisby-folk/antique-atlas/issues).<br/>
Issues and suggestions regarding map sharing, explored map area, and automatic markers are [Surveyor Issues](https://github.com/sisby-folk/surveyor).

## Afterword

Hoofprint is a collaboration between [Garden](https://modrinth.com/user/GardenSystem) (see [SurveyorSurveyor](https://github.com/HestiMae/surveyor-surveyor)) and [sisby](https://modrinth.com/user/sisby-folk), with guidance from many others.

The [surveyor ecosystem](https://modrinth.com/collection/fUFr3Mvj) is trying to make mapping more accessible! (and open-source)<br/>
Feel free to contribute improvements to [hoofprint](https://github.com/HestiMae/hoofprint/issues?q=is%3Aissue%20state%3Aopen%20(label%3Aenhancement%20OR%20label%3Abug)) or [surveyor](https://github.com/sisby-folk/surveyor/issues?q=is%3Aissue%20state%3Aopen%20(label%3Aenhancement%20OR%20label%3Abug)), or utilize surveyor it for your own mods!
