package org.wso2.siddhi.extension.approximate;

/**
 * Created by chamod on 7/13/17.
 */
public interface PercentileCalculater {
    void initialize(double certainty);
    void add(double newData);
    double getPercentile(double percentileNumber);
}
