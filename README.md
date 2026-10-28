# Acceleration Mod

An original Minecraft **Java 26.2 / Fabric** implementation of the dragon-reward spear and soul ability described by the player. Requires **Java 25**, **Fabric Loader 0.19.5+**, and **Fabric API 0.161.0+26.2** or a compatible newer Fabric API for 26.2. Install on both the server and every client. It is a separate mod from Scyer; neither depends on the other.

## Earn the Acceleration Spear

Defeat the actual Ender Dragon managed by the End's dragon fight. After the death animation creates the exit portal and first dragon egg, a single **Acceleration Spear** drops directly above the egg. Respawned dragon victories also give one spear, above the exit fountain if the egg is absent. Unrelated summoned dragons do not count. A persistent dragon-UUID ledger prevents the same victory awarding duplicate spears. The dropped reward does not despawn on a timer and resists damage until collected.

The weapon has native diamond-spear jab, joust, reach, durability, animations and model behavior. It is blessed with every supported beneficial spear enchantment at maximum level, including conflicting damage enchantments, plus **Piercing IV** explicitly. Curses are excluded. Lunge III is included. Piercing is normally a projectile enchantment; this weapon's custom soul strike supplies actual armor/shield penetration. Item model definitions reference Minecraft's built-in assets; no third-party models/textures are redistributed.

## Acceleration from your inventory

Keep the spear anywhere in your inventory, including an ordinary inventory slot or offhand. You do not need to hold it. Sprinting charges acceleration over **five seconds**, up to **four times your otherwise normal sprint movement speed**. Stopping fades the bonus over 1.25 seconds; removing the last spear clears it immediately. Extra spears do not stack extra multipliers. The effect changes the real movement attribute rather than repeatedly giving a Speed potion.

## Leave your body behind

1. Hold the spear and **left-click to lunge**. Native Lunge requires at least seven food points, being out of water, not riding and not gliding.
2. A body with your skin and equipment stays at the **exact launch position**. You can turn around to see it, and other players can see it too.
3. Keep playing with your **normal hand, held items, inventory and interactions**. Flight is enabled without changing your game mode. You can move back to look at your body. This version uses the ordinary moving player model plus the stationary body; it does not provide a translucent custom avatar.
4. **Right-click a nearby player or mob with the spear** to deliver one instant lethal hit and return to normal beside that target. A held Totem of Undying pops; ordinary targets without one die through armor and Protection. This uses normal entity-interaction reach, not a 64-block ray or remote target selection. Clicking empty space does not strike anyone.
5. **Lunge again** to bring your body to where you finish the lunge and return to normal without the special hit.

There is **no projection timer, no distance tether, no sneak-to-return and no extra ability cooldown**. Sneaking, opening inventory and normal left-click actions retain their vanilla behavior; a native spear lunge while projected brings the body to you. There are no soul particles, “soul mode” messages or status labels. Flight uses ordinary player flight with collision, not Spectator mode or wall phasing. Your moving player can take damage normally while the body is behind you.

The second lunge completes recall after movement settles (three nearly stationary ticks after at least four ticks), or after 40 ticks if you keep moving; it uses your actual position then, not the launch position. This is movement completion, not a duration limit on projection.

Ending projection removes the stationary body and restores your original flight settings and game mode. A single projection delivers only one special strike. The body can be damaged: a hit recalls its owner and forwards incoming damage once. Body equipment copies never drop. Creative/spectator targets, invulnerable entities and body mannequins are excluded from special strikes. Disabled PvP and team friendly-fire rules are respected. A special strike needs a clear, in-bounds landing space beside the victim. Other mods' special immunities may prevent a kill. Losing the last spear, changing dimensions or dying ends projection for cleanup.

Logout and normal server shutdown recall projected players. The return position, original game mode and abilities are also stored for recovery on reconnect after an interrupted save/session. Orphaned saved body mannequins are removed when their chunks load. Do not change game modes manually during projection; use the return control or command.

## Commands

With cheats/operator permissions:

```mcfunction
/acceleration give
/acceleration project
```

`give` grants a fully enchanted spear for testing. `project` toggles projection for testing while holding the spear. The optional recovery command returns to the original body:

```mcfunction
/acceleration return
```

The registered item is `acceleration:acceleration_spear`. Ordinary `/give` and Creative-tab spears acquire their blessing on their first server inventory tick. There is no crafting recipe.

## Install

Place `acceleration-1.1.1+mc26.2.jar` and Fabric API for 26.2 in the correct instance's `mods` folder. Launch **Minecraft Java 26.2 with Fabric**. Install the same mod version on all participating clients and the server. Scyer is optional and remains a separate JAR.

## Development and validation

Use `/workspace/Acceleration`; do not alter `/workspace/Syrer` when developing this separate mod. Cloud tasks are already isolated; do not create a Git worktree unless explicitly requested. Source `/workspace/scyer-env.sh` (or `/workspace/acceleration-env.sh`) for the retained Java 25 toolchain and Gradle cache. The Gradle wrapper pins 9.7.1 with checksum verification, Loom 1.18.4 and the listed Minecraft/Fabric dependencies.

```sh
./gradlew build
./gradlew test
./gradlew runGameTest
./gradlew runClient # Requires a graphical desktop
./gradlew runServer # Review Minecraft's EULA before accepting run/eula.txt
```

Three rules tests validate acceleration ramp, decay and speed limits. Twelve actual server GameTests cover the real native lunge, exact body anchor and skin, Survival hand/inventory and flight preservation, a right-click entity callback consuming exactly one totem, armor penetration, body damage forwarding, no expiry after the former ten-second limit, sneaking without recall, a second native lunge bringing the body to the post-lunge position, no distance tether, missing-spear cleanup, reward position/deduplication, persistence, PvP protection and the real dragon-fight reward callback. Minecraft's always-pass test also runs. Test-only code is not bundled.

Server and client sources compile. Graphical first-person hand rendering, looking back at the body, mouse controls and a full multiplayer session still need manual gameplay testing. The server tests verify that projection never changes to Spectator, retains the main-hand item and inventory, and restores original abilities.

This is an original implementation inspired by the requested description, not an official ManePear/Interval SMP release. No video assets or third-party source code are included. See LICENSE.
