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
 * <pre>
 *前面我用了很多章实现了javaCV的基本操作，包括：音视频捕捉（摄像头视频捕捉和话筒音频捕捉），推流(本地音视频或者摄像头话筒混合推流到服务器)，转流（rtsp->rtmp），收流（录制）。
 *
 * 序：
 * 我们知道javaCV中编码需要先取到一帧采样的音频（即采样率x通道数,我们姑且把这个称为一帧采样数据）
 *
 * 其实我们在该篇文章http://blog.csdn.net/eguid_1/article/details/52804246中已经对音频进行转码了。
 *
 * 额。。这个真没看出来（PS:博主也没看出来 0_0 !）。。。。。。。。。
 *
 * 我们获取了本地的音频音频数据（具体啥编码博主也不晓得，只知道是16位的， - -！ ，不过这不要紧，FFMPEG能我们实现，下面将会讲到 ）；
 *
 * 其中我们做了大小端序的转换和byte[]转short[]（双8位转单16位），音频编解码中这个操作我们会经常用；
 *
 * 然后我们使用了recoder.reacordSimples(采样率，通道数，一份采样);
 *
 * 对比一下音频捕获的文章：http://blog.csdn.net/eguid_1/article/details/52702385
 *
 * 发现了吗？没错，我们给recorder设置了一些属性：
 *
 *  
 *
 * // 不可变(固定)音频比特率
 *         recorder.setAudioOption("crf", "0");
 *         // 最高质量
 *         recorder.setAudioQuality(0);
 *         // 音频比特率
 *         recorder.setAudioBitrate(192000);
 *         // 音频采样率
 *         recorder.setSampleRate(44100);
 *         // 双通道(立体声)
 *         recorder.setAudioChannels(2);
 *         // 音频编/解码器
 *         recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
 * 看到了吗？我们其实已经设置了编/解码格式aac，为什么呢？因为javaCV已经封装了解复用和编码这两个操作。
 *
 *  
 *
 * 补充：
 * 补充一下javaCV底层的ffmpeg解复用/编码流程：
 *
 * 我们在进行recoder.reacordSimples的时候javaCV底层调用ffmpeg的swr_convert()方法(详见javaCV的FFmpegFrameRecoder类974行)进行了解码操作，完成了对pcm16le编码的解复用；
 *
 * 解码完成之后又调用了recorder.record(Frame frame)（详见javaCV的FFmpegFrameRecoder类994行），在这个环节完成了调用了FFMPEG的avcodec_encode_audio2()方法（详见javaCV的FFmpegFrameRecoder类1006行）按照我们已经设定好的的aac格式完成了编
 *
 * 码操作，所以我们本身是不需要进行解复用/编码的操作的（视频也是一样，以后会讲到），因为javaCV已经帮我门做了！
 *
 * 到这里肯定有些小伙伴已经5脸懵bi的状态了。。。 - -！，最不幸的是，上面一堆的前言和补充知识，我们的主题还没开始。 0_0 !
 *
 * eguid唯一技术博客是csdn，博主唯一交流群是群号:371249677 （点击这里进群），欢迎大家来埋汰群主
 *
 * 1、java音频预处理
 * 既然javaCV已经帮我门做了解复用和编码，那么我们只需要将获得到的音频数据进行简单的预处理即可。
 *
 * 注：如果是文件或者服务器直播流，那么连预处理都省了，直接设置编码格式即可，不需要我们手动做任何处理。
 *
 *  
 *
 * 这里讲一下特殊的byte[]流，也就是基于socket的IO流形式的音频数据处理，一般我们使用这种的情况是移动端通过socket推流到中转服务器，让中转服务器进行转码推送到流媒体服务器。
 *
 * 1.1、如何从byte[]流中获取一份完整的音频帧（即一帧采样数据）
 * 就拿 8000采样率，16bit，双通道（立体声）的pcm16le编码来说吧举例说明吧
 *
 * 我们知道这个音频采样率是8000，位数是16bit，2个通道，那么我们就知道这个编码的一帧就是（8000x2 ）个byte
 *
 * 1.2、音频原始数据转换
 * 一个byte只能表示8bit数据，我们要表示16位的音频数据就需要装换为short，一个short等于2个byte，在转换的同时进行大小端序转换（大小端序问题详见http://blog.csdn.net/eguid_1/article/details/52790848），那么我们最后得到的数据应该是一个长度是8000的short数组（即short[8000]）来表示一帧音频采样数据。
 *
 * 音频的预处理到此完毕，接下来该javaCV出场了
 *
 * 2、javaCV音频解复用及编码
 * 通过上面一大堆的前言，已经知道：音频数据直接通过recorder设置音频编码参数即可自动进行解复用和编码
 *
 * 只需要调用recorder.recordSamples(采样率，通道数量，一份采样数据)即可。
 *
 * 我的天呐，这真真是用一行代码解决了C/C++好几百行的事情！
 *
 * 支持eguid原创
 * ---------------------
 * 作者： eguid
 * 来源：CSDN
 * 原文：https://blog.csdn.net/eguid_1/article/details/52875793
 * 版权声明：本文为博主原创文章，转载请附上博文链接！
 *     javaCV开发详解之6：本地音频(话筒设备)和视频(摄像头)抓取、混合并推送(录制)到服务器(本地)
 *
 *
 * javaCV开发详解之6：本地音频(话筒设备)和视频(摄像头)抓取、混合并推送(录制)到服务器(本地)
 * 1、实现功能
 * (1)抓取本地录音设备（即,话筒）的实时音频
 *
 * (2)抓取本地摄像头实时视频
 *
 * (3)音频与视频时两个线程分别进行的，互不干扰
 *
 * (4)多8bit的音频转小字节序问题，请参考http://blog.csdn.net/eguid_1/article/details/52790848
 *
 * (5)本章代码包含大量注释，用来阐述每个API的用法和作用
 * ---------------------
 * 作者： eguid
 * 来源：CSDN
 * 原文：https://blog.csdn.net/eguid_1/article/details/52804246
 * 版权声明：本文为博主原创文章，转载请附上博文链接！
 * 	</pre>
 */
public class JavaCV6RecordLocal {
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

    public static void main(String[] args)
            throws Exception {
        recordWebcamAndMicrophone(0,4,"E://tmp/aa.flv",700,800,25);


    }

}
