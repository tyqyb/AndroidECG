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
import java.util.LinkedList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import USTB.AAIST.view.DrawLine;
import USTB.AAIST.view.DrawLine2;
import USTB.AAIST.utils.ChartColors;
import USTB.AAIST.DataProcessor;
import android.os.SystemClock;
import androidx.core.content.pm.PermissionInfoCompat;
import android.bluetooth.BluetoothGattCallback;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Arrays;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import java.util.List;
import android.Manifest;

public class Recdata extends AppCompatActivity {
    private static final String TAG = "Recdata";//调试输出常量
    // 蓝牙相关常量
    private static final UUID CCC_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");//服务特征值匹配描述符
    private static final String SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";//特征值的服务UUID
    private static final String CHARACTERISTIC_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";//特征值UUID
    private BluetoothDevice mDevice;
    private MyBluetoothManager myBluetoothManager;
    private ConnectionState mConnectionState = ConnectionState.DISCONNECTED;
    private String mDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mNotifyCharacteristic;//第一个特征值引用
    // 绘图视图
    private DrawLine mDrawLine1;
    private DrawLine2 mDrawLine2;
    // 数据队列用于绘图
    private final LinkedList<Float> mUriDataQueue = new LinkedList<>();    // URI数据队列
    private final LinkedList<Float> mGluDataQueue = new LinkedList<>();    // GLU数据队列
    private final LinkedList<Float> mUriCacheQueue = new LinkedList<>();    // URI原始数据缓存
    private final LinkedList<Float> mGluCacheQueue = new LinkedList<>();    // GLU原始数据缓存
    private final LinkedList<Float> mProcessedUriDataQueue = new LinkedList<>();    // 处理后的URI数据
    private final LinkedList<Float> mProcessedGluDataQueue = new LinkedList<>();    // 处理后的GLU数据
    private static final int MAX_QUEUE_POINTS = 200; // 绘图队列最大点数
    private static final int MAX_CACHE_POINTS = 1000; // 缓存队列最大点数
    private DataProcessor mDataProcessor; // DataProcessor实例
    //数据接收缓冲区
    private StringBuilder mReceiveBuffer = new StringBuilder();
    private static final String LINE_END = "\r\n";
    // UI组件
    private TextView mDataDisplay;
    private TextView mValueDisplay;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    // 添加时间戳跟踪
    private long startTime = 0; // 数据接收开始时间
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
        mDataProcessor = new DataProcessor();// 初始化DataProcessor

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

        // 设置DrawLine1（血糖图表）的颜色
        mDrawLine1.setChartColorsWithAlpha(
                ChartColors.GLU_LINE_COLOR,
                ChartColors.GLU_FILL_COLOR,
                ChartColors.GLU_POINT_COLOR,
                ChartColors.GLU_FILL_ALPHA
        );

        // DrawLine2已经在自己的initDrawLine2Colors()中设置了颜色，如果需要覆盖，可以在这里再次设置，包括刻度线颜色，方法通line1

        // 根据数据类型设置更合理的初始范围
        mDrawLine1.setYRange(100, 600, true);
        mDrawLine1.setDataLabel("血糖(GLU)");  // chart1 显示血糖

        mDrawLine2.setYRange(3, 15, true);
        mDrawLine2.setDataLabel("尿酸(URI)");  // chart2 显示尿酸

        // 启用小网格线显示
        mDrawLine1.setShowMinorGrid(true);
        mDrawLine2.setShowMinorGrid(true);

        // 设置主副刻度线数量（5条主网格线，每个主网格线之间有4条小网格线）
        mDrawLine1.setGridLines(5, 4);
        mDrawLine2.setGridLines(5, 4);

        // 启用自动范围调整，但设置合理的限制
        mDrawLine1.setAutoAdjustRange(true);
        mDrawLine1.setAutoAdjustEnabled(true);
        mDrawLine2.setAutoAdjustRange(true);
        mDrawLine2.setAutoAdjustEnabled(true);

        // 初始化绘图参数
        mDrawLine1.setMaxPoints(MAX_QUEUE_POINTS);
        mDrawLine2.setMaxPoints(MAX_QUEUE_POINTS);

        // 添加触摸监听，支持双指缩放
        setupChartTouchListeners();
    }

    // 触摸监听设置
    private void setupChartTouchListeners() {
        // 可以为图表添加双击重置等手势
        mDrawLine1.setOnClickListener(v -> {
            mDrawLine1.resetZoom();// 双击重置缩放
        });
        mDrawLine2.setOnClickListener(v -> {
            mDrawLine2.resetZoom();
        });
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
                Log.d(TAG, "特征值读取成功");
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (status != BluetoothGatt.GATT_SUCCESS) {
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
            UUID charUuid = descriptor.getCharacteristic().getUuid();

            String charName = charUuid.equals(UUID.fromString(CHARACTERISTIC_UUID)) ? "特征值" : "未知特征";
            Log.d(TAG, "描述符写入状态: " + status + ", 特征: " + charName + ", UUID: " + charUuid);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            try {
                UUID charUuid = characteristic.getUuid();
                if (charUuid.equals(UUID.fromString(CHARACTERISTIC_UUID))) {
                    byte[] data = characteristic.getValue();
                    if (data != null && data.length > 0) {
                        String chunk = new String(data, "UTF-8");          // 不 trim，保留原始字符
                        Log.d(TAG, "收到原始数据: " + bytesToHex(data));
                        Log.d(TAG, "收到数据块: " + chunk);

                        mReceiveBuffer.append(chunk);// 追加到缓冲区

                        // 循环提取所有完整行（以 \r\n 结尾）
                        int endIndex;
                        while ((endIndex = mReceiveBuffer.indexOf(LINE_END)) != -1) {
                            String line = mReceiveBuffer.substring(0, endIndex);
                            mReceiveBuffer.delete(0, endIndex + LINE_END.length());

                            parseSensorData(line.trim());// 解析完整行（trim 可去除首尾空白，如多余的 \r）
                        }
                    } else {
                        Log.w(TAG, "收到空数据或长度为0的数据");
                    }
                } else {
                    Log.w(TAG, "收到未知特征值的数据: " + charUuid);
                }
            } catch (Exception e) {
                Log.e(TAG, "处理特征值变化时出错", e);
                if (characteristic.getValue() != null) {
                    Log.e(TAG, "错误数据(HEX): " + bytesToHex(characteristic.getValue()));
                }
            }
        }
    };

    //添加此代码便可不需要重复的权限检查
    @SuppressLint("MissingPermission")
    private void setupNotification(BluetoothGatt gatt) {
        BluetoothGattService service = gatt.getService(UUID.fromString(SERVICE_UUID));
        if (service == null) {
            Log.e(TAG, "未找到服务: " + SERVICE_UUID);
            return;
        }
        // 设置特征值的通知
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
    }


    /**
     *  数据提取与绘图核心代码
     *  尿酸(URI) → mDataDisplay → DrawLine1
     *  血糖(GLU) → mValueDisplay → DrawLine2
     * **/
    private void parseSensorData(String dataString) {
        try {
            Pattern pattern = Pattern.compile("URI_RAW=(\\d+),GLU_RAW=(\\d+)");   // 正则表达式提取尿酸和葡萄糖传感数据的ADC采样值
            Matcher matcher = pattern.matcher(dataString);

            //数据缓存与软件UI更新
            if (matcher.find() && matcher.groupCount() == 2) {
                float uriValue = Float.parseFloat(matcher.group(1));    // Group 1：原始URI的ADC采集值
                float gluValue = Float.parseFloat(matcher.group(2));    // Group 2：原始GLU的ADC采集值
                Log.d(TAG, String.format("解析成功: URI=%.3f, GLU=%.3f", uriValue, gluValue));

                // 使用DataProcessor处理和缓存数据
                if (mDataProcessor != null) {
                    mDataProcessor.processAndCache(uriValue, gluValue);
                }

                // 更新UI
                runOnUiThread(() -> {
                    // 获取当前时间（秒）
                    if (startTime == 0) {
                        startTime = System.currentTimeMillis();
                    }
                    float seconds = (System.currentTimeMillis() - startTime) / 1000.0f;

                    // ============ 从DataProcessor获取处理后的数据 ============
                    float[] processedUriArray = mDataProcessor.getProcessedUriData();
                    float[] processedGluArray = mDataProcessor.getProcessedGluData();

                    float processedUri = 0f;
                    float processedGlu = 0f;

                    if (processedUriArray.length > 0) {
                        processedUri = processedUriArray[processedUriArray.length - 1];
                    }
                    if (processedGluArray.length > 0) {
                        processedGlu = processedGluArray[processedGluArray.length - 1];
                    }

                    // ============ 0.选择数据源进行绘图 ============
                    // a.使用原始数据绘制曲线（取消下面的注释，并注释掉方法b）
/*                    float uriToPlot = uriValue;
                    float gluToPlot = gluValue;
                    Log.d(TAG, String.format("使用原始数据绘图: URI=%.3f, GLU=%.3f", uriToPlot, gluToPlot));*/

                    // b.使用处理后的数据绘制曲线（取消下面的注释，并注释掉方法a）
                    float uriToPlot = processedUri;
                    float gluToPlot = processedGlu;
                    Log.d(TAG, String.format("使用处理后数据绘图: URI=%.2f, GLU=%.2f", uriToPlot, gluToPlot));
                    // ============ 0.选择数据源进行绘图 ============

                    //步骤0和1要对应分别同步进行注释/解注

                    // ============ 1.文本显示 ============
                    // 取消注释,使用原始数据显示
/*                    mDataDisplay.setText(String.format(Locale.getDefault(), "%.2f", uriValue));
                    mValueDisplay.setText(String.format(Locale.getDefault(), "%.2f", gluValue));*/
                    // 取消注释,使用处理后数据显示
                    mDataDisplay.setText(String.format(Locale.getDefault(), "%.1f", processedUri));
                    mValueDisplay.setText(String.format(Locale.getDefault(), "%.1f", processedGlu));
                    // ============ 1.文本显示 ============

                    // 2. 更新数据队列 - URI队列用于DrawLine1，GLU队列用于DrawLine2
                    updateDataQueue(mUriDataQueue, uriToPlot, MAX_QUEUE_POINTS);
                    updateDataQueue(mGluDataQueue, gluToPlot, MAX_QUEUE_POINTS);

                    // 3. 绘制曲线 - DrawLine1绘制尿酸，DrawLine2绘制血糖
                    mDrawLine1.addDataPoint(gluToPlot, seconds);
                    mDrawLine2.addDataPoint(uriToPlot, seconds);

                    // ============ 缓存数据用于后续分析 ============
                    // 缓存原始数据，便于后续分析和回滚
/*                    updateDataQueue(mUriCacheQueue, uriValue, MAX_CACHE_POINTS);
                    updateDataQueue(mGluCacheQueue, gluValue, MAX_CACHE_POINTS);*/
                    // 缓存处理后的数据
                    updateDataQueue(mProcessedUriDataQueue, processedUri, MAX_CACHE_POINTS);
                    updateDataQueue(mProcessedGluDataQueue, processedGlu, MAX_CACHE_POINTS);
                    // ============ 缓存数据用于后续分析 ============

                    // 启用自动调整，图表根据数据动态调整范围
                    mDrawLine1.setAutoAdjustRange(true);
                    mDrawLine2.setAutoAdjustRange(true);

                    // ============ 添加数据日志 ============
                    Log.d(TAG, String.format("数据对比 - 原始URI:%.2f, 处理URI:%.2f", uriValue, processedUri));
                    Log.d(TAG, String.format("数据对比 - 原始GLU:%.2f, 处理GLU:%.2f", gluValue, processedGlu));
                });
            } else {
                Log.w(TAG, "数据格式不匹配: " + dataString);
            }
        } catch (Exception e) {
            Log.e(TAG, "解析传感器数据失败: " + dataString, e);
        }
    }

    private void updateDataQueue(LinkedList<Float> queue, float value, int maxSize) {
        if (queue == null) {queue = new LinkedList<>();}
        if (queue.size() >= maxSize) {queue.removeFirst();}
        queue.addLast(value);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

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