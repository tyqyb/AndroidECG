package USTB.AAIST.test;
/**Java的滤波测试20240723
 * 包含ABX三个数组，长度为3，3，122，分别调用Filtfilt.doFiltfilt(B,A,X);和DataFormatUtil.Filter(X);
 * 进行了功能验证**/
import java.util.ArrayList;
import USTB.AAIST.utils.DataFormatUtil;
import USTB.AAIST.utils.dsp.Filtfilt;

public class Test {
    public static void main(String[] args) {

        ArrayList<Double> B = new ArrayList<Double>();
        ArrayList<Double> A = new ArrayList<Double>();
        ArrayList<Double> X = new ArrayList<Double>();

        B.add(0.99556697201764721);
        B.add(-1.9911339440352944);
        B.add( 0.99556697201764721);

        A.add(1.0);
        A.add(-1.9911142922016536);
        A.add(0.99115359586893548);

        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.038875);
        X.add(-0.040125);
        X.add(-0.0405);
        X.add(-0.040125);
        X.add(-0.0405);

        System.out.println(X.size());//输出122
        System.out.println(A.size());//输出122
        System.out.println(B.size());//输出122

        //X\A\B列表数据处理测试
        System.out.println("==========================================X_x==========================================");
        ArrayList<Double> x = Filtfilt.doFiltfilt(B,A,X);
        for (int i = 0; i < x.size(); i++) {
            System.out.println(x.get(i));
        }
        //只处理X数组
        System.out.println("==========================================X_y==========================================");
        ArrayList<Double> y = DataFormatUtil.Filter(X);
        for (int i = 0; i < y.size(); i++){
            System.out.println(y.get(i));
        }

        /**A列表数据处理测试20240723
         * 预期的报错：输入数值X长度太小，数据最少是滤波器阶数的三倍（Filtfilt.Java）
         **/
/*        System.out.println(A.size());
        ArrayList<Double> a = DataFormatUtil.Filter(A);
        for (int i = 0; i < a.size(); i++){
            System.out.println(a.get(i));
        }*/
    }
}