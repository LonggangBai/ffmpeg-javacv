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
 * 获取图片缩略图
 * 随机生成多张缩略图，不返回缩略图实际存放地址
 * @author liuyazhuang
 *
 */
public abstract class VideoImageFrame {

    public static void main(String[] args) throws Exception {
        randomGrabberFFmpegImage("e:/lyz/ffmpeg.mp4", "./target", "screenshot", 5);
    }

    /**
     * 生成图片缩略图
     * @param filePath：视频完整路径
     * @param targerFilePath：缩略图存放目录
     * @param targetFileName：缩略图文件名称
     * @param randomSize：生成随机数的数量
     * @throws Exception
     */
    public static void randomGrabberFFmpegImage(String filePath, String targerFilePath, String targetFileName, int randomSize) throws Exception {
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
     * 旋转图片
     * @param src：图片
     * @param angle:旋转角度
     * @return
     */
    public static IplImage rotate(IplImage src, int angle) {
        IplImage img = IplImage.create(src.height(), src.width(), src.depth(), src.nChannels());
        opencv_core.cvTranspose(src, img);
        opencv_core.cvFlip(img, img, angle);
        return img;
    }

    /**
     * 生成缩略图
     * @param f Frame对象
     * @param targerFilePath
     * @param targetFileName
     * @param index
     */
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

    /**
     * 随机生成随机数集合
     * @param baseNum：随机种子
     * @param length：随机数集合长度
     * @return：随机数集合
     */
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
