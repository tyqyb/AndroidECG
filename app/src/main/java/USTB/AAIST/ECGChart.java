package USTB.AAIST;
/**
 *   APP打包后修改SpeechUtility.createUtility中的APPID
 * **/
import static USTB.AAIST.utils.DataFormatUtil.arrayToHex;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import USTB.AAIST.utils.DataFormatUtil;
import USTB.AAIST.utils.FileUtils;
import USTB.AAIST.utils.SoundTipUtil;//语音播报
import USTB.AAIST.view.Wave;
import android.view.WindowManager;
import USTB.AAIST.view.RPView;
import java.util.Timer;
import java.util.TimerTask;

/**
 * //设置开启之后，才能在onCharacteristicRead()这个方法中收到数据。的if判断中进入了备用方法测试,因此没有接收到数据
 * 参考BLE开发文档修改
 * 20240722为添加按钮监视，将public class ECGChart extends AppCompatActivity{} 改为public class ECGChart extends AppCompatActivity implements View.OnClickListener{}
 * **/
public class ECGChart extends AppCompatActivity implements View.OnClickListener{
    //20240712添加以下两行代码检验是否正确读到数据
    private static final String ECBLEChineseTypeGBK = "gbk";
    private static String ecBLEChineseType = ECBLEChineseTypeGBK;
    //20240722添加以下代码，蓝牙连接标志，重写onConnectionStateChange内部逻辑代码
    private static boolean connectFlag = false;

    private static final String TAG = "ECGChart";
    private final static String SERVICE_EIGENVALUE_SEND = "0000ffe2-0000-1000-8000-00805f9b34fb";//蓝牙的特征值，发送
    private final static String SERVICE_EIGENVALUE_READ = "0000ffe2-0000-1000-8000-00805f9b34fb";//蓝牙的特征值，接收
    private BluetoothGattCharacteristic mNeedCharacteristic;
    private Handler mTimeHandler = new Handler();
    private BluetoothGatt mBtGatt;
    Wave waveShowView;
    BluetoothDevice device = null;
    TextView txtECG;
    private ArrayList<Double> offlineRateData = new ArrayList<>();//记录离线心电数据
    private ArrayList<Double> offlineRespiratoryData = new ArrayList<>();//处理离线呼吸波所需要的原始数据
    ArrayList<Double>   offlineRateOrginateData=new ArrayList<Double>();//心电原始信号
    String HexOriginateHeartData;
    private Queue<Double> RPdataQ = new LinkedList<Double>();//为心电数据一个个展示设置的队列
    int HeartratelistInt=0;


    private int flag = 0;//相当于connectFlag
    private  ArrayList<Double> res488=new ArrayList<>();//如果res追加到了一个波的大小，就计算呼吸波
    StringBuilder sb = new StringBuilder();
    private TextView mTvReceive;
//    WaveShowView waveShowView2;//呼吸波相关参数
//    Respiratory_Wave waveShowView2;
//    RPView waveShowView2;
//    Respiratory_Wave waveShowView2;
//    Wave waveShowView;
//    Ecg_View ecg_view;
//    WaveShowView waveShowView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cecgchart);
        // 隐藏标题栏，setContentView后调用
        if (getSupportActionBar()!=null){
            getSupportActionBar().hide();
        }

        SpeechUtility.createUtility(ECGChart.this, SpeechConstant.APPID +"=5f16ff0d");

        //ChartView功能按钮的选择点击事件
        findViewById(R.id.EcgChartView_disconnect).setOnClickListener(this);
        findViewById(R.id.OfflineData).setOnClickListener(this);
        findViewById(R.id.OfflineView).setOnClickListener(this);

        /**调用python https://chaquo.com/chaquopy/doc/current/android.html#android-startup
         * 偶尔使用python，需要首先检查是否已经启动
         * 万万不能删除，一删除传数据就闪退，即调用python前必须对其用Application继承PyApplication
         **/
        System.out.println("在 ECGChart.java，进行Python.isStarted()判断:" + Python.isStarted());//调用前检查
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(ECGChart.this));
            System.out.println("Python.isStarted():" + Python.isStarted());
        }

        initUI();
        getBleAddress();//接受蓝牙地址
        connectBluetooth(device);

        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (HeartratelistInt > 100) {
                    SoundTipUtil.soundTip(ECGChart.this,"警告！你的心率超过100次/分");
                }
                handler.postDelayed(this, 7000);
            }
        };
        handler.postDelayed(runnable, 7000);
    }

    /**初始化控件
     * @ mTvReceive，find id
     * @ waveShowView,find 心电绘图区域id
     * @ waveShowView2，find 心电绘图呼吸波区域id
     * @ txtECG，更新显示心电数值
     * */
    private void initUI() {
        waveShowView = findViewById(R.id.waveShowView);
        txtECG = findViewById(R.id.txtECG);
        //mTvReceive = findViewById(R.id.mTvReceive);//源呼吸波数字
        //waveShowView2 = findViewById(R.id.waveShowView2);//原呼吸波视图
    }

    /**接受点击的某一个蓝牙地址
     * @ device 传递给device
     * 如果发现没有该太大的地方点击蓝牙连接时退出，大概率从logcat可以看出是此处的问题，原因可能是接收的地址为空，
     * 但实际上之前也这样就没问题啊，再出现的话，将下面代码重新CV
     * */
    private void getBleAddress() {
        Intent bleAddressIntent = getIntent();
        device = bleAddressIntent.getParcelableExtra("deviceAdress");
        //进行一个低通蓝牙通讯
        Log.d("In ECGChart.java,getBleAddress() function", "收到MAC地址：  " + device.getAddress());
    }

    /**连接蓝牙
     *  @ device  目标设备**/
    @SuppressLint("MissingPermission")
    private void connectBluetooth(BluetoothDevice device) {
        Log.i(TAG,"蓝牙搜索状态：关闭蓝牙搜索"); //设置1s延迟，保证搜索完全关闭，再开始连接蓝牙。
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG,"连接蓝牙");
                mBtGatt = device.connectGatt(ECGChart.this, false, mBtGattCallback);//连接蓝牙：autoConnect（布尔值，指示是否在可用时自动连接到BLE设备）
            }
        }, 1000);
    }

    /**蓝牙服务回调，即建立通信**/
    private final BluetoothGattCallback mBtGattCallback = new BluetoothGattCallback() {
        //成功连接到设备调用此方法
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.e(TAG, "onConnectionStateChange中，状态=" + status + "||" + "新状态=" + newState);
            //判断蓝牙是否连接成功
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();//发现设备服务 去获取服务
                Log.i(TAG,"在onConnectionStateChange()函数中: 连接成功");
                connectFlag = true;//20240722
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mBtGatt.close();//关闭回调服务（等于断开蓝牙连接）
                Log.i(TAG, "在onConnectionStateChange()函数中: 连接失败");
                connectFlag = false;//20240722
            }
        }

        /**发现服务，在设备连接成功后调用，扫描到设备服务后调用此方法。
         * 调用mBluetoothGatt.discoverServices();方法后，onServicesDiscovered（）这个方法会被调用，说明发现当前设备了。
         * 然后可以在里面去获取BluetoothGattService和BluetoothGattCharacteristic。
         **/
        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            //判断回调服务是否成功
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "在onServicesDiscovered函数中: 回调服务连接成功，状态为：" + status);
            } else {
                Log.i(TAG, "在onServicesDiscovered函数中没有成功连接回调服务，状态为：" + status);
            }

            //以下打印蓝牙服务
            Log.e(TAG, "==========================================================================================" );
            List<BluetoothGattService> servicesLists = gatt.getServices(); //获取服务UUID并添加进集合
            Log.i(TAG, "扫描到服务的个数:" + servicesLists.size());
            int i = 0;
            //获取单个服务
            for (final BluetoothGattService servicesList : servicesLists) {
                ++i;
                Log.i(TAG, i + "号服务的uuid: " + servicesList.getUuid().toString());
                //获取单个服务下的所有特征
                List<BluetoothGattCharacteristic> gattCharacteristics = servicesList.getCharacteristics();

                int j = 0;
                //对单个服务的特征进行打印
                for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    ++j;
                    if (gattCharacteristic.getUuid().toString().equals(SERVICE_EIGENVALUE_SEND)) {
                        Log.i(TAG, i + "号服务的第" + j + "个特征" + gattCharacteristic.getUuid().toString());
                        String mServiceUUID = servicesList.getUuid().toString();
                        String mReadWriteUUID = gattCharacteristic.getUuid().toString();

                        System.out.println("mServiceUUID：" + mServiceUUID + "   mReadWriteUUID：" + mReadWriteUUID);
                        Log.i(TAG, "-----------------------------");

                        mNeedCharacteristic = gattCharacteristic;
                        Log.i(TAG, "发送特征：" + mNeedCharacteristic.getUuid().toString());
                        //设置开启之后，才能在onCharacteristicRead()这个方法中收到数据。
                        mBtGatt.setCharacteristicNotification(mNeedCharacteristic, true);
                        mTimeHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                BluetoothGattDescriptor clientConfig = mNeedCharacteristic.getDescriptor(UUID.fromString(SERVICE_EIGENVALUE_READ));//这个收取数据的UUID
                                Log.i(TAG,"读取特征值的服务："+clientConfig);//clientConfig目前就是空的

                                if (clientConfig != null) {
                                    clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);//设置接收模式
                                    mBtGatt.writeDescriptor(clientConfig);//必须是设置这个才能监听模块数据
                                } else {
                                    Log.i(TAG, "clientConfig仍为空，备用方法测试");
                                    BluetoothGattService linkLossService = gatt.getService(servicesList.getUuid());
                                    Log.i(TAG,"读取特征值服务："+ linkLossService);
                                    //setNotification(mBtGatt,linkLossService.getCharacteristic(UUID.fromString(SERVICE_EIGENVALUE_READ)),true);
                                }
                            }
                        }, 200);
                    } else {
                        Log.i(TAG, "SERVICE_EIGENVALUE_SEND is false");
                        Log.i(TAG, i + "号服务的第" + j + "个特征" + gattCharacteristic.getUuid().toString());
                    }
                }
            }
            Log.e(TAG, "==========================================================================================" );
        }

        /**开启监听，建立与设备的通信的收发数据通道，BLE开发中只有当上位机成功开启监听后才能与下位机收发数据.开启监听成功调用此方法**/
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "在onDescriptorWrite函数中: 开启蓝牙监听成功");
            }else{
                Log.i(TAG, "在onDescriptorWrite函数中: 没开启蓝牙监听");
            }
        }

        //——————————————————————————————————————————————————————主要修改以下代码内容————————————————————————————————————————————————————————————————
        /**接收数据，发送的数据通过此方法获取。20240803解决
         * 主要涉及到传输的数据格式的转化  尤其是在HexToList过程中存在大问题20240725
         * 理清数据传输的格式，需要十六进制发送，按着十六进制接收，将其转换名为res的List列表 随后进行python的处理调用，关键问题在于 ArrayList<Double> res = DataFormatUtil.hexToList(str);
         * 中的HexToList肯存在问题，处理后的数据直接变为了 -2.554375,-2.52225,-2.457875,-3.774375,-2.554375,-2.55425,-2.488875,。。。
         * **/
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);//20240712

            byte[] value = characteristic.getValue(); //value为蓝牙发送的原始数据

            System.out.println("传输的Value数据格式:"+value);//value：[B@aa67a61、[B@a31d986、、、、

            /**开发完后可删除，于20240712添加的功能
             * 主要功能：logcat输出检验是否正确的接收到了数据，是没问题的
             * 添加的代码有：super.onCharacteristicChanged(gatt, characteristic);以及对应的两个private、if (value != null) 判断
             **/
//            if (value != null) {
//                String str = "";
//                if (Objects.equals(ecBLEChineseType, ECBLEChineseTypeGBK)) {
//                    try {
//                        str = new String(value, "GBK");
//                    } catch (Throwable ignored) {
//                    }
//                } else {
//                    str = new String(value);
//                }
//                String strHex = arrayToHex(value);//将string转为Hex
//                Log.e("DataReceiveCheck", "读取成功[string]:" + str);
//                Log.e("DataReceiveCheck", "读取成功[hex]:" + strHex);
//            }

            String str = arrayToHex(value);//字符串类型的数，返回的是去掉尾空格，大写的Hex字符串
            System.out.println("经过arrayToHex后的str数据格式:"+str);//2D 30 2E 31 31 33 0D 0A 2D 30 2E 30 39 32 0D 0A 2D 30 2E 30 37 37 0D 0A ，转字符串后为-0.113 （0A（回车））-0.092（0A（回车））-0.077，但仍存在数据截断
            Log.i(TAG, "DataFormatUtil.arrayToHex(value)：：" + DataFormatUtil.arrayToHex(value));//2D 30 2E 31 31 33 0D 0A 2D 30 2E 30 39 32 0D 0A 2D 30 2E 30 37 37 0D 0A

            HexOriginateHeartData+=str;//保存至txt所需的变量20240724

            ArrayList<Double> res = DataFormatUtil.hexToList(str);//关键点，将hex转为List列表
            //打印res内容
            for(int i=0; i<res.size();i++){
                System.out.print(res.get(i)+",");//res输出：-2.554375,-2.52225,-2.457875,-3.774375,-2.554375,-2.55425,-2.488875,
            }//改之后循环内的输出应为十六进制数组

            System.out.println("==在ECGChart.Java中, res大小为==:"+res.size());//大小会自增，488个数据最后输出打印的大小为245

//            ArrayList<Double> cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN = DataFormatUtil.Filter(res);//经过滤波之后的点
            ArrayList<Double> Heartratelist = new ArrayList<Double>();//心电数据


            ArrayList<Double> RespiratoryWavelist = new ArrayList<Double>();//呼吸波数据

            /**将原始心电信号存储到offlineRateOrginateData数组当中，转存到txt当中**/
            for(int i=0;i<res.size();i++){
                offlineRateOrginateData.add(res.get(i));
            }
            //大小会自增，488个数据最后输出打印的大小为1884，最终保存到手机中的也是1884个数据
            System.out.println("==在ECGChart.Java中, offlineRateOrginateData大小为==:"+offlineRateOrginateData.size());
            System.out.print("——————————————————————————————————————————————————————————————————————————————————————————————");

        //——————————————————————————————————————————————————————主要修改以上代码内容————————————————————————————————————————————————————————————————

            /**将原始心电信号经过调用python代码进行滤波，并计算心率（滤波算法以及心率的计算均在python代码当中【ecgFilter.py】）*/
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //20240727注释以下代码：/**将Java的ArrayList对象传入Python中使用**/~~~txtECG.setText(HeartratelistInt + " ");
                    //为的是直接显示标准的数图像

//                    ///**将Java的ArrayList对象传入Python中使用**/
//                    Python py = Python.getInstance();//创建连接Python的接口
//                    ///**1.进行数据滤波**/
//                    PyObject obj = py.getModule("ecgFilterNew").callAttr("ecgFilter", res.get(0),res.get(1),res.get(2),res.get(3),res.get(4),res.get(5),res.get(6),res.get(7),res.get(8),res.get(9),res.get(10),res.get(11),res.get(12),res.get(13),res.get(14),res.get(15),res.get(16),res.get(17),res.get(18),res.get(19),res.get(20),res.get(21),res.get(22),res.get(23),res.get(24),res.get(25),res.get(26),res.get(27),res.get(28),res.get(29),res.get(30),res.get(31),res.get(32),res.get(33),res.get(34),res.get(35),res.get(36),res.get(37),res.get(38),res.get(39),res.get(40),res.get(41),res.get(42),res.get(43),res.get(44),res.get(45),res.get(46),res.get(47),res.get(48),res.get(49),res.get(50),res.get(51),res.get(52),res.get(53),res.get(54),res.get(55),res.get(56),res.get(57),res.get(58),res.get(59),res.get(60),res.get(61),res.get(62),res.get(63),res.get(64),res.get(65),res.get(66),res.get(67),res.get(68),res.get(69),res.get(70),res.get(71),res.get(72),res.get(73),res.get(74),res.get(75),res.get(76),res.get(77),res.get(78),res.get(79),res.get(80),res.get(81),res.get(82),res.get(83),res.get(84),res.get(85),res.get(86),res.get(87),res.get(88),res.get(89),res.get(90),res.get(91),res.get(92),res.get(93),res.get(94),res.get(95),res.get(96),res.get(97),res.get(98),res.get(99),res.get(100),res.get(101),res.get(102),res.get(103),res.get(104),res.get(105),res.get(106),res.get(107),res.get(108),res.get(109),res.get(110),res.get(111),res.get(112),res.get(113),res.get(114),res.get(115),res.get(116),res.get(117),res.get(118),res.get(119),res.get(120),res.get(121));
//                    List<PyObject> pyList = obj.asList();//将从python中取得的值进行java转换
//                    System.out.println("在 ECGChart.Java Runnable()函数中, pyList大小为:"+pyList.size());
//                    for (int i = 0; i < pyList.size(); i++) {
//                        Double x = pyList.get(i).toDouble();
//                        //为离线分析做处理
//                        offlineRateData.add(x);
//                        offlineRespiratoryData.add(x);
//                        Heartratelist.add(x);//为绘制心电图做处理
//                    }
//                    //只有下面这句输出打印的offlineRateData的大小与实际发送的数据大小一致，实际发送488个
//                    System.out.println("在 ECGChart.Java Runnable()函数中, offlineRateData 大小为:"+offlineRateData.size());
//
//                    ///**2.通过python调用计算心率**/
//                    PyObject obj2 = py.getModule("ecgFilterNew").callAttr("get_hear_rate");
//                    Integer rate = obj2.toJava(Integer.class);
//                    HeartratelistInt = rate.intValue();
//                    System.out.println("在 ECGChart.Java Runnable()函数中, 心率为:"+HeartratelistInt);
//                    txtECG.setText(HeartratelistInt + " ");

                    //直接绘图时离线图片没有数据，在OfflineRateActivity.java中offline_ratedata因注释上面两个步骤是空的了，导致后面的数据都是空的，所以没图像
                    //20240803添加以下代码解决
                    for (int i = 0; i < res.size(); i++) {
                        offlineRateData.add(res.get(i));
                        offlineRateOrginateData.add(res.get(i));
                    }

                    /**3.展示心电图数据**/
                    ////下面这句输出打印的Heartratelist的大小与实际发送的数据大小一致，实际发送488个
                    System.out.println("在 ECGChart.Java Runnable()函数中, Heartratelist.size()大小为："+ res.size());//20240726将Heartratelist改为res直接测试原数据绘图
                    for (int i = 0; i < res.size(); i++) {
                        waveShowView.showLine(res.get(i));
                    }

//                    System.out.println("obj3");
//                    List<PyObject> pyList3 = obj3;
//                    for (int i = 0; i < pyList3.size(); i++) {
//                        Double x = pyList.get(i).toDouble();
//                        //为绘制呼吸波做处理
//                        RespiratoryWavelist.add(x);
//
//                    }
//                    //展示呼吸波
//                    for (int i = 0; i < RespiratoryWavelist.size(); i++) {
//                        waveShowView2.showLine(RespiratoryWavelist.get(i));
//                    }
                }
            });

/**呼吸波相关，通过python调用进行呼吸波的提取**/
//            RPdataQ.addAll(res);
//            System.out.println("RPdataQ:"+RPdataQ.size());
//
//            res488.addAll(res);
//            System.out.println("res488 size::"+res488.size());
//            ArrayList<Double> respiratoryData = new ArrayList<>();
//            if(res488.size()==1464){
//                respiratoryData= RespiratoryCalc.respiratoryCalc(res488);
//                System.out.println("respiratoryData");
//                System.out.println("size:"+respiratoryData.size());
//                System.out.println(respiratoryData.toString());
//                res488.clear();
//            }
//            //画呼吸波
//            for (int i = 0; i < respiratoryData.size(); i++) {
////                System.out.println("list of for:"+list.get(i).getClass());
////                ecg_view.showLine(res.get(i));
//                waveShowView2.showLine(respiratoryData.get(i));
//            }
//            for (int i = 0; i < res.size(); i++) {
////                System.out.println("list of for:"+list.get(i).getClass());
////                ecg_view.showLine(res.get(i));
//                waveShowView.showLine(res.get(i));
//            }

//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Python py = Python.getInstance();// 将Java的ArrayList对象传入Python中使用
//                    //拿到呼吸波数据
//                    PyObject obj3 = py.getModule("ecgFilter").callAttr("getRespiratorywave", cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(0), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(1), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(2), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(3), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(4), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(5), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(6), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(7), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(8), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(9), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(10), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(11), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(12), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(13), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(14), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(15), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(16), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(17), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(18), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(19), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(20), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(21), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(22), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(23), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(24), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(25), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(26), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(27), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(28), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(29), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(30), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(31), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(32), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(33), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(34), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(35), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(36), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(37), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(38), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(39), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(40), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(41), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(42), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(43), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(44), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(45), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(46), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(47), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(48), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(49), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(50), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(51), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(52), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(53), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(54), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(55), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(56), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(57), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(58), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(59), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(60), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(61), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(62), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(63), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(64), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(65), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(66), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(67), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(68), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(69), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(70), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(71), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(72), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(73), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(74), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(75), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(76), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(77), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(78), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(79), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(80), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(81), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(82), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(83), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(84), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(85), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(86), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(87), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(88), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(89), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(90), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(91), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(92), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(93), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(94), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(95), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(96), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(97), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(98), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(99), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(100), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(101), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(102), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(103), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(104), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(105), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(106), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(107), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(108), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(109), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(110), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(111), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(112), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(113), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(114), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(115), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(116), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(117), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(118), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(119), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(120), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(121));
//                    //将从python中取得的值进行java转换
//                    if(obj3!=null){
//                        List<PyObject> pyList3 = obj3.asList();
//                        System.out.println("obj3");
//                        System.out.println(obj3);
//                    }
//                }
//            });

        }
    };

    /**触发返回按钮并断开蓝牙连接
     * 后续可删除**/
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                mBtGatt.disconnect();
                Log.i(TAG, "在ECGChart.Java中，蓝牙连接状态: 断开蓝牙连接");
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    /**离线功能选择按钮点击事件20240722-20240802**/
    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.OfflineView:
                if(ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    mBtGatt.disconnect();
                    Toast.makeText(ECGChart.this, "蓝牙连接已断开", Toast.LENGTH_SHORT).show();
                    // 通过Intent传递对象给Service
                    Intent intent = new Intent(ECGChart.this, OfflineRateActivity.class);
                    intent.setAction("action");
                    intent.putExtra("offline_orginateratedata", offlineRateOrginateData);//心电原始信号
                    intent.putExtra("offline_ratedata", offlineRateData);//心电滤波信号

                    if (intent.resolveActivity(getPackageManager()) != null) {//20240801
                        Log.i(TAG,"可以正常启动处理Intent");
                        startActivity(intent);
                    } else {Log.i(TAG,"没有活动可以处理这个Intent");}
                }else{
                    Toast.makeText(ECGChart.this, "没有蓝牙权限", Toast.LENGTH_SHORT).show();
                }
            break;

            case R.id.EcgChartView_disconnect:
                    mBtGatt.disconnect();
                    Toast.makeText(ECGChart.this, "蓝牙连接已断开！", Toast.LENGTH_SHORT).show();
                    //finish();//添加此代码返回至BLE界面，
            break;

            case R.id.OfflineData:
                System.out.println("ECGChart.Java,case  R.id.offline_data,原始离线数据的大小:"+offlineRateOrginateData.size());
                mBtGatt.disconnect();
                Log.i(TAG, "选择了保存离线数据，已断开蓝牙连接");
                //将原始数据存储在txt当中，OfflineRateActivity同样解除注释调用，可能会有冲突
                AlertDialog.Builder builder = new AlertDialog.Builder(ECGChart.this);
                builder.setTitle("请输入编号信息");//设置对话框标题
                builder.setIcon(android.R.drawable.btn_star);//设置对话框标题前的图标
                final EditText edit = new EditText(ECGChart.this);
                builder.setView(edit);
                builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(ECGChart.this, "文件已保存至 我的手机/Android/data/USTB.AAIST/files", Toast.LENGTH_SHORT).show();
                        //存储经过计算之后的心电信号
                        String orginatepath=FileUtils.getOrginateFilesPath(ECGChart.this,edit.getText().toString());
                        for(int i=0;i<offlineRateOrginateData.size();i++){
                            FileUtils.orginatewrite(orginatepath,offlineRateOrginateData.get(i)+"\n");//离线数据分隔格式：回车符分隔
                        }
                    }
                });
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(ECGChart.this, "已取消", Toast.LENGTH_SHORT).show();
                    }
                });
                builder.setCancelable(true);//设置按钮是否可以按返回键取消,false则不可以取消
                AlertDialog dialog = builder.create();//创建对话框
                dialog.setCanceledOnTouchOutside(true);//设置弹出框失去焦点是否隐藏,即点击屏蔽其它地方是否隐藏
                dialog.show();
            break;
        }
    }
  }