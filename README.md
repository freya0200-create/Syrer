# Acceleration Mod

An original Minecraft **Java 26.2 / Fabric** implementation of the dragon-reward spear and soul ability described by the player. Requires **Java 25**, **Fabric Loader 0.19.5+**, and **Fabric API 0.161.0+26.2** or a compatible newer Fabric API for 26.2. Install on both the server and every client. It is a separate mod from Scyer; neither depends on the other.

## Earn the Ghost Spear

Defeat the actual Ender Dragon managed by the End's dragon fight. After the death animation creates the exit portal and first dragon egg, a single **Ghost Spear** drops directly above the egg. Respawned dragon victories also give one spear, above the exit fountain if the egg is absent. Unrelated summoned dragons do not count. A persistent dragon-UUID ledger prevents the same victory awarding duplicate spears. The dropped reward does not despawn on a timer and resists damage until collected.

The weapon has native diamond-spear jab, joust, reach, durability, animations and model behavior. It is blessed with every supported beneficial spear enchantment at maximum level, including conflicting damage enchantments, plus **Piercing IV** explicitly. Curses are excluded. Lunge III is included. Piercing is normally a projectile enchantment; this weapon's custom soul strike supplies actual armor/shield penetration. Item model definitions reference Minecraft's built-in assets; no third-party models/textures are redistributed.

## Acceleration from your inventory

Keep the spear anywhere in your inventory, including an ordinary inventory slot or offhand. You do not need to hold it. Sprinting gradually increases acceleration for as long as you keep running: **+5% after five seconds, +13% after thirteen seconds**, gaining 1% per second, then continuing at the same rate. There is no mod-defined gameplay speed cap (Minecraft still has engine attribute/physics bounds). Stopping rapidly fades the bonus; removing the last spear clears it immediately. Extra spears do not stack extra multipliers. The effect changes the real movement attribute rather than repeatedly giving a Speed potion.

## Leave your body behind

1. Hold the spear and **left-click to lunge**. Native Lunge requires at least seven food points, being out of water, not riding and not gliding.
2. A linked player-shaped body with your skin and equipment stays at the **exact launch position**. You can turn around to see it, and other players can see it too.
3. Keep playing with your **normal hand, held items, inventory and interactions**. Projection grants no flight, Creative abilities or game-mode change. You can move back to look at your body. This version uses the ordinary moving player model plus the stationary body; it does not provide a translucent custom avatar. The body is a dedicated Avatar entity rendered with Minecraft's player renderer, not a mannequin or a separate logged-in player. It has no NPC AI, dialogue, inventory or independent health; your actual player owns health, effects, armor and equipment.
4. **Right-click a nearby player or mob with the spear** to deliver one instant lethal hit and return to normal beside that target. A held Totem of Undying pops; ordinary targets without one die through armor and Protection. This uses normal entity-interaction reach, not a 64-block ray or remote target selection. Clicking empty space does not strike anyone.
5. **Eating is allowed; lunging is disabled while you are a ghost.** Attempting another lunge does not grant an impulse, reunite your body, teleport you or zero your existing movement. Normal movement and sprinting remain available.

There is **no projection timer, no distance tether, no sneak-to-return and no extra ability cooldown**. Sneaking, opening inventory and normal left-click actions retain their vanilla behavior; attempted spear lunges are blocked while projected without changing your existing momentum. There are no soul particles, “soul mode” messages or status labels. Contact with players and entities causes no pushing in either direction while projected, using synced state on server and clients. Ordinary block collision and gravity remain; this does not enable wall phasing. Hunger and exhaustion do not drain during projection; ordinary hunger-based natural regeneration is paused too. Eating and using wind charges still work and do not end projection. Wind-charge wind jumps retain their normal knockback. Throwing an ender pearl also preserves projection until its successful native teleport; then you reunite at the pearl landing point. Normal pearl damage applies after materializing. Building, using blocks and using non-food items other than the spear, ender pearls or wind charges reunite the body at your current position before the action proceeds, preserving momentum. Lunging is disabled while projected. The ghost is ignored by mob targeting (including Wardens) and cannot take direct damage; attacks on the body still damage your real health. Ordinary attacks, trading, riding, feeding and other entity interactions are blocked during projection, except that you may left-click your own linked body to hurt your shared real health without recall. Other ghosts’ bodies do not gain this exception. The Ghost Spear’s special right-click hit remains the explicit exception that ends projection and reunites you beside the victim.

Ending projection removes the stationary body and restores your original flight settings while preserving your current game mode. A single projection delivers only one special strike. The body can be damaged: a hit forwards incoming damage once to your real player without recall, teleportation, game-mode change or a return message. The real player's armor, invulnerability frames, totem and mortality apply; a lethal hit kills the real owner. Healing the player also updates the linked body. Body equipment copies never drop, and track your current real equipment. Creative/spectator targets, invulnerable entities and linked body entities are excluded from special strikes. Disabled PvP and team friendly-fire rules are respected. A special strike needs a clear, in-bounds landing space beside the victim. Other mods' special immunities may prevent a kill. Losing the last spear, changing dimensions or dying ends projection for cleanup.

Logout and normal server shutdown recall projected players. The return position and original abilities are also stored for recovery on reconnect after an interrupted save/session. Orphaned saved linked body entities are removed when their chunks load. An administrator changing the player's game mode is respected; recovery never restores a stale Creative mode over current Survival mode.

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

Place `acceleration-1.2.2+mc26.2.jar` and Fabric API for 26.2 in the correct instance's `mods` folder. Launch **Minecraft Java 26.2 with Fabric**. Install the same mod version on all participating clients and the server. Scyer is optional and remains a separate JAR.

## Development and validation

Use `/workspace/Acceleration`; do not alter `/workspace/Syrer` when developing this separate mod. Cloud tasks are already isolated; do not create a Git worktree unless explicitly requested. Source `/workspace/scyer-env.sh` (or `/workspace/acceleration-env.sh`) for the retained Java 25 toolchain and Gradle cache. The Gradle wrapper pins 9.7.1 with checksum verification, Loom 1.18.4 and the listed Minecraft/Fabric dependencies.

```sh
./gradlew build
./gradlew test
./gradlew runGameTest
./gradlew runClient # Requires a graphical desktop
./gradlew runServer # Review Minecraft's EULA before accepting run/eula.txt
```

Three rules tests validate the gentler acceleration ramp, decay and continuing uncapped growth. Nineteen actual server GameTests cover the real native lunge, exact body anchor and skin, Survival hand/inventory preservation, no flight, Warden targeting/body vulnerability, blocked ordinary entity interactions, non-pushing contact physics and normal collision restoration, real wind-charge launches, wind-jump knockback and projection persistence, the real native pearl landing callback, frozen hunger, eating without recall, materializing block/item actions without momentum loss, a right-click entity callback consuming exactly one totem, armor penetration, shared body damage without recall, real vanilla self-punches sharing damage and preserving Survival, real totem use and lethal body hits, stale-Creative snapshot protection, no expiry after the former ten-second limit, sneaking without recall, blocked lunges preserving projection, existing momentum and the stationary body, no distance tether, missing-spear cleanup, reward position/deduplication, persistence, PvP protection and the real dragon-fight reward callback. Minecraft's always-pass test also runs. Test-only code is not bundled.

Server and client sources compile. Graphical first-person hand rendering, looking back at the body, mouse controls and a full multiplayer session still need manual gameplay testing. The server tests verify that projection never changes to Spectator, retains the main-hand item and inventory, and restores original abilities.

This is an original implementation inspired by the requested description, not an official ManePear/Interval SMP release. No video assets or third-party source code are included. See LICENSE.
