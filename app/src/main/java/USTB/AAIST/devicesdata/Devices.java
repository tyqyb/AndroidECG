package USTB.AAIST.devicesdata;
//蓝牙扫描设备列表集合
import androidx.annotation.NonNull;

public class Devices {
    private String name;
    private String address;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }

    @NonNull
    @Override
    public String toString() {
        return "设备列表{" + "设备名：" + name + "=" + "MAC地址：" + address + "}";
    }
}
