package USTB.AAIST.utils;
// 动态申请权限
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionUtil {
    //检查权限， 参数permissions为权限数组
    public static void checkPermission(Context context, int requestCode, String[] permissions) {
        //Android 8.0才用动态权限
        if (Build.VERSION.SDK_INT < 26) return;
        //创建未被授权的权限数组
        List<String> unauthorizedPermissions = new ArrayList<>();
        //逐个检查未通过的权限
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                //添加到未被授权数组中
                unauthorizedPermissions.add(permission);
            }
        }
        //如果有未被授权权限，继续申请权限。
        if (unauthorizedPermissions.size() > 0) {
            ActivityCompat.requestPermissions((Activity) context, permissions, requestCode);
        }
    }
}
