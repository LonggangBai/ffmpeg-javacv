package com.easy.rtsp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * ReceiveSocket：用来接收服务器发来的RTP和RTCP协议数据，只是简单地对UDP进行了包装而已
 */
public class ReceiveSocket implements Runnable{

    private DatagramSocket ds;
    public ReceiveSocket(String localAddress, int port){
        try {

            InetSocketAddress addr = new InetSocketAddress("192.168.31.106", port);
            ds = new DatagramSocket(addr);//监听16264端口
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        while(true){
            byte[] buf = new byte[20];
            DatagramPacket dp = new DatagramPacket(buf,buf.length);
            try
            {
                ds.receive(dp);
                String ip = dp.getAddress().getHostAddress();   //数据提取
                String data = new String(dp.getData(),0,dp.getLength());
                int port = dp.getPort();
                System.out.println(data+"."+port+".."+ip);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void send(byte[] buf, String ip, int rec_port) throws IOException {
        // TODO Auto-generated method stub
        DatagramPacket dp = new DatagramPacket(buf,buf.length,InetAddress.getByName(ip),rec_port);//10000为定义的端口
        ds.send(dp);
        //ds.close();
    }
}
