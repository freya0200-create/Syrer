package dev.acceleration;

import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.*;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

public final class AccelerationGameTests {
    private void setup(GameTestHelper c){
        var server=c.getLevel().getServer();
        for(var player:List.copyOf(server.getPlayerList().getPlayers())){
            SoulMode.finish(player,null);player.level().removePlayerImmediately(player,Entity.RemovalReason.DISCARDED);
        }
        for(var world:server.getAllLevels()){
            List<Entity> remove=new ArrayList<>();
            world.getAllEntities().forEach(e->{if(e instanceof LivingEntity||e instanceof ItemEntity)remove.add(e);});remove.forEach(Entity::discard);
        }
        c.getLevel().getGameRules().set(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS,false,server);
        c.getLevel().getGameRules().set(net.minecraft.world.level.gamerules.GameRules.PVP,true,server);
        for(int x=0;x<16;x++)for(int z=0;z<16;z++)c.setBlock(x,1,z,Blocks.STONE);
    }
    private ServerPlayer player(GameTestHelper c){
        var p=new ServerPlayer(c.getLevel().getServer(),c.getLevel(),
            new com.mojang.authlib.GameProfile(UUID.randomUUID(),"acceleration-test"),net.minecraft.server.level.ClientInformation.createDefault());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        c.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,net.minecraft.server.network.CommonListenerCookie.createInitial(p.getGameProfile(),false));
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        p.setGameMode(GameType.SURVIVAL);
        p.setPos(Vec3.atBottomCenterOf(c.absolutePos(new BlockPos(2,2,2))));
        p.setItemSlot(EquipmentSlot.MAINHAND,AccelerationSpear.create(c.getLevel()));return p;
    }
    private PlayerBody body(GameTestHelper c,ServerPlayer player){
        var session=SoulState.get(c.getLevel().getServer()).projection(player.getUUID());
        return (PlayerBody)c.getLevel().getEntity(session.body());
    }
    @GameTest(maxTicks=60,padding=32,environment="acceleration_gametest:reward_has_all_max_enchantments_and_native_spear_components")
    public void rewardHasAllMaxEnchantmentsAndNativeSpearComponents(GameTestHelper c){
        setup(c);var stack=AccelerationSpear.create(c.getLevel());
        c.assertTrue(stack.getMaxDamage()==Items.DIAMOND_SPEAR.getDefaultInstance().getMaxDamage(),"Spear must use diamond durability, not netherite");
        c.assertTrue(stack.get(DataComponents.ATTRIBUTE_MODIFIERS).equals(Items.DIAMOND_SPEAR.getDefaultInstance().get(DataComponents.ATTRIBUTE_MODIFIERS)),"Base combat attributes must match a diamond spear");
        c.assertTrue(stack.has(DataComponents.PIERCING_WEAPON)&&stack.has(DataComponents.KINETIC_WEAPON),"Reward must be a real jab/joust spear");
        c.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().forEach(e->{
            if(!e.is(EnchantmentTags.CURSE)&&e.value().isSupportedItem(stack))c.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(e,stack)==e.value().getMaxLevel(),"Every supported non-curse enchantment must be maximal: "+e.key().identifier());
        });
        var registry=c.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        c.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(registry.getOrThrow(Enchantments.PIERCING),stack)==4,"Reward must explicitly include Piercing IV");
        c.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(registry.getOrThrow(Enchantments.LUNGE),stack)==3,"Reward must have Lunge III");c.succeed();
    }
    @GameTest(maxTicks=60,padding=32,environment="acceleration_gametest:inventory_acceleration_works_without_holding_spear")
    public void inventoryAccelerationWorksWithoutHoldingSpear(GameTestHelper c){
        setup(c);var p=player(c);p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);p.getInventory().setItem(20,AccelerationSpear.create(c.getLevel()));
        p.setSprinting(false);AccelerationMod.accelerationTick(p);double base=p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        p.setSprinting(true);for(int i=0;i<100;i++)AccelerationMod.accelerationTick(p);
        c.assertTrue(AccelerationMod.charge(p)==100,"A spear in an ordinary inventory slot must charge acceleration");
        double charged=p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        c.assertTrue(Math.abs(charged-base*1.3*1.35)<1e-6,"Five seconds of sprinting must give a 35-percent acceleration bonus on top of native sprint speed");
        p.setSprinting(false);for(int i=0;i<25;i++)AccelerationMod.accelerationTick(p);
        c.assertTrue(AccelerationMod.charge(p)==0,"Stopping must decay acceleration");
        p.setSprinting(true);for(int i=0;i<50;i++)AccelerationMod.accelerationTick(p);p.getInventory().setItem(20,ItemStack.EMPTY);AccelerationMod.accelerationTick(p);
        c.assertTrue(AccelerationMod.charge(p)==0&&p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)<charged,"Removing spear must remove speed immediately");c.succeed();
    }
    @GameTest(maxTicks=60,padding=32,environment="acceleration_gametest:native_lunge_leaves_body_exactly_at_launch_position")
    public void nativeLungeLeavesBodyExactlyAtLaunchPosition(GameTestHelper c){
        setup(c);var p=player(c);p.setYRot(-90);p.getFoodData().setFoodLevel(20);
        Vec3 launch=p.position();p.getMainHandItem().get(DataComponents.PIERCING_WEAPON).attack(p,EquipmentSlot.MAINHAND);
        c.assertTrue(SoulMode.active(p)&&!p.isSpectator()&&p.gameMode()==GameType.SURVIVAL&&!p.getAbilities().flying&&!p.getAbilities().mayfly,"Native lunge must preserve Survival controls without granting flight");
        var body=body(c,p);c.assertTrue(body!=null&&body.position().distanceToSqr(launch)<1e-8,"Body must be exactly where the lunge began");
        c.assertTrue(p.getMainHandItem().is(AccelerationMod.SPEAR)&&p.containerMenu==p.inventoryMenu,"Projection must keep the held spear and real inventory");
        c.assertTrue(body.getProfile().partialProfile().id().equals(p.getUUID()),"Body must use the owner's actual skin/profile");
        p.setPos(launch.add(10,3,0));
        c.assertTrue(body.position().distanceToSqr(launch)<1e-8,"Body must remain behind when the soul moves");
        c.assertTrue(SoulMode.finish(p,null)&&!p.isSpectator()&&p.position().distanceToSqr(launch)<1e-8,"Return must restore Survival at the original body");
        c.assertTrue(body.isRemoved()&&!p.getAbilities().mayfly&&!p.getAbilities().invulnerable,"Body and temporary flight abilities must be cleaned up");c.succeed();
    }
    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:soul_strike_pops_exactly_one_totem_and_teleports")
    public void soulStrikePopsExactlyOneTotemAndTeleports(GameTestHelper c){
        setup(c);var actor=player(c);var target=player(c);target.setPos(actor.position().add(2,0,0));
        target.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);target.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.TOTEM_OF_UNDYING));
        c.runAtTickTime(5,()->{
            Vec3 launch=actor.position();c.assertTrue(SoulMode.begin(actor,launch),"Projection must begin");
            var body=body(c,actor);actor.setYRot(-90);actor.setXRot(0);
            Vec3 nearby=target.position();target.setPos(launch.add(7,0,0));
            c.assertTrue(!SoulMode.strike(actor,target)&&SoulMode.active(actor),"Right-click strike must require normal interaction reach, not remote aiming");target.setPos(nearby);
            c.assertTrue(net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.invoker().interact(actor,c.getLevel(),net.minecraft.world.InteractionHand.MAIN_HAND,target,null)==net.minecraft.world.InteractionResult.SUCCESS,"Right-click interaction must land the hit");
            c.assertTrue(target.isAlive()&&target.getHealth()==1&&target.getOffhandItem().isEmpty(),"Exactly one normal totem must activate");
            c.assertTrue(!actor.isSpectator()&&!SoulMode.active(actor)&&actor.distanceToSqr(target)<9,"Attacker must materialize beside the target");
            c.assertTrue(body.isRemoved(),"Old body must disappear after a committed strike");
            c.assertTrue(!SoulMode.strike(actor,target)&&target.getHealth()==1,"Repeated action must not cause another hit after projection ends");
            c.assertTrue(!actor.getCooldowns().isOnCooldown(actor.getMainHandItem()),"Returning must not add an artificial ability timer");c.succeed();
        });
    }
    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:soul_strike_kills_without_totem_through_armor")
    public void soulStrikeKillsWithoutTotemThroughArmor(GameTestHelper c){
        setup(c);var actor=player(c);var target=player(c);target.setPos(actor.position().add(2,0,0));
        var protection=c.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION);
        var slots=new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
        var items=new Item[]{Items.NETHERITE_HELMET,Items.NETHERITE_CHESTPLATE,Items.NETHERITE_LEGGINGS,Items.NETHERITE_BOOTS};
        for(int i=0;i<4;i++){var armor=new ItemStack(items[i]);armor.enchant(protection,4);target.setItemSlot(slots[i],armor);}
        target.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
        c.runAtTickTime(5,()->{
            target.doTick();c.assertTrue(target.getArmorValue()==20,"Fixture must wear real netherite armor");
            c.assertTrue(SoulMode.begin(actor,actor.position())&&net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.invoker().interact(actor,c.getLevel(),net.minecraft.world.InteractionHand.MAIN_HAND,target,null)==net.minecraft.world.InteractionResult.SUCCESS,"Right-click must immediately teleport and land the lethal strike");
            c.assertTrue(target.isDeadOrDying(),"A target without a totem must die through Protection IV netherite");c.succeed();
        });
    }
    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:hitting_body_returns_owner_and_forwards_damage_once")
    public void hittingBodySharesRealHealthWithoutRecallOrModeChange(GameTestHelper c){
        setup(c);var owner=player(c);Vec3 launch=owner.position();
        c.runAtTickTime(5,()->{
            c.assertTrue(SoulMode.begin(owner,launch),"Projection must begin");var body=body(c,owner);
            owner.setPos(launch.add(10,4,0));body.hurtServer(c.getLevel(),c.getLevel().damageSources().generic(),4);
            c.assertTrue(SoulMode.active(owner)&&owner.gameMode()==GameType.SURVIVAL&&owner.position().distanceToSqr(launch.add(10,4,0))<1e-8,"Body damage must preserve projection, position and Survival mode");
            c.assertTrue(owner.getHealth()==16,"Damage must be forwarded exactly once, after removing spectator invulnerability; health="+owner.getHealth());
            c.assertTrue(!body.isRemoved()&&body.getHealth()==owner.getHealth(),"Body must remain linked to real player health");
            SoulMode.finish(owner,owner.position());c.assertTrue(owner.gameMode()==GameType.SURVIVAL&&!owner.getAbilities().invulnerable&&!owner.getAbilities().instabuild,"Reuniting must not give Creative powers");c.succeed();
        });
    }
    @GameTest(maxTicks=240,padding=32,environment="acceleration_gametest:projection_expires_and_restores_original_mode")
    public void projectionHasNoTimerAndLungeReunitesAfterForwardMovement(GameTestHelper c){
        setup(c);var p=player(c);Vec3 origin=p.position();
        c.runAtTickTime(5,()->{p.setGameMode(GameType.ADVENTURE);c.assertTrue(SoulMode.begin(p,origin),"Adventure projection must begin");p.setPos(origin.add(15,5,0));});
        c.runAtTickTime(204,()->c.assertTrue(SoulMode.active(p),"Projection must still exist at 199 elapsed ticks"));
        c.runAtTickTime(207,()->{c.assertTrue(SoulMode.active(p)&&p.gameMode()==GameType.ADVENTURE,"Projection must remain active beyond the old ten-second timeout");
            p.setShiftKeyDown(true);SoulMode.tick(p);c.assertTrue(SoulMode.active(p),"Sneaking must not end projection");p.setShiftKeyDown(false);
            Vec3 momentum=new Vec3(.3,0,0);p.setDeltaMovement(momentum);
            p.getMainHandItem().get(DataComponents.PIERCING_WEAPON).attack(p,EquipmentSlot.MAINHAND);
            c.assertTrue(SoulMode.active(p)&&p.getDeltaMovement().lengthSqr()>momentum.lengthSqr(),"Ghost lunge must apply its real impulse and leave the body behind while moving");});
        Vec3 endpoint=origin.add(22,5,0);
        c.runAtTickTime(209,()->{p.setPos(endpoint);p.setDeltaMovement(new Vec3(.4,.1,0));});
        c.runAtTickTime(216,()->{
            c.assertTrue(!SoulMode.active(p)&&p.gameMode()==GameType.ADVENTURE&&p.position().distanceToSqr(endpoint)<1e-8,"Body must reunite at the actual post-lunge position, not the lunge start");
            c.assertTrue(p.getDeltaMovement().distanceToSqr(new Vec3(.4,.1,0))<1e-8,"Reuniting after the lunge must preserve momentum without suddenly stopping");
            c.assertTrue(!p.getAbilities().mayfly&&!p.getAbilities().flying,"Explicit cleanup must restore original flight abilities");c.succeed();});
    }
    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:soul_radius_and_missing_spear_force_safe_return")
    public void projectionHasNoTetherAndMissingSpearReturnsSafely(GameTestHelper c){
        setup(c);var p=player(c);Vec3 origin=p.position();SoulMode.begin(p,origin);
        p.setPos(origin.add(129,0,0));SoulMode.tick(p);
        c.assertTrue(SoulMode.active(p)&&p.position().distanceToSqr(origin.add(129,0,0))<1e-8,"Projection must not have the old distance tether");
        SoulMode.finish(p,null);
        p.getCooldowns().removeCooldown(p.getCooldowns().getCooldownGroup(p.getMainHandItem()));SoulMode.begin(p,origin);p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);SoulMode.tick(p);
        c.assertTrue(!SoulMode.active(p)&&!p.isSpectator(),"Losing the spear must restore the body rather than leave spectator powers");c.succeed();
    }
    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:dragon_reward_is_above_egg_and_unique_after_save")
    public void dragonRewardIsAboveEggAndUniqueAfterSave(GameTestHelper c){
        setup(c);BlockPos egg=c.absolutePos(new BlockPos(5,2,5));c.setBlock(5,2,5,Blocks.DRAGON_EGG);UUID dragon=UUID.randomUUID();
        c.assertTrue(DragonReward.drop(c.getLevel(),dragon,egg.below()),"Dragon victory must drop the spear");
        var drops=c.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(egg).inflate(5),e->e.getItem().is(AccelerationMod.SPEAR));
        c.assertTrue(drops.size()==1&&drops.getFirst().getY()>egg.getY()+1,"Reward must spawn on top of the actual dragon egg");
        c.assertTrue(!DragonReward.drop(c.getLevel(),dragon,egg.below()),"Repeated event must not duplicate the same dragon's reward");
        var state=SoulState.get(c.getLevel().getServer());var encoded=SoulState.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,state).getOrThrow();
        c.assertTrue(SoulState.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,encoded).getOrThrow().rewarded(dragon),"Rewarded dragon IDs must persist across saves");c.succeed();
    }
    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:projection_recovery_state_survives_serialization")
    public void projectionRecoveryStateSurvivesSerialization(GameTestHelper c){
        setup(c);var p=player(c);var origin=p.position();SoulMode.begin(p,origin);
        var state=SoulState.get(c.getLevel().getServer());var encoded=SoulState.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,state).getOrThrow();
        var restored=SoulState.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,encoded).getOrThrow().projection(p.getUUID());
        c.assertTrue(restored!=null&&restored.anchor().distanceToSqr(origin)<1e-8&&restored.mode()==GameType.SURVIVAL&&!restored.abilities().mayFly(),"Crash-recovery snapshot must preserve exact body anchor, original mode and abilities");
        c.assertTrue(SoulMode.finish(p,null)&&p.gameMode()==GameType.SURVIVAL&&!p.getAbilities().invulnerable,"Reconnect/stop recovery path must restore normal play");c.succeed();
    }
    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:creative_targets_and_disabled_pvp_are_protected")
    public void creativeTargetsAndDisabledPvpAreProtected(GameTestHelper c){
        setup(c);var actor=player(c);var target=player(c);target.setPos(actor.position().add(2,0,0));target.setGameMode(GameType.CREATIVE);
        SoulMode.begin(actor,actor.position());c.assertTrue(!SoulMode.strike(actor,target)&&SoulMode.active(actor),"Creative targets must not be killed");
        target.setGameMode(GameType.SURVIVAL);c.getLevel().getGameRules().set(net.minecraft.world.level.gamerules.GameRules.PVP,false,c.getLevel().getServer());
        c.assertTrue(!SoulMode.strike(actor,target)&&target.getHealth()==20,"Disabled PvP must prevent soul strike");
        c.getLevel().getGameRules().set(net.minecraft.world.level.gamerules.GameRules.PVP,true,c.getLevel().getServer());SoulMode.finish(actor,null);c.succeed();
    }
    @GameTest(maxTicks=100,padding=64,environment="acceleration_gametest:actual_dragon_victory_hook_rewards_only_tracked_dragon")
    public void actualDragonVictoryHookRewardsOnlyTrackedDragon(GameTestHelper c){
        setup(c);
        var dragon=EntityTypes.ENDER_DRAGON.create(c.getLevel(),EntitySpawnReason.COMMAND);
        var exit=c.absolutePos(new BlockPos(7,2,7));
        var fight=new net.minecraft.world.level.dimension.end.EnderDragonFight(false,false,false,Optional.empty(),0,
            Optional.of(dragon.getUUID()),Optional.of(exit),List.of(0),List.of());
        fight.init(c.getLevel(),4827,exit);
        var unrelated=EntityTypes.ENDER_DRAGON.create(c.getLevel(),EntitySpawnReason.COMMAND);
        fight.setDragonKilled(unrelated);
        c.assertTrue(!SoulState.get(c.getLevel().getServer()).rewarded(unrelated.getUUID()),"An unrelated summoned dragon must not reward a victory");
        fight.setDragonKilled(dragon);
        var rewards=c.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(exit).inflate(24),e->e.getItem().is(AccelerationMod.SPEAR));
        c.assertTrue(rewards.size()==1&&SoulState.get(c.getLevel().getServer()).rewarded(dragon.getUUID()),"The real EnderDragonFight callback must drop one blessed spear after portal/egg creation");
        var reward=rewards.getFirst();
        c.assertTrue(c.getLevel().getBlockState(reward.blockPosition().below()).is(Blocks.DRAGON_EGG),"Real victory reward must be directly above the newly generated dragon egg");
        c.succeed();
    }

    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:body_totem_and_death_share_owner_health")
    public void bodyTotemAndDeathShareOwnerHealth(GameTestHelper c){
        setup(c);var owner=player(c);Vec3 anchor=owner.position();
        c.runAtTickTime(5,()->{
            owner.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.TOTEM_OF_UNDYING));
            SoulMode.begin(owner,anchor);var body=body(c,owner);owner.setPos(anchor.add(10,4,0));
            c.assertTrue(body.getType()==AccelerationMod.BODY,"Body must be the dedicated linked avatar, not a mannequin NPC");
            body.hurtServer(c.getLevel(),c.getLevel().damageSources().generic(),1000);
            c.assertTrue(owner.isAlive()&&owner.getHealth()==1&&owner.getOffhandItem().isEmpty(),"A body hit must consume the real player's totem exactly once");
            c.assertTrue(SoulMode.active(owner)&&!body.isRemoved()&&body.getHealth()==1,"A saved player must remain projected with shared body health");
            owner.invulnerableTime=0;
            body.hurtServer(c.getLevel(),c.getLevel().damageSources().generic(),1000);
            c.assertTrue(owner.isDeadOrDying()&&!SoulMode.active(owner)&&body.isRemoved(),"A lethal body hit must kill the real owner and clean up without dropping body equipment");
            c.assertTrue(owner.gameMode()==GameType.SURVIVAL&&!owner.getAbilities().instabuild,"Death must not switch to Creative");c.succeed();
        });
    }
    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:body_self_hit_and_current_mode_are_preserved")
    public void bodySelfHitAndCurrentModeArePreserved(GameTestHelper c){
        setup(c);var owner=player(c);Vec3 anchor=owner.position();
        c.runAtTickTime(5,()->{
            SoulMode.begin(owner,anchor);var body=body(c,owner);owner.setPos(anchor.add(10,4,0));
            owner.getInventory().setItem(20,owner.getMainHandItem().copy());owner.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            owner.setPos(anchor.add(1,0,0));
            c.assertTrue(net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.invoker().interact(owner,c.getLevel(),net.minecraft.world.InteractionHand.MAIN_HAND,body,null)==net.minecraft.world.InteractionResult.PASS,"Normal left-click must be permitted on your own body");
            owner.attack(body);
            c.assertTrue(owner.getHealth()<20&&!SoulMode.active(owner)&&body.isRemoved()&&owner.position().distanceToSqr(anchor)<1e-8,"Your own vanilla punch must damage shared health and return you to your body");
            owner.setHealth(20);owner.invulnerableTime=0;owner.setItemSlot(EquipmentSlot.MAINHAND,AccelerationSpear.create(c.getLevel()));
            SoulMode.begin(owner,anchor);body=body(c,owner);owner.setPos(anchor.add(1,0,0));
            body.hurtServer(c.getLevel(),c.getLevel().damageSources().playerAttack(owner),4);
            c.assertTrue(owner.getHealth()==16&&!SoulMode.active(owner)&&owner.gameMode()==GameType.SURVIVAL&&owner.position().distanceToSqr(anchor)<1e-8,"Only your own direct hit must recall you, applying damage exactly once");
            owner.invulnerableTime=0;SoulMode.begin(owner,anchor);body=body(c,owner);owner.setPos(anchor.add(1,0,0));
            var attacker=player(c);body.hurtServer(c.getLevel(),c.getLevel().damageSources().playerAttack(attacker),4);
            c.assertTrue(owner.getHealth()==12&&SoulMode.active(owner)&&owner.position().distanceToSqr(anchor.add(1,0,0))<1e-8,"Another player's hit must damage you without recalling you");
            c.assertTrue(net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.invoker().interact(owner,c.getLevel(),net.minecraft.world.InteractionHand.MAIN_HAND,body,null)==net.minecraft.world.InteractionResult.SUCCESS,"Right-clicking your own body with the spear must also recall you, not lethally strike yourself");
            c.assertTrue(!SoulMode.active(owner)&&owner.getHealth()==12&&owner.position().distanceToSqr(anchor)<1e-8,"Self right-click must return to the body without the instant-kill hit");
            owner.setItemSlot(EquipmentSlot.MAINHAND,AccelerationSpear.create(c.getLevel()));owner.setGameMode(GameType.CREATIVE);SoulMode.begin(owner,owner.position());owner.setGameMode(GameType.SURVIVAL);
            SoulMode.finish(owner,owner.position());
            c.assertTrue(owner.gameMode()==GameType.SURVIVAL&&!owner.getAbilities().instabuild&&!owner.getAbilities().invulnerable&&!owner.getAbilities().mayfly,"A stale Creative snapshot must never override current Survival mode or abilities");c.succeed();
        });
    }

    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:projection_has_no_flight_or_contact_pushing")
    public void projectionHasNoFlightOrContactPushing(GameTestHelper c){
        setup(c);var owner=player(c);var other=player(c);other.setPos(owner.position().add(.4,0,0));
        var cow=c.spawn(EntityTypes.COW,new BlockPos(3,2,2));
        SoulMode.begin(owner,owner.position());
        c.assertTrue(owner.gameMode()==GameType.SURVIVAL&&!owner.getAbilities().mayfly&&!owner.getAbilities().flying&&!owner.getAbilities().invulnerable,"Projection must never grant flight or Creative powers");
        owner.setDeltaMovement(Vec3.ZERO);other.setDeltaMovement(Vec3.ZERO);cow.setDeltaMovement(Vec3.ZERO);
        owner.push(other);other.push(owner);owner.push(cow);cow.push(owner);
        c.assertTrue(owner.getDeltaMovement().lengthSqr()==0&&other.getDeltaMovement().lengthSqr()==0&&cow.getDeltaMovement().lengthSqr()==0,"Projected player and contacted players/mobs must not push one another");
        c.assertTrue(!owner.isPushable()&&!owner.canCollideWith(other)&&!cow.canCollideWith(owner),"Server and synced client contact physics must treat projection as non-colliding");
        SoulMode.finish(owner,owner.position());other.push(owner);
        c.assertTrue(owner.isPushable()&&owner.getDeltaMovement().lengthSqr()>0,"Ordinary player pushing must resume after projection ends");c.succeed();
    }

    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:ghost_ignores_warden_but_body_remains_mortal")
    public void ghostIgnoresWardenButBodyRemainsMortal(GameTestHelper c){
        setup(c);var owner=player(c);var warden=c.spawn(EntityTypes.WARDEN,new BlockPos(5,2,5));
        var zombie=c.spawn(EntityTypes.ZOMBIE,new BlockPos(6,2,6));zombie.setTarget(owner);
        c.runAtTickTime(5,()->{
            SoulMode.begin(owner,owner.position());var body=body(c,owner);
            c.assertTrue(zombie.getTarget()==null,"Starting projection must clear existing mob targeting");
            zombie.setTarget(owner);c.assertTrue(zombie.getTarget()==null,"Mobs must not acquire the ghost as a target");
            c.assertTrue(!warden.canTargetEntity(owner)&&warden.canTargetEntity(body),"Warden must ignore the ghost while still being able to target the mortal linked body");
            float health=owner.getHealth();
            c.assertTrue(!owner.hurtServer(c.getLevel(),c.getLevel().damageSources().sonicBoom(warden),10)&&owner.getHealth()==health,"Warden sonic boom must not directly hurt the ghost");
            float mobHealth=zombie.getHealth();
            c.assertTrue(zombie.hurtServer(c.getLevel(),c.getLevel().damageSources().playerAttack(owner),4)&&zombie.getHealth()<mobHealth,"Ghost must be able to deal ordinary attack damage");
            c.assertTrue(net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.invoker().interact(owner,c.getLevel(),net.minecraft.world.InteractionHand.MAIN_HAND,zombie,null)==net.minecraft.world.InteractionResult.PASS,"Ordinary ghost attacks must be allowed");
            var held=owner.getMainHandItem();owner.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            c.assertTrue(net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.invoker().interact(owner,c.getLevel(),net.minecraft.world.InteractionHand.MAIN_HAND,zombie,null)==net.minecraft.world.InteractionResult.FAIL,"Ordinary right-click entity interactions must be blocked");owner.setItemSlot(EquipmentSlot.MAINHAND,held);
            body.hurtServer(c.getLevel(),c.getLevel().damageSources().sonicBoom(warden),4);
            c.assertTrue(owner.getHealth()==health-4&&SoulMode.active(owner),"A Warden hit on the body must damage the shared real health without recall");
            SoulMode.finish(owner,owner.position());c.succeed();
        });
    }

    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:ghost_hunger_eating_and_materializing_actions")
    public void ghostHungerEatingAndMaterializingActions(GameTestHelper c){
        setup(c);var owner=player(c);SoulMode.begin(owner,owner.position());var food=owner.getFoodData();food.setFoodLevel(10);food.setSaturation(0);
        owner.causeFoodExhaustion(40);for(int i=0;i<200;i++)food.tick(owner);
        c.assertTrue(food.getFoodLevel()==10&&owner.getHealth()==20,"Hunger and health must not drain during projection");
        owner.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.APPLE));
        net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.invoker().interact(owner,c.getLevel(),net.minecraft.world.InteractionHand.MAIN_HAND);
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.invoker().interact(owner,c.getLevel(),net.minecraft.world.InteractionHand.MAIN_HAND,new BlockHitResult(owner.position(),Direction.UP,owner.blockPosition(),false));
        food.eat(4,.3F);
        c.assertTrue(SoulMode.active(owner)&&food.getFoodLevel()==14,"Eating must still work without bringing back the body");
        Vec3 position=owner.position().add(6,0,0);owner.setPos(position);owner.setDeltaMovement(new Vec3(.4,0,0));
        owner.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.STONE));
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.invoker().interact(owner,c.getLevel(),net.minecraft.world.InteractionHand.MAIN_HAND,new BlockHitResult(position,Direction.UP,BlockPos.containing(position),false));
        c.assertTrue(!SoulMode.active(owner)&&owner.position().distanceToSqr(position)<1e-8&&owner.getDeltaMovement().x==.4,"A block action must reunite at the ghost's current position without a sudden momentum stop");
        owner.setItemSlot(EquipmentSlot.MAINHAND,AccelerationSpear.create(c.getLevel()));SoulMode.begin(owner,owner.position());owner.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.SNOWBALL));
        net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.invoker().interact(owner,c.getLevel(),net.minecraft.world.InteractionHand.MAIN_HAND);
        c.assertTrue(!SoulMode.active(owner),"Using a non-food, non-spear item must materialize the player");c.succeed();
    }

    private static final class LandingPearl extends net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl {
        LandingPearl(ServerLevel world){super(EntityTypes.ENDER_PEARL,world);}
        void land(Vec3 point){setPos(point);setOldPosAndRot();onHit(new BlockHitResult(point,Direction.UP,BlockPos.containing(point).below(),false));}
    }
    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:pearl_only_materializes_after_successful_landing")
    public void pearlOnlyMaterializesAfterSuccessfulLanding(GameTestHelper c){
        setup(c);var owner=player(c);var anchor=owner.position();
        c.runAtTickTime(5,()->{
            SoulMode.begin(owner,anchor);var body=body(c,owner);
            owner.getInventory().setItem(20,owner.getMainHandItem().copy());owner.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.ENDER_PEARL));
            net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.invoker().interact(owner,c.getLevel(),net.minecraft.world.InteractionHand.MAIN_HAND);
            c.assertTrue(SoulMode.active(owner)&&!body.isRemoved(),"Throwing a pearl must keep the player in ghost mode with the body behind");
            Vec3 point=anchor.add(8,0,0);var pearl=new LandingPearl(c.getLevel());pearl.setOwner(owner);c.getLevel().addFreshEntity(pearl);pearl.land(point);
            c.assertTrue(!SoulMode.active(owner)&&body.isRemoved()&&owner.position().distanceToSqr(point)<1e-8,"The real native pearl teleport must reunite the body at the landing point, not the launch point");
            c.assertTrue(owner.gameMode()==GameType.SURVIVAL&&!owner.getAbilities().flying&&owner.getHealth()==15,"After landing the player must be mortal Survival again, including normal pearl damage");c.succeed();
        });
    }

    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:wind_charges_are_usable_without_materializing")
    public void windChargesAreUsableWithoutMaterializing(GameTestHelper c){
        setup(c);var owner=player(c);SoulMode.begin(owner,owner.position());var body=body(c,owner);
        owner.getInventory().setItem(20,owner.getMainHandItem().copy());owner.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.WIND_CHARGE,2));
        var hand=net.minecraft.world.InteractionHand.MAIN_HAND;
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.invoker().interact(owner,c.getLevel(),hand,new BlockHitResult(owner.position(),Direction.UP,owner.blockPosition().below(),false));
        net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.invoker().interact(owner,c.getLevel(),hand);
        c.assertTrue(SoulMode.active(owner)&&!body.isRemoved(),"Using wind charges in air or while looking at blocks must leave the body behind");
        owner.setXRot(90);owner.getMainHandItem().use(c.getLevel(),owner,hand);
        var charges=c.getLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge.class,owner.getBoundingBox().inflate(8),charge->charge.getOwner()==owner);
        c.assertTrue(charges.size()==1&&owner.getMainHandItem().getCount()==1&&SoulMode.active(owner),"A real wind-charge item use must launch and consume a charge without materializing");
        c.runAtTickTime(10,()->{
            c.assertTrue(SoulMode.active(owner)&&!body.isRemoved(),"Wind charge impact must not recall the body");
            c.assertTrue(owner.getDeltaMovement().lengthSqr()>0,"Native wind-charge knockback must still move the ghost for wind jumps");
            SoulMode.finish(owner,owner.position());c.succeed();
        });
    }

    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:right_click_dragon_part_is_an_instant_lethal_strike")
    public void rightClickDragonPartIsAnInstantLethalStrike(GameTestHelper c){
        setup(c);var owner=player(c);
        c.runAtTickTime(5,()->{
            var dragon=EntityTypes.ENDER_DRAGON.create(c.getLevel(),EntitySpawnReason.COMMAND);dragon.setPos(owner.position().add(2,0,0));
            dragon.head.setPos(owner.position().add(2,0,0));
            dragon.getPhaseManager().setPhase(net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.SITTING_SCANNING);
            SoulMode.begin(owner,owner.position());
            var result=net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.invoker().interact(owner,c.getLevel(),net.minecraft.world.InteractionHand.MAIN_HAND,dragon.head,null);
            c.assertTrue(result==net.minecraft.world.InteractionResult.SUCCESS&&!SoulMode.active(owner)&&dragon.isDeadOrDying(),"Right-clicking a dragon's actual hitbox must materialize and lethally strike the boss in that interaction");c.succeed();
        });
    }

    private static final class SelfWindCharge extends net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge {
        SelfWindCharge(ServerLevel world){super(EntityTypes.WIND_CHARGE,world);}
        void burst(Vec3 position){setPos(position);explode(position);}
    }
    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:self_wind_bursts_stack_upward_without_body_recall")
    public void selfWindBurstsStackUpwardWithoutBodyRecall(GameTestHelper c){
        setup(c);var owner=player(c);var attacker=player(c);Vec3 anchor=owner.position();
        c.runAtTickTime(5,()->{
            SoulMode.begin(owner,anchor);var body=body(c,owner);Vec3 ghostPosition=anchor.add(8,0,0);owner.setPos(ghostPosition);
            owner.setDeltaMovement(new Vec3(.3,.7,-.2));
            var first=new SelfWindCharge(c.getLevel());first.setOwner(owner);first.burst(ghostPosition.add(0,.1,0));
            double one=owner.getDeltaMovement().y;
            var second=new SelfWindCharge(c.getLevel());second.setOwner(owner);second.burst(ghostPosition.add(0,.1,0));
            double two=owner.getDeltaMovement().y;
            var third=new SelfWindCharge(c.getLevel());third.setOwner(owner);third.burst(ghostPosition.add(0,.1,0));
            double three=owner.getDeltaMovement().y;
            c.assertTrue(one>.7&&two>one&&three>two,"Each actual self wind-charge explosion must add upward velocity to the previous velocity");
            c.assertTrue(Math.abs(owner.getDeltaMovement().x-.3)<1e-6&&Math.abs(owner.getDeltaMovement().z+.2)<1e-6,"Self launches must preserve existing sideways momentum");
            c.assertTrue(SoulMode.active(owner)&&!body.isRemoved()&&owner.position().distanceToSqr(ghostPosition)<1e-8,"Stacking wind charges must leave the body behind and never teleport the ghost");
            body.hurtServer(c.getLevel(),c.getLevel().damageSources().playerAttack(attacker),4);
            c.assertTrue(owner.getHealth()==16&&SoulMode.active(owner)&&owner.position().distanceToSqr(ghostPosition)<1e-8,"Another player's body hit must still damage the owner without recall or teleportation");
            Vec3 ordinary=new Vec3(.1,.2,.3);
            c.assertTrue(GhostWind.impulse(null,owner,ordinary).equals(Vec3.ZERO),"Ordinary explosions must not push the untouchable ghost");
            var otherCharge=new SelfWindCharge(c.getLevel());otherCharge.setOwner(attacker);
            c.assertTrue(GhostWind.impulse(otherCharge,owner,ordinary).equals(Vec3.ZERO),"Another player's wind charges must not push the ghost");
            Vec3 beforeOtherBlast=owner.getDeltaMovement();otherCharge.burst(ghostPosition.add(0,.1,0));
            c.assertTrue(owner.getDeltaMovement().distanceToSqr(beforeOtherBlast)<1e-8,"An actual other-player wind-charge explosion must leave ghost velocity unchanged");
            SoulMode.finish(owner,owner.position());
            c.assertTrue(GhostWind.impulse(first,owner,ordinary).equals(ordinary),"Extra launch power must stop after ghost mode ends");c.succeed();
        });
    }

    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:lunges_cost_no_hunger_in_body_or_ghost_mode")
    public void lungesCostNoHungerInBodyOrGhostMode(GameTestHelper c){
        setup(c);var owner=player(c);var food=owner.getFoodData();food.setFoodLevel(10);food.setSaturation(5);
        for(int i=0;i<10;i++){
            owner.getMainHandItem().get(DataComponents.PIERCING_WEAPON).attack(owner,EquipmentSlot.MAINHAND);
            c.assertTrue(SoulMode.active(owner),"A native body-mode lunge must still activate the projection");
            SoulMode.finish(owner,owner.position());food.tick(owner);
            c.assertTrue(food.getFoodLevel()==10&&food.getSaturationLevel()==5,"Lunging from body mode must not accumulate exhaustion or consume saturation/hunger");
        }
        SoulMode.begin(owner,owner.position());
        for(int i=0;i<10;i++)owner.getMainHandItem().get(DataComponents.PIERCING_WEAPON).attack(owner,EquipmentSlot.MAINHAND);
        SoulMode.finish(owner,owner.position());for(int i=0;i<10;i++)food.tick(owner);
        c.assertTrue(food.getFoodLevel()==10&&food.getSaturationLevel()==5,"Ghost lunges must not store hunger costs that appear after reuniting");
        owner.causeFoodExhaustion(5);food.tick(owner);
        c.assertTrue(food.getSaturationLevel()==4,"Ordinary non-lunge body-mode exhaustion must still work");c.succeed();
    }


    @GameTest(maxTicks=100,padding=32,environment="acceleration_gametest:ghost_can_attack_while_hidden_and_untouchable")
    public void ghostSuccessfulAttacksReuniteAtCurrentPosition(GameTestHelper c){
        setup(c);var owner=player(c);var observer=player(c);
        c.runAtTickTime(10,()->{
            Vec3 anchor=owner.position();SoulMode.begin(owner,anchor);var body=body(c,owner);
            Vec3 ghostPosition=anchor.add(8,0,0);owner.setPos(ghostPosition);observer.setPos(ghostPosition.add(2,0,0));
            owner.getInventory().setItem(20,owner.getMainHandItem().copy());owner.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.NETHERITE_SWORD));
            float health=owner.getHealth();observer.attack(owner);
            c.assertTrue(owner.getHealth()==health&&SoulMode.active(owner),"Opponents must be unable to damage the ghost before its own action");
            c.assertTrue(!owner.isPickable()&&!owner.isPushable(),"Ghost must not be selectable or physically pushable");
            c.assertTrue(net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.invoker().interact(observer,c.getLevel(),net.minecraft.world.InteractionHand.MAIN_HAND,owner,null)==net.minecraft.world.InteractionResult.FAIL,"Incoming attacks must be rejected");
            c.assertTrue(net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.invoker().interact(observer,c.getLevel(),net.minecraft.world.InteractionHand.MAIN_HAND,owner,null)==net.minecraft.world.InteractionResult.FAIL,"Incoming right-clicks must be rejected");
            c.assertTrue(ProjectionPhysics.hiddenFrom(owner,observer)&&!ProjectionPhysics.hiddenFrom(owner,owner)&&!ProjectionPhysics.hiddenFrom(body,observer)&&body.isPickable(),"Ghost must be hidden from others while its body remains visible and vulnerable");
            var cow=c.spawn(EntityTypes.COW,new BlockPos(3,2,2));cow.setPos(ghostPosition.add(0,0,1.5));float before=cow.getHealth();
            owner.setDeltaMovement(new Vec3(.4,0,0));owner.attack(cow);
            c.assertTrue(cow.getHealth()<before&&!SoulMode.active(owner)&&body.isRemoved(),"A native successful sword hit must damage the mob and reunite the body");
            c.assertTrue(owner.position().distanceToSqr(ghostPosition)<1e-8&&owner.getDeltaMovement().x==.4,"Hit recall must bring the body to the ghost and preserve momentum, not return to the old anchor");
            c.assertTrue(owner.isPickable()&&!ProjectionPhysics.hiddenFrom(owner,observer),"Visibility and normal targeting must resume after the hit");
            owner.setItemSlot(EquipmentSlot.MAINHAND,AccelerationSpear.create(c.getLevel()));SoulMode.begin(owner,owner.position());
            owner.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.NETHERITE_SWORD));owner.resetOnlyAttackStrengthTicker();owner.attack(observer);
            c.assertTrue(observer.getHealth()<20&&!SoulMode.active(owner),"Successful native hits on players must also reunite the body immediately");c.succeed();
        });
    }
    @GameTest(maxTicks=100,padding=32,environment="acceleration_gametest:normal_spear_hit_recall_does_not_restart_projection")
    public void normalSpearHitRecallDoesNotRestartProjection(GameTestHelper c){
        setup(c);var owner=player(c);var target=player(c);
        c.runAtTickTime(10,()->{
            Vec3 anchor=owner.position();SoulMode.begin(owner,anchor);var body=body(c,owner);
            Vec3 strikePosition=anchor.add(8,0,0);owner.setPos(strikePosition);target.setPos(strikePosition.add(0,0,2));owner.setYRot(0);owner.setXRot(0);
            owner.getMainHandItem().get(DataComponents.PIERCING_WEAPON).attack(owner,EquipmentSlot.MAINHAND);
            c.assertTrue(target.getHealth()<20&&!SoulMode.active(owner)&&body.isRemoved(),"A real native spear jab must reunite on a hit and its post-hit Lunge must not create another ghost in the same action");
            c.assertTrue(owner.position().distanceToSqr(strikePosition)<1e-8,"Normal spear hit must bring the body to the current strike position");c.succeed();
        });
    }


    private static final class StrikeStand extends net.minecraft.world.entity.decoration.ArmorStand {
        float received;
        StrikeStand(ServerLevel world){super(EntityTypes.ARMOR_STAND,world);}
        @Override public boolean hurtServer(ServerLevel world,net.minecraft.world.damagesource.DamageSource source,float amount){received=amount;return super.hurtServer(world,source,amount);}
    }
    @GameTest(maxTicks=100,padding=32,environment="acceleration_gametest:maximum_damage_strikes_damageable_entities_and_bosses")
    public void maximumDamageStrikesDamageableEntitiesAndBosses(GameTestHelper c){
        setup(c);var owner=player(c);
        c.runAtTickTime(5,()->{
            var stand=new StrikeStand(c.getLevel());stand.setPos(owner.position().add(2,0,0));c.getLevel().addFreshEntity(stand);
            SoulMode.begin(owner,owner.position());
            c.assertTrue(SoulMode.strike(owner,stand)&&stand.isRemoved()&&stand.received==Float.MAX_VALUE,"Stand strike must receive maximum finite damage and destroy it immediately");
            var cart=c.spawn(EntityTypes.MINECART,new BlockPos(6,2,6));owner.setPos(cart.position().add(2,0,0));SoulMode.begin(owner,owner.position());
            c.assertTrue(SoulMode.strike(owner,cart)&&cart.isRemoved()&&!SoulMode.active(owner),"Right-click strike must destroy a damageable non-living minecart and materialize its attacker");
            var wither=c.spawn(EntityTypes.WITHER,new BlockPos(10,2,10));wither.setInvulnerableTicks(200);owner.setPos(wither.position().add(2,0,0));SoulMode.begin(owner,owner.position());
            c.assertTrue(SoulMode.strike(owner,wither)&&wither.isDeadOrDying(),"The strike must kill even a Wither during its spawn protection");c.succeed();
        });
    }

}
