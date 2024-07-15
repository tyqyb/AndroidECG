# import signal
# import random
# import math
# import numpy as np
# from scipy.signal import firwin, convolve
# from scipy.signal import find_peaks
# from scipy import signal
# my_global_list = list()
# Respiratory_global_list=list()
# heart_rate_mean=0
# def append_to_global_list(local_list):
#     global my_global_list
#     my_global_list += local_list
# def append_to_global_Respiratory(local_list):
#     global Respiratory_global_list
#     Respiratory_global_list += local_list
# # 心电信号的滤波以及心率的计算
# def ecgFilter(data1, data2, data3, data4, data5, data6, data7, data8, data9, data10, data11, data12, data13, data14,
#                data15, data16, data17, data18, data19, data20, data21, data22, data23, data24, data25, data26, data27,
#                data28, data29, data30, data31, data32, data33, data34, data35, data36, data37, data38, data39, data40,
#                data41, data42, data43, data44, data45, data46, data47, data48, data49, data50, data51, data52, data53,
#                data54, data55, data56, data57, data58, data59, data60, data61, data62, data63, data64, data65, data66,
#                data67, data68, data69, data70, data71, data72, data73, data74, data75, data76, data77, data78, data79,
#                data80, data81, data82, data83, data84, data85, data86, data87, data88, data89, data90, data91, data92,
#                data93, data94, data95, data96, data97, data98, data99, data100, data101, data102, data103, data104,
#                data105, data106, data107, data108, data109, data110, data111, data112, data113, data114, data115,
#                data116, data117, data118, data119, data120, data121, data122
#                ):
#     cECG_Raw_WaveList = []
#     cECG_Raw_WaveList.append(data1)
#     cECG_Raw_WaveList.append(data2)
#     cECG_Raw_WaveList.append(data3)
#     cECG_Raw_WaveList.append(data4)
#     cECG_Raw_WaveList.append(data5)
#     cECG_Raw_WaveList.append(data6)
#     cECG_Raw_WaveList.append(data7)
#     cECG_Raw_WaveList.append(data8)
#     cECG_Raw_WaveList.append(data9)
#     cECG_Raw_WaveList.append(data10)
#     cECG_Raw_WaveList.append(data11)
#     cECG_Raw_WaveList.append(data12)
#     cECG_Raw_WaveList.append(data13)
#     cECG_Raw_WaveList.append(data14)
#     cECG_Raw_WaveList.append(data15)
#     cECG_Raw_WaveList.append(data16)
#     cECG_Raw_WaveList.append(data17)
#     cECG_Raw_WaveList.append(data18)
#     cECG_Raw_WaveList.append(data19)
#     cECG_Raw_WaveList.append(data20)
#     cECG_Raw_WaveList.append(data21)
#     cECG_Raw_WaveList.append(data22)
#     cECG_Raw_WaveList.append(data23)
#     cECG_Raw_WaveList.append(data24)
#     cECG_Raw_WaveList.append(data25)
#     cECG_Raw_WaveList.append(data26)
#     cECG_Raw_WaveList.append(data27)
#     cECG_Raw_WaveList.append(data28)
#     cECG_Raw_WaveList.append(data29)
#     cECG_Raw_WaveList.append(data30)
#     cECG_Raw_WaveList.append(data31)
#     cECG_Raw_WaveList.append(data32)
#     cECG_Raw_WaveList.append(data33)
#     cECG_Raw_WaveList.append(data34)
#     cECG_Raw_WaveList.append(data35)
#     cECG_Raw_WaveList.append(data36)
#     cECG_Raw_WaveList.append(data37)
#     cECG_Raw_WaveList.append(data38)
#     cECG_Raw_WaveList.append(data39)
#     cECG_Raw_WaveList.append(data40)
#     cECG_Raw_WaveList.append(data41)
#     cECG_Raw_WaveList.append(data42)
#     cECG_Raw_WaveList.append(data43)
#     cECG_Raw_WaveList.append(data44)
#     cECG_Raw_WaveList.append(data45)
#     cECG_Raw_WaveList.append(data46)
#     cECG_Raw_WaveList.append(data47)
#     cECG_Raw_WaveList.append(data48)
#     cECG_Raw_WaveList.append(data49)
#     cECG_Raw_WaveList.append(data50)
#     cECG_Raw_WaveList.append(data51)
#     cECG_Raw_WaveList.append(data52)
#     cECG_Raw_WaveList.append(data53)
#     cECG_Raw_WaveList.append(data54)
#     cECG_Raw_WaveList.append(data55)
#     cECG_Raw_WaveList.append(data56)
#     cECG_Raw_WaveList.append(data57)
#     cECG_Raw_WaveList.append(data58)
#     cECG_Raw_WaveList.append(data59)
#     cECG_Raw_WaveList.append(data60)
#     cECG_Raw_WaveList.append(data61)
#     cECG_Raw_WaveList.append(data62)
#     cECG_Raw_WaveList.append(data63)
#     cECG_Raw_WaveList.append(data64)
#     cECG_Raw_WaveList.append(data65)
#     cECG_Raw_WaveList.append(data66)
#     cECG_Raw_WaveList.append(data67)
#     cECG_Raw_WaveList.append(data68)
#     cECG_Raw_WaveList.append(data69)
#     cECG_Raw_WaveList.append(data70)
#     cECG_Raw_WaveList.append(data71)
#     cECG_Raw_WaveList.append(data72)
#     cECG_Raw_WaveList.append(data73)
#     cECG_Raw_WaveList.append(data74)
#     cECG_Raw_WaveList.append(data75)
#     cECG_Raw_WaveList.append(data76)
#     cECG_Raw_WaveList.append(data77)
#     cECG_Raw_WaveList.append(data78)
#     cECG_Raw_WaveList.append(data79)
#     cECG_Raw_WaveList.append(data80)
#     cECG_Raw_WaveList.append(data81)
#     cECG_Raw_WaveList.append(data82)
#     cECG_Raw_WaveList.append(data83)
#     cECG_Raw_WaveList.append(data84)
#     cECG_Raw_WaveList.append(data85)
#     cECG_Raw_WaveList.append(data86)
#     cECG_Raw_WaveList.append(data87)
#     cECG_Raw_WaveList.append(data88)
#     cECG_Raw_WaveList.append(data89)
#     cECG_Raw_WaveList.append(data90)
#     cECG_Raw_WaveList.append(data91)
#     cECG_Raw_WaveList.append(data92)
#     cECG_Raw_WaveList.append(data93)
#     cECG_Raw_WaveList.append(data94)
#     cECG_Raw_WaveList.append(data95)
#     cECG_Raw_WaveList.append(data96)
#     cECG_Raw_WaveList.append(data97)
#     cECG_Raw_WaveList.append(data98)
#     cECG_Raw_WaveList.append(data99)
#     cECG_Raw_WaveList.append(data100)
#     cECG_Raw_WaveList.append(data101)
#     cECG_Raw_WaveList.append(data102)
#     cECG_Raw_WaveList.append(data103)
#     cECG_Raw_WaveList.append(data104)
#     cECG_Raw_WaveList.append(data105)
#     cECG_Raw_WaveList.append(data106)
#     cECG_Raw_WaveList.append(data107)
#     cECG_Raw_WaveList.append(data108)
#     cECG_Raw_WaveList.append(data109)
#     cECG_Raw_WaveList.append(data110)
#     cECG_Raw_WaveList.append(data111)
#     cECG_Raw_WaveList.append(data112)
#     cECG_Raw_WaveList.append(data113)
#     cECG_Raw_WaveList.append(data114)
#     cECG_Raw_WaveList.append(data115)
#     cECG_Raw_WaveList.append(data116)
#     cECG_Raw_WaveList.append(data117)
#     cECG_Raw_WaveList.append(data118)
#     cECG_Raw_WaveList.append(data119)
#     cECG_Raw_WaveList.append(data120)
#     cECG_Raw_WaveList.append(data121)
#     cECG_Raw_WaveList.append(data122)
#
#     fs = 500
#     num_ReRes = [0.99556697201764721, -1.9911339440352944, 0.99556697201764721]
#     den_ReRes = [1, -1.9911142922016536, 0.99115359586893548]
#     b1, a1 = signal.butter(2, [49 / (fs / 2), 51 / (fs / 2)], 'bandstop')  # 配置滤波器 2表示滤波器的阶数
#     b2, a2 = signal.butter(2, 30 / (fs / 2), 'lowpass')
#     b3, a3 = signal.butter(2, [0.1 / (fs / 2), 0.4 / (fs / 2)], 'bandstop')
#     b, a = signal.butter(2, 0.5 / (500 / 2), "highpass")
#     # print("tttttttttttt")
#     # print(type(cECG_Raw_WaveList))
#     cECG_Wave_RemoveRe_ReomvePowerLine = signal.filtfilt(b, a,cECG_Raw_WaveList)
#     # 2.50HZ工频
#     cECG_Wave_RemoveRe_ReomvePowerLine = signal.filtfilt(b1,a1, cECG_Wave_RemoveRe_ReomvePowerLine)
#     # 3.上限频率35HZ
#     cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN = signal.filtfilt(b2, a2,
#                                                                                cECG_Wave_RemoveRe_ReomvePowerLine)
#     print("cECG_Wave_RemoveRe_ReomvePowerLine",len(cECG_Wave_RemoveRe_ReomvePowerLine))
#     print("my_global_list:",len(my_global_list))
#
#    # print(peaks, cECG_Wave_RemoveRe_ReomvePowerLine[peaks])
#     # heart_rate = 60 / np.mean(np.diff(peaks)
#     # print(heart_rate)
#     append_to_global_list(cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.tolist())
#     global heart_rate_mean
#
#     if(len(my_global_list)%488==0):
#
#         t = np.arange(0, 30, 1 / fs)  # 时间序列
#         #peaks, _ = find_peaks(np.array(cECG_Wave_RemoveRe_ReomvePowerLine), height=0, distance=250)
# #         peaks, _ = signal.find_peaks(np.array(my_global_list), height=0, distance=fs / 2)  # 设置高度阈值为0，距离阈值为半秒
#         peaks, _ = signal.find_peaks(np.array(my_global_list), height=0.5, distance=200)  # 设置高度阈值为0，距离阈值为半秒
#        # print("peaks")
#        # print(peaks)
#        # print(np.array(my_global_list)[peaks])
#         #print("peaks")
#         heart_rate = 60 / np.mean(np.diff(peaks))
#          # 计算峰值之间的时间间隔和心率
#         intervals = np.diff(t[peaks])  # 时间间隔
#         hr = 60 / intervals  # 心率（次/分钟）
#         print(hr)
#         if(len(hr)!=0):
#             mean_hr=np.mean(hr)
#           #  print("心率为:hr",mean_hr)
#             heart_rate_mean =mean_hr
#
#         #print("心率为:heart_rate",heart_rate)
#
# #         if(heart_rate_mean!=)
#
# #         get_hear_rate(mean_hr)
#     if(len(my_global_list)>4880):
#         my_global_list.clear()
#     #print(len(my_global_list) )
#     return cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN
# # 获取心率
# def get_hear_rate():
#     #print("get_hear_rate:",heart_rate_mean)
#     return int(heart_rate_mean)
# # 获取呼吸波
# def getRespiratorywave(data1, data2, data3, data4, data5, data6, data7, data8, data9, data10, data11, data12, data13, data14,
#                                       data15, data16, data17, data18, data19, data20, data21, data22, data23, data24, data25, data26, data27,
#                                       data28, data29, data30, data31, data32, data33, data34, data35, data36, data37, data38, data39, data40,
#                                       data41, data42, data43, data44, data45, data46, data47, data48, data49, data50, data51, data52, data53,
#                                       data54, data55, data56, data57, data58, data59, data60, data61, data62, data63, data64, data65, data66,
#                                       data67, data68, data69, data70, data71, data72, data73, data74, data75, data76, data77, data78, data79,
#                                       data80, data81, data82, data83, data84, data85, data86, data87, data88, data89, data90, data91, data92,
#                                       data93, data94, data95, data96, data97, data98, data99, data100, data101, data102, data103, data104,
#                                       data105, data106, data107, data108, data109, data110, data111, data112, data113, data114, data115,
#                                       data116, data117, data118, data119, data120, data121, data122):
#     cECG_Raw_WaveList = []
#     cECG_Raw_WaveList.append(data1)
#     cECG_Raw_WaveList.append(data2)
#     cECG_Raw_WaveList.append(data3)
#     cECG_Raw_WaveList.append(data4)
#     cECG_Raw_WaveList.append(data5)
#     cECG_Raw_WaveList.append(data6)
#     cECG_Raw_WaveList.append(data7)
#     cECG_Raw_WaveList.append(data8)
#     cECG_Raw_WaveList.append(data9)
#     cECG_Raw_WaveList.append(data10)
#     cECG_Raw_WaveList.append(data11)
#     cECG_Raw_WaveList.append(data12)
#     cECG_Raw_WaveList.append(data13)
#     cECG_Raw_WaveList.append(data14)
#     cECG_Raw_WaveList.append(data15)
#     cECG_Raw_WaveList.append(data16)
#     cECG_Raw_WaveList.append(data17)
#     cECG_Raw_WaveList.append(data18)
#     cECG_Raw_WaveList.append(data19)
#     cECG_Raw_WaveList.append(data20)
#     cECG_Raw_WaveList.append(data21)
#     cECG_Raw_WaveList.append(data22)
#     cECG_Raw_WaveList.append(data23)
#     cECG_Raw_WaveList.append(data24)
#     cECG_Raw_WaveList.append(data25)
#     cECG_Raw_WaveList.append(data26)
#     cECG_Raw_WaveList.append(data27)
#     cECG_Raw_WaveList.append(data28)
#     cECG_Raw_WaveList.append(data29)
#     cECG_Raw_WaveList.append(data30)
#     cECG_Raw_WaveList.append(data31)
#     cECG_Raw_WaveList.append(data32)
#     cECG_Raw_WaveList.append(data33)
#     cECG_Raw_WaveList.append(data34)
#     cECG_Raw_WaveList.append(data35)
#     cECG_Raw_WaveList.append(data36)
#     cECG_Raw_WaveList.append(data37)
#     cECG_Raw_WaveList.append(data38)
#     cECG_Raw_WaveList.append(data39)
#     cECG_Raw_WaveList.append(data40)
#     cECG_Raw_WaveList.append(data41)
#     cECG_Raw_WaveList.append(data42)
#     cECG_Raw_WaveList.append(data43)
#     cECG_Raw_WaveList.append(data44)
#     cECG_Raw_WaveList.append(data45)
#     cECG_Raw_WaveList.append(data46)
#     cECG_Raw_WaveList.append(data47)
#     cECG_Raw_WaveList.append(data48)
#     cECG_Raw_WaveList.append(data49)
#     cECG_Raw_WaveList.append(data50)
#     cECG_Raw_WaveList.append(data51)
#     cECG_Raw_WaveList.append(data52)
#     cECG_Raw_WaveList.append(data53)
#     cECG_Raw_WaveList.append(data54)
#     cECG_Raw_WaveList.append(data55)
#     cECG_Raw_WaveList.append(data56)
#     cECG_Raw_WaveList.append(data57)
#     cECG_Raw_WaveList.append(data58)
#     cECG_Raw_WaveList.append(data59)
#     cECG_Raw_WaveList.append(data60)
#     cECG_Raw_WaveList.append(data61)
#     cECG_Raw_WaveList.append(data62)
#     cECG_Raw_WaveList.append(data63)
#     cECG_Raw_WaveList.append(data64)
#     cECG_Raw_WaveList.append(data65)
#     cECG_Raw_WaveList.append(data66)
#     cECG_Raw_WaveList.append(data67)
#     cECG_Raw_WaveList.append(data68)
#     cECG_Raw_WaveList.append(data69)
#     cECG_Raw_WaveList.append(data70)
#     cECG_Raw_WaveList.append(data71)
#     cECG_Raw_WaveList.append(data72)
#     cECG_Raw_WaveList.append(data73)
#     cECG_Raw_WaveList.append(data74)
#     cECG_Raw_WaveList.append(data75)
#     cECG_Raw_WaveList.append(data76)
#     cECG_Raw_WaveList.append(data77)
#     cECG_Raw_WaveList.append(data78)
#     cECG_Raw_WaveList.append(data79)
#     cECG_Raw_WaveList.append(data80)
#     cECG_Raw_WaveList.append(data81)
#     cECG_Raw_WaveList.append(data82)
#     cECG_Raw_WaveList.append(data83)
#     cECG_Raw_WaveList.append(data84)
#     cECG_Raw_WaveList.append(data85)
#     cECG_Raw_WaveList.append(data86)
#     cECG_Raw_WaveList.append(data87)
#     cECG_Raw_WaveList.append(data88)
#     cECG_Raw_WaveList.append(data89)
#     cECG_Raw_WaveList.append(data90)
#     cECG_Raw_WaveList.append(data91)
#     cECG_Raw_WaveList.append(data92)
#     cECG_Raw_WaveList.append(data93)
#     cECG_Raw_WaveList.append(data94)
#     cECG_Raw_WaveList.append(data95)
#     cECG_Raw_WaveList.append(data96)
#     cECG_Raw_WaveList.append(data97)
#     cECG_Raw_WaveList.append(data98)
#     cECG_Raw_WaveList.append(data99)
#     cECG_Raw_WaveList.append(data100)
#     cECG_Raw_WaveList.append(data101)
#     cECG_Raw_WaveList.append(data102)
#     cECG_Raw_WaveList.append(data103)
#     cECG_Raw_WaveList.append(data104)
#     cECG_Raw_WaveList.append(data105)
#     cECG_Raw_WaveList.append(data106)
#     cECG_Raw_WaveList.append(data107)
#     cECG_Raw_WaveList.append(data108)
#     cECG_Raw_WaveList.append(data109)
#     cECG_Raw_WaveList.append(data110)
#     cECG_Raw_WaveList.append(data111)
#     cECG_Raw_WaveList.append(data112)
#     cECG_Raw_WaveList.append(data113)
#     cECG_Raw_WaveList.append(data114)
#     cECG_Raw_WaveList.append(data115)
#     cECG_Raw_WaveList.append(data116)
#     cECG_Raw_WaveList.append(data117)
#     cECG_Raw_WaveList.append(data118)
#     cECG_Raw_WaveList.append(data119)
#     cECG_Raw_WaveList.append(data120)
#     cECG_Raw_WaveList.append(data121)
#     cECG_Raw_WaveList.append(data122)
#     [b3,a3] = signal.butter(3,0.4/250,'low')
#     append_to_global_Respiratory(cECG_Raw_WaveList)
#     print("len append:",len(Respiratory_global_list))
#     cECG_Envelop_Respiratory_Wave=[]
#     if(len(Respiratory_global_list)%488==0):
#        cECG_filter_process = cECG_Raw_WaveList
#        print("cECG_filter_process size  ",len(cECG_filter_process))
#     #    % 低通 上限截止频率为0.4Hz
#
#        n=len(cECG_Raw_WaveList)
#        #122
#        print("n size  ", len(cECG_Raw_WaveList))
#        for i in range(0,n):
#            # % 取所有点的平均值
#            cECG_mean = np.mean(cECG_filter_process)
#            # % 去均值
#            cECG_Demean = (cECG_filter_process)-cECG_mean
#            # % 整流
#            cECG_absolute = abs(cECG_Demean)
#            cECG_absolute_mean = np.mean(cECG_absolute)
#            cECG_absolute_Demean=(cECG_absolute)-cECG_absolute_mean
#            # % 取包络（呼吸波）
#            cECG_Envelop_Respiratory_Wave = signal.filtfilt(b3, a3, cECG_absolute_Demean).tolist()
#        print("cECG_Envelop_Respiratory_Wave")
#        print(cECG_Envelop_Respiratory_Wave)
#        print(type(cECG_Envelop_Respiratory_Wave))
#     print("none:")
#     print(cECG_Envelop_Respiratory_Wave)
#     return cECG_Envelop_Respiratory_Wave
#
#
# #     return respiratory_signal
