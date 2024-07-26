package USTB.AAIST.utils;
/**文件工具组件
 * 默认存储的原始路径为：我的手机/Android/data/USTB.AAIST/files
 * **/
import android.content.Context;
import android.os.Environment;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

public class FileUtils {

    public static  String getFilesPath(Context context) {
        Calendar c = Calendar.getInstance();
        String fname="_心电滤波数据.csv";
        String filePath ;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !Environment.isExternalStorageRemovable()) {
            filePath = context.getExternalFilesDir(null).getPath() + "/" + fname;//外部存储可用
        } else {
            filePath = context.getFilesDir().getPath() + "/" + fname;//外部存储不可用
        }
        return filePath;
    }

    public static  void write(String filePath,String content){
        try {
            FileOutputStream outputStream = new FileOutputStream(filePath, true);// true 为追加写入，false 为删除写入(把之前的数据清除掉再写入)
            outputStream.write(content.getBytes());
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

/**写入原始路径，主要用到的是这个，和第一个代码一样**/
    public static  String getOrginateFilesPath(Context context,String id) {
        Calendar c = Calendar.getInstance();
        String fname=id+"_心电原始数据.csv";
        String filePath ;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !Environment.isExternalStorageRemovable()) {
            filePath = context.getExternalFilesDir(null).getPath() + "/" + fname;//外部存储可用
        } else {
            filePath = context.getFilesDir().getPath() + "/" + fname;//外部存储不可用
        }
        return filePath;
    }

/**写入16进制原始信号**/
    public static  void orginatewrite(String filePath,String content){
        try {
            FileOutputStream outputStream = new FileOutputStream(filePath, true);// true 为追加写入，false为删除写入
            outputStream.write(content.getBytes());
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

