# Scyer

A Minecraft **Java Edition 26.2 / Fabric** horror boss mod, version **1.6.1**. Requires **Java 25**, **Fabric Loader 0.19.5+** and **Fabric API 0.161.0+26.2** (or a compatible newer Fabric API for 26.2).

## Relentless tracking

Scyer acquires survival/adventure players on its first AI tick, within a spherical **100-block radius**. Its supernatural tracker detects silent, motionless and crouched targets through walls, with no visible radar added to its eyes. Players always take priority over mobs, including mobs that hit it. When no eligible player is nearby, it hunts the nearest living creature, including undead, or retaliates against a nearby attacker. Creative/spectator players, invulnerable entities and other Scyers are exempt.

It moves quickly, recalculates its path every five ticks, and strikes every ten ticks at close range. A hit still requires a clear line to its victim and a reachable path: it does not teleport or tunnel through walls. Roaring never pauses pursuit or delays the first strike. Crouching, rain, food and competing noises no longer make it passive. Actual server sounds still attract investigation when no living target is found.

The pale body, empty eye sockets, hanging jaw and long claws retain the original scary appearance. All four levels are **four blocks tall**, need suitable headroom, resist knockback, and are immune to fire. No villager or potion particles are emitted. Roar retains its sonic effect and immobilizes survival/adventure players within ten blocks for 2.5 seconds, with a twenty-second cooldown. Normal shields, totems, Resistance, absorption and invulnerability mechanics remain.

## Four levels

| Level | Health | Raw attack damage | Equipment |
| --- | --- | --- | --- |
| 1 | 1,000 HP | 10 HP claw / 27 HP smash | Bare claws |
| 2 | 1,008 HP | 10 HP claw / 27 HP smash | Adamantium armor |
| 3 | 1,016 HP | 10 HP claw / 27 HP smash | Uru (우르 금속) armor |
| 4 | 1,024 HP | 10 HP claw / 27 HP smash | Uru armor and miniature sword, axe, mace, pickaxe, spear, trident, bow and crossbow in its hands |

Scyer alternates a **Sharpness III netherite-sword-equivalent claw hit (10 HP raw)** and a **six-block Density I mace-equivalent smash (27 HP raw)**. The smash has Breach I's vanilla armor-reduction effect. Density and Breach are normally mutually exclusive player enchantments; this is a monster-specific combined attack, not a craftable combined-enchantment mace. Strength V and armor/Protection-bypassing attacks have been removed. Armor, Protection, shields and totems work normally. The miniature weapons remain visual rather than separately fired projectiles.

**Fifty-smash balance:** all four levels survive 49 fully charged Density V mace smashes with **six blocks of actual fall distance before impact**, then die on hit 50, from full health without extra healing or defenses. Native six-block Density V smash damage is 39 HP; incoming density-smash damage is normalized after armor to this reference. Other Density levels, fall heights, partial charges and Breach change the result. A six-block platform does not always mean six blocks of fall before you hit a four-block-tall creature. The old per-hit sword/mace/explosion caps and stage resilience multipliers are gone; ordinary attacks and explosions use normal damage and armor rules. Breach still increases damage against boss armor and wearable armor.

Regeneration II resumes after five seconds without taking damage. Sustained attacks pause it, so healing does not cancel the fifty-hit target. The roar's movement/action lock now lasts exactly **50 ticks / 2.5 seconds**, with the existing ten-block radius and twenty-second cooldown.

## Death upgrades and loot

Only **one Scyer may exist across the entire server**, including unloaded chunks and other dimensions. All four entity types, natural spawns, commands, eggs and stalking spawns share that slot. A death advances the world's next natural/stalking level, capped at four. Progression and each entity's level are saved across restarts; unloading or discarding an entity does not count as a death. The slot releases when its death animation finishes/removal occurs. Creative-mode eggs replace the previous Scyer, including an unloaded one, with exactly one new creature of the selected level. This replacement gives no loot and does not advance evolution. Survival-mode eggs preserve the cap and print a clear message when blocked.

In the Overworld, stalking attempts run every thirty seconds on supported, loaded, dry terrain 52–64 horizontal blocks from an eligible player, at least fifty blocks from every player. They need four-block clearance and use temporary simulation tickets. Peaceful and disabled mob spawning prevent automatic spawns. Natural biome spawns retain normal darkness rules.

- Level 1: no armor drops.
- Level 2: a full Adamantium armor set.
- Levels 3 and 4: a full Uru armor set.
- Each dropped armor piece has a randomly selected **20–50 maximum durability**, even after repair.
- Level 4 independently rolls **50% Uru pickaxe**, **10% Uru sword**, **4% Uru mace** and **35% Uru axe**. Several weapons can drop together, or none. Looting does not change these percentages.

The tools are functional equipment with 4,096 durability: the pickaxe mines at netherite harvest level, the axe can strip/scrape, the sword attacks normally, and the mace retains real smash behavior and accepts vanilla mace enchantments. Armor and weapons appear in the relevant creative tabs. Ordinary mob-loot gamerules apply.

## Install and spawn

Replace your older Syrer/Scyer mod JAR with `build/libs/scyer-1.6.1+mc26.2.jar` in the instance's `mods` folder. Keep Fabric API installed. Install the same versions on client and server. Old `syrer` namespaces are not migrated. Existing `scyer` entities are upgraded on load; use a fresh creature to compare the levels.

The Spawn Eggs tab contains **Level 1 Scyer Spawn Egg** through **Level 4 Scyer Spawn Egg**:

```mcfunction
/give @s scyer:scyer_spawn_egg
/give @s scyer:scyer_level_2_spawn_egg
/give @s scyer:scyer_level_3_spawn_egg
/give @s scyer:scyer_level_4_spawn_egg
/summon scyer:scyer_level_4 ~ ~ ~6
```

The base entity remains `scyer:scyer`; other types are `scyer:scyer_level_2`, `scyer:scyer_level_3`, and `scyer:scyer_level_4`. An evolved natural spawn can use the base entity type while its saved level, displayed name and appearance are higher. To clear the existing creature for testing any egg, use `/kill @e[type=#scyer:scyers]` in its dimension, then wait for its death animation to finish. The cap includes other dimensions. Version 1.6.1 adds operator recovery commands that also work when the old creature is unloaded or its slot is stale:

```mcfunction
/scyer status
/scyer reset
/scyer spawn 1
/scyer spawn 4
```

`status` reports the occupied slot, difficulty, and loaded creature locations. `reset` discards loaded creatures in all dimensions and persistently retires old UUIDs, clearing inaccessible slots without loot or evolution. `spawn <1-4>` validates open dry ground first, then replaces the previous creature and spawns the requested level nearby. Failed space/difficulty checks preserve the old creature. The commands require cheats/operator permissions; run `/difficulty normal` first if the world is Peaceful. Eggs also explain Peaceful difficulty and insufficient headroom in chat. Version 1.6.0 population saves are migrated automatically; reset history prevents a replaced creature from returning when its old chunk loads.

## Develop and validate

Cloud tasks use the existing `/workspace/Syrer` checkout; do not create a Git worktree unless requested. Source `/workspace/scyer-env.sh` for Java 25 and the retained Gradle cache. `/workspace/scyer-install.sh` recreates tool activation and runs the checks. The GitHub repository itself has not been renamed.

```sh
./gradlew build
./gradlew test
./gradlew runGameTest
./gradlew runClient  # Graphical desktop required
./gradlew runServer  # Review Minecraft's EULA before accepting run/eula.txt
```

Use `gradlew.bat` on Windows. The wrapper pins Gradle 9.7.1 with its checksum; Loom is 1.18.4. Generated model sources and texture assets are checked in. Optional regeneration uses `python tools/generate_scyer_model.py` and `python tools/generate_gear_assets.py` (the latter requires Pillow).

The suite has three balance tests and nineteen Scyer server GameTests (plus Minecraft's built-in always-pass case). It checks immediate player-first attacks and mob fallback; through-wall/100-block tracking; all four egg levels and walking; every level surviving 49 native six-block Density V smashes and dying on hit 50; the new sword/mace attack strengths and real armor/Protection reductions; ordinary explosion damage; Breach; actual death drops and fragile durability; independent loot rolls; saved evolution and entity levels; distant spawning; the population cap; precisely 2.5 seconds of immobilization; and regeneration cooldown. Regression tests exercise actual Creative egg clicks replacing all four levels, actual reset/spawn/vanilla-summon commands, old-format save migration, ghost-slot recovery, permanent rejection of retired unloaded creatures, and safe failure in Peaceful or without room. Tests keep the real cap enabled and are excluded from the release JAR.

Headless model checks verify all four variants, armor/weapon visibility, jaw/claw animations and pose resets. Actual appearance, sounds, item rendering and movement on complex terrain still need graphical Minecraft testing. Visual credits and license terms are in [VISUAL_CREDITS.md](VISUAL_CREDITS.md).
