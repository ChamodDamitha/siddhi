package org.wso2.siddhi.extension.approximate;

import org.apache.commons.math3.distribution.NormalDistribution;

/**
 * Created by chamod on 7/13/17.
 */
public class PercentileNormalApproximator implements PercentileCalculater{
    private double sum = 0;
    private double squareSum = 0;
    private double count = 0;
    private double mean;
    private double stdDeviation;

    @Override
    public void initialize(double certainty) {
        sum = 0;
        squareSum = 0;
        count = 0;
    }

    @Override
    public void add(double newData) {
        sum += newData;
        squareSum += (newData * newData);
        count ++;

        mean = sum/count;
        stdDeviation = Math.sqrt((squareSum/count) - (mean * mean));
    }

    @Override
    public double getPercentile(double percentileNumber) {
        return getpercentileValue(mean,stdDeviation,percentileNumber);
    }


    /**
     * This method calculates given percentile using mean and standard deviation
     *
     * @param mean
     * @param stdDeviation
     * @param percentile
     * @return
     */
    public Double getpercentileValue(Double mean, Double stdDeviation, Double percentile){
        double zValue = new NormalDistribution().inverseCumulativeProbability(percentile);
        return mean + zValue * stdDeviation;
    }
}
