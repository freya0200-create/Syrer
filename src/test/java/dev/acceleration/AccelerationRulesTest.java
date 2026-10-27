package dev.acceleration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AccelerationRulesTest {
    @Test void sprintBuildsToFourTimesNormalInFiveSeconds(){int c=0;for(int i=0;i<100;i++)c=AccelerationRules.charge(c,true,true);assertEquals(100,c);assertEquals(3,AccelerationRules.speedBonus(c));assertEquals(100,AccelerationRules.charge(c,true,true));}
    @Test void slowingAndRemovingSpearClearTheBonus(){int c=100;for(int i=0;i<25;i++)c=AccelerationRules.charge(c,true,false);assertEquals(0,c);assertEquals(0,AccelerationRules.charge(100,false,true));assertEquals(0,AccelerationRules.speedBonus(-10));}
    @Test void rangesAndTimeLimitsAreBounded(){assertEquals(200,AccelerationRules.SOUL_TICKS);assertEquals(600,AccelerationRules.COOLDOWN_TICKS);assertTrue(AccelerationRules.STRIKE_RANGE<AccelerationRules.SOUL_RADIUS);assertEquals(3,AccelerationRules.speedBonus(10000));}
}
