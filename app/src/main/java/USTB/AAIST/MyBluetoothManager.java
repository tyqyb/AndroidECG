package USTB.AAIST;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;
import android.bluetooth.BluetoothGattCallback;
import androidx.core.content.ContextCompat;

public class MyBluetoothManager {
    // 单例实例
    private static MyBluetoothManager instance;
    private String deviceAddress; // 添加设备地址存储
    private static final String TAG = "MyBluetoothManager";
    public static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 1001; // 可以是任意唯一整数
    private BluetoothGattCallback bluetoothGattCallback;
    // 实例变量
    private BLE bleInstance;
    private boolean isConnected = false;
    private BluetoothGatt bluetoothGatt;
    private Context context; // 添加Context引用
    private BluetoothDevice bluetoothDevice;// 新增存储BluetoothDevice对象
    private BluetoothGattCallback mExternalCallback;

    //private BluetoothGattCallback mGattCallback;

    // 添加公共方法检查连接状态
    public boolean isConnected() {
        return isConnected && bluetoothGatt != null;
    }

    // 私有构造函数
    private MyBluetoothManager(Context context) {
        this.context = context.getApplicationContext(); // 使用 Application Context 避免内存泄漏
    }

    // 设置外部回调（供Recdata使用）
    public void setExternalCallback(BluetoothGattCallback callback) {
        this.mExternalCallback = callback;
    }

    // 转发特征值变化事件
    public void forwardCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (mExternalCallback != null) {
            mExternalCallback.onCharacteristicChanged(gatt, characteristic);
        }
    }

    // 单例获取方法（需传入 Context）
    public static synchronized MyBluetoothManager getInstance(Context context) {
        if (instance == null) {
            instance = new MyBluetoothManager(context);
        }
        return instance;
    }

    public BluetoothDevice getBluetoothDevice() {
        if (bluetoothGatt != null && bluetoothGatt.getDevice() != null) {
            return bluetoothDevice;//返回存储的BluetoothDevice
        }
        return null;
    }

    // 设置连接状态
    public void setConnected(BLE bleInstance, BluetoothGatt gatt, BluetoothGattCallback callback) {
        //空白检查
        if (bleInstance == null || gatt == null) {
            Log.e(TAG, "setConnected called with null parameters");
            return;
        }
        this.bleInstance = bleInstance;
        this.bluetoothGatt = gatt;
        this.bluetoothGattCallback = callback;
        this.isConnected = true;
        // 更新设备对象
        this.bluetoothDevice = gatt.getDevice();
        if (gatt != null && gatt.getDevice() != null) {
            this.deviceAddress = gatt.getDevice().getAddress();//保存设备地址
        }

    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    // 添加设置设备地址的方法
    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    // 断开连接
    public void disconnect() {
        isConnected = false;
        if (!checkBluetoothPermission()) {
            Log.e(TAG, "蓝牙权限不足，无法断开连接");
            Toast.makeText(context, "需要蓝牙权限", Toast.LENGTH_SHORT).show();
            return;
        }

        if (bluetoothGatt != null) {
            try {
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
            } catch (Exception e) {
                Log.e(TAG, "断开连接失败", e);
            }
            bluetoothGatt = null;
        }
        bleInstance = null;
    }

    // 检查权限（使用 ContextCompat）
    private boolean checkBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Android 12 以下不需要 BLUETOOTH_CONNECT 权限
    }

    // 获取蓝牙Gatt实例
    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    // 检查连接状态
    public boolean isDeviceConnected() {
        return isConnected && deviceAddress != null && !deviceAddress.isEmpty();
    }
}