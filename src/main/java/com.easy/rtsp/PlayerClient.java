package com.easy.rtsp;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

/**
 *     PlayerClient：播放类，通过不同状态之间的相互转化完成RTSP协议的交互工作。这里有一点非常关键：请注意setup这个状态，在和服务器建立连接之后，如果直接发送PLAY请求，服务器不会向指定的端口发送RTCP数据(这个问题困扰了我一晚上)。因此在发送PLAY请求之前，client接收RTCP和RTP的两个端口必须先向服务器的RTCP和RTP端口发送任意的数据，发送方式为UDP，服务器在setup操作时已经返回RTCP和RTP的端口信息。具体的实现参考sendBeforePlay()。我在网上没有找到这么操作的原因，这还是通过wireshark对VLC进行抓包才发现这个隐藏逻辑。
 * ---------------------
 * 作者：浪迹中华
 * 来源：CSDN
 * 原文：https://blog.csdn.net/csluanbin/article/details/51713479
 * 版权声明：本文为博主原创文章，转载请附上博文链接！
 */
public class PlayerClient {
    private RTSPClient rtspClient = new RTSPClient();
    private static final String VERSION = " RTSP/1.0\r\n";
    private static final String RTSP_OK = "RTSP/1.0 200 OK";
    private Selector selector;
    private enum Status {
        init, options, describe, setup, play, pause, teardown
    }
    private Status sysStatus = Status.init;
    private String rtspAddress = "rtsp://218.204.223.237:554/live/1/66251FC11353191F/e7ooqwcfbqjoo80j.sdp";
    private String localAddress = "192.168.31.106";
    private int localPort=0;
    private String remoteAddress = "218.204.223.237";
    private int count=0;
    private String sessionid;
    private String trackInfo;
    private boolean isSended=true;
    private int localPortOdd=50002;
    private int localPortEven=50003;
    private ReceiveSocket socket1 = new ReceiveSocket(localAddress,localPortOdd);
    private ReceiveSocket socket2 = new ReceiveSocket(localAddress,localPortEven);

    public void init(){
        rtspClient.setLocalIpAddress(localAddress);
        rtspClient.setLocalPort(localPort);
        rtspClient.setRemoteIpAddress(remoteAddress);
        rtspClient.setRemoteIPort(554);
        rtspClient.setRtspAddress(rtspAddress);
        rtspClient.setLocalPortEven(this.localPortEven);
        rtspClient.setLocalPortOdd(this.localPortOdd);
        rtspClient.addSocket(this.localPortOdd, socket1);
        rtspClient.addSocket(this.localPortEven, socket2);
        try
        {
            rtspClient.inital();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.selector = rtspClient.getSelector();
        new Thread(socket1).start();
        new Thread(socket2).start();
    }

    public void run() throws IOException{
        int seq=2;
        while(true){
            if(rtspClient.isConnected() && isSended){
                switch (sysStatus) {
                    case init:
                        byte[] message = RTSPProtocal.encodeOption(this.rtspAddress, this.VERSION, seq);
                        this.rtspClient.write(message);
                        break;
                    case options:
                        seq++;
                        message = RTSPProtocal.encodeDescribe(this.rtspAddress, this.VERSION, seq);
                        this.rtspClient.write(message);
                        break;
                    case describe:
                        seq++;
                        message = RTSPProtocal.encodeSetup(this.rtspAddress, VERSION, sessionid,
                                localPortEven, localPortOdd,seq, trackInfo);
                        this.rtspClient.write(message);
                        break;
                    case setup:
                        if(sessionid==null&&sessionid.length()>0){
                            System.out.println("setup还没有正常返回");
                        }else{
                            seq++;
                            message = RTSPProtocal.encodePlay(this.rtspAddress, VERSION, sessionid, seq);
                            this.rtspClient.write(message);
                        }
                        break;
                    case play:
                        count++;
                        System.out.println("count: "+count);
                        break;
                    case pause:
                        break;
                    default:
                        break;
                }
                isSended=false;
            }
            else{

            }
            select();
        }
    }

    private void handle(byte[] msg) {
        String tmp = new String(msg);
        System.out.println("返回内容："+tmp);
        if (tmp.startsWith(RTSP_OK)) {
            switch (sysStatus) {
                case init:
                    sysStatus = Status.options;
                    System.out.println("option ok");
                    isSended=true;
                    break;
                case options:
                    sysStatus = Status.describe;
                    trackInfo=tmp.substring(tmp.indexOf("trackID"));
                    System.out.println("describe ok");
                    isSended=true;
                    break;
                case describe:
                    sessionid = tmp.substring(tmp.indexOf("Session: ") + 9, tmp
                            .indexOf("Date:"));
                    int index = tmp.indexOf("server_port=");
                    String serverPort1 = tmp.substring(tmp.indexOf("server_port=") + 12, tmp
                            .indexOf("-", index));
                    String serverPort2 = tmp.substring(tmp.indexOf("-", index) + 1, tmp
                            .indexOf("\r\n", index));

                    this.rtspClient.setRemotePortEven(Integer.valueOf(serverPort1));
                    this.rtspClient.setRemotePortOdd(Integer.valueOf(serverPort2));

                    if(sessionid!=null&&sessionid.length()>0){
                        sysStatus = Status.setup;
                        System.out.println("setup ok");
                    }
                    isSended=true;
                    break;
                case setup:
                    sysStatus = Status.play;
                    System.out.println("play ok");
                    this.rtspClient.sendBeforePlay();
                    this.rtspClient.sendBeforePlay();
                    isSended=true;
                    break;
                case play:
                    //sysStatus = Status.pause;
                    System.out.println("pause ok");
                    isSended=true;
                    break;
                case pause:
                    sysStatus = Status.teardown;
                    System.out.println("teardown ok");
                    isSended=true;
                    //shutdown.set(true);
                    break;
                case teardown:
                    sysStatus = Status.init;
                    System.out.println("exit start");
                    isSended=true;
                    break;
                default:
                    break;
            }
        } else {
            System.out.println("返回错误：" + tmp);
        }

    }

    private void select() {
        int n = 0;
        try
        {
            if (selector == null) {
                return;
            }
            n = selector.select(1000);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        // 如果select返回大于0，处理事件
        if (n > 0) {
            for (final Iterator<SelectionKey> i = selector.selectedKeys()
                    .iterator(); i.hasNext();) {
                // 得到下一个Key
                final SelectionKey sk = i.next();
                i.remove();
                // 检查其是否还有效
                if (!sk.isValid()) {
                    continue;
                }
                if (sk.isReadable()) {
                    byte[] message = rtspClient.receive();
                    handle(message);
                }
                if (sk.isConnectable()) {
                    try {
                        rtspClient.reConnect(sk);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}
