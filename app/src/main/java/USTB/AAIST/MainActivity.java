package USTB.AAIST;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.fragment.app.FragmentTransaction;
import android.view.WindowManager;
import android.view.Window;
import com.chaquo.python.Kwarg;
import com.chaquo.python.Python;
import com.chaquo.python.PyObject;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import USTB.AAIST.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private LinearLayout cECG_Layout;
    private LinearLayout Oxygen_Layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 隐藏标题栏，setContentView后调用，并且需要requestWindowFeature函数
        if (getSupportActionBar()!=null){
            getSupportActionBar().hide();
        }
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);//状态栏字体变暗

        initUI();//初始化控件，并触发点击跳转事件

    }

    /**初始化控件，点击控件触发跳转事件**/
    private void initUI() {
        cECG_Layout = findViewById(R.id.cECG_Layout);
       // Oxygen_Layout = findViewById(R.id.Spo2_Layout);

        //心电点击事件监听
        cECG_Layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //先跳转到蓝牙连接界面
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, BLE.class);
                startActivity(intent);
            }
        });

        /**
         * 血氧点击事件监听，后续功能开放需解除如下注释：
         * ① private LinearLayout Oxygen_Layout;
         * ② Oxygen_Layout = findViewById(R.id.Spo2_Layout);
         * ③ Oxygen_Layout.setOnClickListener(new View.OnClickListener() {
         * **/
//        Oxygen_Layout.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                //跳转至血氧监测界面
//                Intent intent = new Intent();
//                intent.setClass(MainActivity.this,OxgenChartActivity.class);
//                startActivity(intent);
//            }
//        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.ble:
                Intent bleIntent = new Intent(this, BLE.class);
                startActivity(bleIntent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = new MenuInflater(this);
        menuInflater.inflate(R.menu.blemenu,menu);
        return super.onCreateOptionsMenu(menu);
    }
}