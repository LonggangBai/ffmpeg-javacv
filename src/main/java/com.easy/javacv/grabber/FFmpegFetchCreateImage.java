package com.easy.javacv.grabber;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.madgag.gif.fmsware.AnimatedGifEncoder;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

/**
 * java从视频中获截帧生成静态图与GIF，图片旋转
 */
public class FFmpegFetchCreateImage {
    /**
     * 截取视频指定帧生成gif
     * @param videofile 视频文件
     * @param startFrame 开始帧
     * @param frameCount 截取帧数
     * @param frameRate 帧频率（默认：3）
     * @param margin 每截取一次跳过多少帧（默认：3）
     * @throws IOException 截取的长度超过视频长度
     */
    public static void buildGif(String videofile,int startFrame,int frameCount,Integer frameRate,Integer margin) throws IOException {
        if(margin==null)margin=3;
        if(frameRate==null)frameRate=3;
        FileOutputStream targetFile = new FileOutputStream(videofile.substring(0,videofile.lastIndexOf("."))+".gif");
        FFmpegFrameGrabber ff = new FFmpegFrameGrabber(videofile);
        Java2DFrameConverter converter = new Java2DFrameConverter();
        ff.start();
        try {
            if(startFrame>ff.getLengthInFrames() & (startFrame+frameCount)>ff.getLengthInFrames()) {
                throw new RuntimeException("视频太短了");
            }
            ff.setFrameNumber(startFrame);
            AnimatedGifEncoder en = new AnimatedGifEncoder();
            en.setFrameRate(frameRate);
            en.start(targetFile);
            for(int i=0;i<frameCount;i++) {
                en.addFrame(converter.convert(ff.grab()));
                ff.setFrameNumber(ff.getFrameNumber()+margin);
            }
            en.finish();
        }finally {
            ff.stop();
            ff.close();
        }
    }
    /**
     * 将图片旋转指定度
     * @param bufferedimage 图片
     * @param degree 旋转角度
     * @return
     */
    public static BufferedImage rotateImage(BufferedImage bufferedimage,int degree){
        int w= bufferedimage.getWidth();// 得到图片宽度。
        int h= bufferedimage.getHeight();// 得到图片高度。
        int type= bufferedimage.getColorModel().getTransparency();// 得到图片透明度。
        BufferedImage img;// 空的图片。
        Graphics2D graphics2d;// 空的画笔。
        (graphics2d= (img= new BufferedImage(w, h, type))
                .createGraphics()).setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2d.rotate(Math.toRadians(degree), w / 2, h / 2);// 旋转，degree是整型，度数，比如垂直90度。
        graphics2d.drawImage(bufferedimage, 0, 0, null);// 从bufferedimagecopy图片至img，0,0是img的坐标。
        graphics2d.dispose();
        return img;// 返回复制好的图片，原图片依然没有变，没有旋转，下次还可以使用。
    }

    /**
     * 截取视频指定帧保存为指定格式的图片（图片保存在视频同文件夹下）
     * @param videofile 视频地址
     * @param imgSuffix 图片格式
     * @param indexFrame 第几帧（默认：5）
     * @throws Exception
     */
    public static void fetchFrame(String videofile,String imgSuffix,Integer indexFrame)throws Exception {
        if(indexFrame==null)indexFrame=5;
        FFmpegFrameGrabber ff = new FFmpegFrameGrabber(videofile);
        ff.start();
        try {
            int lenght = ff.getLengthInFrames();
            int i = 0;
            Frame f = null;
            while (i < lenght) {
                f = ff.grabFrame();
                if ((i > indexFrame) && (f.image != null)) {
                    break;
                }
                i++;
            }
            int owidth = f.imageWidth ;
            int oheight = f.imageHeight ;
            int width = 800;
            int height = (int) (((double) width / owidth) * oheight);
            Java2DFrameConverter converter =new Java2DFrameConverter();
            BufferedImage fecthedImage =converter.getBufferedImage(f);
            BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            bi.getGraphics().drawImage(fecthedImage.getScaledInstance(width, height, Image.SCALE_SMOOTH),
                    0, 0, null);
            bi=rotateImage(bi, 90);
            File targetFile = new File(videofile.substring(0,videofile.lastIndexOf("."))+imgSuffix);
            ImageIO.write(bi, "jpg", targetFile);
        }finally {
            ff.stop();
            ff.close();
        }
    }

    public static void main(String[] args) {
        try {
            //fetchFrame("D:\\test\\QQ.mp4",".jpg",10);
            buildGif("D:\\test\\QQ.mp4", 5, 20, 3,3);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}