package dev.acceleration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AccelerationRulesTest {
    @Test void accelerationBuildsGradually(){long c=0;for(int i=0;i<100;i++)c=AccelerationRules.charge(c,true,true);assertEquals(.05,AccelerationRules.speedBonus(c),1e-9);assertEquals(.13,AccelerationRules.speedBonus(260),1e-9);assertEquals(1,AccelerationRules.speedBonus(2000));}
    @Test void slowingAndRemovingSpearClearTheBonus(){long c=100;for(int i=0;i<25;i++)c=AccelerationRules.charge(c,true,false);assertEquals(0,c);assertEquals(0,AccelerationRules.charge(100,false,true));assertEquals(0,AccelerationRules.speedBonus(-10));}
    @Test void continuedRunningHasNoGameplayCap(){assertEquals(6001,AccelerationRules.charge(6000,true,true));assertEquals(3,AccelerationRules.speedBonus(6000));assertEquals(30,AccelerationRules.speedBonus(60000));}
}
