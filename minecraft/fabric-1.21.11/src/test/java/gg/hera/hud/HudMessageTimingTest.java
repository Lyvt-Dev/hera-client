package gg.hera.hud;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HudMessageTimingTest {
 @Test void titleUsesFadeInStayAndFadeOutDurations(){assertEquals(0,HudMessageTiming.title(100,10,70,20,0));assertEquals(.5f,HudMessageTiming.title(95,10,70,20,0));assertEquals(1,HudMessageTiming.title(50,10,70,20,0));assertEquals(.5f,HudMessageTiming.title(10,10,70,20,0));assertEquals(0,HudMessageTiming.title(0,10,70,20,0));}
 @Test void actionbarFadesContinuouslyAndStopsAtExpiry(){assertEquals(1,HudMessageTiming.action(40,0));assertEquals(.5f,HudMessageTiming.action(10,0));assertEquals(.475f,HudMessageTiming.action(10,.5f));assertEquals(0,HudMessageTiming.action(0,.8f));}
 @Test void zeroFadeDurationsAndPartialTickBoundsAreSafe(){assertEquals(1,HudMessageTiming.title(4,0,4,0,0));assertEquals(0,HudMessageTiming.title(-1,0,0,0,0));assertEquals(0,HudMessageTiming.action(-5,0));assertEquals(.95f,HudMessageTiming.action(20,4));}
}
