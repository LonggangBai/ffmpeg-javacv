package com.easy.javacv.grabber;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.bytedeco.javacpp.opencv_core.IplImage;
import static org.bytedeco.javacpp.opencv_core.cvReleaseImage;
import static org.bytedeco.javacpp.opencv_imgcodecs.cvLoadImage;
import static org.bytedeco.javacpp.opencv_imgcodecs.cvSaveImage;
import static org.bytedeco.javacpp.opencv_imgproc.cvSmooth;

/**
 * 利用JavaCV实现将视频以帧方式抽取
 */
public class JavaCVSplitFrame {

    // the image's path;
    final static String imagePath = "/home/lance/abc.jpg/";
    // the vedio's path and filename;
    final static String vedioPath = "/home/lance/target-a/";
    final static String vedioName = "origin-a.mp4";


    public static void main(String[] args) throws Exception {
        smooth(imagePath);
        grabberFFmpegImage(vedioPath + vedioName, vedioPath
                , vedioName, 30);
    }

    // the method of compress image;
    public static void smooth(String fileName) {
        IplImage iplImage = cvLoadImage(fileName);
        if (iplImage != null) {
            cvSmooth(iplImage, iplImage);
            cvSaveImage(fileName, iplImage);
            cvReleaseImage(iplImage);
        }
    }

    // grab ffmpegImage from vedio;
    public static void grabberFFmpegImage(String filePath, String fileTargetPath
            , String fileTargetName, int grabSize) throws Exception{
        FFmpegFrameGrabber ff = FFmpegFrameGrabber.createDefault(filePath);
        ff.start();
        for (int i = 0; i < grabSize; i++){
            Frame frame = ff.grabImage();
            doExecuteFrame(frame, filePath, fileTargetName, i);
        }
        ff.stop();
    }

    // grab frame from vedio;
    public static void doExecuteFrame(Frame frame, String targetFilePath, String targetFileName, int index) {
        if ( frame == null || frame.image == null) {
            return;
        }
        Java2DFrameConverter converter = new Java2DFrameConverter();
        String imageMat = "jpg";
        String fileName = targetFilePath + File.pathSeparator + targetFileName + "_" + index + "." + imageMat;
        BufferedImage bi = converter.getBufferedImage(frame);
        File output = new File(fileName);
        try{
            ImageIO.write(bi, imageMat, output);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}