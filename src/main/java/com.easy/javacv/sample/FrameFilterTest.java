package com.easy.javacv.sample;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.*;
import org.junit.Test;

import static org.bytedeco.javacpp.avcodec.*;
import static org.bytedeco.javacpp.avutil.*;
import static org.junit.Assert.*;


/**
 * Test cases for FrameFilter classes. Also uses other classes from JavaCV.
 *
 * @author Samuel Audet
 */
public class FrameFilterTest {

    @Test
    public void testFFmpegFrameFilter() {
        System.out.println("FFmpegFrameFilter");

        File tempFile = new File(Loader.getTempDir(), "test.mov");
        try {
            FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(tempFile, 800, 600, 2);
            recorder.setFormat("mov");
            recorder.setPixelFormat(AV_PIX_FMT_YUV420P);
            recorder.setFrameRate(30);
            recorder.setVideoCodec(AV_CODEC_ID_H265);
            recorder.setVideoQuality(10);
            recorder.setSampleFormat(AV_SAMPLE_FMT_FLTP);
            recorder.setSampleRate(48000);
            recorder.setAudioCodec(AV_CODEC_ID_AAC);
            recorder.setAudioQuality(10);
            recorder.start();

            int n = 1000;
            Frame frame = new Frame(800, 600, Frame.DEPTH_UBYTE, 3);
            for (int i = 0; i < n; i++) {
                recorder.record(frame);
            }
            Frame audioFrame = new Frame();
            ShortBuffer audioBuffer = ShortBuffer.allocate(48000 * 2 * n / 30);
            audioFrame.sampleRate = 48000;
            audioFrame.audioChannels = 2;
            audioFrame.samples = new ShortBuffer[] {audioBuffer};
            recorder.record(audioFrame);
            recorder.stop();
            recorder.release();

            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(tempFile);
            grabber.setSampleMode(FrameGrabber.SampleMode.FLOAT);
            grabber.start();

            FFmpegFrameFilter filter = new FFmpegFrameFilter(
                    "scale=400x300,transpose=cclock_flip,format=gray",
                    "volume=0.5,aformat=sample_fmts=u8:channel_layouts=mono",
                    grabber.getImageWidth(), grabber.getImageHeight(), grabber.getAudioChannels());
            filter.setPixelFormat(grabber.getPixelFormat());
            filter.setSampleFormat(grabber.getSampleFormat());
            filter.start();

            FFmpegFrameFilter nullFilter = new FFmpegFrameFilter(null, null, 0, 0, 0);
            nullFilter.start();

            int a = 0, b = 0, c = 0, d = 0;
            Frame frame2;
            while ((frame2 = grabber.grab()) != null) {
                if (frame2.image != null) {
                    a++;
                }
                if (frame2.samples != null) {
                    b++;
                }
                filter.push(frame2);
                Frame frame3;
                while ((frame3 = filter.pull()) != null) {
                    if (frame3.image != null) {
                        c++;
                        assertEquals(300, frame3.imageWidth);
                        assertEquals(400, frame3.imageHeight);
                        assertEquals(1, frame3.imageChannels);
                    }
                    if (frame3.samples != null) {
                        d++;
                        assertEquals(1, frame3.audioChannels);
                        assertEquals(1, frame3.samples.length);
                        assertTrue(frame3.samples[0] instanceof ByteBuffer);
                        assertEquals(frame2.samples.length, frame3.samples.length);
                        assertEquals(frame2.samples[0].limit() / 2, frame3.samples[0].limit());
                    }
                }
                nullFilter.push(frame2);
                assertEquals(frame2, nullFilter.pull());
            }
            assertEquals(a, c);
            assertEquals(b, d);
            assertEquals(null, grabber.grab());
            filter.stop();
            filter.release();
            grabber.restart();
            grabber.stop();
            grabber.release();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception should not have been thrown: " + e);
        } finally {
            tempFile.delete();
        }
    }

    @Test
    public void testFFmpegFrameFilterMultipleInputs() {
        System.out.println("FFmpegFrameFilterMultipleInputs");

        File tempFile = new File(Loader.getTempDir(), "test.avi");
        try {
            FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(tempFile, 320, 200, 2);
            recorder.setVideoCodec(AV_CODEC_ID_VP8);
            recorder.setAudioCodec(AV_CODEC_ID_VORBIS);
            recorder.start();

            int n = 1000;
            Frame frame = new Frame(320, 200, Frame.DEPTH_UBYTE, 3);
            for (int i = 0; i < n; i++) {
                recorder.record(frame);
            }
            Frame audioFrame = new Frame();
            ShortBuffer audioBuffer = ShortBuffer.allocate(8000 * 2 * n / 30);
            audioFrame.sampleRate = 8000;
            audioFrame.audioChannels = 2;
            audioFrame.samples = new ShortBuffer[] {audioBuffer};
            recorder.record(audioFrame);
            recorder.stop();
            recorder.release();

            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(tempFile);
            grabber.start();

            FFmpegFrameFilter filter = new FFmpegFrameFilter(
                    "[0:v][1:v]hstack=inputs=2[v]",
                    "[0:a][1:a]amerge[a]",
                    grabber.getImageWidth(), grabber.getImageHeight(), grabber.getAudioChannels());
            filter.setPixelFormat(grabber.getPixelFormat());
            filter.setSampleFormat(grabber.getSampleFormat());
//            filter.setVideoInputs(2);
//            filter.setAudioInputs(2);
            filter.start();

            int a = 0, b = 0, c = 0, d = 0;
            Frame frame2;
            while ((frame2 = grabber.grab()) != null) {
                if (frame2.image != null) {
                    a++;
                }
                if (frame2.samples != null) {
                    b++;
                }
                filter.push( frame2,0);
                filter.push( frame2,1);
                Frame frame3;
                while ((frame3 = filter.pull()) != null) {
                    if (frame3.image != null) {
                        c++;
                        assertEquals(640, frame3.imageWidth);
                        assertEquals(200, frame3.imageHeight);
                        assertEquals(3, frame3.imageChannels);
                    }
                    if (frame3.samples != null) {
                        d++;
                        assertEquals(2, frame3.audioChannels);
                        assertEquals(1, frame3.samples.length);
                        assertTrue(frame3.samples[0] instanceof ByteBuffer);
                        assertEquals(frame2.samples.length, frame3.samples.length);
                        assertEquals(frame2.samples[0].limit(), frame3.samples[0].limit());
                    }
                }
            }
            assertEquals(a, c);
            assertEquals(b, d);
            assertEquals(null, grabber.grab());
            filter.stop();
            filter.release();
            grabber.restart();
            grabber.stop();
            grabber.release();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception should not have been thrown: " + e);
        } finally {
            tempFile.delete();
        }
    }

}