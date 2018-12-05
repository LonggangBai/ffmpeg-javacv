package com.easy.javacv;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FrameGrabber.Exception;
import org.bytedeco.javacv.OpenCVFrameGrabber;

import javax.swing.*;

/**
 * 调用本地摄像头窗口视频
 * @version 2016年6月13日
 * @see javavcCameraTest
 * @since  javacv1.2
 *javacv开发包是用于支持java多媒体开发的一套开发包，可以适用于本地多媒体（音视频）调用以及音视频，图片等文件后期操作（图片修改，音视频解码剪辑等等功能），
 * 这里只使用最简单的本地摄像头调用来演示一下javacv的基础功能
 * 重要：
 * 建议使用最新javaCV1.3版本，该版本已解决更早版本中已发现的大部分bug
 *
 * javacv系列文章使用6个jar包：
 *
 * javacv.jar,javacpp.jar,ffmpeg.jar,ffmpeg-系统平台.jar，opencv.jar,opencv-系统平台.jar。
 *
 * 其中ffmpeg-系统平台.jar，opencv-系统平台.jar中的系统平台根据开发环境或者测试部署环境自行更改为对应的jar包，比如windows7 64位系统替换为ffmpeg-x86-x64.jar
 *
 * 为什么要这样做：因为ffmpeg-系统平台.jar中存放的是c/c++本地so/dll库，而ffmpeg.jar就是使用javacpp封装的对应本地库java接口的实现，而javacpp就是基于jni的一个功能性封装包，方便实现jni，javacv.jar就是对9个视觉库进行了二次封装，但是实现的功能有限，所以建议新手先熟悉openCV和ffmpeg这两个C/C++库的API后再来看javaCV思路就会很清晰了。
 */

public class JavaCV01JavavcCameraTest {
    public static void main(String[] args) throws Exception, InterruptedException {
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
        grabber.start();   //开始获取摄像头数据
        CanvasFrame canvas = new CanvasFrame("摄像头");//新建一个窗口
        canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        canvas.setAlwaysOnTop(true);

        while (true) {
            if (!canvas.isDisplayable()) {//窗口是否关闭
                grabber.stop();//停止抓取
                System.exit(2);//退出
            }
            canvas.showImage(grabber.grab());//获取摄像头图像并放到窗口上显示， 这里的Frame frame=grabber.grab(); frame是一帧视频图像

            Thread.sleep(50);//50毫秒刷新一次图像
        }
    }
}