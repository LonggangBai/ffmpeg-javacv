package com.easy.javacv;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;

import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.Timer;

import static org.bytedeco.javacpp.opencv_core.cvReleaseImage;


/**
 *
 * Use JavaCV/OpenCV to capture camera images
 *
 * There are two functions in this demo:
 * 1) show real-time camera images
 * 2) capture camera images by mouse-clicking anywhere in the JFrame,
 * the jpg file is saved in a hard-coded path.
 *
 * @author ljs
 * 2011-08-19
 *
 */
public class CameraCapture {
    public static String savedImageFile = "c:\\tmp\\my.jpg";

    //timer for image capture animation
    static class TimerAction implements ActionListener {
        private Graphics2D g;
        private CanvasFrame canvasFrame;
        private int width,height;

        private int delta=10;
        private int count = 0;

        private Timer timer;
        public void setTimer(Timer timer){
            this.timer = timer;
        }

        public TimerAction(CanvasFrame canvasFrame){
            this.g = (Graphics2D)canvasFrame.getCanvas().getGraphics();
            this.canvasFrame = canvasFrame;
            this.width = canvasFrame.getCanvas().getWidth();
            this.height = canvasFrame.getCanvas().getHeight();
        }
        public void actionPerformed(ActionEvent e) {
            int offset = delta*count;
            if(width-offset>=offset && height-offset >= offset) {
                g.drawRect(offset, offset, width-2*offset, height-2*offset);
                canvasFrame.repaint();
                count++;
            }else{
                //when animation is done, reset count and stop timer.
                timer.stop();
                count = 0;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        //open camera source
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
        grabber.start();

        //create a frame for real-time image display
        CanvasFrame canvasFrame = new CanvasFrame("Camera");
        OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
        Frame grabframe =grabber.grab();
        opencv_core.IplImage grabbedImage =null;
        if(grabframe!=null){
            System.out.println("取到第一帧");
            grabbedImage = converter.convert(grabframe);
        }else{
            System.out.println("没有取到第一帧");
        }


        int width = grabframe.imageWidth;
        int height = grabframe.imageHeight;
        canvasFrame.setCanvasSize(width, height);

        //onscreen buffer for image capture
        final BufferedImage bImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D bGraphics = bImage.createGraphics();

        //animation timer
        TimerAction timerAction = new TimerAction(canvasFrame);
        final Timer timer=new Timer(10, timerAction);
        timerAction.setTimer(timer);

        //click the frame to capture an image
        canvasFrame.getCanvas().addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e){
                timer.start(); //start animation
                try {
                    ImageIO.write(bImage, "jpg", new File(savedImageFile));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        //real-time image display
        while(canvasFrame.isVisible() && (grabframe=grabber.grab()) != null){
            if(!timer.isRunning()) { //when animation is on, pause real-time display
                canvasFrame.showImage(grabframe);
                //draw the onscreen image simutaneously
                bGraphics.drawImage(bImage ,null,0,0);
            }
        }

        //release resources
        cvReleaseImage(grabbedImage);
        grabber.stop();
        canvasFrame.dispose();
    }





}
