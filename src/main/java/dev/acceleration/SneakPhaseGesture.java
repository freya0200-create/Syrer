package dev.acceleration;
/** One short sneak tap, release, then a second held press. Tick rate: 20 Hz. */
public final class SneakPhaseGesture {
    private boolean down,armed,second,fired;
    private int held,gap;
    public void reset(){down=armed=second=fired=false;held=gap=0;}
    public boolean tick(boolean pressed){
        if(pressed){
            if(!down){second=armed&&gap<=8;armed=false;held=0;fired=false;}
            down=true;held++;
            if(second&&!fired&&held>=10){fired=true;return true;}
        }else{
            if(down){armed=!second&&held<=5;gap=0;}
            down=false;second=false;
            if(armed&&++gap>8)armed=false;
        }
        return false;
    }
}
