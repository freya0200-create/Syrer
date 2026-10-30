package dev.acceleration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SneakPhaseGestureTest {
    @Test void tapThenHoldTogglesOnlyOnce(){
        var gesture=new SneakPhaseGesture();
        assertFalse(gesture.tick(true));assertFalse(gesture.tick(false));
        for(int i=0;i<9;i++)assertFalse(gesture.tick(true));
        assertTrue(gesture.tick(true));for(int i=0;i<40;i++)assertFalse(gesture.tick(true));
        gesture.tick(false);gesture.tick(true);gesture.tick(false);
        for(int i=0;i<9;i++)assertFalse(gesture.tick(true));assertTrue(gesture.tick(true));
    }
    @Test void normalSneakingAndExpiredDoubleTapDoNotToggle(){
        var gesture=new SneakPhaseGesture();for(int i=0;i<40;i++)assertFalse(gesture.tick(true));
        gesture.tick(false);for(int i=0;i<20;i++)assertFalse(gesture.tick(true));gesture.reset();
        gesture.tick(true);for(int i=0;i<10;i++)gesture.tick(false);
        for(int i=0;i<20;i++)assertFalse(gesture.tick(true));
    }
    @Test void openingMenuClearsGesture(){
        var gesture=new SneakPhaseGesture();gesture.tick(true);gesture.tick(false);gesture.reset();
        for(int i=0;i<20;i++)assertFalse(gesture.tick(true));
    }
}
