package org.wso2.siddhi.extension.approximate.TDigest;

import java.util.Random;

/**
 * Created by chamod on 7/18/17.
 */
public class AVLTreeDigest extends Tdigest{

    private double compression;
    private long count = 0;
    private Random random;

    private AVLGroupTree groupTree;


    public AVLTreeDigest(double compression) {
        this.compression = compression;
        this.groupTree = new AVLGroupTree();

        this.random = new Random();
    }

    @Override
    public void add(double x, int w) {
//        set the start node
        int start = groupTree.floorNode(x);
        if(start == AVLTree.NIL){
            start = groupTree.leastNode();
        }

//        empty tree
        if(start == AVLTree.NIL){
            assert groupTree.size() == 0;
            groupTree.add(x, w);
            count = w;
        }
//        tree has nodes
        else{
            double minDis = Double.MAX_VALUE;
            int lastNeighbour = AVLTree.NIL;
            double dif;

//            choose the nearest neighbour from either sides
            for(int neighbour = start; neighbour != AVLTree.NIL; neighbour = groupTree.nextNode(neighbour)){
                dif = Math.abs(x - groupTree.mean(neighbour));
                if(dif < minDis){
                    start = neighbour;
                    minDis = dif;
                }
                // as soon as z increases, we have passed the nearest neighbor and can quit
                else if (dif > minDis){
                    lastNeighbour = neighbour;
                    break;
                }
            }

            int closest = AVLTree.NIL;
            long sum = groupTree.lowerHeadSum(start);
            double n = 0;

            for(int neighbour = start; neighbour != lastNeighbour; neighbour = groupTree.nextNode(neighbour)){
                assert minDis == Math.abs(x - groupTree.mean(neighbour));

                double q = (count == 1 ? 0.5 : ((sum + (groupTree.count(neighbour) - 1) / 2.0) / (count - 1)));
                double k = 4 * count * q * (1 - q) / compression;

//                check whether the value can be merged into the neighbour centroid
                if(groupTree.count(neighbour) + w < k){
                    n++;
//                    with the increase of n, the probability of going inside the if condition decrease
                    if(random.nextDouble() < (1 / n)){
                        closest = neighbour;
                    }
                }
                sum += groupTree.count(neighbour);
            }

//TODO :: ??
            if(closest == AVLTree.NIL){
                groupTree.add(x, w);
            }
            else{
                // if the nearest point was not unique, then we may not be modifying the first copy
                // which means that ordering can change
                double centroid = groupTree.mean(closest);
                int centroid_count = groupTree.count(closest);

//                merge the value with the centroid
                centroid = weightedAverage(centroid, centroid_count, x, w);
                centroid_count += w;

                groupTree.update(closest, centroid, centroid_count);
            }
            count += w;

//            check size and compress
            if(groupTree.size() > 20 * compression){
                compress();
            }
        }

    }

    /**
     * compress the tree to reduce the number of centroids
     */
    @Override
    public void compress() {
        System.out.println("cxc ....");
        if(groupTree.size() <= 1){
            return;
        }

//        create a new group tree
        AVLGroupTree previousGroupTree = this.groupTree;
        this.groupTree = new AVLGroupTree();

//        fill nodes in ascending order
        final int[] nodes = new int[previousGroupTree.size()];
        nodes[0] = previousGroupTree.leastNode();
        for(int i = 1; i < nodes.length; i++){
            nodes[i] = previousGroupTree.nextNode(nodes[i - 1]);
        }

//        randomly swap the nodes
        for(int i = previousGroupTree.size() - 1; i > 0; i--){
            final int randomIndex = random.nextInt(i + 1);
            final int tmp = nodes[i];
            nodes[i] = nodes[randomIndex];
            nodes[randomIndex] = tmp;
        }

//        add the nodes to new tree
        for(int node : nodes){
            add(previousGroupTree.mean(node), previousGroupTree.count(node));
        }
    }



    @Override
    public double quantile(double q) {
        if(q < 0 || q > 1){
            throw  new IllegalArgumentException("q must be in range of [0, 1], inserted q value = " + q);
        }
//        empty tree, So no quantile
        if(groupTree.size() == 0){
            return Double.NaN;
        }
//        only one centroid available
        if(groupTree.size() == 1){
            return groupTree.mean(groupTree.leastNode());
        }

        final double index = q * (count - 1);
        double previousMean = Double.NaN;
        double previousIndex = 0;

        int next = groupTree.floorSumNode((long)index);
        assert next != AVLTree.NIL;

        long total = groupTree.lowerHeadSum(next);

        final int prev = groupTree.previousNode(next);

        if(prev != AVLTree.NIL){
            previousMean = groupTree.mean(prev);
            previousIndex = total - ((groupTree.count(prev) + 1.0) / 2);
        }


        while (true){
            final double nextIndex = total + ((groupTree.count(next) - 1.0) / 2);

            if(nextIndex >= index){
                if(Double.isNaN(previousMean)){
                    // special case 1: the index we are interested in is before the 1st centroid
                    assert total == 0;
                    if(nextIndex == previousIndex){
                        return groupTree.mean(next);
                    }
                    // assume values grow linearly between index previousIndex=0 and nextIndex2
                    int next2 = groupTree.nextNode(next);
                    final double nextIndex2 = total + groupTree.count(next) + (groupTree.count(next2) - 1.0) / 2;
                    previousMean = (nextIndex2 * groupTree.mean(next) - nextIndex * groupTree.mean(next2))
                            / (nextIndex2 - nextIndex);
                }
                // common case: we found two centroids previous and next so that the desired quantile is
                // after 'previous' but before 'next'
                return quantile(index, previousIndex, nextIndex, previousMean, groupTree.mean(next));
            }
            else if(groupTree.nextNode(next) == AVLTree.NIL){
                // special case 2: the index we are interested in is beyond the last centroid
                // again, assume values grow linearly between index previousIndex and (count - 1)
                // which is the highest possible index
                final double nextIndex2 = count - 1;
                final double nextMean2 = (groupTree.mean(next) * (nextIndex2 - previousIndex) - previousMean * (nextIndex2 - nextIndex))
                        / (nextIndex - previousIndex);

                return quantile(index, nextIndex, nextIndex2, groupTree.mean(next), nextMean2);
            }

            total += groupTree.count(next);
            previousMean = groupTree.mean(next);
            previousIndex = nextIndex;
            next = groupTree.nextNode(next);
        }

    }
}
