package org.wso2.siddhi.extension.approximate.TDigest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public abstract class AbstractTDigest extends TDigest {
    protected Random gen = new Random();
    protected boolean recordAllData = false;

    /**
     * Same as {@link #weightedAverageSorted(double, int, double, int)} but flips 
     * the order of the variables if <code>x2</code> is greater than 
     * <code>x1</code>. 
     */
    public static double weightedAverage(double x1, int w1, double x2, int w2) {
        if (x1 <= x2) {
            return weightedAverageSorted(x1, w1, x2, w2);
        } else {
            return weightedAverageSorted(x2, w2, x1, w1);
        }
    }

    /**
     * Compute the weighted average between <code>x1</code> with a weight of 
     * <code>w1</code> and <code>x2</code> with a weight of <code>w2</code>. 
     * This expects <code>x1</code> to be less than or equal to <code>x2</code> 
     * and is guaranteed to return a number between <code>x1</code> and 
     * <code>x2</code>. 
     */
    public static double weightedAverageSorted(double x1, int w1, double x2, int w2) {
        assert x1 <= x2;
        final double x = (x1 * w1 + x2 * w2) / (w1 + w2);
        return Math.max(x1, Math.min(x, x2));
    }


    abstract void add(double x, int w, Centroid base);


    /**
     * Computes an interpolated value of a quantile that is between two centroids. 
     *
     * Index is the quantile desired multiplied by the total number of samples - 1. 
     *
     * @param index              Denormalized quantile desired 
     * @param previousIndex      The denormalized quantile corresponding to the center of the previous centroid. 
     * @param nextIndex          The denormalized quantile corresponding to the center of the following centroid. 
     * @param previousMean       The mean of the previous centroid. 
     * @param nextMean           The mean of the following centroid. 
     * @return  The interpolated mean. 
     */
    static double quantile(double index, double previousIndex, double nextIndex, double previousMean, double nextMean) {
        final double delta = nextIndex - previousIndex;
        final double previousWeight = (nextIndex - index) / delta;
        final double nextWeight = (index - previousIndex) / delta;
        return previousMean * previousWeight + nextMean * nextWeight;
    }

    /**
     * Sets up so that all centroids will record all data assigned to them.  For testing only, really. 
     */
    @Override
    public TDigest recordAllData() {
        recordAllData = true;
        return this;
    }

    @Override
    public boolean isRecording() {
        return recordAllData;
    }

    /**
     * Adds a sample to a histogram. 
     *
     * @param x The value to add. 
     */
    @Override
    public void add(double x) {
        add(x, 1);
    }

    @Override
    public void add(TDigest other) {
        List<Centroid> tmp = new ArrayList<Centroid>();
        for (Centroid centroid : other.centroids()) {
            tmp.add(centroid);
        }

        Collections.shuffle(tmp, gen);
        for (Centroid centroid : tmp) {
            add(centroid.mean(), centroid.count(), centroid);
        }
    }

}