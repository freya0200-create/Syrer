package dev.acceleration;
public final class AccelerationRules {
    private AccelerationRules(){}
    public static long charge(long previous,boolean carrying,boolean sprinting){return charge(previous,carrying,sprinting,false);}
    public static long charge(long previous,boolean carrying,boolean sprinting,boolean preserveSpearCharge){
        if(!carrying)return 0;
        if(sprinting)return previous==Long.MAX_VALUE?previous:Math.max(0,previous)+1;
        if(preserveSpearCharge)return Math.max(0,previous);
        return Math.max(0,previous-Math.max(4,previous/10));
    }
    // Twelve percent additional speed per second of continuous running. No gameplay cap.
    public static double speedBonus(long charge){return Math.max(0,charge)*.12/20.0;}
}
