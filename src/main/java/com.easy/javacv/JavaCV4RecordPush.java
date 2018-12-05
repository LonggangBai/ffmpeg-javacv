package com.easy.javacv;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_objdetect;
import org.bytedeco.javacv.*;

import javax.swing.*;

/**
 *
 * <pre>
 * 那个方法内部做了实时编码，编码后才推出去，如果是推rtmp，那就是用tcp不间断的推流过去，如果是rtsp,那么默认是使用udp一帧一帧推
 *
 * 我使用ffmpeg的ffserver搭建了一个流媒体服务器。但是如何能够与你这里的代码相结合，实现实时视频同步播放的功能呢？ FrameRecorder recorder; FrameRecorder.createDefault(&amp;quot;rtmp://192.168.30.21/live/pushFlow&amp;quot; , 1280, 720);
 *
 *
 *
 * String rtspPath = &quot;rtsp://admin:leeking123@192.168.1.64:554/Streaming/Channels/1&quot;;
 * FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(rtspPath );
 * grabber.setOption(&quot;rtsp_transport&quot;, &quot;tcp&quot;); // 设置为tcp的方式，避免udp丢包
 * grabber.start();
 * OpenCVFrameConverter.ToIplImage conveter = new OpenCVFrameConverter.ToIplImage();
 * 		for(int i=0 ; true; i++){
 * 			Frame frame = grabber.grabImage();
 * 			if(frame == null){
 * 				System.out.println(&quot;frame is null &quot;+i);
 * 				continue ;
 *                        }
 * 			System.out.println(i +&quot;:&quot;+grabber.getFormat()+ &quot;:&quot;+grabber.getFrameNumber());
 * 			cvSaveImage(imagesPath+i+&quot;.jpg&quot;, conveter.convertToIplImage(frame));* 		}
 *
 * 		但是我现在的实时视频需求是这样的： 1.从网络摄像头获取视频流，然后解码成一帧帧的jpg图片； 2.把jpg图片进行加工； 3.把加工后的图片实时地组成一个视频; 3.通过rtsp在web前端实时播放。 项目是bs架构，我现在第1、2步实现了。第3、4步就在纠结，因为图片一张一张地过来。我要不断地把它合成视频，
 * 		但同时合成好的又要在web前端实时播放。所以想请教一下。如何实现第3和第4步呢？
 *
 *
 *
 * 如果想要给视频添加水印，需要从视频中取出图像帧，给图像帧添加文字、图片水印即可
 * //获取BufferedImage可以给图像帧添加水印
 * 		Java2DFrameConverter javaconverter=new Java2DFrameConverter();
 * 		BufferedImage buffImg=javaconverter.convert(grabber.grab());
 * 		获取到了BufferedImage我们就可以给视频帧添加文字或者图片水印了
 *
 * 	补充：
 *
 * 作为转流器可以轻松实现rtsp/rtmp/本地文件/本地摄像头推送到rtmp流媒体服务器；
 *
 * 作为收流器可以用来把流媒体服务器视频流录制到本地文件。
 *
 * 关于默认接收/推送rtsp流丢帧问题，由于ffmpeg默认采用udp方式，所以可以通过更改为tcp的方式来实现丢帧补偿，解决方式如下：
 *
 * 1、FFmpeg命令方式：增加一个配置命令 -rtsp_transport tcp
 *
 * 2、javacv方式：FFmpegFrameGrabber.java中533行 AVDictionary options= new AVDictionary(null);后面增加一个配置av_dict_set(options, "rtsp_transport", "tcp", 0); 即可
 *
 * 3、ffmpeg原生方式：同上
 * ---------------------
 * 作者： eguid
 * 来源：CSDN
 * 原文：https://blog.csdn.net/eguid_1/article/details/52691707
 * 版权声明：本文为博主原创文章，转载请附上博文链接！
 * 	</pre>
 */
public class JavaCV4RecordPush {

    /**
     * 转流器
     * @param inputFile
     * @param outputFile
     * @throws Exception
     * @throws org.bytedeco.javacv.FrameRecorder.Exception
     * @throws InterruptedException
     */
    public static void recordPush(String inputFile,String outputFile,int v_rs) throws Exception, org.bytedeco.javacv.FrameRecorder.Exception, InterruptedException{
        Loader.load(opencv_objdetect.class);
        long startTime=0;
        FrameGrabber grabber = FFmpegFrameGrabber.createDefault(inputFile);
        try {
            grabber.start();
        } catch (Exception e) {
            try {
                grabber.restart();
            } catch (Exception e1) {
                throw e;
            }
        }

        OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
        Frame grabframe =grabber.grab();
        opencv_core.IplImage grabbedImage =null;
        if(grabframe!=null){
            System.out.println("取到第一帧");
            grabbedImage = converter.convert(grabframe);
        }else{
            System.out.println("没有取到第一帧");
        }
        //如果想要保存图片,可以使用 opencv_imgcodecs.cvSaveImage("hello.jpg", grabbedImage);来保存图片
        FrameRecorder recorder;
        try {
            recorder = FrameRecorder.createDefault(outputFile, 1280, 720);
        } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
            throw e;
        }
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264); // avcodec.AV_CODEC_ID_H264
        recorder.setFormat("flv");
        recorder.setFrameRate(v_rs);
        recorder.setGopSize(v_rs);
        System.out.println("准备开始推流...");
        try {
            recorder.start();
        } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
            try {
                System.out.println("录制器启动失败，正在重新启动...");
                if(recorder!=null)
                {
                    System.out.println("尝试关闭录制器");
                    recorder.stop();
                    System.out.println("尝试重新开启录制器");
                    recorder.start();
                }

            } catch (org.bytedeco.javacv.FrameRecorder.Exception e1) {
                throw e;
            }
        }
        System.out.println("开始推流");
        CanvasFrame frame = new CanvasFrame("camera", CanvasFrame.getDefaultGamma() / grabber.getGamma());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setAlwaysOnTop(true);
        while (frame.isVisible() && (grabframe=grabber.grab()) != null) {
            System.out.println("推流...");
            frame.showImage(grabframe);
            grabbedImage = converter.convert(grabframe);
            Frame rotatedFrame = converter.convert(grabbedImage);

            if (startTime == 0) {
                startTime = System.currentTimeMillis();
            }
            recorder.setTimestamp(1000 * (System.currentTimeMillis() - startTime));//时间戳
            if(rotatedFrame!=null){
                recorder.record(rotatedFrame);
            }

            Thread.sleep(40);
        }
        frame.dispose();
        recorder.stop();
        recorder.release();
        grabber.stop();
        System.exit(2);
    }
    public static void main(String[] args)
            throws Exception {

        String inputFile = "rtsp://admin:admin@192.168.2.236:37779/cam/realmonitor?channel=1&subtype=0";

        String outputFile = "rtmp://192.168.30.21/live/pushFlow";

        recordPush(inputFile, outputFile, 25);
    }

}
