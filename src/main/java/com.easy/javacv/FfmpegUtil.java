package com.easy.javacv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * @description
 * @auth panmingshuai
 * @time 2018年3月18日下午11:31:39
 *
 */

public class FfmpegUtil {
    // 转为flv，格式自动检测
    public static void processFLV() throws IOException {
        List<String> commend = new ArrayList<String>();
        commend.add("E:\\ffmpeg\\ffmpeg"); // ffmpeg文件的位置
        commend.add("-i");
        commend.add("E:\\qwe.mp4");
        commend.add("-ab");
        commend.add("56");
        commend.add("-ar");
        commend.add("22050");
        commend.add("-qscale");
        commend.add("8");
        commend.add("-r");
        commend.add("15");
        commend.add("-s");
        commend.add("600x500");
        commend.add("E:\\qwenew.flv");

        ProcessBuilder builder = new ProcessBuilder();
        builder.command(commend);
        builder.start();
    }

    // 指定格式转换 但是这里没有设置其他参数，所以生成的文件以ffmpeg内置的生成
    // MP4转avi c:\ffmpeg\ffmpeg -i c:\ffmpeg\input\c.mp4 -f avi
    // c:\ffmpeg\output\a.avi
    public static void process() throws IOException {
        List<String> commend = new ArrayList<String>();
        commend.add("E:\\ffmpeg\\ffmpeg"); // ffmpeg文件的位置
        commend.add("-i"); // 指定要处理的文件
        commend.add("E:\\qwe.mp4");
        commend.add("-f"); // 指定转换格式
        commend.add("avi");
        commend.add("E:\\qwe2.avi"); // 最后指定文件输出的路径

        ProcessBuilder builder = new ProcessBuilder();
        builder.command(commend);
        builder.start();
    }

    // 截图
    // ffmpeg -i test.asf -y -f image2 -t 0.001 -s 352x240 a.jpg
    public static void cutScreen() throws IOException {
        List<String> commend = new ArrayList<String>();
        commend.add("E:\\ffmpeg\\ffmpeg"); // ffmpeg文件的位置
        commend.add("-i");
        commend.add("E:\\qwe.mp4");
        commend.add("-y");// 输出覆盖路径，即如果已存在下面指定路径的文件，则覆盖掉
        commend.add("-f");
        commend.add("image2");
        commend.add("-ss"); // 指定在哪截图
        commend.add("5");
        commend.add("-t"); // 指定要记录的时间，因为是截图所以是0.001s
        commend.add("0.001");
        commend.add("-s");
        commend.add("1920x1080");
        commend.add("E:\\qwe3.jpg");

        ProcessBuilder builder = new ProcessBuilder();
        builder.command(commend);
        builder.start();
    }

    // 截取gif，不推荐效果不好
    // ffmpeg -ss 25 -t 10 -i D:\Media\bear.wmv -s 320x240 -f gif -r 1 D:\b.gif
    public static void cutGif() throws IOException {
        List<String> commend = new ArrayList<String>();
        commend.add("E:\\ffmpeg\\ffmpeg"); // ffmpeg文件的位置
        commend.add("-ss");
        commend.add("25");
        commend.add("-t");
        commend.add("10");
        commend.add("-i");
        commend.add("E:\\qwe.mp4");
        commend.add("-s");
        commend.add("320x240");
        commend.add("-f");
        commend.add("gif");
        commend.add("-r");
        commend.add("1");
        commend.add("E:\\qwe4.gif");

        ProcessBuilder builder = new ProcessBuilder();
        builder.command(commend);
        builder.start();
    }

    // 截取视频
    // ffmpeg -ss [start] -t [duration] -accurate_seek -i [in].mp4 -codec copy
    // -avoid_negative_ts 1 [out].mp4
    /**
     * 一般情况下截取视频不会对视频重新编码，直接截取相关时间，导出视频， 但是这种方式会导致：如果视频结尾不是关键帧，那么视频最后就会出现一段空白。
     *
     * 使用上面的方法截取视频之后，空白视频是没有了，但是时间不会精确截取，
     * 它会找到下一个关键帧，补全这个视频，所以，导致在连续分割的视频时，视频之间存在细微的交集。
     *
     * @throws IOException
     */
    public static void cutVedio() throws IOException {
        List<String> commend = new ArrayList<String>();
        commend.add("E:\\ffmpeg\\ffmpeg"); // ffmpeg文件的位置
        commend.add("-i");
        commend.add("E:\\qwe.mp4");
        commend.add("-vcodec");//视频使用原来一样的视频编解码器。
        commend.add("copy");
        commend.add("-acodec");//音频使用原来一样的音频编解码器。
        commend.add("copy");
        commend.add("-ss");
        commend.add("00:00:25");
        commend.add("-t");
        commend.add("10");
        commend.add("E:\\qwe563.mp4");
        commend.add("-y");

        ProcessBuilder builder = new ProcessBuilder();
        builder.command(commend);
        builder.start();
    }

    public static void main(String[] args) throws IOException {
        FfmpegUtil.cutVedio();
    }
}