# Acceleration Mod

An original Minecraft **Java 26.2 / Fabric** implementation of the dragon-reward spear and soul ability described by the player. Requires **Java 25**, **Fabric Loader 0.19.5+**, and **Fabric API 0.161.0+26.2** or a compatible newer Fabric API for 26.2. Install on both the server and every client. It is a separate mod from Scyer; neither depends on the other.

## Earn the Ghost Spear

Defeat the actual Ender Dragon managed by the End's dragon fight. After the death animation creates the exit portal and first dragon egg, a single **Ghost Spear** drops directly above the egg. Respawned dragon victories also give one spear, above the exit fountain if the egg is absent. Unrelated summoned dragons do not count. A persistent dragon-UUID ledger prevents the same victory awarding duplicate spears. The dropped reward does not despawn on a timer and resists damage until collected.

The weapon has native diamond-spear jab, joust, reach, durability, animations and model behavior. It is blessed with every supported beneficial spear enchantment at maximum level, including conflicting damage enchantments, plus **Piercing IV** explicitly. Curses are excluded. Lunge III is included. Piercing is normally a projectile enchantment; this weapon's custom soul strike supplies actual armor/shield penetration. Item model definitions reference Minecraft's built-in assets; no third-party models/textures are redistributed.

## Acceleration from your inventory

Keep the spear anywhere in your inventory, including an ordinary inventory slot or offhand. You do not need to hold it. Press **I** to enable acceleration (press again to switch it off and clear the bonus). It starts disabled each session. Rebind **Toggle Acceleration** under **Options → Controls → Key Binds → Acceleration Mod**. While enabled, sprinting gradually increases acceleration for as long as you keep running: **+60% after five seconds, +120% after ten seconds**, gaining 12% per second, then continuing at the same rate. There is no mod-defined gameplay speed cap (Minecraft still has engine attribute/physics bounds). Stopping rapidly fades the bonus; removing the last spear clears it immediately. Extra spears do not stack extra multipliers. The effect changes the real movement attribute rather than repeatedly giving a Speed potion.

## Leave your body behind

1. Hold the spear and **left-click to lunge** to enter ghost mode. **I toggles the acceleration speed boost only; it does not activate ghost mode.** Spear lunges and attacks are hunger-free for all spear materials; ordinary hunger outside these actions remains vanilla. Native Lunge still requires at least seven food points, being out of water, not riding and not gliding.
2. A linked player-shaped body with your skin and equipment stays at the **exact launch position**. You can turn around to see it, and other players can see it too.
3. Keep playing with your **normal hand, held items, inventory and interactions**. Projection grants no flight, Creative abilities or game-mode change. You can move back to look at your body. Your ghost retains its normal hand, inventory and optional third-person self-view, but other clients do not render your ghost, its equipment or nameplate. Your linked body remains visible. Every participating client must install this version for the visibility hook. The body is a dedicated Avatar entity rendered with Minecraft's player renderer, not a mannequin or a separate logged-in player. It has no NPC AI, dialogue, inventory or independent health; your actual player owns health, effects, armor and equipment.
4. **Right-click a nearby player, mob or damageable entity (including dragon hitboxes, armor stands and minecarts) with the spear** to deliver one instant lethal hit and return to normal beside that target. Damage lands first, while you remain at your pre-hit position; only a successful hit then teleports and reunites you beside the target. Merely holding/right-clicking the spear while looking at a block does not recall early. The hit uses the largest finite damage value (effectively infinite in gameplay) and bypasses armor, shields, Protection and Resistance. A held Totem of Undying still pops once, as requested. Without a totem, ordinary targets die; Wither spawn protection is also removed for this hit. Creative/spectator targets, server PvP restrictions and entities explicitly marked invulnerable remain protected. The first target uses normal entity-interaction reach, or native spear attack reach for jousting. Collateral piercing extends six blocks along the thrust, stopping at solid blocks. Both paths always apply the same maximum ghost-strike damage, rather than ordinary velocity-scaled spear damage. There is no 64-block ray or remote target selection. Clicking empty space does not strike anyone.
5. **Lunge while you are a ghost to reunite your body after moving forward.** The native lunge impulse runs first. Once movement settles, your body rejoins at the position you reached and you return to normal, preserving your momentum. Eating and wind charges keep you in ghost mode.

There is **no projection timer, no distance tether, no sneak-to-return and no extra ability cooldown**. Sneaking, opening inventory and normal left-click actions retain their vanilla behavior; empty spear lunges keep you projected; a successful spear jab reunites you after its forward movement, preserving momentum. There are no soul particles, “soul mode” messages or status labels. Contact with players and entities causes no pushing in either direction while projected, using synced state on server and clients. Ordinary block collision and gravity remain; this does not enable wall phasing. Hunger and exhaustion do not drain during projection; ordinary hunger-based natural regeneration is paused too. Eating and using wind charges still work and do not end projection. Self-owned wind-charge blasts that reach your ghost add their normal knockback plus 0.5 upward velocity to your existing momentum. Repeated self blasts therefore stack upward launches; the adjusted impulse is used by both server physics and the client explosion packet. Other players’ charges and other explosions cannot push the ghost; normal-mode knockback remains vanilla. Blasts still need to reach you within normal range; throwing a charge that misses does not launch you. Throwing an ender pearl also preserves projection until its successful native teleport; then you reunite at the pearl landing point. Normal pearl damage applies after materializing. Building, using blocks and using non-food items other than the spear, ender pearls or wind charges reunite the body at your current position before the action proceeds, preserving momentum. After a successful spear jab, lunge recall waits at least four server ticks and until you have moved and then settled for three ticks; continuous movement or a blocked lunge resolves after twenty ticks, using the actual current position and retaining velocity. This completion delay does not limit how long you can remain a ghost. The ghost is ignored by mob targeting (including Wardens) and cannot take direct damage; attacks on the body still damage your real health. A successful ordinary left-click hit with your hands, sword or other weapon brings your body to your current position and returns you to normal, respecting normal PvP rules. A Ghost Spear jab that lunges is the exception: its damage lands first, then its forward lunge completes before the body reunites at the reached position. It preserves momentum and does not snap you back to the old body position. Missed or rejected hits do not trigger this recall. Native spear hits cannot immediately restart projection from the same attack’s post-hit Lunge effect. Opponents cannot select, damage, push or right-click your ghost; mobs continue to ignore it. Trading, riding and feeding interactions remain blocked while projected. Hitting your own linked body still recalls you after applying shared damage; right-clicking it with the Ghost Spear recalls you without the lethal hit. Other players hitting your body still damage you without recall. The Ghost Spear’s special right-click hit remains the explicit exception that ends projection and reunites you beside the victim.

Ending projection removes the stationary body and restores your original flight settings while preserving your current game mode. A single projection delivers only one special strike. The body can be damaged: a hit from anyone else forwards incoming damage once without recall, teleportation, game-mode change or a return message. Only your own direct melee hit recalls you to the body after applying its damage. Self-sourced wind-charge effects do not count as a melee recall. The real player's armor, invulnerability frames, totem and mortality apply; a lethal hit kills the real owner. Healing the player also updates the linked body. Body equipment copies never drop, and track your current real equipment. Creative/spectator targets, invulnerable entities and linked body entities are excluded from special strikes. Disabled PvP and team friendly-fire rules are respected. A special strike needs a clear, in-bounds landing space beside the victim. Other mods' special immunities may prevent a kill. Losing the last spear, changing dimensions or dying ends projection for cleanup.

Logout and normal server shutdown recall projected players. The return position and original abilities are also stored for recovery on reconnect after an interrupted save/session. Orphaned saved linked body entities are removed when their chunks load. An administrator changing the player's game mode is respected; recovery never restores a stale Creative mode over current Survival mode.

## Commands

With cheats/operator permissions:

```mcfunction
/acceleration give
/acceleration project
```

`give` grants a fully enchanted spear for testing. `project` begins projection for testing while holding the spear; during projection it requests the normal delayed lunge reunion. The optional recovery command returns to the original body:

```mcfunction
/acceleration return
```

The registered item is `acceleration:acceleration_spear`. Ordinary `/give` and Creative-tab spears acquire their blessing on their first server inventory tick. There is no crafting recipe.

## Install

Place `acceleration-1.5.3+mc26.2.jar` and Fabric API for 26.2 in the correct instance's `mods` folder. Launch **Minecraft Java 26.2 with Fabric**. Install the same mod version on all participating clients and the server. Scyer is optional and remains a separate JAR.

## Development and validation

Use `/workspace/Acceleration`; do not alter `/workspace/Syrer` when developing this separate mod. Cloud tasks are already isolated; do not create a Git worktree unless explicitly requested. Source `/workspace/scyer-env.sh` (or `/workspace/acceleration-env.sh`) for the retained Java 25 toolchain and Gradle cache. The Gradle wrapper pins 9.7.1 with checksum verification, Loom 1.18.4 and the listed Minecraft/Fabric dependencies.

```sh
./gradlew build
./gradlew test
./gradlew runGameTest
./gradlew runClient # Requires a graphical desktop
./gradlew runServer # Review Minecraft's EULA before accepting run/eula.txt
```

Three rules tests validate the gentler acceleration ramp, decay and continuing uncapped growth. Thirty-two actual server GameTests cover native right-click jousting with tiny base damage promoted to maximum damage and one totem activation, I-key speed toggling without ghost activation, inventory-only speed boost and disable behavior, actual End reentry creating the upgraded fight, normal first dragon, duplicate prevention, three-shot volley timing, hit-before-teleport ordering, all-material spear lunges and successful stabs without hunger cost, repeated hunger-free native lunges in both modes with ordinary body hunger preserved, the real native lunge, exact body anchor and skin, Survival hand/inventory preservation, no flight, Warden targeting/body vulnerability, successful native melee hits reuniting at the current position, spear jab recall without immediate reprojection, incoming interaction rejection, non-selectability and observer visibility policy, non-pushing contact physics and normal collision restoration, real wind-charge launches, repeated native self-explosion stacking, wind-jump knockback and projection persistence, maximum-damage armor-stand/minecart strikes and Wither spawn protection, the real native pearl landing callback, frozen hunger, eating without recall, materializing block/item actions without momentum loss, a right-click entity callback consuming exactly one totem, armor penetration, shared body damage without recall, owner-only self-punch recall versus another player’s non-recalling hit, self right-click recall, dragon hitbox lethal right-clicks, real totem use and lethal body hits, stale-Creative snapshot protection, no expiry after the former ten-second limit, sneaking without recall, native ghost lunges applying an impulse before recall at the post-lunge position while preserving momentum, no distance tether, missing-spear cleanup, reward position/deduplication, persistence, PvP protection and the real dragon-fight reward callback. Minecraft's always-pass test also runs. Test-only code is not bundled.

Wind-charge and pearl hooks modify computed expression values instead of replacing vanilla calls with Redirect injectors, reducing conflicts with other mods such as Carpet. This workspace could not download Carpet for a full integration run, so compatibility with the user’s full modpack is not verified.

Server and client sources compile. Graphical first-person hand rendering, looking back at the body, mouse controls and a full multiplayer session still need manual gameplay testing. The server tests verify that projection never changes to Spectator, retains the main-hand item and inventory, and restores original abilities.

This is an original implementation inspired by the requested description, not an official ManePear/Interval SMP release. No video assets or third-party source code are included. See LICENSE.

The supplied 13-second reference clip was reviewed visually: disappearance from other players’ views, a rapid attack with totem particles, and wind-charge launches. This version recreates those visible behaviors with the specified body and controls; it does not claim to reproduce the original video’s underlying glitch.

## Ascended Ender Dragon

The first fight stays vanilla (200 HP). After that dragon is defeated, leaving and re-entering the End automatically starts the next fight with an **Ascended Ender Dragon: 1,000 HP / 500 hearts**. This is shared world progression, not a personal visit counter; returning while the original dragon is still alive does not replace it. Native crystal-respawned dragons after the first defeat are upgraded too. There is only one active fight; further returning players cannot duplicate the boss. Normal portal, boss-bar, save and dragon reward handling are retained. The upgraded dragon uses the normal dragon model and appearance.

Each native strafe fireball becomes a volley of **three ender fireballs**, with ten ticks (half a second) between shots. Extra shots do not recursively create more volleys, and a dead/unloaded dragon cancels its queued shots. Reloading the dragon preserves its current damaged health.

Ghost Spear right-click strikes now pierce multiple entities in a narrow forward line up to six blocks long. Blocks stop collateral piercing; entities off to the side are unaffected. Every target is damaged once before a single body reunion, so one held totem still saves each victim. This applies to both quick clicks and native jousting hits.

1.4.2: Empty ghost lunges keep projection active and the original body in place. Successful jab hits still reunite after movement. Right-click piercing extends six blocks along the thrust, stops at solid blocks, and keeps level through aligned tall mobs; the initial target still requires normal interaction/joust reach.

1.4.3: Ghost Spear attack speed is 2 attacks/second (0.5-second jab/lunge recharge, previously 1.05 seconds). Diamond base damage is unchanged.

1.4.4: Mace hit reunion preserves smash fall-distance context until native item/enchantment effects execute, allowing Wind Burst to launch normally after body reunion.

1.4.5: Hostile mobs acquire the visible linked body as a combat target within their normal follow range and line of sight while ignoring the ghost. Hits on the body still damage the owner without recall. Ghost-owned wind charges do not push other entities, including projectiles; self-launch remains enabled.

1.4.6: While projected, your wind charges cannot push any other entity or projectile. Self wind-charge launches still stack. Normal body-mode wind charges keep vanilla knockback. Hostile mobs still hunt the linked body.

1.4.7: Ghost Spear item name is red, including existing spears when in inventory. Anvil custom names are preserved.

1.5.0: Press 6 to toggle phasing while carrying the Ghost Spear. Press 7 to cycle Wall Phasing (sideways), Underneath Blocks Phasing (down through floors), and On Top of Blocks Phasing (up through ceilings). Mode changes work while on or off and do not change ghost/body mode. No flight is granted. Turning off finds nearby open space or your last clear position. Both keys are rebindable in Controls; their vanilla hotbar slot selection also remains.

1.5.3 is based on 1.5.0: 6 toggles continuous phasing and 7 cycles modes. Ghost right-click contacts now use the special strike even when vanilla marks them knockback-only. Cramped landing space no longer cancels damage: the body reunites at the current attack position instead. Totems and PvP protection still apply.
