package com.easy.javacv;

import org.bytedeco.javacv.CanvasFrame;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * 一、实现的功能
 * 1、屏幕设备遍历
 *
 * 2、本地屏幕图像采集（也叫屏幕图像捕获）
 *
 * 3、播放本地图像（采用javacv）
 *
 * 4、关闭播放窗口即停止图像采集
 */
public class CaptureScreenTest {
    public static void captureScreen(){
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();// 获取当前屏幕大小
        Rectangle rectangle = new Rectangle(screenSize);// 指定捕获屏幕区域大小，这里使用全屏捕获
        //做好自己!--eguid，eguid的博客是:blog.csdn.net/eguid_1
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();//本地环境
        GraphicsDevice[] gs = ge.getScreenDevices();//获取本地屏幕设备列表
        System.err.println("eguid温馨提示，找到"+gs.length+"个屏幕设备");
        Robot robot=null;
        int ret=-1;
        for(int index=0;index<10;index++){
            GraphicsDevice g=gs[index];
            try {
                robot= new Robot(g);
                BufferedImage img=robot.createScreenCapture(rectangle);
                if(img!=null&&img.getWidth()>1){
                    ret=index;
                    break;
                }
            } catch (AWTException e) {
                System.err.println("打开第"+index+"个屏幕设备失败，尝试打开第"+(index+1)+"个屏幕设备");
            }
        }
        System.err.println("打开的屏幕序号："+ret);
        CanvasFrame frame = new CanvasFrame("eguid屏幕录制");// javacv提供的图像展现窗口
        int width = 800;
        int height = 600;
        frame.setBounds((int) (screenSize.getWidth() - width) / 2, (int) (screenSize.getHeight() - height) / 2, width,
                height);// 窗口居中
        frame.setCanvasSize(width, height);// 设置CanvasFrame窗口大小
        while (frame.isShowing()) {
            BufferedImage image = robot.createScreenCapture(rectangle);// 从当前屏幕中读取的像素图像，该图像不包括鼠标光标
            frame.showImage(image);

            try {
                Thread.sleep(45);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        frame.dispose();
    }

}
