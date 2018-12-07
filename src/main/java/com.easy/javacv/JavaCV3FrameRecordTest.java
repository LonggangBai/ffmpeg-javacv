package com.easy.javacv;

import org.bytedeco.javacv.*;

/**
 *<pre>
 * 收流器实现，录制流媒体服务器的rtsp/rtmp视频文件(基于javaCV-FFMPEG)
 *
 * 到这里我们已经实现了直播功能的全部基本操作：推流，录制，简单的直播系统和多人视频等已经可以实现了；
 *
 * 突然发现，额。。。我们的直播系统貌似没有声音！！！好吧，确实是这样，直播听不到声音确实有点low
 *
 *第一步，创建单独的目录（因为软件较多，容易混乱），下载需要的软件: 
 * 我们需要下载nginx，pcre，zlib，openssl以及nginx-rtmp-module：
 *
 * nginx 官网下载最新，
 *
 * nginx-rtmp-module 可以在github上下载最新，
 *
 * 本人是在官网下载最新
 *
 * mkdir work
 *
 * cd work
 *
 * wget http://nginx.org/download/nginx-1.10.3.tar.gz
 *
 * wget http://zlib.net/zlib-1.2.11.tar.gz
 *
 * wget https://ftp.pcre.org/pub/pcre/pcre-8.40.tar.gz
 *
 * wget https://www.openssl.org/source/openssl-1.0.2k.tar.gz
 *
 * wget https://github.com/arut/nginx-rtmp-module/archive/master.zip
 * 第二步，分别解压这四个文件：
 * tar -zxvf 文件名
 * 第三步，编译安装nginx：
 * ./configure --prefix=/usr/local/nginx --with-debug --with-pcre=../pcre-8.40 --with-zlib=../zlib-1.2.11 --with-openssl=../openssl-1.0.2k --add-module=../nginx-rtmp-module-master
 *
 * make
 *
 * sudo make install
 * 第四步：测试：
 * 1.安装目标/usr/local/nginx目录
 *
 *
 *  进入sbin目录,执行nginx，
 *
 * /usr/local/nginx/sbin/nginx
 * 2.设置开机启动
 * sudo wget https://raw.github.com/JasonGiedymin/nginx-init-ubuntu/master/nginx -O /etc/init.d/nginx
 * sudo chmod +x /etc/init.d/nginx
 * sudo update-rc.d nginx defaults
 * 3.启动nginx服务
 * sudo service nginx start
 * sudo service nginx stop
 * 4.验证nginx开启状态
 * 如果是本地，在浏览器中输入：http://localhost:8080，，localhost可以写成127.0.0.1，如果是服务器，写入服务器ip和port。
 *
 * 如下图，则开启成功。
 *
 *
 *
 * 5.配置nginx.conf
 * Nginx服务器安装好后，服务器修改配置文件nginx.conf（默认端口号8080），以支持RTMP和HLS协议，老板们可参考安装好的 rtmp-nginx-module目录下的README.md来配置，本机的路径：/usr/local/nginx/rtmp-nginx-module-master/share/rtmp-nginx-module/README.md
 *
 * 首先在里面加入rtmp的配置：
 *
 * rtmp {
 *     server {
 *         listen 1935;
 *   	#直播
 *         application live {
 *             live on;
 *         }
 *
 *         application hls {
 *             live on;
 *             hls on;
 *             hls_path /tmp/hls;
 *         }
 *
 * 	#点播
 * 	application vod {
 *             play /tmp/video;
 *                }
 *
 *     }
 * }
 * 然后,针对hls,还需要在http里面增加一个location配置
 *
 * location /hls {
 *             types {
 *                 application/vnd.apple.mpegurl m3u8;
 *                 video/mp2t ts;
 *             }
 *             root /tmp;
 *             add_header Cache-Control no-cache;
 * }
 * 注意：修改nginx.conf之后，需重启nginx服务，才会生效：
 *
 * $/usr/local/nginx/sbin/nginx -s reload
 * 再次在浏览器中测试：http://localhost:8080，以确认nginx开启的状态。
 *
 *
 * 这是一个最简单,最基础的配置, rtmp监听1935端口,如果是hls的话用hls on开启hls,并且为hls设置一个临时文件目录hls_path /tmp/hls; 其它更高级的配置可以参看nginx-rtmp-module的readme,里面有比较详细的介绍其它的配置,并且它还提供了一个通过JWPlayer在网页上播放的例子. 
 *
 * 保存完配置文件后,启动nginx,通过netstat -ltn命令可以看到增加了一个1935端口的监听.8080是nginx默认的http监听端口.
 *
 * loong@loong-machine:/usr/local/nginx$ netstat -ltn
 * 激活Internet连接 (仅服务器)
 * Proto Recv-Q Send-Q Local Address           Foreign Address         State
 * tcp        0      0 0.0.0.0:1935            0.0.0.0:*               LISTEN
 * tcp        0      0 0.0.0.0:8080            0.0.0.0:*               LISTEN
 * tcp        0      0 127.0.1.1:53            0.0.0.0:*               LISTEN
 * tcp        0      0 127.0.0.1:631           0.0.0.0:*               LISTEN
 * tcp6       0      0 ::1:631                 :::*                    LISTEN
 * loong@loong-machine:/usr/local/nginx$
 * FFmpeg推流
 * 1、下载FFmpeg
 * 官网上下载即可FFmpeg
 *
 *
 * 也可以自己下载FFmpeg源码在编译安装，以免缺少编解码库支持，
 *
 * FFmpeg编译安装教程：https://blog.csdn.net/heng615975867/article/details/79388439
 *
 *
 *
 * 2、rtmp流和hls流推流
 * 第一个是rtmp流，推到了上面配置的live上:
 *
 *  ffmpeg -re -i /home/loong/video/input.mp4 -vcodec libx264 -vprofile baseline -acodec aac -ar 44100 -strict -2 -ac 1 -f flv -s 1280x720 -q 10 rtmp://127.0.0.1:1935/live/test
 *
 *
 * 第二个HLS流，推送到hls上：
 *
 * ffmpeg -re -i /home/loong/video/input.mp4 -vcodec libx264 -vprofile baseline -acodec aac -ar 44100 -strict -2 -ac 1 -f flv -s 1280x720 -q 10 rtmp://127.0.0.1:1935/hls/test
 * 其中，HLS流表现较明显，在nginx的临时目录下，直观的可看到m3u8索引文件和N多个.ts文件。m3u8列表会实时更新，且会动态更改当前播放索引切片（.ts）。这种实时更新的机制，不会使得.ts文件长时间存在于Nginx服务器上，且当推流结束之后，该目录下的内容会被全部清除，这样无形中减缓了nginx服务器的压力。另外，也阐释了HLS这种流媒体播放相较RTMP延时较高的原因
 *
 *
 *
 * 3、播放rtmp流或hls流
 * 最简单的测试，可通过VLC播放器，也可以用ffplay，建立网络任务实现播放。所谓的播放，就是从Nginx服务器取到视频流并播放，也称之为“拉流”。需注意的是，HLS是基于HTTP的流媒体传输协议，端口为8080；而RTMP本身即为实时消息传输协议，端口为1935。由此决定了客户端访问直播流的方式，见下图：
 *
 * 直播拉流地址:
 *
 * RTMP流：rtmp://localhost:1935/rtmplive/test
 *
 * HLS流：http://localhost:8080/hls/test.m3u8
 *
 *
 *
 * FFmpeg推流：
 *
 *
 * ffplay拉流：
 *
 * ffplay rtmp://127.0.0.1:1935/live/test
 *
 * 点播拉流地址，需要把播放文件放入vod设置的路径/tmp/video下：
 *
 * ffplay rtmp://127.0.0.1/vod/input.flv
 *
 * 关于点播额外的配置nginx.conf文件，可以参考https://blog.csdn.net/code_better/article/details/54898098
 *
 * rtmp {
 *     server {
 *         listen 1935;
 *
 *         application vod {
 *             play /var/flvs; #指定存放视频文件的路径
 *         }
 *
 *         application vod_http {
 *             #myserver.com及服务器地址，如果只是本地播放，填写127.0.0.1:端口号 就行，端口好看配置文件中http监听的端口下同
 *             play http://myserver.com/vod;
 *         }
 *
 *         application vod_mirror {
 *             play /var/local_mirror http://myserver.com/vod;
 *         }
 *     }
 * }
 * 推流状态查看
 * 在nginx.cnf的http块下添加
 *
 *     location /stat {
 *             rtmp_stat all;
 *         rtmp_stat_stylesheet stat.xsl;
 *     }
 *
 *     location /stat.xsl {
 *         root /usr/local/nginx/nginx-rtmp-module/;
 *     }
 * 打开网页就可以看到正在推流的信息。
 *
 *
 *
 * FFmpeg使用语法
 * 命令参考资料
 * 推流参考资料
 * ffmpeg -i [输入文件名] [参数选项] -f [格式] [输出文件]
 * 参数选项：
 *
 * -an: 去掉音频
 * -acodec: 音频选项， 一般后面加copy表示拷贝
 * -vcodec:视频选项，一般后面加copy表示拷贝
 * -re ffmpeg读取文件有两种方式:一种是直接读取,文件被迅速读完;一种是按时间戳读取。一般都是按时间戳读取文件，
 * 格式：
 *
 * h264: 表示输出的是h264的视频裸流
 * mp4: 表示输出的是mp4的视频
 * mpegts: 表示ts视频流
 * 命令行加入-re，表示按时间戳读取文件
 *
 * 示例
 * H264视频转mp4
 *
 * ffmpeg -i test.h264 -vcodec copy -f mp4 test.mp4
 * 重新调整视频尺寸大小(仅限Linux平台)
 *
 * ffmpeg -vcodec mpeg4 -b 1000 -r 10 -g 300 -i ~/test.avi -s 800×600 ~/test-800-600.avi
 * 把摄像头的实时视频录制下来，存储为文件(仅限Linux平台)
 *
 * ffmpeg  -f video4linux -s 320*240 -r 10 -i /dev/video0 test.asf
 * udp视频流的推送
 *
 * ffmpeg -re  -i 1.ts  -c copy -f mpegts   udp://192.168.0.106:1234```
 *
 *
 * 参考：
 *
 * https://blog.csdn.net/code_better/article/details/54898098
 *
 * https://www.jianshu.com/p/31c195fd50a4?utm_campaign=maleskine&utm_content=note&utm_medium=seo_notes&utm_source=recommendation
 *
 *
 * ---------------------
 * 作者：Loongxu
 * 来源：CSDN
 * 原文：https://blog.csdn.net/heng615975867/article/details/80519274
 * 版权声明：本文为博主原创文章，转载请附上博文链接！
 *
 * </pre>
 */
public class JavaCV3FrameRecordTest {


    /**
     * 按帧录制视频
     *
     * @param inputFile-该地址可以是网络直播/录播地址，也可以是远程/本地文件路径
     * @param outputFile
     *            -该地址只能是文件地址，如果使用该方法推送流媒体服务器会报错，原因是没有设置编码格式
     * @throws FrameGrabber.Exception
     * @throws FrameRecorder.Exception
     * @throws org.bytedeco.javacv.FrameRecorder.Exception
     */
    public static void frameRecord(String inputFile, String outputFile, int audioChannel)
            throws Exception, org.bytedeco.javacv.FrameRecorder.Exception {

        boolean isStart=true;//该变量建议设置为全局控制变量，用于控制录制结束
        // 获取视频源
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputFile);
        // 流媒体输出地址，分辨率（长，高），是否录制音频（0:不录制/1:录制）
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, 1280, 720, audioChannel);
        // 开始取视频源
        recordByFrame(grabber, recorder, isStart);
    }


    private static void recordByFrame(FFmpegFrameGrabber grabber, FFmpegFrameRecorder recorder, Boolean status)
            throws Exception, org.bytedeco.javacv.FrameRecorder.Exception {
        try {//建议在线程中使用该方法
            grabber.start();
            recorder.start();
            Frame frame = null;
            while (status&& (frame = grabber.grabFrame()) != null) {
                recorder.record(frame);
            }
            recorder.stop();
            grabber.stop();
        } finally {
            if (grabber != null) {
                grabber.stop();
            }
        }
    }
    public static void main(String[] args)
            throws Exception {

        String inputFile = "rtsp://admin:admin@192.168.2.236:37779/cam/realmonitor?channel=1&subtype=0";
        // Decodes-encodes
        String outputFile = "recorde.mp4";
        frameRecord(inputFile, outputFile,1);
    }

}
