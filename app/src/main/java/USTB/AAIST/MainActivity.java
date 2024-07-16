package USTB.AAIST;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.fragment.app.FragmentTransaction;


import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
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
    private ActivityMainBinding binding;    //导航跳转


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 隐藏标题栏
//        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_main);

        // 隐藏标题栏，setContentView后调用，并且需要requestWindowFeature函数
        if (getSupportActionBar()!=null){
            getSupportActionBar().hide();
        }
        //初始化控件，并触发点击跳转事件
        initUI();
    }



//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        //初始化python环境
//        if(!Python.isStarted()){
//            Python.start(new AndroidPlatform(this));
//        }
//        Python python=Python.getInstance();
//        //调用hello_python.py里面的Python_say_Hello函式
//        PyObject pyObject=python.getModule("hello_python");
//        pyObject.callAttr("Python_say_Hello");
//
//    }


    /**
     * 初始化控件
     * 点击控件触发跳转事件
     */
    private void initUI() {
        cECG_Layout = findViewById(R.id.cECG_Layout);
       // Oxygen_Layout = findViewById(R.id.Oxygen_Layout);
        cECG_Layout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                //跳转至心电呼吸测量界面，可以删掉
//                Intent intent = new Intent();
//                intent.setClass(MainActivity.this,ECGChart.class);
//                startActivity(intent);
                //先跳转到蓝牙连接界面
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, BLE.class);
                startActivity(intent);
            }
        });

        //血氧脉搏
//        Oxygen_Layout.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                //跳转至血氧脉搏测量界面
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