package com.easy.javacppopencv;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_imgcodecs;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacpp.opencv_videoio;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Scalar;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * <pre>
 * javacpp-opencv图像处理3：使用opencv原生方法遍历摄像头设备及调用(增加实时帧率计算方法)
 *
 * 前言：
 * 鉴于很多同学反馈目前javacv采集摄像头存在几点问题
 *
 * 1、javacv采集摄像头帧率很低
 *
 * 2、javacv中的摄像头采集依赖opencv的capture采集器，获取的Mat没有及时释放，容易内存溢出
 *
 * 3、javacv封装的太死，调用摄像头不灵活，无法遍历摄像头设备列表
 *
 * 4、javacv打开摄像头太慢，一般要3秒才能打开摄像头设备
 *
 * 所以直接使用opencv采集摄像头设备是一个比较好的方案，并且采集效率上得到了很大的提高，不会像javacv里面一样摄像头掉帧比较严重。
 *
 *  
 *
 * 一、实现的功能
 *  
 *
 * （1）opencv原生摄像头图像采集
 *
 * （2）opencv原生摄像头设备遍历
 *
 * （3）Mat转换为Frame
 *
 * （4）计算实时帧率
 *
 * （5）文字水印（显示实时帧率）
 *
 *  
 *
 * 二、实现代码
 * 1、无水印，无帧率计算实现：
 * ---------------------
 * 作者： eguid
 * 来源：CSDN
 * 原文：https://blog.csdn.net/eguid_1/article/details/58027720
 * 版权声明：本文为博主原创文章，转载请附上博文链接！
 * </pre>
 */
public class Javacppopencv03AddImageMarker {


    /**
     * 、无水印，无帧率计算实现：
     */
    public void ddd(){
//        做好自己！--eguid
        opencv_videoio.VideoCapture vc=null;
        //遍历查找摄像头
        int index=-1;
        for(;index<2;index++){
            vc=new opencv_videoio.VideoCapture(index);
            if(vc.grab()){
                //找到摄像头设备，退出遍历
                System.err.println("当前摄像头："+index);
                break;
            }
            vc.close();//没找到设备，释放资源
        }
        //vc为null，并且设备没正常开启，说明没找到设备
        if(vc!=null&&!vc.isOpened()){
            System.err.println("无法找到摄像头，请检查是否存在摄像头设备");
            return;
        }
        //使用java的JFrame显示图像
        CanvasFrame cFrame = new CanvasFrame("做好自己！--eguid！http://www.eguid.cc/",CanvasFrame.getDefaultGamma()/2.2);
        //javacv提供的转换器，方便mat转换为Frame
        OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
        opencv_core.Mat mat=new opencv_core.Mat();
        for(;;){
            vc.retrieve(mat);//重新获取mat
            if(vc.grab()){//是否采集到摄像头数据
                if(vc.read(mat)){//读取一帧mat图像
//				opencv_highgui.imshow("eguid", mat);该opencv方法windows下会无响应
                    cFrame.showImage(converter.convert(mat));
                }
                mat.release();//释放mat
            }

            try {
                Thread.sleep(45);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
    /**
     * 实时计算帧率并加实时帧率文字水印到图像
     */
    public void dd() {
        //做好自己！--eguid！http://www.eguid.cc
        String msg = "fps:";//水印文字
        // 水印文字位置
        Point point = new Point(10, 50);
        // 颜色，使用黄色
        Scalar scalar = new Scalar(0, 255, 255, 0);
        DecimalFormat df = new DecimalFormat(".##");//数字格式化
        opencv_videoio.VideoCapture vc = null;
        //遍历查找摄像头
        int index = -1;
        for (; index < 2; index++) {
            vc = new opencv_videoio.VideoCapture(index);
            if (vc.grab()) {
                //找到摄像头设备，退出遍历
                System.err.println("做好自己！--eguid温馨提示，获取本机当前摄像头序号：" + index);
                break;
            }
            vc.close();//没找到设备，释放资源
        }
        //vc为null，并且设备没正常开启，说明没找到设备
        if (vc != null && !vc.isOpened()) {
            System.err.println("无法找到摄像头，请检查是否存在摄像头设备");
            return;
        }
        //使用java的JFrame显示图像
        CanvasFrame cFrame = new CanvasFrame("做好自己！--eguid！http://www.eguid.cc", CanvasFrame.getDefaultGamma() / 2.2);
        //javacv提供的转换器，方便mat转换为Frame
        OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
        opencv_core.Mat mat = new opencv_core.Mat();
        double start = System.currentTimeMillis();
        double end;
        for (int i = 0; ; i++) {
            vc.retrieve(mat);//重新获取mat
            if (vc.grab()) {//是否采集到摄像头数据
                if (vc.read(mat)) {//读取一帧mat图像
                    end = System.currentTimeMillis();
                    if (mat != null) {
                        opencv_imgproc.putText(mat, msg + df.format((1000.0 / (end - start))), point, opencv_imgproc.CV_FONT_VECTOR0, 1.2, scalar, 1, 20, false);
                    }
//				opencv_highgui.imshow("eguid", mat);该opencv方法windows下会无响应
                    cFrame.showImage(converter.convert(mat));
                    System.err.println(i);
                    start = end;
                }
                mat.release();//释放mat
            }
        }

    }
}
