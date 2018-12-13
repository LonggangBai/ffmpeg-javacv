package com.easy.javacv;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;

import javax.imageio.ImageIO;
import java.io.IOException;

/**
 * <pre>
 *     实时监控、直播流、流媒体、视频网站开发方案设计简要
 * 2016年06月21日 11:22:35 eguid 阅读数：29237更多
 * 所属专栏： java流媒体技术
 * 版权声明：eguid温馨提示：本博客所有原创文章均采用知识共享署名-相同方式共享 3.0 中国大陆许可协议进行许可。如有转载请注明出处和eguid作者名，侵权必究！	https://blog.csdn.net/eguid_1/article/details/51725970
 * 欢迎大家积极开心的加入讨论群
 * 群号:371249677 （点击这里进群）
 * 一、本地推送端
 * 1、本地：采用javaCV（安卓和java平台推荐javaCV）、ffmpeg、openCV或者jmf可以很方便的获取到本地摄像头流媒体
 *
 * javaCV系列文章：
 *
 * javacv开发详解之1：调用本机摄像头视频
 *
 * javaCV开发详解之2：推流器实现，推本地摄像头视频到流媒体服务器以及摄像头录制视频功能实现(基于javaCV-FFMPEG、javaCV-openCV)
 *
 * javaCV开发详解之3：收流器实现，录制流媒体服务器的rtsp/rtmp视频文件(基于javaCV-FFMPEG)
 *
 * javaCV开发详解之4：转流器实现（也可作为本地收流器、推流器，新增添加图片及文字水印，视频图像帧保存），实现rtsp/rtmp/本地文件转发到rtmp流媒体服务器(基于javaCV-FFMPEG)
 *
 * javaCV开发详解之5：录制音频(录制麦克风)到本地文件/流媒体服务器(基于javax.sound、javaCV-FFMPEG)
 *
 * javaCV开发详解之6：本地音频(话筒设备)和视频(摄像头)抓取、混合并推送(录制)到服务器(本地)
 *
 * javaCV开发详解之7：让音频转换更加简单，实现通用音频编码格式转换、重采样等音频参数的转换功能（以pcm16le编码的wav转mp3为例）
 *
 * 补充篇：
 *
 * 音视频编解码问题：javaCV如何快速进行音频预处理和解复用编解码（基于javaCV-FFMPEG）
 *
 * 音视频编解码问题：16/24/32位位音频byte[]转换为小端序short[],int[]，以byte[]转short[]为例
 *
 * 实现给图片增加图片水印或者文字水印（也支持视频图像帧添加水印）
 *
 * javacpp-ffmpeg系列:
 *
 * javacpp-FFmpeg系列之1：视频拉流解码成YUVJ420P，并保存为jpg图片
 *
 * javacpp-FFmpeg系列之2：通用拉流解码器，支持视频拉流解码并转换为YUV、BGR24或RGB24等图像像素数据
 *
 * javacpp-FFmpeg系列之3： 图像数据转换（BGR与BufferdImage互转，RGB与BufferdImage互转）
 *
 * javaCV图像处理系列：
 *
 * 一、javaCV图像处理之1：实时视频添加文字水印并截取视频图像保存成图片，实现文字水印的字体、位置、大小、粗度、翻转、平滑等操作
 *
 * 二、javaCV图像处理之2：实时视频添加图片水印，实现不同大小图片叠加，图像透明度控制
 *
 * 三、opencv图像处理3：使用opencv原生方法遍历摄像头设备及调用（方便多摄像头遍历及调用，相比javacv更快的摄像头读取速度和效率，方便读取后的图像处理）
 *
 * 四、opencv图像处理系列：国内车辆牌照检测识别系统（万份测试准确率99.7%以上）
 *
 * 2、监控（第三方摄像头）：通过设备sdk或者rtsp直播流获取流媒体源
 *
 * 二、转流端
 * 直播：通过ffmpeg（推荐），live555将接收rtsp或者字节码流并转为flv格式发布到rtmp流媒体服务器（流媒体服务器必须先建好）
 *
 * hls原理同上
 *
 * 注意：rtmp只支持flv格式封装的视频流
 *
 * ffmpeg服务实现方式实例请参考：
 *
 * http://blog.csdn.net/eguid_1/article/details/51777716
 *
 * http://blog.csdn.net/eguid_1/article/details/51787646
 *
 * 也可以参考javaCV的转流器实现：javaCV开发详解之4：转流器实现，实现rtsp/rtmp/本地文件转发到rtmp服务器
 *
 * java封装FFmpeg命令，支持原生ffmpeg全部命令，实现FFmpeg多进程处理与多线程输出控制(开启、关闭、查询)，rtsp/rtmp推流、拉流
 *
 * 三、流媒体服务器
 * 目前主流的流媒体服务器有：fms,nginx-rtmp,red5(java),flazr
 *
 * 本地视频：直接通过流媒体服务器解码并推送视频流
 *
 * 直播流：通过开启udp/rtp/rtsp/rtmp/hls等等流媒体服务，从ffmpeg/live555获取推送过来的实时视频流并发布到rtmp/hls直播流并推送（可以边直播边保存）
 *
 * rtmp和hls这两种是web领域主流的流媒体协议。使用rtp或rtsp协议的一般都是监控。
 *
 * 流媒体协议选择：rtmp基于tcp协议，rtmp能够保持3秒左右延迟。hls是基于http协议，所以实时性特别差，想要用hls保持实时性的就别想了，hls延迟基本超过10秒。
 *
 * 实时性要求特高的，建议使用基于udp协议的一些流媒体协议。
 *
 * 基于tcp和udp两种流媒体协议区别就是tcp会强制同步，udp是数据发出去就不管了。
 *
 * 所以最终的方案就是：强同步但是实时性要求不高用基于tcp协议的，强实时性弱同步就udp。
 *
 * 补充：nginx-rtmp流媒体服务器搭建实例：http://blog.csdn.net/eguid_1/article/details/51749830
 *
 * nginx-rtmp配置指令详细含义和用法：http://blog.csdn.net/eguid_1/article/details/51821297
 * 四、播放端（收流端）
 * 直播：通过flex(flash)播放器或者第三方播放器（videoJS，ckplayer，VideoLAN 等...）调用流媒体服务器的流媒体源解码并播放，如果不需要兼容低版本IE，可以采用HTML5的webSocket播放器，videoJS是flash/html5双核播放器。
 *
 *
 *
 * 视频：通过html自带播放器、flex(flash)播放器或者第三方播放器（videoJS，ckplayer，VideoLAN 等...）进行播放
 *
 * videoJS/ckplayer播放器二次开发支持rtmp直播、hls直播及普通视频播放：http://blog.csdn.net/eguid_1/article/details/51898912
 *
 *
 *
 * 一般使用videoLAN播放器作为测试工具，用于测试音视频流发布状况
 *
 * 补充：
 *
 * 1、如果是采用nginx服务器，它提供的rtmp模块可以发布rtmp直播、录播及hls，nginx可以把ffmpeg整合进去方流媒体后期处理（加水印等）。
 *
 * 2、java是可以调用ffmpeg的，通过jni的方式有两种方法：
 *
 * 2.1、javaCV1.2支持通过javacpp调用ffmpeg，javaCV目前整合了8种流媒体处理框架，是安卓和javaEE平台不可或缺的强大流媒体处理利器
 *
 * 2.2、javaAV（目前最新0.7，release最新0.5）提供了对java调用ffmpeg的支持，当前已停止更新
 *
 *
 *
 * 补充：为什么没有基于原生java（或者说自带GC的语言）的流媒体框架，原因来自GC，也就是java引以为豪的自动垃圾回收机制（真的是成也萧何，败也萧何）
 *
 * 为什么呢？
 *
 * 大家知道，直播（顾名思义，实时视频转发），这种实时性项目会产生大量的对象，这样会导致两种情况：
 *
 * 1、产生大量对象后占据的内存资源得不到及时释放，于是虚拟机内存溢出。
 *
 * 2、产生大量对象导致GC满负荷运行进行资源回收，会严重占用系统资源，导致系统运行迟滞，影响系统运行性能和实时性等等。
 *
 * </pre>
 */
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
