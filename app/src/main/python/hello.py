#
# from scipy import signal
# import numpy as np
# from scipy.signal import find_peaks, square
#
# def sayHello():
#     print("Hello World")
# def data():
# #滤波所需
#     x = np.linspace(3,103,10000)
#
#     sin = np.clip(np.sin(0.6*x)-0.5,0,10)
#     tri = np.concatenate([np.linspace(0,0.3,5000),np.linspace(0.3,0,5000)],axis =0)
#     sig = np.sin(6*x-1.2)
#
#     full = sin+tri+sig
#     peaks = find_peaks(full)[0]
#     print(peaks)
#
# def filt():
#         # 滤波所需
#          fs = 500
#          num_ReRes = [0.99556697201764721, -1.9911339440352944, 0.99556697201764721]
#          den_ReRes = [1, -1.9911142922016536, 0.99115359586893548]
#          b1, a1 = signal.butter(2, [49 / (fs / 2), 51 / (fs / 2)], 'bandstop')  # 配置滤波器 2表示滤波器的阶数
#          b2, a2 = signal.butter(2, 35 / (fs / 2), 'lowpass');
#          b3, a3 = signal.butter(2, [0.1 / (fs / 2), 0.4 / (fs / 2)], 'bandstop')
#          cECG_Raw_WaveList = [
#                                    0.150375, 0.2175, 0.217625, 0.217125, 0.21625, 0.2155, 0.215, 0.215375, 0.21525, 0.21525, 0.2155, 0.215125, 0.215375, 0.215, 0.215125, 0.2145, 0.21325, 0.214, 0.21375, 0.213375, 0.213375, 0.212875, 0.212375, 0.212375, 0.21225, 0.211875, 0.21125, 0.21075, 0.210125, 0.209625, 0.209, 0.209125, 0.208875, 0.2085, 0.2085, 0.20800000000000002, 0.20700000000000002, 0.20650000000000002, 0.206625, 0.20750000000000002, 0.20775000000000002, 0.20725000000000002, 0.206875, 0.20650000000000002, 0.206125, 0.20525000000000002, 0.20450000000000002, 0.20425000000000001, 0.203625, 0.203125, 0.203125, 0.20350000000000001, 0.203875, 0.204375, 0.203875, 0.202125, 0.201, 0.20025, 0.199625, 0.200125, 0.20075, 0.2005, 0.19975, 0.1985, 0.1975, 0.197, 0.197125, 0.19775, 0.198125, 0.197875, 0.1985, 0.19875, 0.19825, 0.197625, 0.19675, 0.19575, 0.195, 0.19425, 0.194875, 0.1955, 0.195875, 0.19575, 0.196125, 0.196125, 0.19625, 0.195375, 0.19475, 0.195125, 0.194125, 0.19325, 0.19275, 0.19225, 0.19225, 0.192, 0.19125, 0.1905, 0.19025, 0.18987500000000002, 0.18987500000000002, 0.19037500000000002, 0.18975, 0.18975, 0.18987500000000002, 0.18937500000000002, 0.1895, 0.18937500000000002, 0.18862500000000001, 0.1885, 0.18825, 0.18887500000000002, 0.187375, 0.1865, 0.1865, 0.18675, 0.187125, 0.18625, 0.18575, 0.185625, 0.186, 0.186, 0.185875, 0.185875, 0.186375, 0.1865, 0.185875, 0.18475, 0.184125, 0.183375, 0.183, 0.182625, 0.185375, 0.184, 0.1825, 0.18175, 0.180875, 0.18075, 0.181625, 0.18225, 0.181375, 0.181375, 0.181125, 0.180375, 0.179875, 0.179625, 0.18075, 0.181625, 0.182125, 0.181875, 0.180875, 0.180375, 0.18, 0.17975, 0.179375, 0.179625, 0.179, 0.179375, 0.18, 0.17975, 0.179125, 0.178, 0.177, 0.176375, 0.176625, 0.177125, 0.177125, 0.177125, 0.17625, 0.175625, 0.17425000000000002, 0.172875, 0.17275000000000001, 0.172625, 0.17250000000000001, 0.17275000000000001, 0.17375000000000002, 0.17400000000000002, 0.173875, 0.17350000000000002, 0.17275000000000001, 0.17175, 0.17125, 0.171625, 0.171625, 0.171875, 0.17200000000000001, 0.172125, 0.17225000000000001, 0.17175, 0.171, 0.17, 0.168875, 0.168125, 0.168, 0.168875, 0.169125, 0.16925, 0.16975, 0.169375, 0.1685, 0.16825, 0.167625, 0.1675, 0.16775, 0.168125, 0.16875, 0.16825, 0.1685, 0.168125, 0.16775, 0.16725, 0.15175, 0.122625, 0.091625, 0.061875, 0.032, 0.0022500000000000003, -0.027375, -0.057, -0.08700000000000001, -0.0905, -0.082, -0.0895, -0.1, -0.113625, -0.12625, -0.141125, -0.1545, -0.161375, -0.1645, -0.17400000000000002, -0.176625, -0.172875, -0.170125, -0.16825, -0.15825, -0.146625, -0.131375, -0.11812500000000001, -0.10325000000000001, -0.081, -0.082625, -0.092125, -0.08375, -0.064125, -0.037875, -0.009125, 0.020375, 0.05, 0.078625, 0.107625, 0.128125, 0.138625, 0.14375000000000002, 0.14675, 0.148125, 0.148625, 0.149375, 0.149125, 0.148875, 0.1485, 0.148125, 0.1475, 0.147375, 0.147125, 0.147, 0.14725, 0.14825, 0.147875, 0.146875, 0.146375, 0.146375, 0.145375, 0.144875, 0.145875, 0.146625, 0.14675, 0.146875, 0.146125, 0.1455, 0.144375, 0.147, 0.1465, 0.14575, 0.145375, 0.145125, 0.1455, 0.146, 0.1455, 0.145375, 0.145, 0.152625, 0.16175, 0.1765, 0.20775000000000002, 0.244375, 0.28725, 0.337125, 0.36112500000000003, 0.346875, 0.26787500000000003, 0.150125, 0.013000000000000001, -0.15425, -0.288625, -0.93, -0.055625, -0.055625, -0.055625, -0.055625, -0.055625, -0.055625, -0.055625, -0.055625, -0.055625, -0.055625, -0.055625, -0.107, -0.2585, -0.04275, 0.139625, 0.248875, 0.2915, 0.295125, 0.288875, 0.27525, 0.253625, 0.228125, 0.208875, 0.18987500000000002, 0.175375, 0.168, 0.16425, 0.162875, 0.1625, 0.162625, 0.162875, 0.163625, 0.163, 0.1625, 0.16175, 0.162, 0.1615, 0.1605, 0.1605, 0.160875, 0.1615, 0.16175, 0.160875, 0.16025, 0.160125, 0.15987500000000002, 0.15962500000000002, 0.1595, 0.1595, 0.15937500000000002, 0.15925, 0.15862500000000002, 0.15837500000000002, 0.15825, 0.1585, 0.15825, 0.15762500000000002, 0.15775, 0.15825, 0.15812500000000002, 0.15862500000000002, 0.15875, 0.15787500000000002, 0.15725, 0.15712500000000001, 0.157, 0.15625, 0.156125, 0.15637500000000001, 0.15637500000000001, 0.156125, 0.15662500000000001, 0.1565, 0.1555, 0.1545, 0.155, 0.15475, 0.1545, 0.154375, 0.154625, 0.15475, 0.154875, 0.1545, 0.1545, 0.15475, 0.154, 0.142125, 0.121625, 0.097375, 0.068125, 0.038625, 0.0095, -0.0205, -0.049875, -0.078, -0.074375, -0.0695, -0.080625, -0.099375, -0.11325, -0.131125, -0.154875, -0.181875, -0.2005, -0.21912500000000001, -0.242375, -0.2665, -0
#
#                                  ]
#          b, a = signal.butter(2, 0.5 / (500 / 2), "highpass")
#          cECG_Wave_RemoveRe_ReomvePowerLine = signal.filtfilt(b, a, cECG_Raw_WaveList);
#          print(cECG_Wave_RemoveRe_ReomvePowerLine)
#          # 2.50HZ工频
#          cECG_Wave_RemoveRe_ReomvePowerLine = signal.filtfilt(b1, a1,
#                                                               cECG_Wave_RemoveRe_ReomvePowerLine);
#          print("cECG_Wave_RemoveRe_ReomvePowerLine")
#          print(cECG_Wave_RemoveRe_ReomvePowerLine)
#          # 3.上限频率35HZ
#          cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN = signal.filtfilt(b2, a2,
#                                                                         cECG_Wave_RemoveRe_ReomvePowerLine)
#          print("cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN")
#          print(cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN)
#          # 最后可视化展示的最终结果
#          cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN_RemoveLFN = signal.filtfilt(b3, a3,
#                                                                                   cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN)
#          print("cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN_RemoveLFN")
#          print(cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN_RemoveLFN)
# def print_list(data1, data2, data3, data4, data5, data6, data7, data8, data9, data10, data11, data12, data13, data14, data15, data16, data17, data18, data19, data20, data21, data22, data23, data24, data25, data26, data27, data28, data29, data30, data31, data32, data33, data34, data35, data36, data37, data38, data39, data40, data41, data42, data43, data44, data45, data46, data47, data48, data49, data50, data51, data52, data53, data54, data55, data56, data57, data58, data59, data60, data61, data62, data63, data64, data65, data66, data67, data68, data69, data70, data71, data72, data73, data74, data75, data76, data77, data78, data79, data80, data81, data82, data83, data84, data85, data86, data87, data88, data89, data90, data91, data92, data93, data94, data95, data96, data97, data98, data99, data100, data101, data102, data103, data104, data105, data106, data107, data108, data109, data110, data111, data112, data113, data114, data115, data116, data117, data118, data119, data120, data121, data122
# ):
#         cECG_Raw_WaveList=[]
#         cECG_Raw_WaveList.append(data1)
#         cECG_Raw_WaveList.append(data2)
#         cECG_Raw_WaveList.append(data3)
#         cECG_Raw_WaveList.append(data4)
#         cECG_Raw_WaveList.append(data5)
#         cECG_Raw_WaveList.append(data6)
#         cECG_Raw_WaveList.append(data7)
#         cECG_Raw_WaveList.append(data8)
#         cECG_Raw_WaveList.append(data9)
#         cECG_Raw_WaveList.append(data10)
#         cECG_Raw_WaveList.append(data11)
#         cECG_Raw_WaveList.append(data12)
#         cECG_Raw_WaveList.append(data13)
#         cECG_Raw_WaveList.append(data14)
#         cECG_Raw_WaveList.append(data15)
#         cECG_Raw_WaveList.append(data16)
#         cECG_Raw_WaveList.append(data17)
#         cECG_Raw_WaveList.append(data18)
#         cECG_Raw_WaveList.append(data19)
#         cECG_Raw_WaveList.append(data20)
#         cECG_Raw_WaveList.append(data21)
#         cECG_Raw_WaveList.append(data22)
#         cECG_Raw_WaveList.append(data23)
#         cECG_Raw_WaveList.append(data24)
#         cECG_Raw_WaveList.append(data25)
#         cECG_Raw_WaveList.append(data26)
#         cECG_Raw_WaveList.append(data27)
#         cECG_Raw_WaveList.append(data28)
#         cECG_Raw_WaveList.append(data29)
#         cECG_Raw_WaveList.append(data30)
#         cECG_Raw_WaveList.append(data31)
#         cECG_Raw_WaveList.append(data32)
#         cECG_Raw_WaveList.append(data33)
#         cECG_Raw_WaveList.append(data34)
#         cECG_Raw_WaveList.append(data35)
#         cECG_Raw_WaveList.append(data36)
#         cECG_Raw_WaveList.append(data37)
#         cECG_Raw_WaveList.append(data38)
#         cECG_Raw_WaveList.append(data39)
#         cECG_Raw_WaveList.append(data40)
#         cECG_Raw_WaveList.append(data41)
#         cECG_Raw_WaveList.append(data42)
#         cECG_Raw_WaveList.append(data43)
#         cECG_Raw_WaveList.append(data44)
#         cECG_Raw_WaveList.append(data45)
#         cECG_Raw_WaveList.append(data46)
#         cECG_Raw_WaveList.append(data47)
#         cECG_Raw_WaveList.append(data48)
#         cECG_Raw_WaveList.append(data49)
#         cECG_Raw_WaveList.append(data50)
#         cECG_Raw_WaveList.append(data51)
#         cECG_Raw_WaveList.append(data52)
#         cECG_Raw_WaveList.append(data53)
#         cECG_Raw_WaveList.append(data54)
#         cECG_Raw_WaveList.append(data55)
#         cECG_Raw_WaveList.append(data56)
#         cECG_Raw_WaveList.append(data57)
#         cECG_Raw_WaveList.append(data58)
#         cECG_Raw_WaveList.append(data59)
#         cECG_Raw_WaveList.append(data60)
#         cECG_Raw_WaveList.append(data61)
#         cECG_Raw_WaveList.append(data62)
#         cECG_Raw_WaveList.append(data63)
#         cECG_Raw_WaveList.append(data64)
#         cECG_Raw_WaveList.append(data65)
#         cECG_Raw_WaveList.append(data66)
#         cECG_Raw_WaveList.append(data67)
#         cECG_Raw_WaveList.append(data68)
#         cECG_Raw_WaveList.append(data69)
#         cECG_Raw_WaveList.append(data70)
#         cECG_Raw_WaveList.append(data71)
#         cECG_Raw_WaveList.append(data72)
#         cECG_Raw_WaveList.append(data73)
#         cECG_Raw_WaveList.append(data74)
#         cECG_Raw_WaveList.append(data75)
#         cECG_Raw_WaveList.append(data76)
#         cECG_Raw_WaveList.append(data77)
#         cECG_Raw_WaveList.append(data78)
#         cECG_Raw_WaveList.append(data79)
#         cECG_Raw_WaveList.append(data80)
#         cECG_Raw_WaveList.append(data81)
#         cECG_Raw_WaveList.append(data82)
#         cECG_Raw_WaveList.append(data83)
#         cECG_Raw_WaveList.append(data84)
#         cECG_Raw_WaveList.append(data85)
#         cECG_Raw_WaveList.append(data86)
#         cECG_Raw_WaveList.append(data87)
#         cECG_Raw_WaveList.append(data88)
#         cECG_Raw_WaveList.append(data89)
#         cECG_Raw_WaveList.append(data90)
#         cECG_Raw_WaveList.append(data91)
#         cECG_Raw_WaveList.append(data92)
#         cECG_Raw_WaveList.append(data93)
#         cECG_Raw_WaveList.append(data94)
#         cECG_Raw_WaveList.append(data95)
#         cECG_Raw_WaveList.append(data96)
#         cECG_Raw_WaveList.append(data97)
#         cECG_Raw_WaveList.append(data98)
#         cECG_Raw_WaveList.append(data99)
#         cECG_Raw_WaveList.append(data100)
#         cECG_Raw_WaveList.append(data101)
#         cECG_Raw_WaveList.append(data102)
#         cECG_Raw_WaveList.append(data103)
#         cECG_Raw_WaveList.append(data104)
#         cECG_Raw_WaveList.append(data105)
#         cECG_Raw_WaveList.append(data106)
#         cECG_Raw_WaveList.append(data107)
#         cECG_Raw_WaveList.append(data108)
#         cECG_Raw_WaveList.append(data109)
#         cECG_Raw_WaveList.append(data110)
#         cECG_Raw_WaveList.append(data111)
#         cECG_Raw_WaveList.append(data112)
#         cECG_Raw_WaveList.append(data113)
#         cECG_Raw_WaveList.append(data114)
#         cECG_Raw_WaveList.append(data115)
#         cECG_Raw_WaveList.append(data116)
#         cECG_Raw_WaveList.append(data117)
#         cECG_Raw_WaveList.append(data118)
#         cECG_Raw_WaveList.append(data119)
#         cECG_Raw_WaveList.append(data120)
#         cECG_Raw_WaveList.append(data121)
#         cECG_Raw_WaveList.append(data122)
#         cECG_Raw_WaveList.append(data118)
#         cECG_Raw_WaveList.append(data119)
#         cECG_Raw_WaveList.append(data120)
#         cECG_Raw_WaveList.append(data121)
#         cECG_Raw_WaveList.append(data122)
#         print(cECG_Raw_WaveList)
#         fs = 500
#         num_ReRes = [0.99556697201764721, -1.9911339440352944, 0.99556697201764721]
#         den_ReRes = [1, -1.9911142922016536, 0.99115359586893548]
#         b1, a1 = signal.butter(2, [49 / (fs / 2), 51 / (fs / 2)], 'bandstop')  # 配置滤波器 2表示滤波器的阶数
#         b2, a2 = signal.butter(2, 35 / (fs / 2), 'lowpass');
#         b3, a3 = signal.butter(2, [0.1 / (fs / 2), 0.4 / (fs / 2)], 'bandstop')
#
#         b, a = signal.butter(2, 0.5 / (500 / 2), "highpass")
#        # print("tttttttttttt")
#        # print(type(cECG_Raw_WaveList))
#
#         cECG_Wave_RemoveRe_ReomvePowerLine = signal.filtfilt(b, a,
#                                                                          cECG_Raw_WaveList);
#         #print(cECG_Wave_RemoveRe_ReomvePowerLine)
#         peaks, _ = find_peaks(cECG_Wave_RemoveRe_ReomvePowerLine, height=-0.5,distance=250)
#         print(peaks,cECG_Wave_RemoveRe_ReomvePowerLine[peaks])
#         #heart_rate = 60 / np.mean(np.diff(peaks)
#         #print(heart_rate)
#         return cECG_Wave_RemoveRe_ReomvePowerLine
# def get_hear_rate():
#     #print("get_hear_rate:",heart_rate_mean)
#     return int(heart_rate_mean)
