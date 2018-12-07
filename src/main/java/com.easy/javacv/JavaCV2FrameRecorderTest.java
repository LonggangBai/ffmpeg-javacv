package com.easy.javacv;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_objdetect;
import org.bytedeco.javacv.*;

import javax.swing.*;

/**
 * <pre>
 *     一、rtmp服务器搭建
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
 * ffmpeg -re -i /home/holle.flv -vcodec copy -acodec aac -ar 44100 -f flv
 * rtmp://192.168.1.201:1935/stream/example
 *
 * 第三种：利用javacv推本地摄像头视频到流媒体服务器（代码实现如下）
 * ---------------------
 * 作者：dns007
 * 来源：CSDN
 * 原文：https://blog.csdn.net/lipei1220/article/details/80234281
 * 版权声明：本文为博主原创文章，转载请附上博文链接！
 * 参考：
 *
 * https://hub.docker.com/r/alfg/nginx-rtmp/
 *
 * https://blog.csdn.net/eguid_1/article/details/52678775
 *
 *
 * 利用javacv推本地摄像头视频到流媒体服务器
 *
 *
 *
 * 推流器实现，推本地摄像头视频到流媒体服务器以及摄像头录制视频功能实现(基于javaCV-FFMPEG、javaCV-openCV)
 * 增加视频推流到流媒体服务器和视频录制的功能；
 *
 * 功能：实现边播放边录制/推流，停止预览即停止录制/推流
 *
 * 本功能采用按帧录制/推流，通过关闭播放窗口停止视频录制/推流
 *
 * 注：长时间运行该代码会导致内存溢出的原因是没有及时释放IplImage资源（由于javacv是jni方式调用C，部分对象需要手动释放资源，以防止内存溢出错误）
 *
 *FFmpeg编译安装教程：https://blog.csdn.net/heng615975867/article/details/79388439
 *
 * </pre>
 */
public class JavaCV2FrameRecorderTest {

    /**
     * 按帧录制本机摄像头视频（边预览边录制，停止预览即停止录制）
     *
     * @param outputFile -录制的文件路径，也可以是rtsp或者rtmp等流媒体服务器发布地址
     * @param frameRate  - 视频帧率
     * @throws Exception
     * @throws InterruptedException
     * @throws org.bytedeco.javacv.FrameRecorder.Exception
     * @author eguid
     */
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
        long startTime = 0;
        long videoTS = 0;
        CanvasFrame frame = new CanvasFrame("camera", CanvasFrame.getDefaultGamma() / grabber.getGamma());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setAlwaysOnTop(true);
        Frame rotatedFrame = converter.convert(grabbedImage);//不知道为什么这里不做转换就不能推到rtmp
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

    /**
     * inputFile设置为服务器播放地址，outputFile设置为本地地址，这里演示.mp4，也可以是flv等其他后缀名
     *
     * @param args
     * @throws Exception
     * @throws InterruptedException
     * @throws org.bytedeco.javacv.FrameRecorder.Exception
     */
    public static void main(String[] args) throws Exception, InterruptedException, org.bytedeco.javacv.FrameRecorder.Exception {
        //远程播放
        //recordCamera("rtmp://192.168.31.47:1935/stream/test345",25);
        //本地播放
        recordCamera("E://tmp//1.flv", 25);
    }

}
