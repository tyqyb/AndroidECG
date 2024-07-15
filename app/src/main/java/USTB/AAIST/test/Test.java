package USTB.AAIST.test;

import java.util.ArrayList;

import USTB.AAIST.utils.DataFormatUtil;

public class Test {

    public static void main(String[] args) {
        // TODO Auto-generated method stub
//         Filtfilt.doFiltfilt();
//        ArrayList<Double> B = new ArrayList<Double>();
//        ArrayList<Double> A = new ArrayList<Double>();
        ArrayList<Double> X = new ArrayList<Double>();

//        B.add(0.99556697201764721);
//        B.add(-1.9911339440352944);
//        B.add( 0.99556697201764721);
//
//        A.add(1.0);
//        A.add(-1.9911142922016536);
//        A.add(0.99115359586893548);

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
       // System.out.println(X.size());

//122
//        ArrayList<Double> y = Filtfilt.doFiltfilt(B,A,X);
//        for (int i = 0; i < y.size(); i++)
//            System.out.println(y.get(i));
        ArrayList<Double> y =   DataFormatUtil.Filter(X);
//        for (int i = 0; i < y.size(); i++){
//            System.out.println(y.get(i));
//        }
        ArrayList<Double> z =   DataFormatUtil.Filter(X);
//        for (int i = 0; i < z.size(); i++){
//            System.out.println(z.get(i));
//        }

    }


}