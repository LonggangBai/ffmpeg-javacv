package com.easy.rtsp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

/**
 *    RTSPClient：使用RTSPProtocal中的静态方法获取字符创，拥有发送和接收数据的功能
 */
public class RTSPClient {

    private static final int BUFFER_SIZE = 8192;

    private String localIpAddress;
    private String remoteIpAddress;

    private int localPort;
    private int localPortOdd;
    private int localPortEven;
    private int remoteIPort;

    private int remotePortOdd;
    private int remotePortEven;

    private Map<Integer, ReceiveSocket> map = new HashMap<>();



    public int getRemotePortOdd() {
        return remotePortOdd;
    }
    public void setRemotePortOdd(int remotePortOdd) {
        this.remotePortOdd = remotePortOdd;
    }
    public int getRemotePortEven() {
        return remotePortEven;
    }
    public void setRemotePortEven(int remotePortEven) {
        this.remotePortEven = remotePortEven;
    }

    public void addSocket(Integer port, ReceiveSocket socket){
        map.put(port, socket);
    }

    private String rtspAddress;
    private Socket tcpSocket;

    private SocketChannel socketChannel;
    private Selector selector;

    public String getLocalIpAddress() {
        return localIpAddress;
    }
    public void setLocalIpAddress(String localIpAddress) {
        this.localIpAddress = localIpAddress;
    }
    public int getLocalPort() {
        return localPort;
    }
    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }
    public int getLocalPortOdd() {
        return localPortOdd;
    }
    public void setLocalPortOdd(int localPortOdd) {
        this.localPortOdd = localPortOdd;
    }
    public int getLocalPortEven() {
        return localPortEven;
    }
    public void setLocalPortEven(int localPortEven) {
        this.localPortEven = localPortEven;
    }
    public String getRtspAddress() {
        return rtspAddress;
    }
    public void setRtspAddress(String rtspAddress) {
        this.rtspAddress = rtspAddress;
    }
    public Socket getTcpSocket() {
        return tcpSocket;
    }
    public void setTcpSocket(Socket tcpSocket) {
        this.tcpSocket = tcpSocket;
    }


    public String getRemoteIpAddress() {
        return remoteIpAddress;
    }
    public void setRemoteIpAddress(String remoteIpAddress) {
        this.remoteIpAddress = remoteIpAddress;
    }
    public int getRemoteIPort() {
        return remoteIPort;
    }
    public void setRemoteIPort(int remoteIPort) {
        this.remoteIPort = remoteIPort;
    }

    public Selector getSelector() {
        return selector;
    }
    public void setSelector(Selector selector) {
        this.selector = selector;
    }
    //new InetSocketAddress(
    //remoteIp, 554),
    //new InetSocketAddress("192.168.31.106", 0),
    //"rtsp://218.204.223.237:554/live/1/66251FC11353191F/e7ooqwcfbqjoo80j.sdp"
    public void inital() throws IOException{
        socketChannel = SocketChannel.open();
        socketChannel.socket().setSoTimeout(30000);
        socketChannel.configureBlocking(false);

        InetSocketAddress localAddress = new InetSocketAddress(this.localIpAddress, localPort);
        InetSocketAddress remoteAddress=new InetSocketAddress(this.remoteIpAddress, 554);

        socketChannel.socket().bind(localAddress);
        if (socketChannel.connect(remoteAddress)) {
            System.out.println("开始建立连接:" + remoteAddress);
        }

        if (selector == null) {
            // 创建新的Selector
            try {
                selector = Selector.open();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        socketChannel.register(selector, SelectionKey.OP_CONNECT
                | SelectionKey.OP_READ | SelectionKey.OP_WRITE, this);
        System.out.println("端口打开成功");
    }

    public void write(byte[] out) throws IOException {
        if (out == null || out.length < 1) {
            return;
        }
        System.out.println(out.toString());
        ByteBuffer sendBuf = ByteBuffer.allocateDirect(BUFFER_SIZE);
        sendBuf.clear();
        sendBuf.put(out);
        sendBuf.flip();
        if (isConnected()) {
            try {
                socketChannel.write(sendBuf);
            } catch (final IOException e) {
            }
        } else {
            System.out.println("通道为空或者没有连接上");
        }
    }

    public boolean isConnected() {
        return socketChannel != null && socketChannel.isConnected();
    }

    public byte[] receive() {
        if (isConnected()) {
            try {
                int len = 0;
                int readBytes = 0;
                ByteBuffer receiveBuf = ByteBuffer.allocateDirect(BUFFER_SIZE);
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
    /*
     * 非常重要
     * */
    public void sendBeforePlay(){
        ReceiveSocket socketEven = map.get(this.localPortEven);
        ReceiveSocket socketOdd = map.get(this.localPortOdd);
        if(socketEven == null){
            socketEven = new ReceiveSocket(this.localIpAddress,this.localPortEven);
            map.put(this.localPortEven, socketEven);
        }
        if(socketOdd == null){
            socketEven = new ReceiveSocket(this.localIpAddress, this.localPortOdd);
            map.put(this.localPortOdd, socketOdd);
        }
        byte[] bytes = new byte[1];
        bytes[0]=0;
        try {
            socketEven.send(bytes, this.remoteIpAddress, this.remotePortEven);
            socketOdd.send(bytes, this.remoteIpAddress, this.remotePortOdd);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return;
    }

    public void reConnect(SelectionKey key) throws IOException {
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
}
