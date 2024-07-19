# Bug&&DeBug

## 一、Bug描述

~~1.当打开软件启动Splash页面进入主页面后，按返回退出至桌面还会返回到Splash页面，并且终止在该页面，返回键无效，只能通过杀后台进程退出。~~

*2.数据经过python滤波后图像明显发生了变形，与正常绘制的图像差异很大，可能原因是数据源本身是已经滤波完的，而不是直接采集的信号，截取122个数据时是不完整的，如*

```
    0.058
    0.068
    0.
    读取成功[string]:080
    0.083
    0.080
    0.085
```

采取16进制发送数据还会出现该结果，保存到手机的原数据与发送的数据差异很大

发送的部分数据：

```
-0.207
-0.195
-0.215
-0.180
-0.203
-0.210
-0.203...
```

接受保存的原始数据：

```
-2.554375
-2.49025
-2.33
-3.774375
-2.554375
-2.52225
-2.392875
```

3.绘制完图像闪退至主页面，不稳定。

4.关闭蓝牙搜索按钮后再打开搜索不到蓝牙，估计是没有进行初始化的原因，需要根据其他蓝牙软件重写这部分逻辑。

5.数据量大时绘制完闪退，例如3s的数据循环三次。

6.ECG绘图界面右侧的功能区在xml布局距离右侧边界没有距离，真机模拟时出现很大的空白距离，预计采取强制ECGChart页面全屏解决

~~7.点击连接某一蓝牙，跳转到绘制ECG图像页面过程中会出现一阵黑屏，Logcat对应的日志：~~

```
2024-07-19 21:18:29.910  2178-2178  AndroidRuntime          USTB.AAIST                           D  Shutting down VM
2024-07-19 21:18:29.917  2178-2178  AndroidRuntime          USTB.AAIST                           E  FATAL EXCEPTION: main
                                                                                                    Process: USTB.AAIST, PID: 2178
                                                                                                    java.lang.NullPointerException: Attempt to invoke virtual method 'void android.widget.TextView.setText(java.lang.CharSequence)' on a null object reference
                                                                                                    	at USTB.AAIST.BLE$2$1.run(BLE.java:161)
                                                                                                    	at android.app.Activity.runOnUiThread(Activity.java:8019)
                                                                                                    	at USTB.AAIST.BLE$2.onItemClick(BLE.java:158)
                                                                                                    	at android.widget.AdapterView.performItemClick(AdapterView.java:332)
                                                                                                    	at android.widget.AbsListView.performItemClick(AbsListView.java:1484)
                                                                                                    	at android.widget.AbsListView$PerformClick.run(AbsListView.java:3633)
                                                                                                    	at android.widget.AbsListView$5.run(AbsListView.java:4776)
                                                                                                    	at android.os.Handler.handleCallback(Handler.java:996)
                                                                                                    	at android.os.Handler.dispatchMessage(Handler.java:110)
                                                                                                    	at android.os.Looper.loopOnce(Looper.java:210)
                                                                                                    	at android.os.Looper.loop(Looper.java:302)
                                                                                                    	at android.app.ActivityThread.main(ActivityThread.java:9652)
                                                                                                    	at java.lang.reflect.Method.invoke(Native Method)
                                                                                                    	at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:601)
                                                                                                    	at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1062)
2024-07-19 21:18:29.925  2178-2178  HiView                  USTB.AAIST                           I  Begin report 1002
2024-07-19 21:18:29.925  2178-2178  HiEvent                 USTB.AAIST                           I  Flatten done: 1002
2024-07-19 21:18:29.925  2178-2178  Process                 USTB.AAIST                           I  Sending signal. PID: 2178 SIG: 9
```



## 二、DeBug

1.20240701解决该bug，重写了页面跳转逻辑，对应res->anim->fade_in与fade_out两个函数。





7.20240719解决该BUG，原因是：原来的状态标记id已经弃用，导致在运行过程中mTvState.setText(getString(R.string.connecting)); setText的id已经是空的。

## 三、特征

1.实时数据绘制

2.离线数据与绘图存储

## 四、预增功能

1.默认的离线数据保存路径为：我的手机/Android/data/USTB.AAIST/files，需要后期自定义数据存储文件夹

2.=