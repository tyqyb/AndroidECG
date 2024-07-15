package USTB.AAIST.utils;

import java.util.ArrayList;

import USTB.AAIST.utils.dsp.Filtfilt;

public class RespiratoryCalc {
    // 低通滤波器，截止频率为 0.4Hz
    private static ArrayList<Double>  b3 = new ArrayList<>();
    private static ArrayList<Double>  a3 =new ArrayList<>();

    public static ArrayList<Double> respiratoryCalc(ArrayList<Double> cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN){
        a3.clear();
        b3.clear();
        a3.add(1.0);
        a3.add( -2.989946914091736);
        a3.add(2.979944296951953);
        a3.add(-0.989997256494488);
        b3.add(1.579571604101638e-08);
        b3.add(4.738714812304913e-08);
        b3.add(4.738714812304913e-08);
        b3.add(1.579571604101638e-08);
        int m = cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.size();
        //System.out.println("m size is:"+m);
        ArrayList<Double> cECG_filter_process = cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN;
        // Compute mean of all points
        double cECG_mean = 0.0;
        for (int i = 0; i < m; i++) {
            cECG_mean += cECG_filter_process.get(i);
        }
        cECG_mean /= m;
        // System.out.println("cECG_mean:"+cECG_mean);
        // Subtract mean from all points
        double[] cECG_Demean = new double[m];
        for (int i = 0; i < m; i++) {
            cECG_Demean[i] = cECG_filter_process.get(i) - cECG_mean;
            //   System.out.println("cECG_Demean:"+cECG_Demean);
        }

        // Rectify signal
        double[] cECG_absolute = new double[m];
        for (int i = 0; i < m; i++) {
            cECG_absolute[i] = Math.abs(cECG_Demean[i]);
        }
        double cECG_absolute_mean = 0.0;
        for (int i = 0; i < m; i++) {
            cECG_absolute_mean += cECG_absolute[i];
        }
        cECG_absolute_mean /= m;
        //System.out.println("cECG_absolute_mean:"+cECG_absolute_mean);
        double[] cECG_absolute_Demean = new double[m];
        for (int i = 0; i < m; i++) {
            cECG_absolute_Demean[i] = cECG_absolute[i] - cECG_absolute_mean;
        }
        // Compute envelope (respiratory wave) using low-pass filter
        ArrayList<Double> cECG_Envelop_Respiratory_Wave = new ArrayList<>(m);
        for (int i = 0; i < m; i++) {
            cECG_Envelop_Respiratory_Wave.add(cECG_absolute_Demean[i]);
            //cECG_Envelop_Respiratory_Wave.set(i+1,cECG_absolute_Demean[i]);
        }
        ArrayList<Double> res=  Filtfilt.doFiltfilt(b3,a3,cECG_Envelop_Respiratory_Wave);
        return res;
    }
}
