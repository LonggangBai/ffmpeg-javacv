//package com.easy.javacv.Study;
//
///**
// * 花了三天的时间，研究视频直播的采集、编码、推流，历尽艰辛，删减参考代码，多次实验，详细检查，终于自己另外写了一个demo。有一些地方还看不懂，我基本能看懂的地方做了详尽的注释。
// *
// * 示例代码来自于github：
// *
// * https://github.com/runner365/android_rtmppush_sdk
// * 其中包括sdk。
// *
// * 我分步骤，先实现采集，再实现视频推送，最后加上音频推送比较顺利。方法是把源码中自己感觉多余的代码删除掉，把繁琐的代码简化掉，给源码写注释。反复试验。
// *
// * 然后自己建项目，写代码，多次运行，比较，历时三天。
// *
// * 终于把一个视频直播的框架理清楚。
// *
// * 思路大概是：
// *
// * 一、首先要有一个直播服务器，负责拉流和分发。我找到一个现成的nginx
// *
// * https://github.com/illuspas/nginx-rtmp-win32
// * 下载解压，直接运行即可，端口是1935
// *
// * 二、在手机端采集、编码、推流
// *
// * 1.初始化surfaceView和surfaceHolder
// *
// * 2.初始化camera，一般放在holder的回调中，因为需要预览
// *
// * 3.在预览回调中处理捕获的每一帧的数据，主要是格式
// *
// * 4.初始化录音设备
// *
// * 5.创建两个线程，负责处理视频和音频的推流，将处理好的最终数据放置到rtmp会话管理器中
// *
// * 6.开始推流，启动编码，启动线程
// */
//public class MainActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback {
//    private String rtmpPushUrl = "rtmp://192.168.1.102:1935/live/stream";//推流路径
//    /**
//     * surface
//     */
//    private SurfaceView surfaceView = null;//预览界面
//    private SurfaceHolder holder;
//    /**
//     * camera
//     */
//    private Camera camera = null;//摄像头
//    private SWVideoEncoder swVideoEncoder = null;//编码器
//    private int WIDTH = 480, HEIGHT = 640;//设置宽和高
//    private int FRAMERATE = 20, BITRATE = 800 * 1000;//设置帧频和比特率
//    private int codecType = android.graphics.ImageFormat.NV21;//编码类型
//    private int degress = 0;//镜头旋转角度
//    private boolean isFront = true;//是否前置镜头
//
//    /**
//     * audioRecord
//     */
//    private AudioRecord audioRecord = null;//录音机
//    private int recordBufferSize = 0;//录音缓冲区大小
//    private byte[] recordBuffer = null;//录音缓冲区
//    private FdkAacEncode fdkAacEncode = null;//编码器
//    private int aacInit = 0;
//    private int SAMPLE_RATE = 22050;
//    private int CHANNEL_NUMBER = 2;//声道
//
//    /**
//     * push
//     */
//    private byte[] yuvEdit = new byte[WIDTH * HEIGHT * 3 / 2];
//    private RtmpSessionManager rtmpSessionManager;//会话管理器
//    private Queue<byte[]> queue = new LinkedList<>();
//    private Lock queueLock = new ReentrantLock();
//    private boolean isPushing = false;//是否正在推送
//
//    private Thread h264Thread = null;//处理视频的线程
//    private Thread aacThread = null;//处理音频的线程
//    // region 处理h264的回调
//    private Runnable h264Runnable = new Runnable() {
//        @Override
//        public void run() {
//            while (!h264Thread.interrupted() && isPushing) {//todo ??
//                int size = queue.size();
//                if (size > 0) {
//                    queueLock.lock();
//                    byte[] yuvData = queue.poll();
//                    queueLock.unlock();
//
//                    if (yuvData == null) {
//                        continue;
//                    }
//
//                    if (isFront) {
//                        yuvEdit = swVideoEncoder.YUV420pRotate270(yuvData, HEIGHT, WIDTH);
//                    } else {
//                        yuvEdit = swVideoEncoder.YUV420pRotate90(yuvData, HEIGHT, WIDTH);
//                    }
//
//                    byte[] h264Data = swVideoEncoder.EncoderH264(yuvEdit);
//
//                    if (h264Data != null) {
//                        rtmpSessionManager.InsertVideoData(h264Data);
//                    }
//                }
//            }
//            queue.clear();
//        }
//    };
//    // endregion
//    // region 处理aac的回调
//    private Runnable aacRunnable = new Runnable() {
//        @Override
//        public void run() {
//            while (!aacThread.interrupted() && isPushing) {
//                int len = audioRecord.read(recordBuffer, 0, recordBuffer.length);
//                if (len != AudioRecord.ERROR_BAD_VALUE && len != 0) {
//                    if (aacInit != 0) {
//                        byte[] aacBuffer = fdkAacEncode.FdkAacEncode(aacInit, recordBuffer);
//                        if (aacBuffer != null) {
//                            rtmpSessionManager.InsertAudioData(aacBuffer);
//                        }
//                    }
//                }
//            }
//        }
//    };
//    // endregion
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        //设置全屏
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        //设置常亮
//        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        //初始化
//        init();
//    }
//
//    /**
//     * 开始初始化
//     */
//    private void init() {
//        initSurface();//初始化surface，在回调方法中初始化camera
//        initAudioRecord();//初始化录音器
//        startPush();//开始推送
//    }
//
//    /**
//     * 初始化SurfaceView
//     */
//    private void initSurface() {
//        int screenWidth = ScreenUtils.getScreenWidth(this);//屏幕宽
//        int screenHeight = ScreenUtils.getScreenHeight(this);//屏幕高
//        int iNewWidth = (int) (screenHeight * 3.0 / 4.0);//将宽重新计算，目的为适应屏幕宽度，比例为4:3
//        //?
//        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
//                RelativeLayout.LayoutParams.MATCH_PARENT);
//        int iPos = screenWidth - iNewWidth;
//        layoutParams.setMargins(iPos, 0, 0, 0);
//
//        //初始化surfaceView
//        surfaceView = (SurfaceView) this.findViewById(R.id.sv_preview);
//        holder = surfaceView.getHolder();
//        holder.setFixedSize(HEIGHT, WIDTH);//这里两个参数是反的
//        holder.setKeepScreenOn(true);//设置屏幕常亮
//        holder.addCallback(this);//添加回调
//
//        surfaceView.setLayoutParams(layoutParams);
//    }
//
//    /**
//     * 初始化摄像头
//     */
//    private void initCamera() {
//        Camera.Parameters parameters = camera.getParameters();//获取摄像头的参数
//
//        //设置格式
//        List<Integer> previewFormats = parameters.getSupportedPreviewFormats();//获取摄像头支持的所有格式
//        int flagNv21 = 0, flagYv12 = 0;
//        for (int f : previewFormats) {
//            if (ImageFormat.YV12 == f) {
//                flagYv12 = f;
//            }
//            if (ImageFormat.NV21 == f) {
//                flagNv21 = f;
//            }
//        }
//
//        if (flagNv21 != 0) {
//            codecType = flagNv21;
//        } else if (flagYv12 != 0) {
//            codecType = flagYv12;
//        }
//
//        parameters.setPreviewSize(HEIGHT, WIDTH);//这里两个参数是反的，否则像素比例失真
//        parameters.setPreviewFormat(codecType);//设置预览格式
//        parameters.setPreviewFrameRate(FRAMERATE);
//        degress = getDisplayOritation(getDispalyRotation(), 0);
//        parameters.setRotation(degress);
//
//        camera.setParameters(parameters);//重设参数
//        camera.setDisplayOrientation(degress);
//
//        camera.setPreviewCallback(this);//设置预览回调，很重要
//        try {
//            camera.setPreviewDisplay(holder);//设置预览设备
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        camera.cancelAutoFocus();//这一句可以自动对焦
//        camera.startPreview();//开始预览
//    }
//
//    /**
//     * 初始化录音设备
//     */
//    private void initAudioRecord() {
//        //录音缓冲区大小
//        recordBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
//                AudioFormat.CHANNEL_CONFIGURATION_STEREO,
//                AudioFormat.ENCODING_PCM_16BIT);
//        //初始化录音设备
//        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
//                SAMPLE_RATE,
//                AudioFormat.CHANNEL_CONFIGURATION_STEREO,
//                AudioFormat.ENCODING_PCM_16BIT,
//                recordBufferSize);
//        //录音缓冲区
//        recordBuffer = new byte[recordBufferSize];
//        //实例化音频编码器
//        fdkAacEncode = new FdkAacEncode();
//        aacInit = fdkAacEncode.FdkAacInit(SAMPLE_RATE, CHANNEL_NUMBER);
//    }
//
//
//    /**
//     * 开始推送
//     */
//    public void startPush() {
//        //实例化会话管理器
//        rtmpSessionManager = new RtmpSessionManager();
//        rtmpSessionManager.Start(rtmpPushUrl);
//
//        //实例化视频编码器
//        swVideoEncoder = new SWVideoEncoder(WIDTH, HEIGHT, FRAMERATE, BITRATE);
//        swVideoEncoder.start(codecType);
//
//        isPushing = true;
//
//        //启动视频编码线程
//        h264Thread = new Thread(h264Runnable);
//        h264Thread.setPriority(Thread.MAX_PRIORITY);
//        h264Thread.start();
//        //启动音频编码线程
//        audioRecord.startRecording();
//        aacThread = new Thread(aacRunnable);
//        aacThread.setPriority(Thread.MAX_PRIORITY);
//        aacThread.start();
//    }
//
//    @Override
//    public void surfaceCreated(SurfaceHolder holder) {
//        degress = getDisplayOritation(getDispalyRotation(), 0);
//        //如果摄像头还不存在，则打开前置摄像头
//        if (camera == null) {
//            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
//        }
//        initCamera();//初始化
//    }
//
//    @Override
//    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//        camera.autoFocus(new Camera.AutoFocusCallback() {
//            @Override
//            public void onAutoFocus(boolean success, Camera camera) {
//                if (success) {
//                    initCamera();
//                }
//            }
//        });
//    }
//
//    @Override
//    public void surfaceDestroyed(SurfaceHolder holder) {
//
//    }
//
//    @Override
//    public void onPreviewFrame(byte[] data, Camera camera) {
//        if (!isPushing) {
//            return;
//        }
//        //格式转换
//        byte[] yuv420Data = null;
//        if (codecType == ImageFormat.YV12) {
//            yuv420Data = new byte[data.length];
//            swVideoEncoder.swapYV12toI420_Ex(data, yuv420Data, HEIGHT, WIDTH);
//        } else if (codecType == ImageFormat.NV21) {
//            yuv420Data = swVideoEncoder.swapNV21toI420(data, HEIGHT, WIDTH);
//        }
//
//        if (yuv420Data == null) {
//            return;
//        }
//        //清除帧数据
//        queueLock.lock();
//        if (queue.size() > 0) {
//            queue.clear();
//        }
//        //加入新的帧数据
//        queue.offer(yuv420Data);
//        queueLock.unlock();
//
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        //加上状态判断，否则会空指针异常
//        if (isPushing) {
//            stopPush();
//        }
//    }
//
//    /**
//     * 停止推流
//     */
//    private void stopPush() {
//        isPushing = false;//修改状态
//
//        h264Thread.interrupt();//中断线程
//        aacThread.interrupt();
//
//        audioRecord.stop();//停止编码
//        swVideoEncoder.stop();
//
//        rtmpSessionManager.Stop();//终止会话
//
//        queueLock.lock();//清除帧数据
//        queue.clear();
//        queueLock.unlock();
//    }
//
//    /**
//     * 获取摄像头旋转角度
//     *
//     * @param degrees
//     * @param cameraId
//     * @return
//     */
//    private int getDisplayOritation(int degrees, int cameraId) {
//        Camera.CameraInfo info = new Camera.CameraInfo();
//        Camera.getCameraInfo(cameraId, info);
//        int result = 0;
//        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//            result = (info.orientation + degrees) % 360;
//            result = (360 - result) % 360;
//        } else {
//            result = (info.orientation - degrees + 360) % 360;
//        }
//        return result;
//    }
//
//    /**
//     * 获取屏幕的旋转角度
//     *
//     * @return
//     */
//    private int getDispalyRotation() {
//        int i = getWindowManager().getDefaultDisplay().getRotation();
//        switch (i) {
//            case Surface.ROTATION_0:
//                return 0;
//            case Surface.ROTATION_90:
//                return 90;
//            case Surface.ROTATION_180:
//                return 180;
//            case Surface.ROTATION_270:
//                return 270;
//        }
//        return 0;
//    }
//}