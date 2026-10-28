package dev.acceleration;
import java.util.*;
import dev.acceleration.mixin.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.*;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonStrafePlayerPhase;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.level.*;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.phys.Vec3;

public final class AscendedDragon {
    public static final String TAG="acceleration_ascended_dragon",EXTRA="acceleration_volley_extra",SEED="acceleration_volley_seed";
    public static final double HEALTH=1000;
    private record Volley(ResourceKey<Level> dimension,UUID dragon,UUID target,Vec3 direction,long next,int remaining){}
    private static final List<Volley> VOLLEYS=new ArrayList<>();
    private static final Map<UUID,ResourceKey<Level>> DIMENSIONS=new HashMap<>();
    private AscendedDragon(){}
    public static boolean upgraded(EnderDragon dragon){return dragon.entityTags().contains(TAG);}
    public static void upgrade(EnderDragon dragon){
        boolean fresh=!upgraded(dragon);dragon.addTag(TAG);dragon.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH);
        if(fresh){dragon.setHealth((float)HEALTH);dragon.setCustomName(Component.literal("Ascended Ender Dragon"));}
    }
    public static EnderDragon spawnOnReturn(ServerLevel world,EnderDragonFight fight){
        if(fight==null||!fight.hasPreviouslyKilledDragon())return null;
        var access=(DragonFightAccess)fight;
        if(!access.acceleration$isKilled()||access.acceleration$respawnStage()!=null||world.getDragons().stream().anyMatch(Entity::isAlive))return null;
        access.acceleration$setKilled(false);
        EnderDragon dragon=access.acceleration$create();
        if(dragon==null){access.acceleration$setKilled(true);fight.setDirty();return null;}
        upgrade(dragon);access.acceleration$portal(false);fight.setDirty();return dragon;
    }
    public static void observeDimension(ServerPlayer player){
        var current=player.level().dimension();var previous=DIMENSIONS.put(player.getUUID(),current);
        if(previous!=null&&!previous.equals(current)&&current.equals(Level.END))spawnOnReturn(player.level(),player.level().getDragonFight());
    }
    private static void fireballAdded(DragonFireball ball,ServerLevel world){
        if(ball.entityTags().contains(EXTRA)||ball.entityTags().contains(SEED)||!(ball.getOwner() instanceof EnderDragon dragon)||!upgraded(dragon))return;
        ball.addTag(SEED);
        LivingEntity target=null;
        if(dragon.getPhaseManager().getCurrentPhase() instanceof DragonStrafePlayerPhase phase)target=((DragonTargetAccess)phase).acceleration$target();
        Vec3 direction=ball.getDeltaMovement().normalize();
        if(direction.lengthSqr()<.001)direction=dragon.getLookAngle();
        VOLLEYS.add(new Volley(world.dimension(),dragon.getUUID(),target==null?null:target.getUUID(),direction,world.getGameTime()+10,2));
    }
    public static void tick(net.minecraft.server.MinecraftServer server){
        for(var player:server.getPlayerList().getPlayers())observeDimension(player);
        var iterator=VOLLEYS.listIterator();
        while(iterator.hasNext()){
            var volley=iterator.next();var world=server.getLevel(volley.dimension());
            if(world==null){iterator.remove();continue;}
            var entity=world.getEntity(volley.dragon());
            if(!(entity instanceof EnderDragon dragon)||!dragon.isAlive()||!upgraded(dragon)){iterator.remove();continue;}
            if(world.getGameTime()<volley.next())continue;
            Vec3 origin=dragon.head.position().add(0,.5,0),direction=volley.direction();
            var target=volley.target()==null?null:world.getEntity(volley.target());
            if(target instanceof LivingEntity living&&living.isAlive()&&!ProjectionPhysics.active(living))direction=living.getEyePosition().subtract(origin).normalize();
            var shot=new DragonFireball(world,dragon,direction);shot.addTag(EXTRA);shot.setPos(origin.add(direction.scale(.5)));world.addFreshEntity(shot);
            world.levelEvent(1017,dragon.blockPosition(),0);
            if(volley.remaining()==1)iterator.remove();
            else iterator.set(new Volley(volley.dimension(),volley.dragon(),volley.target(),volley.direction(),world.getGameTime()+10,volley.remaining()-1));
        }
    }
    public static void clear(){VOLLEYS.clear();DIMENSIONS.clear();}
    public static void initialize(){
        ServerEntityEvents.ENTITY_LOAD.register((entity,world)->{
            if(entity instanceof EnderDragon dragon&&upgraded(dragon))upgrade(dragon);
            if(entity instanceof DragonFireball ball)fireballAdded(ball,world);
        });
        ServerTickEvents.END_SERVER_TICK.register(AscendedDragon::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->DIMENSIONS.remove(handler.player.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPING.register(server->clear());
    }
}
