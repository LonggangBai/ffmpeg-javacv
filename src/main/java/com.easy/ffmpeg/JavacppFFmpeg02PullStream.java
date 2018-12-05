package com.easy.ffmpeg;

import org.apache.coyote.http2.StreamException;
import org.bytedeco.javacpp.*;

import java.io.FileNotFoundException;
import java.io.IOException;

import static org.bytedeco.javacpp.avcodec.*;
import static org.bytedeco.javacpp.avformat.*;
import static org.bytedeco.javacpp.avutil.*;
import static org.bytedeco.javacpp.swscale.*;
import static org.bytedeco.javacpp.avcodec.*;
import static org.bytedeco.javacpp.avformat.*;
import static org.bytedeco.javacpp.avutil.*;
import static org.bytedeco.javacpp.swscale.*;


import java.io.IOException;
import java.nio.ByteBuffer;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.PointerPointer;


/**
 * <pre>
 * javacpp-FFmpeg系列之2：通用拉流解码器，支持视频拉流解码并转换为YUV、BGR24或RGB24等图像像素数据
 *
 * 第一篇中视频解码成YUVJ420P图像像素数据（以下简称YUV或YUV数据），只是YUV在流媒体协议中用的较多（数据少，节省流量带宽），在图像处理应用较多的是BGR和RGB像素数据。我们已经获取到了YUV数据，那么把YUV转成BGR或者RGB需要再进行一次转换，显然性能上的表现并不是很好，所以本篇会通过编写一个通用转换器来介绍如何使用ffmpeg解码转出BGR、RGB、YUV等像素数据。
 *
 * 补充：
 *
 * （1）为什么暂时没有用python实现，主要是先熟悉ffmpeg的API，后面会出的
 *
 * （2）为什么要转成BGR、RGB像素数据，因为有了这俩其中一个就可以直接生成java的BufferImage了啊，最重要的是我们本意不是要转成BufferImage，而是直接编码成base64的图像数据啊
 * 本章主要详解ffmpeg拉流解码的各个流程，可以通过本章代码可以轻松实现不限于RGB/BGR/YUV的FFmpeg所支持的多种像素格式转换
 * ---------------------
 * 作者： eguid
 * 来源：CSDN
 * 原文：https://blog.csdn.net/eguid_1/article/details/82735524
 * 版权声明：本文为博主原创文章，转载请附上博文链接！
 * </pre>
 */
public abstract class JavacppFFmpeg02PullStream {


    static {
// Register all formats and codecs
        av_register_all();
        avformat_network_init();
    }
    //保留，暂不用

    protected int width;//宽度

    protected int height;//高度

    /**
     * 打开视频流
     *
     * @param url -url
     * @return
     * @throws FileNotOpenException
     */


    protected AVFormatContext openInput(String url) throws RuntimeException {
        AVFormatContext pFormatCtx = new AVFormatContext(null);
        if (avformat_open_input(pFormatCtx, url, null, null) == 0) {
            return pFormatCtx;
        }
        throw new RuntimeException("Didn't open video file");
    }

    /**
     * 检索流信息
     *
     * @param pFormatCtx
     * @return
     */


    protected AVFormatContext findStreamInfo(AVFormatContext pFormatCtx) throws RuntimeException {
        if (avformat_find_stream_info(pFormatCtx, (PointerPointer<?>) null) >= 0) {
            return pFormatCtx;
        }
        throw new RuntimeException("Didn't retrieve stream information");
    }

    /**
     * 获取第一帧视频位置
     *
     * @param pFormatCtx
     * @return
     */


    protected int findVideoStreamIndex(AVFormatContext pFormatCtx) {
        int i = 0, videoStream = -1;
        for (i = 0; i < pFormatCtx.nb_streams(); i++) {
            AVStream stream = pFormatCtx.streams(i);
            AVCodecContext codec = stream.codec();
            if (codec.codec_type() == AVMEDIA_TYPE_VIDEO) {
                videoStream = i;
                break;
            }
        }
        return videoStream;
    }

    /**
     * 指定视频帧位置获取对应视频帧
     *
     * @param pFormatCtx
     * @param videoStream
     * @return
     */


    protected AVCodecContext findVideoStream(AVFormatContext pFormatCtx, int videoStream) throws RuntimeException {
        if (videoStream >= 0) {
// Get a pointer to the codec context for the video stream
            AVStream stream = pFormatCtx.streams(videoStream);
            AVCodecContext pCodecCtx = stream.codec();
            return pCodecCtx;
        }
        throw new RuntimeException("Didn't open video file");
    }

    /**
     * 查找并尝试打开解码器
     *
     * @return
     */


    protected AVCodecContext findAndOpenCodec(AVCodecContext pCodecCtx) {
// Find the decoder for the video stream
        AVCodec pCodec = avcodec_find_decoder(pCodecCtx.codec_id());
        if (pCodec == null) {
            System.err.println("Codec not found");
            throw new RuntimeException("Codec not found");
        }
        AVDictionary optionsDict = null;
// Open codec
        if (avcodec_open2(pCodecCtx, pCodec, optionsDict) < 0) {
            System.err.println("Could not open codec");
            throw new RuntimeException("Could not open codec"); // Could not open codec
        }
        return pCodecCtx;
    }


    /**
     * 抓取视频帧（默认跳过音频帧和空帧）
     *
     * @param url -视频源（rtsp/rtmp/hls/文件等等）
     * @param fmt - 像素格式，比如AV_PIX_FMT_BGR24
     * @return
     * @throws IOException
     */


    public ByteBuffer grabVideoFrame(String url, int fmt) throws IOException {
// Open video file
        AVFormatContext pFormatCtx = openInput(url);

// Retrieve stream information
        pFormatCtx = findStreamInfo(pFormatCtx);

// Dump information about file onto standard error
//av_dump_format(pFormatCtx, 0, url, 0);

//Find a video stream
        int videoStream = findVideoStreamIndex(pFormatCtx);
        AVCodecContext pCodecCtx = findVideoStream(pFormatCtx, videoStream);

// Find the decoder for the video stream
        pCodecCtx = findAndOpenCodec(pCodecCtx);

// Allocate video frame
        AVFrame pFrame = av_frame_alloc();
//Allocate an AVFrame structure
        AVFrame pFrameRGB = av_frame_alloc();

        width = pCodecCtx.width();
        height = pCodecCtx.height();
        pFrameRGB.width(width);
        pFrameRGB.height(height);
        pFrameRGB.format(fmt);

// Determine required buffer size and allocate buffer
        int numBytes = avpicture_get_size(fmt, width, height);


        SwsContext sws_ctx = sws_getContext(width, height, pCodecCtx.pix_fmt(), width, height, fmt, SWS_BILINEAR, null, null, (DoublePointer) null);

        BytePointer buffer = new BytePointer(av_malloc(numBytes));
// Assign appropriate parts of buffer to image planes in pFrameRGB
// Note that pFrameRGB is an AVFrame, but AVFrame is a superset
// of AVPicture
        avpicture_fill(new AVPicture(pFrameRGB), buffer, fmt, width, height);
        AVPacket packet = new AVPacket();
        int[] frameFinished = new int[1];
        try {
// Read frames and save first five frames to disk
            while (av_read_frame(pFormatCtx, packet) >= 0) {
// Is this a packet from the video stream?
                if (packet.stream_index() == videoStream) {
// Decode video frame
                    avcodec_decode_video2(pCodecCtx, pFrame, frameFinished, packet);
// Did we get a video frame?
                    if (frameFinished != null && frameFinished[0] != 0) {
// Convert the image from its native format to BGR
                        sws_scale(sws_ctx, pFrame.data(), pFrame.linesize(), 0, height, pFrameRGB.data(), pFrameRGB.linesize());
//Convert BGR to ByteBuffer
                        return saveFrame(pFrameRGB, width, height);
                    }
                }
// Free the packet that was allocated by av_read_frame
                av_free_packet(packet);
            }
            return null;
        } finally {
//Don't free buffer
//av_free(buffer);
            av_free(pFrameRGB);// Free the RGB image
            av_free(pFrame);// Free the YUV frame
            sws_freeContext(sws_ctx);//Free SwsContext
            avcodec_close(pCodecCtx);// Close the codec
            avformat_close_input(pFormatCtx);// Close the video file
        }
    }

    /**
     * BGR图像帧转字节缓冲区（BGR结构）
     *
     * @param pFrame             -bgr图像帧
     * @param width              -宽度
     * @param height             -高度
     * @return
     * @throws IOException
     */


    abstract ByteBuffer saveFrame(AVFrame pFrameRGB, int width, int height);
}
