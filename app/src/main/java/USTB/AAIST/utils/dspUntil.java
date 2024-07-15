package USTB.AAIST.utils;


public class dspUntil {
    public synchronized double[] IIRFilter(double[] signal, double[] a, double[] b) {

        double[] in = new double[b.length];
        double[] out = new double[a.length - 1];
        double[] outData = new double[signal.length];

        for (int i = 0; i < signal.length; i++) {
            System.arraycopy(in, 0, in, 1, in.length - 1);
            in[0] = signal[i];
            //calculate y based on a and b coefficients
            //and in and out.
            float y = 0;
            for (int j = 0; j < b.length; j++) {
                y += b[j] * in[j];
            }

            for (int j = 0; j < a.length - 1; j++) {
                y -= a[j + 1] * out[j];
            }

            //shift the out array
            System.arraycopy(out, 0, out, 1, out.length - 1);
            out[0] = y;
            outData[i] = y;

        }
        return outData;
    }
}