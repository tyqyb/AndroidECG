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
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import androidx.appcompat.app.AppCompatActivity;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SpeechConstant;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import USTB.AAIST.view.DrawLine;
import USTB.AAIST.view.DrawLine2;
import android.bluetooth.BluetoothGattCallback;

public class Recdata extends AppCompatActivity {
    // 蓝牙相关常量
    private static final String TAG = "Recdata";
    private static final UUID CCC_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final String SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
    private static final String CHARACTERISTIC_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";//指标1特征
    private static final String CHARACTERISTIC_UUID2 = "0000ffe2-0000-1000-8000-00805f9b34fb"; // 指标2特征

    private BluetoothDevice mDevice;
    private static final int BLUETOOTH_CONNECT_REQUEST_CODE = 1001;
    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 1001; // 可以是任意唯一整数

    // 蓝牙连接状态
    private enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING
    }

    private MyBluetoothManager myBluetoothManager;
    private ConnectionState mConnectionState = ConnectionState.DISCONNECTED;
    private String mDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mNotifyCharacteristic;//第一个特征值引用
    private BluetoothGattCharacteristic mNotifyCharacteristic2;//第二个特征值引用

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
    private float mPreviousValue = 0f;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recdata);

        // 隐藏标题栏
        if (getSupportActionBar()!=null){
            getSupportActionBar().hide();
        }

        // 初始化语音服务
        SpeechUtility.createUtility(this, SpeechConstant.APPID + "=5f16ff0d");

        // 初始化Python环境
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        // 获取蓝牙管理器实例
        myBluetoothManager = MyBluetoothManager.getInstance(getApplicationContext());

        //设置外部回调
        myBluetoothManager.setExternalCallback(mGattCallback);

        // 检查设备连接状态
        if (!myBluetoothManager.isDeviceConnected()) {
            Toast.makeText(this, "蓝牙未连接", Toast.LENGTH_SHORT).show();
            Log.e("Recdata", "蓝牙未连接或设备地址无效");
            finish();
            return;
        }

        // 获取设备地址
        mDeviceAddress = myBluetoothManager.getDeviceAddress();
        if (mDeviceAddress == null || mDeviceAddress.isEmpty()) {
            Toast.makeText(this, "设备地址无效", Toast.LENGTH_SHORT).show();
            Log.e("Recdata", "设备地址无效");
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

        initViews();

        // 直接使用现有的蓝牙连接，而不是重新连接
        mBluetoothGatt = myBluetoothManager.getBluetoothGatt();
        if (mBluetoothGatt != null) {
/*            // 重新设置回调以接收数据
            //mBluetoothGatt.setCallback(mGattCallback);
            myBluetoothManager.reconnectWithNewCallback(myBluetoothManager.getBluetoothGattCallback());
            //mBluetoothGatt.setCallback(myBluetoothManager.getBluetoothGattCallback());*/
            setupNotification(mBluetoothGatt);
        } else {
            Toast.makeText(this, "蓝牙连接不存在", Toast.LENGTH_SHORT).show();
            finish();
        }

        initViews();// 初始化UI
        initChartViews();// 初始化绘图视图
        handleIntent(getIntent());// 安全获取设备信息
    }

    private void initChartViews() {
        mDrawLine1 = findViewById(R.id.chartView1);
        mDrawLine2 = findViewById(R.id.chartView2);

        // 设置初始范围和自动调整
        mDrawLine1.setYRange(0, 100, true); // 假设初始范围0-100
       // mDrawLine2.setYRange(0, 500, true);

        // 启用自动范围调整
        mDrawLine1.setAutoAdjustRange(true);
        mDrawLine1.setAutoAdjustEnabled(true);
        //mDrawLine2.setAutoAdjustRange(true);

        // 初始化绘图参数
        mDrawLine1.setMaxPoints(MAX_DATA_POINTS);
        mDrawLine2.setMaxPoints(MAX_DATA_POINTS);
        //mDrawLine1.setLabel("汗糖值 (mMol/L)");
        //mDrawLine2.setLabel("尿酸值 (mMol/L)");
    }





    private void safeDisconnectGatt() {
        // 先检查权限
        if (!checkBluetoothPermission()) {
            requestBluetoothPermissions();
            return;
        }
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

    private boolean checkBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

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
            Log.e(TAG, "初始化视图失败", e);
            Toast.makeText(this, "初始化界面失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

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

    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, message);
        finish();
    }

    private void checkConnectionStatus() {
        if (mConnectionState == ConnectionState.CONNECTING) {
            Log.w(TAG, "连接超时，强制断开");
            disconnectGatt();
            showErrorAndFinish("连接超时");
        }
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
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (characteristic.getUuid().equals(UUID.fromString(CHARACTERISTIC_UUID2))) {
                    processReceivedData(characteristic.getValue(), 2);
                }
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            //super.onConnectionStateChange(gatt, status, newState);

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "连接错误，状态码: " + status);
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
            Log.d(TAG, "描述符写入状态: " + status + ", UUID: " + descriptor.getCharacteristic().getUuid());
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            //调试日志
            Log.d(TAG, "收到数据 UUID: " + characteristic.getUuid());
            Log.d(TAG, "数据内容: " + Arrays.toString(characteristic.getValue()));

            try {
                UUID charUuid = characteristic.getUuid();
                Log.d(TAG, "特征值变化: " + charUuid + " 数据长度: " + characteristic.getValue().length);

                if (charUuid.equals(UUID.fromString(CHARACTERISTIC_UUID))) {
                    byte[] data = characteristic.getValue();
                    // 添加详细日志
                    Log.d(TAG, "原始数据: " + Arrays.toString(data));
                    String ascii = bytesToAscii(data);
                    Log.d(TAG, "ASCII: " + ascii);
                    processReceivedData(characteristic.getValue(), 1);

                } else if (charUuid.equals(UUID.fromString(CHARACTERISTIC_UUID2))) {
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
        for (BluetoothGattService service : gatt.getServices()) {
            Log.d(TAG, "发现服务: " + service.getUuid());
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                Log.d(TAG, "特征值: " + characteristic.getUuid() +
                        " 属性: " + characteristic.getProperties());
            }
        }

        BluetoothGattService service = gatt.getService(UUID.fromString(SERVICE_UUID));
        if (service == null) {
            Log.e(TAG, "未找到服务: " + SERVICE_UUID);
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
        mNotifyCharacteristic2 = service.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID2));
        // 启用通知并写入描述符
        if (!gatt.setCharacteristicNotification(mNotifyCharacteristic2, true)) {
            Log.e(TAG, "无法启用特征2通知");
        }

        if (mNotifyCharacteristic2 != null) {
            gatt.setCharacteristicNotification(mNotifyCharacteristic2, true);
            BluetoothGattDescriptor descriptor2 = mNotifyCharacteristic2.getDescriptor(CCC_DESCRIPTOR_UUID);
            if (descriptor2 != null) {
                descriptor2.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor2);
            }
        } else {
            Log.w(TAG, "未找到特征2: " + CHARACTERISTIC_UUID2);
            return; // 添加错误返回
        }
    }

    /*以下是数据处理相关代码*/
    private void processReceivedData(byte[] data, int dataType) {
        if (data == null || data.length == 0) {
            Log.w(TAG, "收到空数据");
            return;
        }

        // 调试输出
        Log.d(TAG, "收到数据 - 类型" + dataType + " 长度: " + data.length + " 内容: " + Arrays.toString(data));

        // 直接处理为十六进制整数值
        StringBuilder hexBuilder = new StringBuilder();
        for (byte b : data) {
            hexBuilder.append(String.format("%02X", b & 0xFF));
        }
        String hexString = hexBuilder.toString().trim();
        Log.d(TAG, "HEX原始数据: " + hexString);

        // 尝试解析为整数
        try {
            // 根据您的设备协议，这里可能需要调整解析逻辑
            // 示例：假设数据包中第6-9字节是有效值 (小端序)
            if (data.length >= 10) {
                int value = ((data[5] & 0xFF) << 24) |
                        ((data[6] & 0xFF) << 16) |
                        ((data[7] & 0xFF) << 8)  |
                        (data[8] & 0xFF);

                Log.d(TAG, "解析成功 - 类型" + dataType + " HEX:" + hexString + " 值:" + value);
                updateUIWithValue(value, dataType);
            } else {
                Log.w(TAG, "数据长度不足，无法解析");
            }
        } catch (Exception e) {
            Log.e(TAG, "解析错误: " + hexString, e);
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

    // 统一更新UI
    private void updateUIWithValue(float value, int dataType) {
        runOnUiThread(() -> {
            if (dataType == 1) {
                mValueDisplay.setText(String.format(Locale.getDefault(), "%d", (int)value));
                //更新数据队列
                updateDataQueue(mDataQueue1, value);
                mDrawLine1.addDataPoint(value);
            } else if (dataType == 2) {
                //mDataDisplay.setText(String.format(Locale.getDefault(), "%.2f", value));
                // 更新数据队列
                //updateDataQueue(mDataQueue2, value);
                //mDrawLine2.addDataPoint(value);
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
}