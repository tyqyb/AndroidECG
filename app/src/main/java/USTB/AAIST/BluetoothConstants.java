package USTB.AAIST;

import java.util.UUID;

public final class BluetoothConstants {
    // 服务 UUID
    public static final String SERVICE_UUID_STRING = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public static final UUID SERVICE_UUID = UUID.fromString(SERVICE_UUID_STRING);
    // 特征值 UUID（收发数据）
    public static final String CHARACTERISTIC_UUID_STRING = "0000ffe1-0000-1000-8000-00805f9b34fb";
    public static final UUID CHARACTERISTIC_UUID = UUID.fromString(CHARACTERISTIC_UUID_STRING);
    // 客户端特征配置描述符（CCCD）
    public static final UUID CCC_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private BluetoothConstants() { } // 防止实例化
}