package com.easy.rtsp;

/**
 *  最近对于流媒体技术比较感兴趣，虽然读书的时候学过相关方面的基础知识，但是大学上课，你懂得，一方面理论与实际脱节很严重，另一方面考试完全就是突击。学了和没学一样。好了，吐槽结束，书归正文。
 *
 *     研究流媒体技术的前提是先明白三个协议，RTSP，RTCP和RTP。关于这三种协议具体的定义百度上可以说是一抓一大把。总的来说， RTSP控制负责控制，包括创建，播放和暂停等操作，RTCP和RTP可以认为是一种协议，最大的区别 是RTCP中没有负载(payload，也就是媒体数据流)，RTP则包含了负载。RTCP主要负责传输server和client的状态，如已经接收了多少数据，时间戳是什么，而RTP主要作用就是传输流媒体数据。
 *
 *     大部分对于RTSP都提到了这一个词：“RTSP是文本协议”，这句话是什么意思？通俗点说，如果你想告诉服务器你的名字，你首先构建一个类似于name="xxxxx"的字符串，然后把这个字符串转成byte[]，经过SOCKET传给服务器，服务器就能够知道你的名字了。与之形成对比的是RTCP，RTCP规定了每个比特的每一位都代表什么，例如一个RTCP包的第一个比特的前两位代表版本，第三位用来填充，而第二个比特代表这次会话的序列号。坦率的说，实现RTCP协议可比RTSP烧脑多了。
 *
 *    回到RTSP这个话题，RTSP协议包含以下几种操作，option，describe，setup，play，pause和teardown。option是询问服务器你能提供什么方法，describe则是获取服务器的详细信息，setup是与服务器建立连接，服务器返回一个sessionid用来之后进行鉴权，play就是通知服务器可以发数据了，pause则是通知服务器暂停发数据，teardown，挥泪告别，さようなら。
 *
 *    如果你在百度上搜索过如下的关键字：RTSP  java。你会发现有人已经实现了RTSP协议，如果你真的使用了那份代码，恭喜你，你踩到坑啦。大部分转载的人并没有对转载的内容进行验证。我被网上的这份代码坑了号就，今天刚刚出坑，特此记录。
 *
 *     RTSPProtocal：RTSP协议类，主要负责创建RTSP文本
 * ---------------------
 * 作者：浪迹中华
 * 来源：CSDN
 * 原文：https://blog.csdn.net/csluanbin/article/details/51713479
 * 版权声明：本文为博主原创文章，转载请附上博文链接！
 */
public class RTSPProtocal {

    public static byte[] encodeOption(String address, String VERSION, int seq) {
        StringBuilder sb = new StringBuilder();
        sb.append("OPTIONS ");
        sb.append(address.substring(0, address.lastIndexOf("/")));
        sb.append(VERSION);
        sb.append("Cseq: ");
        sb.append(seq);
        sb.append("\r\n");
        sb.append("\r\n");
        System.out.println(sb.toString());
        //send(sb.toString().getBytes());
        return sb.toString().getBytes();
    }

    public static byte[] encodeDescribe(String address, String VERSION, int seq) {
        StringBuilder sb = new StringBuilder();
        sb.append("DESCRIBE ");
        sb.append(address);
        sb.append(VERSION);
        sb.append("Cseq: ");
        sb.append(seq);
        sb.append("\r\n");
        sb.append("\r\n");
        System.out.println(sb.toString());
        //send(sb.toString().getBytes());
        return sb.toString().getBytes();
    }

    public static byte[] encodeSetup(String address, String VERSION, String sessionid,
                                     int portOdd, int portEven, int seq, String trackInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("SETUP ");
        sb.append(address);
        sb.append("/");
        sb.append(trackInfo);
        sb.append(VERSION);
        sb.append("Cseq: ");
        sb.append(seq++);
        sb.append("\r\n");
        //"50002-50003"
        sb.append("Transport: RTP/AVP;UNICAST;client_port="+portEven+"-"+portOdd+";mode=play\r\n");
        sb.append("\r\n");
        System.out.println(sb.toString());
        System.out.println(sb.toString());
        //send(sb.toString().getBytes());
        return sb.toString().getBytes();
    }

    public static byte[] encodePlay(String address, String VERSION, String sessionid, int seq) {
        StringBuilder sb = new StringBuilder();
        sb.append("PLAY ");
        sb.append(address);
        sb.append(VERSION);
        sb.append("Session: ");
        sb.append(sessionid);
        sb.append("Cseq: ");
        sb.append(seq);
        sb.append("\r\n");
        sb.append("Range: npt=0.000-");
        sb.append("\r\n");
        sb.append("\r\n");
        System.out.println(sb.toString());
        //send(sb.toString().getBytes());
        return sb.toString().getBytes();
    }

    public static byte[] encodePause(String address, String VERSION, String sessionid, int seq) {
        StringBuilder sb = new StringBuilder();
        sb.append("PAUSE ");
        sb.append(address);
        sb.append("/");
        sb.append(VERSION);
        sb.append("Cseq: ");
        sb.append(seq);
        sb.append("\r\n");
        sb.append("Session: ");
        sb.append(sessionid);
        sb.append("\r\n");
        System.out.println(sb.toString());
        //send(sb.toString().getBytes());
        return sb.toString().getBytes();
    }

    public static byte[] encodeTeardown(String address, String VERSION, String sessionid, int seq) {
        StringBuilder sb = new StringBuilder();
        sb.append("TEARDOWN ");
        sb.append(address);
        sb.append("/");
        sb.append(VERSION);
        sb.append("Cseq: ");
        sb.append(seq);
        sb.append("\r\n");
        sb.append("User-Agent: LibVLC/2.2.1 (LIVE555 Streaming Media v2014.07.25)\r\n");
        sb.append("Session: ");
        sb.append(sessionid);
        sb.append("\r\n");
        System.out.println(sb.toString());
        return sb.toString().getBytes();
        //send(sb.toString().getBytes());
        //
    }
}
