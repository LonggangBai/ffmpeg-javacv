package com.easy.javacv;

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
 * javacpp-opencv图像处理之1：实时视频添加文字水印并截取视频图像保存成图片，实现文字水印的字体、位置、大小、粗度、翻转、平滑等操作
 * 一、javaCV图像处理之1：实时视频添加文字水印并截取视频图像保存成图片，实现文字水印的字体、位置、大小、粗度、翻转、平滑等操作
 *
 * 二、javaCV图像处理之2：实时视频添加图片水印，实现不同大小图片叠加，图像透明度控制
 *
 * 三、opencv图像处理3：使用opencv原生方法遍历摄像头设备及调用（方便多摄像头遍历及调用，相比javacv更快的摄像头读取速度和效率，方便读取后的图像处理）
 *
 * 四、opencv图像处理系列：国内车辆牌照检测识别系统（万份测试准确率99.7%以上）
 *
 *  
 *1、实现的功能
 * （1）摄像头视频抓取
 *
 * （2）Frame和Mat相互转换
 *
 * （3）给图像添加文字
 *
 * （4）把添加水印文字的图像转换为Frame并显示到窗口
 *
 * （5）保存图片
 *  
 *
 * 前言：
 * 本章全部采用openCV中的Mat类型进行图像操作，Mat类型与CvMat和IplImage类型都可以代表和显示图像，但是，Mat类型侧重于计算，数学性较高，openCV对Mat类型的计算也进行了优化。而CvMat和IplImage类型更侧重于“图像”，openCV对其中的图像操作（缩放、单通道提取、图像阈值操作等）进行了优化。
 * ---------------------
 * 作者： eguid
 * 来源：CSDN
 * 原文：https://blog.csdn.net/eguid_1/article/details/53236923
 * 版权声明：本文为博主原创文章，转载请附上博文链接！
 */
public class OpenCVFrameGrabberTest {


    public static void converter()throws  Exception {
// 转换器，用于Frame/Mat/IplImage相互转换
        OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();

        // 使用OpenCV抓取本机摄像头，摄像头设备号默认0
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);

        // 开启抓取器
        grabber.start();

        //做好自己！--eguid版权所有，转载请注明出处！
        CanvasFrame cFrame = new CanvasFrame("做好自己！--eguid！", CanvasFrame.getDefaultGamma() / grabber.getGamma());
        cFrame.setAlwaysOnTop(true);
        cFrame.setVisible(true);
        // 水印文字位置
        opencv_core.Point point1 = new opencv_core.Point(10, 50);
        opencv_core.Point point2 = new opencv_core.Point(200, 200);
        opencv_core.Point point3 = new opencv_core.Point(200, 240);
        // 颜色
        opencv_core.Scalar scalar1 = new opencv_core.Scalar(0, 255, 255, 0);
        opencv_core.Scalar scalar2 = new opencv_core.Scalar(255, 0, 0, 0);
        Frame frame = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        int index = 0;
        // 关闭窗口即停止运行
        while (cFrame.isShowing()) {
            if ((frame = grabber.grabFrame()) != null) {
                // 取一帧视频（图像），并转换为Mat
                opencv_core.Mat mat = converter.convertToMat(grabber.grabFrame());

                // 加文字水印，opencv_imgproc.putText（图片，水印文字，文字位置，字体，字体大小，字体颜色，字体粗度，文字反锯齿，是否翻转文字）
                opencv_imgproc.putText(mat, "eguid!", point2, opencv_imgproc.CV_FONT_VECTOR0, 2.2, scalar2, 1, 0,
                        false);
                // 翻转字体，文字平滑处理（即反锯齿）
                opencv_imgproc.putText(mat, "eguid!", point3, opencv_imgproc.CV_FONT_VECTOR0, 2.2, scalar2, 1, 20,
                        true);

                opencv_imgproc.putText(mat, sdf.format(new Date()), point1, opencv_imgproc.CV_FONT_ITALIC, 0.8, scalar1,
                        2, 20, false);
                // 在窗口显示处理后的图像，Frame frame=converter.convert(mat);
                cFrame.showImage(converter.convert(mat));
                if (index == 0) {
                    // 保存第一帧图片到本地
                    opencv_imgcodecs.imwrite("eguid.jpg", mat);
                }
                // 释放Mat资源
                mat.release();
                mat.close();

            }
            Thread.sleep(40);
            index++;
        }

        cFrame.dispose();// 销毁窗口
        grabber.stop();// 停止抓取器

        // 手动释放资源
        scalar1.close();
        scalar2.close();
        point1.close();
        point2.close();
        point3.close();


    }
    public static void main(String[] args) throws Exception {
        converter();
    }

}
