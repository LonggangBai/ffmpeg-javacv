package com.easy.rtsp;

import java.io.IOException;

/**
 * 只要在ReceiveSocket的run方法中打断点，你就会发现源源不断的数据向你发来，是不是感觉很爽，哈哈哈。
 */
public class Test {
    public static void main(String[] args){
        PlayerClient player = new PlayerClient();
        player.init();
        try
        {
            player.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
