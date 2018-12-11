package com.easy.javacv;

import javax.swing.JFrame;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_objdetect;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.FrameGrabber.Exception;

/**
 * <pre>
 *     利用docker搭建RTMP直播流服务器实现直播
 * 2018年05月08日 09:17:38 dns007 阅读数：1365 标签： 直播 rtmp javacv  更多
 * 个人分类： 直播
 * 版权声明：本文为博主原创文章，未经博主允许不得转载。	https://blog.csdn.net/lipei1220/article/details/80234281
 * 一、rtmp服务器搭建
 *
 * 环境：
 *
 * centos 7.*
 *
 * 1.先安装docker（省略）
 *
 * 2.下载docker容器
 *
 * docker pull alfg/nginx-rtmp
 *
 * 3.运行容器（记得打开防火墙端口 1935和8080）
 *
 * docker run -it -p 1935:1935 -p 8080:80 --rm nginx-rtmp
 *
 * 二、推流方式
 *
 * ok rtmp服务器 搭建成功，接下来就是推流了（这里列举三种方式）
 *
 * 第一种：利用OBS Studio推送直播到这个地址
 *
 * rtmp://服务器ip:1935/stream/自定义名称
 *
 * 第二种：ffmpeg推送本地视频为直播流
 *
 * ffmpeg -re -i /home/holle.flv -vcodec copy -acodec aac -ar 44100 -f flv rtmp://192.168.1.201:1935/stream/example
 *
 * 第三种：利用javacv推本地摄像头视频到流媒体服务器（代码实现如下）
 * javacv依赖
 *
 * 		<dependency>
 * 			<groupId>org.bytedeco</groupId>
 * 			<artifactId>javacv-platform</artifactId>
 * 			<version>1.3.2</version>
 * 		</dependency>
 * 三、观看直播（rtmp流）
 *
 * 观看 rtmp流 可以用PotPlayer 
 *
 * 菜单——打开——打开链接  贴入rtmp流地址  即可播放了
 *
 * rtmp://192.168.1.201:1935/stream/example
 *
 *
 *FFmpeg学习笔记-YUV以H264或H265编码最后打包TS流过程（进阶版）
 * 2018年07月19日 11:25:09 凤凰院凶真丶 阅读数：295更多
 * 个人分类： FFmpeg学习笔记
 * 在学习了FFmpeg后，才发现其中美妙，之前发一个yuv转ts的初学版，现在看看感觉不堪回首。所以一直在思考能不能直接用命令就完成整个过程。在请教大神后得出了命令。
 *
 * YUV420_8bit->H264
 *
 * 还可以在参数中设置编码码率。真是太舒服了。
 *
 * E:\ffmpeg.exe   -s  1920x1080  -pix_fmt yuv420p      -i    E:\Demo_1920_1080_HD.yuv  -vcodec  libx264  -x264-params  fps=25:bframes=7:keyint=24:bitrate=2000:preset=fast    Demo_1920_1080_2M.h264
 * YUV420_8bit->H265
 *
 * H265和H264的用法差不多。
 *
 * E:\ffmpeg1.exe    -s  720x576  -pix_fmt yuv420p      -i    E:\demo_720_576.yuv  -vcodec  libx265  -x265-params  fps=25:bframes=7:keyint=24:bitrate=2000:preset=fast    demo_720_576_2M.h265
 * H264->TS
 *
 * E:\ffmpeg1.exe    -use_wallclock_as_timestamps  true   -i  E:\demo_720_576_2M.h264      -vcodec     copy  demo_2M_H264.ts
 * H265->TS
 *
 * E:\\ffmpeg1.exe   -i  E:\demo_720_576_2M.h265    -vcodec hevc -y demo_720_576_2M_H265.ts
 * 到此为止大功告成。
 *
 * 参考：
 *
 * https://hub.docker.com/r/alfg/nginx-rtmp/
 *
 * https://blog.csdn.net/eguid_1/article/details/52678775
 * ---------------------
 * 作者：dns007
 * 来源：CSDN
 * 原文：https://blog.csdn.net/lipei1220/article/details/80234281
 * 版权声明：本文为博主原创文章，转载请附上博文链接！
 * </pre>
 */
public class JavavcCameraTest
{
    public static void recordCamera(String outputFile, double frameRate)
            throws Exception, InterruptedException, org.bytedeco.javacv.FrameRecorder.Exception {
        Loader.load(opencv_objdetect.class);
        FrameGrabber grabber = FrameGrabber.createDefault(0);//本机摄像头默认0，这里使用javacv的抓取器，至于使用的是ffmpeg还是opencv，请自行查看源码
        grabber.start();//开启抓取器

        OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();//转换器
        opencv_core.IplImage grabbedImage = converter.convert(grabber.grab());//抓取一帧视频并将其转换为图像，至于用这个图像用来做什么？加水印，人脸识别等等自行添加
        int width = grabbedImage.width();
        int height = grabbedImage.height();

        FrameRecorder recorder = FrameRecorder.createDefault(outputFile, width, height);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264); // avcodec.AV_CODEC_ID_H264，编码
        recorder.setFormat("flv");//封装格式，如果是推送到rtmp就必须是flv封装格式
        recorder.setFrameRate(frameRate);

        recorder.start();//开启录制器
        long startTime=0;
        long videoTS=0;
        CanvasFrame frame = new CanvasFrame("camera", CanvasFrame.getDefaultGamma() / grabber.getGamma());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setAlwaysOnTop(true);
        Frame rotatedFrame=converter.convert(grabbedImage);//不知道为什么这里不做转换就不能推到rtmp
        while (frame.isVisible() && (grabbedImage = converter.convert(grabber.grab())) != null) {
            rotatedFrame = converter.convert(grabbedImage);
            frame.showImage(rotatedFrame);
            if (startTime == 0) {
                startTime = System.currentTimeMillis();
            }
            videoTS = 1000 * (System.currentTimeMillis() - startTime);
            recorder.setTimestamp(videoTS);
            recorder.record(rotatedFrame);
            Thread.sleep(40);
        }
        frame.dispose();
        recorder.stop();
        recorder.release();
        grabber.stop();

    }

    public static void main(String[] args) throws Exception, InterruptedException, org.bytedeco.javacv.FrameRecorder.Exception {
//        recordCamera("D:\\example.mp4",25); //保持到本地
        recordCamera("rtmp://192.168.1.201:1935/stream/example",25); //推流到rtmp服务器
    }

}
