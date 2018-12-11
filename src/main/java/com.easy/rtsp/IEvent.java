package com.easy.rtsp;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/** *//**
 * IEvent.java 网络事件处理器，当Selector可以进行操作时，调用这个接口中的方法.
 * 2007-3-22 下午03:35:51
 * @author sycheng
 * @version 1.0
 */
public interface IEvent {
    /** *//**
     * 当channel得到connect事件时调用这个方法.
     * @param key
     * @throws IOException
     */
    void connect(SelectionKey key) throws IOException;

    /** *//**
     * 当channel可读时调用这个方法.
     * @param key
     * @throws IOException
     */
    void read(SelectionKey key) throws IOException;

    /** *//**
     * 当channel可写时调用这个方法.
     * @throws IOException
     */
    void write() throws IOException;

    /** *//**
     * 当channel发生错误时调用.
     * @param e
     */
    void error(Exception e);
}