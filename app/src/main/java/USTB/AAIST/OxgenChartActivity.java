package USTB.AAIST;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import USTB.AAIST.devicesdata.Devices;
import USTB.AAIST.utils.DataFormatUtil;

public class OxgenChartActivity extends AppCompatActivity {


    //后续可删除的控件
    private Button click;
    private Button conBlebtn;
    private TextView recTxt;
    private Context mContext;
    private BluetoothAdapter mBtAdapter;
    private BluetoothGatt mBtGatt;
    private static final String TAG = "MainActivity";
    //蓝牙的特征值，发送
    private final static String SERVICE_EIGENVALUE_SEND = "0000ffe2-0000-1000-8000-00805f9b34fb";
    //蓝牙的特征值，接收

    private final static String SERVICE_EIGENVALUE_READ = "0000ffe2-0000-1000-8000-00805f9b34fb";
    private Handler mTimeHandler = new Handler();
    private BluetoothGattCharacteristic mNeedCharacteristic;
    ArrayList<Double> res = new ArrayList<>();

    /**
     * 服务回调状态标记符
     */
    private static boolean isGattSuccess = false;
    /**
     * 查重数组
     */
    private final List<String> mDuplicateData = new ArrayList<>();

    /**
     * 设备列表（设备名称、MAC地址）
     */
    private final List<Devices> mDevices = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_oxgen_chart);
        conBlebtn = findViewById(R.id.btn);
//        recTxt = findViewById(R.id.rec);
        //可删除后续
//        click = findViewById(R.id.click);
//        click.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                startActivity(new Intent(OxgenChartActivity.this,TestViewActivity.class));
//            }
//        });




        conBlebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initBluetooth();
                scanBluetooth();

//                //获取HC-2得地址
//                设备名：HC-42=MAC地址：C0:FC:6C:C1:82:59
                BluetoothDevice device = mBtAdapter.getRemoteDevice("C0:FC:6C:C1:82:59"); //获取蓝牙MAC地址
                connectBluetooth(device, mContext); //连接蓝牙
//                Log.i(TAG, "onItemClick: 连接蓝牙:" + table.getName() + " MAC地址:" + table.getAddress());
            }
        });

    }

    /**
     * 初始化蓝牙
     */
    @SuppressLint("MissingPermission")
    public void initBluetooth() {
        //获取蓝牙默认适配器
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        //判断设备是否支持低功耗蓝牙
        if (mBtAdapter == null) {
            Log.i(TAG, "openBluetooth: 该设备不支持低功耗蓝牙");
        } else {
            //打开蓝牙
            if (!mBtAdapter.isEnabled()) {
                mBtAdapter.enable();
                Log.i(TAG, "openBluetooth: 已打开蓝牙");
            } else {
                Log.i(TAG, "initBluetooth: 蓝牙已打开");
            }
        }
    }

    /**
     * 查找低功耗蓝牙设备
     */
    @SuppressLint("MissingPermission")
    private void scanBluetooth() {
        Log.i(TAG, "scanBluetooth: 搜索低功耗蓝牙");
        mBtAdapter.startLeScan(mBtLeScanCallback);

        Log.i(TAG, "scanBluetooth: 搜索结束");
    }

    /**
     * 扫描回调
     */
    private final BluetoothAdapter.LeScanCallback mBtLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

            //跳过设备名字为空的蓝牙
            if (device.getName() == null) return;

            //将蓝牙设备添加进Devices集合
            Devices tmp = new Devices();
            tmp.setName(device.getName());
            tmp.setAddress(device.getAddress());
            String str = device.getAddress();
            //查重
            if (!mDuplicateData.contains(str)) {
                mDuplicateData.add(str);
                mDevices.add(tmp);
            }
            Log.i(TAG, "onLeScan:设备数组包含的数据:" + mDevices);
        }
    };


    /**
     * 连接蓝牙
     *
     * @param device  目标设备
     * @param context 上下文对象
     */
    @SuppressLint("MissingPermission")
    private void connectBluetooth(BluetoothDevice device, Context context) {
        Log.i(TAG, "connectBluetooth: 关闭蓝牙搜索");
        this.mContext = context;
        //关闭蓝牙搜索，连接蓝牙之前关闭蓝牙搜索，因为搜索过程非常耗电。
        mBtAdapter.stopLeScan(mBtLeScanCallback);
        //设置延迟，保证搜索完全关闭，再开始连接蓝牙。
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "run: 连接蓝牙");
                //连接蓝牙：autoConnect（布尔值，指示是否在可用时自动连接到BLE设备）
                mBtGatt = device.connectGatt(context, false, mBtGattCallback);
            }
        }, 1000);
    }

    /**
     * 蓝牙服务回调（建立通信）
     */
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
//                        mTvState.setText(getString(R.string.connection_succeeded));
//                    }
//                });
                Log.i(TAG, "onConnectionStateChange: 连接成功");

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                //关闭回调服务（等于断开蓝牙连接）
                mBtGatt.close();
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        mTvState.setText(getString(R.string.connection_failed));
//                    }
//                });
                Log.i(TAG, "onConnectionStateChange: 连接失败");
            }
        }

        //发现服务，在设备连接成功后调用，扫描到设备服务后调用此方法。
        //调用mBluetoothGatt.discoverServices();方法后，onServicesDiscovered（）这个方法会被调用，说明发现当前设备了。然后我们就可以在里面去获取BluetoothGattService和BluetoothGattCharacteristic。
        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            //判断回调服务是否成功
            if (status == BluetoothGatt.GATT_SUCCESS) {
                isGattSuccess = true;
                Log.i(TAG, "onServicesDiscovered: 回调服务连接成功");
            } else {
                isGattSuccess = false; //状态标记
                Log.i(TAG, "onServicesDiscovered: 回调服务连接失败" + status);
            }

            /**
             * 模仿源代码仿写
             */
            List<BluetoothGattService> servicesLists = gatt.getServices(); //获取服务UUID并添加进集合
            Log.i(TAG, "扫描到服务的个数:" + servicesLists.size());
            int i = 0;
            //获取单个服务
            for (final BluetoothGattService servicesList : servicesLists) {
                ++i;
                Log.i(TAG, "-----------打印服务----------");
                Log.i(TAG, i + "号服务的uuid: " + servicesList.getUuid().toString());
                List<BluetoothGattCharacteristic> gattCharacteristics = servicesList
                        .getCharacteristics();//获取单个服务下的所有特征

                int j = 0;
                Log.i(TAG, "----------打印特征-----------");
                //对单个服务得特征进行打印
                for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    ++j;
                    if (gattCharacteristic.getUuid().toString().equals(SERVICE_EIGENVALUE_SEND)) {//汇承蓝牙的UUID
                        Log.i(TAG, "汇承蓝牙的UUID");
                        Log.i(TAG, i + "号服务的第" + j + "个特征" + gattCharacteristic.getUuid().toString());
                        String mServiceUUID = servicesList.getUuid().toString();
                        String mReadWriteUUID = gattCharacteristic.getUuid().toString();
                        System.out.println("mServiceUUID" + mServiceUUID + "   mReadWriteUUID" + mReadWriteUUID);
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
            }

        }


        //接收数据，若发送的数据符合通信协议，则下位机会向上位机回复相应的数据。发送的数据通过此方法获取。
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] data = characteristic.getValue(); //value为设备发送的数据，根据数据协议进行解析。
            //String str = DataFormatUtil.arrayToHex(value);
            //122个点，进行画波操作 List类型
            res = DataFormatUtil.hexToList(data);

            //经过计算得到的数据    -0.038875,-0.040125,-0.0405,-0.038125,-0.。。。。。。。。。
//            for(int i =0;i<res.size();i++){
//                System.out.print(res.get(i)+",");
//            }


//            ecg_view.setRefreshList(res);

//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    recTxt.setText(res.size()+"   ");
//                }
//            });
            //蓝牙接受到的原始16进制信号   蓝牙发送过来的数据:C9 7E BF 7E BC 7E CF 7E DD 7E EB 7E F9 7E 00 7F FF 7E。。。。。。。。。。。
//            Log.i(TAG, "onCharacteristicChanged: 蓝牙发送过来的数据:" + DataFormatUtil.arrayToHex(value));
        }
    };

    /**
     * 触发返回按钮，关闭蓝牙
     *  //断开连接
     *             mBtGatt.disconnect();
     *                Log.i(TAG, "disConnected: 断开蓝牙连接");
     */
//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if (keyCode == KeyEvent.KEYCODE_BACK) {
//            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                mBtGatt.disconnect();
//                Log.i(TAG, "disConnected: 断开蓝牙连接");
//            }
//
//        }
//        return super.onKeyDown(keyCode, event);
//    }
}