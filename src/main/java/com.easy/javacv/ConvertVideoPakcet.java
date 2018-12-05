package com.easy.javacv;

import static org.bytedeco.javacpp.avcodec.*;
import static org.bytedeco.javacpp.avformat.*;

import java.io.IOException;

import com.easy.javacv.recorder.FFmpegFrameRecorderPlus;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber.Exception;

/**
 * javaCV开发详解之8：转封装在rtsp转rtmp流中的应用（无须转码，更低的资源消耗）
 * rtsp转rtmp（转封装方式）
 *补充：解决javaCV的FFmpegFrameRecorder中dts为空导致播放器过快解码进而导致画面时快时慢等影响视频正常解码播放的问题，目前解决办法如下：
 *
 * 注意：本代码已提交给javacv，目前1.4.4-snapshot版本已修复该问题
 *
 * 修改 FFmpegFrameRecorder中的recordPacket(AVPacket pkt) 方法
 *
 * （1）注释掉pkt.dts(AV_NOPTS_VALUE);
 *
 * （2）在视频帧writePacket之前增加：
 *
 * pkt.dts(av_rescale_q_rnd(pkt.dts(), in_stream.time_base(), video_st.time_base(),(AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX)));
 *
 * （3）在音视帧writePacket之前增加： 
 *
 * pkt.dts(av_rescale_q_rnd(pkt.dts(), in_stream.time_base(), audio_st.time_base(),(AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX)));
 *
 * javaCV开发详解之4：转流器实现中我们使用了Grabber和Recorder的garbFrame和recordFrame实现转流，但是这种方式消耗很大，通过javacv源码发现garbFrame实际上进行decode操作（也就是把h264编码数据解码为yuv数据并保存到Frame对象中，然后在recordFrame中把Frame中的yuv图像像素数据又通过encode为h264编码数据，音频部分则是在garbFrame时先解码成pcm数据，然后在garbFrame中编码为aac），这两部分的编解码操作非常耗资源，显然会影响到转流的整体效率。
 *
 * 既然rtsp和rtmp本身都支持h264视频编码，那么视频编码这块完全可以跳过视频编解码的步骤，音频如果也都是aac编码那当然更好，这样我们可以避免很多不必要的编解码操作。
 *
 * 假设rtsp的视频编码和音频编码是h264和aac，那么我们只需要一步转封装即可完成转流，代码参考如下：
 * ---------------------
 * 作者： eguid
 * 来源：CSDN
 * 原文：https://blog.csdn.net/eguid_1/article/details/83025621
 * 版权声明：本文为博主原创文章，转载请附上博文链接！
 * @author eguid
 */
public class ConvertVideoPakcet {
    FFmpegFrameGrabber grabber = null;
    FFmpegFrameRecorderPlus record = null;
    int width = -1, height = -1;

    // 视频参数
    protected int audiocodecid;
    protected int codecid;
    protected double framerate;// 帧率
    protected int bitrate;// 比特率

    // 音频参数
    // 想要录制音频，这三个参数必须有：audioChannels > 0 && audioBitrate > 0 && sampleRate > 0
    private int audioChannels;
    private int audioBitrate;
    private int sampleRate;

    /**
     * 选择视频源
     *
     * @param src
     * @throws Exception
     * @author eguid
     */
    public ConvertVideoPakcet from(String src) throws Exception {
        // 采集/抓取器
        grabber = new FFmpegFrameGrabber(src);
        if (src.indexOf("rtsp") >= 0) {
            grabber.setOption("rtsp_transport", "tcp");
        }
        grabber.start();// 开始之后ffmpeg会采集视频信息，之后就可以获取音视频信息
        if (width < 0 || height < 0) {
            width = grabber.getImageWidth();
            height = grabber.getImageHeight();
        }
        // 视频参数
        audiocodecid = grabber.getAudioCodec();
        System.err.println("音频编码：" + audiocodecid);
        codecid = grabber.getVideoCodec();
        framerate = grabber.getVideoFrameRate();// 帧率
        bitrate = grabber.getVideoBitrate();// 比特率
        // 音频参数
        // 想要录制音频，这三个参数必须有：audioChannels > 0 && audioBitrate > 0 && sampleRate > 0
        audioChannels = grabber.getAudioChannels();
        audioBitrate = grabber.getAudioBitrate();
        if (audioBitrate < 1) {
            audioBitrate = 128 * 1000;// 默认音频比特率
        }
        return this;
    }

    /**
     * 选择输出
     *
     * @param out
     * @throws IOException
     * @author eguid
     */
    public ConvertVideoPakcet to(String out) throws IOException {
        // 录制/推流器
        record = new FFmpegFrameRecorderPlus(out, width, height);
        record.setVideoOption("crf", "18");
        record.setGopSize(2);
        record.setFrameRate(framerate);
        record.setVideoBitrate(bitrate);

        record.setAudioChannels(audioChannels);
        record.setAudioBitrate(audioBitrate);
        record.setSampleRate(sampleRate);
        AVFormatContext fc = null;
        if (out.indexOf("rtmp") >= 0 || out.indexOf("flv") > 0) {
            // 封装格式flv
            record.setFormat("flv");
            record.setAudioCodecName("aac");
            record.setVideoCodec(codecid);
            fc = grabber.getFormatContext();
        }
        record.start(fc);
        return this;
    }

    /**
     * 转封装
     *
     * @throws IOException
     * @author eguid
     */
    public ConvertVideoPakcet go() throws IOException {
        long err_index = 0;//采集或推流导致的错误次数
        //连续五次没有采集到帧则认为视频采集结束，程序错误次数超过1次即中断程序
        for (int no_frame_index = 0; no_frame_index < 5 || err_index > 1; ) {
            AVPacket pkt = null;
            try {
                //没有解码的音视频帧
                pkt = grabber.grabPacket();
                if (pkt == null || pkt.size() <= 0 || pkt.data() == null) {
                    //空包记录次数跳过
                    no_frame_index++;
                    continue;
                }
                //不需要编码直接把音视频帧推出去
                err_index += (record.recordPacket(pkt) ? 0 : 1);//如果失败err_index自增1
                av_free_packet(pkt);
            } catch (Exception e) {//推流失败
                err_index++;
            } catch (IOException e) {
                err_index++;
            }
        }
        return this;
    }

    public static void main(String[] args) throws Exception, IOException {

//运行，设置视频源和推流地址
        new ConvertVideoPakcet().from("rtsp://184.72.239.149/vod/mp4://BigBuckBunny_175k.mov")
                .to("rtmp://eguid.cc:1935/rtmp/eguid")
                .go();
    }
}