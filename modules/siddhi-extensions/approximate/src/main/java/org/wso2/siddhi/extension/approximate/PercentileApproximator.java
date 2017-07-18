package org.wso2.siddhi.extension.approximate;


import org.wso2.siddhi.extension.approximate.TDigest.Tdigest;

/**
 * Created by chamod on 7/13/17.
 */
public class PercentileApproximator implements PercentileCalculater {
    private Tdigest tDigest;

    @Override
    public void initialize(double certainty) {
        tDigest = Tdigest.createDigest(1/certainty);
    }

    @Override
    public void add(double newData) {
        tDigest.add(newData);
    }

    @Override
    public double getPercentile(double percentileNumber) {
        return tDigest.quantile(percentileNumber);
    }
}
