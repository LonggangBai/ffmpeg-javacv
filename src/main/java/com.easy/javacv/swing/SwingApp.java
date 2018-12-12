package com.easy.javacv.swing;


import java.awt.BorderLayout;
import java.awt.CheckboxMenuItem;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.ScrollPane;
import java.awt.Toolkit;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.WindowConstants;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;

import org.bytedeco.javacv.FrameGrabber.Exception;
import org.bytedeco.javacv.Java2DFrameConverter;

/**
 * Hello world!
 *
 */
public class SwingApp {

    public static void main(String[] args) throws Exception {
        SwingApp SwingApp = new SwingApp();
        SwingApp.play("e://tmp/1.flv");
    }

    Semaphore semaphore = new Semaphore(0); //视频播放控制锁
    final static int INTERVAL = 40; //抽图间隔
    boolean videoIsPlaying = true; //视频播放状态
    boolean videoIsOver = false; //视频完结状态
    boolean videoIsStop = false; //视频停播状态
    boolean frameIsFull = false; //视频全屏状态
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss yyyy-MM-dd");
    ComponentEventFacotry eventFactory = new ComponentEventFacotry();

    /************ 画面组件     **************/
    final static Map<String, JButton>  controllerButtons = new HashMap<>(); //控制台按钮集
    final static Map<String, GridBagConstraints>  gridBagConstraints = new HashMap<>(); //控制台按钮布局
    final static List<String> buttonNames = new ArrayList<>(); //按钮名称集合
    FFmpegFrameGrabber ffmpegFrameGrabber; //视频抓图对象
    CanvasFrame desktopPlayer;//桌面会话播放器
    JPanel playerController; //播放器控制板
    JLabel speedRateTitle; //播放倍数
    JLabel timeClock; //显示时间
    JSlider videoProcess; //视频进度条
    Frame nowFrame; //当前视频播放图片说
    SessionInformation sessionInformation; //会话信息内容
    SpeedMenuItem [] speedMenuItems; //播放速率比例选择
    ScaleMenuItem [] scaleMenuItems; //视频画面比例选择
    PopupMenu speedMenu;
    PopupMenu scaleMenu;


    /************ 视频播放相关参数     **************/
    int maxStamp; //视频总秒数
    int fflength; //视频总帧数
    int startStamp; //视频当前播放秒数
    double present; //视频进度条当前进度
    final long beginingRecordTime = 0 ; //视频开始记录的时间
    double speedRate = 1.0; //播放速率;

    static {
        buttonNames.add("play");
        buttonNames.add("stop");
        buttonNames.add("snapshot");
        buttonNames.add("session information");
        buttonNames.add("full screen model");
        buttonNames.add("scale");
        buttonNames.add("speed");

        controllerButtons.put("play", new JButton("play"));
        controllerButtons.put("stop", new JButton("stop"));
        controllerButtons.put("snapshot", new JButton("snapshot"));
        controllerButtons.put("session information", new JButton("session information"));
        controllerButtons.put("full screen model", new JButton("full screen model"));
        controllerButtons.put("scale", new JButton("scale"));
        controllerButtons.put("speed", new JButton("speed"));

        GridBagConstraints player  = new GridBagConstraints();
        player.insets.top = player.insets.bottom = 10;
        player.weightx = 0;
        player.insets.left = 5;
        gridBagConstraints.put("play", player);

        GridBagConstraints common  = new GridBagConstraints();
        common.insets.top = common.insets.bottom = 10;
        common.weightx = 0;
        common.insets.left = 5;
        gridBagConstraints.put("stop", common);
        gridBagConstraints.put("snapshot", common);
        gridBagConstraints.put("session information", common);
        gridBagConstraints.put("full screen model", common);
        gridBagConstraints.put("scale", common);
        gridBagConstraints.put("speed", common);

        GridBagConstraints speedRate  = new GridBagConstraints();
        speedRate.insets.top = speedRate.insets.bottom = 10;
        speedRate.weightx = 0;
        speedRate.insets.left = 10;
        gridBagConstraints.put("speedRate", speedRate);

        GridBagConstraints timeSlider  = new GridBagConstraints();
        timeSlider.insets.top = timeSlider.insets.bottom = 10;
        timeSlider.weightx = 1;
        timeSlider.insets.left = 20;
        timeSlider.gridx = GridBagConstraints.RELATIVE;
        timeSlider.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.put("timeSlider", timeSlider);

        GridBagConstraints timeLabel  = new GridBagConstraints();
        timeLabel.insets.top = timeLabel.insets.bottom = 10;
        timeLabel.weightx = 0;
        timeLabel.insets.left = 10;
        timeLabel.insets.right = 5;
        timeLabel.gridx = GridBagConstraints.RELATIVE;

        gridBagConstraints.put("timeLabel", timeLabel);
    }

    /**
     * 构造方法 初始化播放器内容
     */
    public SwingApp() {
        playerController = new JPanel();
        GridBagLayout gridbag = new GridBagLayout();
        playerController.setLayout(gridbag);
        List<JButton> controllerButtions = new ArrayList<>();
        for (String buttonName : buttonNames) {
            JButton button = controllerButtons.get(buttonName);
            button.setToolTipText(buttonName);
            button.setPreferredSize(new Dimension(22, 22));
            eventFactory.addComponentEventListener(button, ComponentEventFacotry.COMPONENT_BUTTON, this);
            controllerButtions.add(button);
            if (gridBagConstraints.containsKey(buttonName)) {
                playerController.add(button, gridBagConstraints.get(buttonName));
            }
        }

        speedRateTitle = new JLabel("x01");
        speedRateTitle.setForeground(Color.blue);
        playerController.add(speedRateTitle, gridBagConstraints.get("speedRate"));

        videoProcess = new JSlider(0, 100 ,0);
        videoProcess.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        playerController.add(videoProcess, gridBagConstraints.get("timeSlider"));
        eventFactory.addComponentEventListener(videoProcess, ComponentEventFacotry.COMPONENT_SLIDER, this);

        timeClock = new JLabel(getTimeString((int) (beginingRecordTime/1000)));
        timeClock.setForeground(Color.blue);
        playerController.add(timeClock, gridBagConstraints.get("timeLabel"));

        desktopPlayer = new CanvasFrame("javaCV Player", 1);
        desktopPlayer.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        desktopPlayer.add(playerController, BorderLayout.SOUTH);
        desktopPlayer.setLocation(0, 0);

        GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(desktopPlayer);
        sessionInformation = new SessionInformation("session inforamtion", desktopPlayer);
        initSpeedMenu();
        initScaleMenu();
        desktopPlayer.setVisible(true);
        ScrollPane a = new ScrollPane();
    }

    /**
     * 初始化视频播放速率选择菜单
     */
    void initSpeedMenu() {
        speedMenuItems = new SpeedMenuItem []{
                new SpeedMenuItem("x64", 64, false),
                new SpeedMenuItem("x32", 32, false),
                new SpeedMenuItem("x16", 16, false),
                new SpeedMenuItem("x8.0", 8, false),
                new SpeedMenuItem("x4.0", 4, false),
                new SpeedMenuItem("x2.0", 2, false),
                new SpeedMenuItem("x1.0", 1, true),
                new SpeedMenuItem("x0.5", 0.5, false),
                new SpeedMenuItem("x0.25", 0.25 ,false),
        };
        speedMenu = new PopupMenu();
        desktopPlayer.add(speedMenu);
        for (SpeedMenuItem speedMenuItem : speedMenuItems) {
            eventFactory.addMenuEventListener(speedMenuItem, ComponentEventFacotry.MENU_ITEM_SPEED, this);
            speedMenu.add(speedMenuItem);
        }
    }

    /**
     * 初始化视频画面比例选择菜单
     */
    void initScaleMenu() {
        scaleMenuItems = new ScaleMenuItem []{
                new ScaleMenuItem("100%", 1.0, ScaleMenuItem.TYPE_SELECT),
                new ScaleMenuItem("85%", 0.85, ScaleMenuItem.TYPE_SELECT),
                new ScaleMenuItem("80%", 0.8,ScaleMenuItem.TYPE_SELECT),
                new ScaleMenuItem("75%", 0.7, ScaleMenuItem.TYPE_SELECT),
                new ScaleMenuItem("50%", 0.5, ScaleMenuItem.TYPE_SELECT),
                new ScaleMenuItem("25%", 0.25, ScaleMenuItem.TYPE_SELECT),
                new ScaleMenuItem("AutoFit", 0.0, ScaleMenuItem.TYPE_AUTO),
                new ScaleMenuItem("More...", 0.0,ScaleMenuItem.TYPE_INPUT),
        };
        scaleMenu = new PopupMenu();
        desktopPlayer.add(scaleMenu);
        for (ScaleMenuItem scaleMenuItem : scaleMenuItems) {
            eventFactory.addMenuEventListener(scaleMenuItem, ComponentEventFacotry.MENU_ITEM_SCALE, this);
            scaleMenu.add(scaleMenuItem);
        }
    }

    /**
     * 视频播放器开始播放
     * @param filePath 视频文件路径
     */
    public void play(String filePath) {
        try {
            ffmpegFrameGrabber = FFmpegFrameGrabber.createDefault(filePath);
            ffmpegFrameGrabber.start();
            fflength = ffmpegFrameGrabber.getLengthInFrames();
            maxStamp = (int) (ffmpegFrameGrabber.getLengthInTime()/1000000);
            int count = 0;
            while (true) {
                if (!videoIsPlaying || videoIsOver) {
                    semaphore.acquire();
                }
                if (videoIsPlaying) {
                    nowFrame = ffmpegFrameGrabber.grabImage();
                    startStamp = (int) (ffmpegFrameGrabber.getTimestamp() * 1.0/1000000);
                    present = (startStamp * 1.0 / maxStamp) * 100;
                    if (nowFrame == null) {
                        System.out.println("!!! Failed cvQueryFrame");
                        controllerButtons.get("play").setText("play");
                        controllerButtons.get("play").setToolTipText("play");
                        videoIsOver = true;
                        continue;
                    }
                    timeClock.setText(getTimeString(startStamp));
                    videoProcess.setValue((int) present);
                    count ++;
                    if (count % speedRate != 0) {
                        continue;
                    }
                    desktopPlayer.showImage(nowFrame);
                    Thread.sleep(INTERVAL);
                } else {

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 给滚动条添加监听事件
     * @param slider
     */
    public void addSliderEventListener(JSlider slider) {
        //鼠标点击时间监听器
        slider.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                double x = e.getX() * 1.0 / videoProcess.getWidth();
                x *= maxStamp;
                startStamp = (int)(x + 0.5);
                try {
                    //该时间戳是以微妙计算的
                    ffmpegFrameGrabber.setTimestamp(startStamp * 1000000);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });

        //鼠标移动监听器
        slider.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                double x = e.getX() * 1.0 / videoProcess.getWidth();
                x *= maxStamp;
                int time = (int)(x + 0.5);
                videoProcess.setToolTipText(getTimeString((int) (time + beginingRecordTime)));
            }
        });
    }

    /**
     * 返回格式化时间
     * @param second 秒数
     * @return 格式化的时间字符串
     */
    public String getTimeString(int second) {
        return sdf.format(new Date(beginingRecordTime + second * 1000));
    }
}

/**
 * 给对应的按钮添加对应的处理时间
 * @author Felix
 *
 */
class ComponentEventFacotry {
    /** 鼠标类型  1**/
    final static int COMPONENT_BUTTON = 1;

    /** 滑动条 2 **/
    final static int COMPONENT_SLIDER = 2;

    /** 菜单项 3 **/
    final static int MENU_ITEM_SPEED = 3;

    /** 菜单项 3 **/
    final static int MENU_ITEM_SCALE = 4;

    /** 播放器容器 **/
    SwingApp containner;

    /**
     * 给对应组件添加监听事件
     * @param component
     * @param componentType
     * @param containner
     */
    public void addComponentEventListener(Component component, int componentType, SwingApp containner) {
        this.containner = containner;
        if (containner == null) {
            return;
        }
        switch (componentType) {
            case COMPONENT_BUTTON:
                addButtonEventListener((JButton) component);
                break;
            case COMPONENT_SLIDER:
                addSliderEventListener((JSlider) component);
                break;
            default:
                break;
        }
    }

    /**
     * 给菜单项添加监听事件
     * @param item
     * @param componentType
     * @param containner
     */
    public void addMenuEventListener(MenuItem item, int componentType, SwingApp containner) {
        this.containner = containner;
        if (containner == null) {
            return;
        }
        switch (componentType) {
            case MENU_ITEM_SPEED:
                addSpeedItemEventListener((SpeedMenuItem)item);
                break;
            case MENU_ITEM_SCALE:
                addScaleItemEventListener((ScaleMenuItem)item);
                break;
            default:
                break;
        }
    }

    /**
     * 给控制台按钮添加监听事件
     * @param button 控制台按钮
     */
    private void addButtonEventListener(JButton button) {
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JButton button  = (JButton) e.getSource();
                String buttonName = button.getText();
                //播放
                if (buttonName.equals("play")) {
                    System.out.println("play");
                    try {
                        if (containner.videoIsOver) {
                            containner.ffmpegFrameGrabber.restart();
                            containner.videoIsOver = false;
                        }
                        button.setText("pause");
                        button.setToolTipText("pause");
                        if (containner.videoIsStop) {
                            containner.ffmpegFrameGrabber.start();
                        }
                        containner.videoIsPlaying = true;
                        containner.videoIsStop = false;
                        containner.semaphore.release();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
                //暂时播放
                if (buttonName.equals("pause")) {
                    System.out.println("pause");
                    button.setText("play");
                    button.setToolTipText("play");
                    containner.videoIsPlaying = false;
                    containner.videoIsStop = false;
                }
                //停止播放
                if (buttonName.equals("stop")) {
                    System.out.println("stop");
                    try {
                        containner.controllerButtons.get("play").setText("play");
                        containner.controllerButtons.get("play").setToolTipText("play");
                        containner.ffmpegFrameGrabber.stop();
                        containner.timeClock.setText(containner.getTimeString((int) (containner.beginingRecordTime/1000)));
                        containner.videoProcess.setValue(0);
                        containner.videoIsPlaying = false;
                        containner.videoIsStop = true;
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
                //全屏
                if (buttonName.equals("full screen model")) {
                    System.out.println("full screen model");
                    containner.desktopPlayer.dispose();
                    containner.desktopPlayer.setUndecorated(true);
                    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
                    containner.desktopPlayer.setSize(d.width, d.height);
                    containner.desktopPlayer.setVisible(true);
                    containner.frameIsFull = true;
                }
                //截屏
                if (buttonName.equals("snapshot")) {
                    System.out.println("snapshot");
                    snapshot();
                }
                //播放速率
                if (buttonName.equals("speed")) {
                    System.out.println("speed");
                    showMenu(containner.speedMenu, containner.desktopPlayer, button.getX(), button.getY());
                }
                //会话信息
                if (buttonName.equals("session information")) {
                    System.out.println("session information");
                    containner.sessionInformation.adjustLocation(containner.desktopPlayer);
                    containner.sessionInformation.setVisible(true);
                }
                //视频画面比例缩放
                if (buttonName.equals("scale")) {
                    System.out.println("scale");
                    showMenu(containner.scaleMenu, containner.desktopPlayer, button.getX(), button.getY());
                }
            }
        });

        button.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                if (keyCode == KeyEvent.VK_ESCAPE) {
                    System.out.println("escape");
                    //全屏退出事件
                    if (containner.frameIsFull) {
                        containner.desktopPlayer.dispose();
                        containner.desktopPlayer.setUndecorated(false);
                        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
                        containner.desktopPlayer.setSize(d.width, d.height);
                        containner.desktopPlayer.setVisible(true);
                        containner.frameIsFull = false;
                    }
                }
            };
        } );
    }

    /**
     * 给速率菜单项添加监听事件
     * @param speedMenuItem
     */
    private void addSpeedItemEventListener(SpeedMenuItem speedMenuItem) {
        speedMenuItem.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                SpeedMenuItem speedItem = (SpeedMenuItem) e.getSource();
                containner.speedRate = speedItem.getRateValue();
                System.out.println("speedRate :" + containner.speedRate);
                for (SpeedMenuItem speedMenuItem : containner.speedMenuItems) {
                    speedMenuItem.setState(false);
                }
                speedItem.setState(true);
            }
        });
    }

    /**
     * 给画面比例菜单项添加监听事件
     * @param scaleMenuItem
     */
    private void addScaleItemEventListener(ScaleMenuItem scaleMenuItem) {
        scaleMenuItem.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                ScaleMenuItem scaleItem = (ScaleMenuItem) e.getSource();
                System.out.println("scale" + scaleItem.getScale());
                containner.desktopPlayer.setCanvasScale(1 * scaleItem.getScale());
                for (ScaleMenuItem scaleMenuItem : containner.scaleMenuItems) {
                    scaleMenuItem.setState(false);
                }
                scaleItem.setState(true);
            }
        });
    }

    /**
     * 给滑动条添加监听事件
     * @param slider
     */
    private void addSliderEventListener(JSlider slider) {
        //鼠标点击时间监听器
        slider.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                double x = e.getX() * 1.0 / containner.videoProcess.getWidth();
                x *= containner.maxStamp;
                containner.startStamp = (int)(x + 0.5);
                try {
                    //该时间戳是以微妙计算的
                    containner.ffmpegFrameGrabber.setTimestamp(containner.startStamp * 1000000);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }

        });

        //鼠标移动监听器
        slider.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                double x = e.getX() * 1.0 / containner.videoProcess.getWidth();
                x *= containner.maxStamp;
                int time = (int)(x + 0.5);
                containner.videoProcess.setToolTipText(containner.getTimeString((int) (time + containner.beginingRecordTime)));
            }
        });
    }

    /**
     * 菜单显示
     * @param menu 想显示的菜单
     * @param owner 菜单容器
     * @param x 被点击按钮X
     * @param y 被点击按钮Y
     */
    private void showMenu(final PopupMenu menu, final Component owner, final int x , final int y) {
        Point pt1 = owner.getLocationOnScreen();
        Point pt2 = containner.playerController.getLocationOnScreen();
        final int delta_x = pt2.x - pt1.x;
        final int delta_y = pt2.y - pt1.y;
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(100);
                }  catch (InterruptedException e) {
                    e.printStackTrace();
                }
                menu.show(owner, x + delta_x,y + delta_y);
            };
        }.start();
    }

    /**
     * 截屏方法
     */
    private void snapshot() {
        FileDialog dlg = new FileDialog(new JFrame(),"save picture", FileDialog.SAVE);
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.endsWith("png")) {
                    return true;
                }
                return false;
            }
        };
        dlg.setFilenameFilter(filter);
        dlg.setFile("*.png");
        dlg.setVisible(true);
        String file = dlg.getFile();
        if (file != null) {
            file = dlg.getDirectory() + file;
            if(!file.endsWith(".png")) {
                file += ".png";
            }
            Java2DFrameConverter converter = new Java2DFrameConverter();
            BufferedImage bufferedImage = converter.getBufferedImage(containner.nowFrame);
            if (bufferedImage != null) {
                OutputStream os = null;
                try {
                    os = new FileOutputStream(file);
                    ImageIO.write(bufferedImage, "png", os);
                    os.flush();
                    os.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                } finally {
                    try {
                        if (os != null) {
                            os.close();
                        }
                    } catch (IOException e1) {
                        System.out.println("close imgIO failed");
                    }
                }
            }
        }
    }
}

/**
 * 视频画面比例菜单项
 * @author Felix
 *
 */
class ScaleMenuItem extends CheckboxMenuItem {
    final static int TYPE_INPUT = 0;
    final static int TYPE_SELECT = 1;
    final static int TYPE_AUTO = 2;
    private final double scale;
    private final int type;
    public ScaleMenuItem(String label, double scale, int scaleType) {
        super(label);
        this.scale = scale;
        this.type = scaleType;
    }
    public double getScale() {
        return scale;
    }
    public int getType() {
        return type;
    }
}

/**
 * 播放速率菜单项
 * @author Felix
 *
 */
class SpeedMenuItem extends CheckboxMenuItem {
    /** 播放速率倍数*/
    private final double rateValue;
    public SpeedMenuItem(String label,double rateValue, boolean selected) {
        super(label);
        this.rateValue = rateValue;
        this.setState(selected);
    }
    public double getRateValue() {
        return rateValue;
    }
}

/**
 * 会话信息弹出框
 * @author Felix
 *
 */
class SessionInformation extends JFrame{
    JButton okButton;
    CanvasFrame desktopPlayer;

    public SessionInformation(String title, CanvasFrame desktopPlayer) {
        super(title);
        this.desktopPlayer = desktopPlayer;
        setAlwaysOnTop(true);

        GridBagLayout gridbag = new GridBagLayout();
        setLayout(gridbag);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;

        //account
        gbc.insets = new Insets(5, 0, 1, 0);
        gbc.gridwidth = 1;
        JLabel accountTitle = new JLabel(" account : ");
        add(accountTitle, gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        JLabel account = new JLabel(" account from session");
        add(account, gbc);

        //from
        gbc.gridwidth = 1;
        gbc.insets = new Insets(0, 0, 1, 0);
        JLabel fromTitle = new JLabel("from :");
        add(fromTitle, gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        JLabel to = new JLabel(String.format("%s (%s", "targetName","targetIp"));
        add(to, gbc);

        //start
        gbc.gridwidth = 1;
        JLabel startTitle = new JLabel(" start:");
        add(startTitle, gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        JLabel start = new JLabel("start time from session");
        add(start, gbc);

        //protocol
        gbc.gridwidth = 1;
        JLabel protocolTitle = new JLabel("protocol :");
        add(protocolTitle,gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        JLabel protocol = new JLabel("protocol from session");
        add(protocol, gbc);

        //OK
        gbc.insets = new Insets(4, 0, 10, 0);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.CENTER;
        okButton = new JButton("   OK   ");
        add(okButton, gbc);
        okButton.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                System.out.println("okbutton key");
                int keyCode = e.getKeyCode();
                if(keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_I) {
                    close();
                }
            }
        });
        okButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JButton button  = (JButton) e.getSource();
                String buttonName = button.getText();
                if (buttonName.equals("   OK   ")) {
                    close();
                }
            }
        });

        pack();
        setResizable(false);
        setAlwaysOnTop(true);
        okButton.requestFocus();
    }

    public void adjustLocation(CanvasFrame desktopPlayer) {
        Point location = this.getLocation();
        Point canvasLocation = desktopPlayer.getLocationOnScreen();
        int clientWidth = desktopPlayer.getWidth();
        int clientHeight = desktopPlayer.getHeight();
        if (location.x + this.getWidth() > canvasLocation.x + clientWidth) {
            location.x = canvasLocation.x + clientWidth - this.getWidth();
        }
        if (location.y + this.getHeight() > canvasLocation.y + clientHeight) {
            location.y = canvasLocation.y  + clientHeight - this.getHeight();
        }
        if (location.x < canvasLocation.x) {
            location.x  = canvasLocation.x;
        }
        if(location.y < canvasLocation.y) {
            location.y = canvasLocation.y;
        }
        Point oldLocation = this.getLocation();
        if(oldLocation.x != location.x || oldLocation.y != location.y) {
            this.setLocation(location);;
        }
    }
    public void close() {
        setVisible(false);
    }
}
