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

package org.wso2.siddhi.extension.approximate.percentileExtension;

import org.apache.commons.math3.distribution.NormalDistribution;

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
    public void initialize(double percentile, double accuracy) {

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
