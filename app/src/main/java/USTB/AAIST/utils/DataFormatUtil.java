package USTB.AAIST.utils;
//数据转换工具类
import java.util.ArrayList;
import java.util.List;

import USTB.AAIST.utils.dsp.Filtfilt;

public class DataFormatUtil {
//滤波相关参数
    static ArrayList<Double> B = new ArrayList<>();
    static ArrayList<Double> A = new ArrayList<>();
    static ArrayList<Double> B1 = new ArrayList<>();
    static ArrayList<Double> A1=  new ArrayList<>();
    static ArrayList<Double> B2 = new ArrayList<>();
    static ArrayList<Double> A2 = new ArrayList<>();

    public static List<Double> getRes() {
        return res;
    }

    static List<Double> res=new ArrayList<Double>();

//将16进制字符串通过十进制转换成数组之后进行计算得到计算后的数据
    public static  ArrayList<Double>  hexToList(String str){
        String[] strList =  str.split(" ");
//        List<Integer> file_data = new ArrayList<>();
        List<Integer> cECG_Wave_LowByte= new ArrayList<>();
        List<Integer> cECG_Wave_HighByte= new ArrayList<>();
        ArrayList<Double>  cECG_Raw_Wave= new ArrayList<>();
//        System.out.println("strList数组的长度为：  "+strList.length);
        for(int i=0;i<strList.length;i++){
            int x=Integer.parseInt(strList[i],16);
            if(i%2==0){
                cECG_Wave_LowByte.add(x);
            }else{
                cECG_Wave_HighByte.add(x);
            }

        }
        for(int i=0;i<cECG_Wave_HighByte.size();i++){
            double  cECG_Raw_Wave_data = cECG_Wave_LowByte.get(i) + cECG_Wave_HighByte.get(i) *256;//%%两个高字节+低字节组成的一个数
            cECG_Raw_Wave_data = cECG_Raw_Wave_data-32768;
            cECG_Raw_Wave_data = cECG_Raw_Wave_data* 4.096 / 32768;
            cECG_Raw_Wave.add(cECG_Raw_Wave_data);

            //滤波操作
//              #1 呼吸波
//                    cECG_Wave_RemoveRes = signal.filtfilt(Pyqt5_Serial.num_ReRes, Pyqt5_Serial.den_ReRes, Pyqt5_Serial.cECG_Raw_WaveList)  # 滤波
//            #2.50HZ工频
//                    cECG_Wave_RemoveRe_ReomvePowerLine = signal.filtfilt(Pyqt5_Serial.b1, Pyqt5_Serial.a1, cECG_Wave_RemoveRes);
//            #3.上限频率35HZ
//                    cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN = signal.filtfilt(Pyqt5_Serial.b2, Pyqt5_Serial.a2,
//                    cECG_Wave_RemoveRe_ReomvePowerLine)
            res.add(cECG_Raw_Wave_data);
        }

        return cECG_Raw_Wave;
    }
    /**
     * 字节数组转16进制字符串
     * @param arr 字节数组
     * @return 16进制字符串
     */
    public static String arrayToHex(byte[] arr) {
        if (arr != null) {
            StringBuilder sb = new StringBuilder();
            for (byte b : arr)
                sb.append(String.format("%02x ", b));
            return sb.toString().trim().toUpperCase();
        }

        return null;
    }

    /**
     * 字符串转字节数组
     * @param str 字符串
     * @return 字节数组
     */
    public static byte[] stringToBytes(String str) {
        if (str == null || str.trim().equals("")) {
            return new byte[0];
        }
        byte[] bytes = new byte[str.length() / 2];
        for (int i = 0; i < str.length() / 2; i++) {
            String subStr = str.substring(i * 2, i * 2 + 2);
            bytes[i] = (byte) Integer.parseInt(subStr, 16);
        }
        return bytes;
    }

    /**
     * 对从蓝牙传送过来的数据进行相应的滤波操作
     */
    public  static  ArrayList<Double>  Filter( ArrayList<Double> data){

        B.clear();
        A.clear();
        B1.clear();
        A1.clear();
        B2.clear();
        A2.clear();

        B.add(0.99556697201764721);
        B.add(-1.9911339440352944);
        B.add( 0.99556697201764721);

        A.add(1.0);
        A.add(-1.9911142922016536);
        A.add(0.99115359586893548);

        A1.add(1.0);
        A1.add(-3.207569239098683);
        A1.add(4.536785232165466);
        A1.add(-3.151064930277541);
        A1.add(0.965081173899132);

        B1.add(0.982385438526090);
        B1.add(-3.179317084688110);
        B1.add(4.537095529012413);
        B1.add(-3.179317084688110);
        B1.add(0.982385438526090);

        B2.add(0.027859766117136);
        B2.add(0.055719532234272);
        B2.add(0.027859766117136);

        A2.add(1.0);
        A2.add(-1.475480443592646);
        A2.add(0.586919508061190);
        //去除呼吸波
        ArrayList<Double> cECG_Wave_RemoveRes = Filtfilt.doFiltfilt(B,A,data);
        ArrayList<Double>  cECG_Wave_RemoveRe_ReomvePowerLine =  Filtfilt.doFiltfilt(B1,A1,cECG_Wave_RemoveRes);
        //低通
        ArrayList<Double>  cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN =  Filtfilt.doFiltfilt(B2,A2,cECG_Wave_RemoveRe_ReomvePowerLine);
//         B1.size()+":"+A1.size());

        return cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN;
    }






}
