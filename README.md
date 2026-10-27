# Acceleration Mod

An original Minecraft **Java 26.2 / Fabric** implementation of the dragon-reward spear and soul ability described by the player. Requires **Java 25**, **Fabric Loader 0.19.5+**, and **Fabric API 0.161.0+26.2** or a compatible newer Fabric API for 26.2. Install on both the server and every client. It is a separate mod from Scyer; neither depends on the other.

## Earn the Acceleration Spear

Defeat the actual Ender Dragon managed by the End's dragon fight. After the death animation creates the exit portal and first dragon egg, a single **Acceleration Spear** drops directly above the egg. Respawned dragon victories also give one spear, above the exit fountain if the egg is absent. Unrelated summoned dragons do not count. A persistent dragon-UUID ledger prevents the same victory awarding duplicate spears. The dropped reward does not despawn on a timer and resists damage until collected.

The weapon has native netherite-spear jab, joust, reach, durability, animations and model behavior. It is blessed with every supported beneficial spear enchantment at maximum level, including conflicting damage enchantments, plus **Piercing IV** explicitly. Curses are excluded. Lunge III is included. Piercing is normally a projectile enchantment; this weapon's custom soul strike supplies actual armor/shield penetration. Item model definitions reference Minecraft's built-in assets; no third-party models/textures are redistributed.

## Acceleration from your inventory

Keep the spear anywhere in your inventory, including an ordinary inventory slot or offhand. You do not need to hold it. Sprinting charges acceleration over **five seconds**, up to **four times your otherwise normal sprint movement speed**. Stopping fades the bonus over 1.25 seconds; removing the last spear clears it immediately. Extra spears do not stack extra multipliers. The effect changes the real movement attribute rather than repeatedly giving a Speed potion.

## Lunge into soul mode

1. Hold the spear and **left-click to jab/lunge**. Native Lunge requires at least seven food points, being out of water, not riding and not gliding.
2. Your body stays at the **exact position where you began the lunge**, wearing copies of your equipment and using your player skin. It does not follow the soul.
3. You control the flying soul for up to **10 seconds**. Soul flight uses Minecraft's spectator movement, with blue soul particles and an on-screen status message. The hand/spear is not rendered during spectator projection.
4. Aim at a living target within **64 blocks** and **left- or right-click**. The server chooses the target from your actual aim, materializes you beside them, then applies one lethal spear strike. A normal held Totem of Undying activates; without one, ordinary targets die even through armor, Protection, Resistance and shields.
5. **Sneak/Shift** returns to your body without attacking. Timeout, moving more than **128 blocks** from your body or losing the spear also returns you safely.

A successful strike commits your real position beside the target rather than returning you to the old body. Ending projection removes the body, restores your previous game mode and flight/invulnerability settings, and starts a **30-second soul cooldown**. Normal spear attacks/lunges remain usable while the soul ability is cooling down. A single projection can deliver only one soul strike, preventing a repeated packet from consuming multiple totems.

The body can be damaged: a hit immediately recalls its owner and applies the incoming damage to their real health/armor exactly once. Equipment on the temporary body is never dropped or duplicated. Creative/spectator targets, invulnerable entities and your own body are excluded from soul strikes. Disabled PvP and team friendly-fire restrictions are respected. The soul can pass through walls and its aimed strike is not limited by intervening blocks, but it must find an empty, in-bounds landing space beside the victim. Special damage immunities supplied by other mods may still prevent a kill.

Logout and normal server shutdown recall projected players. The return position, original game mode and abilities are also stored for recovery on reconnect after an interrupted save/session. Orphaned saved body mannequins are removed when their chunks load. Do not change game modes manually during projection; use the return control or command.

## Commands

With cheats/operator permissions:

```mcfunction
/acceleration give
/acceleration soul
```

`give` grants a fully enchanted spear for testing. `soul` activates projection from your current position while holding the spear and obeys the normal cooldown. Players can always recall their own active soul:

```mcfunction
/acceleration return
```

The registered item is `acceleration:acceleration_spear`. Ordinary `/give` and Creative-tab spears acquire their blessing on their first server inventory tick. There is no crafting recipe.

## Install

Place `acceleration-1.0.0+mc26.2.jar` and Fabric API for 26.2 in the correct instance's `mods` folder. Launch **Minecraft Java 26.2 with Fabric**. Install the same mod version on all participating clients and the server. Scyer is optional and remains a separate JAR.

## Development and validation

Use `/workspace/Acceleration`; do not alter `/workspace/Syrer` when developing this separate mod. Cloud tasks are already isolated; do not create a Git worktree unless explicitly requested. Source `/workspace/scyer-env.sh` (or `/workspace/acceleration-env.sh`) for the retained Java 25 toolchain and Gradle cache. The Gradle wrapper pins 9.7.1 with checksum verification, Loom 1.18.4 and the listed Minecraft/Fabric dependencies.

```sh
./gradlew build
./gradlew test
./gradlew runGameTest
./gradlew runClient # Requires a graphical desktop
./gradlew runServer # Review Minecraft's EULA before accepting run/eula.txt
```

Three rules tests validate ramp, decay, limits and time/range constants. Twelve actual server GameTests exercise native spear components and max enchantments; acceleration from an ordinary inventory slot; the actual native lunge hook and exact body anchor/skin profile; server-aimed teleport strike consuming exactly one totem; lethal hits through real Protection IV netherite; one-time body damage forwarding; timeout and mode/ability restoration; range/missing-spear recall; reward position and persistence; serialized recovery; creative/PvP protections; and the real `EnderDragonFight.setDragonKilled` callback after portal/egg generation. Minecraft's built-in always-pass case also runs. Tests use real ServerPlayer instances rather than mocks with fixed game-mode overrides. The test mod is not bundled in the release.

Both server and client sources compile. Client input-hook target signatures and resource references are checked, but graphical controls, skin rendering, first-person camera behavior and Mac gameplay still require manual testing. A full multiplayer play session has not been run here.

This is an original implementation inspired by the requested description, not an official ManePear/Interval SMP release. No video assets or third-party source code are included. See LICENSE.
