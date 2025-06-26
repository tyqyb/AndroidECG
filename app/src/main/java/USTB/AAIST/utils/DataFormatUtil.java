package USTB.AAIST.utils;
/**数据转换工具类**/
import java.util.ArrayList;
import java.util.List;
import USTB.AAIST.utils.dsp.Filtfilt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    /**
     * 修复版ECG数据解析方法 - 直接处理字节数组
     *
     * @param data 原始字节数据
     * @return 解析后的ECG数据列表
     */
    public static  ArrayList<Double>  hexToList(byte[] data){
        ArrayList<Double> resultList = new ArrayList<>();

        if (data == null || data.length == 0) {
            return resultList;
        }

        // 调试日志
        System.out.println("原始数据字节: " + Arrays.toString(data));

        try {
            // 协议格式：每2字节组成一个ECG数据点（小端序）
            for (int i = 0; i < data.length - 1; i += 2) {
                // 组合两个字节为有符号整数 (小端序)
                int lowByte = data[i] & 0xFF;
                int highByte = data[i + 1] & 0xFF;
                int rawValue = (highByte << 8) | lowByte;

                // 转换为带符号整数（假设16位ADC）
                int signedValue = rawValue - 32768; // 调整符号

                // 转换为电压值（根据设备规格）
                double voltage = signedValue * (4.096 / 32768.0);

                // 添加有效ECG数据点
                resultList.add(voltage);

                // 调试日志
                System.out.printf("解析点 %d: raw=0x%04X (%d), voltage=%.4fV%n",
                        i/2, rawValue, signedValue, voltage);
            }
        } catch (Exception e) {
            System.err.println("ECG数据解析错误: " + e.getMessage());
            e.printStackTrace();
        }

        return resultList;
    }


    /**
     * 编辑时间：20240803
     * 编辑描述：将版本源代码中的代码注释并将字符转换的功能单独拿了出来，成为一个新的函数，目前还没有调用，
     * 取决于接收到的数据是电压数据还是标准的数据
     **/
    public static ArrayList<Double> HexDataFormatToStr (String str){
        //以下为源代码
        //List<Integer> file_data = new ArrayList<>();
        String[] strList =  str.split(" ");//将输入的十六进制字符串按回车符分割成一个字符串数组。
        List<Integer> cECG_Wave_LowByte= new ArrayList<>();
        List<Integer> cECG_Wave_HighByte= new ArrayList<>();
        ArrayList<Double>  cECG_Raw_Wave= new ArrayList<>();

        //将分割后的字符串数组中的每个元素转换为整数，并根据其位置（奇数或偶数）分别存储到两个列表中，一个列表存储低字节，另一个列表存储高字节。
        for(int i=0;i<strList.length;i++){
            int x=Integer.parseInt(strList[i],16);
            if(i%2==0){
                cECG_Wave_LowByte.add(x);
            }else{
                cECG_Wave_HighByte.add(x);
            }
        }
        for(int i=0;i<cECG_Wave_HighByte.size();i++){
            double  cECG_Raw_Wave_data = cECG_Wave_LowByte.get(i) + cECG_Wave_HighByte.get(i) *256;//两个高字节+低字节组成的一个数
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



    /**字节数组转16进制字符串
     * @param arr 字节数组
     * @return 16进制字符串
     */
    public static String arrayToHex(byte[] arr) {
        if (arr == null) return "";

        StringBuilder sb = new StringBuilder(arr.length * 3);
        for (byte b : arr) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    /**字符串转字节数组
     * @param str 字符串
     * @return 字节数组
     */
    public static byte[] stringToBytes(String str) {
        if (str == null || str.trim().equals("")) {
            return new byte[0];
        }
        // 移除所有空格
        String cleanStr = str.replaceAll("\\s", "");
        if (cleanStr.length() % 2 != 0) {
            throw new IllegalArgumentException("十六进制字符串长度必须为偶数");
        }
        byte[] bytes = new byte[str.length() / 2];
        for (int i = 0; i < str.length() / 2; i++) {
            String subStr = str.substring(i * 2, i * 2 + 2);
            bytes[i] = (byte) Integer.parseInt(subStr, 16);
        }
        return bytes;
    }

    /**对从蓝牙传送过来的数据进行相应的滤波操作，源代码历史提交中的 增加了滤波功能
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
