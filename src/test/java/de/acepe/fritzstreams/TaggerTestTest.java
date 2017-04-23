package de.acepe.fritzstreams;

import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class TaggerTestTest {

    @Test
    public void test() throws Exception {
        String comment = "joe goddard - lose your love\n"
                + "the penelopes - this is not america – miguel campbell mix"
                + "xinobi - see me\n"
                + "way out west - set my mind – brassica mix\n"
                + "thundercat w/ michael mcdonald & kenny loggins - show you the way\n"
                + "matthew e white & flo morrissey - everybody loves the sunshine\n"
                + "jim sharp - hangin out\n"
                + "hade - range rover hse\n"
                + "trentemøller w/ marie fisker - one eye open – unkle reconstruction\n"
                + "deko deko - what happened – lootbeg mix\n"
                + "jonatan bäckelie - it could go either way – trickski instrumental\n"
                + "glow in the dark - screams under stars\n"
                + "syd - all about me\n"
                + "stray - blink\n"
                + "sully - helios – philip d kick mix\n"
                + "lewis james - spiller\n"
                + "x-static - my inspiration – bjarki mix\n"
                + "mixhell & joe goddard w/ mutado pintado - hard work pays off – club edit\n"
                + "marvin & guy w/ zombies in miami - despierta (tus sentidos)\n"
                + "black loops - baustelle\n"
                + "rimbaudian - drop it on em\n"
                + "baltra - a\n"
                + "folamour - shakkei\n"
                + "folamour - each day is a first day\n"
                + "soul clap w/ ebony houston - numb – marston & karp mix\n"
                + "mr confuse - let the music play – instrumental\n"
                + "the halftone society rhythm section - caribbean queen – dub\n"
                + "pat van dyke w/ melinda camille - love you inside & out\n"
                + "jonwayne - out of sight\n"
                + "bonobo - figures\n"
                + "otik - cautionary tales\n"
                + "gidge - lit\n"
                + "the penelopes - tina – oli petit mix";

        File testFile = new File("//media/omv-media/Podcasts/Fritz/Club_2017-02-12.mp3");
        MP3File f = (MP3File) AudioFileIO.read(testFile);
        f.getTag().setField(FieldKey.COMMENT, comment);

        f.save();
    }
}