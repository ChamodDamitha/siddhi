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
package org.wso2.siddhi.extension.approximate.cardinality;

import org.apache.log4j.Logger;
import org.wso2.siddhi.core.config.SiddhiContext;
import org.wso2.siddhi.extension.approximate.percentile.tdigest.AVLTree;
import sun.rmi.runtime.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;



public class HyperLogLog<E> {


    protected static final Logger LOG = Logger.getLogger(HyperLogLog.class);

    private double accuracy;
    private int noOfBuckets;
    private int lengthOfBucketId;
    private int[] countArray;

    private final double ERROR_FACTOR = 1.04;
    private final double ESTIMATION_FACTOR = 0.7;

    private MessageDigest messageDigest;

    /**
     * Create a new HyperLogLog by specifying the accuracy
     * Based on the accuracy the array size is calculated
     * @param accuracy is a number in the range (0, 1)
     */
    public HyperLogLog(double accuracy) {
        this.accuracy = accuracy;

//      accuracy = ERROR_FACTOR / sqrt(noOfBuckets) = > noOfBuckets = (ERROR_FACTOR / accuracy) ^ 2
        noOfBuckets = (int)Math.ceil(Math.pow(ERROR_FACTOR / accuracy, 2));
//        System.out.println("array size I : " + noOfBuckets); //TODO : added for testing

        lengthOfBucketId = (int)Math.ceil(Math.log(noOfBuckets) / Math.log(2));
//        System.out.println("bucket size : " + lengthOfBucketId); //TODO : added for testing

        noOfBuckets = (1 << lengthOfBucketId);
//        System.out.println("array size II : " + noOfBuckets); //TODO : added for testing

        countArray = new int[noOfBuckets];

//        setting MD5 digest function to generate hashes
        try {
            this.messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Error while ....'" + "' in Siddhi app , " + e.getMessage(), e);
        }

    }

    /**
     * Compute the accuracy using the count array size
     * @return the accuracy value
     */
    public double getAccuracy(){
        return (ERROR_FACTOR / Math.sqrt(noOfBuckets));
    }

    /**
     * Calculate the cardinality(number of unique items in a set)
     * by calculating the harmonic mean of the counts in the buckets.
     * Check for the upper and lower bounds to modify the estimation.
     * @return the cardinality value
     */
    public long getCardinality(){
//        printArray(); // TODO : added for testing

        double harmonicCountSum = 0;
        double harmonicCountMean;
        int noOfZeroBuckets = 0;
        int estimatedCardinality;
        long count;
        long cardinality;

//      calculate harmonic mean of the bucket values
        for(int i = 0; i < noOfBuckets; i++){
            count = countArray[i];
            harmonicCountSum += (1.0 / (1 << count));

//            System.out.println("count : " + count); //TODO : added for testing
//            System.out.println("harm plus : " + (1 << count)); //TODO : added for testing

            if(count == 0){
                noOfZeroBuckets++;
            }
        }

        harmonicCountMean = noOfBuckets / harmonicCountSum;

//      calculate the estimated cardinality
        estimatedCardinality = (int)Math.ceil(noOfBuckets * ESTIMATION_FACTOR * harmonicCountMean);

        final double _2pow32 = Math.pow(2,32);

//      if the estimate E is less than 2.5 * 32 and there are buckets with max-leading-zero count of zero,
//      then instead return −32⋅log(V/32), where V is the number of buckets with max-leading-zero count = 0.
//      threshold of 2.5x comes from the recommended load factor for Linear Counting
        if((estimatedCardinality < 2.5 * noOfBuckets) && noOfZeroBuckets > 0){
//            System.out.println("small....."); // TODO : added for testing
            cardinality = (int)(-noOfBuckets * Math.log((double) noOfZeroBuckets / noOfBuckets));
//            cardinality =  (long)(noOfBuckets * Math.log((double) noOfBuckets / noOfZeroBuckets));
        }
//       if E > 2 ^ (32) / 30 : return −2 ^ (32) * log(1 − E / 2 ^ (32))
        else if(estimatedCardinality > (_2pow32 / 30.0)){
            cardinality = (int)Math.ceil(-(_2pow32 * Math.log(1 - (estimatedCardinality / (_2pow32)))));
        }
        else{
//            System.out.println("medium....."); // TODO : added for testing
            cardinality = estimatedCardinality;
        }

////      TODO : added for testing
//        String confidenceInterval = String.format("confidence interval of cardinality : [%s, %s]",
//                cardinality - cardinality * getAccuracy(), cardinality + cardinality * getAccuracy());
//        System.out.println(confidenceInterval);

        return cardinality;
    }

    /**
     * Adds a new item to the array by hashing and setting the count of relevant bucckets
     * @param item
     */
    public void addItem(E item){
        int hash = getHashValue(item);
//        System.out.println("hash : " + Integer.toBinaryString(hash)); //TODO : added for testing

//      Shift all the bits to right till only the bucket ID is left
        int bucketId = hash >>> (Integer.SIZE - lengthOfBucketId);
//        System.out.println("bucketID : " + Integer.toBinaryString(bucketId)); //TODO : added for testing

//      Shift all the bits to left till the bucket id is removed
        int remainingValue = hash << lengthOfBucketId | (1 << (lengthOfBucketId - 1));
//        System.out.println("Remaining value : " + Integer.toBinaryString(remainingValue)); //TODO : added for testing

        int noOfLeadingZeros = Integer.numberOfLeadingZeros(remainingValue) + 1;
//        System.out.println("no of leading zeros : " + noOfLeadingZeros); //TODO : added for testing

        updateBucket(bucketId, noOfLeadingZeros);
    }

    /**
     * Update the zero count value in the relevant bucket if the given value is larger than the existing value
     * @param index is the bucket ID of the relevant bucket
     * @param leadingZeroCount is the new zero count
     * @return {@code true} if the bucket is updated, {@code false} if the bucket is not updated
     */
    private boolean updateBucket(int index, int leadingZeroCount){
        long currentZeroCount = countArray[index];
        if(currentZeroCount < leadingZeroCount){
            countArray[index] = leadingZeroCount;
            return true;
        }
        return false;
    }


    /**
     * Compute an integer hash value for a given value
     * @param value to be hashed
     * @return integer hash value
     */
    public int getHashValue(E value){
        byte[] bytes = messageDigest.digest(getBytes(value));
        return MurmurHash.hash32(bytes, 10);
    }

    /**
     * return a byte array for input data of type E
     * @param data
     * @return a byte array
     */
    private byte[] getBytes(E data){
        return data.toString().getBytes();
    }

    /**
     * Print the contents of the count array
     */
    private void printArray(){
        for(long x : countArray){
            System.out.print(x + ", ");
        }
        System.out.println();
    }
}


