package dev.scyer;

/** Combat tuned to fifty fully charged Density V mace smashes with six blocks of actual fall. */
public final class ScyerBalance {
    private ScyerBalance() {}
    public static int stage(int stage) { return Math.clamp(stage, 1, 4); }
    public static float health(int stage) { return 1000 + (stage(stage) - 1) * 8; }
    public static double attack(int stage) { return 10; } // Netherite sword + Sharpness III.
    public static float maceDamage(int density, float fall) {
        float smash = fall <= 3 ? 4 * fall : fall <= 8 ? 12 + 2 * (fall - 3) : 22 + fall - 8;
        return 6 + smash + Math.max(0, density) * .5F * fall;
    }
    public static float normalizedSmash(float actualAfterArmor, float referenceAfterArmor, float maxHealth) {
        return actualAfterArmor <= 0 ? 0 : referenceAfterArmor <= 0 ? actualAfterArmor
            : Math.max(0, actualAfterArmor) / referenceAfterArmor * (maxHealth / 50F) + .0001F;
    }
}
