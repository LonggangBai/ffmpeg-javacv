package com.easy.javacv;

import java.awt.image.BufferedImage;

import java.io.File;

import java.io.IOException;

import java.util.ArrayList;

import java.util.Collections;

import java.util.List;


import javax.imageio.ImageIO;
import org.bytedeco.javacpp.opencv_core;

import org.bytedeco.javacpp.opencv_core.IplImage;

import org.bytedeco.javacv.FFmpegFrameGrabber;

import org.bytedeco.javacv.Frame;

import org.bytedeco.javacv.FrameGrabber.Exception;

import org.bytedeco.javacv.Java2DFrameConverter;

import org.bytedeco.javacv.OpenCVFrameConverter;

/**
 * 最近在做一个视频审核的功能，但是运营觉得每个视频都要看一篇太浪费时间了，于是提出了这样一个需求，
 * 给每个视频随机截取5张图片展示出来，根据这5张图片决定是否需要继续观看视频内容，
 * 以提高审核效率。既然运营提出了这样的需求，就得尽力去完成
 *
 */
public abstract class FrameGrabberKit {


    public static void main(String[] args) throws Exception {

    randomGrabberFFmpegImage("e://tmp/1.flv", "e://tmp/3.flv", "screenshot", 5);

//        randomGrabberFFmpegImage("e:/ffmpeg/ffmpeg.mp4", "./target", "screenshot", 5);

    }


    public static void randomGrabberFFmpegImage(String filePath, String targerFilePath, String targetFileName, int randomSize)

            throws Exception {

        FFmpegFrameGrabber ff = FFmpegFrameGrabber.createDefault(filePath);
        ff.start();

        String rotate = ff.getVideoMetadata("rotate");


        int ffLength = ff.getLengthInFrames();

        List<Integer> randomGrab = random(ffLength, randomSize);

        int maxRandomGrab = randomGrab.get(randomGrab.size() - 1);

        Frame f;

        int i = 0;

        while (i < ffLength) {

            f = ff.grabImage();

            if (randomGrab.contains(i)) {

                if (null != rotate && rotate.length() > 1) {

                    OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();

                    IplImage src = converter.convert(f);

                    f = converter.convert(rotate(src, Integer.valueOf(rotate)));

                }

                doExecuteFrame(f, targerFilePath, targetFileName, i);

            }

            if (i >= maxRandomGrab) {

                break;

            }

            i++;

        }

        ff.stop();

    }


    /**
     *  解决图片旋转问题
     *
     * 通过一段时间的搜索了解到，如果拍摄的视频中带有旋转(rotate)信息，那么截取出来的图片就会被旋转。通过查询API发现FFmpegFrameGrabber的getVideoMetadata("rotate")方法可以获取到视频的旋转信息。根据获取到的rotate信息对ff.grabImage()得到的Frame进行旋转，但是Frame并没有提供旋转接口。但有一个IpImage对象提供了旋转方法
     *
     * 那么现在需要解决的就是把Frame转换成IpImage,旋转后在转回Frame。
     *
     * 再次查看API发现OpenCVFrameConverter.ToIplImage提供了相互转换的接口
     *
     * OpenCVFrameConverter.ToIplImageconverter =new OpenCVFrameConverter.ToIplImage();
     *
     * converter有两个重载的方法converter(IplImage img)和converter(Frame frame)可以实现IpImage和Frame的相互转换。
     * ---------------------
     * 作者：zsc我行我素
     * 来源：CSDN
     * 原文：https://blog.csdn.net/z562743237/article/details/54667252
     * 版权声明：本文为博主原创文章，转载请附上博文链接！
     * @param src
     * @param angle
     * @return
     */
    public static IplImage rotate(IplImage src, int angle) {

        IplImage img = IplImage.create(src.height(), src.width(), src.depth(), src.nChannels());

        opencv_core.cvTranspose(src, img);

        opencv_core.cvFlip(img, img, angle);

        return img;

    }


    public static void doExecuteFrame(Frame f, String targerFilePath, String targetFileName, int index) {

        if (null == f || null == f.image) {

            return;

        }


        Java2DFrameConverter converter = new Java2DFrameConverter();


        String imageMat = "png";

        String FileName = targerFilePath + File.separator + targetFileName + "_" + index + "." + imageMat;

        BufferedImage bi = converter.getBufferedImage(f);

        File output = new File(FileName);

        try {

            ImageIO.write(bi, imageMat, output);

        } catch (IOException e) {

            e.printStackTrace();

        }

    }


    public static List<Integer> random(int baseNum, int length) {


        List<Integer> list = new ArrayList<>(length);

        while (list.size() < length) {

            Integer next = (int) (Math.random() * baseNum);

            if (list.contains(next)) {

                continue;

            }

            list.add(next);

        }

        Collections.sort(list);

        return list;

    }

}