package USTB.AAIST;
/**
 *   APP打包后修改SpeechUtility.createUtility中的APPID
 * **/
import static USTB.AAIST.utils.DataFormatUtil.arrayToHex;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import USTB.AAIST.utils.DataFormatUtil;
import USTB.AAIST.utils.FileUtils;
import USTB.AAIST.utils.SoundTipUtil;//语音播报
import USTB.AAIST.view.Wave;
import android.view.WindowManager;
import com.chaquo.python.PyObject;
import java.util.Objects;
import java.util.Arrays;

/**
 * //设置开启之后，才能在onCharacteristicRead()这个方法中收到数据。的if判断中进入了备用方法测试,因此没有接收到数据
 * 参考BLE开发文档修改
 * 20240722为添加按钮监视，将public class ECGChart extends AppCompatActivity{} 改为public class ECGChart extends AppCompatActivity implements View.OnClickListener{}
 * **/
public class ECGChart extends AppCompatActivity implements View.OnClickListener{
    private MyBluetoothManager myBluetoothManager; // 添加蓝牙管理器引用
    private static final String TAG = "ECGChart";

    private final static String SERVICE_EIGENVALUE_READ = "0000ffe1-0000-1000-8000-00805f9b34fb";//蓝牙的特征值，接收
    private static final String SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
    private static final String CHARACTERISTIC_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";//特征值等价于SERVICE_EIGENVALUE_READ
    private static final UUID CCC_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private ECGChart.ConnectionState mConnectionState = ECGChart.ConnectionState.DISCONNECTED;
    private BluetoothGatt mBtGatt; // 统一使用变量名
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING
    }


    private BluetoothGattCharacteristic mNeedCharacteristic;
    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 1001; // 可以是任意唯一整数
    private Handler mTimeHandler = new Handler();
    Wave waveShowView;
    TextView txtECG;
    TextView Bitchar;
    private TextView tvReceivedData;

    private volatile boolean isListening = true;
    private InputStream inputStream;
    private byte[] buffer = new byte[1024];
    private byte[] dataArray = new byte[0];
    String HexOriginateHeartData;

    private ArrayList<Double> offlineRateData = new ArrayList<>();//记录离线心电数据
    //private ArrayList<Double> offlineRespiratoryData = new ArrayList<>();//处理离线呼吸波所需要的原始数据
    ArrayList<Double>   offlineRateOrginateData=new ArrayList<Double>();//心电原始信号
    private Queue<Double> RPdataQ = new LinkedList<Double>();//为心电数据一个个展示设置的队列
    int HeartratelistInt=0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cecgchart);
        // 隐藏标题栏，setContentView后调用
        if (getSupportActionBar()!=null){
            getSupportActionBar().hide();
        }

        // 初始化蓝牙管理器
        myBluetoothManager = MyBluetoothManager.getInstance(getApplicationContext());
        // 检查蓝牙是否已连接
        if (myBluetoothManager.isDeviceConnected()) {
            mBtGatt = myBluetoothManager.getBluetoothGatt();// 使用已建立的Gatt连接
            if (mBtGatt != null) {
                // 先检查权限
                if (!checkBluetoothPermission()) {
                    requestBluetoothPermissions();
                    return;
                }
                // 设置回调
                mBtGatt.connect(); // 确保连接已建立
                mBtGatt = myBluetoothManager.getBluetoothDevice().connectGatt(this, false, mBtGattCallback);
                mBtGatt.discoverServices(); // 开始服务发现
            } else {
                Toast.makeText(this, "蓝牙连接异常", Toast.LENGTH_SHORT).show();
                finish();
            }
        }  else {
            Toast.makeText(this, "蓝牙未连接", Toast.LENGTH_SHORT).show();
            finish();
        }

        tvReceivedData = findViewById(R.id.Bitchar);
        SpeechUtility.createUtility(ECGChart.this, SpeechConstant.APPID +"=5f16ff0d");//语音组件

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


    @SuppressLint("MissingPermission")
    private void setupNotification(BluetoothGatt gatt) {

        if (gatt == null) {
            Log.e(TAG, "setupNotification: BluetoothGatt is null");
            handleConnectionFailure();
            return;
        }

        if (!checkBluetoothPermission()) {
            requestBluetoothPermissions();
            return;
        }

        BluetoothGattService service = gatt.getService(UUID.fromString(SERVICE_UUID));
        if (service == null) {
            Log.e(TAG, "未找到服务: " + SERVICE_UUID);
            handleConnectionFailure();
            return;
        }

        mNotifyCharacteristic = service.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID));
        if (mNotifyCharacteristic == null) {
            Log.e(TAG, "未找到特征: " + CHARACTERISTIC_UUID);
            handleConnectionFailure();
            return;
        }

        gatt.setCharacteristicNotification(mNotifyCharacteristic, true);// 设置通知

        // 配置CCC描述符
        BluetoothGattDescriptor descriptor = mNotifyCharacteristic.getDescriptor(CCC_DESCRIPTOR_UUID);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);
        } else {
            Log.w(TAG, "未找到CCC描述符，尝试直接启用通知");
            gatt.setCharacteristicNotification(mNotifyCharacteristic, true);
        }
    }


    // 设置特征通知，在服务发现后自动调用
    @SuppressLint("MissingPermission")
    private void setupCharacteristicNotification() {

        //空白检查
        Log.i(TAG, "设置特征通知...");
        if (mBtGatt == null) {
            Log.e(TAG, "setupCharacteristicNotification: BluetoothGatt is null");
            return;
        }

        // 获取服务
        BluetoothGattService service = mBtGatt.getService(UUID.fromString(SERVICE_UUID));
        if (service == null) {
            Log.e(TAG, "未找到指定服务");
            Toast.makeText(this, "未找到蓝牙服务", Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取特征
        mNeedCharacteristic = service.getCharacteristic(UUID.fromString(SERVICE_EIGENVALUE_READ));
        if (mNeedCharacteristic == null) {
            Log.e(TAG, "未找到指定特征");
            Toast.makeText(this, "未找到蓝牙特征", Toast.LENGTH_SHORT).show();
            return;
        }

        // 设置通知
        if (!mBtGatt.setCharacteristicNotification(mNeedCharacteristic, true)) {
            Log.e(TAG, "设置特征通知失败");
            Toast.makeText(this, "无法设置蓝牙通知", Toast.LENGTH_SHORT).show();
            return;
        }

        // 设置描述符
        BluetoothGattDescriptor descriptor = mNeedCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            if (!mBtGatt.writeDescriptor(descriptor)) {
                Log.e(TAG, "写入描述符失败");
            }
        } else {
            Log.e(TAG, "未找到通知描述符");
        }

        Log.i(TAG, "特征通知设置完成");
    }


    //检查权限相关
    private boolean checkBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }
    //检查权限相关
    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                requestPermissions(
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        BLUETOOTH_PERMISSION_REQUEST_CODE
                );
            } catch (Exception e) {
                Log.e(TAG, "请求蓝牙权限失败", e);
                Toast.makeText(this, "无法请求蓝牙权限", Toast.LENGTH_SHORT).show();
            }
        }
    }


    /**初始化控件
     * @ mTvReceive，find id
     * @ waveShowView,find 心电绘图区域id
     * @ txtECG，更新显示心电数值
     * */
    private void initUI() {
        waveShowView = findViewById(R.id.waveShowView);
        txtECG = findViewById(R.id.txtECG);
        Bitchar = findViewById(R.id.Bitchar);
    }

    /**处理接收到的数据点**/
    private void processDataPoints(ArrayList<Double> dataPoints) {
        // 保存原始数据
        for (Double point : dataPoints) {
            offlineRateOrginateData.add(point);
            offlineRateData.add(point);
        }

        // 绘制波形
        for (Double point : dataPoints) {
            waveShowView.showLine(point);
        }

        // 更新UI显示接收到的数据点数量
        Bitchar.setText("接收点: " + offlineRateOrginateData.size());

        // 更新心率显示
        // txtECG.setText(HeartratelistInt + " BPM");
    }


    /**蓝牙服务回调，即建立通信**/
    private final BluetoothGattCallback mBtGattCallback = new BluetoothGattCallback() {

        @SuppressLint("MissingPermission")  //这句可以忽略下面的需要权限检查
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "蓝牙已连接，开始发现服务");
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "蓝牙已断开");
            }
        }

        /**发现服务回调**/
        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "服务发现成功");
                runOnUiThread(() -> {
                    setupNotification(gatt); // 在这里设置通知
                    setupCharacteristicNotification();// 服务发现成功后设置特征通知
                });
            } else {
                Log.e(TAG, "服务发现失败: " + status);
            }
        }

        /**发现服务，在设备连接成功后调用，扫描到设备服务后调用此方法。
         * 调用mBluetoothGatt.discoverServices();方法后，onServicesDiscovered（）这个方法会被调用，说明发现当前设备了。
         * 然后可以在里面去获取BluetoothGattService和BluetoothGattCharacteristic。
         **/
        /*     //20250619注释掉
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
        }*/


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

        /**数据接收回调**/
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            // 确保是需要的特征
            if (!characteristic.getUuid().toString().equals(SERVICE_EIGENVALUE_READ)) {
                return;
            }

            byte[] data = characteristic.getValue();
            if (data == null || data.length == 0) {
                Log.w(TAG, "收到空数据");
                return;
            }
            // 打印原始数据用于调试
            Log.d(TAG, "收到数据, 长度: " + data.length);
            Log.d(TAG, "原始数据: " + Arrays.toString(data));
            // 转换为十六进制字符串
            String hexString = DataFormatUtil.arrayToHex(data);
            Log.d(TAG, "十六进制数据: " + hexString);
            // 解析数据
            ArrayList<Double> dataPoints = DataFormatUtil.hexToList(data);
            if (dataPoints == null || dataPoints.isEmpty()) {
                Log.w(TAG, "数据解析失败");
                return;
            }
            Log.d(TAG, "解析出数据点: " + dataPoints.size());

            // 在主线程更新UI
            runOnUiThread(() -> processDataPoints(dataPoints));
        }


    };

    private void handleConnectionFailure() {
        runOnUiThread(() -> {
            Toast.makeText(ECGChart.this, "蓝牙连接失败", Toast.LENGTH_LONG).show();
            disconnectGatt();
            finish();
        });
    }
    @SuppressLint("MissingPermission")
    private void disconnectGatt() {
        if (mBtGatt == null) return;

        mConnectionState = ECGChart.ConnectionState.DISCONNECTING;
        try {
            mBtGatt.disconnect();
            mBtGatt.close();
        } catch (Exception e) {
            Log.e(TAG, "断开连接时出错", e);
        } finally {
            mBtGatt = null;
            mConnectionState = ECGChart.ConnectionState.DISCONNECTED;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 先检查权限
        if (!checkBluetoothPermission()) {
            requestBluetoothPermissions();
            return;
        }

        // 取消通知
        if (mBtGatt != null) {
            try {
                mBtGatt.disconnect();
                mBtGatt.close();
            } catch (Exception e) {
                Log.e(TAG, "关闭Gatt连接时出错", e);
            }
            mBtGatt = null;
        }
    }

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