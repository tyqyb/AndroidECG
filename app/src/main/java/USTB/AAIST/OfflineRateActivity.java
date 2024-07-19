package USTB.AAIST;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import java.util.ArrayList;

import USTB.AAIST.utils.FileUtils;
import USTB.AAIST.view.ECGAllDataView;
import USTB.AAIST.view.WH_ECGView;

public class OfflineRateActivity extends AppCompatActivity {
    private WH_ECGView ecgView;
    private ECGAllDataView allData_view;
    private  ArrayList<Double> orginate_data_source;//原始信号
    private ArrayList<Double> data_source;//滤波信号

    //提示框输入的id通过edit传入String orginatepath=中的第二个id参数，参考的是ECG中的同样的用法
    final EditText edit = new EditText(OfflineRateActivity.this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_rate);
        ecgView = (WH_ECGView)findViewById(R.id.ecg_data_ecgView);
        allData_view = (ECGAllDataView)findViewById(R.id.allData_ecgView);
        Intent intent = getIntent();
        if ("action".equals(intent.getAction())) {
            data_source = (ArrayList<Double>) intent.getSerializableExtra("Offline_RateData");
            orginate_data_source= (ArrayList<Double>) intent.getSerializableExtra("Offline_OrginateRateData");
            System.out.println("In OfflineRateActivity.Java, data_source.toString()");
            System.out.println(data_source.toString());
            System.out.println("data_source:"+data_source.size());

            //20240717解除以下两行注释，可能预见的冲突，在ECG中调用完后还会继续弹出
            String path= FileUtils.getFilesPath(OfflineRateActivity.this);
            String orginatepath = FileUtils.getOrginateFilesPath(OfflineRateActivity.this,edit.getText().toString());

            for(int i=0;i<orginate_data_source.size();i++){
                FileUtils.orginatewrite(orginatepath,orginate_data_source.get(i)+",\n");
            }
            //滤波信号
            for(int i=0;i<data_source.size();i++){
                FileUtils.write(path,data_source.get(i)+",\n");
            }
            ecgView.setData(data_source);
            allData_view.setData(data_source);
        }
    }
}