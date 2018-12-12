package com.easy.javacv;
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameGrabber;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 * <pre>
 实现录制本机麦克风音频到本地文件或者流媒体服务器，

 对于录制音视频混合的同学可以很方便的将本章代码移植到到录制视频的代码里

 注意：由于音频、视频时两个不同线程同时进行，所以在进行混合录制的时候需要注意统一帧率，以防止音画不同步现象


 本章对于ffmpeg的需要有一定了解以及对于音频处理有一定基础，可以先了解javaCV是如何进行音频的解复用和编码的：


 在开始之前可以了解一下javaCV-FFMPEG是如何帮我们做解复用和编码：http://blog.csdn.net/eguid_1/article/details/52875793
 1、实现功能
 (1)抓取本地录音设备（即,话筒）的实时音频

 (2)抓取本地摄像头实时视频

 (3)音频与视频时两个线程分别进行的，互不干扰

 (4)多8bit的音频转小字节序问题，请参考http://blog.csdn.net/eguid_1/article/details/52790848

 (5)本章代码包含大量注释，用来阐述每个API的用法和作用

 http://blog.csdn.net/eguid_1/article/details/52875793
 * 	</pre>
 */
public class JavaCV5RecordLocal {

    /**
     * 推送/录制本机的音/视频(Webcam/Microphone)到流媒体服务器(Stream media server)
     *
     * @param WEBCAM_DEVICE_INDEX
     *            - 视频设备，本机默认是0
     * @param AUDIO_DEVICE_INDEX
     *            - 音频设备，本机默认是4
     * @param outputFile
     *            - 输出文件/地址(可以是本地文件，也可以是流媒体服务器地址)
     * @param captureWidth
     *            - 摄像头宽
     * @param captureHeight
     *            - 摄像头高
     * @param FRAME_RATE
     *            - 视频帧率:最低 25(即每秒25张图片,低于25就会出现闪屏)
     * @throws org.bytedeco.javacv.FrameGrabber.Exception
     */
    public static void recordWebcamAndMicrophone(int WEBCAM_DEVICE_INDEX, int AUDIO_DEVICE_INDEX, String outputFile,
                                                 int captureWidth, int captureHeight, int FRAME_RATE) throws org.bytedeco.javacv.FrameGrabber.Exception {
        long startTime = 0;
        long videoTS = 0;
        /**
         * FrameGrabber 类包含：OpenCVFrameGrabber
         * (opencv_videoio),C1394FrameGrabber, FlyCaptureFrameGrabber,
         * OpenKinectFrameGrabber,PS3EyeFrameGrabber,VideoInputFrameGrabber, 和
         * FFmpegFrameGrabber.
         */
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(WEBCAM_DEVICE_INDEX);
        grabber.setImageWidth(captureWidth);
        grabber.setImageHeight(captureHeight);
        System.out.println("开始抓取摄像头...");
        int isTrue = 0;// 摄像头开启状态
        try {
            grabber.start();
            isTrue += 1;
        } catch (org.bytedeco.javacv.FrameGrabber.Exception e2) {
            if (grabber != null) {
                try {
                    grabber.restart();
                    isTrue += 1;
                } catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
                    isTrue -= 1;
                    try {
                        grabber.stop();
                    } catch (org.bytedeco.javacv.FrameGrabber.Exception e1) {
                        isTrue -= 1;
                    }
                }
            }
        }
        if (isTrue < 0) {
            System.err.println("摄像头首次开启失败，尝试重启也失败！");
            return;
        } else if (isTrue < 1) {
            System.err.println("摄像头开启失败！");
            return;
        } else if (isTrue == 1) {
            System.err.println("摄像头开启成功！");
        } else if (isTrue == 1) {
            System.err.println("摄像头首次开启失败，重新启动成功！");
        }

        /**
         * FFmpegFrameRecorder(String filename, int imageWidth, int imageHeight,
         * int audioChannels) fileName可以是本地文件（会自动创建），也可以是RTMP路径（发布到流媒体服务器）
         * imageWidth = width （为捕获器设置宽） imageHeight = height （为捕获器设置高）
         * audioChannels = 2（立体声）；1（单声道）；0（无音频）
         */
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, captureWidth, captureHeight, 2);
        recorder.setInterleaved(true);

        /**
         * 该参数用于降低延迟 参考FFMPEG官方文档：https://trac.ffmpeg.org/wiki/StreamingGuide
         * 官方原文参考：ffmpeg -f dshow -i video="Virtual-Camera" -vcodec libx264
         * -tune zerolatency -b 900k -f mpegts udp://10.1.0.102:1234
         */

        recorder.setVideoOption("tune", "zerolatency");
        /**
         * 权衡quality(视频质量)和encode speed(编码速度) values(值)：
         * ultrafast(终极快),superfast(超级快), veryfast(非常快), faster(很快), fast(快),
         * medium(中等), slow(慢), slower(很慢), veryslow(非常慢)
         * ultrafast(终极快)提供最少的压缩（低编码器CPU）和最大的视频流大小；而veryslow(非常慢)提供最佳的压缩（高编码器CPU）的同时降低视频流的大小
         * 参考：https://trac.ffmpeg.org/wiki/Encode/H.264 官方原文参考：-preset ultrafast
         * as the name implies provides for the fastest possible encoding. If
         * some tradeoff between quality and encode speed, go for the speed.
         * This might be needed if you are going to be transcoding multiple
         * streams on one machine.
         */
        recorder.setVideoOption("preset", "ultrafast");
        /**
         * 参考转流命令: ffmpeg
         * -i'udp://localhost:5000?fifo_size=1000000&overrun_nonfatal=1' -crf 30
         * -preset ultrafast -acodec aac -strict experimental -ar 44100 -ac
         * 2-b:a 96k -vcodec libx264 -r 25 -b:v 500k -f flv 'rtmp://<wowza
         * serverIP>/live/cam0' -crf 30
         * -设置内容速率因子,这是一个x264的动态比特率参数，它能够在复杂场景下(使用不同比特率，即可变比特率)保持视频质量；
         * 可以设置更低的质量(quality)和比特率(bit rate),参考Encode/H.264 -preset ultrafast
         * -参考上面preset参数，与视频压缩率(视频大小)和速度有关,需要根据情况平衡两大点：压缩率(视频大小)，编/解码速度 -acodec
         * aac -设置音频编/解码器 (内部AAC编码) -strict experimental
         * -允许使用一些实验的编解码器(比如上面的内部AAC属于实验编解码器) -ar 44100 设置音频采样率(audio sample
         * rate) -ac 2 指定双通道音频(即立体声) -b:a 96k 设置音频比特率(bit rate) -vcodec libx264
         * 设置视频编解码器(codec) -r 25 -设置帧率(frame rate) -b:v 500k -设置视频比特率(bit
         * rate),比特率越高视频越清晰,视频体积也会变大,需要根据实际选择合理范围 -f flv
         * -提供输出流封装格式(rtmp协议只支持flv封装格式) 'rtmp://<FMS server
         * IP>/live/cam0'-流媒体服务器地址
         */
        recorder.setVideoOption("crf", "25");
        // 2000 kb/s, 720P视频的合理比特率范围
        recorder.setVideoBitrate(2000000);
        // h264编/解码器
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        // 封装格式flv
        recorder.setFormat("flv");
        // 视频帧率(保证视频质量的情况下最低25，低于25会出现闪屏)
        recorder.setFrameRate(FRAME_RATE);
        // 关键帧间隔，一般与帧率相同或者是视频帧率的两倍
        recorder.setGopSize(FRAME_RATE * 2);
        // 不可变(固定)音频比特率
        recorder.setAudioOption("crf", "0");
        // 最高质量
        recorder.setAudioQuality(0);
        // 音频比特率
        recorder.setAudioBitrate(192000);
        // 音频采样率
        recorder.setSampleRate(44100);
        // 双通道(立体声)
        recorder.setAudioChannels(2);
        // 音频编/解码器
        recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
        System.out.println("开始录制...");

        try {
            recorder.start();
        } catch (org.bytedeco.javacv.FrameRecorder.Exception e2) {
            if (recorder != null) {
                System.out.println("关闭失败，尝试重启");
                try {
                    recorder.stop();
                    recorder.start();
                } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
                    try {
                        System.out.println("开启失败，关闭录制");
                        recorder.stop();
                        return;
                    } catch (org.bytedeco.javacv.FrameRecorder.Exception e1) {
                        return;
                    }
                }
            }

        }
        // 音频捕获
        new Thread(new Runnable() {
            @Override
            public void run() {
                /**
                 * 设置音频编码器 最好是系统支持的格式，否则getLine() 会发生错误
                 * 采样率:44.1k;采样率位数:16位;立体声(stereo);是否签名;true:
                 * big-endian字节顺序,false:little-endian字节顺序(详见:ByteOrder类)
                 */
                AudioFormat audioFormat = new AudioFormat(44100.0F, 16, 2, true, false);

                // 通过AudioSystem获取本地音频混合器信息
                Mixer.Info[] minfoSet = AudioSystem.getMixerInfo();
                // 通过AudioSystem获取本地音频混合器
                Mixer mixer = AudioSystem.getMixer(minfoSet[AUDIO_DEVICE_INDEX]);
                // 通过设置好的音频编解码器获取数据线信息
                DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
                try {
                    // 打开并开始捕获音频
                    // 通过line可以获得更多控制权
                    // 获取设备：TargetDataLine line
                    // =(TargetDataLine)mixer.getLine(dataLineInfo);
                    TargetDataLine line = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
                    line.open(audioFormat);
                    line.start();
                    // 获得当前音频采样率
                    int sampleRate = (int) audioFormat.getSampleRate();
                    // 获取当前音频通道数量
                    int numChannels = audioFormat.getChannels();
                    // 初始化音频缓冲区(size是音频采样率*通道数)
                    int audioBufferSize = sampleRate * numChannels;
                    byte[] audioBytes = new byte[audioBufferSize];

                    ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
                    exec.scheduleAtFixedRate(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                // 非阻塞方式读取
                                int nBytesRead = line.read(audioBytes, 0, line.available());
                                // 因为我们设置的是16位音频格式,所以需要将byte[]转成short[]
                                int nSamplesRead = nBytesRead / 2;
                                short[] samples = new short[nSamplesRead];
                                /**
                                 * ByteBuffer.wrap(audioBytes)-将byte[]数组包装到缓冲区
                                 * ByteBuffer.order(ByteOrder)-按little-endian修改字节顺序，解码器定义的
                                 * ByteBuffer.asShortBuffer()-创建一个新的short[]缓冲区
                                 * ShortBuffer.get(samples)-将缓冲区里short数据传输到short[]
                                 */
                                ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples);
                                // 将short[]包装到ShortBuffer
                                ShortBuffer sBuff = ShortBuffer.wrap(samples, 0, nSamplesRead);
                                // 按通道录制shortBuffer
                                recorder.recordSamples(sampleRate, numChannels, sBuff);
                            } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }, 0, (long) 1000 / FRAME_RATE, TimeUnit.MILLISECONDS);
                } catch (LineUnavailableException e1) {
                    e1.printStackTrace();
                }
            }
        }).start();

        // javaCV提供了优化非常好的硬件加速组件来帮助显示我们抓取的摄像头视频
        CanvasFrame cFrame = new CanvasFrame("Capture Preview", CanvasFrame.getDefaultGamma() / grabber.getGamma());
        Frame capturedFrame = null;
        // 执行抓取（capture）过程
        while ((capturedFrame = grabber.grab()) != null) {
            if (cFrame.isVisible()) {
                //本机预览要发送的帧
                cFrame.showImage(capturedFrame);
            }
            //定义我们的开始时间，当开始时需要先初始化时间戳
            if (startTime == 0)
                startTime = System.currentTimeMillis();

            // 创建一个 timestamp用来写入帧中
            videoTS = 1000 * (System.currentTimeMillis() - startTime);
            //检查偏移量
            if (videoTS > recorder.getTimestamp()) {
                System.out.println("Lip-flap correction: " + videoTS + " : " + recorder.getTimestamp() + " -> "
                        + (videoTS - recorder.getTimestamp()));
                //告诉录制器写入这个timestamp
                recorder.setTimestamp(videoTS);
            }
            // 发送帧
            try {
                recorder.record(capturedFrame);
            } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
                System.out.println("录制帧发生异常，什么都不做");
            }
        }

        cFrame.dispose();
        try {
            if (recorder != null) {
                recorder.stop();
            }
        } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
            System.out.println("关闭录制器失败");
            try {
                if (recorder != null) {
                    grabber.stop();
                }
            } catch (org.bytedeco.javacv.FrameGrabber.Exception e1) {
                System.out.println("关闭摄像头失败");
                return;
            }
        }
        try {
            if (recorder != null) {
                grabber.stop();
            }
        } catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
            System.out.println("关闭摄像头失败");
        }
    }


    public  static Runnable recordMicroPhone(String outputFile)throws Exception {


        /**
         * 设置音频编码器 最好是系统支持的格式，否则getLine() 会发生错误
         * 采样率:44.1k;采样率位数:16位;立体声(stereo);是否签名;true:
         * big-endian字节顺序,false:little-endian字节顺序(详见:ByteOrder类)
         */
        AudioFormat audioFormat = new AudioFormat(44100.0F, 16, 2, true, false);
        System.out.println("准备开启音频！");
        // 通过AudioSystem获取本地音频混合器信息
        Mixer.Info[] minfoSet = AudioSystem.getMixerInfo();
        // 通过AudioSystem获取本地音频混合器
        Mixer mixer = AudioSystem.getMixer(minfoSet[1]);
        // 通过设置好的音频编解码器获取数据线信息
        DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

        // 打开并开始捕获音频
        // 通过line可以获得更多控制权
        // 获取设备：TargetDataLine line
        // =(TargetDataLine)mixer.getLine(dataLineInfo);
        Line dataline = null;
        try {
            dataline = AudioSystem.getLine(dataLineInfo);
        } catch (LineUnavailableException e2) {
            System.err.println("开启失败...");
            return null;
        }
        TargetDataLine line = (TargetDataLine) dataline;
        try {
            line.open(audioFormat);
        } catch (LineUnavailableException e1) {
            line.stop();
            try {
                line.open(audioFormat);
            } catch (LineUnavailableException e) {
                System.err.println("按照指定音频编码器打开失败...");
                return null;
            }
        }
        line.start();
        System.out.println("已经开启音频！");
        // 获得当前音频采样率
        int sampleRate = (int) audioFormat.getSampleRate();
        // 获取当前音频通道数量
        int numChannels =0; audioFormat.getChannels();
        // 初始化音频缓冲区(size是音频采样率*通道数)
        int audioBufferSize = sampleRate * numChannels;
        byte[] audioBytes = new byte[audioBufferSize];
        // 流媒体输出地址，分辨率（长，高），是否录制音频（0:不录制/1:录制）
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, 1280, 720, numChannels);



//        recorder.start();
        Runnable crabAudio = new Runnable() {
            ShortBuffer sBuff = null;
            int nBytesRead;
            int nSamplesRead;

            @Override
            public void run() {
                System.out.println("读取音频数据...");
                // 非阻塞方式读取
                nBytesRead = line.read(audioBytes, 0, line.available());
                // 因为我们设置的是16位音频格式,所以需要将byte[]转成short[]
                nSamplesRead = nBytesRead / 2;
                short[] samples = new short[nSamplesRead];
                /**
                 * ByteBuffer.wrap(audioBytes)-将byte[]数组包装到缓冲区
                 * ByteBuffer.order(ByteOrder)-按little-endian修改字节顺序，解码器定义的
                 * ByteBuffer.asShortBuffer()-创建一个新的short[]缓冲区
                 * ShortBuffer.get(samples)-将缓冲区里short数据传输到short[]
                 */
                ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples);
                // 将short[]包装到ShortBuffer
                sBuff = ShortBuffer.wrap(samples, 0, nSamplesRead);
                // 按通道录制shortBuffer
                try {
                    System.out.println("录制音频数据...");
                    recorder.recordSamples(sampleRate, numChannels, sBuff);
                } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
                    // do nothing
                    e.printStackTrace();
                }
            }

            @Override
            protected void finalize() throws Throwable {
                sBuff.clear();
                sBuff = null;
                super.finalize();
            }
        };
        return crabAudio;
    }

    public static void test2() throws Exception {
        int FRAME_RATE = 25;
        ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
        Runnable crabAudio = recordMicroPhone("E:\\tmp\11.flv");//对应上面的方法体
        ScheduledFuture tasker = exec.scheduleAtFixedRate(crabAudio, 0, (long) 1000 / FRAME_RATE,
                TimeUnit.MILLISECONDS);
        Thread.sleep(20 * 1000);
        tasker.cancel(true);
        if (!exec.isShutdown()) {
            exec.shutdownNow();
        }
    }

    public static void main(String[] args)
            throws Exception {
        test2();



    }

}
