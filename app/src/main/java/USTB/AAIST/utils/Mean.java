package USTB.AAIST.utils;

import java.util.ArrayList;

public  class Mean {
    public static  double mean(ArrayList<Double> arr){
        double res=0;
        for(int i=0;i<arr.size();i++){
            res+=arr.get(i);
        }
        res = res/arr.size();
        return res;
    }
}
