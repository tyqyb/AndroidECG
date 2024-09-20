package USTB.AAIST.utils;
/**数据转换工具类**/
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

    /**
     * 编辑时间：20240803
     * 编辑描述：以下的hexToList中包含两部分互斥的内容，Code_1避免因数据量的问题调用ECGChart中的python一次处理122个数据函数而导致闪退，直接将拿到的数据进行处理与绘制，return resultList;;
     * Code_2是源代码中调用Python时需要提前进行电压数据的滤波，将其单独拿出来作为HexDataFormatToStr函数，但目前没有进行调用，return cECG_Raw_Wave;
     * 使用说明：使用Code_1需要注释掉ECGChart中，public void run()函数中的1、2两个Python步骤，同时要将heartList.改为res.size()/get(),而使用Code_2则需解除该部分的注释
     * **/
    public static  ArrayList<Double>  hexToList(String str){
        //Code_1:20240726~20240803
        str = str.replace(" ", "");// 去除空格
        String[] splitHex = str.split("0A");// 按回车符0A分割
        ArrayList<Double> resultList = new ArrayList<>();

        for (String segment : splitHex) {
            if (segment.isEmpty()) {
                continue;
            }
            StringBuilder asciiString = new StringBuilder();
            // 将每个分割后的十六进制字符串转换为字节数组并构建字符串
            for (int i = 0; i < segment.length(); i += 2) {
                String hexByte = segment.substring(i, i + 2);
                int decimal = Integer.parseInt(hexByte, 16);
                asciiString.append((char) decimal);
            }
            try {// 将字符串转换为浮点数
                double value = Double.parseDouble(asciiString.toString());
                resultList.add(value);
            } catch (NumberFormatException e) {
                e.printStackTrace();// 如果无法转换为浮点数，则忽略该段
            }
        }
        return resultList;

        //Code_2:以下为原版本源代码
        /**将一个十六进制字符串转换为ECG原始波形数据列表，源代码于20240726注释
         * ### 实现原理
         * 1. 字符串分割：将输入的十六进制字符串按空格分割成一个字符串数组。
         * 2. 高低字节分离：将分割后的字符串数组中的每个元素转换为整数，并根据其位置（奇数或偶数）分别存储到两个列表中，一个列表存储低字节，另一个列表存储高字节。
         * 3. 合并高低字节：将低字节和高字节合并成一个整数，并减去32768（假设原始数据是16位的带符号整数），然后乘以4.096/32768（假设电压范围是-32768到32767，转换为电压值，转换系数为4.096）。
         * 4. 结果存储：将计算得到的电压值存储到一个列表中，并返回该列表。
         * ### 用途
         * 这段代码通常用于将来自ECG设备或传感器的原始数据（以十六进制字符串形式表示）转换为可读的电压值列表，以便进一步分析和处理。
         * ### 注意事项
         * 1. 输入格式：输入的十六进制字符串应该是由空格分隔的十六进制数，例如："FF 00 11 22"。
         * 2. 数据范围：代码假设原始数据是16位的带符号整数，并且电压范围是-32768到32767。如果实际情况不同，需要调整相应的计算公式。
         * 3. 数据对齐：代码假设输入的十六进制字符串长度是偶数，并且每两个十六进制数代表一个完整的16位数据。如果输入格式不符合这个假设，可能会导致错误。
         * 4. 异常处理：代码中没有处理可能的异常情况，例如输入字符串格式错误或转换失败。在实际应用中，应该添加适当的异常处理代码。
         * ### 示例
         * 假设输入的十六进制字符串是："FF 00 11 22"，那么代码的执行过程如下：
         * 1. 分割字符串得到数组：["FF", "00", "11", "22"]。
         * 2. 转换为整数并分离高低字节：
         *    - 低字节列表：[255, 17]
         *    - 高字节列表：[0, 34]
         * 3. 合并高低字节并计算电压值：
         *    - 第一个数据：255 + 0 * 256 = 255，255 - 32768 = -30213，-30213 * 4.096 / 32768 ≈ -1.5
         *    - 第二个数据：17 + 34 * 256 = 8739，8739 - 32768 = -23929，-23929 * 4.096 / 32768 ≈ -1.2
         * 4. 结果列表：[-1.5, -1.2]。
         * 这段代码将返回一个包含两个电压值的列表。
         * */
/*        //List<Integer> file_data = new ArrayList<>();
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
        return cECG_Raw_Wave;*/

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
        if (arr != null) {
            StringBuilder sb = new StringBuilder();
            for (byte b : arr)
                sb.append(String.format("%02x ", b));
            return sb.toString().trim().toUpperCase();//转字符串并且去除首尾空格（trim），转大写（toUpperCase），并返回新的字符串
        }
        else {return null;}
    }

    /**字符串转字节数组
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
