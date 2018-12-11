package com.easy.javacv;


import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;

/**
 * javacv把海康摄像头rtsp流转推到rtmp流
 */
public class TestRTMPGrabberRecorder {
    static boolean exit  = false;
    public static void main(String[] args) throws Exception {
        System.out.println("start...");
        String rtmpPath = "rtmp://casic207-pc1/live360p/ss1";
        //	String rtspPath = "rtmp://live.hkstv.hk.lxdns.com/live/hks"; // 香港收视
        //String rtspPath = "rtsp://184.72.239.149/vod/mp4://BigBuckBunny_175k.mov";
        String rtspPath = "rtsp://admin:123123@192.168.1.64:554/h264/ch1/main/av_stream";
        //ffmpeg -f rtsp -rtsp_transport tcp -i rtsp://admin:leeking123@192.168.1.64:554/h264/ch1/main/av_stream rtmp://casic207-pc1/live360p/ss1
        // ffmpeg -i  rtsp://admin:123123@192.168.1.64:554/h264/ch1/main/av_stream -vcodec copy -acodec copy -f flv rtmp://casic207-pc1/live360p/ss1
        int audioRecord =0; // 0 = 不录制，1=录制
        boolean saveVideo = false;
        test(rtmpPath,rtspPath,audioRecord,saveVideo);
        System.out.println("end...");
    }

    public static void test(String rtmpPath,String rtspPath,int audioRecord,boolean saveVideo ) throws Exception  {
        //FrameGrabber grabber = FrameGrabber.createDefault(0); // 本机摄像头 默认
        // 使用rtsp的时候需要使用 FFmpegFrameGrabber，不能再用 FrameGrabber
        int width = 640,height = 480;
        FFmpegFrameGrabber grabber = FFmpegFrameGrabber.createDefault(rtspPath);
        grabber.setOption("rtsp_transport", "tcp"); // 使用tcp的方式，不然会丢包很严重
        // 一直报错的原因！！！就是因为是 2560 * 1440的太大了。。
        grabber.setImageWidth(width);
        grabber.setImageHeight(height);
        System.out.println("grabber start");
        grabber.start();
        //FrameRecorder recorder = FrameRecorder.createDefault(rtmpPath, 640,480,0);
        // 流媒体输出地址，分辨率（长，高），是否录制音频（0:不录制/1:录制）
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(rtmpPath, width, height, audioRecord);
        recorder.setInterleaved(true);
        recorder.setVideoOption("crf","28");
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264); // 28
        recorder.setFormat("flv"); // rtmp的类型
        recorder.setFrameRate(25);
        recorder.setPixelFormat(0); // yuv420p
        System.out.println("recorder start");
        recorder.start();
        //
        OpenCVFrameConverter.ToIplImage conveter = new OpenCVFrameConverter.ToIplImage();
        System.out.println("all start!!");
        int count = 0;
        while(!exit){
            count++;
            Frame frame = grabber.grabImage();
            if(frame == null){
                continue;
            }
            if(count % 100 == 0){
                System.out.println("count="+count);
            }
            recorder.record(frame);
        }
        grabber.stop();
        grabber.release();
        recorder.stop();
        recorder.release();
    }
}
