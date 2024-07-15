package USTB.AAIST.utils;
//文件工具
import android.content.Context;
import android.os.Environment;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

public class FileUtils {

    public static  String getFilesPath(Context context) {
        Calendar c = Calendar.getInstance();
        String fname="心电滤波数据.txt";
        String filePath ;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !Environment.isExternalStorageRemovable()) {
            //外部存储可用
            filePath = context.getExternalFilesDir(null).getPath() + "/" + fname;
        } else {
            //外部存储不可用
            filePath = context.getFilesDir().getPath() + "/" + fname;
        }
        return filePath;
    }

    public static  void write(String filePath,String content){
        try {
            // true 为 追加写入，false 为删除写入(把之前的数据清除掉，再写入)
            FileOutputStream outputStream = new FileOutputStream(filePath, true);
            outputStream.write(content.getBytes());
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //写入原始路径
    public static  String getOrginateFilesPath(Context context,String id) {
        Calendar c = Calendar.getInstance();
        String fname=id+"原始心电数据.txt";
        String filePath ;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !Environment.isExternalStorageRemovable()) {
            //外部存储可用
            filePath = context.getExternalFilesDir(null).getPath() + "/" + fname;
        } else {
            //外部存储不可用
            filePath = context.getFilesDir().getPath() + "/" + fname;
        }
        return filePath;
    }

    //写入16进制原始信号
    public static  void orginatewrite(String filePath,String content){
        try {
            FileOutputStream outputStream = new FileOutputStream(filePath, true);// true 为 追加写入，false 为删除写入(把之前的数据清除掉，再写入)
            outputStream.write(content.getBytes());
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

