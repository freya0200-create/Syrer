package dev.acceleration;
public final class AccelerationRules {
    public static final int CHARGE_TICKS=100;
    private AccelerationRules(){}
    public static int charge(int previous,boolean carrying,boolean sprinting){return !carrying?0:Math.clamp(previous+(sprinting?1:-4),0,CHARGE_TICKS);}
    public static double speedBonus(int charge){return Math.clamp(charge,0,CHARGE_TICKS)*3.0/CHARGE_TICKS;}
}
