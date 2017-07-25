package org.wso2.siddhi.extension.approximate.tdigest;

import java.util.Random;


public abstract class TDigest {


    protected Random gen = new Random();

    /**
     * TDigest provide a relative accuracy for different percentiles.
     * Therefore, to specify an accuracy, the percentile must also be specified.
     * @param percentile is a decimal in the range [0,1]
     * @param accuracy is a decimal in the range [0,1], lower the value higher the accuracy
     * @return
     */
    public static TDigest createDigest(double percentile, double accuracy){
//      accuracy = percentile * (1 - percentile) * certainty = percentile * (1 - percentile) / compression
        double compression = percentile * (1 - percentile) / accuracy;
        return createDigest(compression);
    }


    /**
     * Create a TDigest by specifying the compression
     * @param compression is the compression factor which is greater than 1
     * @return
     */
    public static TDigest createDigest(double compression) {
        if(compression < 1){
            throw new IllegalArgumentException("compression must be greater than 1");
        }
        return new AVLTreeDigest(compression);
    }

    /**
     * Adds a value to the digest
     *
     * @param x The value to add.
     * @param w The weight of this point.
     */
    public abstract void add(double x, int w);

    protected final void checkValue(double x) {
        if (Double.isNaN(x)) {
            throw new IllegalArgumentException("Cannot add NaN");
        }
    }


    public abstract void compress();


    /**
     * Returns the percentile(or percentile) value
     *
     * @param q The desired fraction
     * @return The value x such that cdf(x) == q
     */
    public abstract double percentile(double q);



    /**
     * Compute the weighted average between x1 with a weight of
     * w1 and x2 with a weight of w2
     */
    public static double weightedAverage(double x1, int w1, double x2, int w2) {
        return (x1 * w1 + x2 * w2) / (w1 + w2);
    }

    /**
     * Computes an interpolated value of a percentile that is between two centroids.
     *
     * @param index              percentile desired
     * @param previousIndex      percentile corresponding to the center of the previous centroid.
     * @param nextIndex          percentile corresponding to the center of the following centroid.
     * @param previousMean       The mean of the previous centroid.
     * @param nextMean           The mean of the following centroid.
     * @return  The interpolated mean.
     */
    static double percentile(double index, double previousIndex, double nextIndex, double previousMean, double nextMean) {
        final double delta = nextIndex - previousIndex;
        final double previousWeight = (nextIndex - index) / delta;
        final double nextWeight = (index - previousIndex) / delta;
        return previousMean * previousWeight + nextMean * nextWeight;
    }


    /**
     * Adds a value to the digest
     *
     * @param x The value to add.
     */
    public void add(double x) {
        add(x, 1);
    }




}
