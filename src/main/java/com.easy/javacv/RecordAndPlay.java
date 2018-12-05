package com.easy.javacv;

import java.io.*;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.TargetDataLine;
public class RecordAndPlay {
    static volatile boolean stop=false;
    public static void main(String[] args) {
        Play();
    }
    //播放音频文件
    public static void Play() {

        try {
            AudioFormat audioFormat =
//                    new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100F,
//                    8, 1, 1, 44100F, false);
                    new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,44100F, 16, 2, 4,
                            44100F, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat); TargetDataLine targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(audioFormat);
            final SourceDataLine sourceDataLine;
            info = new DataLine.Info(SourceDataLine.class, audioFormat);
            sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
            sourceDataLine.open(audioFormat);
            targetDataLine.start();
            sourceDataLine.start();
            FloatControl fc=(FloatControl)sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
            double value=2;
            float dB = (float)
                    (Math.log(value==0.0?0.0001:value)/Math.log(10.0)*20.0);
            fc.setValue(dB);
            int nByte = 0;
            final int bufSize=4*100;
            byte[] buffer = new byte[bufSize];
            while (nByte != -1) {
                //System.in.read();
                nByte = targetDataLine.read(buffer, 0, bufSize);
                sourceDataLine.write(buffer, 0, nByte);
            }
            sourceDataLine.stop();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
