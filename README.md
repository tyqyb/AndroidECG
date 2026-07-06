# 开发记录

## 一、整体预览

本软件项目是为适配本人研究生课题而做的Android版本手机APP以实现第二部分的预期功能，需要适配硬件检测项目使用（已完善）。注：下文中更详细的设计内容请参考本人的硕士论文相关章节（后续挂到Github）

本项目所用的开发工具为Android Studio（Version：Koala）；Gradle构建版本：8.3；Target SDK Version：34（Android 14.0）；Min SDK Version：31（Android 12.0）；

现在花费太多时间在UI设计上对于整个小项目的推进是来不及的，同时还有其他事情要做（发电器件与汗液检测实验、检测硬件等），所以UI上显得粗制滥造了些，功能简单所以UI页面也十分的简洁...

<img src=".\ReadmePic\0.jpg" alt="Splash" style="zoom:20%;" /><img src=".\ReadmePic\1.jpg" alt="SearchBLE" style="zoom:20%;" /><img src=".\ReadmePic\2.jpg" alt="SearchBLEList" style="zoom:20%;" />

<img src=".\ReadmePic\3.jpg" alt="SelectPage" style="zoom:20%;" /><img src=".\ReadmePic\4.jpg" alt="BiochemicalIndicatorsPage" style="zoom:20%;" />

当然，由于蓝牙连接代码的重构与页面之间跳转逻辑的改变，心电测量页面的跳转还没有进行修改，这是后续有时间有要求可以修复的。

> 不得不承认使用AS开发太考验人了，尤其是环境的配制、项目的构建、版本的升级、模拟器配置、真机调试等等等等，不如一些更好用的开发方式（这是对小白来讲，当然对于大型主流APP当然还是AS更加强大，软件构建自主程度高），也就是[uniapp](https://uniapp.dcloud.net.cn/quickstart.html)，后续需要的话可以考虑。

## 二、软件功能

0.实现低功耗蓝牙的搜索与稳定连接（重构版本）

1.实现心电图数据的接收与实时绘制（原有功能）

2.实现两通道汗液生化指标实时监测（新建功能）

## 三、页面说明

该部分介绍每个页面的主要功能函数的作用以及实现方式，由于软件 还在开发过程中，每个页面的功能修改由每次提交到GitHub时的描述记录：

### 软件启动页面（Splash）

该页面通过一个延时函数延时1s从 该页面跳转到BLE连接页面，主要伪代码如下：

```java
        new Handler().postDelayed(() -> {
            startActivities(new Intent[]{new Intent().setClass(this, BLE.class)});
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }, 1000);
```

> 页面布局文件位于**：res/layout/activity_splash.xml**

### 蓝牙连接页面（BLE）

> 布局文件位于：**res/layout/activity_ble.xml**和**res/layout/devices_list_inble.xml**

蓝牙的连接流程为：**权限检查 → 启用蓝牙 → 扫描设备 → 选择设备 → 建立连接 → 发现服务 → 启用通知 → 跳转主界面**，步骤总结如下：  

> 1.初始化UI和蓝牙适配器。
>
> 2.请求必要权限。
>
> 3.用户打开蓝牙开关（或蓝牙已开）则开始扫描设备。
>
> 4.扫描到的设备显示在列表中。
>
> 5.用户点击设备进行连接。
>
> 6.连接成功后，发现服务并启用特征通知。（预留了启用两个服务和两个特征并分别启用通知，为的是接收两个不同通道的数据，实际操作是**接收一个服务特征解析不同的数据位，硬件需要适配**）
>
> 7.启用通知后跳转到主界面。
>
> 8.断开连接时清理资源。

#### 1.初始化与权限检查

UI初始化与监听：

```java
        // 保持UI初始化不变
        btSwitch = findViewById(R.id.st_main_blue);
        btSwitch.setChecked(false); // 强制设置为关闭状态
        listView = findViewById(R.id.discover_device_list);
        btnRefresh = findViewById(R.id.button_refresh);//刷新设备列表
        mTvState = findViewById(R.id.tv_state);
        
        setupListeners();// 设置UI事件监听
```

蓝牙适配器初始化：

在`initBluetoothManager()`中获取系统蓝牙服务，初始化`BluetoothAdapter`和`BluetoothLeScanner`（Android 5.0+）。

```java
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
```

权限检查与请求：

在`checkPermissions()`中动态请求必要权限：

```java
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
```

若权限缺失，通过`ActivityCompat.requestPermissions()`请求。

#### 2.蓝牙开关控制

- **开启蓝牙**
  打开开关时调用`enableBluetooth()`：

  - 检查`BLUETOOTH_CONNECT`权限（Android 12+）

  - 通过`Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)`请求启用蓝牙

    ```java
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
    ```

    

- **关闭蓝牙**
  关闭开关时停止扫描、清空设备列表、更新UI状态。

```java
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopScan();
        closeGatt();
        btSwitch.setChecked(false);// 确保退出时 Switch 复位
    }
```

#### 3.设备扫描流程

**启动扫描**
`startScan()`的步骤：

> 1.检查蓝牙开启状态和权限
>
> 2.清空旧设备列表
>
> 3.根据API版本选择扫描方式：
>
> ​			Android 5.0+ : mBluetoothLeScanner.startScan(mScanCallback)
>
> ​			旧版本: mBtAdapter.startLeScan(mLeScanCallback)
>
> 4.10秒后自动停止扫描（Handler.postDelayed）

​			代码如下：

```java
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
```

**设备发现**
在`processDevice()`中处理扫描结果：

- 通过设备地址去重（`mDeviceMap`）

- 将新设备添加到列表并刷新UI

  ```java
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
  ```

  

**停止扫描**
`stopScan()`停止扫描并更新状态提示。

```java
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
```

#### 4.设备连接与通信

**连接设备**
点击列表项触发`connectToDevice()`：

```java
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
```

**连接状态回调**
在`mGattCallback`中处理：

- **连接成功**：发现服务（`discoverServices()`）

- **连接断开**：关闭GATT并更新UI

  ```java
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
  ```

  

**服务发现**
`onServicesDiscovered()`中：

1. 获取指定UUID的服务和特征

2. 跳转到主界面（`MainActivity`）

   ```java
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
   ```

#### 5.启用特征通知

注：此部分并没有通过此方法进行两个特征值的通知启用，而是直接解析一个特征值发送的数据包中不同的数据位。

**关键方法**
`enableNotificationsForCharacteristics()`：

```java
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
```

对`SERVICE_UUID`/`CHARACTERISTIC_UUID`和`SERVICE_UUID2`/`CHARACTERISTIC_UUID2`重复此操作。

####  6.连接管理

- **单例管理**
  通过`MyBluetoothManager`单例保存连接状态：

  ```java
  myBluetoothManager.setConnected(BLE.this, mBtGatt, mGattCallback);
  myBluetoothManager.setDeviceAddress(deviceAddress);
  ```

- **资源释放**
  `closeGatt()`中安全关闭连接：

  ```java
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
  ```

#### 7.异常处理

- **权限检查**
  关键操作前验证权限（如`checkBluetoothPermission()`）

  ```java
      private boolean checkBluetoothPermission() {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
              return checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
                      PackageManager.PERMISSION_GRANTED;
          }
          return true;
      }
  ```

- **错误处理**

  - 蓝牙不支持时退出Activity
  - GATT操作失败时记录错误日志
  - 安全异常捕获（`SecurityException`）

  

#### 8.生命周期管理

- **onResume**
  更新连接状态（通过单例判断）

  ```java
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
  ```

- **onDestroy**
  停止扫描、关闭GATT连接、复位UI状态

  ```java
      @Override
      protected void onDestroy() {
          super.onDestroy();
          stopScan();
          closeGatt();
          btSwitch.setChecked(false);// 确保退出时 Switch 复位
      }
  ```

#### 9.关键设计特点

1. **双服务支持**
   同时处理两个GATT服务（`SERVICE_UUID`和`SERVICE_UUID2`）的通知配置（虽然没有采用，但是设计保留了）。
2. **版本兼容**
   兼容新旧Android版本的BLE扫描API。
3. **状态同步**
   通过单例`MyBluetoothManager`跨Activity管理连接状态。
4. **权限分层**
   按需请求权限（扫描/连接/位置），处理Android 12+的特殊权限。



### 功能选择页面（MainActivity）

> 布局文件位于：**res/layout/activity_main.xml**

#### 0.完整工作流程

1. **用户进入主界面**
   - 检查是否从 BLE 页面返回（带 `target_page` 参数）
   - 若蓝牙已连接则自动跳转目标页
2. **用户选择功能**
   - 点击心电/数据接收区域
   - 系统检查蓝牙连接状态：
     - **已连接** → 直接打开对应功能页
     - **未连接** → 跳转 BLE 页（携带 `target_page` 参数）
3. **用户通过菜单连接**
   - 点击菜单蓝牙图标
   - 进入 BLE 页（无 `target_page` 参数）
   - 连接成功后返回主界面（不自动跳转）
4. **蓝牙连接后自动跳转**
   - 从功能入口跳转到 BLE 页时携带 `target_page`
   - BLE 页连接成功后触发主界面的 `handleIntent()`
   - 主界面验证连接状态后跳转目标页

#### 1. 初始化与界面设置

- **单例管理**
  在 `onCreate()` 中获取 `MyBluetoothManager` 单例实例，用于管理蓝牙连接状态：

  ```java
  myBluetoothManager = MyBluetoothManager.getInstance(getApplicationContext());
  ```

**UI 配置**

- 隐藏标题栏：`getSupportActionBar().hide()`
- 设置沉浸式状态栏：`setSystemUiVisibility()`
- 初始化功能入口控件（心电和数据接收）

#### 2. 意图处理机制

- **目标页面跳转**
  通过 `handleIntent()` 处理来自蓝牙连接页面（BLE Activity）的跳转请求：

  ```java
      private void handleIntent(Intent intent) {
          if (intent != null) {
              // 检查是否有目标页面需要跳转
              if (intent.hasExtra("target_page")) {
                  int targetPage = intent.getIntExtra("target_page", -1);
                  // 使用更可靠的isDeviceConnected() 方法，因为它不仅检查连接状态，还检查设备地址的有效性
                  if (targetPage != -1 && MyBluetoothManager.getInstance(getApplicationContext()).isDeviceConnected()) {
                      navigateToTargetPage(targetPage);
                  }
              }
          }
      }
  ```

  

  **新意图处理**
  重写 `onNewIntent()` 确保 Activity 重用时的意图处理

#### 3.功能导航逻辑

- **点击事件绑定**
  为两个功能区域设置点击监听器：

  ```java
      private void initUI() {
          cECG_Layout = findViewById(R.id.cECG_Layout);
          Recdata_Layout = findViewById(R.id.recdataid);
  
          cECG_Layout.setOnClickListener(view -> navigateToTargetPage(PAGE_ECG));// 心电点击事件
          Recdata_Layout.setOnClickListener(view -> navigateToTargetPage(PAGE_RECDATA));// 数据接收点击事件
      }
  ```

**智能导航流程**
`navigateToTargetPage()` 实现条件跳转：

1. **已连接蓝牙** → 直接跳转目标页面
   - 心电页面（ECGChart）
   - 数据接收页面（Recdata），并传递设备地址
2. **未连接蓝牙** → 跳转蓝牙连接页（BLE Activity）

```java
    private void navigateToTargetPage(int pageType) {
        if (myBluetoothManager.isConnected()) {
            Intent intent = new Intent();
            switch (pageType) {
                case PAGE_ECG:
                    intent.setClass(this, ECGChart.class);
                    break;
                case PAGE_RECDATA:
                    intent.setClass(this, Recdata.class);
                    intent.putExtra("DEVICE_ADDRESS", myBluetoothManager.getDeviceAddress());
                    break;
            }
            startActivity(intent);
        } else {
            Toast.makeText(this, "请先连接蓝牙设备", Toast.LENGTH_SHORT).show();
            Intent bleIntent = new Intent(this, BLE.class);
            bleIntent.putExtra("target_page", pageType);
            startActivity(bleIntent);
        }
    }
```

#### 4. 蓝牙状态感知

- **连接状态验证**
  跳转前通过单例检查蓝牙连接状态：

  ```java
  if (myBluetoothManager.isConnected()) { 
      // 允许跳转 
  } else {
      // 提示并跳转至蓝牙连接页
      Toast.makeText(this, "请先连接蓝牙设备", Toast.LENGTH_SHORT).show();
  }
  ```

- **设备地址传递**
  数据接收页面需要设备地址：

  ```java
  intent.putExtra("DEVICE_ADDRESS", myBluetoothManager.getDeviceAddress());
  ```

  

#### 5.菜单事件处理

点击菜单项直接跳转至蓝牙连接页：

```java
public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    if (item.getItemId() == R.id.ble) {
        startActivity(new Intent(this, BLE.class));
        return true;
    }
    return super.onOptionsItemSelected(item);
}
```

#### 关键设计特点

1. **状态驱动的导航**
   - 通过 `target_page` 参数实现连接后自动跳转
   - 未连接时保存目标页面信息（传递给 BLE Activity）
2. **蓝牙状态解耦**
   - 所有蓝牙状态通过 `MyBluetoothManager` 单例获取
   - 避免直接操作 GATT 对象，提高代码可维护性
3. **用户引导机制**
   - 未连接时明确提示 "请先连接蓝牙设备"
   - 提供快捷入口（菜单项）进入蓝牙连接页
4. **模块化页面跳转与核心功能函数**



![](.\ReadmePic\liucheng.png)

![](.\ReadmePic\5.png)

### 数据接收页面（Recdata）

该页面通过保持蓝牙连接服务，获取同一服务下同一个特征值数据包中的不同字节来解析数据，*需要详细结合硬件设计*。

核心功能如下：

- **蓝牙数据接收**：通过BLE GATT特性通知机制接收两个通道的传感器数据
- **实时数据解析**：解析原始字节数据，提取两个指标的有效值
- **双通道绘图**：使用自定义视图`DrawLine`和`DrawLine2`实时绘制两个指标的曲线
- **动态UI更新**：在TextView中实时显示解析后的数值

#### 1. **关键设计**

##### (1) **双通道数据接收架构**（这是预留的）

```java
// 两个独立的特征值UUID
private static final String CHARACTERISTIC_UUID = "0000ffe1..."; // 指标1
private static final String CHARACTERISTIC_UUID2 = "0000fff1..."; // 指标2

// 独立的特征值引用
private BluetoothGattCharacteristic mNotifyCharacteristic;
private BluetoothGattCharacteristic mNotifyCharacteristic2;
```

- **优势**：物理隔离两个数据通道，避免数据干扰
- **实现**：为每个特征值独立设置通知和描述符

##### (2) **数据解析机制**

需要结合硬件数据协议：

- **协议特点**：固定16字节数据包，包含起始/结束标志
- **错误处理**：标志校验 + 数值范围过滤
- **多通道支持**：从固定位置解析两个指标值

```java
private void processReceivedData(byte[] data, int dataType) {
    // 校验起始标志 (0x0A, 0xFA)
    if (data[0] != START_BYTE1 || data[1] != START_BYTE2) return;
    
    // 校验结束标志 (0x00, 0x0B)
    if (data[14] != END_BYTE1 || data[15] != END_BYTE2) return;
    
    // 解析指标值（小端序）
    int Index1Value = ((data[5] & 0xFF) | ((data[6] & 0xFF) << 8));
    int Index2Value = ((data[11] & 0xFF) | ((data[12] & 0xFF) << 8));
    
    // 范围校验 (0-3000)
    if (Index1Value >= 0 && Index1Value <= 3000) {
        updateUIWithValue(Index1Value, Index2Value, dataType);
    }
}
```

##### (3) **实时绘图系统**

```java
// 数据队列（双通道）
private final LinkedList<Float> mDataQueue1 = new LinkedList<>();
private final LinkedList<Float> mDataQueue2 = new LinkedList<>();

// 绘图视图
private DrawLine mDrawLine1; // 指标1
private DrawLine mDrawLine2; // 指标2

// UI更新
private void updateUIWithValue(float value, float value2, int dataType) {
    float seconds = (System.currentTimeMillis() - startTime) / 1000.0f;
    
    // 更新指标1
    mValueDisplay.setText(String.format("%.1f", value));
    updateDataQueue(mDataQueue1, value);
    mDrawLine1.addDataPoint(value, seconds);
    
    // 更新指标2
    mDataDisplay.setText(String.format("%.1f", value2));
    updateDataQueue(mDataQueue2, value2);
    mDrawLine2.addDataPoint(value2, seconds);
}
```

- **核心组件**：
  - 双数据队列（FIFO，最大200点）
  - 自定义绘图视图（支持自动范围调整）
- **时间处理**：基于系统时间的相对时间戳

##### (4) **蓝牙连接管理**

```java
private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        // 状态处理：连接成功/断开
    }
    
    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        setupNotification(gatt); // 发现服务后配置通知
    }
    
    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        // 根据UUID分发数据到不同处理流程
        if (charUuid.equals(UUID.fromString(CHARACTERISTIC_UUID))) {
            processReceivedData(..., 1);
        } else if (charUuid.equals(UUID.fromString(CHARACTERISTIC_UUID2))) {
            processReceivedData(..., 2);
        }
    }
};
```

- **状态机**：使用枚举管理连接状态（DISCONNECTED/CONNECTING等）
- **安全断开**：`safeDisconnectGatt()`方法实现优雅断开

#### 2. **潜在问题与改进**

1. **数据解析耦合**
   - **问题**：协议格式硬编码在业务逻辑中
   - **建议**：抽象为协议解析器类，支持动态配置
2. **UI更新性能**
   - **风险**：高频数据可能造成UI线程阻塞
   - **优化**：使用采样机制或Buffer减少UI刷新频率
3. **时间精度问题**
   - **风险**：`System.currentTimeMillis()`可能受系统时间修改影响
   - **改进**：改用`SystemClock.elapsedRealtime()`

### 蓝牙适配器功能（MyBluetoothManager）

#### 1. **核心功能**

- **蓝牙连接管理**：封装蓝牙设备的连接、断开、重连等核心操作。
- **权限处理**：动态处理 Android 12+ 的蓝牙权限（`BLUETOOTH_CONNECT`）。
- **回调机制**：支持外部回调（如特征值变化通知），实现模块解耦。
- **状态管理**：维护设备连接状态、存储设备信息（地址、BluetoothDevice 对象）。

------

#### 2. **关键设计**

##### (1) **单例模式**

全局唯一实例，避免资源重复初始化。

```java
    // 单例获取方法（需传入 Context）
    public static synchronized MyBluetoothManager getInstance(Context context) {
        if (instance == null) {
            instance = new MyBluetoothManager(context);
        }
        return instance;
    }
```

##### (2) **连接状态管理**

- `isConnected` 标志位 + `bluetoothGatt` 非空校验：

```java
public boolean isConnected() {
    return isConnected && bluetoothGatt != null;
}
```

断开时保留设备地址便于重连（不置空 `deviceAddress`）。

##### (3) **回调转发机制**

```java
    public void forwardCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (mExternalCallback != null) {
            mExternalCallback.onCharacteristicChanged(gatt, characteristic);
        }
    }
```

该部分允许外部模块（如数据处理类）接收特征值变化事件。

##### (4) **动态回调更新**

```java
    @SuppressLint("MissingPermission")
    public void reconnectWithNewCallback(BluetoothGattCallback newCallback) {
        if (bluetoothGatt != null && bluetoothDevice != null) {
            // 断开旧连接
            bluetoothGatt.disconnect();
            bluetoothGatt.close();

            // 使用新回调重新连接
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                bluetoothGatt = bluetoothDevice.connectGatt(context, false, newCallback, BluetoothDevice.TRANSPORT_LE);
            } else {
                bluetoothGatt = bluetoothDevice.connectGatt(context, false, newCallback);
            }
            this.bluetoothGattCallback = newCallback;
        }
    }
```

支持运行时切换 GATT 回调（需重新连接生效）。

##### (5) **权限适配**

兼容不同安卓版本需求

```
private boolean checkBluetoothPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return checkSelfPermission(BLUETOOTH_CONNECT) == PERMISSION_GRANTED;
    }
    return true; // Android 12 以下无需此权限
}
```

#### 3. **主要方法**

| **方法**                           | **功能**                                       |
| ---------------------------------- | ---------------------------------------------- |
| `setConnected(...)`                | 更新连接状态及关联对象（BLE 实例、GATT、回调） |
| `disconnect()`                     | 断开连接并释放资源，保留设备地址               |
| `getCharacteristic()`              | 获取预设 UUID 的特征值（硬编码服务/特征 UUID） |
| `requestBluetoothPermissions(...)` | 在 Activity 中触发权限请求弹窗                 |
| `isDeviceConnected()`              | 综合检查连接状态（标志位 + 设备地址非空）      |

总而言之，该蓝牙管理器实现了以下核心能力：

1. **生命周期管理**：单例模式保证资源可控。
2. **跨版本兼容**：动态处理 Android 12+ 权限。
3. **事件转发**：通过外部回调支持业务扩展。
4. **连接复用**：断开时保留设备信息便于重连。

但仍有**改进方向**，如：解耦硬件参数（UUID）、优化回调架构、增强连接状态可靠性。整体设计合理，适用于固定蓝牙设备的场景，但需扩展灵活性以支持多设备。

## 数据绘图功能（DrawLine、DrawLine2）

#### 1. **核心功能**

- **实时曲线绘制**：动态绘制随时间变化的数据曲线
- **双轴自适应系统**：
  - Y轴：动态范围调整（带平滑过渡）
  - X轴：智能时间标签生成与避让
- **数据可视化增强**：
  - 曲线填充效果（半透明蓝色区域）
  - 稀疏数据点标记（性能优化）
  - 最新值高亮显示
- **数据管理**：FIFO队列控制（最大点数限制）

#### 2. **关键设计亮点**

##### (1) 智能Y轴范围调整

```java
private void updateDataRange(float value) {
    // 处理全等值情况
    if (queueMaxValue == queueMinValue) {
        float delta = Math.abs(queueMaxValue) * 0.2f;
        minValue = queueMinValue - delta;
        maxValue = queueMaxValue + delta;
        return;
    }

    // 动态缓冲区域（15%）
    float margin = (queueMaxValue - queueMinValue) * 0.15f;
    float newMin = queueMinValue - margin;
    float newMax = queueMaxValue + margin;

    // 平滑过渡（30%因子）
    float transitionFactor = 0.3f;
    minValue = minValue + (newMin - minValue) * transitionFactor;
    maxValue = maxValue + (newMax - maxValue) * transitionFactor;
}
```

- **边界处理**：全等值数据自动生成合理范围
- **视觉优化**：15%缓冲区域避免曲线贴边
- **平滑动画**：30%过渡因子消除范围跳变

##### (2) 时间轴智能标签系统

```java
private String formatTimeLabel(float totalSeconds) {
    if (seconds < 60) {
        return String.format(Locale.getDefault(), "%ds", seconds);
    } else if (seconds < 3600) {
        return String.format(Locale.getDefault(), "%dm%02ds", minutes, remainingSeconds);
    } else {
        return String.format(Locale.getDefault(), "%dh%02dm%02ds", hours, minutes, remainingSeconds);
    }
}
```

**多级时间格式**：秒→分秒→时分秒自动转换

**标签避让机制**：

```java
if (Math.abs(x - lastDrawnX) < 100) continue; // 100像素最小间距
```

**按需生成**：基于时间间隔（1秒）生成标签

##### (3) 高性能绘制优化

- **数据点稀疏渲染**：

  ```java
  if (i % 5 == 0 || i == dataList.size() - 1) {
      canvas.drawCircle(x, y, 8, pointPaint);
  }
  ```

**路径复用**：`dataPath`和`fillPath`对象复用

**局部更新**：数据变化时仅标记脏区域

##### (4) 数据点三元组结构

```java
private static class DataPoint {
    float value;    // 数据值
    float time;     // 相对时间(秒)
    String label;   // 格式化时间标签
}
```

- **时空绑定**：将数值、时间和显示标签封装
- **按需计算**：标签仅在需要时生成（LABEL_INTERVAL控制）

##### (5) 双极值跟踪系统

```java
// 全局范围
private float maxValue = 100f;
private float minValue = 0f;

// 队列范围
private float queueMaxValue = Float.MIN_VALUE;
private float queueMinValue = Float.MAX_VALUE;
```

- **分离关注点**：全局范围用于绘制，队列范围用于计算
- **高效更新**：添加/移除时动态更新极值

##### (6) 智能范围约束

```java
// 确保最小可见范围
float minRange = Math.max(currentRange * 0.5f, 10f);
if (maxValue - minValue < minRange) {
    float center = (minValue + maxValue) / 2;
    minValue = center - minRange / 2;
    maxValue = center + minRange / 2;
}
```

- 防止微小波动导致的轴抖动
- 保证最小10单位的可视范围

#### 3.潜在的改进方向

**曲线平滑**：添加贝塞尔曲线插值算法

**动态密度**：根据设备性能自动调整渲染密度

**批处理绘制**：使用drawLines替代Path提升性能

**内存优化**：环形缓冲区替代LinkedList

## 四、预增功能

1.默认的离线数据保存路径为：我的手机/Android/data/USTB.AAIST/files，需要后期自定义数据存储文件夹

2.绘图区域的手势缩放

## 五、注

1.数据接收有些卡顿，需要匹配硬件的采样率，当然硬件本身就是心电采集的，肯定是与我想做的硬件不一样，自然也就与目前的软件不适配