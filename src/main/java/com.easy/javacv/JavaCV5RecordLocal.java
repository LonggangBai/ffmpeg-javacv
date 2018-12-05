//package com.ewivt.bjss.ccs;
//
//import javax.sound.sampled.*;
//import java.nio.ByteBuffer;
//import java.nio.ByteOrder;
//import java.nio.ShortBuffer;
//
///**
// *
// * <pre>
// 实现录制本机麦克风音频到本地文件或者流媒体服务器，
//
// 对于录制音视频混合的同学可以很方便的将本章代码移植到到录制视频的代码里
//
// 注意：由于音频、视频时两个不同线程同时进行，所以在进行混合录制的时候需要注意统一帧率，以防止音画不同步现象
// * 	</pre>
// */
//public class JavaCV5RecordLocal {
//
//
//
//    public Runnable dd(){
//
//
//        /**
//         * 设置音频编码器 最好是系统支持的格式，否则getLine() 会发生错误
//         * 采样率:44.1k;采样率位数:16位;立体声(stereo);是否签名;true:
//         * big-endian字节顺序,false:little-endian字节顺序(详见:ByteOrder类)
//         */
//        AudioFormat audioFormat = new AudioFormat(44100.0F, 16, 2, true, false);
//        System.out.println("准备开启音频！");
//        // 通过AudioSystem获取本地音频混合器信息
//        Mixer.Info[] minfoSet = AudioSystem.getMixerInfo();
//        // 通过AudioSystem获取本地音频混合器
//        Mixer mixer = AudioSystem.getMixer(minfoSet[1]);
//        // 通过设置好的音频编解码器获取数据线信息
//        DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
//
//        // 打开并开始捕获音频
//        // 通过line可以获得更多控制权
//        // 获取设备：TargetDataLine line
//        // =(TargetDataLine)mixer.getLine(dataLineInfo);
//        Line dataline = null;
//        try {
//            dataline = AudioSystem.getLine(dataLineInfo);
//        } catch (LineUnavailableException e2) {
//            System.err.println("开启失败...");
//            return null;
//        }
//        TargetDataLine line = (TargetDataLine) dataline;
//        try {
//            line.open(audioFormat);
//        } catch (LineUnavailableException e1) {
//            line.stop();
//            try {
//                line.open(audioFormat);
//            } catch (LineUnavailableException e) {
//                System.err.println("按照指定音频编码器打开失败...");
//                return null;
//            }
//        }
//        line.start();
//        System.out.println("已经开启音频！");
//        // 获得当前音频采样率
//        int sampleRate = (int) audioFormat.getSampleRate();
//        // 获取当前音频通道数量
//        int numChannels = audioFormat.getChannels();
//        // 初始化音频缓冲区(size是音频采样率*通道数)
//        int audioBufferSize = sampleRate * numChannels;
//        byte[] audioBytes = new byte[audioBufferSize];
//
//        Runnable crabAudio = new Runnable() {
//            ShortBuffer sBuff = null;
//            int nBytesRead;
//            int nSamplesRead;
//
//            @Override
//            public void run() {
//                System.out.println("读取音频数据...");
//                // 非阻塞方式读取
//                nBytesRead = line.read(audioBytes, 0, line.available());
//                // 因为我们设置的是16位音频格式,所以需要将byte[]转成short[]
//                nSamplesRead = nBytesRead / 2;
//                short[] samples = new short[nSamplesRead];
//                /**
//                 * ByteBuffer.wrap(audioBytes)-将byte[]数组包装到缓冲区
//                 * ByteBuffer.order(ByteOrder)-按little-endian修改字节顺序，解码器定义的
//                 * ByteBuffer.asShortBuffer()-创建一个新的short[]缓冲区
//                 * ShortBuffer.get(samples)-将缓冲区里short数据传输到short[]
//                 */
//                ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples);
//                // 将short[]包装到ShortBuffer
//                sBuff = ShortBuffer.wrap(samples, 0, nSamplesRead);
//                // 按通道录制shortBuffer
//                try {
//                    System.out.println("录制音频数据...");
//                    recorder.recordSamples(sampleRate, numChannels, sBuff);
//                } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
//                    // do nothing
//                }
//            }
//
//            @Override
//            protected void finalize() throws Throwable {
//                sBuff.clear();
//                sBuff = null;
//                super.finalize();
//            }
//        };
//        return crabAudio;
//
//    }
//    crabAudio = new Runnable() {
//            ShortBuffer sBuff = null;
//            int nBytesRead;
//            int nSamplesRead;
//
//            @Override
//            public void run() {
//                System.out.println("读取音频数据...");
//                // 非阻塞方式读取
//                nBytesRead = line.read(audioBytes, 0, line.available());
//                // 因为我们设置的是16位音频格式,所以需要将byte[]转成short[]
//                nSamplesRead = nBytesRead / 2;
//                short[] samples = new short[nSamplesRead];
//                /**
//                 * ByteBuffer.wrap(audioBytes)-将byte[]数组包装到缓冲区
//                 * ByteBuffer.order(ByteOrder)-按little-endian修改字节顺序，解码器定义的
//                 * ByteBuffer.asShortBuffer()-创建一个新的short[]缓冲区
//                 * ShortBuffer.get(samples)-将缓冲区里short数据传输到short[]
//                 */
//                ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples);
//                // 将short[]包装到ShortBuffer
//                sBuff = ShortBuffer.wrap(samples, 0, nSamplesRead);
//                // 按通道录制shortBuffer
//                try {
//                    System.out.println("录制音频数据...");
//                    recorder.recordSamples(sampleRate, numChannels, sBuff);
//                } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
//                    // do nothing
//                }
//            }
//
//            @Override
//            protected void finalize() throws Throwable {
//                sBuff.clear();
//                sBuff = null;
//                super.finalize();
//            }
//        };
//        return crabAudio;
//
//    }
//
//
//
//    }
//    public static void main(String[] args)
//            throws Exception {
//
//        String inputFile = "rtsp://admin:admin@192.168.2.236:37779/cam/realmonitor?channel=1&subtype=0";
//
//        String outputFile = "rtmp://192.168.30.21/live/pushFlow";
//
//        recordPush(inputFile, outputFile, 25);
//    }
//
//}
