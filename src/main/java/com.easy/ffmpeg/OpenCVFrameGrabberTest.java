package com.ewivt.bjss.ccs;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;

public class OpenCVFrameGrabberTest {
    static OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();

    public static void main(String[] args) throws Exception {
        // 抓取取本机摄像头
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
        grabber.start();
        //取一帧视频（图像）
        converter(grabber.grab());
        grabber.stop();
    }

    public static void converter(Frame frame) {

        // 将Frame转为Mat
        opencv_core.Mat mat = converter.convertToMat(frame);

        // 将Mat转为Frame
        Frame convertFrame1 = converter.convert(mat);

        // 将Frame转为IplImage
        opencv_core.IplImage image1 = converter.convertToIplImage(frame);
        opencv_core.IplImage image2 = converter.convert(frame);

        // 将IplImage转为Frame
        Frame convertFrame2 = converter.convert(image1);

        //Mat转IplImage
        opencv_core.IplImage matImage = new opencv_core.IplImage(mat);

        //IplImage转Mat
        opencv_core.Mat mat2 = new opencv_core.Mat(matImage);

    }


}
