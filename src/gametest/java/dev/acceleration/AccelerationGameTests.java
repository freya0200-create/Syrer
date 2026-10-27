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
import net.minecraft.world.entity.decoration.Mannequin;
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
    private Mannequin body(GameTestHelper c,ServerPlayer player){
        var session=SoulState.get(c.getLevel().getServer()).projection(player.getUUID());
        return (Mannequin)c.getLevel().getEntity(session.body());
    }
    @GameTest(maxTicks=60,padding=32,environment="acceleration_gametest:reward_has_all_max_enchantments_and_native_spear_components")
    public void rewardHasAllMaxEnchantmentsAndNativeSpearComponents(GameTestHelper c){
        setup(c);var stack=AccelerationSpear.create(c.getLevel());
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
        c.assertTrue(charged>base*3.9,"Movement attribute must actually receive the four-times multiplier");
        p.setSprinting(false);for(int i=0;i<25;i++)AccelerationMod.accelerationTick(p);
        c.assertTrue(AccelerationMod.charge(p)==0,"Stopping must decay acceleration");
        p.setSprinting(true);for(int i=0;i<50;i++)AccelerationMod.accelerationTick(p);p.getInventory().setItem(20,ItemStack.EMPTY);AccelerationMod.accelerationTick(p);
        c.assertTrue(AccelerationMod.charge(p)==0&&p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)<charged/2,"Removing spear must remove speed immediately");c.succeed();
    }
    @GameTest(maxTicks=60,padding=32,environment="acceleration_gametest:native_lunge_leaves_body_exactly_at_launch_position")
    public void nativeLungeLeavesBodyExactlyAtLaunchPosition(GameTestHelper c){
        setup(c);var p=player(c);p.setYRot(-90);p.getFoodData().setFoodLevel(20);
        Vec3 launch=p.position();p.getMainHandItem().get(DataComponents.PIERCING_WEAPON).attack(p,EquipmentSlot.MAINHAND);
        c.assertTrue(SoulMode.active(p)&&p.isSpectator(),"An actual native spear lunge must begin projection");
        var body=body(c,p);c.assertTrue(body!=null&&body.position().distanceToSqr(launch)<1e-8,"Body must be exactly where the lunge began");
        c.assertTrue(body.getProfile().partialProfile().id().equals(p.getUUID()),"Body must use the owner's actual skin/profile");
        p.setPos(launch.add(10,3,0));
        c.assertTrue(body.position().distanceToSqr(launch)<1e-8,"Body must remain behind when the soul moves");
        c.assertTrue(SoulMode.finish(p,null)&&!p.isSpectator()&&p.position().distanceToSqr(launch)<1e-8,"Return must restore Survival at the original body");
        c.assertTrue(body.isRemoved()&&!p.getAbilities().mayfly&&!p.getAbilities().invulnerable,"Body and spectator abilities must be cleaned up");c.succeed();
    }
    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:soul_strike_pops_exactly_one_totem_and_teleports")
    public void soulStrikePopsExactlyOneTotemAndTeleports(GameTestHelper c){
        setup(c);var actor=player(c);var target=player(c);target.setPos(actor.position().add(7,0,0));
        target.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);target.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.TOTEM_OF_UNDYING));
        c.runAtTickTime(5,()->{
            Vec3 launch=actor.position();c.assertTrue(SoulMode.begin(actor,launch),"Projection must begin");
            var body=body(c,actor);actor.setYRot(-90);actor.setXRot(0);
            c.assertTrue(SoulMode.strikeAimed(actor),"Actual server raycast must select and hit the aimed player");
            c.assertTrue(target.isAlive()&&target.getHealth()==1&&target.getOffhandItem().isEmpty(),"Exactly one normal totem must activate");
            c.assertTrue(!actor.isSpectator()&&!SoulMode.active(actor)&&actor.distanceToSqr(target)<9,"Attacker must materialize beside the target");
            c.assertTrue(body.isRemoved(),"Old body must disappear after a committed strike");
            c.assertTrue(!SoulMode.strike(actor,target)&&target.getHealth()==1,"Repeated action must not cause another hit after projection ends");
            c.assertTrue(actor.getCooldowns().isOnCooldown(actor.getMainHandItem()),"Projection must apply its cooldown");c.succeed();
        });
    }
    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:soul_strike_kills_without_totem_through_armor")
    public void soulStrikeKillsWithoutTotemThroughArmor(GameTestHelper c){
        setup(c);var actor=player(c);var target=player(c);target.setPos(actor.position().add(7,0,0));
        var protection=c.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION);
        var slots=new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
        var items=new Item[]{Items.NETHERITE_HELMET,Items.NETHERITE_CHESTPLATE,Items.NETHERITE_LEGGINGS,Items.NETHERITE_BOOTS};
        for(int i=0;i<4;i++){var armor=new ItemStack(items[i]);armor.enchant(protection,4);target.setItemSlot(slots[i],armor);}
        target.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
        c.runAtTickTime(5,()->{
            target.doTick();c.assertTrue(target.getArmorValue()==20,"Fixture must wear real netherite armor");
            c.assertTrue(SoulMode.begin(actor,actor.position())&&SoulMode.strike(actor,target),"Soul strike must land");
            c.assertTrue(target.isDeadOrDying(),"A target without a totem must die through Protection IV netherite");c.succeed();
        });
    }
    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:hitting_body_returns_owner_and_forwards_damage_once")
    public void hittingBodyReturnsOwnerAndForwardsDamageOnce(GameTestHelper c){
        setup(c);var owner=player(c);Vec3 launch=owner.position();
        c.runAtTickTime(5,()->{
            c.assertTrue(SoulMode.begin(owner,launch),"Projection must begin");var body=body(c,owner);
            owner.setPos(launch.add(10,4,0));body.hurtServer(c.getLevel(),c.getLevel().damageSources().generic(),4);
            c.assertTrue(!SoulMode.active(owner)&&!owner.isSpectator()&&owner.position().distanceToSqr(launch)<1e-8,"Hitting anchored body must recall owner");
            c.assertTrue(owner.getHealth()==16,"Damage must be forwarded exactly once, after removing spectator invulnerability; health="+owner.getHealth());
            c.assertTrue(body.isRemoved(),"Body must not remain as a duplicated gear source");c.succeed();
        });
    }
    @GameTest(maxTicks=240,padding=32,environment="acceleration_gametest:projection_expires_and_restores_original_mode")
    public void projectionExpiresAndRestoresOriginalMode(GameTestHelper c){
        setup(c);var p=player(c);Vec3 origin=p.position();
        c.runAtTickTime(5,()->{p.setGameMode(GameType.ADVENTURE);c.assertTrue(SoulMode.begin(p,origin),"Adventure projection must begin");p.setPos(origin.add(15,5,0));});
        c.runAtTickTime(204,()->c.assertTrue(SoulMode.active(p),"Projection must still exist at 199 elapsed ticks"));
        c.runAtTickTime(207,()->{c.assertTrue(!SoulMode.active(p)&&p.gameMode()==GameType.ADVENTURE&&p.position().distanceToSqr(origin)<1e-8,"Ten-second timeout must restore original mode and position");c.succeed();});
    }
    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:soul_radius_and_missing_spear_force_safe_return")
    public void soulRadiusAndMissingSpearForceSafeReturn(GameTestHelper c){
        setup(c);var p=player(c);Vec3 origin=p.position();SoulMode.begin(p,origin);
        p.setPos(origin.add(129,0,0));SoulMode.tick(p);
        c.assertTrue(!SoulMode.active(p)&&p.position().distanceToSqr(origin)<1e-8,"Soul radius must safely recall the player");
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
        setup(c);var actor=player(c);var target=player(c);target.setPos(actor.position().add(7,0,0));target.setGameMode(GameType.CREATIVE);
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

}
