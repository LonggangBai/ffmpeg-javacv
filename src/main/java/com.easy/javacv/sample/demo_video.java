package com.easy.javacv.sample;


import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.*;

import java.nio.Buffer;
import java.nio.IntBuffer;

import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2RGBA;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;

public class demo_video {
    public static void main(String[] args) throws  Exception {

    }

    private static void testCamera() {
        //Create canvas frame for displaying video.
        CanvasFrame canvas = new CanvasFrame("VideoCanvas");

        //Set Canvas frame to close on exit
        canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);

        //Declare FrameGrabber to import video from "video.mp4"
        FrameGrabber grabber = new OpenCVFrameGrabber("video.mp4");


        try {

            //Start grabber to capture video
            grabber.start();

            //Declare img as IplImage

            while (true) {

                //inser grabed video fram to IplImage img
                Frame img = grabber.grab();

                //Set canvas size as per dimentions of video frame.
                canvas.setCanvasSize(grabber.getImageWidth(), grabber.getImageHeight());

                if (img != null) {
                    //Show video frame in canvas
                    canvas.showImage(img);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}