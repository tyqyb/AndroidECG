package USTB.AAIST;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import USTB.AAIST.adapter.DevicesAdapterList;
import USTB.AAIST.devicesdata.Devices;
import android.bluetooth.BluetoothManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class BLE extends AppCompatActivity implements View.OnClickListener {
    // 蓝牙相关常量
    private static final UUID CCC_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");//指标1服务特征值匹配描述符
    private static final String SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";//特征值1的服务UUID
    private static final String CHARACTERISTIC_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";//指标1特征值UUID
    private static final UUID CCC_DESCRIPTOR_UUID2 = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");//指标2服务特征值匹配描述符，同1
    private static final String SERVICE_UUID2 = "0000fff0-0000-1000-8000-00805f9b34fb";//特征值2的服务UUID
    private static final String CHARACTERISTIC_UUID2 = "0000fff1-0000-1000-8000-00805f9b34fb"; // 指标2特征值UUID

    private static final int PERMISSION_REQUEST_BLUETOOTH_CONNECT = 102;
    private static final String TAG = "BLE_DEBUG";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 1001; // 可以是任意唯一整数
    private BluetoothAdapter mBtAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private DevicesAdapterList mDeviceAdapter;
    private final List<Devices> mDevices = new ArrayList<>();
    private final Map<String, Devices> mDeviceMap = new HashMap<>(); // 设备去重映射
    private BluetoothGatt mBtGatt;
    private boolean mScanning = false;
    private Handler mHandler = new Handler();
    private TextView mTvState;
    private SwitchCompat btSwitch;
    private ListView listView;
    private Button btnRefresh;
    private String mConnectedDeviceAddress;
    private MyBluetoothManager myBluetoothManager;// 添加 MyBluetoothManager 引用

    @RequiresApi(api = Build.VERSION_CODES.S)
    private final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);

        // 确保在任何操作前初始化 MyBluetoothManager
        myBluetoothManager = MyBluetoothManager.getInstance(getApplicationContext());
        if (myBluetoothManager == null) {
            Log.e(TAG, "MyBluetoothManager instance is null!");
            Toast.makeText(this, "蓝牙管理器初始化失败", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 检查蓝牙状态
        if (mBtAdapter != null && mBtAdapter.isEnabled()) {
            startScan(); // 如果蓝牙已开启，自动开始扫描
        }

        // 保持UI初始化不变
        btSwitch = findViewById(R.id.st_main_blue);
        btSwitch.setChecked(false); // 强制设置为关闭状态
        listView = findViewById(R.id.discover_device_list);
        btnRefresh = findViewById(R.id.button_refresh);//刷新设备列表
        mTvState = findViewById(R.id.tv_state);

        // 设备列表适配器
        mDeviceAdapter = new DevicesAdapterList(this, mDevices);
        listView.setAdapter(mDeviceAdapter);

        initBluetoothManager();// 初始化蓝牙管理器
        setupListeners();// 设置UI事件监听
        checkPermissions();// 检查并请求权限
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 确保单例已初始化
        if (myBluetoothManager == null) {
            myBluetoothManager = MyBluetoothManager.getInstance(getApplicationContext());
        }
        // 更新UI状态
        if (myBluetoothManager != null && myBluetoothManager.isConnected()) {
            updateConnectionState("已连接");
        } else {
            updateConnectionState("未连接");
        }
    }

    //重新初始化单例
    private void initBluetoothManager() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            mBtAdapter = bluetoothManager.getAdapter();
        }
        btSwitch.setChecked(false);// 设置开关初始状态为关闭（覆盖蓝牙适配器的实际状态）
        // 检查设备是否支持BLE
        if (mBtAdapter == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // 初始化蓝牙扫描器
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBluetoothLeScanner = mBtAdapter.getBluetoothLeScanner();
        }
        btSwitch.setChecked(mBtAdapter.isEnabled());// 恢复蓝牙开关状态
    }

    private void setupListeners() {
        // 蓝牙开关监听
        btSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                enableBluetooth();
            } else {
                stopScan();
                //disconnectGatt();
                clearDeviceList();
                updateConnectionState("蓝牙已关闭");
            }
        });

        // 设备点击监听
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Devices device = mDevices.get(position);
            connectToDevice(device);
        });

        // 刷新按钮监听，修改刷新按钮点击事件
        btnRefresh.setOnClickListener(v -> {
            if (mBtAdapter != null && mBtAdapter.isEnabled()) {
                // 启动刷新动画
                //Animation rotate = AnimationUtils.loadAnimation(this, R.anim.rotate_anim);
                //btnRefresh.startAnimation(rotate);

                stopScan();
                clearDeviceList();
                startScan();

/*                // 10秒后停止动画（与扫描时间一致）
                new Handler().postDelayed(() -> {
                    btnRefresh.clearAnimation();
                }, 10000);*/
            } else {
                Toast.makeText(this, "请先开启蓝牙", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void checkPermissions() {
        if (hasPermissions()) return;

        List<String> missingPermissions = new ArrayList<>();
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    missingPermissions.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE
            );
        }
    }

    @SuppressLint("MissingPermission")
    private void enableBluetooth() {
        if (mBtAdapter == null) return;
        // 检查并请求 BLUETOOTH_CONNECT 权限（仅 Android 12+ 需要）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        PERMISSION_REQUEST_BLUETOOTH_CONNECT
                );
                return;
            }
        }

        if (!mBtAdapter.isEnabled()) {
            // 使用标准方式请求开启蓝牙
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            startScan();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                startScan();
            } else {
                btSwitch.setChecked(false);
                Toast.makeText(this, "需要开启蓝牙才能扫描设备", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void startScan() {

        // 检查蓝牙是否开启
        if (mBtAdapter == null || !mBtAdapter.isEnabled()) {
            Toast.makeText(this, "蓝牙未开启", Toast.LENGTH_SHORT).show();
            return;
        }

        //检查权限是否已拥有
        if (!hasPermissions()) {
            checkPermissions();
            return;
        }

        if (mScanning) return;// 已经在扫描中

        // 清空旧设备列表
        clearDeviceList();
        mScanning = true;

        if (mBluetoothLeScanner != null) {
            // 使用新API扫描 (Android 5.0+)
            mBluetoothLeScanner.startScan(mScanCallback);
            Log.d(TAG, "使用新API开始扫描");
        } else {
            // 兼容旧设备
            mBtAdapter.startLeScan(mLeScanCallback);
            Log.d(TAG, "使用旧API开始扫描");
        }

        // 10秒后停止扫描
        mHandler.postDelayed(this::stopScan, 10000);
        updateConnectionState("扫描中...");
        Toast.makeText(this, "正在扫描设备...", Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("MissingPermission")
    private void stopScan() {
        if (!mScanning) return;
        mScanning = false;
        if (mBluetoothLeScanner != null) {
            mBluetoothLeScanner.stopScan(mScanCallback);
        } else if (mBtAdapter != null) {
            mBtAdapter.stopLeScan(mLeScanCallback);
        }
        updateConnectionState(mDevices.isEmpty() ? "未发现设备" : "选择设备连接");
    }

    // 新扫描回调 (API 21+)
    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            processDevice(result.getDevice());
        }};

    // 旧扫描回调 (API < 21)
    private final BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            processDevice(device);
        }
    };

    private void processDevice(BluetoothDevice device) {
        runOnUiThread(() -> {
            if (device == null) return;
            // 检查蓝牙权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            String name;
            try {
                name = device.getName();
                if (name == null) return;
            } catch (SecurityException e) {
                Log.e("Bluetooth", "No BLUETOOTH_CONNECT permission", e);// 处理权限异常
                return;
            }

            String address = device.getAddress();
            if (!mDeviceMap.containsKey(address)) {
                // 假设Devices类有相应的构造函数或使用setter方法
                Devices newDevice = new Devices();
                newDevice.setName(name);
                newDevice.setAddress(address);

                mDevices.add(newDevice);
                mDeviceMap.put(address, newDevice);
                mDeviceAdapter.notifyDataSetChanged();
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice(Devices device) {
        // 移除所有旧连接逻辑
        if (mBtGatt != null) {
            mBtGatt.disconnect();
            mBtGatt = null;
        }
        BluetoothDevice bluetoothDevice = mBtAdapter.getRemoteDevice(device.getAddress());
        mBtGatt = bluetoothDevice.connectGatt(
                this,
                false,
                mGattCallback, // 使用回调
                BluetoothDevice.TRANSPORT_LE
        );

        // 保存到单例管理器，同时传递回调 - 确保参数顺序正确
        myBluetoothManager.setConnected(
                BLE.this,    // BLE 实例
                mBtGatt,      // BluetoothGatt 实例
                mGattCallback // BluetoothGattCallback 实例
        );
        // 添加设备地址保存
        myBluetoothManager.setDeviceAddress(device.getAddress());
    }
    // Manager
    public BluetoothGatt getBluetoothGatt() {
        return mBtGatt;
    }

    // GATT回调处理，需要处理两个服务对应的特征值
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            // 先检查权限
            if (!checkBluetoothPermission()) {
                requestBluetoothPermissions();
                return;
            }
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "连接失败: " + status); //连接状态监听
                return;
            }

            runOnUiThread(() -> {
                // 添加空值检查
                if (myBluetoothManager == null) {
                    Log.e(TAG, "myBluetoothManager is null in callback");
                    myBluetoothManager = MyBluetoothManager.getInstance(BLE.this);

                    if (myBluetoothManager == null) {
                        Log.e(TAG, "Failed to initialize MyBluetoothManager in callback");
                        return;
                    }
                }

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    mBtGatt = gatt;
                    mConnectedDeviceAddress = gatt.getDevice().getAddress();
                    //传递当前回调实例
                    myBluetoothManager.setConnected(
                            BLE.this,
                            mBtGatt,
                            mGattCallback  // 传递当前回调
                    );
                    myBluetoothManager.setDeviceAddress(mConnectedDeviceAddress);// 设置设备地址

                    if (!mBtGatt.discoverServices()) {
                        Log.e(TAG, "启动服务发现失败");
                    }
                    updateConnectionState("已连接，正在发现服务...");
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    updateConnectionState("连接断开");
                    closeGatt();
                    btSwitch.setChecked(false);
                    // 通知单例断开连接
                    if (myBluetoothManager != null) {
                        myBluetoothManager.disconnect();
                    }
                }
            });
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            // 转发特征值变化事件
            if (myBluetoothManager != null) {
                myBluetoothManager.forwardCharacteristicChanged(gatt, characteristic);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                runOnUiThread(() -> {
                    Toast.makeText(BLE.this, "蓝牙连接成功", Toast.LENGTH_SHORT).show();
                    updateConnectionState("已连接");
                    enableNotificationsForCharacteristics();// 启用两个特征值的通知
                    // 跳转到选择页面
                    Intent intent = new Intent(BLE.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                });
            }
        }

        //启用两个特征值的通知
        @SuppressLint("MissingPermission")
        private void enableNotificationsForCharacteristics() {
            if (mBtGatt == null) return;

            BluetoothGattService service1 = mBtGatt.getService(UUID.fromString(SERVICE_UUID));//获取服务1
            if (service1 != null) {
                BluetoothGattCharacteristic char1 = service1.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID));//获取特征1
                if (char1 != null) {
                    mBtGatt.setCharacteristicNotification(char1, true);// 启用通知1
                    BluetoothGattDescriptor descriptor = char1.getDescriptor(CCC_DESCRIPTOR_UUID);//获取并写入CCCD描述符
                    if (descriptor != null) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        mBtGatt.writeDescriptor(descriptor);
                    }
                }
            }

            BluetoothGattService service2 = mBtGatt.getService(UUID.fromString(SERVICE_UUID2));//获取服务2
            if (service2 != null) {
                BluetoothGattCharacteristic char2 = service2.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID2));//获取特征2
                if (char2 != null) {
                    mBtGatt.setCharacteristicNotification(char2, true);// 启用通知2
                    BluetoothGattDescriptor descriptor = char2.getDescriptor(CCC_DESCRIPTOR_UUID2);//获取并写入CCCD描述符
                    if (descriptor != null) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        mBtGatt.writeDescriptor(descriptor);
                    }
                }
            }
        }
    };

    private void closeGatt() {
        if (mBtGatt != null) {
            // 检查 BLUETOOTH_CONNECT 权限
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                try {
                    mBtGatt.disconnect();
                    mBtGatt.close();
                } catch (SecurityException e) {
                    Log.e("Bluetooth", "BLUETOOTH_CONNECT permission denied", e);
                }
            } else {
                Log.e("Bluetooth", "No BLUETOOTH_CONNECT permission to close GATT");
            }
            mBtGatt = null;
            mConnectedDeviceAddress = null;
        }

        btSwitch.setChecked(false);// 关闭 GATT 后，复位 Switch 按钮状态
        myBluetoothManager.disconnect();
    }
    //清除搜索设备列表
    private void clearDeviceList() {
        mDevices.clear();
        mDeviceMap.clear();
        mDeviceAdapter.notifyDataSetChanged();
    }

    private void updateConnectionState(String state) {
        if (mTvState != null) {
            mTvState.setText("状态: " + state);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private boolean hasPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopScan();
        closeGatt();
        btSwitch.setChecked(false);// 确保退出时 Switch 复位
    }

    //处理权限回调
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            Toast.makeText(this, "已获取蓝牙权限", Toast.LENGTH_SHORT).show();
        }
        // 处理 BLUETOOTH_CONNECT 权限请求
        else if (requestCode == PERMISSION_REQUEST_BLUETOOTH_CONNECT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableBluetooth();// 权限已授予，重试启用蓝牙
            } else {
                // 权限被拒绝
                btSwitch.setChecked(false);
                Toast.makeText(this, "需要蓝牙连接权限才能开启蓝牙", Toast.LENGTH_SHORT).show();
            }
        }
        // 处理 MyBluetoothManager 可能需要的权限请求
        if (requestCode == MyBluetoothManager.BLUETOOTH_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                myBluetoothManager.disconnect();// 权限已授予，执行断开操作
            }
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

    /*以下是连接完成蓝牙后的跳转操作*/
    @Override
    public void onClick(View v) {
        // 检查是否已连接到蓝牙设备
        if (mBtGatt != null && mConnectedDeviceAddress != null) {
            // 已连接，跳转到 MainActivity，这一步在BluetoothGattCallback mGattCallback = new BluetoothGattCallback() 中已经实现了，所以这个点击事件可有可无
            /*Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish(); // 可选：关闭当前 Activity*/
        } else {

            Toast.makeText(this, "请先连接蓝牙设备", Toast.LENGTH_SHORT).show();// 未连接，保持原有的点击事件处理
        }
    }

    public boolean isBluetoothConnected() {
        return mBtGatt != null && mConnectedDeviceAddress != null;
    }


}