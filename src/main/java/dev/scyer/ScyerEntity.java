package dev.scyer;

import java.util.Comparator;
import net.minecraft.world.DifficultyInstance;
import java.util.EnumSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** A relentless four-stage hunter. Tracking never depends on visibility, noise or posture. */
public final class ScyerEntity extends Monster {
    public static final ResourceKey<DamageType> RENDING = ResourceKey.create(Registries.DAMAGE_TYPE,
        Identifier.fromNamespaceAndPath("scyer", "rending"));
    private static final EntityDataAccessor<Boolean> ROARING = SynchedEntityData.defineId(ScyerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> STAGE = SynchedEntityData.defineId(ScyerEntity.class, EntityDataSerializers.INT);
    private long roarReadyAt, roaringUntil, strikeAt, investigateUntil, regenerateAfter;
    private Vec3 investigate;
    private LivingEntity attacker;
    private boolean deathRecorded;
    private int successfulStrikes;
    private ItemStack attackWeapon;

    public ScyerEntity(EntityType<? extends Monster> type, Level world) { this(type, world, 1); }
    public ScyerEntity(EntityType<? extends Monster> type, Level world, int stage) {
        super(type, world);
        if (world instanceof ServerLevel) { setStage(stage); ensureEffects(); }
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ROARING, false); builder.define(STAGE, 1);
    }
    public int stage() { return entityData.get(STAGE); }
    @Override protected net.minecraft.network.chat.Component getTypeName() {
        return net.minecraft.network.chat.Component.translatable(stage() == 1
            ? "entity.scyer.scyer" : "entity.scyer.scyer_level_" + stage());
    }
    public void setStage(int stage) {
        entityData.set(STAGE, Math.clamp(stage, 1, 4));
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(ScyerBalance.health(stage()));
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(ScyerBalance.attack(stage()));
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.38 + stage() * .025);
        getAttribute(Attributes.ARMOR).setBaseValue(0);
        ScyerGear.equip(this);
        setHealth(getMaxHealth());
    }
    @Override protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output); output.putInt("ScyerStage", stage());
        output.putLong("ScyerRegenerateAfter", regenerateAfter);
    }
    @Override protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        float health = getHealth();
        setStage(input.getIntOr("ScyerStage", stage()));
        setHealth(Math.min(health, getMaxHealth()));
        regenerateAfter = input.getLongOr("ScyerRegenerateAfter", 0);
    }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty,
            EntitySpawnReason reason, SpawnGroupData group) {
        var data = super.finalizeSpawn(world, difficulty, reason, group);
        if (reason == EntitySpawnReason.NATURAL && getType() == ScyerMod.SCYER)
            setStage(ScyerEvolution.get(world.getLevel().getServer()).nextStage());
        return data;
    }
    private void ensureEffects() {
        removeEffect(MobEffects.STRENGTH);
        var regen = getEffect(MobEffects.REGENERATION);
        if (level().getGameTime() < regenerateAfter) removeEffect(MobEffects.REGENERATION);
        else if (regen == null || regen.getAmplifier() != 1 || regen.isVisible())
            addEffect(new MobEffectInstance(MobEffects.REGENERATION, -1, 1, true, false, false));
    }
    @Override public void tick() {
        if (level() instanceof ServerLevel world) {
            ensureEffects();
            if (tickCount % 20 == 0) ScyerSpawning.keepAwake(world, blockPosition());
        }
        super.tick();
        if (level() instanceof ServerLevel && level().getGameTime() >= roaringUntil) entityData.set(ROARING, false);
    }
    @Override public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(world, source, amount);
        if (hurt) {
            regenerateAfter = world.getGameTime() + 100;
            removeEffect(MobEffects.REGENERATION);
        }
        if (hurt && source.getEntity() instanceof LivingEntity living && eligible(living)) {
            attacker = living;
            setTarget(living);
        }
        return hurt;
    }
    @Override public void die(DamageSource source) {
        if (!deathRecorded && !isRemoved() && level() instanceof ServerLevel world) {
            deathRecorded = true;
            ScyerEvolution.get(world.getServer()).recordDeath(stage());
        }
        super.die(source);
    }
    @Override protected void dropEquipment(ServerLevel world) { /* Only the explicit damaged drops below. */ }
    @Override protected void dropCustomDeathLoot(ServerLevel world, DamageSource source, boolean playerKill) {
        super.dropCustomDeathLoot(world, source, playerKill);
        for (ItemStack stack : ScyerGear.deathDrops(stage(), random)) spawnAtLocation(world, stack);
    }
    @Override public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        if (reason.shouldDestroy() && level() instanceof ServerLevel world)
            ScyerPopulation.get(world.getServer()).release(getUUID());
    }
    public boolean isRoaring() { return entityData.get(ROARING); }
    public boolean roar(ServerLevel world) {
        long now = world.getGameTime();
        if (now < roarReadyAt) return false;
        roarReadyAt = now + 400; roaringUntil = now + 20;
        entityData.set(ROARING, true);
        playSound(SoundEvents.WARDEN_ROAR, 3F, .65F);
        for (var player : world.players()) if (eligible(player) && distanceToSqr(player) <= 100) Fainting.apply(player);
        world.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + 3.2, getZ(), 1, 0, 0, 0, 0);
        return true;
    }
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 1000)
            .add(Attributes.MOVEMENT_SPEED, .405).add(Attributes.ATTACK_DAMAGE, 10)
            .add(Attributes.ARMOR, 0).add(Attributes.ARMOR_TOUGHNESS, 0)
            .add(Attributes.FOLLOW_RANGE, 100).add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this)); goalSelector.addGoal(1, new HuntGoal());
    }
    @Override public void playSound(SoundEvent sound, float volume, float pitch) {
        SoundSignals.withoutSelfEcho(() -> super.playSound(sound, volume, pitch));
    }
    @Override protected float getDamageAfterArmorAbsorb(DamageSource source, float amount) {
        return ScyerCombat.afterArmor(this, source, super.getDamageAfterArmorAbsorb(source, amount));
    }
    @Override public ItemStack getWeaponItem() { return attackWeapon == null ? ItemStack.EMPTY : attackWeapon; }
    @Override public boolean doHurtTarget(ServerLevel world, Entity target) {
        ensureEffects();
        boolean smash = successfulStrikes % 2 == 1;
        var enchantments = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        attackWeapon = new ItemStack(smash ? net.minecraft.world.item.Items.MACE : net.minecraft.world.item.Items.NETHERITE_SWORD);
        if (smash) {
            // A monster-only combined attack, rather than a player-obtainable incompatible enchantment recipe.
            attackWeapon.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.DENSITY), 1);
            attackWeapon.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.BREACH), 1);
        } else attackWeapon.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS), 3);
        DamageSource source = world.damageSources().mobAttack(this);
        float amount = smash ? ScyerBalance.maceDamage(1, 6) :
            net.minecraft.world.item.enchantment.EnchantmentHelper.modifyDamage(world, attackWeapon, target, source, 8);
        boolean hit = target.hurtServer(world, source, amount);
        if (hit) { successfulStrikes++; swing(InteractionHand.MAIN_HAND); }
        return hit;
    }
    private boolean eligible(LivingEntity actor) {
        return actor.isAlive() && !actor.isInvulnerable() && actor.level() == level() && !(actor instanceof ScyerEntity)
            && (!(actor instanceof Player player) || (!player.isCreative() && !player.isSpectator()));
    }
    /** Hearing supplements tracking; it can never interrupt pursuit or stun the hunter. */
    public void hear(Vec3 position, Entity source) {
        if (source instanceof ScyerEntity || position.distanceToSqr(position()) > 10000) return;
        if (source instanceof Player player && (player.isCreative() || player.isSpectator())) return;
        investigate = position; investigateUntil = level().getGameTime() + 100;
    }
    private final class HuntGoal extends Goal {
        private long repathAt;
        HuntGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() { return true; }
        @Override public boolean canContinueToUse() { return true; }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void tick() {
            if (!(level() instanceof ServerLevel world)) return;
            long now = world.getGameTime();
            LivingEntity victim = world.players().stream().filter(p -> eligible(p) && distanceToSqr(p) <= 10000)
                .min(Comparator.comparingDouble(ScyerEntity.this::distanceToSqr)).orElse(null);
            if (victim == null) {
                if (attacker != null && eligible(attacker) && distanceToSqr(attacker) <= 10000) victim = attacker;
                else victim = world.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(100),
                    entity -> eligible(entity) && distanceToSqr(entity) <= 10000).stream()
                    .min(Comparator.comparingDouble(ScyerEntity.this::distanceToSqr)).orElse(null);
            }
            boolean changed = getTarget() != victim;
            setTarget(victim);
            Vec3 destination = victim != null ? victim.position() : now < investigateUntil ? investigate : null;
            if (destination == null) { getNavigation().stop(); return; }
            getLookControl().setLookAt(destination.x, destination.y + 1, destination.z);
            if (changed || now >= repathAt) {
                getNavigation().moveTo(destination.x, destination.y, destination.z, 1.35);
                repathAt = now + 5;
            }
            // Strike before roaring: no windup, crouch immunity, food pause or confusion.
            if (victim != null && distanceToSqr(victim) <= 12.25 && hasLineOfSight(victim) && now >= strikeAt) {
                doHurtTarget(world, victim); strikeAt = now + 10;
            }
            if (victim instanceof Player && distanceToSqr(victim) <= 100) roar(world);
        }
    }
}
