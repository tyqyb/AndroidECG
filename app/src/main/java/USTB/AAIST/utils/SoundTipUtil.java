package USTB.AAIST.utils;
/**
 * 语音提示，
 * 在ECGChart.java中的47行和 public void run() {}函数中可以进行调用
 **/
import android.content.Context;

public class SoundTipUtil {
    private static KqwSpeechSynthesizer kqwSpeechSynthesizer;

    public static void soundTip(Context context,String text) {
        kqwSpeechSynthesizer = new KqwSpeechSynthesizer(context);
        kqwSpeechSynthesizer.start(text);
    }

}