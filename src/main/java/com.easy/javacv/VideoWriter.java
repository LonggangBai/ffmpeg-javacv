package com.easy.javacv;


import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.*;

public class VideoWriter {
    public static final String FILENAME = "e:\\output.mp4";

    public static void main(String[] args) throws Exception {
        try {
            OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
            grabber.start();
            Frame grabbedImage= grabber.grab();
            CanvasFrame canvasFrame = new CanvasFrame("Video with JavaCV");
            canvasFrame.setCanvasSize(grabbedImage.imageWidth,
                    grabbedImage.imageHeight);
            grabber.setFrameRate(grabber.getFrameRate());

            FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(FILENAME,
                    grabber.getImageWidth(), grabber.getImageHeight()); // specify
            // your
            // path
            recorder.setFormat("mp4");
            //recorder.setPixelFormat(avutil.PIX_FMT_YUV420P10);
            recorder.setFrameRate(30);
            recorder.setVideoBitrate(10 * 1024 * 1024);

            recorder.start();
            while (canvasFrame.isVisible()
                    && (grabbedImage = grabber.grab()) != null) {
                canvasFrame.showImage(grabbedImage);
                recorder.record(grabbedImage);
            }
            recorder.stop();
            grabber.stop();
            canvasFrame.dispose();

        } catch (FrameGrabber.Exception ex) {
            ex.printStackTrace();
        } catch (FrameRecorder.Exception ex) {
            ex.printStackTrace();
        }
    }

}
