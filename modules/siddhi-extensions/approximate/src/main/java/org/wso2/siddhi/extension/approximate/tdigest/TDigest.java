/*
* Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.siddhi.extension.approximate.tdigest;

import java.util.Random;


public abstract class TDigest {


    protected Random gen = new Random();


    public static TDigest createDigest(double compression) {
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
     * Returns the quantile(or percentile) value
     *
     * @param q The desired fraction
     * @return The value x such that cdf(x) == q
     */
    public abstract double quantile(double q);



    /**
     * Compute the weighted average between x1 with a weight of
     * w1 and x2 with a weight of w2
     */
    public static double weightedAverage(double x1, int w1, double x2, int w2) {
        return (x1 * w1 + x2 * w2) / (w1 + w2);
    }

    /**
     * Computes an interpolated value of a quantile that is between two centroids.
     *
     * @param index              quantile desired
     * @param previousIndex      quantile corresponding to the center of the previous centroid.
     * @param nextIndex          quantile corresponding to the center of the following centroid.
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
     * Adds a value to the digest
     *
     * @param x The value to add.
     */
    public void add(double x) {
        add(x, 1);
    }

}
