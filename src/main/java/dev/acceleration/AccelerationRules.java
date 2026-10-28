package dev.acceleration;
public final class AccelerationRules {
    private AccelerationRules(){}
    public static long charge(long previous,boolean carrying,boolean sprinting){
        if(!carrying)return 0;
        if(sprinting)return previous==Long.MAX_VALUE?previous:Math.max(0,previous)+1;
        return Math.max(0,previous-Math.max(4,previous/10));
    }
    // Seven percent additional speed per second of continuous running. No gameplay cap.
    public static double speedBonus(long charge){return Math.max(0,charge)*.07/20.0;}
}
