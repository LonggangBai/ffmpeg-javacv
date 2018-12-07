package com.easy.javacv.sample;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameRecorder.Exception;
import org.bytedeco.javacv.OpenCVFrameGrabber;

/**
 * http://192.168.31.47:8080/live/test345.m3u8
 * <p>
 * <p>
 * <p>
 * 基于流的
 * 实时直播并发布
 * <p>
 * OBS Configuration
 * Stream Type: Custom Streaming Server
 * URL: rtmp://localhost:1935/stream
 * Stream Key: hello
 * Watch Stream
 * In Safari, VLC or any HLS player, open:
 * http://<server ip>:8080/live/$STREAM_NAME.m3u8
 * Example Playlist: http://localhost:8080/live/hello.m3u8
 * VideoJS Player
 * FFplay: ffplay -fflags nobuffer rtmp://localhost:1935/stream/hello
 * <pre>
 * 我们需要下载nginx，pcre，zlib，openssl以及nginx-rtmp-module：
 *
 * nginx 官网下载最新，
 *
 * nginx-rtmp-module 可以在github上下载最新，
 *
 * 本人是在官网下载最新
 *
 * mkdir work
 *
 * cd work
 *
 * wget http://nginx.org/download/nginx-1.10.3.tar.gz
 *
 * wget http://zlib.net/zlib-1.2.11.tar.gz
 *
 * wget https://ftp.pcre.org/pub/pcre/pcre-8.40.tar.gz
 *
 * wget https://www.openssl.org/source/openssl-1.0.2k.tar.gz
 *
 * ./configure --prefix=/usr/local/nginx --with-debug --with-pcre=../pcre-8.40 --with-zlib=../zlib-1.2.11 --with-openssl=../openssl-1.0.2k --add-module=../nginx-rtmp-module-master
 *
 * make
 * ---------------------
 * 作者：Loongxu
 * 来源：CSDN
 * 原文：https://blog.csdn.net/heng615975867/article/details/80519274
 * 版权声明：本文为博主原创文章，转载请附上博文链接！
 *
 *
 *
 *
 *     /opt/nginx # cat nginx.conf
 * daemon off;
 *
 * error_log logs/error.log debug;
 *
 * events {
 *     worker_connections 1024;
 * }
 *
 * rtmp {
 *     server {
 *         listen 1935;
 *         chunk_size 4000;
 *
 *         application stream {
 *             live on;
 *
 *             exec ffmpeg -i rtmp://localhost:1935/stream/$name
 *               -c:a libfdk_aac -b:a 128k -c:v libx264 -b:v 2500k -f flv -g 30 -r 30 -s 1280x720 -preset superfast -profile:v baseline rtmp://localhost:1935/hls/$name_720p2628kbs
 *               -c:a libfdk_aac -b:a 128k -c:v libx264 -b:v 1000k -f flv -g 30 -r 30 -s 854x480 -preset superfast -profile:v baseline rtmp://localhost:1935/hls/$name_480p1128kbs
 *               -c:a libfdk_aac -b:a 128k -c:v libx264 -b:v 750k -f flv -g 30 -r 30 -s 640x360 -preset superfast -profile:v baseline rtmp://localhost:1935/hls/$name_360p878kbs
 *               -c:a libfdk_aac -b:a 128k -c:v libx264 -b:v 400k -f flv -g 30 -r 30 -s 426x240 -preset superfast -profile:v baseline rtmp://localhost:1935/hls/$name_240p528kbs
 *               -c:a libfdk_aac -b:a 64k -c:v libx264 -b:v 200k -f flv -g 15 -r 15 -s 426x240 -preset superfast -profile:v baseline rtmp://localhost:1935/hls/$name_240p264kbs;
 *         }
 *
 *         application hls {
 *             live on;
 *             hls on;
 *             hls_fragment_naming system;
 *             hls_fragment 5;
 *             hls_playlist_length 10;
 *             hls_path /opt/data/hls;
 *             hls_nested on;
 *
 *             hls_variant _720p2628kbs BANDWIDTH=2628000,RESOLUTION=1280x720;
 *             hls_variant _480p1128kbs BANDWIDTH=1128000,RESOLUTION=854x480;
 *             hls_variant _360p878kbs BANDWIDTH=878000,RESOLUTION=640x360;
 *             hls_variant _240p528kbs BANDWIDTH=528000,RESOLUTION=426x240;
 *             hls_variant _240p264kbs BANDWIDTH=264000,RESOLUTION=426x240;
 *         }
 *     }
 * }
 *
 * http {
 *     ssl_ciphers         HIGH:!aNULL:!MD5;
 *     ssl_protocols       TLSv1 TLSv1.1 TLSv1.2;
 *     ssl_session_cache   shared:SSL:10m;
 *     ssl_session_timeout 10m;
 *
 *     server {
 *         listen 80;
 *
 *         # Uncomment these lines to enable SSL.
 *         # Update the ssl paths with your own certificate and private key.
 *         # listen 443 ssl;
 *         # ssl_certificate     /opt/certs/example.com.crt;
 *         # ssl_certificate_key /opt/certs/example.com.key;
 *
 *         location /hls {
 *             types {
 *                 application/vnd.apple.mpegurl m3u8;
 *                 video/mp2t ts;
 *             }
 *             root /opt/data;
 *             add_header Cache-Control no-cache;
 *             add_header Access-Control-Allow-Origin *;
 *         }
 *
 *         location /live {
 *           alias /opt/data/hls;
 *           types {
 *               application/vnd.apple.mpegurl m3u8;
 *               video/mp2t ts;
 *           }
 *           add_header Cache-Control no-cache;
 *           add_header Access-Control-Allow-Origin *;
 *         }
 *
 *         location /stat {
 *             rtmp_stat all;
 *             rtmp_stat_stylesheet static/stat.xsl;
 *         }
 *
 *         location /static {
 *             alias /www/static;
 *         }
 *
 *         location = /crossdomain.xml {
 *             root /www/static;
 *             default_type text/xml;
 *             expires 24h;
 *         }
 *     }
 * }
 * </pre>
 */
public class WebcamAndMicrophoneCapture {
    final private static int WEBCAM_DEVICE_INDEX = 0;
    final private static int AUDIO_DEVICE_INDEX = 4;

    final private static int FRAME_RATE = 30;
    final private static int GOP_LENGTH_IN_FRAMES = 60;

    private static long startTime = 0;
    private static long videoTS = 0;

    public static void main(String[] args) throws Exception, org.bytedeco.javacv.FrameGrabber.Exception {
        int captureWidth = 1280;
        int captureHeight = 720;

        // The available FrameGrabber classes include OpenCVFrameGrabber (opencv_videoio),
        // DC1394FrameGrabber, FlyCaptureFrameGrabber, OpenKinectFrameGrabber,
        // PS3EyeFrameGrabber, VideoInputFrameGrabber, and FFmpegFrameGrabber.
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(WEBCAM_DEVICE_INDEX);
        grabber.setImageWidth(captureWidth);
        grabber.setImageHeight(captureHeight);
        grabber.start();

        // org.bytedeco.javacv.FFmpegFrameRecorder.FFmpegFrameRecorder(String
        // filename, int imageWidth, int imageHeight, int audioChannels)
        // For each param, we're passing in...
        // filename = either a path to a local file we wish to create, or an
        // RTMP url to an FMS / Wowza server
        // imageWidth = width we specified for the grabber
        // imageHeight = height we specified for the grabber
        // audioChannels = 2, because we like stereo
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(
                "rtmp://192.168.31.47:1935/stream/test345",
                captureWidth, captureHeight, 2);
        recorder.setInterleaved(true);

        // decrease "startup" latency in FFMPEG (see:
        // https://trac.ffmpeg.org/wiki/StreamingGuide)
        recorder.setVideoOption("tune", "zerolatency");
        // tradeoff between quality and encode speed
        // possible values are ultrafast,superfast, veryfast, faster, fast,
        // medium, slow, slower, veryslow
        // ultrafast offers us the least amount of compression (lower encoder
        // CPU) at the cost of a larger stream size
        // at the other end, veryslow provides the best compression (high
        // encoder CPU) while lowering the stream size
        // (see: https://trac.ffmpeg.org/wiki/Encode/H.264)
        recorder.setVideoOption("preset", "ultrafast");
        // Constant Rate Factor (see: https://trac.ffmpeg.org/wiki/Encode/H.264)
        recorder.setVideoOption("crf", "28");
        // 2000 kb/s, reasonable "sane" area for 720
        recorder.setVideoBitrate(2000000);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setFormat("flv");
        // FPS (frames per second)
        recorder.setFrameRate(FRAME_RATE);
        // Key frame interval, in our case every 2 seconds -> 30 (fps) * 2 = 60
        // (gop length)
        recorder.setGopSize(GOP_LENGTH_IN_FRAMES);

        // We don't want variable bitrate audio
        recorder.setAudioOption("crf", "0");
        // Highest quality
        recorder.setAudioQuality(0);
        // 192 Kbps
        recorder.setAudioBitrate(192000);
        recorder.setSampleRate(44100);
        recorder.setAudioChannels(2);
        recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);

        // Jack 'n coke... do it...
        recorder.start();

        // Thread for audio capture, this could be in a nested private class if you prefer...
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Pick a format...
                // NOTE: It is better to enumerate the formats that the system supports,
                // because getLine() can error out with any particular format...
                // For us: 44.1 sample rate, 16 bits, stereo, signed, little endian
                AudioFormat audioFormat = new AudioFormat(44100.0F, 16, 2, true, false);

                // Get TargetDataLine with that format
                Mixer.Info[] minfoSet = AudioSystem.getMixerInfo();
                Mixer mixer = AudioSystem.getMixer(minfoSet[AUDIO_DEVICE_INDEX]);
                DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

                try {
                    // Open and start capturing audio
                    // It's possible to have more control over the chosen audio device with this line:
                    // TargetDataLine line = (TargetDataLine)mixer.getLine(dataLineInfo);
                    TargetDataLine line = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
                    line.open(audioFormat);
                    line.start();

                    int sampleRate = (int) audioFormat.getSampleRate();
                    int numChannels = audioFormat.getChannels();

                    // Let's initialize our audio buffer...
                    int audioBufferSize = sampleRate * numChannels;
                    byte[] audioBytes = new byte[audioBufferSize];

                    // Using a ScheduledThreadPoolExecutor vs a while loop with
                    // a Thread.sleep will allow
                    // us to get around some OS specific timing issues, and keep
                    // to a more precise
                    // clock as the fixed rate accounts for garbage collection
                    // time, etc
                    // a similar approach could be used for the webcam capture
                    // as well, if you wish
                    ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
                    exec.scheduleAtFixedRate(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                // Read from the line... non-blocking
                                int nBytesRead = line.read(audioBytes, 0, line.available());

                                // Since we specified 16 bits in the AudioFormat,
                                // we need to convert our read byte[] to short[]
                                // (see source from FFmpegFrameRecorder.recordSamples for AV_SAMPLE_FMT_S16)
                                // Let's initialize our short[] array
                                int nSamplesRead = nBytesRead / 2;
                                short[] samples = new short[nSamplesRead];

                                // Let's wrap our short[] into a ShortBuffer and
                                // pass it to recordSamples
                                ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples);
                                ShortBuffer sBuff = ShortBuffer.wrap(samples, 0, nSamplesRead);

                                // recorder is instance of
                                // org.bytedeco.javacv.FFmpegFrameRecorder
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

        // A really nice hardware accelerated component for our preview...
        CanvasFrame cFrame = new CanvasFrame("Capture Preview", CanvasFrame.getDefaultGamma() / grabber.getGamma());

        Frame capturedFrame = null;

        // While we are capturing...
        while ((capturedFrame = grabber.grab()) != null) {
            if (cFrame.isVisible()) {
                // Show our frame in the preview
                cFrame.showImage(capturedFrame);
            }

            // Let's define our start time...
            // This needs to be initialized as close to when we'll use it as
            // possible,
            // as the delta from assignment to computed time could be too high
            if (startTime == 0)
                startTime = System.currentTimeMillis();

            // Create timestamp for this frame
            videoTS = 1000 * (System.currentTimeMillis() - startTime);

            // Check for AV drift
            if (videoTS > recorder.getTimestamp()) {
                System.out.println(
                        "Lip-flap correction:"
                                + videoTS + " :"
                                + recorder.getTimestamp() + " -> "
                                + (videoTS - recorder.getTimestamp()));

                // We tell the recorder to write this frame at this timestamp
                recorder.setTimestamp(videoTS);
            }

            // Send the frame to the org.bytedeco.javacv.FFmpegFrameRecorder
            recorder.record(capturedFrame);
        }

        cFrame.dispose();
        recorder.stop();
        grabber.stop();
    }
}