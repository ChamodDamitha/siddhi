package org.wso2.siddhi.extension.approximate;


import org.wso2.siddhi.extension.approximate.TDigest.TDigest;

/**
 * Created by chamod on 7/13/17.
 */
public class PercentileApproximator implements PercentileCalculater {
    private TDigest tDigest;

    @Override
    public void initialize(double certainty) {
        tDigest = TDigest.createDigest(1/certainty);
    }

    @Override
    public void add(double newData) {
        tDigest.add(newData);
        System.out.println(""+tDigest.compression());
    }

    @Override
    public double getPercentile(double percentileNumber) {
        return tDigest.quantile(percentileNumber);
    }
}
