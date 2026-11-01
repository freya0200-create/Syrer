package dev.acceleration;
import java.util.*;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.level.*;
import net.minecraft.world.level.saveddata.*;
import net.minecraft.world.phys.Vec3;
public final class SoulState extends SavedData {
    public record Projection(UUID owner,ResourceKey<Level> dimension,Vec3 anchor,float yaw,float pitch,GameType mode,Abilities.Packed abilities,UUID body,long until){
        public static final Codec<Projection> CODEC=RecordCodecBuilder.create(i->i.group(
            UUIDUtil.STRING_CODEC.fieldOf("owner").forGetter(Projection::owner),Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Projection::dimension),
            Vec3.CODEC.fieldOf("anchor").forGetter(Projection::anchor),Codec.FLOAT.fieldOf("yaw").forGetter(Projection::yaw),Codec.FLOAT.fieldOf("pitch").forGetter(Projection::pitch),
            GameType.CODEC.fieldOf("mode").forGetter(Projection::mode),Abilities.Packed.CODEC.fieldOf("abilities").forGetter(Projection::abilities),
            UUIDUtil.STRING_CODEC.fieldOf("body").forGetter(Projection::body),Codec.LONG.fieldOf("until").forGetter(Projection::until)).apply(i,Projection::new));
    }
    public static final Codec<SoulState> CODEC=RecordCodecBuilder.create(i->i.group(
        Projection.CODEC.listOf().optionalFieldOf("projections",List.of()).forGetter(s->new ArrayList<>(s.projections.values())),
        UUIDUtil.STRING_CODEC.listOf().optionalFieldOf("rewarded",List.of()).forGetter(s->new ArrayList<>(s.rewarded))).apply(i,SoulState::new));
    private static final SavedDataType<SoulState> TYPE=new SavedDataType<>(AccelerationMod.id("soul_state"),SoulState::new,CODEC,null);
    private final Map<UUID,Projection> projections=new HashMap<>();private final Set<UUID> rewarded=new HashSet<>();
    public SoulState(){this(List.of(),List.of());}
    private SoulState(List<Projection> projections,List<UUID> rewarded){projections.forEach(p->this.projections.put(p.owner(),p));this.rewarded.addAll(rewarded);}
    public static SoulState get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(TYPE);}
    public Projection projection(UUID owner){return projections.get(owner);}
    public Projection body(UUID body){return projections.values().stream().filter(p->p.body().equals(body)).findFirst().orElse(null);}
    public void put(Projection projection){projections.put(projection.owner(),projection);setDirty();}
    public void remove(UUID owner){if(projections.remove(owner)!=null)setDirty();}
    public boolean rewarded(UUID dragon){return rewarded.contains(dragon);}
    public void reward(UUID dragon){rewarded.add(dragon);setDirty();}
}
