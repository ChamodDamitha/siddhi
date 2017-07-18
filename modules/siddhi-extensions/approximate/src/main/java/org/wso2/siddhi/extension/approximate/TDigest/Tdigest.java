package org.wso2.siddhi.extension.approximate.TDigest;

/**
 * Created by chamod on 7/17/17.
 */
public abstract class Tdigest {
    /**
     * Creates an AVLTreeDigest.  AVLTreeDigest is generally the best known implementation right now.
     *
     * @param compression The compression parameter.  100 is a common value for normal uses.  1000 is extremely large.
     *                    The number of centroids retained will be a smallish (usually less than 10) multiple of this number.
     * @return the TDigest
     */
    public static Tdigest createDigest(double compression) {
        return new AVLTreeDigest(compression);
    }

    /**
     * Adds a sample to a histogram.
     *
     * @param x The value to add.
     * @param w The weight of this point.
     */
    public abstract void add(double x, int w);

    /**
     * Re-examines a t-digest to determine whether some centroids are redundant.  If your data are
     * perversely ordered, this may be a good idea.  Even if not, this may save 20% or so in space.
     * <p/>
     * The cost is roughly the same as adding as many data points as there are centroids.  This
     * is typically < 10 * compression, but could be as high as 100 * compression.
     * <p/>
     * This is a destructive operation that is not thread-safe.
     */
    public abstract void compress();

    /**
     * Returns an estimate of the cutoff such that a specified fraction of the data
     * added to this TDigest would be less than or equal to the cutoff.
     *
     * @param q The desired fraction
     * @return The value x such that cdf(x) == q
     */
    public abstract double quantile(double q);

    /**
     * Add a sample to this TDigest.
     *
     * @param x The data value to add with weight = 1
     */
    public void add(double x){
        add(x, 1);
    }

    /**
     * Compute the weighted average between <code>x1</code> with a weight of
     * <code>w1</code> and <code>x2</code> with a weight of <code>w2</code>.
     */
    public static double weightedAverage(double x1, int w1, double x2, int w2) {
        return (x1 * w1 + x2 * w2) / (w1 + w2);
    }

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

        return previousWeight * previousMean + nextWeight * nextMean;
    }

}
