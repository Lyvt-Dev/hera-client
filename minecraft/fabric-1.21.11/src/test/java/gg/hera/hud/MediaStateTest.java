package gg.hera.hud;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MediaStateTest {
    @Test void playbackProgressInterpolatesAndStopsAtDuration(){var playing=new MediaState.Track(true,"Track","Artist","Spotify",true,2000,10000,1000);assertEquals(3500,MediaState.positionAt(playing,2500));assertEquals(10000,MediaState.positionAt(playing,20000));}
    @Test void pausedAndUnknownDurationAreNotInvented(){var paused=new MediaState.Track(true,"Track","Artist","Browser",false,2000,10000,1000);assertEquals(2000,MediaState.positionAt(paused,5000));var unknown=new MediaState.Track(true,"Live","Artist","Browser",true,2000,0,1000);assertEquals(0,MediaState.positionAt(unknown,5000));}
}
