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
 *     java音视频编解码问题：16/24/32位位音频byte[]转换为小端序short[],int[]，以byte[]转short[]为例
 * 2016年10月11日 19:42:29 eguid 阅读数：3741更多
 * 个人分类： java
 * 所属专栏： java流媒体技术 javacv开发指南（音视频/图像处理从入门到精通）
 * 版权声明：eguid温馨提示：本博客所有原创文章均采用知识共享署名-相同方式共享 3.0 中国大陆许可协议进行许可。如有转载请注明出处和eguid作者名，侵权必究！	https://blog.csdn.net/eguid_1/article/details/52790848
 * 前言：Java默认采用大端序存储方式，实际编码的音频数据是小端序，如果处理单8bit的音频当然不需要做转换，但是如果是16bit或者以上的就需要处理成小端序字节顺序。
 *
 * 注：大、小端序指的是字节的存储顺序是按从高到低还是从低到高的顺序存储，与处理器架构有关，Intel的x86平台是典型的小端序存储方式
 *
 * 1、Java中使用ByteOrder.LITTLE_ENDIAN表示小端序，ByteOrder.BIG_ENDIAN表示大端序
 * 小端序：数据的高位字节存放在地址的低端 低位字节存放在地址高端
 *
 * 大端序：数据的高位字节存放在地址的高端 低位字节存放在地址低端
 *
 * 大端序是按照数字的书写顺序进行存储的，而小端序则是反顺序进行存储的。
 *
 * Big-endian 存放顺序(顺序存储)
 * 0x00000001           -- 12
 * 0x00000002           -- 34
 * 0x00000003           -- 56
 * 0x00000004           -- 78
 *
 * Little-endian 存放顺序(反序储存)
 * 0x00000001           -- 78
 * 0x00000002           -- 56
 * 0x00000003           -- 34
 * 0x00000004           -- 12
 *
 * 2、java中的大小端序转换
 * 例如byte[]转成short[]：
 *
 * 假设data作为源音频数据，是一个byte[]
 *
 * int dataLength=data.length;
 *
 * //byte[]转成short[]，数组长度缩减一半
 *
 * int shortLength=dataLength/2;
 *
 * //把byte[]数组装进byteBuffer缓冲
 *
 * ByteBuffer byteBuffer=ByteBuffer.wrap(data, 0,dataLength);
 *
 * //将byteBuffer转成小端序并获取shortBuffer
 *
 * ShortBuffer shortBuffer=byteBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
 *
 * short[] shortSamples=new short[shortLength];
 *
 * //取出shortBuffer中的short数组
 *
 * shortBuffer.get(shortSamples,0,shortLength);
 *
 *
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
