package com.easy.javacv;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class RTMPPushlocal2Server {

    /**
     * Socket客户端
     */
    public static void main(String[] args) throws Exception {
        try {
            int count = 0;
            while (true) {
// "GET /ipcam/jpeg.cgi HTTP/1.1\r\n\r\nAuthorization: Basic YWRtaW46OTk5OQ====\r\n\r\n"
// 创建Socket对象
                Socket socket = new Socket("192.168.31.47", 8080);
// 根据输入输出流和服务端连接
                OutputStream outputStream = socket.getOutputStream();// 获取一个输出流，向服务端发送信息
// "GET /ipcam/jpeg.cgi HTTP/1.1\r\nAuthorization: Basic YWRtaW46OTk5OQ====\r\n\r\n"
                outputStream
                        .write("GET /ipcam/avc.cgi HTTP/1.1\r\nAuthorization: Basic YWRtaW46OTk5OQ====\r\n\r\n"
                                .getBytes());
                outputStream.flush();
                socket.shutdownOutput();// 关闭输出流
                InputStream inputStream = socket.getInputStream();// 获取一个输入流，接收服务端的信息
                String outputFile = "rtmp://192.168.31.47:1935/stream/test6";
                frameRecord(inputStream, outputFile, 1);
                count++;
                System.out.println("================================" + count + "次数");
                socket.close();
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    /**
     *  
     *  * 按帧录制视频 
     *  *
     *  * @param inputFile-该地址可以是网络直播/录播地址，也可以是远程/本地文件路径 
     *  * @param outputFile 
     *  *         -该地址只能是文件地址，如果使用该方法推送流媒体服务器会报错，原因是没有设置编码格式 
     *  * @throws FrameGrabber.Exception 
     *  * @throws FrameRecorder.Exception 
     *  * @throws org.bytedeco.javacv.FrameRecorder.Exception 
     *  
     */
    public static void frameRecord(InputStream inputFile, String outputFile, int audioChannel)
            throws Exception, org.bytedeco.javacv.FrameRecorder.Exception {
        boolean isStart = true;//该变量建议设置为全局控制变量，用于控制录制结束
        // 获取视频源
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputFile);
        grabber.setOption("rtsp_transport", "tcp");
        // 流媒体输出地址，分辨率（长，高），是否录制音频（0:不录制/1:录制）
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, 1280, 720, audioChannel);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
//        recorder.setFormat("flv");
        // 开始取视频源
        recordByFrame(grabber, recorder, isStart);
    }


    private static void recordByFrame(FFmpegFrameGrabber grabber, FFmpegFrameRecorder recorder, Boolean status)
            throws Exception, org.bytedeco.javacv.FrameRecorder.Exception {
        try {//建议在线程中使用该方法
            grabber.start();
            recorder.start();
            Frame frame = null;
            while (status && (frame = grabber.grabFrame()) != null) {
                recorder.record(frame);
            }
            recorder.stop();
            grabber.stop();
        } finally {
            if (grabber != null) {
                grabber.stop();
            }
        }
    }
}
