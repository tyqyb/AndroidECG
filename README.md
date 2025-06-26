# 开发记录

## 一、整体预览

现在画太多时间在UI设计上对于整个小项目的推进是来不及的，同时还有其他事情要做（实验、硬件等），所以UI上显得粗制滥造了些，功能简单所以UI页面也十分的简洁...

<img src=".\ReadmePic\0.jpg" alt="Splash" style="zoom:20%;" /><img src=".\ReadmePic\1.jpg" alt="SearchBLE" style="zoom:20%;" /><img src=".\ReadmePic\2.jpg" alt="SearchBLEList" style="zoom:20%;" />

<img src=".\ReadmePic\3.jpg" alt="SelectPage" style="zoom:20%;" /><img src=".\ReadmePic\4.jpg" alt="BiochemicalIndicatorsPage" style="zoom:20%;" />

当然，最后一页目前还有点小问题需要修复。。。

## 二、页面说明

该部分介绍每个页面的主要功能函数的作用以及实现方式，由于软件 还在开发过程中，每个页面的功能修改由每次提交到GitHub时的描述记录：

## 三、软件功能

1.实现心电图数据的接收与绘制
2.实现两通道汗液生化指标实时监测



## 四、预增功能

1.默认的离线数据保存路径为：我的手机/Android/data/USTB.AAIST/files，需要后期自定义数据存储文件夹

2.绘图区域的手势缩放

## 五、注

1.对于两个指标，考虑使用蓝牙的两个不同特征值进行发送指标数据（现已完成的大部分内容Draw1、2）或者就是同一个特征值下不同的字节位置，参考E:\00-UXXXT\03-AXXXojects\APXXXXS\硬件文档下的说明文档
2.数据接收有些卡顿，需要匹配硬件的采样率（有时间再做吧。。。）