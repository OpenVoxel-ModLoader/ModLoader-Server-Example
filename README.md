# OpenVoxel ModLoader — server plugin example: Hunger Games

A minimal mini-game plugin for the OpenVoxel dedicated server, using the official plugin API
(`net.openvoxel.server.plugin.*`). Runs with [ModLoader-Server](https://github.com/OpenVoxel-ModLoader/ModLoader-Server)
and works with vanilla clients.

| Command | Effect |
|---------|--------|
| `/hg start [radius]` | Spreads every online player in a circle around the starter, heals them, counts down, 30 s grace period, then last player standing wins. |
| `/hg stop` | Cancels the current game. |
| `/hg status` | Shows the state and the number of tributes alive. |

## Install

Download `HungerGames.jar` from Releases into `ModLoader-Server/mods/` (it is copied to `plugins/` on start).

## Build

```
build.bat   /   ./build.sh      # -> dist/HungerGames.jar
```

Requires JDK 11+ and the server JAR (`OPENVOXEL_SERVER_JAR`, or `../ModLoader-Server/server/openvoxel-server.jar`).

## Layout

```
src/.../HungerGames.java    extends Plugin implements Listener
resources/plugin.yml        name, main class, version
```

Events used: `PlayerDeathEvent`, `PlayerQuitEvent`, `PlayerJoinEvent`; scheduler via `VoxelServer.getScheduler()`.
