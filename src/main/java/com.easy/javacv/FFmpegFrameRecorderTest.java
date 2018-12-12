package com.easy.javacv;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;

import javax.imageio.ImageIO;
import java.io.IOException;

public class FFmpegFrameRecorderTest {

    private static FFmpegFrameRecorder setRecorder(String rtmpUrl, int imageWidth, int height) {
        // 流媒体输出地址，分辨率（长，高），是否录制音频（0:不录制/1:录制）
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(rtmpUrl, imageWidth, height, 0);
        recorder.setInterleaved(true);
        // 该参数用于降低延迟
        // recorder.setVideoOption("tune", "zerolatency");
        // ultrafast(终极快)提供最少的压缩（低编码器CPU）和最大的视频流大小；
        // 参考以下命令: ffmpeg -i '' -crf 30 -preset ultrafast
        recorder.setVideoOption("preset", "ultrafast");
        recorder.setVideoOption("crf", "30");
        // 视频编码器输出的比特率2000kbps/s
        recorder.setVideoBitrate(2000000);
        // H.264编码格式
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        // 提供输出流封装格式(rtmp协议只支持flv封装格式)
        recorder.setFormat("flv");
        // 视频帧率
        recorder.setFrameRate(30);
        // 关键帧间隔，一般与帧率相同或者是视频帧率的两倍
        recorder.setGopSize(60);
        // 不可变(固定)音频比特率
        recorder.setAudioOption("crf", "0");
        // Highest quality
        recorder.setAudioQuality(0);
        // 音频比特率 192 Kbps
        recorder.setAudioBitrate(192000);
        // 频采样率
        recorder.setSampleRate(44100);
        // 双通道(立体声)
        recorder.setAudioChannels(2);
        // 音频编/解码器
        recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
        return recorder;
    }

    public static void generatorOutputfile(String inputFile, String outputFile) {
        {
            System.out.println("视频解析开始");
            FFmpegFrameRecorder recorder = null;
            FFmpegFrameGrabber frameGrabber = null;
            try {

                // 获取视频并解析视频流
                frameGrabber = new FFmpegFrameGrabber(inputFile);
//                frameGrabber.setFormat("mp4");
                frameGrabber.start();
                // 流媒体输出地址，分辨率（长，高），是否录制音频（0:不录制/1:录制）
                recorder = setRecorder(outputFile, frameGrabber.getImageWidth(), frameGrabber.getImageHeight());
                System.out.println("流媒体输出地址:" + outputFile);
                recorder.start();
                long startTime = 0, videoTS = 0;
                Frame frame = null;
                while ((frame = frameGrabber.grabFrame()) != null) {
                    if (startTime == 0) {
                        startTime = System.currentTimeMillis();
                    }
                    videoTS = 1000 * (System.currentTimeMillis() - startTime);
                    recorder.setTimestamp(videoTS);
                    recorder.record(frame);
                }
                recorder.stop();
                frameGrabber.stop();
                frameGrabber.release();
                System.out.println("流媒体输出结束");
            } catch (Exception e) {
                System.out.println("parse 解析过程失败" + e);
                e.printStackTrace();
            } finally {
                // 播放结束或server端主动断开时，需要清空内存

                if (frameGrabber != null) {
                    try {
                        frameGrabber.stop();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws FrameGrabber.Exception {

        generatorOutputfile("E://tmp/1.flv", "E://tmp//123456.flv");
    }

}
