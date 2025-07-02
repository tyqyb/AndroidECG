package USTB.AAIST;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.pm.PermissionInfoCompat;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import USTB.AAIST.view.DrawLine;
import USTB.AAIST.view.DrawLine2;
import android.bluetooth.BluetoothGattCallback;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Recdata extends AppCompatActivity {
    private static final String TAG = "Recdata";//调试输出常量
    // 蓝牙相关常量
    private static final UUID CCC_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");//指标1服务特征值匹配描述符
    private static final String SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";//特征值1的服务UUID
    private static final String CHARACTERISTIC_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";//指标1特征值UUID
    private static final UUID CCC_DESCRIPTOR_UUID2 = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");//指标2服务特征值匹配描述符，同1
    private static final String SERVICE_UUID2 = "0000fff0-0000-1000-8000-00805f9b34fb";//特征值2的服务UUID
    private static final String CHARACTERISTIC_UUID2 = "0000fff1-0000-1000-8000-00805f9b34fb"; // 指标2特征值UUID
    private BluetoothDevice mDevice;
    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 1001; // 可以是任意唯一整数
    private MyBluetoothManager myBluetoothManager;
    private ConnectionState mConnectionState = ConnectionState.DISCONNECTED;
    private String mDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mNotifyCharacteristic;//第一个特征值引用
    private BluetoothGattCharacteristic mNotifyCharacteristic2;//第二个特征值引用
    private static final int BLUETOOTH_CONNECT_REQUEST_CODE = 1001;
    //数据解析相关变量
    private static final byte START_BYTE1 = 0x0A; // 起始标志1
    private static final byte START_BYTE2 = (byte) 0xFA; // 起始标志2
    private static final byte END_BYTE1 = 0x00; // 结束标志1
    private static final byte END_BYTE2 = 0x0B; // 结束标志2
    private static final int PACKET_LENGTH = 16; // 数据包长度
    // 绘图视图
    private DrawLine mDrawLine1;
    private DrawLine2 mDrawLine2;
    // 存储两个指标的历史值
    private float mPreviousValue1 = 0f;
    private float mPreviousValue2 = 0f;
    // 数据队列用于绘图
    private final LinkedList<Float> mDataQueue1 = new LinkedList<>();
    private final LinkedList<Float> mDataQueue2 = new LinkedList<>();
    private static final int MAX_DATA_POINTS = 200; // 最大存储点数
    // UI组件
    private TextView mDataDisplay;
    private TextView mValueDisplay;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private float mPreviousValue = 0f;
    // 添加时间戳跟踪
    private long startTime = 0; // 数据接收开始时间
    private long lastTimestamp = 0;
    private static final long TIMESTAMP_INTERVAL = 1000; // 1秒间隔
    // 蓝牙连接状态
    private enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recdata);
        if (getSupportActionBar()!=null){getSupportActionBar().hide();}// 隐藏标题栏
        if (!Python.isStarted()) {Python.start(new AndroidPlatform(this));}// 初始化Python环境

        myBluetoothManager = MyBluetoothManager.getInstance(getApplicationContext());// 获取蓝牙管理器实例
        myBluetoothManager.setExternalCallback(mGattCallback);//设置外部回调

        // 检查设备连接状态
        if (!myBluetoothManager.isDeviceConnected()) {
            Toast.makeText(this, "蓝牙未连接", Toast.LENGTH_SHORT).show();
            //Log.e("Recdata", "蓝牙未连接或设备地址无效");
            finish();
            return;
        }

        // 获取设备地址
        mDeviceAddress = myBluetoothManager.getDeviceAddress();
        if (mDeviceAddress == null || mDeviceAddress.isEmpty()) {
            Toast.makeText(this, "设备地址无效", Toast.LENGTH_SHORT).show();
            //Log.e("Recdata", "设备地址无效");
            finish();
            return;
        }

        // 安全获取设备对象
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            mDevice = adapter.getRemoteDevice(mDeviceAddress);
            Log.d(TAG, "连接设备地址: " + mDeviceAddress);
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, "设备地址无效: " + mDeviceAddress, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        initViews();// 初始化UI
        initChartViews();// 初始化绘图视图
        handleIntent(getIntent());// 安全获取设备信息
    }

    //初始化绘图视图
    private void initChartViews() {
        mDrawLine1 = findViewById(R.id.chartView1);
        mDrawLine2 = findViewById(R.id.chartView2);

        // 设置初始范围和自动调整
        mDrawLine1.setYRange(0, 100, true); // 指标1初始范围
        mDrawLine2.setYRange(0, 50, true);// 指标2初始范围

        // 启用自动范围调整
        mDrawLine1.setAutoAdjustRange(true);
        mDrawLine1.setAutoAdjustEnabled(true);
        mDrawLine2.setAutoAdjustRange(true); //指标2的y轴自动调整
        mDrawLine2.setAutoAdjustEnabled(true);

        // 初始化绘图参数
        mDrawLine1.setMaxPoints(MAX_DATA_POINTS);
        mDrawLine2.setMaxPoints(MAX_DATA_POINTS);
    }

    //异步断开蓝牙连接
    @SuppressLint("MissingPermission")
    private void safeDisconnectGatt() {
        if (mBluetoothGatt == null) return;
        try {
            // 1. 取消通知
            if (mNotifyCharacteristic != null) {
                mBluetoothGatt.setCharacteristicNotification(mNotifyCharacteristic, false);
            }
            // 2. 异步断开
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    mBluetoothGatt.disconnect();
                } catch (Exception e) {
                    Log.e(TAG, "断开连接异常", e);
                }
                // 延迟关闭资源
                new Handler().postDelayed(() -> {
                    try {
                        if (mBluetoothGatt != null) {
                            mBluetoothGatt.close();
                            Log.d(TAG, "Gatt资源已释放");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "关闭Gatt异常", e);
                    } finally {
                        mBluetoothGatt = null;
                    }
                }, 500);
            });
        } catch (Exception e) {
            Log.e(TAG, "断开连接异常", e);
        }
    }

    //初始化视图
    private void initViews() {
        try {
            mDataDisplay = findViewById(R.id.BitChar_NiaoSuan);
            mValueDisplay = findViewById(R.id.BitChar_HanTang);
            // 添加空检查
            if (mDataDisplay == null || mValueDisplay == null) {
                throw new IllegalStateException("未能找到必要的TextView组件");
            }
            mDataDisplay.setMovementMethod(new ScrollingMovementMethod());
        } catch (Exception e) {
            //Log.e(TAG, "初始化视图失败", e);
            Toast.makeText(this, "初始化界面失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    //处理设备地址
    private void handleIntent(Intent intent) {
        mDeviceAddress = intent.getStringExtra("DEVICE_ADDRESS");
        if (TextUtils.isEmpty(mDeviceAddress)) {
            showErrorAndFinish("未获取到设备地址");
            return;
        }

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            showErrorAndFinish("设备不支持蓝牙");
            return;
        }

        try {
            mDevice = adapter.getRemoteDevice(mDeviceAddress);
        } catch (IllegalArgumentException e) {
            showErrorAndFinish("设备地址无效: " + mDeviceAddress);
            return;
        }

        Log.d(TAG, "连接设备地址: " + mDeviceAddress);
    }

    //错误检查
    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, message);
        finish();
    }

    private void handlePermissionError() {
        runOnUiThread(() -> {
            Toast.makeText(this, "需要蓝牙权限才能继续", Toast.LENGTH_LONG).show();
            disconnectGatt();
            finish();
        });
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        //增强回调处理：
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (characteristic.getUuid().equals(UUID.fromString(CHARACTERISTIC_UUID2))) {
                    processReceivedData(characteristic.getValue(), 2);
                }
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                //Log.e(TAG, "连接错误，状态码: " + status);
                handleConnectionFailure();
                return;
            }
            runOnUiThread(() -> {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    mConnectionState = ConnectionState.CONNECTED;
                    Log.i(TAG, "已连接到设备");
                    try {
                        gatt.discoverServices();
                    } catch (SecurityException e) {
                        Log.e(TAG, "缺少权限", e);
                        handlePermissionError();
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    mConnectionState = ConnectionState.DISCONNECTED;
                    Log.i(TAG, "设备已断开");
                    cleanup();
                }
            });
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "服务发现失败: " + status);
                handleConnectionFailure();
                return;
            }
            Log.d(TAG, "服务发现成功");
            setupNotification(gatt);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            //Log.d(TAG, "描述符写入状态: " + status + ", UUID: " + descriptor.getCharacteristic().getUuid());
            UUID charUuid = descriptor.getCharacteristic().getUuid();
            String charName = charUuid.equals(UUID.fromString(CHARACTERISTIC_UUID)) ? "特征1" :
                    charUuid.equals(UUID.fromString(CHARACTERISTIC_UUID2)) ? "特征2" : "未知特征";

            Log.d(TAG, "描述符写入状态: " + status + ", 特征: " + charName + ", UUID: " + charUuid);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            //调试日志
            //Log.d(TAG, "收到数据 UUID: " + characteristic.getUuid());
            //Log.d(TAG, "数据内容: " + Arrays.toString(characteristic.getValue()));

            try {
                UUID charUuid = characteristic.getUuid();
                //Log.d(TAG, "特征值变化: " + charUuid + " 数据长度: " + characteristic.getValue().length);

                if (charUuid.equals(UUID.fromString(CHARACTERISTIC_UUID))) {//处理服务1特征值1下接收到的第一通道数据
                    byte[] data = characteristic.getValue();
                    // 添加详细日志
                    //Log.d(TAG, "指标1原始数据: " + Arrays.toString(data));
                    String ascii = bytesToAscii(data);
                    //Log.d(TAG, "指标1的ASCII: " + ascii);
                    processReceivedData(characteristic.getValue(), 1);

                } else if (charUuid.equals(UUID.fromString(CHARACTERISTIC_UUID2))) {//处理服务2特征值2下接收到的第二通道数据
                    byte[] data = characteristic.getValue();
                    Log.d(TAG, "特征2原始数据(HEX): " + bytesToHex(data));
                    // 添加详细日志
                    Log.d(TAG, "指标2原始数据: " + Arrays.toString(data));
                    String ascii = bytesToAscii(data);
                    Log.d(TAG, "指标2的ASCII: " + ascii);
                    processReceivedData(characteristic.getValue(), 2);
                } else {
                    Log.w(TAG, "未知特征值: " + charUuid);
                }
            } catch (Exception e) {
                Log.e(TAG, "处理特征值变化时出错", e);
            }
        }
    };

    @SuppressLint("MissingPermission")//添加此代码便可不需要重复的权限检查
    private void setupNotification(BluetoothGatt gatt) {
        // 打印所有服务和特征值用于调试
/*        for (BluetoothGattService service : gatt.getServices()) {
            Log.d(TAG, "发现服务: " + service.getUuid());
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                Log.d(TAG, "特征值: " + characteristic.getUuid() + " 属性: " + characteristic.getProperties());
            }
        }*/

        BluetoothGattService service = gatt.getService(UUID.fromString(SERVICE_UUID));
        BluetoothGattService service2 = gatt.getService(UUID.fromString(SERVICE_UUID2));
        if (service == null) {
            Log.e(TAG, "未找到服务1: " + SERVICE_UUID );
            return;
        } else if (service2 == null) {
            Log.e(TAG, "未找到服务2: " + SERVICE_UUID2 );
            return;
        }

        // 设置第一个特征值的通知，指标1
        mNotifyCharacteristic = service.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID));
        if (mNotifyCharacteristic != null) {
            // 配置CCC描述符
            gatt.setCharacteristicNotification(mNotifyCharacteristic, true);// 设置通知
            BluetoothGattDescriptor descriptor = mNotifyCharacteristic.getDescriptor(CCC_DESCRIPTOR_UUID);
            if (descriptor != null) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
            }else {
                Log.w(TAG, "未找到CCC描述符，尝试直接启用通知");
                gatt.setCharacteristicNotification(mNotifyCharacteristic, true);//直接启用通知
            }
        } else {
            Log.e(TAG, "未找到特征: " + CHARACTERISTIC_UUID);
        }

        // 设置第二个特征值的通知，指标2
        mNotifyCharacteristic2 = service2.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID2));
        if (mNotifyCharacteristic2 == null) {
            Log.e(TAG, "未找到特征2: " + CHARACTERISTIC_UUID2);//避免空指针
            return;
        }

        // 检查特征值属性
        int properties = mNotifyCharacteristic2.getProperties();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0) {
            Log.e(TAG, "特征2不支持通知属性！");
            return;
        }

        // 启用通知并写入描述符
        if (!gatt.setCharacteristicNotification(mNotifyCharacteristic2, true)) {
            Log.e(TAG, "无法启用特征2通知");
        }

        if (mNotifyCharacteristic2 != null) {
            gatt.setCharacteristicNotification(mNotifyCharacteristic2, true);
            BluetoothGattDescriptor descriptor2 = mNotifyCharacteristic2.getDescriptor(CCC_DESCRIPTOR_UUID2);
            if (descriptor2 != null) {
                descriptor2.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor2);
            }
        } else {
            Log.w(TAG, "未找到特征2: " + CHARACTERISTIC_UUID2);//能找到服务2和特征值2
            return; // 添加错误返回
        }


        // 获取并写入描述符
        BluetoothGattDescriptor descriptor2 = mNotifyCharacteristic2.getDescriptor(CCC_DESCRIPTOR_UUID2);
        if (descriptor2 == null) {
            Log.e(TAG, "未找到特征2的CCC描述符");//调试输出正常
            return;
        }

        descriptor2.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        if (!gatt.writeDescriptor(descriptor2)) { // 关键修改：检查写入结果
            Log.e(TAG, "写入特征2的CCC描述符失败");//调试输出异常，写入失败
        } else {
            Log.d(TAG, "已发起特征2描述符写入请求");
        }

    }

    /**
     * 以下是数据处理相关代码，在这里更改不同的解析字节
     * **/
    private void processReceivedData(byte[] data, int dataType) {
        if (data == null || data.length == 0) {
            Log.w(TAG, "收到空数据");
            return;
        }
/*
        // 调试输出
        Log.d(TAG, "收到数据 - 类型" + dataType + " 长度: " + data.length + " 内容: " + Arrays.toString(data));

        // 直接处理为十六进制整数值
        StringBuilder hexBuilder = new StringBuilder();
        for (byte b : data) {
            hexBuilder.append(String.format("%02X", b & 0xFF));
        }
        String hexString = hexBuilder.toString().trim();
        Log.d(TAG, "HEX原始数据: " + hexString);*/

        // 检查起始标志
        if (data[0] != START_BYTE1 || data[1] != START_BYTE2) {
            //Log.w(TAG, "起始标志错误");
            return;
        }

        // 检查结束标志
        if (data[14] != END_BYTE1 || data[15] != END_BYTE2) {
            //Log.w(TAG, "结束标志错误");
            return;
        }
        int Index1Value = ((data[5] & 0xFF) | ((data[6] & 0xFF) << 8));// 解析数据结果（字节5和6，低字节在前）
        int Index2Value = ((data[11] & 0xFF) | ((data[12] & 0xFF) << 8));// 解析数据结果（字节5和6，低字节在前）//逻辑很简单，直接解析不同的数据位
        Log.d(TAG, "类型" + dataType + "值:" + Index2Value);
        // 添加合理范围检查（0-3000）
        if (Index1Value >= 0 && Index1Value <= 3000) {
            //Log.d(TAG, "解析成功 - 类型" + dataType + "值:" + Index1Value);
            updateUIWithValue(Index1Value,Index2Value,dataType);
        } else {
            //Log.w(TAG, "值超出范围: " + Index1Value);
        }
    }

    /**
     * 统一更新UI
     * 这里是通过@dataType参数实现的更新不同通道数据，如果是1则更新指标1，是2则更新指标2，其中，1、2分别为服务特征值的UUID
     * 使用不同的特征值来区分不同的数据通道是GATT架构最自然、最标准的使用方式
     * 另外的一种办法便是同一个特征值UUID的不同数据位解析得到数据进行不同指标的更新
     * 当数据相关性极强时，打包方案可能更优。比如九轴姿态传感器（加速度+陀螺仪+磁力计），厂商常把9个int16打包进一个特征值。这样单次通知就能获取完整姿态数据，避免三次通信延迟。
     * 如果所有通道数据打包进一个特征值，即使只有一个通道的数据更新了，也必须发送整个数据包（包含所有通道当前的数据）。这会显著增加不必要的空中传输开销，尤其当数据包较大或更新频繁但不同步时，浪费带宽和功耗。
     **/
    private void updateUIWithValue(float value, float value2, int dataType) {//原本参数只有1个value，两个特征值的话自己加的value2
        runOnUiThread(() -> {
            // 如果是第一次收到数据，记录起始时间
            if (startTime == 0) {
                startTime = System.currentTimeMillis();
            }
            float seconds = (System.currentTimeMillis() - startTime) / 1000.0f;// 使用系统时间，计算相对时间（秒）
            // 始终更新图表数据（但图表会自己处理标签）
            if (dataType == 1) {
                mValueDisplay.setText(String.format(Locale.getDefault(), "%.1f", value));//"%d", (int)value 强制转为整型格式
                //更新数据队列
                updateDataQueue(mDataQueue1, value);
                mDrawLine1.addDataPoint(value, seconds);


                mDataDisplay.setText(String.format(Locale.getDefault(), "%.1f", value2));
                //Log.d(TAG, "解析成功 - 类型" + dataType + "值:" + value2);
                // 更新数据队列
                updateDataQueue(mDataQueue2, value2);
                mDrawLine2.addDataPoint(value2, seconds);
            } else if (dataType == 2) {
                //以下代码是通过不同的特征值进行获取数据的代码实现
/*                mDataDisplay.setText(String.format(Locale.getDefault(), "%.2f", value2));
                //Log.d(TAG, "解析成功 - 类型" + dataType + "值:" + value2);
                // 更新数据队列
                updateDataQueue(mDataQueue2, value2);
                mDrawLine2.addDataPoint(value2, seconds);*/
            }
        });
    }

    private void updateDataQueue(LinkedList<Float> queue, float value) {
        if (queue.size() >= MAX_DATA_POINTS) {
            queue.removeFirst();
        }
        queue.addLast(value);
    }

    private float extractValue(String input, int dataType) {
        if (TextUtils.isEmpty(input)) {
            return (dataType == 1) ? mPreviousValue1 : mPreviousValue2;
        }

        try {
            // 尝试直接解析为浮点数
            float value = Float.parseFloat(input);
            if (dataType == 1) {
                mPreviousValue1 = value;
            } else {
                mPreviousValue2 = value;
            }
            return value;
        } catch (NumberFormatException e) {
            // 使用正则表达式提取数值
            Pattern pattern = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+");
            Matcher matcher = pattern.matcher(input);
            if (matcher.find()) {
                try {
                    float value = Float.parseFloat(matcher.group());
                    if (dataType == 1) {
                        mPreviousValue1 = value;
                    } else {
                        mPreviousValue2 = value;
                    }
                    return value;
                } catch (NumberFormatException ex) {
                    Log.w(TAG, "数值提取失败: " + matcher.group());
                }
            }
        }
        return (dataType == 1) ? mPreviousValue1 : mPreviousValue2;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

    private String bytesToAscii(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            if (b >= 32 && b <= 126) {
                sb.append((char) b);
            }
        }
        return sb.toString();
    }
    /*以上是数据处理相关代码*/

    @SuppressLint("MissingPermission")
    private void disconnectGatt() {
        if (mBluetoothGatt == null) return;
        mConnectionState = ConnectionState.DISCONNECTING;
        try {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
        } catch (Exception e) {
            Log.e(TAG, "断开连接时出错", e);
        } finally {
            mBluetoothGatt = null;
            mConnectionState = ConnectionState.DISCONNECTED;
        }
    }

    private void handleConnectionFailure() {
        runOnUiThread(() -> {
            Toast.makeText(Recdata.this, "蓝牙连接失败", Toast.LENGTH_LONG).show();
            disconnectGatt();
            finish();
        });
    }

    private void cleanup() {
        mHandler.removeCallbacksAndMessages(null);
        disconnectGatt();
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);// 先停止数据接收
        safeDisconnectGatt();// 再断开连接
        super.onDestroy();
    }

    //检查连接状态
    private void checkConnectionStatus() {
        if (mConnectionState == ConnectionState.CONNECTING) {
            Log.w(TAG, "连接超时，强制断开");
            disconnectGatt();
            showErrorAndFinish("连接超时");
        }
    }

    // 解析字节数组为浮点数列表
    private List<Float> parseFloatArray(byte[] data) {
        List<Float> values = new ArrayList<>();
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        try {
            while (buffer.remaining() >= 4) {
                values.add(buffer.getFloat());
            }
            Log.d(TAG, "解析为浮点数组: " + values);
        } catch (Exception e) {
            Log.e(TAG, "浮点数组解析失败", e);
        }
        return values;
    }

    //请求蓝牙权限
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

    //检查蓝牙权限
    private boolean checkBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }
}