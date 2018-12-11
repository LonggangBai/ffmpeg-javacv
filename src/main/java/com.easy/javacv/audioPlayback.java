package com.easy.javacv;

import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;

import javax.sound.sampled.*;
import javax.swing.*;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * 播放视频里面的音频
 *
 * 在javacv封装的ffmpeg包中，可以通过FFMpegFrameGrabber捕获image数据和sample数据，而且捕获的都是译码后的。下面通过一个Demo来实现如何处理sample数据。
 *
 * (1) 首先实例化一个FFMpegFrameGrabber对象，然后调用grabSamples对象就可以得到一个包含音频数据的帧对象Frame，其Buffer[] samples域就是存放的就是解码后的音频数据。samples数组的长度是1或者2，这个取决于采样数据格式的类型。
 *
 * (2) ffmpeg采样的数据类型在avutil.java文件中有描述，如下图所示，可以看到，采样数据的类型有无符号short，有符号short还有float，double，int等等，这些都可以对应于IntBuffer,FloatBuffer,DoubleBuffer,ShortBuffer。
 * 3)接着介绍ffmpeg中planar和非planer的概念:
 *
 * planar型：AV_SAMPLE_FMT中有加P后缀的比如FLTP，S16P等就是planar型，对于planar型的数据，返回的Frame中samples的长度为2，sample[0]就是存放立体声的第一个声道数据，sample[1]就是立体声的第二个声道数据。所以我们要将左右声道的数据合并成一个流后送入java的sourceDataLine中播放。
 *
 * 非planar型：没有加p后缀的类型，这类型返回的Frame中samples的长度为1，那么也就是左右声道的数据已经合并成一个流了，那么我们直接送入sourceDataLine播放即可。
 * ---------------------
 * 作者：_Thomas
 * 来源：CSDN
 * 原文：https://blog.csdn.net/a694543965/article/details/78387156
 * 版权声明：本文为博主原创文章，转载请附上博文链接！
 *
 *
 */
public class audioPlayback {
    private AudioFormat af = null;
    private SourceDataLine sourceDataLine;
    private DataLine.Info dataLineInfo;
    public audioPlayback(String musicPath) throws FrameGrabber.Exception {
        FFmpegFrameGrabber fg = new FFmpegFrameGrabber(musicPath);
        int sec = 60;
        Frame f;
        fg.start();
        //fg.setTimestamp(sec*1000000);//纯音频文件设置了时间戳有问题，视频没问题。
        sampleFormat = fg.getSampleFormat();
        printMusicInfo(fg);
        initSourceDataLine(fg);
        while(true){
            f = fg.grabSamples();
            if(f == null){
                fg.stop();
                System.exit(0);
            }
            processAudio(f.samples);
        }
    }
    Buffer[] buf;
    FloatBuffer leftData,rightData;
    ShortBuffer ILData,IRData;
    ByteBuffer TLData,TRData;
    float vol = 1;//音量
    int sampleFormat;
    byte[] tl,tr;
    byte[] combine;
    private void processAudio(Buffer[] samples) {
        int k;
        buf = samples;
        switch(sampleFormat){
            case avutil.AV_SAMPLE_FMT_FLTP://平面型左右声道分开。
                leftData = (FloatBuffer)buf[0];
                TLData = floatToByteValue(leftData,vol);
                rightData = (FloatBuffer)buf[1];
                TRData = floatToByteValue(leftData,vol);
                tl = TLData.array();
                tr = TRData.array();
                combine = new byte[tl.length+tr.length];
                k = 0;
                for(int i=0;i<tl.length;i=i+2) {//混合两个声道。
                    for (int j = 0; j < 2; j++) {
                        combine[j+4*k] = tl[i + j];
                        combine[j + 2+4*k] = tr[i + j];
                    }
                    k++;
                }
                sourceDataLine.write(combine,0,combine.length);
                break;
            case avutil.AV_SAMPLE_FMT_S16://非平面型左右声道在一个buffer中。
                ILData = (ShortBuffer)buf[0];
                TLData = shortToByteValue(ILData,vol);
                tl = TLData.array();
                sourceDataLine.write(tl,0,tl.length);
                break;
            case avutil.AV_SAMPLE_FMT_FLT://float非平面型
                leftData = (FloatBuffer)buf[0];
                TLData = floatToByteValue(leftData,vol);
                tl = TLData.array();
                sourceDataLine.write(tl,0,tl.length);
                break;
            case avutil.AV_SAMPLE_FMT_S16P://平面型左右声道分开
                ILData = (ShortBuffer)buf[0];
                IRData = (ShortBuffer)buf[1];
                TLData = shortToByteValue(ILData,vol);
                TRData = shortToByteValue(IRData,vol);
                tl = TLData.array();
                tr = TRData.array();
                combine = new byte[tl.length+tr.length];
                k = 0;
                for(int i=0;i<tl.length;i=i+2) {
                    for (int j = 0; j < 2; j++) {
                        combine[j+4*k] = tl[i + j];
                        combine[j + 2+4*k] = tr[i + j];
                    }
                    k++;
                }
                sourceDataLine.write(combine,0,combine.length);
                break;
            default:
                JOptionPane.showMessageDialog(null,"unsupport audio format","unsupport audio format",JOptionPane.ERROR_MESSAGE);
                System.exit(0);
                break;
        }
    }

    private void initSourceDataLine(FFmpegFrameGrabber fg) {
        switch(fg.getSampleFormat()){
            case avutil.AV_SAMPLE_FMT_U8://无符号short 8bit
                break;
            case avutil.AV_SAMPLE_FMT_S16://有符号short 16bit
                af = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,fg.getSampleRate(),16,fg.getAudioChannels(),fg.getAudioChannels()*2,fg.getSampleRate(),true);
                break;
            case avutil.AV_SAMPLE_FMT_S32:
                break;
            case avutil.AV_SAMPLE_FMT_FLT:
                af = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,fg.getSampleRate(),16,fg.getAudioChannels(),fg.getAudioChannels()*2,fg.getSampleRate(),true);
                break;
            case avutil.AV_SAMPLE_FMT_DBL:
                break;
            case avutil.AV_SAMPLE_FMT_U8P:
                break;
            case avutil.AV_SAMPLE_FMT_S16P://有符号short 16bit,平面型
                af = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,fg.getSampleRate(),16,fg.getAudioChannels(),fg.getAudioChannels()*2,fg.getSampleRate(),true);
                break;
            case avutil.AV_SAMPLE_FMT_S32P://有符号short 32bit，平面型，但是32bit的话可能电脑声卡不支持，这种音乐也少见
                af = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,fg.getSampleRate(),32,fg.getAudioChannels(),fg.getAudioChannels()*2,fg.getSampleRate(),true);
                break;
            case avutil.AV_SAMPLE_FMT_FLTP://float 平面型 需转为16bit short
                af = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,fg.getSampleRate(),16,fg.getAudioChannels(),fg.getAudioChannels()*2,fg.getSampleRate(),true);
                break;
            case avutil.AV_SAMPLE_FMT_DBLP:
                break;
            case avutil.AV_SAMPLE_FMT_S64://有符号short 64bit 非平面型
                break;
            case avutil.AV_SAMPLE_FMT_S64P://有符号short 64bit平面型
                break;
            default:
                System.out.println("不支持的音乐格式");
                System.exit(0);
        }
        dataLineInfo = new DataLine.Info(SourceDataLine.class,
                af, AudioSystem.NOT_SPECIFIED);
        try {
            sourceDataLine = (SourceDataLine)AudioSystem.getLine(dataLineInfo);
            sourceDataLine.open(af);
            sourceDataLine.start();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }
    public static ByteBuffer shortToByteValue(ShortBuffer arr,float vol) {
        int len  = arr.capacity();
        ByteBuffer bb = ByteBuffer.allocate(len * 2);
        for(int i = 0;i<len;i++){
            bb.putShort(i*2,(short)((float)arr.get(i)*vol));
        }
        return bb; // 默认转为大端序
    }
    public static ByteBuffer floatToByteValue(FloatBuffer arr,float vol){
        int len = arr.capacity();
        float f;
        float v;
        ByteBuffer res = ByteBuffer.allocate(len*2);
        v = 32768.0f*vol;
        for(int i=0;i<len;i++){
            f = arr.get(i)*v;//Ref：https://stackoverflow.com/questions/15087668/how-to-convert-pcm-samples-in-byte-array-as-floating-point-numbers-in-the-range
            if(f>v) f = v;
            if(f<-v) f = v;
            //默认转为大端序
            res.putShort(i*2,(short)f);//注意乘以2，因为一次写入两个字节。
        }
        return res;
    }
    private void printMusicInfo(FFmpegFrameGrabber fg) {
        //System.out.println("音频采样率"+fg.getSampleRate());
        //System.out.println("音频通道数"+fg.getAudioChannels());
    }

    public static void main(String[] args){
        /**
         * 测试成功。
         */
        //Stream #0:1: Audio: mp3 (U[0][0][0] / 0x0055), 44100 Hz, stereo, s16p, 96 kb/s
        //mp3，立体声，有short 16bit 平面型测试。
        String s1 = "C:\\Users\\Administrator\\Videos\\myVideo.avi";
        //Stream #0:1(und): Audio: aac (LC) (mp4a / 0x6134706D), 44100 Hz, stereo, fltp, 132 kb/s (default)
        //aac，立体声，float 平面型测试。
        String s2 = "C:\\Users\\Administrator\\Videos\\c.mp4";
        // Stream #0:0: Audio: pcm_s16le ([1][0][0][0] / 0x0001), 22050 Hz, 1 channels, s16, 352 kb/s
        //wav, 单通道，有符号short 16bot 非平面型测试
        String s3 = "C:\\Users\\Administrator\\Music\\滴答.wav";
        //Stream #0:0: Audio: flac, 44100 Hz, stereo, s16
        //flac, 双通道，有符号short 16bit 非平面型测试
        String s4 = "C:\\Users\\Administrator\\Music\\Sample Music\\陈小春 - 独家记忆.flac";
        // Stream #0:1: Audio: flac, 48000 Hz, stereo, s16 (default)
        //flac, 双通道，有符号short 16bit 非平面型测试。
        String s5 = "E:\\BaiduYunDownload\\t.mkv";
        //Stream #0:0: Audio: ac3, 44100 Hz, stereo, fltp, 320 kb/s
        //ac3 立体声，float 平面型测试
        String s7 = "D:\\FFOutput\\陈小春 - 独家记忆.ac3";
        //Stream #0:0: Audio: amr_nb (samr / 0x726D6173), 8000 Hz, mono, flt
        //amr,单声道，float 非平面型
        String s8 = "D:\\FFOutput\\陈小春 - 独家记忆.amr";
        //Stream #0:1(und): Audio: aac (LC) (mp4a / 0x6134706D), 44100 Hz, stereo, fltp, 311 kb/s (default)
        //m4a 立体声，float 平面型
        String s9 = "D:\\FFOutput\\陈小春 - 独家记忆.m4a";
        //Audio: aac (LC) (mp4a / 0x6134706D), 44100 Hz, stereo, fltp, 311 kb/s (default)
        //m4r 立体声，float 平面型
        String s10 = "D:\\FFOutput\\陈小春 - 独家记忆.m4r";
        //Stream #0:0: Audio: adpcm_yamaha, 8000 Hz, mono, s16, 32 kb/s
        //mmf 单声道，有符号short平面型
        String s11 = "D:\\FFOutput\\陈小春 - 独家记忆.mmf";
        //Stream #0:0: Audio: mp2, 44100 Hz, stereo, s16p, 320 kb/s
        //mp2 ，立体声，有符号short平面型
        String s12 = "D:\\FFOutput\\陈小春 - 独家记忆.mp2";
        //Stream #0:1: Audio: vorbis, 44100 Hz, stereo, fltp
        //ogg 立体声，float平面型
        String s13 = "D:\\FFOutput\\陈小春 - 独家记忆.ogg";
        //Stream #0:1: Audio: wmav2 (a[1][0][0] / 0x0161), 44100 Hz, 2 channels, fltp, 320 kb/s
        //wma，立体声，float平面型
        String s14 = "D:\\FFOutput\\陈小春 - 独家记忆.wma";
        //Stream #0:0: Audio: wavpack, 44100 Hz, stereo, s16p
        //wv，立体声，有符号short平面型
        String s15 = "D:\\FFOutput\\陈小春 - 独家记忆.wv";
        /**
         * 测试失败：ape。
         */
        //Stream #0:0: Audio: ape (APE  / 0x20455041), 44100 Hz, stereo, s16p
        //ape，双通道，有符号short 16bit 平面型。
        String s6 = "C:\\Users\\Administrator\\Music\\a.ape";
        try {
            new audioPlayback(s1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
