package com.easy.rtsp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *  其中:rtsp://218.207.101.236:554/mobile/3/67A451E937422331/8jH5QPU5GWS07Ugn.sdp为我在网上找到的一个rtsp的sdp地址,读者可自行更换,RTSP的默认端口为554.
 * 3.3  运行结果
 *        运行RTSPClient.java,运行结果如下所示:
 *
 * Java代码
 * 端口打开成功
 * OPTIONS rtsp://218.207.101.236:554/mobile/3/67A451E937422331 RTSP/1.0
 * Cseq: 1
 *
 *
 * 返回内容：
 * RTSP/1.0 200 OK
 * Server: PVSS/1.4.8 (Build/20090111; Platform/Win32; Release/StarValley; )
 * Cseq: 1
 * Public: DESCRIBE, SETUP, TEARDOWN, PLAY, PAUSE, OPTIONS, ANNOUNCE, RECORD
 *
 *
 * DESCRIBE rtsp://218.207.101.236:554/mobile/3/67A451E937422331/8jH5QPU5GWS07Ugn.sdp RTSP/1.0
 * Cseq: 2
 *
 *
 * 返回内容：
 * RTSP/1.0 200 OK
 * Server: PVSS/1.4.8 (Build/20090111; Platform/Win32; Release/StarValley; )
 * Cseq: 2
 * Content-length: 421
 * Date: Mon, 03 Aug 2009 08:50:36 GMT
 * Expires: Mon, 03 Aug 2009 08:50:36 GMT
 * Content-Type: application/sdp
 * x-Accept-Retransmit: our-retransmit
 * x-Accept-Dynamic-Rate: 1
 * Content-Base: rtsp://218.207.101.236:554/mobile/3/67A451E937422331/8jH5QPU5GWS07Ugn.sdp/
 *
 * v=0
 * o=MediaBox 127992 137813 IN IP4 0.0.0.0
 * s=RTSP Session
 * i=Starv Box Live Cast
 * c=IN IP4 218.207.101.236
 * t=0 0
 * a=range:npt=now-
 * a=control:*
 * m=video 0 RTP/AVP 96
 * b=AS:20
 * a=rtpmap:96 MP4V-ES/1000
 * a=fmtp:96 profile-level-id=8; config=000001b008000001b5090000010000000120008440fa282c2090a31f; decode_buf=12586
 * a=range:npt=now-
 * a=framerate:5
 * a=framesize:96 176-144
 * a=cliprect:0,0,144,176
 * a=control:trackID=1
 *
 * SETUP rtsp://218.207.101.236:554/mobile/3/67A451E937422331/8jH5QPU5GWS07Ugn.sdp/trackID=1
 *  RTSP/1.0
 * Cseq: 3
 * Transport: RTP/AVP;UNICAST;client_port=16264-16265;mode=play
 *
 *
 * 返回内容：
 * RTSP/1.0 200 OK
 * Server: PVSS/1.4.8 (Build/20090111; Platform/Win32; Release/StarValley; )
 * Cseq: 3
 * Session: 15470472221769
 * Date: Mon, 03 Aug 2009 08:50:36 GMT
 * Expires: Mon, 03 Aug 2009 08:50:36 GMT
 * Transport: RTP/AVP;UNICAST;mode=play;client_port=16264-16265;server_port=20080-20081
 *
 *
 * PLAY rtsp://218.207.101.236:554/mobile/3/67A451E937422331/8jH5QPU5GWS07Ugn.sdp RTSP/1.0
 * Session: 15470472221769
 * Cseq: 4
 *
 *
 * 返回内容：
 * RTSP/1.0 200 OK
 * Server: PVSS/1.4.8 (Build/20090111; Platform/Win32; Release/StarValley; )
 * Cseq: 4
 * Session: 15470472221769
 * RTP-Info: url=rtsp://218.207.101.236:554/mobile/3/67A451E937422331/8jH5QPU5GWS07Ugn.sdp/trackID=1;seq=0;rtptime=0
 *
 *
 * PAUSE rtsp://218.207.101.236:554/mobile/3/67A451E937422331/8jH5QPU5GWS07Ugn.sdp/ RTSP/1.0
 * Cseq: 5
 * Session: 15470472221769
 *
 *
 * 返回内容：
 * RTSP/1.0 200 OK
 * Server: PVSS/1.4.8 (Build/20090111; Platform/Win32; Release/StarValley; )
 * Cseq: 5
 * Session: 15470472221769
 *
 *
 * TEARDOWN rtsp://218.207.101.236:554/mobile/3/67A451E937422331/8jH5QPU5GWS07Ugn.sdp/ RTSP/1.0
 * Cseq: 6
 * User-Agent: RealMedia Player HelixDNAClient/10.0.0.11279 (win32)
 * Session: 15470472221769
 *
 *
 * 返回内容：
 * RTSP/1.0 200 OK
 * Server: PVSS/1.4.8 (Build/20090111; Platform/Win32; Release/StarValley; )
 * Cseq: 6
 * Session: 15470472221769
 * Connection: Close
 *
 *
 * 端口关闭成功
 */
public class RTSPClient2 extends Thread implements IEvent {

    private static final String VERSION = " RTSP/1.0/r/n";
    private static final String RTSP_OK = "RTSP/1.0 200 OK";

    /** *//** 远程地址 */
    private final InetSocketAddress remoteAddress;

    /** *//** * 本地地址 */
    private final InetSocketAddress localAddress;

    /** *//** * 连接通道 */
    private SocketChannel socketChannel;

    /** *//** 发送缓冲区 */
    private final ByteBuffer sendBuf;

    /** *//** 接收缓冲区 */
    private final ByteBuffer receiveBuf;

    private static final int BUFFER_SIZE = 8192;

    /** *//** 端口选择器 */
    private Selector selector;

    private String address;

    private Status sysStatus;

    private String sessionid;

    /** *//** 线程是否结束的标志 */
    private AtomicBoolean shutdown;

    private int seq=1;

    private boolean isSended;

    private String trackInfo;


    private enum Status {
        init, options, describe, setup, play, pause, teardown
    }

    public RTSPClient2(InetSocketAddress remoteAddress,
                      InetSocketAddress localAddress, String address) {
        this.remoteAddress = remoteAddress;
        this.localAddress = localAddress;
        this.address = address;

        // 初始化缓冲区
        sendBuf = ByteBuffer.allocateDirect(BUFFER_SIZE);
        receiveBuf = ByteBuffer.allocateDirect(BUFFER_SIZE);
        if (selector == null) {
            // 创建新的Selector
            try {
                selector = Selector.open();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

        startup();
        sysStatus = Status.init;
        shutdown=new AtomicBoolean(false);
        isSended=false;
    }

    public void startup() {
        try {
            // 打开通道
            socketChannel = SocketChannel.open();
            // 绑定到本地端口
            socketChannel.socket().setSoTimeout(30000);
            socketChannel.configureBlocking(false);
            socketChannel.socket().bind(localAddress);
            if (socketChannel.connect(remoteAddress)) {
                System.out.println("开始建立连接:" + remoteAddress);
            }
            socketChannel.register(selector, SelectionKey.OP_CONNECT
                    | SelectionKey.OP_READ | SelectionKey.OP_WRITE, this);
            System.out.println("端口打开成功");

        } catch (final IOException e1) {
            e1.printStackTrace();
        }
    }

    public void send(byte[] out) {
        if (out == null || out.length < 1) {
            return;
        }
        synchronized (sendBuf) {
            sendBuf.clear();
            sendBuf.put(out);
            sendBuf.flip();
        }

        // 发送出去
        try {
            write();
            isSended=true;
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public void write() throws IOException {
        if (isConnected()) {
            try {
                socketChannel.write(sendBuf);
            } catch (final IOException e) {
            }
        } else {
            System.out.println("通道为空或者没有连接上");
        }
    }

    public byte[] recieve() {
        if (isConnected()) {
            try {
                int len = 0;
                int readBytes = 0;

                synchronized (receiveBuf) {
                    receiveBuf.clear();
                    try {
                        while ((len = socketChannel.read(receiveBuf)) > 0) {
                            readBytes += len;
                        }
                    } finally {
                        receiveBuf.flip();
                    }
                    if (readBytes > 0) {
                        final byte[] tmp = new byte[readBytes];
                        receiveBuf.get(tmp);
                        return tmp;
                    } else {
                        System.out.println("接收到数据为空,重新启动连接");
                        return null;
                    }
                }
            } catch (final IOException e) {
                System.out.println("接收消息错误:");
            }
        } else {
            System.out.println("端口没有连接");
        }
        return null;
    }

    public boolean isConnected() {
        return socketChannel != null && socketChannel.isConnected();
    }

    private void select() {
        int n = 0;
        try {
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

                // 处理事件
                final IEvent handler = (IEvent) sk.attachment();
                try {
                    if (sk.isConnectable()) {
                        handler.connect(sk);
                    } else if (sk.isReadable()) {
                        handler.read(sk);
                    } else {
                        // System.err.println("Ooops");
                    }
                } catch (final Exception e) {
                    handler.error(e);
                    sk.cancel();
                }
            }
        }
    }

    public void shutdown() {
        if (isConnected()) {
            try {
                socketChannel.close();
                System.out.println("端口关闭成功");
            } catch (final IOException e) {
                System.out.println("端口关闭错误:");
            } finally {
                socketChannel = null;
            }
        } else {
            System.out.println("通道为空或者没有连接");
        }
    }

    @Override
    public void run() {
        // 启动主循环流程
        while (!shutdown.get()) {
            try {
                if (isConnected()&&(!isSended)) {
                    switch (sysStatus) {
                        case init:
                            doOption();
                            break;
                        case options:
                            doDescribe();
                            break;
                        case describe:
                            doSetup();
                            break;
                        case setup:
                            if(sessionid==null&&sessionid.length()>0){
                                System.out.println("setup还没有正常返回");
                            }else{
                                doPlay();
                            }
                            break;
                        case play:
                            doPause();
                            break;

                        case pause:
                            doTeardown();
                            break;
                        default:
                            break;
                    }
                }
                // do select
                select();
                try {
                    Thread.sleep(1000);
                } catch (final Exception e) {
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        shutdown();
    }

    public void connect(SelectionKey key) throws IOException {
        if (isConnected()) {
            return;
        }
        // 完成SocketChannel的连接
        socketChannel.finishConnect();
        while (!socketChannel.isConnected()) {
            try {
                Thread.sleep(300);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
            socketChannel.finishConnect();
        }

    }

    public void error(Exception e) {
        e.printStackTrace();
    }

    public void read(SelectionKey key) throws IOException {
        // 接收消息
        final byte[] msg = recieve();
        if (msg != null) {
            handle(msg);
        } else {
            key.cancel();
        }
    }

    private void handle(byte[] msg) {
        String tmp = new String(msg);
        System.out.println("返回内容：");
        System.out.println(tmp);
        if (tmp.startsWith(RTSP_OK)) {
            switch (sysStatus) {
                case init:
                    sysStatus = Status.options;
                    break;
                case options:
                    sysStatus = Status.describe;
                    trackInfo=tmp.substring(tmp.indexOf("trackID"));
                    break;
                case describe:
                    sessionid = tmp.substring(tmp.indexOf("Session: ") + 9, tmp
                            .indexOf("Date:"));
                    if(sessionid!=null&&sessionid.length()>0){
                        sysStatus = Status.setup;
                    }
                    break;
                case setup:
                    sysStatus = Status.play;
                    break;
                case play:
                    sysStatus = Status.pause;
                    break;
                case pause:
                    sysStatus = Status.teardown;
                    shutdown.set(true);
                    break;
                case teardown:
                    sysStatus = Status.init;
                    break;
                default:
                    break;
            }
            isSended=false;
        } else {
            System.out.println("返回错误：" + tmp);
        }

    }

    private void doTeardown() {
        StringBuilder sb = new StringBuilder();
        sb.append("TEARDOWN ");
        sb.append(this.address);
        sb.append("/");
        sb.append(VERSION);
        sb.append("Cseq: ");
        sb.append(seq++);
        sb.append("/r/n");
        sb.append("User-Agent: RealMedia Player HelixDNAClient/10.0.0.11279 (win32)/r/n");
        sb.append("Session: ");
        sb.append(sessionid);
        sb.append("/r/n");
        send(sb.toString().getBytes());
        System.out.println(sb.toString());
    }

    private void doPlay() {
        StringBuilder sb = new StringBuilder();
        sb.append("PLAY ");
        sb.append(this.address);
        sb.append(VERSION);
        sb.append("Session: ");
        sb.append(sessionid);
        sb.append("Cseq: ");
        sb.append(seq++);
        sb.append("/r/n");
        sb.append("/r/n");
        System.out.println(sb.toString());
        send(sb.toString().getBytes());

    }

    private void doSetup() {
        StringBuilder sb = new StringBuilder();
        sb.append("SETUP ");
        sb.append(this.address);
        sb.append("/");
        sb.append(trackInfo);
        sb.append(VERSION);
        sb.append("Cseq: ");
        sb.append(seq++);
        sb.append("/r/n");
        sb.append("Transport: RTP/AVP;UNICAST;client_port=16264-16265;mode=play/r/n");
        sb.append("/r/n");
        System.out.println(sb.toString());
        send(sb.toString().getBytes());
    }

    private void doOption() {
        StringBuilder sb = new StringBuilder();
        sb.append("OPTIONS ");
        sb.append(this.address.substring(0, address.lastIndexOf("/")));
        sb.append(VERSION);
        sb.append("Cseq: ");
        sb.append(seq++);
        sb.append("/r/n");
        sb.append("/r/n");
        System.out.println(sb.toString());
        send(sb.toString().getBytes());
    }

    private void doDescribe() {
        StringBuilder sb = new StringBuilder();
        sb.append("DESCRIBE ");
        sb.append(this.address);
        sb.append(VERSION);
        sb.append("Cseq: ");
        sb.append(seq++);
        sb.append("/r/n");
        sb.append("/r/n");
        System.out.println(sb.toString());
        send(sb.toString().getBytes());
    }

    private void doPause() {
        StringBuilder sb = new StringBuilder();
        sb.append("PAUSE ");
        sb.append(this.address);
        sb.append("/");
        sb.append(VERSION);
        sb.append("Cseq: ");
        sb.append(seq++);
        sb.append("/r/n");
        sb.append("Session: ");
        sb.append(sessionid);
        sb.append("/r/n");
        send(sb.toString().getBytes());
        System.out.println(sb.toString());
    }

    public static void main(String[] args) {
        try {
            // RTSPClient(InetSocketAddress remoteAddress,
            // InetSocketAddress localAddress, String address)
            RTSPClient2 client = new RTSPClient2(
                    new InetSocketAddress("218.207.101.236", 554),
                    new InetSocketAddress("192.168.2.28", 0),
                    "rtsp://218.207.101.236:554/mobile/3/67A451E937422331/8jH5QPU5GWS07Ugn.sdp");
            client.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}