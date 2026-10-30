package dev.acceleration;

import java.util.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class BlockPhasing {
    private record Anchor(ServerLevel world,Vec3 position){}
    private static final Map<UUID,Anchor> SAFE=new HashMap<>();
    private BlockPhasing(){}
    public static int mode(Player player){return player.getEntityData().get(ProjectionPhysics.PHASE_MODE);}
    public static String modeName(Player player){return switch(mode(player)){case 1->"Underneath Blocks Phasing";case 2->"On Top of Blocks Phasing";default->"Wall Phasing";};}
    public static boolean cycleMode(ServerPlayer player){
        if(!player.isAlive()||player.isSpectator()||!AccelerationMod.carrying(player))return false;
        player.getEntityData().set(ProjectionPhysics.PHASE_MODE,(mode(player)+1)%3);
        sync(player,active(player));player.sendOverlayMessage(Component.literal(modeName(player)+" — "+(active(player)?"ON":"OFF (tap Sneak, then hold Sneak)")));return true;
    }
    public static boolean active(Player player){return player.getEntityData().get(ProjectionPhysics.PHASING);}
    private static void sync(ServerPlayer player,boolean on){
        player.getEntityData().set(ProjectionPhysics.PHASING,on);
        if(ServerPlayNetworking.canSend(player,SoulPackets.PhaseState.TYPE))ServerPlayNetworking.send(player,new SoulPackets.PhaseState(on,mode(player)));
    }
    public static boolean toggle(ServerPlayer player){
        if(active(player))return stop(player);
        if(!player.isAlive()||player.isSpectator()||player.isPassenger()||!AccelerationMod.carrying(player))return false;
        if(clear(player,player.position()))SAFE.put(player.getUUID(),new Anchor(player.level(),player.position()));
        sync(player,true);player.sendOverlayMessage(Component.literal(modeName(player)+" ON — 7 changes mode"));return true;
    }
    public static boolean step(ServerPlayer player){
        if(!player.isAlive()||player.isSpectator()||player.isPassenger()||!AccelerationMod.carrying(player))return false;
        Vec3 direction;
        if(mode(player)==1)direction=new Vec3(0,-1,0);
        else if(mode(player)==2)direction=new Vec3(0,1,0);
        else{
            Vec3 look=player.getLookAngle();
            direction=Math.abs(look.x)>Math.abs(look.z)?new Vec3(Math.signum(look.x),0,0):new Vec3(0,0,Math.signum(look.z));
        }
        Vec3 start=player.position(),exit=null;
        double first=-1;
        double extent=mode(player)==0?player.getBbWidth():player.getBbHeight();
        for(double distance=0;distance<=4;distance+=.125){
            Vec3 position=start.add(direction.scale(distance));
            boolean open=clear(player,position);
            if(!open&&first<0)first=distance;
            if(first>=0&&distance-first>1+extent+.125)break;
            if(first>=0&&open){exit=position;break;}
            if(first<0&&distance>=1.5)break;
        }
        if(first<0&&clear(player,start.add(direction)))exit=start.add(direction);
        if(exit==null){player.sendOverlayMessage(Component.literal("One-block phase needs clear space beyond a single block."));return false;}
        Vec3 velocity=player.getDeltaMovement();
        player.teleportTo(player.level(),exit.x,exit.y,exit.z,Set.of(),player.getYRot(),player.getXRot(),true);
        player.setDeltaMovement(velocity);player.fallDistance=0;
        if(active(player))SAFE.put(player.getUUID(),new Anchor(player.level(),exit));
        return true;
    }
    private static boolean clear(ServerPlayer p,Vec3 pos){
        return pos.y>=p.level().getMinY()+1&&pos.y+p.getBbHeight()<p.level().getMaxY()
            &&p.level().getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(pos))
            &&p.level().noCollision(p,p.getBoundingBox().move(pos.subtract(p.position())));
    }
    public static boolean stop(ServerPlayer player){
        if(!active(player)){SAFE.remove(player.getUUID());return false;}
        if(!player.isAlive()){sync(player,false);SAFE.remove(player.getUUID());return true;}
        Vec3 exit=clear(player,player.position())?player.position():null;
        if(exit==null){
            outer:for(int r=1;r<=6;r++)for(int y=-r;y<=r;y++)for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++){
                if(Math.max(Math.abs(x),Math.max(Math.abs(y),Math.abs(z)))!=r)continue;
                Vec3 point=player.position().add(x,y,z);if(clear(player,point)){exit=point;break outer;}
            }
        }
        var anchor=SAFE.get(player.getUUID());
        if(exit==null&&anchor!=null&&anchor.world()==player.level()&&clear(player,anchor.position()))exit=anchor.position();
        if(exit==null){player.sendOverlayMessage(Component.literal("Move toward open space before disabling phasing."));return false;}
        if(exit.distanceToSqr(player.position())>1e-8){
            player.teleportTo(player.level(),exit.x,exit.y,exit.z,Set.of(),player.getYRot(),player.getXRot(),true);
            player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;
        }
        sync(player,false);SAFE.remove(player.getUUID());player.sendOverlayMessage(Component.literal("Block phasing OFF"));return true;
    }
    public static void tick(ServerPlayer player){
        if(!active(player))return;
        if(!player.isAlive()||player.isSpectator()||!AccelerationMod.carrying(player)){stop(player);return;}
        if(clear(player,player.position()))SAFE.put(player.getUUID(),new Anchor(player.level(),player.position()));
    }
    public static void forget(ServerPlayer player){stop(player);SAFE.remove(player.getUUID());}
}
