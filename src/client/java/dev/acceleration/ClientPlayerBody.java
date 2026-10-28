package dev.acceleration;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.level.Level;
import com.mojang.authlib.GameProfile;

public final class ClientPlayerBody extends PlayerBody implements ClientAvatarEntity {
    private final ClientAvatarState state=new ClientAvatarState();
    private GameProfile loadedProfile;
    private Supplier<PlayerSkin> skin;
    public ClientPlayerBody(EntityType<? extends PlayerBody> type,Level world){super(type,world);}
    @Override public ClientAvatarState avatarState(){return state;}
    @Override public PlayerSkin getSkin(){
        var profile=getProfile().partialProfile();
        if(skin==null||!profile.equals(loadedProfile)){
            loadedProfile=profile;skin=Minecraft.getInstance().getSkinManager().createLookup(profile,false);
        }
        return skin.get();
    }
    @Override public Parrot.Variant getParrotVariantOnShoulder(boolean left){return null;}
    @Override public boolean showExtraEars(){return false;}
    @Override public void tick(){super.tick();state.tick(position(),getDeltaMovement());}
}
