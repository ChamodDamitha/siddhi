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
package org.wso2.siddhi.extension.approximate;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A probabilistic data structure to keep count of different items
 * @param <E> is the type of data to be counted
 */
public class CountMinSketch<E> {
    private int depth;
    private int width;

//  2D array to store counts
    private long[][] countArray;

//  Error factors of approximation
    private double accuracy;
    private double certainty;


    private MessageDigest messageDigest;

    /**
     * instantiate the count min sketch based on a given accuracy and certainty
     * @param accuracy is a positive number less than 1 (e.g. 0.01)
     * @param certainty is a positive number less than 1 (e.g. 0.01)
     */
    public CountMinSketch(double accuracy, double certainty) {
        if( !(accuracy <= 1 && accuracy >= 0) || !(certainty <= 1 && certainty >=0)){
            throw new IllegalArgumentException("certainty and accuracy must be values in the range [0,1]");
        }

        this.accuracy = accuracy;
        this.certainty = certainty;

        this.depth = (int)Math.ceil(Math.log(1 / certainty));
        this.width = (int)Math.ceil(Math.E / accuracy);

        this.countArray = new long[depth][width];

//      setting MD5 digest function to generate hashes
        try {
            this.messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        System.out.println("depth : " + depth);//TODO : added for testing
        System.out.println("width : " + width);//TODO : added for testing
    }

    /**
     * Compute a cell position in a row of the count array for a given hash value
     * @param hash is the integer hash value generated from some hash function
     * @return an integer value in the range [0,width)
     */
    private int getArrayIndex(int hash){
        return Math.abs(hash % width);
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
     * Compute a set of different int values for a byte array of data
     * @param data is the array of bytes for which the hash values are needed
     * @param noOfHashes is the number of hash values needed
     * @return an int array of hash values
     */
    private int[] getHashValues(byte[] data, int noOfHashes){
        int[] hashValues = new int[noOfHashes];
        byte salt = 0;
        int completedHashes = 0;
        byte[] digest;
        int hash;

//      Loop until the number of hash values are completed
        while(completedHashes < noOfHashes) {
            messageDigest.update(salt);
            digest = messageDigest.digest(data);

//            System.out.println("digest " + digest.toString()); //TODO: added only for testing

//          jump from 4 by 4 to create some randomness
            for(int i = 0; i < digest.length / 4 ; i++){
                hash = 1;
                for(int j = 4 * i; j < (4 * i + 4); j++){
//                  multiply the hash by 2^8
                    hash <<= 8;
//                  the least significant byte of digest[j] is taken
                    hash |= ((int)digest[j]) & 0xff;


                    hashValues[completedHashes] = hash;
                    completedHashes++;

                    if(completedHashes == noOfHashes){
                        return hashValues;
                    }
                }
            }

//          salt is incremented to obtain new values for the digest
            salt++;

        }

        return hashValues;
    }


    /**
     * Adds the count of an item to the count min sketch
     * calculate hash values for number of row in the count array
     * compute indices in the range of [0, width) from those hash values
     * increment each value in the cell of relevant row and index (e.g. countArray[row][index]++)
     * @param item
     */
    public void insert(E item){
        int[] hashValues = getHashValues(getBytes(item), depth);
        int index;

        for(int i = 0; i < depth; i++){
            index = getArrayIndex(hashValues[i]);
            countArray[i][index]++;
        }
    }

    /**
     * Compute the approximate count for a given item
     * Check the relevant cell values for the given item by hashing it to cell indices
     * Then take the minimum out of those values
     * @param item
     * @return
     */
    public long approximateCount(E item){
        int[] hashValues = getHashValues(getBytes(item), depth);
        int index;

        long minCount = Long.MAX_VALUE;
        long tempCount;

        for(int i = 0; i < depth; i++){
            index = getArrayIndex(hashValues[i]);
            tempCount = countArray[i][index];
            if(tempCount < minCount){
                minCount = tempCount;
            }
        }
//      if item not found
        if(minCount == Long.MAX_VALUE){
            return 0;
        }
//      if item is found
        return minCount;
    }


    /**
     * Return the accuracy of the count min sketch
     * @return
     */
    public double getAccuracy() {
        return accuracy;
    }

    /**
     * Return the certainty of the count min sketch
     * @return
     */
    public double getCertainty() {
        return certainty;
    }
}
