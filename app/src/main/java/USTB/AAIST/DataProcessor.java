package USTB.AAIST;
import java.util.LinkedList;

public class DataProcessor {
    private LinkedList<Float> uriCache = new LinkedList<>();
    private LinkedList<Float> gluCache = new LinkedList<>();
    private LinkedList<Float> processedUriCache = new LinkedList<>();
    private LinkedList<Float> processedGluCache = new LinkedList<>();
    private int maxCacheSize = 1000;

    // 处理原始数据
    public void processAndCache(float uriValue, float gluValue) {
        // 缓存原始数据
        cacheData(uriCache, uriValue);
        cacheData(gluCache, gluValue);

        // 处理数据（当前只是×1.5倍）
        float processedUri = uriValue * 1.5f;
        float processedGlu = gluValue * 1.5f;

        // 缓存处理后的数据
        cacheData(processedUriCache, processedUri);
        cacheData(processedGluCache, processedGlu);
    }

    private void cacheData(LinkedList<Float> cache, float value) {
        if (cache.size() >= maxCacheSize) {
            cache.removeFirst();
        }
        cache.addLast(value);
    }

    // 获取处理后的数据数组
    public float[] getProcessedUriData() {
        return linkedListToFloatArray(processedUriCache);
    }

    public float[] getProcessedGluData() {
        return linkedListToFloatArray(processedGluCache);
    }

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

    public interface ProcessingAlgorithm {
        float process(float value);
    }
}
