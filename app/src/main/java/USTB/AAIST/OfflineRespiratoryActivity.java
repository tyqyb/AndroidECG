package USTB.AAIST;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;

import USTB.AAIST.utils.dsp.Filtfilt;
import USTB.AAIST.view.RespiratoryView;
import USTB.AAIST.view.Respiratory_AllData_View;


public class OfflineRespiratoryActivity extends AppCompatActivity {
    private Respiratory_AllData_View Respiratory_allData_View;
    private RespiratoryView Respiratory_View_offline;

    private ArrayList<Double> cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN;
    private ArrayList<Double> cECG_Envelop_Respiratory_Wave;

    // Low-pass filter with cutoff frequency of 0.4Hz
    private ArrayList<Double>  b3 = new ArrayList<>();
    private ArrayList<Double>  a3 =new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_respiratory);
        Intent intent = getIntent();
        //if ("action".equals(intent.getAction())) {
            cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN = (ArrayList<Double>) intent.getSerializableExtra("offline_Respiratorydata");
          //  System.out.println("offline_Respiratorydata:");

         //   System.out.println(cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.toString());
            //需要经过python呼吸波去处理

            int m = cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN.size();
            //System.out.println("m size is:"+m);
            ArrayList<Double> cECG_filter_process = cECG_Wave_RemoveRe_ReomvePowerLine_RemoveHFN;
            a3.add(1.0);
            a3.add( -2.989946914091736);
            a3.add(2.979944296951953);
            a3.add(-0.989997256494488);
            b3.add(1.579571604101638e-08);
            b3.add(4.738714812304913e-08);
            b3.add(4.738714812304913e-08);
            b3.add(1.579571604101638e-08);

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
           Log.d("ArrayList", res.toString());

         //   ecgView = (WH_ECGView)findViewById(R.id.ecg_data_ecgView2);
            Respiratory_View_offline =  findViewById(R.id.respiratory_view_offline);
           Respiratory_allData_View=findViewById(R.id.respiratory_allData_view);
            Respiratory_View_offline.setData(res);
            Respiratory_allData_View.setData(res);

    }
}