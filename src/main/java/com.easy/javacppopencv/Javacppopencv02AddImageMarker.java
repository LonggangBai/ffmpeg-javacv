package com.easy.javacppopencv;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_imgcodecs;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;

import java.text.SimpleDateFormat;
import java.util.Date;
/**
 * javacpp-opencv图像处理之2：实时视频添加图片水印，实现不同大小图片叠加，图像透明度控制，文字和图片双水印
 * 前言：
 * 本章通过javaCV-openCV处理图像，其中javaCV部分负责摄像头抓取和Frame、Mat转换操作，openCV负责图像加载、图像保存以及 图像叠加、感兴趣区域和透明度处理。
 *
 *  
 *
 * 1、实现的功能
 * （1）摄像头视频抓取
 *
 * （2）视频帧Frame与Mat图像相互转换
 *
 * （3）图像加载级图像保存
 *
 * （4）图像叠加、图像感兴趣区、图像透明处理
 *
 * （5）文字叠加、字体大小、粗度、颜色及平滑处理等
 *
 *  
 * ---------------------
 * 作者： eguid
 * 来源：CSDN
 * 原文：https://blog.csdn.net/eguid_1/article/details/53259649
 * 版权声明：本文为博主原创文章，转载请附上博文链接！
 */
public class Javacppopencv02AddImageMarker {

    public void addChar()throws  Exception{
        // 转换器，用于Frame/Mat/IplImage相互转换
        OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
        // 使用OpenCV抓取本机摄像头，摄像头设备号默认0
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
        // 开启抓取器
        grabber.start();
        //做好自己 - - eguid!,转载请注明出处
        CanvasFrame cFrame = new CanvasFrame("做好自己！--eguid！", CanvasFrame.getDefaultGamma() / grabber.getGamma());
        cFrame.setAlwaysOnTop(true);
        cFrame.setVisible(true);
        // 水印文字位置
        opencv_core.Point point = new opencv_core.Point(10, 50);
        // 颜色，使用黄色
        opencv_core.Scalar scalar = new opencv_core.Scalar(0, 255, 255, 0);
        Frame frame = null;
        int index = 0;

        opencv_core.Mat logo = opencv_imgcodecs.imread("4ycfb.png");
        opencv_core.Mat mask = opencv_imgcodecs.imread("4ycfb.png", 0);

        opencv_imgproc.threshold(mask,mask,254,255,opencv_imgcodecs.IMWRITE_PNG_BILEVEL);

        double alpha = 0.5;// 图像透明权重值,0-1之间
        while (cFrame.isShowing()) {
            if ((frame = grabber.grabFrame()) != null) {
                // 取一帧视频（图像），并转换为Mat
                opencv_core.Mat mat = converter.convertToMat(grabber.grabFrame());

                // 加文字水印，opencv_imgproc.putText（图片，水印文字，文字位置，字体，字体大小，字体颜色，字体粗度，平滑字体，是否翻转文字）
                opencv_imgproc.putText(mat, "eguid!", point, opencv_imgproc.CV_FONT_VECTOR0, 1.2, scalar, 1, 20, false);
                // 定义感兴趣区域(位置，logo图像大小)
                opencv_core.Mat ROI = mat.apply(new opencv_core.Rect(400, 350, logo.cols(), logo.rows()));

                opencv_core.addWeighted(ROI, alpha, logo, 1.0 - alpha, 0.0, ROI);
                // 把logo图像复制到感兴趣区域
//				 logo.copyTo(ROI, mask);
                // 显示图像到窗口
                cFrame.showImage(converter.convert(mat));
                if (index == 0) {
                    // 保存第一帧图片到本地
                    opencv_imgcodecs.imwrite("eguid.jpg", mat);
                }
                // 释放Mat资源
                ROI.release();
                ROI.close();
                mat.release();
                mat.close();
                Thread.sleep(40);
                index++;
            }

            index++;
        }
        // 关闭窗口
        cFrame.dispose();
        // 停止抓取器
        grabber.stop();
        // 释放资源
        logo.release();
        logo.close();
        mask.release();
        mask.close();
        scalar.close();
        point.close();
    }
}
