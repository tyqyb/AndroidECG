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
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import USTB.AAIST.utils.DataFormatUtil;
import USTB.AAIST.utils.FileUtils;
import USTB.AAIST.utils.SoundTipUtil;//语音播报
import USTB.AAIST.view.RPView;
import USTB.AAIST.view.Wave;

/**
 * //设置开启之后，才能在onCharacteristicRead()这个方法中收到数据。的if判断中进入了备用方法测试,因此没有接收到数据
 * 参考BLE开发文档修改
 * **/
public class ECGChart extends AppCompatActivity {

    //20240712添加以下两行代码检验是否正确读到数据
    private static final String ECBLEChineseTypeGBK = "gbk";
    private static String ecBLEChineseType = ECBLEChineseTypeGBK;




    private TextView mTvReceive;
    private static final String TAG = "MainActivity";
    StringBuilder sb = new StringBuilder();
    //蓝牙的特征值，发送
    private final static String SERVICE_EIGENVALUE_SEND = "0000ffe2-0000-1000-8000-00805f9b34fb";
    //蓝牙的特征值，接收
    private final static String SERVICE_EIGENVALUE_READ = "0000ffe2-0000-1000-8000-00805f9b34fb";
    private BluetoothGattCharacteristic mNeedCharacteristic;
    private Handler mTimeHandler = new Handler();
    private BluetoothGatt mBtGatt;
    //Ecg_View ecg_view;
//    WaveShowView waveShowView;
    Wave waveShowView;
    // WaveShowView waveShowView2;
    // Respiratory_Wave waveShowView2;
    //RPView waveShowView2;


//   Respiratory_Wave waveShowView2;

    //    Wave waveShowView;
    BluetoothDevice device = null;
    TextView txtECG;
    //记录离线心电数据
    private ArrayList<Double> offlineRateData = new ArrayList<>();
    //处理离线呼吸波所需要的原始数据
    private ArrayList<Double> offlineRespiratoryData = new ArrayList<>();
    //如果res追加到了一个波的大小，就计算呼吸波
    private  ArrayList<Double> res488=new ArrayList<>();

    //心电原始信号
    ArrayList<Double>   offlineRateOrginateData=new ArrayList<Double>();

    String HexOriginateHeartData;
    //为心电数据一个个展示设置的队列
    private Queue<Double> RPdataQ = new LinkedList<Double>();
    private int flag = 0;
    int HeartratelistInt=0;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cecgchart);


        SpeechUtility.createUtility(ECGChart.this, SpeechConstant.APPID +"=5f16ff0d");
        /**调用python https://chaquo.com/chaquopy/doc/current/android.html#android-startup
         *偶尔使用python，需要首先检查是否已经启动
         **/
        System.out.println("Python.isStarted():" + Python.isStarted());//调用前检擦
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(ECGChart.this));
            System.out.println("Python.isStarted():" + Python.isStarted());//是否已经成功调用
        }
//        B3.add(1.579571604101638e-08);
//        B3.add(4.738714812304913e-08);
//        B3.add(4.738714812304913e-08);
//        B3.add(1.579571604101638e-08);
//
//        A3.add(1.0);
//        A3.add(-2.989946914091736);
//        A3.add(2.979944296951953);
//        A3.add(-0.965081173899132);
        initUI();
        //接受蓝牙地址
        getBleAddress();
        connectBluetooth(device);

//         simulator();
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
//      mTvReceive = findViewById(R.id.mTvReceive);
        waveShowView = findViewById(R.id.waveShowView);
//      waveShowView2 = findViewById(R.id.waveShowView2);
        txtECG = findViewById(R.id.txtECG);
    }

    /**接受点击的某一个蓝牙地址
     * @ device 传递给device
     * 如果发现没有该太大的地方点击蓝牙连接时退出，大概率从logcat可以看出是此处的问题，原因可能是接收的地址为空，但实际上之前也这样就没问题啊，再出现的话，将下面代码重新CV
     * */
    //解决点击闪退的CV代码
    //    private void getBleAddress() {
    //        Intent bleAddressIntent = getIntent();
    //        device = bleAddressIntent.getParcelableExtra("deviceAdress");
    //        //进行低通蓝牙得一个通讯
    //        Log.d("DemoInfo", "收到MAC地址：  " + device.getAddress());
    //    }
    private void getBleAddress() {
        Intent bleAddressIntent = getIntent();
        device = bleAddressIntent.getParcelableExtra("deviceAdress");
        //进行低通蓝牙得一个通讯
        Log.d("DemoInfo", "收到MAC地址：  " + device.getAddress());
    }

    /**连接蓝牙
     * @ device  目标设备
     * */
    @SuppressLint("MissingPermission")
    private void connectBluetooth(BluetoothDevice device) {
        Log.i("TAG", "connectBluetooth: 关闭蓝牙搜索"); //设置1s延迟，保证搜索完全关闭，再开始连接蓝牙。
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i("=======", "连接蓝牙");
                //连接蓝牙：autoConnect（布尔值，指示是否在可用时自动连接到BLE设备）
                mBtGatt = device.connectGatt(ECGChart.this, false, mBtGattCallback);
            }
        }, 1000);
    }


    /**蓝牙服务回调（建立通信）*/
    private final BluetoothGattCallback mBtGattCallback = new BluetoothGattCallback() {
        //成功连接到设备调用此方法
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            //判断蓝牙是否连接成功
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                //发现设备服务 去获取服务
                gatt.discoverServices();
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        mTvState.setText(getString(R.string.connection_succeeded));//设置文本状态为“已连接”
//                    }
//                });
                Log.i("TAG", "onConnectionStateChange: 连接成功");

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                //关闭回调服务（等于断开蓝牙连接）
                mBtGatt.close();
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        mTvState.setText(getString(R.string.connection_failed));
//                    }
//                });
                Log.i("TAG", "onConnectionStateChange: 连接失败");
            }
        }

        //发现服务，在设备连接成功后调用，扫描到设备服务后调用此方法。
        //调用mBluetoothGatt.discoverServices();方法后，onServicesDiscovered（）这个方法会被调用，说明发现当前设备了。然后我们就可以在里面去获取BluetoothGattService和BluetoothGattCharacteristic。
        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            //判断回调服务是否成功
            if (status == BluetoothGatt.GATT_SUCCESS) {
                /// isGattSuccess = true;
                Log.i("TAG", "onServicesDiscovered: 回调服务连接成功");
            } else {
                //  isGattSuccess = false; //状态标记
                Log.i("TAG", "onServicesDiscovered: 回调服务连接失败" + status);
            }

            /**模仿源代码仿写*/
            List<BluetoothGattService> servicesLists = gatt.getServices(); //获取服务UUID并添加进集合
            Log.i(TAG, "扫描到服务的个数:" + servicesLists.size());
            int i = 0;
            //获取单个服务
            for (final BluetoothGattService servicesList : servicesLists) {
                ++i;
                Log.i(TAG, "-----------打印服务----------");
                Log.i(TAG, i + "号服务的uuid: " + servicesList.getUuid().toString());
                //获取单个服务下的所有特征
                List<BluetoothGattCharacteristic> gattCharacteristics = servicesList
                        .getCharacteristics();

                int j = 0;
                Log.i(TAG, "----------打印特征-----------");
                //对单个服务得特征进行打印
                for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    ++j;
                    if (gattCharacteristic.getUuid().toString().equals(SERVICE_EIGENVALUE_SEND)) {//汇承蓝牙的UUID
                        Log.i(TAG, "蓝牙的UUID");
                        Log.i(TAG, i + "号服务的第" + j + "个特征" + gattCharacteristic.getUuid().toString());
                        String mServiceUUID = servicesList.getUuid().toString();
                        String mReadWriteUUID = gattCharacteristic.getUuid().toString();
                        System.out.println("mServiceUUID：" + mServiceUUID + "   mReadWriteUUID：" + mReadWriteUUID);
                        mNeedCharacteristic = gattCharacteristic;
                        Log.i(TAG, "发送特征：" + mNeedCharacteristic.getUuid().toString());
                        //设置开启之后，才能在onCharacteristicRead()这个方法中收到数据。
                        mBtGatt.setCharacteristicNotification(
                                mNeedCharacteristic, true);
                        mTimeHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                BluetoothGattDescriptor clientConfig = mNeedCharacteristic
                                        .getDescriptor(UUID.fromString(SERVICE_EIGENVALUE_READ));//这个收取数据的UUID
                                Log.i(TAG,"SERVICE_EIGENVALUE_READ："+clientConfig);//clientConfig目前就是空的

                                if (clientConfig != null) {
                                    //BluetoothGatt.getService(service)
                                    clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);//设置接收模式
                                    mBtGatt.writeDescriptor(clientConfig);//必须是设置这个才能监听模块数据
                                } else {
                                    Log.i(TAG, "备用方法测试");
                                    BluetoothGattService linkLossService = gatt.getService(servicesList.getUuid());
//                                    setNotification(mBtGatt,linkLossService.getCharacteristic(UUID.fromString(SERVICE_EIGENVALUE_READ)),true);
                                }
                            }
                        }, 200);
                    } else {

                        Log.i(TAG, i + "号服务的第" + j + "个特征" + gattCharacteristic.getUuid().toString());
                    }
                }
            }

        }

        //开启监听，即建立与设备的通信的首发数据通道，BLE开发中只有当上位机成功开启监听后才能与下位机收发数据.开启监听成功调用此方法。
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onDescriptorWrite: 开启监听成功");
            }else{
                Log.i(TAG, "onDescriptorWrite: 没开启监听");
            }

        }


        //接收数据，若发送的数据符合通信协议，则下位机会向上位机回复相应的数据。发送的数据通过此方法获取。
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);//20240712，这行别删
            //接受到的原始数据
            byte[] value = characteristic.getValue(); //value为设备发送的数据，根据数据协议进行解析。

/**20240712
* logcat输出检验是否正确的读到了数据，是没问题的    /可删除
 * 添加的代码有：super.onCharacteristicChanged(gatt, characteristic);以及对应的两个private
 * if (value != null) 判断
* **/
            if (value != null) {
                String str = "";
                if (Objects.equals(ecBLEChineseType, ECBLEChineseTypeGBK)) {
                    try {
                        str = new String(value, "GBK");
                    } catch (Throwable ignored) {
                    }
                } else {
                    str = new String(value);
                }
                String strHex = arrayToHex(value);
                Log.e("ble-receive", "读取成功[string]:" + str);
                Log.e("ble-receive", "读取成功[hex]:" + strHex);
            }


            System.out.println(value);
            //字符串类型的数据
            String str = arrayToHex(value);
            //保存至txt所需的变量
            HexOriginateHeartData+=str;
            //每次都是122个点的列表
            ArrayList<Double> res = DataFormatUtil.hexToList(str);
            //经过滤波之后的点
//            ArrayList<Double> cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN = DataFormatUtil.Filter(res);
            //心电数据
            ArrayList<Double> Heartratelist = new ArrayList<Double>();
            //呼吸波数据
            ArrayList<Double> RespiratoryWavelist = new ArrayList<Double>();


            /**
             * 将原始心电信号存储到offlineRateOrginateData数组当中
             */
            //将原始信号存入到txt当中
            for(int i=0;i<res.size();i++){
                offlineRateOrginateData.add(res.get(i));
            }

            /**
             * 呼吸波相关的
             */
            //通过python调用进行呼吸波的提取

//            RPdataQ.addAll(res);
//            System.out.println("RPdataQ:"+RPdataQ.size());



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
//            //去画呼吸波
//            for (int i = 0; i < respiratoryData.size(); i++) {
//                // System.out.println("list of for:"+list.get(i).getClass());
////                ecg_view.showLine(res.get(i));
//                waveShowView2.showLine(respiratoryData.get(i));
//            }
//            for (int i = 0; i < res.size(); i++) {
//                // System.out.println("list of for:"+list.get(i).getClass());
////                ecg_view.showLine(res.get(i));
//                waveShowView.showLine(res.get(i));
//            }

            /**将原始心电信号经过调用python代码进行滤波，并计算心率（滤波算法以及心率的计算均在python代码当中【ecgFilter.py】）*/
            //进行与python的调用
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // 将Java的ArrayList对象传入Python中使用
                    Python py = Python.getInstance();
                    //****************************1.进行数据滤波******************************************
                    //  PyObject obj = py.getModule("ecgFilter").callAttr("ecgFilter", cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(0), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(1), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(2), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(3), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(4), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(5), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(6), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(7), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(8), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(9), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(10), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(11), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(12), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(13), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(14), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(15), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(16), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(17), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(18), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(19), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(20), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(21), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(22), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(23), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(24), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(25), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(26), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(27), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(28), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(29), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(30), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(31), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(32), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(33), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(34), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(35), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(36), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(37), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(38), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(39), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(40), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(41), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(42), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(43), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(44), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(45), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(46), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(47), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(48), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(49), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(50), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(51), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(52), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(53), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(54), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(55), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(56), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(57), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(58), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(59), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(60), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(61), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(62), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(63), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(64), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(65), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(66), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(67), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(68), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(69), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(70), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(71), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(72), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(73), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(74), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(75), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(76), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(77), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(78), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(79), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(80), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(81), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(82), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(83), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(84), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(85), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(86), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(87), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(88), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(89), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(90), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(91), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(92), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(93), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(94), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(95), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(96), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(97), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(98), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(99), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(100), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(101), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(102), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(103), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(104), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(105), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(106), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(107), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(108), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(109), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(110), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(111), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(112), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(113), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(114), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(115), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(116), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(117), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(118), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(119), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(120), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(121)
                    PyObject obj = py.getModule("ecgFilterNew").callAttr("ecgFilter", res.get(0),res.get(1),res.get(2),res.get(3),res.get(4),res.get(5),res.get(6),res.get(7),res.get(8),res.get(9),res.get(10),res.get(11),res.get(12),res.get(13),res.get(14),res.get(15),res.get(16),res.get(17),res.get(18),res.get(19),res.get(20),res.get(21),res.get(22),res.get(23),res.get(24),res.get(25),res.get(26),res.get(27),res.get(28),res.get(29),res.get(30),res.get(31),res.get(32),res.get(33),res.get(34),res.get(35),res.get(36),res.get(37),res.get(38),res.get(39),res.get(40),res.get(41),res.get(42),res.get(43),res.get(44),res.get(45),res.get(46),res.get(47),res.get(48),res.get(49),res.get(50),res.get(51),res.get(52),res.get(53),res.get(54),res.get(55),res.get(56),res.get(57),res.get(58),res.get(59),res.get(60),res.get(61),res.get(62),res.get(63),res.get(64),res.get(65),res.get(66),res.get(67),res.get(68),res.get(69),res.get(70),res.get(71),res.get(72),res.get(73),res.get(74),res.get(75),res.get(76),res.get(77),res.get(78),res.get(79),res.get(80),res.get(81),res.get(82),res.get(83),res.get(84),res.get(85),res.get(86),res.get(87),res.get(88),res.get(89),res.get(90),res.get(91),res.get(92),res.get(93),res.get(94),res.get(95),res.get(96),res.get(97),res.get(98),res.get(99),res.get(100),res.get(101),res.get(102),res.get(103),res.get(104),res.get(105),res.get(106),res.get(107),res.get(108),res.get(109),res.get(110),res.get(111),res.get(112),res.get(113),res.get(114),res.get(115),res.get(116),res.get(117),res.get(118),res.get(119),res.get(120),res.get(121)

                    );
                    //将从python中取得的值进行java转换
                    List<PyObject> pyList = obj.asList();

                    for (int i = 0; i < pyList.size(); i++) {
                        Double x = pyList.get(i).toDouble();
                        //为离线分析做处理
                        offlineRateData.add(x);
                        offlineRespiratoryData.add(x);
                        //为绘制心电图做处理
                        Heartratelist.add(x);

                    }
                    //   System.out.println("offlineRateData 长度为:"+offlineRateData.size());
                    //****************************2.通过python调用计算心率*****************************************
                    PyObject obj2 = py.getModule("ecgFilterNew").callAttr("get_hear_rate");
                    Integer rate = obj2.toJava(Integer.class);
                    HeartratelistInt = rate.intValue();
                    //System.out.println("心率值为"+HeartratelistInt);



                    txtECG.setText(HeartratelistInt + " ");
                    //****************************3.展示心电图数据的*****************************************
                    System.out.println(" Heartratelist.size()"+ Heartratelist.size());
                    for (int i = 0; i < Heartratelist.size(); i++) {
                        // System.out.println("list of for:"+list.get(i).getClass());
//                ecg_view.showLine(res.get(i));
                        waveShowView.showLine(Heartratelist.get(i));
                    }









//
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
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    // 将Java的ArrayList对象传入Python中使用
//                    Python py = Python.getInstance();
//                    //拿到呼吸波数据
//                    PyObject obj3 = py.getModule("ecgFilter").callAttr("getRespiratorywave", cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(0), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(1), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(2), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(3), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(4), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(5), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(6), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(7), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(8), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(9), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(10), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(11), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(12), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(13), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(14), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(15), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(16), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(17), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(18), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(19), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(20), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(21), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(22), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(23), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(24), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(25), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(26), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(27), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(28), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(29), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(30), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(31), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(32), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(33), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(34), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(35), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(36), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(37), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(38), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(39), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(40), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(41), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(42), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(43), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(44), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(45), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(46), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(47), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(48), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(49), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(50), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(51), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(52), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(53), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(54), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(55), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(56), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(57), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(58), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(59), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(60), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(61), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(62), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(63), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(64), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(65), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(66), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(67), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(68), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(69), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(70), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(71), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(72), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(73), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(74), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(75), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(76), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(77), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(78), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(79), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(80), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(81), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(82), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(83), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(84), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(85), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(86), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(87), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(88), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(89), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(90), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(91), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(92), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(93), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(94), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(95), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(96), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(97), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(98), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(99), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(100), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(101), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(102), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(103), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(104), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(105), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(106), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(107), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(108), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(109), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(110), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(111), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(112), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(113), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(114), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(115), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(116), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(117), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(118), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(119), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(120), cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.get(121));
//                    //将从python中取得的值进行java转换
//                    if(obj3!=null){
//                        List<PyObject> pyList3 = obj3.asList();
//                        System.out.println("obj3");
//                        System.out.println(obj3);
//                    }
//
//
//                }
//            });


        }
    };

    /**
     * 触发返回按钮，关闭蓝牙
     *  //断开连接
     *             mBtGatt.disconnect();
     *                Log.i(TAG, "disConnected: 断开蓝牙连接");
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                mBtGatt.disconnect();
                Log.i(TAG, "disConnected: 断开蓝牙连接");
            }

        }
        return super.onKeyDown(keyCode, event);
    }


    //
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.offline_rate:
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    mBtGatt.disconnect();
                    // Intent intent =new Intent();
                    //传递：
                    // 通过Intent传递对象给Service
                    Intent intent = new Intent(ECGChart.this, OfflineRateActivity.class);
                    intent.setAction("action");
                    //System.out.println("R.id.offline_rate :"+offlineRateData.size());
                    //心电原始信号
                    intent.putExtra("offline_orginateratedata", offlineRateOrginateData);
                    //心电滤波信号
                    intent.putExtra("offline_ratedata", offlineRateData);

                    startActivity(intent);
//                    intent.putDoubleArrayListExtra("arrayList", offlineRateData);
//                    intent.putArrayListExtra("offlineRateData", offlineRateData);
                    Log.i(TAG, "disConnected: 断开蓝牙连接");
                }

                //将 值传递给 另外一个界面 记录数据 并进行界面的跳转
                break;
//            case R.id.offline_respiratory:
//                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                    mBtGatt.disconnect();
//                    // Intent intent =new Intent();
//                    //传递：
//                    // 通过Intent传递对象给Service
//                    Intent intent = new Intent(ECGChart.this, OfflineRespiratoryActivity.class);
//                    intent.setAction("action");
//                    intent.putExtra("offline_Respiratorydata", offlineRespiratoryData);
//                    startActivity(intent);
////                    intent.putDoubleArrayListExtra("arrayList", offlineRateData);
////                    intent.putArrayListExtra("offlineRateData", offlineRateData);
//                    Log.i(TAG, "disConnected: 断开蓝牙连接");
//                }
//                break;
            case  R.id.offline_data:
                System.out.println("len is:"+offlineRateOrginateData.size());


                mBtGatt.disconnect();
                //将原始数据存储在txt当中
                AlertDialog.Builder builder = new AlertDialog.Builder(ECGChart.this);
                builder.setTitle("请输入编号信息");    //设置对话框标题
                builder.setIcon(android.R.drawable.btn_star);   //设置对话框标题前的图标
                final EditText edit = new EditText(ECGChart.this);
                builder.setView(edit);
                builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Toast.makeText(cECGChartActivity.this, "你输入的是: " + edit.getText().toString(), Toast.LENGTH_SHORT).show();
                        //存储经过计算之后的心电信号
                        String orginatepath=FileUtils.getOrginateFilesPath(ECGChart.this,edit.getText().toString());
                        for(int i=0;i<offlineRateOrginateData.size();i++){
                            FileUtils.orginatewrite(orginatepath,offlineRateOrginateData.get(i)+",\n");

                        }

                    }
                });
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(ECGChart.this, "你点了取消", Toast.LENGTH_SHORT).show();
                    }
                });
                builder.setCancelable(true);    //设置按钮是否可以按返回键取消,false则不可以取消
                AlertDialog dialog = builder.create();  //创建对话框
                dialog.setCanceledOnTouchOutside(true); //设置弹出框失去焦点是否隐藏,即点击屏蔽其它地方是否隐藏
                dialog.show();

                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = new MenuInflater(this);
        menuInflater.inflate(R.menu.blemenu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * 模拟心电发送，心电数据是一秒500个包，所以
     */
    private void simulator(){
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if(RPView.isRunning){
                    if(RPdataQ.size() > 0){
                        RPView.addRPData(RPdataQ.poll().floatValue());
                    }
                }
            }
        }, 0, 2);
    }
  }