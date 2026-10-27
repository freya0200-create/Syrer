package dev.acceleration;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
public final class DragonReward {
    private DragonReward(){}
    public static boolean drop(ServerLevel world,UUID dragon,BlockPos exit){
        var state=SoulState.get(world.getServer());if(state.rewarded(dragon))return false;
        BlockPos base=exit==null?new BlockPos(0,world.getSeaLevel(),0):exit;
        BlockPos top=world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,base);
        outer:for(int y=base.getY()-2;y<=base.getY()+12;y++)for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
            BlockPos at=new BlockPos(base.getX()+x,y,base.getZ()+z);
            if(world.getBlockState(at).is(Blocks.DRAGON_EGG)){top=at.above();break outer;}
        }
        ItemEntity reward=new ItemEntity(world,top.getX()+.5,top.getY()+.5,top.getZ()+.5,AccelerationSpear.create(world));
        reward.setDeltaMovement(0,0,0);reward.setUnlimitedLifetime();reward.setInvulnerable(true);reward.setPickUpDelay(20);
        if(!world.addFreshEntity(reward))return false;state.reward(dragon);return true;
    }
}
