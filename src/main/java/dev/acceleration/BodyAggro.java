package dev.acceleration;

import java.util.Comparator;
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
        var conditions=TargetingConditions.forCombat().range(range);
        PlayerBody target=world.getEntitiesOfClass(PlayerBody.class,mob.getBoundingBox().inflate(range),body->{
            var session=SoulState.get(world.getServer()).body(body.getUUID());
            var owner=session==null?null:world.getServer().getPlayerList().getPlayer(session.owner());
            return owner!=null&&owner.isAlive()&&!owner.isCreative()&&!owner.isSpectator()
                &&conditions.test(world,mob,body);
        }).stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        if(target==null)return;
        mob.setTarget(target);
        var brain=mob.getBrain();
        if(brain.checkMemory(MemoryModuleType.ATTACK_TARGET,MemoryStatus.REGISTERED))
            brain.setMemory(MemoryModuleType.ATTACK_TARGET,target);
        if(mob instanceof Warden warden&&warden.getTarget()!=target)warden.setAttackTarget(target);
    }
}
