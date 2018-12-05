package com.easy.javacv;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.*;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameRecorder.Exception;

/**
 * <pre>
 *
 *     javaCV开发详解之7：让音频转换更加简单，实现通用音频编码格式转换、重采样等音频参数的转换功能（以pcm16le编码的wav转mp3为例）
 *     实现功能：
 * ①音频编码转换②音频格式转换③音频重采样④等等。。。跟多功能自行探索
 * 	</pre>
 * 音频参数转换（包含采样率、编码，位数，通道数）
 *javaCV开发详解之7：让音频转换更加简单，实现通用音频编码格式转换、重采样等音频参数的转换功能（以pcm16le编码的wav转mp3为例）
 *
 * 前言：本篇文章依赖四个jar包，其中javacv.jar，javacpp.jar和opencv.jar为固定jar包，opencv-系统环境.jar为选配（根据自己的系统平台，x64还是x86而定）
 *
 * 须知：
 *
 * OpenCVFrameConverter.ToIplImage可以用于将Frame转换为Mat和IplImage，Mat和IplImage转为Frame
 *
 * Mat和IplImage之间的转换可以使用opeoCV库中提供的功能
 *
 *
 * ---------------------
 * 作者： eguid
 * 来源：CSDN
 * 原文：https://blog.csdn.net/eguid_1/article/details/53218461
 * 版权声明：本文为博主原创文章，转载请附上博文链接！
 * @author eguid
 *
 */
public class JavaCV7RecordTransfer {

    static OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();

    public static void converter(Frame frame) {
        // 将Frame转为Mat
        opencv_core.Mat mat = converter.convertToMat(frame);

        // 将Mat转为Frame
        Frame convertFrame1 = converter.convert(mat);

        // 将Frame转为IplImage
        opencv_core.IplImage image1 = converter.convertToIplImage(frame);
        opencv_core.IplImage image2 = converter.convert(frame);

        // 将IplImage转为Frame
        Frame convertFrame2 = converter.convert(image1);

        //Mat转IplImage
        opencv_core.IplImage matImage = new opencv_core.IplImage(mat);

        //IplImage转Mat
        opencv_core.Mat mat2 = new opencv_core.Mat(matImage);

    }


    /**
         * 通用音频格式参数转换
         *
         * @param inputFile
         *            -导入音频文件
         * @param outputFile
         *            -导出音频文件
         * @param audioCodec
         *            -音频编码
         * @param sampleRate
         *            -音频采样率
         * @param audioBitrate
         *            -音频比特率
         */
        public static void convert(String inputFile, String outputFile, int audioCodec, int sampleRate, int audioBitrate,
                                   int audioChannels) {
            Frame audioSamples = null;
            // 音频录制（输出地址，音频通道）
            FFmpegFrameRecorder recorder = null;
            //抓取器
            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputFile);

            // 开启抓取器
            if (start(grabber)) {
                recorder = new FFmpegFrameRecorder(outputFile, audioChannels);
                recorder.setAudioOption("crf", "0");
                recorder.setAudioCodec(audioCodec);
                recorder.setAudioBitrate(audioBitrate);
                recorder.setAudioChannels(audioChannels);
                recorder.setSampleRate(sampleRate);
                recorder.setAudioQuality(0);
                recorder.setAudioOption("aq", "10");
                // 开启录制器
                if (start(recorder)) {
                    try {
                        // 抓取音频
                        while ((audioSamples = grabber.grab()) != null) {
                            recorder.setTimestamp(grabber.getTimestamp());
                            recorder.record(audioSamples);
                        }

                    } catch (org.bytedeco.javacv.FrameGrabber.Exception e1) {
                        System.err.println("抓取失败");
                    } catch (Exception e) {
                        System.err.println("录制失败");
                    }
                    stop(grabber);
                    stop(recorder);
                }
            }

        }

        public static boolean start(FrameGrabber grabber) {
            try {
                grabber.start();
                return true;
            } catch (org.bytedeco.javacv.FrameGrabber.Exception e2) {
                try {
                    System.err.println("首次打开抓取器失败，准备重启抓取器...");
                    grabber.restart();
                    return true;
                } catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
                    try {
                        System.err.println("重启抓取器失败，正在关闭抓取器...");
                        grabber.stop();
                    } catch (org.bytedeco.javacv.FrameGrabber.Exception e1) {
                        System.err.println("停止抓取器失败！");
                    }
                }

            }
            return false;
        }

        public static boolean start(FrameRecorder recorder) {
            try {
                recorder.start();
                return true;
            } catch (Exception e2) {
                try {
                    System.err.println("首次打开录制器失败！准备重启录制器...");
                    recorder.stop();
                    recorder.start();
                    return true;
                } catch (Exception e) {
                    try {
                        System.err.println("重启录制器失败！正在停止录制器...");
                        recorder.stop();
                    } catch (Exception e1) {
                        System.err.println("关闭录制器失败！");
                    }
                }
            }
            return false;
        }

        public static boolean stop(FrameGrabber grabber) {
            try {
                grabber.flush();
                grabber.stop();
                return true;
            } catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
                return false;
            } finally {
                try {
                    grabber.stop();
                } catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
                    System.err.println("关闭抓取器失败");
                }
            }
        }

        public static boolean stop(FrameRecorder recorder) {
            try {
                recorder.stop();
                recorder.release();
                return true;
            } catch (Exception e) {
                return false;
            } finally {
                try {
                    recorder.stop();
                } catch (Exception e) {

                }
            }
        }


    // 测试
//    public static void main2(String[] args) {
//        //pcm参数转换
////		convert("东部信息.wav", "eguid.wav", avcodec.AV_CODEC_ID_PCM_S16LE, 8000, 16000,1);
//        //pcm转mp3编码示例
//        convert("E:\\tmp\\1.flv", "eguid.mp3", avcodec.AV_CODEC_ID_MP3, 8000, 16,1);
//    }

    public static void main(String[] args) throws Exception {
            try{
                // 抓取取本机摄像头
                OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
                grabber.start();
                //取一帧视频（图像）
                converter(grabber.grab());
                grabber.stop();
            }catch (java.lang.Exception ex)
            {
                ex.printStackTrace();
            }

    }

}
