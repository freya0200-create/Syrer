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
            SoulMode.finish(player,null);BlockPhasing.forget(player);player.level().removePlayerImmediately(player,Entity.RemovalReason.DISCARDED);
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
    private void chargeSpear(ServerPlayer player){
        player.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        if(!SoulMode.active(player)||!player.getMainHandItem().is(AccelerationMod.SPEAR))return;
        try{
            var remaining=LivingEntity.class.getDeclaredField("useItemRemaining");remaining.setAccessible(true);
            int delay=player.getMainHandItem().get(DataComponents.KINETIC_WEAPON).delayTicks();
            remaining.setInt(player,player.getUseItem().getUseDuration(player)-delay);
        }catch(ReflectiveOperationException error){throw new RuntimeException(error);}
    }
    private boolean chargedStrike(ServerPlayer player,Entity target){if(SoulMode.active(player))chargeSpear(player);return SoulMode.strike(player,target);}
    private net.minecraft.world.InteractionResult chargedInteract(ServerPlayer player,net.minecraft.world.level.Level world,net.minecraft.world.InteractionHand hand,Entity target,net.minecraft.world.phys.EntityHitResult hit){
        if(SoulMode.active(player)&&player.getMainHandItem().is(AccelerationMod.SPEAR)&&!SoulMode.ownBody(player,target))chargeSpear(player);
        return net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.invoker().interact(player,world,hand,target,hit);
    }
    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:ghost_spear_requires_full_charge_and_taps_do_no_damage")
    public void ghostSpearRequiresFullChargeAndTapsDoNoDamage(GameTestHelper c){
        setup(c);var owner=player(c);var target=player(c);target.setPos(owner.position().add(2,0,0));
        target.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);target.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.TOTEM_OF_UNDYING));
        SoulMode.begin(owner,owner.position());var linked=body(c,owner);
        net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.invoker().interact(owner,c.getLevel(),net.minecraft.world.InteractionHand.MAIN_HAND,target,null);
        c.assertTrue(owner.isUsingItem()&&!SpearCharge.ready(owner)&&target.getHealth()==20&&SoulMode.active(owner),"A quick right click on an entity must start charging without damage or reunion");
        c.assertTrue(!owner.stabAttack(EquipmentSlot.MAINHAND,target,0,false,true,false)&&target.getHealth()==20&&!linked.isRemoved(),"Uncharged contact must not deal instant damage or knock the target");
        for(int i=0;i<9;i++)owner.doTick();
        c.assertTrue(!SpearCharge.ready(owner)&&!SoulMode.strike(owner,target)&&target.getHealth()==20,"Nine charge ticks must not unlock the instant strike");
        owner.stopUsingItem();owner.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        c.assertTrue(!SpearCharge.ready(owner),"Releasing and re-pressing must restart the charge");
        for(int i=0;i<10;i++)owner.doTick();
        c.assertTrue(SpearCharge.ready(owner),"Ten actual native player ticks must fully charge the spear");
        Vec3 position=owner.position();
        c.assertTrue(SoulMode.strike(owner,target)&&target.getHealth()==1&&target.getOffhandItem().isEmpty()&&!SoulMode.active(owner)&&linked.isRemoved(),"A fully charged hit must pop one totem and return the body");
        c.assertTrue(owner.position().distanceToSqr(position)<1e-8,"Full-charge strike must retain smooth in-place reunion");c.succeed();
    }
    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:ghost_spear_has_density_and_breach_with_real_effects")
    public void ghostSpearHasDensityAndBreachWithRealEffects(GameTestHelper c){
        setup(c);var owner=player(c);var stack=owner.getMainHandItem();var registry=c.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        c.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(registry.getOrThrow(Enchantments.DENSITY),stack)==5&&EnchantmentHelper.getItemEnchantmentLevel(registry.getOrThrow(Enchantments.BREACH),stack)==4,"New Ghost Spear must have Density V and Breach IV together");
        var old=new ItemStack(AccelerationMod.SPEAR);AccelerationMod.SPEAR.inventoryTick(old,c.getLevel(),owner,EquipmentSlot.MAINHAND);
        c.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(registry.getOrThrow(Enchantments.DENSITY),old)==5&&EnchantmentHelper.getItemEnchantmentLevel(registry.getOrThrow(Enchantments.BREACH),old)==4,"Existing spear must receive both enchantments");
        var golem=EntityTypes.IRON_GOLEM.create(c.getLevel(),EntitySpawnReason.COMMAND);golem.setPos(owner.position().add(2,0,0));c.getLevel().addFreshEntity(golem);
        owner.fallDistance=6;owner.setOnGround(false);var source=c.getLevel().damageSources().playerAttack(owner);
        c.assertTrue(Math.abs(SpearDensity.bonus(owner,golem,source)-15)<1e-6,"Density V must add 2.5 damage per fallen block");
        c.assertTrue(Math.abs(EnchantmentHelper.modifyArmorEffectiveness(c.getLevel(),stack,golem,source,1)-.4)<1e-5,"Breach IV must reduce armor effectiveness by sixty percent");
        owner.fallDistance=0;c.assertTrue(SpearDensity.bonus(owner,golem,source)==0,"Grounded spear hits must not gain Density fall damage");c.succeed();
    }

    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:ghost_spear_fire_aspect_and_flame_survive_reunion")
    public void ghostSpearFireAspectAndFlameSurviveReunion(GameTestHelper c){
        setup(c);var owner=player(c);var target=player(c);var registry=c.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        target.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);target.setPos(owner.position().add(0,0,2));
        var aspect=new ItemStack(AccelerationMod.SPEAR);aspect.enchant(registry.getOrThrow(Enchantments.FIRE_ASPECT),2);
        owner.setItemSlot(EquipmentSlot.MAINHAND,aspect);target.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.TOTEM_OF_UNDYING));
        SoulMode.begin(owner,owner.position());Vec3 origin=owner.position();
        c.assertTrue(chargedStrike(owner,target)&&target.isAlive()&&target.getHealth()==1&&target.getOffhandItem().isEmpty(),"Fire Aspect hit must still pop exactly one totem");
        c.assertTrue(target.getRemainingFireTicks()>=160&&!SoulMode.active(owner)&&owner.position().distanceToSqr(origin)<1e-8,"Fire Aspect II must ignite for eight seconds after the totem and smooth reunion");
        target.clearFire();target.removeAllEffects();target.setHealth(20);target.invulnerableTime=0;target.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.TOTEM_OF_UNDYING));
        var flame=new ItemStack(AccelerationMod.SPEAR);flame.enchant(registry.getOrThrow(Enchantments.FLAME),1);owner.setItemSlot(EquipmentSlot.MAINHAND,flame);
        SoulMode.begin(owner,owner.position());chargeSpear(owner);
        c.assertTrue(owner.stabAttack(EquipmentSlot.MAINHAND,target,0,false,true,false)&&target.isAlive()&&target.getHealth()==1&&target.getOffhandItem().isEmpty(),"Flame native right-click contact must still activate one totem");
        c.assertTrue(target.getRemainingFireTicks()>=100&&!SoulMode.active(owner)&&owner.position().distanceToSqr(origin)<1e-8,"Flame must ignite for five seconds after reunion without a dash");owner.stopUsingItem();
        c.succeed();
    }

    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:spear_special_hit_has_no_dash_camera_snap_or_momentum_change")
    public void spearSpecialHitHasNoDashCameraSnapOrMomentumChange(GameTestHelper c){
        setup(c);var owner=player(c);Vec3 origin=owner.position(),velocity=new Vec3(.2,.1,.4);
        var stand=new StrikeStand(c.getLevel());stand.setPos(origin.add(0,0,2));c.getLevel().addFreshEntity(stand);
        SoulMode.begin(owner,origin);var linked=body(c,owner);owner.setDeltaMovement(velocity);owner.setYRot(35);owner.setXRot(-10);
        try{
            var counter=net.minecraft.server.network.ServerGamePacketListenerImpl.class.getDeclaredField("awaitingTeleport");counter.setAccessible(true);int before=counter.getInt(owner.connection);
            c.assertTrue(chargedStrike(owner,stand)&&stand.received==Float.MAX_VALUE&&stand.isRemoved(),"Smooth strike must still deal maximum damage");
            c.assertTrue(!SoulMode.active(owner)&&linked.isRemoved()&&owner.position().distanceToSqr(origin)<1e-10,"Special strike must reunite without dashing or repositioning");
            c.assertTrue(counter.getInt(owner.connection)==before,"In-place reunion must not send a teleport correction to the client");
            c.assertTrue(owner.getDeltaMovement().distanceToSqr(velocity)<1e-10&&owner.getYRot()==35&&owner.getXRot()==-10,"Strike must preserve camera angles and existing movement");
            var target=player(c);target.setPos(origin.add(0,0,3.5));target.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);target.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.TOTEM_OF_UNDYING));
            SoulMode.begin(owner,origin);owner.setDeltaMovement(velocity);chargeSpear(owner);before=counter.getInt(owner.connection);
            c.assertTrue(owner.stabAttack(EquipmentSlot.MAINHAND,target,0,false,true,false)&&target.getHealth()==1&&target.getOffhandItem().isEmpty(),"Native right-click contact must still pop one totem");
            c.assertTrue(!SoulMode.active(owner)&&owner.position().distanceToSqr(origin)<1e-10&&counter.getInt(owner.connection)==before&&owner.getDeltaMovement().distanceToSqr(velocity)<1e-10,"Native right-click must also reunite smoothly without a teleport or velocity kick");owner.stopUsingItem();
        }catch(ReflectiveOperationException error){throw new RuntimeException(error);}
        c.succeed();
    }

    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:body_equipment_updates_preserve_counts_damage_and_owner_inventory")
    public void bodyEquipmentUpdatesPreserveCountsDamageAndOwnerInventory(GameTestHelper c){
        setup(c);var owner=player(c);SoulMode.begin(owner,owner.position());var linked=body(c,owner);
        var helmet=new ItemStack(Items.DIAMOND_HELMET);helmet.setDamageValue(10);owner.setItemSlot(EquipmentSlot.HEAD,helmet);
        owner.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.ENDER_PEARL,5));linked.tick();
        c.assertTrue(linked.getItemBySlot(EquipmentSlot.HEAD).getDamageValue()==10&&linked.getItemBySlot(EquipmentSlot.OFFHAND).getCount()==5,"Body must immediately reflect changed equipment");
        helmet.setDamageValue(20);owner.getOffhandItem().setCount(3);linked.tick();
        c.assertTrue(linked.getItemBySlot(EquipmentSlot.HEAD).getDamageValue()==20&&linked.getItemBySlot(EquipmentSlot.OFFHAND).getCount()==3,"In-place durability and count changes must still sync on the next body tick");
        linked.getOffhandItem().setCount(1);c.assertTrue(owner.getOffhandItem().getCount()==3,"Body equipment copies must never mutate the real inventory");
        linked.tick();c.assertTrue(linked.getOffhandItem().getCount()==3,"Body must resync equipment after an independent change");c.succeed();
    }
    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:body_target_selection_keeps_nearest_and_retires_removed_body")
    public void bodyTargetSelectionKeepsNearestAndRetiresRemovedBody(GameTestHelper c){
        setup(c);var first=player(c);var second=player(c);Vec3 anchor=first.position();second.setPos(anchor.add(8,0,0));
        for(int x=-1;x<=9;x++)for(int y=0;y<4;y++)for(int z=-1;z<=1;z++)
            c.getLevel().setBlockAndUpdate(BlockPos.containing(anchor).offset(x,y,z),Blocks.AIR.defaultBlockState());
        SoulMode.begin(first,anchor);var firstBody=body(c,first);first.setPos(anchor.add(0,5,0));
        SoulMode.begin(second,second.position());var secondBody=body(c,second);second.setPos(second.position().add(0,5,0));
        var zombie=c.spawn(EntityTypes.ZOMBIE,new BlockPos(3,2,2));BodyAggro.targetBody(zombie);
        c.assertTrue(zombie.getTarget()==firstBody,"Mob must choose the nearest eligible linked body");
        SoulMode.finish(first,first.position());BodyAggro.targetBody(zombie);
        c.assertTrue(zombie.getTarget()==secondBody,"Mob must stop targeting a retired body and acquire the remaining eligible one; actual="+zombie.getTarget()+", remaining="+secondBody+", active="+SoulMode.active(second)+", distance="+zombie.distanceTo(secondBody)+", condition="+net.minecraft.world.entity.ai.targeting.TargetingConditions.forCombat().range(zombie.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE)).test(c.getLevel(),zombie,secondBody));c.succeed();
    }

    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:block_click_and_cancel_stay_ghost_until_successful_break")
    public void blockClickAndCancelStayGhostUntilSuccessfulBreak(GameTestHelper c){
        setup(c);var owner=player(c);Vec3 anchor=owner.position();
        SoulMode.begin(owner,anchor);var linked=body(c,owner);Vec3 ghost=anchor.add(5,0,0);owner.setPos(ghost);
        owner.getInventory().setItem(20,owner.getMainHandItem().copy());owner.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_PICKAXE));
        var pos=BlockPos.containing(ghost.add(1,0,0));c.getLevel().setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
        net.fabricmc.fabric.api.event.player.AttackBlockCallback.EVENT.invoker().interact(owner,c.getLevel(),net.minecraft.world.InteractionHand.MAIN_HAND,pos,Direction.UP);
        c.assertTrue(SoulMode.active(owner)&&!linked.isRemoved(),"A single left click on stone must keep ghost mode and the original body");
        owner.gameMode.handleBlockBreakAction(pos,net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,Direction.UP,c.getLevel().getMaxY(),0);
        owner.gameMode.handleBlockBreakAction(pos,net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,Direction.UP,c.getLevel().getMaxY(),1);
        c.assertTrue(SoulMode.active(owner)&&c.getLevel().getBlockState(pos).is(Blocks.STONE),"Starting and aborting native mining must not recall or break the block");
        owner.setGameMode(GameType.ADVENTURE);
        c.assertTrue(!owner.gameMode.destroyBlock(pos)&&SoulMode.active(owner),"A denied break must not recall the ghost");owner.setGameMode(GameType.SURVIVAL);
        owner.setDeltaMovement(new Vec3(.3,.1,0));
        c.assertTrue(owner.gameMode.destroyBlock(pos)&&c.getLevel().getBlockState(pos).isAir(),"Successful native block break must remove the stone");
        c.assertTrue(!SoulMode.active(owner)&&linked.isRemoved()&&owner.position().distanceToSqr(ghost)<1e-8,"Only the completed break must reunite the body at the ghost position");
        c.assertTrue(owner.getDeltaMovement().distanceToSqr(new Vec3(.3,.1,0))<1e-8,"Completed-break reunion must preserve momentum");c.succeed();
    }

    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:neutral_enderman_stays_calm_until_provoked")
    public void neutralEndermanStaysCalmUntilProvoked(GameTestHelper c){
        setup(c);var owner=player(c);var anchor=Vec3.atBottomCenterOf(c.absolutePos(new BlockPos(7,2,7)));
        owner.setPos(anchor);var enderman=c.spawn(EntityTypes.ENDERMAN,new BlockPos(4,2,7));
        SoulMode.begin(owner,anchor);var linked=body(c,owner);owner.setPos(anchor.add(0,5,0));
        BodyAggro.targetBody(enderman);c.assertTrue(enderman.getTarget()==null,"Entering projection must not make a calm Enderman attack the body");
        c.runAtTickTime(20,()->{
            c.assertTrue(enderman.getTarget()==null,"Unprovoked Enderman must remain calm during ghost mode");
            enderman.setLastHurtByMob(owner);enderman.setPersistentAngerTarget(net.minecraft.world.entity.EntityReference.of(owner));enderman.setTimeToRemainAngry(1000);
            BodyAggro.targetBody(enderman);c.assertTrue(enderman.getTarget()==linked,"An already provoked Enderman must still be allowed to attack the linked body");c.succeed();
        });
    }
    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:speed_commands_set_add_reset_and_maximum")
    public void speedCommandsSetAddResetAndMaximum(GameTestHelper c){
        setup(c);var owner=player(c);var server=c.getLevel().getServer();
        var source=server.createCommandSourceStack().withEntity(owner);
        double base=owner.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        try{
            var dispatcher=server.getCommands().getDispatcher();
            dispatcher.execute("accleration speed set 100",source);
            c.assertTrue(Math.abs(SpeedCommands.value(owner)-100)<1e-6&&Math.abs(owner.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)-base*2)<1e-6,"set 100 must double normal movement speed");
            dispatcher.execute("accleration speed add 50",source);c.assertTrue(SpeedCommands.value(owner)==150,"add must increase the configured percent");
            dispatcher.execute("acceleration speed reset",source);
            c.assertTrue(SpeedCommands.value(owner)==0&&!AccelerationMod.accelerationEnabled(owner)&&AccelerationMod.charge(owner)==0&&Math.abs(owner.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)-base)<1e-6,"reset alias must restore zero bonus and clear acceleration");
            dispatcher.execute("accleration speed set maximum",source);dispatcher.execute("accleration speed add 100",source);
            c.assertTrue(SpeedCommands.value(owner)==SpeedCommands.MAX_PERCENT&&Double.isFinite(owner.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)),"Maximum speed must saturate without overflow");
            dispatcher.execute("accleration speed set 0",source);c.assertTrue(SpeedCommands.value(owner)==0,"set 0 must also restore base speed");
        }catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new RuntimeException(error);}
        c.succeed();
    }
    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:held_spear_right_click_reaches_four_point_five_blocks")
    public void heldSpearRightClickReachesFourPointFiveBlocks(GameTestHelper c){
        setup(c);var owner=player(c);
        c.runAtTickTime(5,()->{
            owner.doTick();
            c.assertTrue(Math.abs(owner.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE)-4.5)<1e-6,"Holding Ghost Spear must increase actual server entity interaction reach to 4.5, got "+owner.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE));
            var stand=new StrikeStand(c.getLevel());stand.setPos(owner.position().add(4.4,0,0));c.getLevel().addFreshEntity(stand);
            SoulMode.begin(owner,owner.position());
            c.assertTrue(owner.isWithinEntityInteractionRange(stand,0),"Native server interaction validation must accept the extended reach");
            c.assertTrue(chargedInteract(owner,c.getLevel(),net.minecraft.world.InteractionHand.MAIN_HAND,stand,null)==net.minecraft.world.InteractionResult.SUCCESS&&stand.isRemoved()&&!SoulMode.active(owner),"Direct right click beyond three blocks must hit and reunite");c.succeed();
        });
    }

    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:point_blank_ghost_spear_strikes_without_minimum_range")
    public void pointBlankGhostSpearStrikesWithoutMinimumRange(GameTestHelper c){
        setup(c);var owner=player(c);var target=player(c);target.setPos(owner.position().add(.25,0,0));
        var range=owner.getMainHandItem().get(DataComponents.ATTACK_RANGE);
        c.assertTrue(range.minReach()==0&&range.minCreativeReach()==0&&range.maxReach()==4.5F&&range.maxCreativeReach()==6.5F,"Only the minimum spear reach must change to zero");
        c.assertTrue(owner.isWithinAttackRange(owner.getMainHandItem(),target.getBoundingBox(),0),"Overlapping point-blank target must be within native attack range");
        target.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);target.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.TOTEM_OF_UNDYING));
        SoulMode.begin(owner,owner.position());chargeSpear(owner);
        c.assertTrue(owner.stabAttack(EquipmentSlot.MAINHAND,target,0,false,true,false)&&target.getHealth()==1&&target.getOffhandItem().isEmpty()&&!SoulMode.active(owner),"Point-blank native right-click contact must pop one totem and reunite");owner.stopUsingItem();
        var golem=EntityTypes.IRON_GOLEM.create(c.getLevel(),EntitySpawnReason.COMMAND);golem.setPos(owner.position().add(.4,0,0));c.getLevel().addFreshEntity(golem);
        SoulMode.begin(owner,owner.position());chargeSpear(owner);
        c.assertTrue(owner.stabAttack(EquipmentSlot.MAINHAND,golem,0,false,true,false)&&golem.isDeadOrDying()&&!SoulMode.active(owner),"Point-blank ghost spear must kill an ordinary target and reunite");owner.stopUsingItem();c.succeed();
    }

    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:ghost_knockback_only_contact_always_strikes")
    public void ghostKnockbackOnlyContactAlwaysStrikes(GameTestHelper c){
        setup(c);var owner=player(c);var target=player(c);target.setPos(owner.position().add(0,0,3.5));
        target.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);target.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.TOTEM_OF_UNDYING));
        SoulMode.begin(owner,owner.position());chargeSpear(owner);
        boolean hit=owner.stabAttack(EquipmentSlot.MAINHAND,target,0,false,true,false);
        c.assertTrue(hit&&target.isAlive()&&target.getHealth()==1&&target.getOffhandItem().isEmpty(),"Knockback-only contact must apply maximum ghost damage and pop exactly one totem");
        c.assertTrue(!SoulMode.active(owner),"Successful contact must reunite body");owner.stopUsingItem();
        var golem=EntityTypes.IRON_GOLEM.create(c.getLevel(),EntitySpawnReason.COMMAND);golem.setPos(owner.position().add(0,0,3));c.getLevel().addFreshEntity(golem);
        SoulMode.begin(owner,owner.position());chargeSpear(owner);
        c.assertTrue(owner.stabAttack(EquipmentSlot.MAINHAND,golem,0,false,true,false)&&golem.isDeadOrDying()&&!SoulMode.active(owner),"Zero-base-damage shove contact must kill ordinary targets and reunite");owner.stopUsingItem();c.succeed();
    }
    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:ghost_cramped_landing_still_strikes")
    public void ghostCrampedLandingStillStrikes(GameTestHelper c){
        setup(c);var owner=player(c);Vec3 origin=owner.position();
        var stand=new StrikeStand(c.getLevel());stand.setPos(origin.add(2,0,0));c.getLevel().addFreshEntity(stand);
        for(Vec3 offset:List.of(new Vec3(1.6,0,0),new Vec3(-1.6,0,0),new Vec3(0,0,1.6),new Vec3(0,0,-1.6),new Vec3(0,1,0))){
            var pos=BlockPos.containing(stand.position().add(offset));c.getLevel().setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());c.getLevel().setBlockAndUpdate(pos.above(),Blocks.STONE.defaultBlockState());
        }
        SoulMode.begin(owner,origin);
        c.assertTrue(chargedStrike(owner,stand)&&stand.isRemoved()&&!SoulMode.active(owner),"Blocked landing offsets must not cancel a valid hit or leave the body behind");
        c.assertTrue(owner.position().distanceToSqr(origin)<1e-8,"No landing space must reunite at attack position");c.succeed();
    }

    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:phasing_accepts_real_movement_packets")
    public void phasingAcceptsRealMovementPackets(GameTestHelper c){
        setup(c);var owner=player(c);var start=owner.position();
        c.setBlock(3,2,2,Blocks.STONE);c.setBlock(3,3,2,Blocks.STONE);
        BlockPhasing.toggle(owner);
        try{
            var field=net.minecraft.server.network.ServerGamePacketListenerImpl.class.getDeclaredField("awaitingTeleport");field.setAccessible(true);
            owner.connection.handleAcceptTeleportPacket(new net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket(field.getInt(owner.connection)));
        }catch(ReflectiveOperationException error){throw new RuntimeException(error);}
        owner.setPos(start);owner.connection.resetPosition();
        owner.connection.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(start.add(1,0,0),true,false));
        c.assertTrue(Math.abs(owner.getX()-start.x-1)<1e-6,"The server must accept a real movement packet entering the wall, without rubber-banding");
        owner.connection.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(start.add(2,0,0),true,false));
        c.assertTrue(Math.abs(owner.getX()-start.x-2)<1e-6,"The next movement packet must emerge on the other side");
        c.assertTrue(BlockPhasing.stop(owner),"Phasing must disable after moving through the wall");c.succeed();
    }

    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:block_phasing_toggle_collision_and_safe_exit")
    public void blockPhasingToggleCollisionAndSafeExit(GameTestHelper c){
        setup(c);var owner=player(c);Vec3 start=owner.position();
        c.setBlock(3,2,2,Blocks.STONE);c.setBlock(3,3,2,Blocks.STONE);
        owner.move(MoverType.SELF,new Vec3(2,0,0));
        c.assertTrue(owner.getX()<start.x+1,"Normal player must collide with the wall");owner.setPos(start);
        c.assertTrue(SoulPackets.handleAction(owner,SoulPackets.TOGGLE_PHASING)&&BlockPhasing.active(owner),"6-key action must enable phasing");
        c.assertTrue(owner.gameMode()==GameType.SURVIVAL&&!owner.getAbilities().mayfly&&!SoulMode.active(owner),"Phasing must not grant flight, change mode or activate projection");
        owner.move(MoverType.SELF,new Vec3(2,0,0));
        c.assertTrue(Math.abs(owner.getX()-start.x-2)<1e-6,"Phased player must move through a solid wall");
        double y=owner.getY();owner.move(MoverType.SELF,new Vec3(0,-.5,0));
        c.assertTrue(Math.abs(owner.getY()-y)<1e-6,"Normal floors must remain until deliberately sneaking");
        c.assertTrue(SoulPackets.handleAction(owner,SoulPackets.CYCLE_PHASING)&&BlockPhasing.mode(owner)==1,"7 must select Underneath Blocks mode");owner.move(MoverType.SELF,new Vec3(0,-.5,0));
        c.assertTrue(owner.getY()<y-.4,"Underneath Blocks mode must let the player descend into a floor");
        c.assertTrue(!owner.isInWall(),"Phasing inside blocks must not suffocate");
        c.assertTrue(SoulPackets.handleAction(owner,SoulPackets.TOGGLE_PHASING)&&!BlockPhasing.active(owner),"Pressing 6 again must disable phasing");
        c.assertTrue(c.getLevel().noCollision(owner,owner.getBoundingBox()),"Disabling inside a floor must relocate to open space");
        c.assertTrue(SoulPackets.handleAction(owner,SoulPackets.CYCLE_PHASING)&&BlockPhasing.mode(owner)==2&&!BlockPhasing.active(owner),"7 must select On Top mode without changing toggle state");
        owner.setPos(start);c.setBlock(2,4,2,Blocks.STONE);BlockPhasing.toggle(owner);
        owner.move(MoverType.SELF,new Vec3(0,1,0));
        c.assertTrue(Math.abs(owner.getY()-start.y-1)<1e-6,"On Top mode must let upward movement pass through a ceiling");BlockPhasing.stop(owner);
        SoulPackets.handleAction(owner,SoulPackets.CYCLE_PHASING);c.assertTrue(BlockPhasing.mode(owner)==0,"7 must cycle back to Wall mode");
        owner.setPos(start);SoulMode.begin(owner,start);var linked=body(c,owner);
        c.assertTrue(BlockPhasing.toggle(owner),"Phasing must also enable during ghost mode");owner.move(MoverType.SELF,new Vec3(2,0,0));
        c.assertTrue(SoulMode.active(owner)&&!linked.isRemoved()&&linked.position().distanceToSqr(start)<1e-8,"Phasing must leave linked body anchored and ghost active");
        BlockPhasing.stop(owner);SoulMode.finish(owner,owner.position());
        owner.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);owner.getInventory().setItem(20,ItemStack.EMPTY);
        c.assertTrue(!BlockPhasing.toggle(owner),"Phasing must require the spear in inventory");c.succeed();
    }

    @GameTest(maxTicks=60,padding=32,environment="acceleration_gametest:reward_has_all_max_enchantments_and_native_spear_components")
    public void rewardHasAllMaxEnchantmentsAndNativeSpearComponents(GameTestHelper c){
        setup(c);var stack=AccelerationSpear.create(c.getLevel());
        c.assertTrue(stack.getMaxDamage()==Items.DIAMOND_SPEAR.getDefaultInstance().getMaxDamage(),"Spear must use diamond durability, not netherite");
        var modifiers=stack.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers();
        var speed=modifiers.stream().filter(e->e.attribute().equals(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED)).findFirst().orElseThrow();
        c.assertTrue(Math.abs(4+speed.modifier().amount()-4)<1e-6,"Held spear must have 4 attacks per second: quarter-second cooldown");
        var damage=modifiers.stream().filter(e->e.attribute().equals(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)).findFirst().orElseThrow();
        var vanillaDamage=Items.DIAMOND_SPEAR.getDefaultInstance().get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().stream().filter(e->e.attribute().equals(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)).findFirst().orElseThrow();
        c.assertTrue(damage.equals(vanillaDamage),"Faster spear must retain diamond attack damage");
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
        AccelerationMod.toggleAcceleration(p);p.setSprinting(true);for(int i=0;i<100;i++)AccelerationMod.accelerationTick(p);
        c.assertTrue(AccelerationMod.charge(p)==100,"A spear in an ordinary inventory slot must charge acceleration");
        double charged=p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        c.assertTrue(Math.abs(charged-base*1.3*1.60)<1e-6,"Five seconds of sprinting must give a 60-percent acceleration bonus on top of native sprint speed");
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
            c.assertTrue(!chargedStrike(actor,target)&&SoulMode.active(actor),"Right-click strike must require normal interaction reach, not remote aiming");target.setPos(nearby);
            c.assertTrue(chargedInteract(actor,c.getLevel(),net.minecraft.world.InteractionHand.MAIN_HAND,target,null)==net.minecraft.world.InteractionResult.SUCCESS,"Right-click interaction must land the hit");
            c.assertTrue(target.isAlive()&&target.getHealth()==1&&target.getOffhandItem().isEmpty(),"Exactly one normal totem must activate");
            c.assertTrue(!actor.isSpectator()&&!SoulMode.active(actor)&&actor.distanceToSqr(target)<9,"Attacker must materialize in place after the strike");
            c.assertTrue(body.isRemoved(),"Old body must disappear after a committed strike");
            c.assertTrue(!chargedStrike(actor,target)&&target.getHealth()==1,"Repeated action must not cause another hit after projection ends");
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
            c.assertTrue(SoulMode.begin(actor,actor.position())&&chargedInteract(actor,c.getLevel(),net.minecraft.world.InteractionHand.MAIN_HAND,target,null)==net.minecraft.world.InteractionResult.SUCCESS,"Right-click must immediately land the lethal strike and reunite in place");
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
            c.assertTrue(SoulMode.active(p)&&p.gameMode()==GameType.ADVENTURE&&p.position().distanceToSqr(endpoint)<1e-8,"An empty ghost lunge must keep the original body and projection");
            c.assertTrue(p.getDeltaMovement().distanceToSqr(new Vec3(.4,.1,0))<1e-8,"An empty ghost lunge must preserve momentum");
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
        var decoded=SoulState.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,encoded).getOrThrow();
        var restored=decoded.projection(p.getUUID());
        c.assertTrue(decoded.body(restored.body())==restored,"Saved body ownership must restore correctly for damage forwarding");
        decoded.remove(p.getUUID());c.assertTrue(decoded.body(restored.body())==null,"Retired body ownership must be removed after recovery");
        c.assertTrue(restored!=null&&restored.anchor().distanceToSqr(origin)<1e-8&&restored.mode()==GameType.SURVIVAL&&!restored.abilities().mayFly(),"Crash-recovery snapshot must preserve exact body anchor, original mode and abilities");
        c.assertTrue(SoulMode.finish(p,null)&&p.gameMode()==GameType.SURVIVAL&&!p.getAbilities().invulnerable,"Reconnect/stop recovery path must restore normal play");c.succeed();
    }
    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:creative_targets_and_disabled_pvp_are_protected")
    public void creativeTargetsAndDisabledPvpAreProtected(GameTestHelper c){
        setup(c);var actor=player(c);var target=player(c);target.setPos(actor.position().add(2,0,0));target.setGameMode(GameType.CREATIVE);
        SoulMode.begin(actor,actor.position());c.assertTrue(!chargedStrike(actor,target)&&SoulMode.active(actor),"Creative targets must not be killed");
        target.setGameMode(GameType.SURVIVAL);c.getLevel().getGameRules().set(net.minecraft.world.level.gamerules.GameRules.PVP,false,c.getLevel().getServer());
        c.assertTrue(!chargedStrike(actor,target)&&target.getHealth()==20,"Disabled PvP must prevent soul strike");
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
            c.assertTrue(chargedInteract(owner,c.getLevel(),net.minecraft.world.InteractionHand.MAIN_HAND,body,null)==net.minecraft.world.InteractionResult.SUCCESS,"Right-clicking your own body with the spear must also recall you, not lethally strike yourself");
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

    @GameTest(maxTicks=180,padding=32,environment="acceleration_gametest:zombie_hunts_and_hits_body_while_ghost_wind_moves_skeleton")
    public void zombieHuntsAndHitsBodyWhileGhostWindMovesSkeleton(GameTestHelper c){
        setup(c);var owner=player(c);var anchor=Vec3.atBottomCenterOf(c.absolutePos(new BlockPos(7,2,7)));
        owner.setPos(anchor);SoulMode.begin(owner,anchor);var linked=body(c,owner);
        owner.setPos(anchor.add(0,5,0));
        var zombie=c.spawn(EntityTypes.ZOMBIE,new BlockPos(3,2,7));zombie.setPersistenceRequired();
        zombie.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));
        double distance=zombie.distanceToSqr(linked);
        c.runAtTickTime(30,()->{
            c.assertTrue(zombie.getTarget()==linked&&zombie.distanceToSqr(linked)<distance,"Zombie must naturally acquire the linked body and walk toward it without a manual target assignment");
            c.assertTrue(!zombie.canAttack(owner),"Zombie must not detect or attack the ghost");
        });
        c.runAtTickTime(90,()->{
            c.assertTrue(owner.getHealth()<20&&SoulMode.active(owner),"Zombie must reach and damage the real owner through the body without recalling the ghost");
            zombie.discard();
            var skeleton=c.spawn(EntityTypes.SKELETON,new BlockPos(5,2,5));skeleton.setNoAi(true);
            skeleton.setDeltaMovement(Vec3.ZERO);Vec3 before=skeleton.position();
            var charge=new SelfWindCharge(c.getLevel());charge.setOwner(owner);charge.burst(before.add(-.5,.1,0));
            c.assertTrue(skeleton.getDeltaMovement().lengthSqr()<1e-8,"A real ghost-fired wind charge must not push a skeleton");
            c.assertTrue(SoulMode.active(owner)&&!linked.isRemoved(),"Using wind charges must keep the ghost and linked body active");
            var cow=c.spawn(EntityTypes.COW,new BlockPos(3,2,3));
            var arrow=EntityTypes.ARROW.create(c.getLevel(),EntitySpawnReason.COMMAND);
            var pearl=EntityTypes.ENDER_PEARL.create(c.getLevel(),EntitySpawnReason.COMMAND);
            var playerTarget=player(c);Vec3 impulse=new Vec3(.2,.5,-.1);
            for(Entity target:List.of(skeleton,cow,arrow,pearl,playerTarget,linked))
                c.assertTrue(GhostWind.impulse(charge,target,impulse).equals(Vec3.ZERO),"Ghost-fired wind must suppress knockback for every entity type including projectiles: "+target.getType());
            SoulMode.finish(owner,owner.position());skeleton.setDeltaMovement(Vec3.ZERO);
            var normalCharge=new SelfWindCharge(c.getLevel());normalCharge.setOwner(owner);normalCharge.burst(before.add(-.5,.1,0));
            c.assertTrue(skeleton.getDeltaMovement().lengthSqr()>.001,"Normal body-mode wind charges must retain vanilla knockback");c.succeed();
        });
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
            c.assertTrue(chargedInteract(owner,c.getLevel(),net.minecraft.world.InteractionHand.MAIN_HAND,zombie,null)==net.minecraft.world.InteractionResult.FAIL,"Ordinary right-click entity interactions must be blocked");owner.setItemSlot(EquipmentSlot.MAINHAND,held);
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
            var result=chargedInteract(owner,c.getLevel(),net.minecraft.world.InteractionHand.MAIN_HAND,dragon.head,null);
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
            c.assertTrue(chargedInteract(observer,c.getLevel(),net.minecraft.world.InteractionHand.MAIN_HAND,owner,null)==net.minecraft.world.InteractionResult.FAIL,"Incoming right-clicks must be rejected");
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
    @GameTest(maxTicks=100,padding=32,environment="acceleration_gametest:ghost_mace_wind_burst_survives_reunion")
    public void ghostMaceWindBurstSurvivesReunion(GameTestHelper c){
        setup(c);var owner=player(c);var origin=owner.position().add(0,3,0);
        var mace=new ItemStack(Items.MACE);
        mace.enchant(c.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.WIND_BURST),3);
        owner.getInventory().setItem(20,owner.getMainHandItem().copy());
        owner.setItemSlot(EquipmentSlot.MAINHAND,mace);
        double[] normal={0};
        c.runAtTickTime(10,()->{
            var golem=EntityTypes.IRON_GOLEM.create(c.getLevel(),EntitySpawnReason.COMMAND);golem.setPos(origin.add(1,0,0));c.getLevel().addFreshEntity(golem);
            owner.setPos(origin);owner.setOnGround(false);owner.fallDistance=6;owner.setDeltaMovement(new Vec3(0,-.6,0));
            owner.attack(golem);normal[0]=owner.getDeltaMovement().y;
            c.assertTrue(normal[0]>.1,"Normal Wind Burst III smash must launch the attacker");golem.discard();
        });
        c.runAtTickTime(45,()->{
            owner.setItemSlot(EquipmentSlot.MAINHAND,AccelerationSpear.create(c.getLevel()));
            owner.setPos(origin);SoulMode.begin(owner,origin.add(-5,0,0));var oldBody=body(c,owner);
            owner.setItemSlot(EquipmentSlot.MAINHAND,mace.copy());
            var golem=EntityTypes.IRON_GOLEM.create(c.getLevel(),EntitySpawnReason.COMMAND);golem.setPos(origin.add(1,0,0));c.getLevel().addFreshEntity(golem);
            owner.setOnGround(false);owner.fallDistance=6;owner.setDeltaMovement(new Vec3(0,-.6,0));
            owner.attack(golem);
            c.assertTrue(golem.getHealth()<golem.getMaxHealth()&&!SoulMode.active(owner)&&oldBody.isRemoved(),"Ghost mace hit must damage target and reunite body");
            c.assertTrue(owner.getDeltaMovement().y>.1&&Math.abs(owner.getDeltaMovement().y-normal[0])<1e-6,"Reunion must preserve the same Wind Burst launch as a normal smash: ghost="+owner.getDeltaMovement().y+", normal="+normal[0]);
            c.succeed();
        });
    }

    @GameTest(maxTicks=100,padding=32,environment="acceleration_gametest:normal_spear_hit_recall_does_not_restart_projection")
    public void normalSpearHitRecallDoesNotRestartProjection(GameTestHelper c){
        setup(c);var owner=player(c);var target=player(c);Vec3 anchor=owner.position();Vec3 strikePosition=anchor.add(8,0,0),endpoint=strikePosition.add(0,0,3);
        c.runAtTickTime(10,()->{
            SoulMode.begin(owner,anchor);owner.setPos(strikePosition);target.setPos(strikePosition.add(0,0,2));owner.setYRot(0);owner.setXRot(0);
            owner.getMainHandItem().get(DataComponents.PIERCING_WEAPON).attack(owner,EquipmentSlot.MAINHAND);
            c.assertTrue(target.getHealth()<20&&SoulMode.active(owner),"A native spear jab must hit first and keep the body behind while the lunge moves forward");
        });
        c.runAtTickTime(12,()->{owner.setPos(endpoint);owner.setDeltaMovement(new Vec3(0,0,.4));});
        c.runAtTickTime(20,()->{
            c.assertTrue(!SoulMode.active(owner)&&owner.position().distanceToSqr(endpoint)<1e-8,"A lunge that hits must still reunite at its actual forward endpoint, not teleport before movement");
            c.assertTrue(owner.getDeltaMovement().z==.4,"Post-hit lunge reunion must preserve forward momentum");c.succeed();
        });
    }



    private static final class StrikeStand extends net.minecraft.world.entity.decoration.ArmorStand {
        float received;Vec3 attackerAtDamage;boolean projectedAtDamage;
        StrikeStand(ServerLevel world){super(EntityTypes.ARMOR_STAND,world);}
        @Override public boolean hurtServer(ServerLevel world,net.minecraft.world.damagesource.DamageSource source,float amount){received=amount;if(source.getEntity() instanceof ServerPlayer actor){attackerAtDamage=actor.position();projectedAtDamage=SoulMode.active(actor);}return super.hurtServer(world,source,amount);}
    }
    @GameTest(maxTicks=100,padding=32,environment="acceleration_gametest:maximum_damage_strikes_damageable_entities_and_bosses")
    public void maximumDamageStrikesDamageableEntitiesAndBosses(GameTestHelper c){
        setup(c);var owner=player(c);
        c.runAtTickTime(5,()->{
            var stand=new StrikeStand(c.getLevel());stand.setPos(owner.position().add(2,0,0));c.getLevel().addFreshEntity(stand);
            SoulMode.begin(owner,owner.position());
            c.assertTrue(chargedStrike(owner,stand)&&stand.isRemoved()&&stand.received==Float.MAX_VALUE,"Stand strike must receive maximum finite damage and destroy it immediately");
            var cart=c.spawn(EntityTypes.MINECART,new BlockPos(6,2,6));owner.setPos(cart.position().add(2,0,0));SoulMode.begin(owner,owner.position());
            c.assertTrue(chargedStrike(owner,cart)&&cart.isRemoved()&&!SoulMode.active(owner),"Right-click strike must destroy a damageable non-living minecart and materialize its attacker");
            var wither=c.spawn(EntityTypes.WITHER,new BlockPos(10,2,10));wither.setInvulnerableTicks(200);owner.setPos(wither.position().add(2,0,0));SoulMode.begin(owner,owner.position());
            c.assertTrue(chargedStrike(owner,wither)&&wither.isDeadOrDying(),"The strike must kill even a Wither during its spawn protection");c.succeed();
        });
    }

    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:key_activation_validates_spear_and_does_not_toggle_or_queue_lunge")
    public void keyTogglesAccelerationWithoutChangingGhostState(GameTestHelper c){
        setup(c);var owner=player(c);owner.getInventory().setItem(20,owner.getMainHandItem().copy());owner.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.WIND_CHARGE));
        owner.setSprinting(true);AccelerationMod.accelerationTick(owner);
        c.assertTrue(AccelerationMod.charge(owner)==0&&!SoulMode.active(owner),"Acceleration must wait for the I key and never start ghost mode");
        c.assertTrue(SoulPackets.handleAction(owner,SoulPackets.TOGGLE_ACCELERATION)&&AccelerationMod.accelerationEnabled(owner)&&!SoulMode.active(owner),"I must enable the speed boost with spear in inventory without activating ghost mode");
        for(int i=0;i<100;i++)AccelerationMod.accelerationTick(owner);
        c.assertTrue(AccelerationMod.charge(owner)==100,"Enabled sprinting must build acceleration");
        c.assertTrue(SoulPackets.handleAction(owner,SoulPackets.TOGGLE_ACCELERATION)&&!AccelerationMod.accelerationEnabled(owner)&&AccelerationMod.charge(owner)==0,"I again must immediately disable and clear acceleration");
        owner.setItemSlot(EquipmentSlot.MAINHAND,AccelerationSpear.create(c.getLevel()));SoulMode.begin(owner,owner.position());var body=body(c,owner);
        SoulPackets.handleAction(owner,SoulPackets.TOGGLE_ACCELERATION);SoulPackets.handleAction(owner,SoulPackets.TOGGLE_ACCELERATION);
        c.assertTrue(SoulMode.active(owner)&&!body.isRemoved(),"I must only toggle speed, never toggle ghost mode or queue recall");SoulMode.finish(owner,owner.position());
        owner.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);owner.getInventory().setItem(20,ItemStack.EMPTY);
        c.assertTrue(!SoulPackets.handleAction(owner,SoulPackets.TOGGLE_ACCELERATION),"Activation without the spear must be rejected");
        c.assertTrue(!SoulPackets.handleAction(owner,999),"Unknown action must be ignored");c.succeed();
    }

    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:right_click_joust_always_uses_maximum_ghost_damage")
    public void rightClickJoustAlwaysUsesMaximumGhostDamage(GameTestHelper c){
        setup(c);var owner=player(c);var target=player(c);target.setPos(owner.position().add(0,0,12));
        c.runAtTickTime(5,()->{
            var stand=new StrikeStand(c.getLevel());stand.setPos(owner.position().add(3.5,0,0));c.getLevel().addFreshEntity(stand);
            SoulMode.activateMomentum(owner);chargeSpear(owner);
            owner.stabAttack(EquipmentSlot.MAINHAND,stand,.01F,true,false,false);
            c.assertTrue(stand.received==Float.MAX_VALUE&&stand.isRemoved()&&!SoulMode.active(owner),"A native right-click joust with tiny base damage must still apply the maximum ghost strike using the spear's actual reach");
            owner.stopUsingItem();target.setPos(owner.position().add(3.5,0,0));target.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            target.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.TOTEM_OF_UNDYING));
            SoulMode.activateMomentum(owner);chargeSpear(owner);
            owner.stabAttack(EquipmentSlot.MAINHAND,target,.01F,true,false,false);
            c.assertTrue(target.isAlive()&&target.getHealth()==1&&target.getOffhandItem().isEmpty()&&!SoulMode.active(owner),"A native right-click ghost joust must pop exactly one totem and reunite the body: health="+target.getHealth()+", totem="+target.getOffhandItem()+", projected="+SoulMode.active(owner));owner.stopUsingItem();c.succeed();
        });
    }

    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:ghost_spear_pierces_multiple_targets")
    public void ghostSpearPiercesMultipleTargets(GameTestHelper c){
        setup(c);var owner=player(c);var origin=owner.position();
        var first=new net.minecraft.world.entity.animal.golem.IronGolem(EntityTypes.IRON_GOLEM,c.getLevel());first.setPos(origin.add(2,0,0));c.getLevel().addFreshEntity(first);
        var second=new net.minecraft.world.entity.animal.golem.IronGolem(EntityTypes.IRON_GOLEM,c.getLevel());second.setPos(origin.add(5,0,0));c.getLevel().addFreshEntity(second);
        var side=new StrikeStand(c.getLevel());side.setPos(origin.add(2,0,2));c.getLevel().addFreshEntity(side);
        SoulMode.begin(owner,origin);
        c.assertTrue(chargedStrike(owner,first),"Quick right-click must strike");
        c.assertTrue(first.isDeadOrDying()&&second.isDeadOrDying(),"A single quick click must pierce both aligned iron golems, including the second at five blocks");
        c.assertTrue(!side.isRemoved()&&!SoulMode.active(owner),"Side targets must be safe and body must reunite once");c.succeed();
    }

    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:hit_precedes_teleport_and_spear_use_does_not_recall_early")
    public void hitPrecedesTeleportAndSpearUseDoesNotRecallEarly(GameTestHelper c){
        setup(c);var owner=player(c);var origin=owner.position();var stand=new StrikeStand(c.getLevel());stand.setPos(origin.add(2,0,0));c.getLevel().addFreshEntity(stand);
        SoulMode.begin(owner,origin);
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.invoker().interact(owner,c.getLevel(),net.minecraft.world.InteractionHand.MAIN_HAND,new BlockHitResult(origin,Direction.UP,owner.blockPosition().below(),false));
        c.assertTrue(SoulMode.active(owner),"Right-clicking the spear while looking at a block must not recall before a hit");
        c.assertTrue(chargedStrike(owner,stand)&&stand.projectedAtDamage&&stand.attackerAtDamage.distanceToSqr(origin)<1e-8,"The damage receiver must observe the attacker still projected at the pre-teleport position");
        c.assertTrue(!SoulMode.active(owner)&&owner.position().distanceToSqr(origin)<1e-8,"The body must reunite after the hit without moving the attacker");c.succeed();
    }
    @GameTest(maxTicks=80,padding=64,environment="acceleration_gametest:first_dragon_normal_return_dragon_upgraded_without_duplicates")
    public void firstDragonNormalReturnDragonUpgradedWithoutDuplicates(GameTestHelper c){
        setup(c);var exit=c.absolutePos(new BlockPos(7,2,7));
        var first=new net.minecraft.world.level.dimension.end.EnderDragonFight(false,false,false,Optional.empty(),0,Optional.empty(),Optional.of(exit),List.of(0),List.of());first.init(c.getLevel(),12,exit);
        var normal=((dev.acceleration.mixin.DragonFightAccess)first).acceleration$create();
        c.assertTrue(normal.getMaxHealth()==200&&!AscendedDragon.upgraded(normal),"The native first fight must remain the normal 200-HP dragon");normal.discard();
        var second=new net.minecraft.world.level.dimension.end.EnderDragonFight(false,true,true,Optional.empty(),0,Optional.empty(),Optional.of(exit),List.of(0),List.of());second.init(c.getLevel(),13,exit);
        var boss=AscendedDragon.spawnOnReturn(c.getLevel(),second);
        c.assertTrue(boss!=null&&AscendedDragon.upgraded(boss)&&boss.getHealth()==1000&&boss.getMaxHealth()==1000&&second.dragonUUID().equals(boss.getUUID()),"A return after the first defeat must create and track the upgraded 1000-HP dragon");
        c.assertTrue(AscendedDragon.spawnOnReturn(c.getLevel(),second)==null,"Another returning player must not create a duplicate boss");
        boss.setHealth(750);AscendedDragon.upgrade(boss);c.assertTrue(boss.getHealth()==750,"Reloading or reapplying upgrade must not heal the boss");c.succeed();
    }
    private static final Map<UUID,List<Long>> SHOT_TIMES=new HashMap<>();
    private static boolean shotObserver;
    @GameTest(maxTicks=70,padding=64,environment="acceleration_gametest:upgraded_dragon_fires_exactly_three_sequential_shots")
    public void upgradedDragonFiresExactlyThreeSequentialShots(GameTestHelper c){
        setup(c);AscendedDragon.clear();
        if(!shotObserver){shotObserver=true;net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity,world)->{
            if(entity instanceof net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball ball&&ball.getOwner() instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon&&SHOT_TIMES.containsKey(dragon.getUUID()))SHOT_TIMES.get(dragon.getUUID()).add(world.getGameTime());
        });}
        var boss=EntityTypes.ENDER_DRAGON.create(c.getLevel(),EntitySpawnReason.COMMAND);boss.setPos(Vec3.atBottomCenterOf(c.absolutePos(new BlockPos(7,10,7))));boss.head.setPos(boss.position());boss.setNoAi(true);AscendedDragon.upgrade(boss);c.getLevel().addFreshEntity(boss);SHOT_TIMES.put(boss.getUUID(),new ArrayList<>());
        var normal=EntityTypes.ENDER_DRAGON.create(c.getLevel(),EntitySpawnReason.COMMAND);normal.setPos(boss.position().add(20,0,0));normal.setNoAi(true);c.getLevel().addFreshEntity(normal);SHOT_TIMES.put(normal.getUUID(),new ArrayList<>());
        c.runAtTickTime(5,()->{
            var shot=new net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball(c.getLevel(),boss,new Vec3(0,0,1));shot.setPos(boss.position());c.getLevel().addFreshEntity(shot);
            var ordinary=new net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball(c.getLevel(),normal,new Vec3(0,0,1));ordinary.setPos(normal.position());c.getLevel().addFreshEntity(ordinary);
        });
        c.runAtTickTime(14,()->c.assertTrue(SHOT_TIMES.get(boss.getUUID()).size()==1,"Volley must begin with a single shot, not spawn three simultaneously"));
        c.runAtTickTime(16,()->c.assertTrue(SHOT_TIMES.get(boss.getUUID()).size()==2,"Second fireball must follow after ten ticks"));
        c.runAtTickTime(40,()->{var times=SHOT_TIMES.remove(boss.getUUID());var ordinary=SHOT_TIMES.remove(normal.getUUID());
            c.assertTrue(times.size()==3&&times.get(1)-times.get(0)>=10&&times.get(2)-times.get(1)>=10,"One upgraded dragon shot must produce exactly three sequential fireballs without recursive volleys");
            c.assertTrue(ordinary.size()==1,"The first-fight dragon must retain its normal single shot");c.succeed();});
    }
    @GameTest(maxTicks=80,padding=32,environment="acceleration_gametest:every_spear_material_has_free_lunges")
    public void everySpearMaterialHasFreeLunges(GameTestHelper c){
        setup(c);var owner=player(c);var food=owner.getFoodData();var lunge=c.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LUNGE);
        for(var item:new Item[]{Items.WOODEN_SPEAR,Items.STONE_SPEAR,Items.COPPER_SPEAR,Items.IRON_SPEAR,Items.GOLDEN_SPEAR,Items.DIAMOND_SPEAR,Items.NETHERITE_SPEAR}){
            var stack=new ItemStack(item);stack.enchant(lunge,3);owner.setItemSlot(EquipmentSlot.MAINHAND,stack);food.setFoodLevel(10);food.setSaturation(5);
            for(int i=0;i<3;i++){stack.get(DataComponents.PIERCING_WEAPON).attack(owner,EquipmentSlot.MAINHAND);food.tick(owner);}
            c.assertTrue(food.getFoodLevel()==10&&food.getSaturationLevel()==5,"All spear materials must have zero lunge exhaustion: "+item);
            var victim=c.spawn(EntityTypes.COW,new BlockPos(3,2,3));victim.setPos(owner.position().add(0,0,3.5));float health=victim.getHealth();
            for(int i=0;i<50;i++){victim.invulnerableTime=0;owner.stabAttack(EquipmentSlot.MAINHAND,victim,.01F,true,false,false);food.tick(owner);}
            c.assertTrue(victim.getHealth()<health&&food.getFoodLevel()==10&&food.getSaturationLevel()==5,"Successful combat stabs must also cost no exhaustion for every spear material: "+item);victim.discard();
        }
        owner.causeFoodExhaustion(5);food.tick(owner);c.assertTrue(food.getSaturationLevel()==4,"Normal sprint/damage hunger outside spear actions must remain vanilla");c.succeed();
    }

    @GameTest(maxTicks=80,padding=64,environment="acceleration_gametest:actual_end_reentry_spawns_one_upgraded_fight")
    public void actualEndReentrySpawnsOneUpgradedFight(GameTestHelper c){
        setup(c);AscendedDragon.clear();var owner=player(c);Vec3 original=owner.position();var end=c.getLevel().getServer().getLevel(Level.END);var saved=end.getDragonFight();
        var completed=new net.minecraft.world.level.dimension.end.EnderDragonFight(false,true,true,Optional.empty(),0,Optional.empty(),Optional.of(new BlockPos(0,64,0)),List.of(0),List.of());completed.init(end,21,BlockPos.ZERO);
        var first=new net.minecraft.world.level.dimension.end.EnderDragonFight(false,false,false,Optional.empty(),0,Optional.empty(),Optional.of(new BlockPos(0,64,0)),List.of(0),List.of());first.init(end,20,BlockPos.ZERO);end.setDragonFight(first);
        AscendedDragon.observeDimension(owner);
        c.runAtTickTime(5,()->{
            owner.teleportTo(end,10,80,10,Set.of(),0,0,true);owner.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());AscendedDragon.observeDimension(owner);
            c.assertTrue(end.getDragons().stream().noneMatch(AscendedDragon::upgraded),"Entering before the first defeat must not start an upgraded encounter");
        });
        c.runAtTickTime(10,()->{
            owner.teleportTo(c.getLevel(),original.x,original.y,original.z,Set.of(),0,0,true);owner.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());AscendedDragon.observeDimension(owner);
            end.getDragons().forEach(Entity::discard);end.setDragonFight(completed);
        });
        c.runAtTickTime(15,()->{
            owner.teleportTo(end,10,80,10,Set.of(),0,0,true);owner.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());AscendedDragon.observeDimension(owner);
            c.assertTrue(completed.dragonUUID()!=null&&!((dev.acceleration.mixin.DragonFightAccess)completed).acceleration$isKilled(),"End reentry after the first defeat must initiate a tracked new fight");
        });
        c.runAtTickTime(45,()->{
            try{
                var boss=(net.minecraft.world.entity.boss.enderdragon.EnderDragon)end.getEntity(completed.dragonUUID());
                c.assertTrue(boss!=null&&AscendedDragon.upgraded(boss)&&boss.getMaxHealth()==1000,"The automatically spawned upgraded dragon must become visible after End chunks and the client load complete");
                var uuid=boss.getUUID();AscendedDragon.observeDimension(owner);
                c.assertTrue(completed.dragonUUID().equals(uuid)&&end.getDragons().stream().filter(Entity::isAlive).count()==1,"The same End visit must not repeatedly spawn dragons");c.succeed();
            }finally{
                end.getDragons().forEach(Entity::discard);end.setDragonFight(saved);
                owner.teleportTo(c.getLevel(),original.x,original.y,original.z,Set.of(),0,0,true);owner.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());AscendedDragon.clear();
            }
        });
    }


}
