package dev.acceleration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AccelerationRulesTest {
    @Test void accelerationBuildsGradually(){long c=0;for(int i=0;i<100;i++)c=AccelerationRules.charge(c,true,true);assertEquals(.60,AccelerationRules.speedBonus(c),1e-9);assertEquals(1.56,AccelerationRules.speedBonus(260),1e-9);assertEquals(12,AccelerationRules.speedBonus(2000),1e-9);}
    @Test void slowingAndRemovingSpearClearTheBonus(){long c=100;for(int i=0;i<25;i++)c=AccelerationRules.charge(c,true,false);assertEquals(0,c);assertEquals(0,AccelerationRules.charge(100,false,true));assertEquals(0,AccelerationRules.speedBonus(-10));}
    @Test void spearUsePreservesChargeWithoutFreeGrowth(){assertEquals(100,AccelerationRules.charge(100,true,false,true));assertEquals(0,AccelerationRules.charge(0,true,false,true));assertEquals(101,AccelerationRules.charge(100,true,true,true));assertEquals(0,AccelerationRules.charge(100,false,false,true));}
    @Test void continuedRunningHasNoGameplayCap(){assertEquals(6001,AccelerationRules.charge(6000,true,true));assertEquals(36,AccelerationRules.speedBonus(6000),1e-9);assertEquals(360,AccelerationRules.speedBonus(60000),1e-9);}
}
