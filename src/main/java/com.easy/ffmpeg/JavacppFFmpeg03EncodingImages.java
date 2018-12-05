package com.easy.ffmpeg;

import java.awt.image.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import static org.bytedeco.javacpp.avcodec.*;
import static org.bytedeco.javacpp.avformat.*;
import static org.bytedeco.javacpp.avutil.*;
import static org.bytedeco.javacpp.swscale.*;


import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Base64;
import java.util.Timer;
import java.util.TimerTask;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.PointerPointer;
import sun.misc.BASE64Encoder;

import javax.imageio.ImageIO;
import javax.swing.*;


/**
 * <pre>
 * ffmpeg获取的数据一般为yuv,argb,rgb,bgr,abgr等图像像素数据，
 * 我们可能需要转换为java的图像，来方便我们显示他们，当然不需要转换也可以达到我们的目的。
 *
 * 像素图像数据转换为java图像
 *
 * 上一章我们已经通过ffmpeg获取到了AVFrame，要是能预览这帧视频图像那岂不是更好？
 *
 * 要完成转换为java的图像数据这个转换我们分为两步转换流程。
 *
 * 1、像素图像转换为ByteBuffer
 *
 * 不管是yuv，还是rgb或是bgr，都可以使用该函数转换为ByteBuffer，不同的是ByteBuffer转换为BufferedImage的异同。
 *
 * //eguid原创文章，转载请注明出处和作者名(https://blog.csdn.net/eguid_1)
 *
 * 2、ByteBuffer转换为BufferImage
 * ---------------------
 * 作者： eguid
 * 来源：CSDN
 * 原文：https://blog.csdn.net/eguid_1/article/details/82767187
 * 版权声明：本文为博主原创文章，转载请附上博文链接！
 * </pre>
 */
public abstract class JavacppFFmpeg03EncodingImages {

    public static int getRGB(int[] rgbarr) {//RGB24数组转整型RGB24
        int rgb = ((int) rgbarr[0]) & 0xff | (((int) rgbarr[1]) & 0xff) << 8 | (((int) rgbarr[2]) & 0xff) << 16
                | 0xff000000;
        return rgb;
    }
//eguid原创文章，转载请注明出处和作者名(https://blog.csdn.net/eguid_1)

    public ByteBuffer saveFrame(AVFrame pFrame, int width, int height) {
        BytePointer data = pFrame.data(0);
        int size = width * height * 3;
        ByteBuffer buf = data.position(0).limit(size).asBuffer();
        return buf;
    }


    /**
     * 24位BGR转BufferedImage
     *
     * @param src       -源数据
     * @param width     -宽度
     * @param height-高度
     * @return
     */
    public static BufferedImage BGR2BufferedImage(ByteBuffer src, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        Raster ra = image.getRaster();
        DataBuffer out = ra.getDataBuffer();
        DataBufferByte db = (DataBufferByte) out;
        ByteBuffer.wrap(db.getData()).put(src);
        return image;
    }

    /**
     * 24位整型BGR转BufferedImage
     *
     * @param src       -源数据
     * @param width     -宽度
     * @param height-高度
     * @return
     */
    public static BufferedImage BGR2BufferedImage(IntBuffer src, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
        Raster ra = image.getRaster();
        DataBuffer out = ra.getDataBuffer();
        DataBufferInt db = (DataBufferInt) out;
        IntBuffer.wrap(db.getData()).put(src);
        return image;
    }

    /**
     * 24位整型RGB转BufferedImage
     *
     * @param src       -源数据
     * @param width     -宽度
     * @param height-高度
     * @return
     */
    public static BufferedImage RGB2BufferedImage(IntBuffer src, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Raster ra = image.getRaster();
        DataBuffer out = ra.getDataBuffer();
        DataBufferInt db = (DataBufferInt) out;
        IntBuffer.wrap(db.getData()).put(src);
        return image;
    }

    /**
     * bufferedImage转base64
     *
     * @param format -格式（jpg,png,bmp,gif,jpeg等等）
     * @return
     * @throws IOException
     */
    public static String bufferedImage2Base64(BufferedImage image, String format) throws IOException {
        Base64.Encoder encoder = Base64.getEncoder();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();// 字节流
        ImageIO.write(image, format, baos);// 写出到字节流
        byte[] bytes = baos.toByteArray();
        // 编码成base64
        String jpg_base64 = encoder.encodeToString(bytes);
        return jpg_base64;
    }

    /**
     * 使用窗口显示BufferedImage图片
     *
     * @param image -BufferedImage
     */
    public static void viewImage(BufferedImage image) {
        int width = image.getWidth(), height = image.getHeight();
        JLabel label = new JLabel();
        label.setSize(width, height);
        label.setIcon(new ImageIcon(image));

        JFrame frame = new JFrame();
        frame.setSize(width, height);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(label);
        frame.setVisible(true);
    }


    static String getImageBinary() {
        File f = new File("d://in.jpg");
        try {
            BufferedImage bi = ImageIO.read(f);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "jpg", baos);
            byte[] bytes = baos.toByteArray();

            return Base64.getEncoder().encodeToString(bytes).trim();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    static void base64StringToImage(String base64String) {
        try {
            byte[] bytes1 = Base64.getDecoder().decode(base64String);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes1);
            BufferedImage bi1 = ImageIO.read(bais);
            File f1 = new File("d://out.jpg");
            ImageIO.write(bi1, "jpg", f1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String tobase64(BufferedImage png) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();//io流
        ImageIO.write(png, "png", baos);//写入流中
        byte[] bytes = baos.toByteArray();//转换成字节
        BASE64Encoder encoder = new BASE64Encoder();
        String png_base64 = encoder.encodeBuffer(bytes).trim();//转换成base64串
        png_base64 = png_base64.replaceAll("\n", "").replaceAll("\r", "");//删除 \r\n
        return png_base64;
    }

    /**
     * 一、那么先来个RGB像素使用的小demo压压惊
     * <p>
     * （密集恐惧症预警）
     * <p>
     * 通过这个RGB像素的小demo，更容易理解RGB像素格式
     *
     * @return
     */
    private static int createRandomRgb() {//随机生成RGB24
        int[] rgbarr = new int[3];
        rgbarr[0] = (int) (Math.random() * 255);
        rgbarr[1] = (int) (Math.random() * 255);
        rgbarr[2] = (int) (Math.random() * 255);
        return getRGB(rgbarr);
    }

    //eguid原创文章，转载请注明出处和作者名(blog.csdn.net/eguid_1)

    public static void saveImage(BufferedImage image, String format, String file) throws IOException {
        ImageIO.write(image, format, new File(file));
    }

    //eguid原创文章，转载请注明出处和作者名(blog.csdn.net/eguid_1)
    public static void main(String[] args) {
        int width = 800, height = 600;

        //使用整型RGB24
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int rgb = createRandomRgb();
                image.setRGB(i, j, rgb);
            }
        }
        JLabel label = new JLabel();
        label.setSize(width, height);
        label.setIcon(new ImageIcon(image));

        JFrame frame = new JFrame();
        frame.setSize(width, height);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(label);
        frame.setVisible(true);
        Timer timer = new Timer("定时刷新", true);

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override


            public void run() {
                for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height; j++) {
                        int rgb = createRandomRgb();
                        image.setRGB(i, j, rgb);
                    }
                }
                label.repaint();
            }

        }, 100, 1000 / 25);
    }
}
