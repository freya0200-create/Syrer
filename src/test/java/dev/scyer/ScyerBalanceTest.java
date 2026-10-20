package dev.scyer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScyerBalanceTest {
    @Test void sixBlockMaceReferenceMatchesNativeDamageFormula() {
        assertEquals(39, ScyerBalance.maceDamage(5, 6));
        assertEquals(27, ScyerBalance.maceDamage(1, 6));
        assertEquals(10, ScyerBalance.attack(1));
        assertEquals(10, ScyerBalance.attack(4));
        assertEquals(0, ScyerBalance.normalizedSmash(0,7.8F,1000));
    }
    @Test void allStagesSurviveFortyNineAndDieAtFiftyReferenceSmashes() {
        for(int stage=1;stage<=4;stage++) {
            float health=ScyerBalance.health(stage);
            for(int hit=0;hit<49;hit++) health-=ScyerBalance.normalizedSmash(7.8F,7.8F,ScyerBalance.health(stage));
            assertTrue(health>0);
            health-=ScyerBalance.normalizedSmash(7.8F,7.8F,ScyerBalance.health(stage));
            assertTrue(health<=0);
        }
    }
    @Test void weakerSmashesDoLessAndBreachIncreasesEffectiveDamage() {
        float baseline=ScyerBalance.normalizedSmash(7.8F,7.8F,1000);
        assertTrue(ScyerBalance.normalizedSmash(5.4F,7.8F,1000)<baseline);
        assertTrue(ScyerBalance.normalizedSmash(15.6F,7.8F,1000)>baseline);
        assertEquals(4,ScyerBalance.stage(100));assertEquals(1,ScyerBalance.stage(-1));
    }
}
