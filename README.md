# NeoPlayer Locator Plus

NeoPlayer Locator Plus is a NeoForge 1.21.1 port of
[Player Locator Plus](https://github.com/timas130/PlayerLocatorPlus).
It shows other players as direction markers near the experience bar, similar to a
compass-style player locator.

This fork targets:

- Minecraft 1.21.1
- NeoForge 21.1.233 or newer in the 1.21.1 line
- Java 21

It does not require Fabric API, Fabric Language Kotlin, Cloth Config, or Mod
Menu. The old Fabric/Kotlin sources are left in the repository for upstream
reference, but the NeoForge build uses the Java sources under `src/main/java`.

## Features

- Server sends relative player location updates to clients with the mod.
- Client renders player direction markers on the HUD.
- Optional distance-based marker fade.
- Optional height arrows for players above or below you.
- Players can hide by sneaking, wearing configured hiding equipment, wearing mob
  heads, or being invisible.
- Color modes: UUID, team color, custom player colors, or one constant color.
- `/plp reload`, `/plp random`, and `/plp color` commands.

The Minecraft 1.21.11 vanilla waypoint integration from upstream is not included
in this 1.21.1 port because that API is not available in Minecraft 1.21.1.

## Configuration

The config is created on first launch at:

```text
config/player-locator-plus.properties
```

Custom player colors are stored at:

```text
config/player-locator-plus-player-colors.properties
```

Server and client options are stored in the same properties file. When
`sendServerConfig=true`, the server sends its config to connecting clients that
accept server config.

Common options include:

- `enabled`: enable or disable server-side location updates.
- `visible`: show the locator bar on the client.
- `visibleEmpty`: show the locator even when no markers are visible.
- `acceptServerConfig`: allow the server to override client settings.
- `sendServerConfig`: send server config to clients.
- `sendDistance`: send distance information along with direction information.
- `maxDistance`: maximum visible distance; `0` means unlimited.
- `directionPrecision`: number of direction segments used for quantized updates.
- `ticksBetweenUpdates`: server ticks between location update packets.
- `sneakingHides`: hide sneaking players.
- `pumpkinHides`: hide players wearing a carved pumpkin or tagged hiding gear.
- `mobHeadsHide`: hide players wearing mob/player heads.
- `invisibilityHides`: hide invisible players.
- `colorMode`: `UUID`, `TEAM_COLOR`, `CUSTOM`, or `CONSTANT`.
- `constantColor`: RGB color used by `CONSTANT` mode.
- `fadeMarkers`, `fadeStart`, `fadeEnd`, `fadeEndOpacity`: marker fade settings.
- `showHeight`: show up/down arrows for height difference.
- `alwaysShowHeads`, `showHeadsOnTab`, `showNamesOnTab`: HUD label/head display.

Hiding equipment can be changed with a datapack by editing the item tag:

```text
data/player-locator-plus/tags/item/hiding_equipment.json
```

## Building

Use Java 21, then run:

```powershell
.\gradlew.bat build --no-daemon
```

The built mod jar is written to:

```text
build/libs/neoplayer-locator-plus-2.3.0-neoforge.1+1.21.1.jar
```

## Validation

This fork has been built with Java 21 and validated on a dedicated NeoForge
21.1.233 server using Collin's current NeoForge 1.21.1 Modrinth modpack. The
server loaded the mod and reached `Done`.

## License

Player Locator Plus is licensed under the GNU General Public License Version 3
or later.
