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


public class AVLTreeDigest extends TDigest {

    private double compression;
    private AVLGroupTree avlGroupTree;
    long count = 0;


    public AVLTreeDigest(double compression) {
        this.compression = compression;
        avlGroupTree = new AVLGroupTree();
    }


    @Override
    public void add(double x, int w) {

//        set the start node
        checkValue(x);
        int start = avlGroupTree.floorNode(x);
        if (start == AVLTree.NIL) {
            start = avlGroupTree.leastNode();
        }

//        empty tree
        if (start == AVLTree.NIL) {
            assert avlGroupTree.size() == 0; // empty avlGroupTree
            avlGroupTree.add(x, w);
            count = w;
        }

//        tree has nodes
        else {
            double minDistance = Double.MAX_VALUE;
            int lastNeighbor = AVLTree.NIL;


//            choose the nearest neighbour from either sides
            for (int neighbor = start; neighbor != AVLTree.NIL; neighbor = avlGroupTree.nextNode(neighbor)) {
                double diff = Math.abs(avlGroupTree.mean(neighbor) - x);
                if (diff < minDistance) {
                    start = neighbor;
                    minDistance = diff;
                } else if (diff > minDistance) {
                    // as soon as diff increases, have passed the nearest neighbor and can quit
                    lastNeighbor = neighbor;
                    break;
                }
            }

            int closest = AVLTree.NIL;
            long sum = avlGroupTree.headSum(start);
            double n = 0;
            for (int neighbor = start; neighbor != lastNeighbor; neighbor = avlGroupTree.nextNode(neighbor)) {
                assert minDistance == Math.abs(avlGroupTree.mean(neighbor) - x);
                double q = count == 1 ? 0.5 : (sum + (avlGroupTree.count(neighbor) - 1) / 2.0) / (count - 1);
                double k = 4 * count * q * (1 - q) / compression;


//                check whether the value can be merged into the neighbour centroid
                if (avlGroupTree.count(neighbor) + w <= k) {
                    n++;

//                    with the increase of n, the probability of going inside the if condition decrease
                    if (gen.nextDouble() < 1 / n) {
                        closest = neighbor;
                    }
                }
                sum += avlGroupTree.count(neighbor);
            }

            if (closest == AVLTree.NIL) {
                avlGroupTree.add(x, w);
            } else {
                double centroid = avlGroupTree.mean(closest);
                int count = avlGroupTree.count(closest);

//                merge the value with the centroid
                centroid = weightedAverage(centroid, count, x, w);
                count += w;

                avlGroupTree.update(closest, centroid, count);
            }
            count += w;


            if (avlGroupTree.size() > 20 * compression) {
                compress();
            }
        }
    }

    /**
     * compress the tree to reduce the number of centroids
     */
    @Override
    public void compress() {
        if (avlGroupTree.size() <= 1) {
            return;
        }
//        create a new group tree
        AVLGroupTree centroids = avlGroupTree;
        this.avlGroupTree = new AVLGroupTree();

//        fill nodes in ascending order
        final int[] nodes = new int[centroids.size()];
        nodes[0] = centroids.leastNode();
        for (int i = 1; i < nodes.length; ++i) {
            nodes[i] = centroids.nextNode(nodes[i - 1]);
            assert nodes[i] != AVLTree.NIL;
        }
        assert centroids.nextNode(nodes[nodes.length - 1]) == AVLTree.NIL;


//        randomly swap the nodes
        for (int i = centroids.size() - 1; i > 0; --i) {
            final int other = gen.nextInt(i + 1);
            final int tmp = nodes[other];
            nodes[other] = nodes[i];
            nodes[i] = tmp;
        }


//        add the nodes to new tree
        for (int node : nodes) {
            add(centroids.mean(node), centroids.count(node));
        }
    }



    /**
     * @param q The quantile in the range [0,1].
     * @return
     */
    @Override
    public double quantile(double q) {
        if (q < 0 || q > 1) {
            throw new IllegalArgumentException("q should be in [0,1], got " + q);
        }

        AVLGroupTree groupTree = avlGroupTree;

//        empty tree, So no quantile
        if (groupTree.size() == 0) {
            return Double.NaN;
        }

//        only one centroid available
        else if (groupTree.size() == 1) {
            return groupTree.mean(groupTree.leastNode());
        }

        final double index = q * (count - 1);

        double previousMean = Double.NaN, previousIndex = 0;
        int next = groupTree.floorSumNode((long) index);
        assert next != AVLTree.NIL;
        long total = groupTree.headSum(next);
        final int prev = groupTree.previousNode(next);
        if (prev != AVLTree.NIL) {
            previousMean = groupTree.mean(prev);
            previousIndex = total - ((groupTree.count(prev) + 1.0) / 2);
        }

        while (true) {
            final double nextIndex = total + ((groupTree.count(next) - 1.0) / 2);

            if (nextIndex >= index) {
                if (Double.isNaN(previousMean)) {
                    assert total == 0 : total;
                    if (nextIndex == previousIndex) {
                        return groupTree.mean(next);
                    }
                    int next2 = groupTree.nextNode(next);
                    final double nextIndex2 = total + groupTree.count(next) + (groupTree.count(next2) - 1.0) / 2;
                    previousMean = (nextIndex2 * groupTree.mean(next) - nextIndex * groupTree.mean(next2)) / (nextIndex2 - nextIndex);
                }
                return quantile(index, previousIndex, nextIndex, previousMean, groupTree.mean(next));
            }
            else if (groupTree.nextNode(next) == AVLTree.NIL) {
                final double nextIndex2 = count - 1;
                final double nextMean2 = (groupTree.mean(next) * (nextIndex2 - previousIndex) - previousMean * (nextIndex2 - nextIndex)) / (nextIndex - previousIndex);
                return quantile(index, nextIndex, nextIndex2, groupTree.mean(next), nextMean2);
            }
            total += groupTree.count(next);
            previousMean = groupTree.mean(next);
            previousIndex = nextIndex;
            next = groupTree.nextNode(next);
        }
    }


}
