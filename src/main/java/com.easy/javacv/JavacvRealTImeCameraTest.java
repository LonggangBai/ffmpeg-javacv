package com.easy.javacv;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FrameGrabber.Exception;
import org.bytedeco.javacv.OpenCVFrameGrabber;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;

/**
 * 、摄像头小窗口加到JFrame
 *
 *     问题：grabber.grab()获取的图像格式是Frame，Frame之间是可以切换显示的。但是，其中一个不能加到另一种中进行显示（百度了好久也不能）。
 *
 *   解决思路：将grabber.grab()转为图片格式，然后在控件中显示图片。
 *
 *           grabber.grab()-->IplImage-->BufferedImage
 *
 *   如下是IplImage-->BufferedImage
 * ---------------------
 * 作者：geyan1206
 * 来源：CSDN
 * 原文：https://blog.csdn.net/geyan1206/article/details/80539105
 * 版权声明：本文为博主原创文章，转载请附上博文链接！
 */
public class JavacvRealTImeCameraTest {
    public static void main(String[] args) throws Exception, InterruptedException {
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
        grabber.start();   //开始获取摄像头数据
        CanvasFrame canvas = new CanvasFrame("Camera");//新建一个窗口
        canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        canvas.setAlwaysOnTop(true);

        while (true) {
            if (!canvas.isDisplayable()) {//窗口是否关闭
                grabber.stop();//停止抓取
                grabber.close();
                System.exit(2);//退出
            }

            canvas.showImage(grabber.grab());//获取摄像头图像并放到窗口上显示， 这里的Frame frame=grabber.grab(); frame是一帧视频图像

            Thread.sleep(10);//10毫秒刷新一次图像
        }
    }

    /**
     * 、图片尺寸变化
     *
     *   实时视频的小窗口显示需要改变图片的大小。
     *
     * ImageIcon image = new ImageIcon(bi);
     * image.setImage(image.getImage().getScaledInstance(470, 270,Image.SCALE_DEFAULT ));
     * GUIVideo.label.setIcon(image);//转换图像格式并输出
     * 注：bi是BufferedImage的类对象。
     * ---------------------
     * 作者：geyan1206
     * 来源：CSDN
     * 原文：https://blog.csdn.net/geyan1206/article/details/80539105
     * 版权声明：本文为博主原创文章，转载请附上博文链接！
     * @param mat
     * @return
     */
    public static BufferedImage iplToBufImgData(opencv_core.IplImage mat) {
        if (mat.height() > 0 && mat.width() > 0) {
            BufferedImage image = new BufferedImage(mat.width(), mat.height(),
                    BufferedImage.TYPE_3BYTE_BGR);
            WritableRaster raster = image.getRaster();
            DataBufferByte dataBuffer = (DataBufferByte) raster.getDataBuffer();
            byte[] data = dataBuffer.getData();
            mat.getByteBuffer().get(data);
            BytePointer bytePointer =new BytePointer(data);
            mat.imageData(bytePointer);
            return image;
        }
        return null;
    }

}
