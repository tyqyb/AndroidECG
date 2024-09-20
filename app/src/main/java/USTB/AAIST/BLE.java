package USTB.AAIST;
/**低功耗蓝牙连接
 * Edit By：
 * **/
/**
 * BUG描述：1.当手机蓝牙处于关闭状态时，通过软件蓝牙打开开关不会打开手机蓝牙，也不会搜索设备
 * 最主要的在BLE页面，手机蓝牙与页面蓝牙控制开关军打开后仍然不进行搜索设备，即开关打开后不进行页面设备列表的更新
 * 手机蓝牙打开后再通过软件打开蓝牙控制开关后才可搜索到设备，但返回后该开关仍然自动关闭,这是每次进入页面会进行初始化关闭
 * 点击连接某蓝牙设备后，手机不会显示蓝牙连接到该设备，且状态标记处始终为正在连接，即没有真正连接到蓝牙设备
 * 2.只能接收特定特征值的蓝牙数据,不能动态的修改蓝牙用到的UUID,故只能连接特定的BLE设备
 * 3.BLE界面显示蓝牙连接状态，在该页面添加断开蓝牙连接按钮，不在绘图界面添加，否则在那个页面断开后又无法连接
 * **/
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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import USTB.AAIST.adapter.DevicesAdapterList;
import USTB.AAIST.devicesdata.Devices;
import USTB.AAIST.utils.PermissionUtil;

import android.text.method.ScrollingMovementMethod;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import USTB.AAIST.utils.DataFormatUtil;
import android.widget.Button;
import java.util.UUID;
import android.nfc.Tag;

public class BLE extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "In BLE.Java";//Logcat日志输出的标题
    private BluetoothGatt mBtGatt;
    private BluetoothAdapter mBtAdapter;
    private DevicesAdapterList mDeviceAdapter;
    private final List<String> mDuplicateData = new ArrayList<>();//查重数组
    private final List<Devices> mDevices = new ArrayList<>();//设备名称、MAC地址
    private static boolean connectFlag = false;
    private final int mRequestCode = 0x01;//权限请求码

    private final static String SERVICE_EIGENVALUE_SEND = "0000ffe2-0000-1000-8000-00805f9b34fb";//蓝牙的特征值，发送
    private final static String SERVICE_EIGENVALUE_READ = "0000ffe2-0000-1000-8000-00805f9b34fb";//蓝牙的特征值，接收
    private BluetoothGattCharacteristic mWriteBtGattCharacteristic;
    private BluetoothGattCharacteristic mNeedCharacteristic;
    private Handler mTimeHandler = new Handler();
    private static boolean isGattSuccess = false;//服务回调状态标记符
    private EditText mEtMessage;
    private TextView mTvReceive, mTvState;
    private Context mContext;

    //权限数组
    @RequiresApi(api = Build.VERSION_CODES.S)
    private final String[] permissions = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
    };

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);

        //状态栏相关
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        //getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);//状态栏字体变暗

        //标题栏颜色
        if (getSupportActionBar() != null) {
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#107C8A")));
        }
        //状态栏颜色
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#107C8A"));


        SwitchCompat btSwitch = findViewById(R.id.st_main_blue);
        //Button btnDisConnect = findViewById(R.id.button_disconnect);//BLE布局断开连接
        ListView listView = findViewById(R.id.discover_device_list);

        int accessfilelocationCheck = ContextCompat.checkSelfPermission(BLE.this,Manifest.permission.ACCESS_FINE_LOCATION);
        int accesscoarselocationCheck = ContextCompat.checkSelfPermission(BLE.this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if(accessfilelocationCheck!=PackageManager.PERMISSION_GRANTED ||accesscoarselocationCheck!=PackageManager.PERMISSION_GRANTED){
            String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION};
            ActivityCompat.requestPermissions(BLE.this, permissions, 1);
        }else{
            Toast.makeText(BLE.this,"已拥有权限",Toast.LENGTH_LONG).show();// 有权限
        }
        //btnDisConnect.setOnClickListener(this);//BLE布局断开连接

        //蓝牙开关
        btSwitch.setChecked(false); //默认关闭
        btSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    initBluetooth();
                    scanBluetooth();
                } else {
                    Log.i(TAG, "取消扫描，清空设备列表，断开设备连接");
                    mBtAdapter.cancelDiscovery();
                    disConnected(connectFlag);
                    mDevices.clear();
                    mDeviceAdapter.notifyDataSetChanged();
                }
            }
        });

        //设备列表
        mDeviceAdapter = new DevicesAdapterList(this, mDevices);
        listView.setAdapter(mDeviceAdapter);

        //点击某一个蓝牙触发跳转事件
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Devices table = mDevices.get(position); //获取点击item所在数组中的索引
                BluetoothDevice device = mBtAdapter.getRemoteDevice(table.getAddress()); //获取蓝牙MAC地址

                //把当前的蓝牙设备地址传给心电的activity中去
                Intent tableIntent = new Intent(BLE.this, ECGChart.class);
                tableIntent.putExtra("deviceAdress", device);
                startActivityForResult(tableIntent, 0);
                Log.i(TAG, "连接蓝牙:" + table.getName() + " MAC地址:" + table.getAddress());

                //关闭蓝牙搜索，连接蓝牙之前关闭蓝牙搜索，因为搜索过程非常耗电。
                if (ActivityCompat.checkSelfPermission(BLE.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    mBtAdapter.stopLeScan(mBtLeScanCallback);
                    return;
                }
                //弹出已连接
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(BLE.this,"已连接蓝牙",Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
        //动态申请权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PermissionUtil.checkPermission(this, mRequestCode, permissions);
        }

    }

    /**初始化蓝牙*/
    @SuppressLint("MissingPermission")
    public void initBluetooth() {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();//获取蓝牙默认适配器
        //判断设备是否支持低功耗蓝牙
        if (mBtAdapter == null) {
            Log.i(TAG, "该设备不支持低功耗蓝牙！");
        } else {
            //打开蓝牙
            if (!mBtAdapter.isEnabled()) {
                mBtAdapter.enable();
                Log.i(TAG, "已打开蓝牙");
            } else {
                Log.i(TAG, "蓝牙已打开");
            }
        }
    }

    /**查找低功耗蓝牙设备**/
    @SuppressLint("MissingPermission")
    private void scanBluetooth() {
        Log.i(TAG, "搜索低功耗蓝牙");
        mBtAdapter.startLeScan(mBtLeScanCallback);
        Log.i(TAG, "搜索结束");
    }

    /**扫描结果回调**/
    private final BluetoothAdapter.LeScanCallback mBtLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (device.getName() == null) return;//跳过设备名字为空的蓝牙
            //将蓝牙设备添加进Devices集合
            Devices tmp = new Devices();
            tmp.setName(device.getName());
            tmp.setAddress(device.getAddress());
            String str = device.getAddress();
            //蓝牙查重
            if (!mDuplicateData.contains(str)) {
                mDuplicateData.add(str);
                mDevices.add(tmp);
            }
            mDeviceAdapter.notifyDataSetChanged();
        }
    };

/**后续对比考证可删除以下代码
 * 主要功能：连接蓝牙 ，已经在ECGChart中调用连接蓝牙，此处没有用到
 * 不同的是后者定义的函数没有context这一参数，没有this.mContext = context; 后续需要对比
 * @param device  目标设备；@param context 上下文对象
 */
//    @SuppressLint("MissingPermission")
//    private void connectBluetooth(BluetoothDevice device, Context context) {
//        Log.i(TAG, "在BLE.Java, connectBluetooth函数中: 关闭蓝牙搜索");
//        this.mContext = context;
//        mBtAdapter.stopLeScan(mBtLeScanCallback);//关闭蓝牙搜索，连接蓝牙之前关闭蓝牙搜索
//        //设置延迟，保证搜索完全关闭，再开始连接蓝牙。
//        Handler handler = new Handler();
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                Log.i(TAG, "在BLE.Java,run函数中: 连接蓝牙");
//                mBtGatt = device.connectGatt(context, false, mBtGattCallback);//连接蓝牙：autoConnect（布尔值，指示是否在可用时自动连接到BLE设备）
//            }
//        }, 1000);
//    }

    /**蓝牙服务回调，建立通信**/
    private final BluetoothGattCallback mBtGattCallback = new BluetoothGattCallback() {
        //开启监听，即建立与设备的通信的首发数据通道，BLE开发中只有当上位机成功开启监听后才能与下位机收发数据.开启监听成功调用此方法。
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "开启监听成功！");
            }else{
                Log.i(TAG, "没开启监听");
            }
            //跳转到心电activity
            Intent bleTocECG = new Intent(BLE.this,ECGChart.class);
            startActivity(bleTocECG);
            finish();//跳转的同时销毁程序
        }
    };


    /**断开蓝牙连接   @param b 判断蓝牙服务回调是否成功（防止空对象异常）
     * b是空的,所以在BLE界面点击断开蓝牙根本没有反应**/
    @SuppressLint("MissingPermission")
    private void disConnected(boolean connectFlag) {
        if (connectFlag) {
            mBtGatt.disconnect();//断开连接
            Log.i(TAG, "断开蓝牙连接");
        }else {
            Log.i(TAG, "connectFlag 是空的:"+ connectFlag);
        }
    }


    /**权限申请结果回调**/
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean hasPermissionDismiss = false;//是否授权标识符
        if(requestCode ==1){
            if(grantResults.length>0 &&grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "申请成功！", Toast.LENGTH_SHORT).show();
            }else{//未授权
                Toast.makeText(this, getString(R.string.please_grant_app_permission), Toast.LENGTH_SHORT).show();
            }
        }
        if (mRequestCode == requestCode) {
            for (int results : grantResults) {
                //如果有未授权权限
                if (results == -1) {
                    hasPermissionDismiss = true;
                    break;
                }
            }
            if (hasPermissionDismiss)
                Toast.makeText(this, getString(R.string.please_grant_app_permission), Toast.LENGTH_SHORT).show();
        }
    }

    /**由于父类是一个抽象类，里边有抽象方法，当子类继承父类时，需要使用该该抽象方法
     * 所以设置了一个没有用的点击事件使用该抽象方法，删除就报错，不信你试试~ **/
    @SuppressLint({"MissingPermission", "NonConstantResourceId"})
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.icon_BlueTooth:
                Toast.makeText(BLE.this, "Oops,你发现了华点,但这似乎并没有任何用处~~", Toast.LENGTH_SHORT).show();//点击蓝牙图标，正经人会点击右侧的开关而不是这个图标
                break;
        }
    }

}

