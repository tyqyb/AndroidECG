//传感数据接收处理方法类
package USTB.AAIST;
import java.util.LinkedList;

public class DataProcessor {
    // ========== 硬件配置及校准参数（需根据实际电路和传感器标定结果修正）==========
    // 尿酸 (URI) 参数
    private static final float VREF_URI = 3.3f;          // ADC 参考电压 (V)
    private static final float VZERO_URI = 1.65f;        // LMP91000 内部零点电压 (V) 例如 50% VDD
    private static final float RTIA_URI = 2750.0f;      // TIA跨阻增益电阻 (Ω)
    private static final float B_URI = 5.0e-8f;          // 拟合截距 (A) 例如 50 nA
    private static final float K_URI = 1.69e-7f;         // 拟合斜率 A/(mmol/L) 例如 169 nA/(mmol/L)

    // 汗糖 (GLU) 参数
    private static final float VREF_GLU = 3.3f;
    private static final float VZERO_GLU = 1.65f;
    private static final float RTIA_GLU = 2750.0f;
    private static final float B_GLU = 1.51e-6f;
    private static final float K_GLU = 2.108e-5f;

    // 电流方向标识：true 表示电流流出工作电极(电子从转移介体流向电极表面)，用公式 Vzero - Vout；false 表示电流流入WE，使用公式 Vout - Vzero
    // 需根据实验验证，浓度增加时ADC读数降低为流出，升高为流入
    private static final boolean CURRENT_DIRECTION_OUT = true;

    // ========== 缓冲区 ==========
    private LinkedList<Float> uriCache = new LinkedList<>();           // 原始URI ADC值缓存
    private LinkedList<Float> gluCache = new LinkedList<>();           // 原始GLU ADC值缓存
    private LinkedList<Float> processedUriCache = new LinkedList<>();  // 处理后URI浓度缓存
    private LinkedList<Float> processedGluCache = new LinkedList<>();  // 处理后GLU浓度缓存
    private int maxCacheSize = 1000;                                   // 最大缓存点数

    /**
     * 处理原始ADC值并缓存原始数据及计算结果
     * @param uriValue 尿酸原始ADC值 (0~4095，2^12-1)
     * @param gluValue 血糖原始ADC值 (0~4095，2^12-1)
     */
    public void processAndCache(float uriValue, float gluValue) {
        // 1. 缓存原始数据
        cacheData(uriCache, uriValue);
        cacheData(gluCache, gluValue);
        // 2. 计算尿酸浓度
        float concentrationUri = calculateConcentration(uriValue, VREF_URI, VZERO_URI, RTIA_URI, B_URI, K_URI, CURRENT_DIRECTION_OUT);
        // 3. 计算汗糖浓度
        float concentrationGlu = calculateConcentration(gluValue, VREF_GLU, VZERO_GLU, RTIA_GLU, B_GLU, K_GLU, CURRENT_DIRECTION_OUT);
        // 4. 缓存计算结果
        cacheData(processedUriCache, concentrationUri);
        cacheData(processedGluCache, concentrationGlu);
    }

    /**
     * 根据ADC采集值和相关硬件参数计算浓度
     * @param adc         原始ADC值 (0~4095)
     * @param vref        ADC参考电压 (V)
     * @param vzero       内部零点电压 (V)
     * @param rtia        跨阻增益 (Ω)
     * @param b           背景电流 (A)
     * @param k           灵敏度 (A/浓度单位)
     * @param directionOut true:电流流出WE, false:电流流入WE
     * @return 计算得到的浓度（单位与k和b匹配）
     */
    private float calculateConcentration(float adc, float vref, float vzero, float rtia, float b, float k, boolean directionOut) {
        float vout = (adc / 4095.0f) * vref;    //计算LMP91000输出电压

        float isensor;  // 根据电流方向计算传感器电流
        if (directionOut) {
            isensor = (vzero - vout) / rtia;   // 电流流出WE
        } else {
            isensor = (vout - vzero) / rtia;   // 电流流入WE
        }

        return (isensor - b) / k;   // 计算浓度
    }

    private void cacheData(LinkedList<Float> cache, float value) {
        if (cache.size() >= maxCacheSize) {
            cache.removeFirst();    //向缓存队列中添加数据
        }
        cache.addLast(value);
    }

    // 获取处理后的数据数组
    public float[] getProcessedUriData() {return linkedListToFloatArray(processedUriCache);}
    public float[] getProcessedGluData() {return linkedListToFloatArray(processedGluCache);}

    // 获取原始数据数组，没用到
    public float[] getRawUriData() {return linkedListToFloatArray(uriCache);}
    public float[] getRawGluData() {return linkedListToFloatArray(gluCache);}

    private float[] linkedListToFloatArray(LinkedList<Float> list) {
        float[] array = new float[list.size()];
        int i = 0;
        for (Float value : list) {
            array[i++] = value;
        }
        return array;
    }

    // 可以添加更多数据处理方法
    public float[] applyCustomProcessing(float[] data, ProcessingAlgorithm algorithm) {
        // 这里可以添加更复杂的数据处理算法
        float[] result = new float[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = algorithm.process(data[i]);
        }
        return result;
    }

    public interface ProcessingAlgorithm { float process(float value);}
}
