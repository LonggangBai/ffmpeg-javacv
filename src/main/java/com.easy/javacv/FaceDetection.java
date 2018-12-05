package com.easy.javacv;


import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacpp.opencv_objdetect;
import org.bytedeco.javacpp.opencv_objdetect.CvHaarClassifierCascade;

import static org.bytedeco.javacpp.helper.opencv_objdetect.cvHaarDetectObjects;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.cvLoadImage;
import static org.bytedeco.javacpp.opencv_imgcodecs.cvSaveImage;
import static org.bytedeco.javacpp.opencv_imgproc.CV_AA;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;

public class FaceDetection {

    // The cascade definition to be used for detection.
    private static final String CASCADE_FILE = "C:\\OpenCV2.3\\opencv\\data\\haarcascades\\haarcascade_frontalface_alt.xml";

    public static void main(String arg[]) throws Exception {


        // Load the original image.
        IplImage originalImage = cvLoadImage("c:\\tmp\\churchill-roosevelt-stalin-yalta.jpg",1);

        // We need a grayscale image in order to do the recognition, so we
        // create a new image of the same size as the original one.
        IplImage grayImage = IplImage.create(originalImage.width(),
                originalImage.height(), opencv_core.IPL_DEPTH_8U, 1);

        // We convert the original image to grayscale.
        opencv_imgproc.cvCvtColor(originalImage, grayImage, CV_BGR2GRAY);

        opencv_core.CvMemStorage storage = opencv_core.CvMemStorage.create();

        // We instantiate a classifier cascade to be used for detection, using
        // the cascade definition.
        CvHaarClassifierCascade cascade;
        cascade = new CvHaarClassifierCascade(
                cvLoad(CASCADE_FILE));

        // We detect the faces.
        opencv_core.CvSeq faces = cvHaarDetectObjects(grayImage, cascade, storage, 1.1, 1,
                0);

        // We iterate over the discovered faces and draw yellow rectangles
        // around them.
        for (int i = 0; i < faces.total(); i++) {
            opencv_core.CvRect r = new opencv_core.CvRect(cvGetSeqElem(faces, i));
            opencv_imgproc.cvRectangle(originalImage, cvPoint(r.x(), r.y()),
                    cvPoint(r.x() + r.width(), r.y() + r.height()),
                    CvScalar.YELLOW, 1, CV_AA, 0);

        }

        // Save the image to a new file.
        cvSaveImage("E:\\tmp\\churchill-roosevelt-stalin-yalta_new.jpg", originalImage);

    }

}
