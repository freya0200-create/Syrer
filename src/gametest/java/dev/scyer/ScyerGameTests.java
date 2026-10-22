package dev.scyer;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.gameevent.GameEvent;

public final class ScyerGameTests {
    private static final java.util.List<ScyerEntity> FIXTURES = new java.util.ArrayList<>();
    static {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            FIXTURES.forEach(Entity::discard);
            FIXTURES.clear();
        });
    }
    private ScyerEntity spawnScyer(GameTestHelper context, float x, float y, float z) {
        ScyerSpawning.prepareStalkingChunk(context.getLevel(), context.absolutePos(BlockPos.containing(x, y, z)));
        ScyerEntity scyer = context.spawn(ScyerMod.SCYER, x, y, z);
        FIXTURES.add(scyer);
        return scyer;
    }
    private void floor(GameTestHelper context) {
        FIXTURES.forEach(Entity::discard);
        FIXTURES.clear();
        context.getLevel().getGameRules().set(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS, false, context.getLevel().getServer());
        // Each test has its own environment batch because the population cap is server-wide.
        for (var level : context.getLevel().getServer().getAllLevels()) {
            java.util.List<Entity> old = new java.util.ArrayList<>();
            level.getAllEntities().forEach(e -> { if (e instanceof net.minecraft.world.entity.Mob || e instanceof ItemEntity) old.add(e); });
            old.forEach(Entity::discard);
        }
        for (var player : java.util.List.copyOf(context.getLevel().getServer().getPlayerList().getPlayers())) {
            player.level().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        }
        AABB area = new AABB(context.absolutePos(BlockPos.ZERO)).inflate(64);
        context.getLevel().getEntitiesOfClass(ScyerEntity.class, area, e -> true).forEach(Entity::discard);
        context.getLevel().getEntitiesOfClass(ItemEntity.class, area, e -> true).forEach(Entity::discard);
        for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) {
            context.setBlock(x, 1, z, Blocks.STONE);
        }
    }

    private net.minecraft.server.level.ServerPlayer survivalPlayer(GameTestHelper context) {
        return testPlayer(context, net.minecraft.world.level.GameType.SURVIVAL);
    }
    private net.minecraft.server.level.ServerPlayer testPlayer(GameTestHelper context, net.minecraft.world.level.GameType mode) {
        var player = (net.minecraft.server.level.ServerPlayer) context.makeMockServerPlayer(mode);
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(), false);
        context.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        return player;
    }

    @GameTest(maxTicks = 60, padding = 32, environment = "scyer_gametest:real_game_event_drives_investigation")
    public void realGameEventDrivesInvestigation(GameTestHelper context) {
        floor(context);
        ScyerEntity scyer = spawnScyer(context, 2.5F, 2, 2.5F);
        BlockPos destination = context.absolutePos(new BlockPos(6, 2, 2));
        context.runAtTickTime(5, () -> context.getLevel().gameEvent(GameEvent.NOTE_BLOCK_PLAY,
            Vec3.atCenterOf(destination), GameEvent.Context.of((Entity) null)));
        context.runAtTickTime(15, () -> {
            BlockPos target = scyer.getNavigation().getTargetPos();
            context.assertTrue(target != null && target.closerThan(destination, 2),
                "Expected investigation at " + destination + "; actual navigation target: " + target + "; alive=" + scyer.isAlive() + "; ticks=" + scyer.tickCount);
            context.succeed();
        });
    }

    @GameTest(maxTicks = 60, padding = 32, environment = "scyer_gametest:one_server_wide_slot_persists_and_releases")
    public void oneServerWideSlotPersistsAndReleases(GameTestHelper context) {
        floor(context);
        var server = context.getLevel().getServer();
        var first = spawnScyer(context, 2.5F, 2, 2.5F);
        first.setNoAi(true);
        var population = ScyerPopulation.get(server);
        context.assertTrue(first.isAlive() && population.size() == 1, "The first Scyer must reserve the only server-wide slot");
        var nether = server.getLevel(net.minecraft.world.level.Level.NETHER);
        nether.getChunkAt(BlockPos.ZERO);
        var second = ScyerMod.SCYER.create(nether, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        second.setPos(0, 80, 0);
        nether.addFreshEntity(second);
        context.assertTrue(second.isRemoved() && population.size() == 1, "A second Scyer must be rejected even in another dimension");
        var encoded = ScyerPopulation.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, population).getOrThrow();
        var restored = ScyerPopulation.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, encoded).getOrThrow();
        context.assertTrue(restored.size() == 1 && restored.admit(first.getUUID()) && !restored.admit(java.util.UUID.randomUUID()),
            "A saved population must restore its UUID and cap without freeing unloaded entities");
        first.discard();
        context.assertTrue(population.size() == 0, "Removing Scyer must release its persistent slot");
        var replacement = spawnScyer(context, 2.5F, 2, 2.5F);
        context.assertTrue(replacement.isAlive() && population.size() == 1, "A replacement must spawn after the old Scyer is removed");
        context.succeed();
    }

    @GameTest(maxTicks = 140, padding = 32, environment = "scyer_gametest:roar_faints_for_exactly_two_point_five_seconds_within_ten_blocks")
    public void roarFaintsForExactlyTwoPointFiveSecondsWithinTenBlocks(GameTestHelper context) {
        floor(context);
        var scyer = spawnScyer(context, 2.5F, 2, 2.5F);
        scyer.setNoAi(true);
        var near = survivalPlayer(context);
        near.setPos(scyer.position().add(7, 0, 0));
        var far = survivalPlayer(context);
        far.setPos(scyer.position().add(11, 0, 0));
        var creative = testPlayer(context, net.minecraft.world.level.GameType.CREATIVE);
        creative.setPos(scyer.position().add(3, 0, 0));
        Vec3 anchor = near.position();
        context.runAtTickTime(5, () -> {
            context.assertTrue(scyer.roar(context.getLevel()), "The initial roar must activate");
            context.assertTrue(Fainting.isFainted(near), "A survival player within ten blocks must faint");
            context.assertTrue(!Fainting.isFainted(far) && !Fainting.isFainted(creative), "Distant and creative players must not faint");
            context.assertTrue(near.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS), "Fainting must darken the player's vision");
        });
        context.runAtTickTime(6, () -> {
            context.assertTrue(!scyer.roar(context.getLevel()), "The roar cooldown must prevent stun spam");
            near.setPos(anchor.add(2, 0, 0));
            Fainting.holdStill(near);
            context.assertTrue(near.position().distanceToSqr(anchor) < .001, "Fainted players must not move away from their anchor");
            near.connection.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                anchor.x + 2, anchor.y, anchor.z, true, false));
            context.assertTrue(near.position().distanceToSqr(anchor) < .001, "A client movement packet must also be rejected while fainted");
            var result = net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.invoker().interact(near,
                context.getLevel(), net.minecraft.world.InteractionHand.MAIN_HAND);
            context.assertTrue(result == net.minecraft.world.InteractionResult.FAIL, "Fainted players must not use items");
        });
        context.runAtTickTime(54, () -> context.assertTrue(Fainting.isFainted(near), "Fainting must still apply at 49 elapsed ticks"));
        context.runAtTickTime(55, () -> {
            context.assertTrue(!Fainting.isFainted(near), "Fainting must expire at exactly 50 elapsed ticks");
            near.setPos(anchor.add(1, 0, 0));
            Fainting.holdStill(near);
            context.assertTrue(near.position().distanceToSqr(anchor) > .9, "Movement must be restored after 2.5 seconds");
            context.succeed();
        });
    }

    @GameTest(maxTicks = 60, padding = 32, environment = "scyer_gametest:distant_spawn_has_four_blocks_of_space_and_tracks_player")
    public void distantSpawnHasFourBlocksOfSpaceAndTracksPlayer(GameTestHelper context) {
        floor(context);
        var player = survivalPlayer(context);
        player.setPos(Vec3.atBottomCenterOf(context.absolutePos(new BlockPos(3, 2, 3))));
        long seed = 92612;
        BlockPos candidate = ScyerSpawning.candidateNear(player, net.minecraft.util.RandomSource.create(seed));
        context.getLevel().getChunkAt(candidate);
        context.getLevel().setChunkForced(candidate.getX() >> 4, candidate.getZ() >> 4, true);
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            context.getLevel().setBlockAndUpdate(candidate.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            for (int y = 0; y < 5; y++) context.getLevel().setBlockAndUpdate(candidate.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        var scyer = ScyerSpawning.trySpawnNear(context.getLevel(), player, net.minecraft.util.RandomSource.create(seed));
        context.assertTrue(scyer != null && scyer.isAlive(), "A valid distant hunting spawn must succeed");
        FIXTURES.add(scyer);
        context.assertTrue(scyer.stage() == ScyerEvolution.get(context.getLevel().getServer()).nextStage(), "Stalking spawns must use the world's evolved level");
        context.assertTrue(scyer.distanceToSqr(player) >= 2500, "Automatic Scyers must spawn at least 50 blocks from players");
        context.assertTrue(scyer.getBbHeight() == 4 && context.getLevel().noCollision(scyer), "A four-block Scyer must spawn with actual clearance");
        context.assertTrue(ScyerSpawning.trySpawnNear(context.getLevel(), player, net.minecraft.util.RandomSource.create(seed + 1)) == null,
            "Automatic spawning must obey the one-Scyer cap");
        context.runAtTickTime(15, () -> {
            try {
                context.assertTrue(scyer.getTarget() == player, "The distant hunter must acquire the player's scent trail; alive=" + scyer.isAlive() + ", removed=" + scyer.isRemoved() + ", ticks=" + scyer.tickCount);
                context.succeed();
            } finally { scyer.discard(); }
        });
    }

    @GameTest(maxTicks = 60, padding = 32, environment = "scyer_gametest:immediate_player_first_tracking_ignores_crouching")
    public void immediatePlayerFirstTrackingIgnoresCrouching(GameTestHelper context) {
        floor(context);
        var scyer = spawnScyer(context, 2.5F, 2, 2.5F);
        var pig = context.spawn(net.minecraft.world.entity.EntityTypes.PIG, 4, 2, 2); pig.setNoAi(true);
        var player = survivalPlayer(context); player.setShiftKeyDown(true);
        player.setPos(scyer.position().add(2, 0, 0));
        player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(500);player.setHealth(500);
        context.runAtTickTime(2, () -> {
            context.assertTrue(scyer.getTarget() == player, "A silent motionless crouched player must be acquired immediately, ahead of a closer pig");
            context.assertTrue(player.getHealth() == 490, "Scyer must strike on its first AI tick before roaring; health=" + player.getHealth());
            scyer.hurtServer(context.getLevel(), context.getLevel().damageSources().mobAttack(pig), 1);
        });
        context.runAtTickTime(4, () -> {
            context.assertTrue(scyer.getTarget() == player, "Players must still outrank a retaliating mob");
            player.setHealth(20);player.invulnerableTime=0;
        });
        context.runAtTickTime(15, () -> {
            context.assertTrue(player.isDeadOrDying(), "Scyer must strike crouched players even during its roar");
        });
        context.runAtTickTime(35, () -> {
            context.assertTrue(!pig.isAlive(), "After killing the player, Scyer must kill the nearby mob");
            context.succeed();
        });
    }

    @GameTest(maxTicks = 60, padding = 32, environment = "scyer_gametest:tracker_detects_silent_undead_and_players_through_walls")
    public void trackerDetectsSilentUndeadAndPlayersThroughWalls(GameTestHelper context) {
        floor(context); var scyer = spawnScyer(context, 2.5F, 2, 2.5F);
        var zombie = context.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, 15, 2, 2);
        zombie.setNoAi(true); zombie.setSilent(true); zombie.setNoGravity(true);
        zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(Items.NETHERITE_HELMET));
        for (int y = 2; y < 8; y++) for (int z = 0; z < 8; z++) context.setBlock(5,y,z,Blocks.STONE);
        context.runAtTickTime(3, () -> {
            context.assertTrue(!scyer.hasLineOfSight(zombie) && scyer.getTarget() == zombie, "Tracking must detect silent undead behind a solid wall");
            zombie.setPos(scyer.position().add(101,0,0));
        });
        context.runAtTickTime(5, () -> {
            context.assertTrue(scyer.getTarget() == null, "Tracking must stop beyond 100 blocks");
            var player = survivalPlayer(context); player.setShiftKeyDown(true);
            player.setPos(scyer.position().add(99,0,0));
        });
        context.runAtTickTime(7, () -> {
            context.assertTrue(scyer.getTarget() instanceof net.minecraft.world.entity.player.Player, "A silent crouched player inside 100 blocks must be tracked through walls");
            context.succeed();
        });
    }

    @GameTest(maxTicks = 100, padding = 32, environment = "scyer_gametest:all_four_eggs_select_levels_and_move")
    public void allFourEggsSelectLevelsAndMove(GameTestHelper context) {
        floor(context);
        for (int x=0;x<24;x++) for (int z=0;z<8;z++) context.setBlock(x,1,z,Blocks.STONE);
        ScyerEntity last = null;
        for (int stage = 1; stage <= 4; stage++) {
            if (last != null) last.discard();
            var pos=context.absolutePos(new BlockPos(2,2,2));
            ScyerSpawning.prepareStalkingChunk(context.getLevel(),pos);
            last=ScyerMod.TYPES.get(stage-1).spawn(context.getLevel(), new net.minecraft.world.item.ItemStack(ScyerMod.EGGS.get(stage-1)),
                null,pos,net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE,false,false);
            FIXTURES.add(last);
            context.assertTrue(last != null && last.stage()==stage, "Each egg must select its exact level, independently of world progression");
            context.assertTrue(last.getBbHeight()==4 && last.getMaxHealth()>=1000, "All four levels must remain four blocks tall with boss health");
        }
        var scyer=last; var start=scyer.position();
        var pig=context.spawn(net.minecraft.world.entity.EntityTypes.PIG,20,2,2);pig.setNoAi(true);pig.setSilent(true);
        scyer.setHealth(900);
        context.runAtTickTime(50,()->{
            context.assertTrue(scyer.position().distanceToSqr(start)>4, "A level-four egg spawn must actually walk toward prey");
            context.assertTrue(scyer.getHealth()>900, "Regeneration II must heal it");
            var regen=scyer.getEffect(net.minecraft.world.effect.MobEffects.REGENERATION);
            context.assertTrue(regen!=null && regen.getAmplifier()==1 && !regen.isVisible(), "Regeneration II must have no particles");
            context.succeed();
        });
    }

    @GameTest(maxTicks = 60, padding = 32, environment = "scyer_gametest:fifty_six_block_density_five_smashes_kill_every_level")
    public void fiftySixBlockDensityFiveSmashesKillEveryLevel(GameTestHelper context) {
        floor(context);var player=survivalPlayer(context);
        player.setPos(Vec3.atBottomCenterOf(context.absolutePos(new BlockPos(10,2,2))));
        var mace=new net.minecraft.world.item.ItemStack(Items.MACE);
        var density=context.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
            .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.DENSITY);
        mace.enchant(density,5);player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,mace);
        for(int stage=1;stage<=4;stage++) {
            var scyer=spawnScyer(context,2.5F,2,2.5F);scyer.setStage(stage);scyer.setNoAi(true);
            scyer.tick();scyer.tick();
            if(stage>1)context.assertTrue(scyer.getArmorValue()>=20,"Armored stages must have real armor during calibration");
            for(int hit=1;hit<=50;hit++) {
                player.fallDistance=6;scyer.invulnerableTime=0;
                var source=context.getLevel().damageSources().mace(player);
                float raw=6+((net.minecraft.world.item.MaceItem)Items.MACE).getAttackDamageBonus(scyer,6,source);
                context.assertTrue(raw==39,"Native six-block Density V fully charged smash must deal 39 raw HP");
                scyer.hurtServer(context.getLevel(),source,raw);
                context.assertTrue(hit<50 ? scyer.isAlive() : scyer.isDeadOrDying(),"Level "+stage+" must survive hit 49 and die on hit 50; hit="+hit+", health="+scyer.getHealth());
            }
            scyer.discard();
        }
        context.succeed();
    }

    @GameTest(maxTicks = 60, padding = 32, environment = "scyer_gametest:nerfed_weapon_attacks_respect_armor_and_protection")
    public void nerfedWeaponAttacksRespectArmorAndProtection(GameTestHelper context) {
        floor(context);var scyer=spawnScyer(context,2.5F,2,2.5F);scyer.setNoAi(true);scyer.setStage(4);
        var player=survivalPlayer(context);player.setPos(scyer.position().add(3,0,0));
        player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);player.setHealth(200);
        context.runAtTickTime(4,()->{
            scyer.doHurtTarget(context.getLevel(),player);
            context.assertTrue(player.getHealth()==190,"Claws must match a Sharpness III netherite sword: 10 raw HP, without Strength V");
            player.setHealth(200);player.invulnerableTime=0;scyer.doHurtTarget(context.getLevel(),player);
            context.assertTrue(player.getHealth()==173,"Smash must match a six-block Density I mace: 27 raw HP");
            var breach=context.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.BREACH);
            context.assertTrue(net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(breach,scyer.getWeaponItem())==1,
                "Scyer's reduced smash must use Breach I, without the old armor bypass");
            context.assertTrue(!scyer.hasEffect(net.minecraft.world.effect.MobEffects.STRENGTH),"The old Strength V buff must be removed");
            var protection=context.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.PROTECTION);
            var slots=new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET};
            var armor=new net.minecraft.world.item.Item[]{Items.NETHERITE_HELMET,Items.NETHERITE_CHESTPLATE,Items.NETHERITE_LEGGINGS,Items.NETHERITE_BOOTS};
            for(int i=0;i<4;i++){var stack=new net.minecraft.world.item.ItemStack(armor[i]);stack.enchant(protection,4);player.setItemSlot(slots[i],stack);}
            player.doTick();player.setHealth(200);player.invulnerableTime=0;scyer.doHurtTarget(context.getLevel(),player);
            context.assertTrue(player.getHealth()>190 && player.getHealth()<200,"Netherite and Protection IV must reduce claw damage");
            player.setHealth(200);player.invulnerableTime=0;scyer.doHurtTarget(context.getLevel(),player);
            context.assertTrue(player.getHealth()>173 && player.getHealth()<200,"Protection IV must reduce the Breach smash instead of being bypassed");
            context.succeed();
        });
    }

    @GameTest(maxTicks = 60, padding = 32, environment = "scyer_gametest:breach_works_on_scyer_and_dropped_uru_armor")
    public void breachWorksOnScyerAndDroppedUruArmor(GameTestHelper context) {
        floor(context);var scyer=spawnScyer(context,2.5F,2,2.5F);scyer.setStage(3);scyer.setNoAi(true);
        var player=survivalPlayer(context);player.setPos(scyer.position().add(10,0,0));
        var mace=new net.minecraft.world.item.ItemStack(Items.MACE);player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,mace);
        var breach=context.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
            .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.BREACH);
        var wearer=survivalPlayer(context);wearer.setPos(scyer.position().add(15,0,0));
        wearer.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);wearer.setHealth(200);
        context.runAtTickTime(5,()->{
            context.assertTrue(scyer.getArmorValue()>=20,"The boss must actually wear functional armor");
            scyer.setHealth(1000);scyer.invulnerableTime=0;
            scyer.hurtServer(context.getLevel(),context.getLevel().damageSources().mace(player),100);
            float plain=1000-scyer.getHealth();
            mace.enchant(breach,4);scyer.setHealth(1000);scyer.invulnerableTime=0;
            scyer.hurtServer(context.getLevel(),context.getLevel().damageSources().mace(player),100);
            float breached=1000-scyer.getHealth();
            context.assertTrue(breached>plain*1.5,"Breach IV must increase real damage to Uru-armored Scyer; plain="+plain+", breached="+breached);
            var slots=new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET};
            var drops=ScyerGear.deathDrops(3,net.minecraft.util.RandomSource.create(1));
            for(int i=0;i<4;i++)wearer.setItemSlot(slots[i],drops.get(i));
            wearer.doTick();var playerMace=new net.minecraft.world.item.ItemStack(Items.MACE);
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,playerMace);
            wearer.hurtServer(context.getLevel(),context.getLevel().damageSources().mace(player),10);float normal=200-wearer.getHealth();
            wearer.setHealth(200);wearer.invulnerableTime=0;playerMace.enchant(breach,4);
            wearer.hurtServer(context.getLevel(),context.getLevel().damageSources().mace(player),10);float reduced=200-wearer.getHealth();
            context.assertTrue(reduced>normal,"Breach must also work when a player wears dropped Uru armor; plain="+normal+", breach="+reduced+", armor="+wearer.getArmorValue());
            context.succeed();
        });
    }

    @GameTest(maxTicks = 60, padding = 32, environment = "scyer_gametest:actual_death_drops_armor_with_twenty_to_fifty_durability")
    public void actualDeathDropsArmorWithTwentyToFiftyDurability(GameTestHelper context) {
        floor(context);var scyer=spawnScyer(context,2.5F,2,2.5F);scyer.setStage(2);scyer.setNoAi(true);
        scyer.hurtServer(context.getLevel(),context.getLevel().damageSources().genericKill(),Float.MAX_VALUE);
        var drops=context.getLevel().getEntitiesOfClass(ItemEntity.class,scyer.getBoundingBox().inflate(5),item->true);
        context.assertTrue(drops.size()==4,"Level two must actually drop exactly four Adamantium armor pieces on death");
        for(var drop:drops){var stack=drop.getItem();
            context.assertTrue(java.util.Arrays.asList(ScyerGear.ADAMANTIUM).contains(stack.getItem()),"No undamaged vanilla equipment may leak into boss drops");
            context.assertTrue(stack.getMaxDamage()==5000,"Dropped armor must retain 5000 maximum durability");
            int remaining=stack.getMaxDamage()-stack.getDamageValue();
            context.assertTrue(remaining>=20 && remaining<=50,"Dropped armor must have only 20-50 durability remaining");
            context.assertTrue(stack.getCount()==1,"Each armor piece must drop once");}
        context.succeed();
    }

    @GameTest(maxTicks = 60, padding = 32, environment = "scyer_gametest:level_four_drop_percentages_are_independent")
    public void levelFourDropPercentagesAreIndependent(GameTestHelper context) {
        floor(context);
        class Fixed extends net.minecraft.world.level.levelgen.LegacyRandomSource {
            private final float value; Fixed(float value){super(1);this.value=value;}
            @Override public float nextFloat(){return value;}
        }
        for(var entry:java.util.Map.of(.039F,8,.04F,7,.10F,6,.35F,5,.50F,4).entrySet()){
            var drops=ScyerGear.deathDrops(4,new Fixed(entry.getKey()));
            context.assertTrue(drops.size()==entry.getValue(),"Independent 50/10/4/35 percent rolls must obey their exact boundaries; roll="+entry.getKey());
        }
        context.assertTrue(ScyerGear.deathDrops(1,new Fixed(0)).isEmpty(),"Level one has no armor or weapon loot");
        for(int stage=2;stage<=4;stage++) for(var stack:ScyerGear.deathDrops(stage,new Fixed(.99F))) {
            context.assertTrue(stack.getMaxDamage()==5000,"Every Adamantium/Uru armor piece must have 5000 total durability");
            int remaining=stack.getMaxDamage()-stack.getDamageValue();
            context.assertTrue(remaining>=20 && remaining<=50,"Each armor drop must have 20-50 remaining, not maximum, durability");
            context.assertTrue(new net.minecraft.world.item.ItemStack(stack.getItem()).getMaxDamage()==5000,
                "A newly created or fully repaired armor piece must support 5000 durability");
        }
        var uruMace=new net.minecraft.world.item.ItemStack(ScyerGear.URU_MACE);
        context.assertTrue(uruMace.getItem() instanceof net.minecraft.world.item.MaceItem && uruMace.is(net.minecraft.tags.ItemTags.MACE_ENCHANTABLE),"Uru mace must have real smash behavior and vanilla mace enchantment compatibility");
        context.succeed();
    }

    @GameTest(maxTicks = 60, padding = 32, environment = "scyer_gametest:evolution_and_entity_level_survive_serialization")
    public void evolutionAndEntityLevelSurviveSerialization(GameTestHelper context) {
        floor(context);var evolution=new ScyerEvolution();
        context.assertTrue(evolution.nextStage()==1,"A new world must start at level one");
        evolution.recordDeath(1);context.assertTrue(evolution.nextStage()==2,"First death must unlock Adamantium");
        evolution.recordDeath(2);context.assertTrue(evolution.nextStage()==3,"Second death must unlock Uru");
        evolution.recordDeath(3);evolution.recordDeath(4);context.assertTrue(evolution.nextStage()==4,"Evolution must cap at level four");
        var encoded=ScyerEvolution.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,evolution).getOrThrow();
        context.assertTrue(ScyerEvolution.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,encoded).getOrThrow().nextStage()==4,"World evolution must survive save/reload");
        var scyer=spawnScyer(context,2.5F,2,2.5F);scyer.setStage(4);scyer.setNoAi(true);scyer.setHealth(777);
        var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,context.getLevel().registryAccess());
        scyer.saveWithoutId(output);
        var restored=ScyerMod.SCYER.create(context.getLevel(),net.minecraft.world.entity.EntitySpawnReason.LOAD);
        restored.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,context.getLevel().registryAccess(),output.buildResult()));
        context.assertTrue(restored.stage()==4 && restored.getHealth()==777,"Entity level and health must survive serialization without healing to full");
        var worldEvolution=ScyerEvolution.get(context.getLevel().getServer());
        scyer.hurtServer(context.getLevel(),context.getLevel().damageSources().genericKill(),Float.MAX_VALUE);
        context.assertTrue(worldEvolution.nextStage()==4,"An actual level-four death must save the world's next level");
        context.succeed();
    }
    @GameTest(maxTicks = 160, padding = 32, environment = "scyer_gametest:regeneration_waits_five_seconds_after_damage")
    public void regenerationWaitsFiveSecondsAfterDamage(GameTestHelper context) {
        floor(context); var scyer=spawnScyer(context,2.5F,2,2.5F);scyer.setNoAi(true);scyer.setHealth(900);
        context.runAtTickTime(5,()->{
            scyer.setHealth(900);
            scyer.hurtServer(context.getLevel(),context.getLevel().damageSources().generic(),1);
            context.assertTrue(!scyer.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION),"Combat must pause regeneration so sustained attacks can kill all levels");
        });
        context.runAtTickTime(104,()->context.assertTrue(scyer.getHealth()==899,"No passive healing during the 100-tick combat cooldown"));
        context.runAtTickTime(145,()->{
            context.assertTrue(scyer.getHealth()>899 && scyer.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION),"Regeneration II must resume after five seconds without damage");
            context.succeed();
        });
    }

    @GameTest(maxTicks = 60, padding = 32, environment = "scyer_gametest:real_minecart_explosion_uses_titan_resilience")
    public void realMinecartExplosionUsesTitanResilience(GameTestHelper context) {
        floor(context);var scyer=spawnScyer(context,2.5F,2,2.5F);scyer.setNoAi(true);
        var cart=net.minecraft.world.entity.EntityTypes.TNT_MINECART.create(context.getLevel(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        cart.setPos(scyer.position().add(1,0,0));
        context.getLevel().explode(cart,cart.getX(),cart.getY()+1,cart.getZ(),8,net.minecraft.world.level.Level.ExplosionInteraction.NONE);
        context.assertTrue(scyer.isAlive() && scyer.getHealth()<990,"The old nine-HP explosion cap must be removed; a real explosion must do normal damage; health="+scyer.getHealth());
        context.succeed();
    }

    @GameTest(maxTicks = 60, padding = 32, environment = "scyer_gametest:reset_recovers_ghost_slot_and_vanilla_summon")
    public void resetRecoversGhostSlotAndVanillaSummon(GameTestHelper context) throws Exception {
        floor(context);
        var server=context.getLevel().getServer();var population=ScyerPopulation.get(server);
        var ghost=java.util.UUID.randomUUID();context.assertTrue(population.admit(ghost),"Fixture must occupy the slot without a loaded entity");
        var source=server.createCommandSourceStack().withLevel(context.getLevel()).withPosition(
            Vec3.atBottomCenterOf(context.absolutePos(new BlockPos(3,2,3))));
        var dispatcher=server.getCommands().getDispatcher();
        int stage=ScyerEvolution.get(server).nextStage();
        context.assertTrue(dispatcher.execute("scyer reset",source)==1 && population.size()==0,"Reset command must clear an inaccessible ghost slot");
        context.assertTrue(population.retired(ghost),"The old unloaded UUID must be permanently invalidated");
        context.assertTrue(ScyerEvolution.get(server).nextStage()==stage,"Reset must not count as a death or upgrade progression");
        context.assertTrue(dispatcher.execute("summon scyer:scyer ~ ~ ~",source)==1,"The actual vanilla summon command must work after reset");
        context.getLevel().getAllEntities().forEach(entity->{if(entity instanceof ScyerEntity scyer)FIXTURES.add(scyer);});
        context.assertTrue(population.size()==1,"Vanilla summon must reserve exactly one slot");
        context.succeed();
    }

    @GameTest(maxTicks = 60, padding = 32, environment = "scyer_gametest:actual_creative_egg_clicks_replace_all_four_levels")
    public void actualCreativeEggClicksReplaceAllFourLevels(GameTestHelper context) {
        floor(context);
        var old=spawnScyer(context,2.5F,2,2.5F);old.setNoAi(true);
        var player=testPlayer(context,net.minecraft.world.level.GameType.CREATIVE);
        player.setPos(Vec3.atBottomCenterOf(context.absolutePos(new BlockPos(1,2,6))));
        var clicked=context.absolutePos(new BlockPos(5,1,5));
        var server=context.getLevel().getServer();int progression=ScyerEvolution.get(server).nextStage();
        for(int stage=1;stage<=4;stage++) {
            // Clear the placement location for the next click without releasing its persistent slot.
            for(var fixture:FIXTURES)if(!fixture.isRemoved())fixture.setPos(Vec3.atBottomCenterOf(context.absolutePos(new BlockPos(2,2,2))));
            var egg=new net.minecraft.world.item.ItemStack(ScyerMod.EGGS.get(stage-1),2);
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,egg);
            var use=new net.minecraft.world.item.context.UseOnContext(context.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND,egg,
                new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(clicked).add(0,.5,0),net.minecraft.core.Direction.UP,clicked,false));
            egg.getItem().useOn(use);
            var spawned=context.getLevel().getEntitiesOfClass(ScyerEntity.class,new AABB(clicked).inflate(8),entity->!entity.isRemoved());
            context.assertTrue(spawned.size()==1 && spawned.getFirst().stage()==stage,"Actual creative egg click must replace the previous creature with exact level "+stage+"; found="+spawned.size());
            FIXTURES.add(spawned.getFirst());spawned.getFirst().setNoAi(true);
            context.assertTrue(egg.getCount()==2 && ScyerPopulation.get(server).size()==1,"Creative eggs must preserve item count and the one-creature cap");
        }
        context.assertTrue(old.isRemoved() && ScyerEvolution.get(server).nextStage()==progression,"Replacement must remove old Scyer without loot or evolution");
        context.assertTrue(context.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(clicked).inflate(16),entity->true).isEmpty(),"Creative replacement must not generate armor or weapons");
        context.succeed();
    }

    @GameTest(maxTicks = 60, padding = 32, environment = "scyer_gametest:retired_unloaded_entities_cannot_reclaim_reset_slot")
    public void retiredUnloadedEntitiesCannotReclaimResetSlot(GameTestHelper context) {
        floor(context);var server=context.getLevel().getServer();var oldId=java.util.UUID.randomUUID();
        var oldFormat=new com.google.gson.JsonArray();oldFormat.add(oldId.toString());
        var legacy=ScyerPopulation.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,oldFormat).getOrThrow();
        context.assertTrue(legacy.size()==1 && legacy.contains(oldId),"Old version UUID-list saves must still load");
        var population=ScyerPopulation.get(server);population.admit(oldId);population.reset(server);
        var encoded=ScyerPopulation.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,population).getOrThrow();
        var restored=ScyerPopulation.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,encoded).getOrThrow();
        context.assertTrue(restored.size()==0 && restored.retired(oldId) && !restored.admit(oldId),"Reset history must survive save/reload and reject the old UUID even with a free slot");
        var unloaded=ScyerMod.SCYER.create(context.getLevel(),net.minecraft.world.entity.EntitySpawnReason.LOAD);unloaded.setUUID(oldId);
        unloaded.setPos(Vec3.atBottomCenterOf(context.absolutePos(new BlockPos(3,2,3))));
        context.assertTrue(!context.getLevel().addFreshEntity(unloaded) && unloaded.isRemoved(),"A retired creature loading later must be discarded before reserving the slot");
        var fresh=spawnScyer(context,2.5F,2,2.5F);
        context.assertTrue(fresh.isAlive() && population.size()==1,"A new creature must be allowed after rejecting the retired UUID");
        context.succeed();
    }

    @GameTest(maxTicks = 60, padding = 32, environment = "scyer_gametest:spawn_command_replaces_and_failures_preserve_old_creature")
    public void spawnCommandReplacesAndFailuresPreserveOldCreature(GameTestHelper context) throws Exception {
        floor(context);var old=spawnScyer(context,2.5F,2,2.5F);old.setNoAi(true);
        var server=context.getLevel().getServer();var source=server.createCommandSourceStack().withLevel(context.getLevel())
            .withPosition(Vec3.atBottomCenterOf(context.absolutePos(new BlockPos(3,2,3))));
        var dispatcher=server.getCommands().getDispatcher();
        var difficulty=server.getWorldData().getDifficulty();
        try {
            server.setDifficulty(net.minecraft.world.Difficulty.PEACEFUL,true);
            context.assertTrue(dispatcher.execute("scyer spawn 4",source)==0 && !old.isRemoved(),"Peaceful must give a failure without clearing the old creature");
        } finally {server.setDifficulty(difficulty,true);}
        context.assertTrue(dispatcher.execute("scyer spawn 4",source.withPosition(source.getPosition().add(0,100,0)))==0 && !old.isRemoved(),"No-room failure must preserve the previous creature");
        context.assertTrue(dispatcher.execute("scyer spawn 4",source)==1 && old.isRemoved(),"An operator spawn must replace the old creature and create level four");
        var spawned=context.getLevel().getEntitiesOfClass(ScyerEntity.class,new AABB(context.absolutePos(BlockPos.ZERO)).inflate(32),entity->!entity.isRemoved());
        context.assertTrue(spawned.size()==1 && spawned.getFirst().stage()==4,"The spawn command must leave exactly one requested-level creature");
        FIXTURES.addAll(spawned);context.succeed();
    }

    @GameTest(maxTicks = 60, padding = 32, environment = "scyer_gametest:adamantium_protects_between_netherite_and_uru")
    public void adamantiumProtectsBetweenNetheriteAndUru(GameTestHelper context) {
        floor(context);
        var player=survivalPlayer(context);player.setPos(Vec3.atBottomCenterOf(context.absolutePos(new BlockPos(3,2,3))));
        player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
        var attacker=context.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE,6,2,6);attacker.setNoAi(true);
        var sets=new net.minecraft.world.item.Item[][]{
            {Items.NETHERITE_HELMET,Items.NETHERITE_CHESTPLATE,Items.NETHERITE_LEGGINGS,Items.NETHERITE_BOOTS},
            ScyerGear.ADAMANTIUM,ScyerGear.URU};
        var slots=new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,
            net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET};
        context.runAtTickTime(5,()->{
            for(float hit:new float[]{40,80,120}) {
                float[] taken=new float[3];
                for(int tier=0;tier<3;tier++) {
                    for(int piece=0;piece<4;piece++)player.setItemSlot(slots[piece],new net.minecraft.world.item.ItemStack(sets[tier][piece]));
                    player.doTick();player.setHealth(1000);player.invulnerableTime=0;
                    context.assertTrue(player.getArmorValue()==new int[]{20,24,30}[tier],"Each tier must equip its real armor attributes");
                    context.assertTrue(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS)==new int[]{12,16,20}[tier],
                        "Effective toughness must order netherite 12 < Adamantium 16 < Uru 20 (vanilla cap)");
                    player.hurtServer(context.getLevel(),context.getLevel().damageSources().mobAttack(attacker),hit);
                    taken[tier]=1000-player.getHealth();
                }
                context.assertTrue(taken[0]>taken[1] && taken[1]>taken[2],
                    "Real damage must satisfy netherite > Adamantium > Uru for hit "+hit+"; actual="+java.util.Arrays.toString(taken));
            }
            context.succeed();
        });
    }

}
