package dev.acceleration;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;

/** The visible linked body is the player-shaped threat; never the hidden projection. */
public final class BodyAggro {
    private BodyAggro(){}
    public static void targetBody(Mob mob){
        if(!(mob.level() instanceof ServerLevel world)||!(mob instanceof Enemy)||!mob.isAlive()||mob.isNoAi())return;
        var current=mob.getTarget();
        if(current!=null&&current.isAlive()&&!ProjectionPhysics.active(current)&&!(current instanceof PlayerBody))return;
        double range=mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        var state=SoulState.get(world.getServer());
        TargetingConditions conditions=null;
        net.minecraft.world.phys.AABB search=null;
        PlayerBody target=null;double nearest=Double.POSITIVE_INFINITY;
        // There can be at most one body per online player. Avoid scanning entity sections
        // and constructing a list/stream for every hostile mob on every AI tick.
        for(var owner:world.getServer().getPlayerList().getPlayers()){
            var session=state.projection(owner.getUUID());
            if(session==null||!session.dimension().equals(world.dimension())||!owner.isAlive()||owner.isCreative()||owner.isSpectator())continue;
            var entity=world.getEntity(session.body());
            if(!(entity instanceof PlayerBody body)||body.isRemoved())continue;
            if(search==null)search=mob.getBoundingBox().inflate(range);
            if(!search.intersects(body.getBoundingBox()))continue;
            if(mob instanceof net.minecraft.world.entity.NeutralMob neutral&&!neutral.isAngryAt(owner,world)&&mob.getLastHurtByMob()!=owner&&current!=body)continue;
            if(conditions==null)conditions=TargetingConditions.forCombat().range(range);
            if(!conditions.test(world,mob,body))continue;
            double distance=mob.distanceToSqr(body);
            if(distance<nearest){nearest=distance;target=body;}
        }
        if(target==null)return;
        mob.setTarget(target);
        var brain=mob.getBrain();
        if(brain.checkMemory(MemoryModuleType.ATTACK_TARGET,MemoryStatus.REGISTERED))
            brain.setMemory(MemoryModuleType.ATTACK_TARGET,target);
        if(mob instanceof Warden warden&&warden.getTarget()!=target)warden.setAttackTarget(target);
    }
}
