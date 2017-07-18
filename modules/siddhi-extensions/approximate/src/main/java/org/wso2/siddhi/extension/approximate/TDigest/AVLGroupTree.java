package org.wso2.siddhi.extension.approximate.TDigest;

import java.util.Arrays;

/**
 * A tree of t-digest centroids.
 */
public class AVLGroupTree{

    /**
     * for insertion purposes
     */
    private double centroid;
    private int count;

    /**
     * for saving all the data
     */
    private double[] centroids;
    private int[] counts;
    private int[] aggregateCounts;

    private AVLTree tree;


    AVLGroupTree(){
        tree = new AVLTree() {

            /**
             * increasing the size of the tree and the arrays
             * @param newSize
             */
            @Override
            protected void resize(int newSize){
                super.resize(newSize);
                centroids = Arrays.copyOf(centroids, newSize);
                counts = Arrays.copyOf(counts, newSize);
                aggregateCounts = Arrays.copyOf(aggregateCounts, newSize);
            }

            /**
             * set the total number of nodes upto the given node from bottom
             * @param node is the node
             */
            @Override
            protected void fixAggregateCounts(int node) {
                super.fixAggregateCounts(node);
                aggregateCounts[node] = counts[node] + aggregateCounts[leftNode(node)] + aggregateCounts[rightNode(node)];
            }

            /**
             * the current centroid is compared with the centroid in the given node
             * @param node
             * @return -1 if current centroid is lesser, 0 or 1 if current centroid is larger
             */
            @Override
            protected int compare(int node) {
                if(centroid < centroids[node]){
                    return -1;
                }
                else {
                    // upon equality, the newly added node is considered greater
                    return 1;
                }
            }


            /**
             * Copy the newly arrive data to the given node
             * @param node
             */
            @Override
            protected void copyNewData(int node) {
                centroids[node] = centroid;
                counts[node] = count;
            }
        };

        centroids = new double[tree.capacity()];
        counts = new int[tree.capacity()];
        aggregateCounts = new int[tree.capacity()];
    }


    /**
     * Return the number of centroids in the tree
     * @return
     */
    public int size(){
        return tree.size();
    }

    /**
     * Return the previous node.
     */
    public int previousNode(int node){
        return tree.previousNode(node);
    }

    /**
     * Return the next node.
     */
    public int nextNode(int node){
        return tree.nextNode(node);
    }

    /**
     * Return the least node in the tree.
     */
    public int leastNode() {
        return tree.leastNode(tree.root());
    }

    /**
     * Return the mean for the provided node.
     */
    public double mean(int node){
        return centroids[node];
    }

    /**
     * Return the count for the provided node.
     */
    public int count(int node){
        return counts[node];
    }

    /**
     * Add the provided centroid to the tree.
     */
    public void add(double centroid, int count){
        this.centroid = centroid;
        this.count = count;
        tree.addNode();
    }

    /**
    * Update values associated with a node
    */
    public void update(int node, double centroid, int count){
        this.centroid = centroid;
        this.count = count;
        tree.updateNode(node);
    }

    /**
     * Return the last node whose centroid is less than the given <code>centroid</code>.
     */
    public int floorNode(double centroid) {
        int floor = AVLTree.NIL;
        int cmp;
        for(int node = tree.root(); node != AVLTree.NIL; ){
//            compare the centroids of each node with the given node and traverse
            cmp = Double.compare(centroid, mean(node));
            if(cmp <= 0){
                node = tree.leftNode(node);
            }
            else{
                floor = node;
                node = tree.rightNode(node);
            }
        }
        return floor;
    }

    /**
     * Return the last node so that the sum of counts of nodes that are before
     * it is less than or equal to <code>sum</code>.
     */
    public int floorSumNode(long sum) {
        int floor = AVLTree.NIL;
        int left;
        int leftCount;
        for(int node = tree.root(); node != AVLTree.NIL; ){
            left = tree.leftNode(node);
            leftCount = aggregateCounts[left];
//            left sub tree has more node counts than the sum, so go left
            if(leftCount > sum){
                node = tree.leftNode(node);
            }
//            sum is greater than the sum of counts in left sub tree, so go right
            else{
                floor = node;
                sum -= (leftCount + count(node));
                node = tree.rightNode(node);
            }
        }
        return floor;
    }


    /**
     * Compute the sum of counts for every entry that
     * is strictly before <code>node</code>.
     */
    public long lowerHeadSum(int node) {
        int leftSibling;
        int left = tree.leftNode(node);
        long sum = aggregateCounts[left];
//        go up the tree adding counts of left siblings and parents
        for(int n = node, p = tree.parentNode(node); p != AVLTree.NIL; n = p, p = tree.parentNode(n)){
            if(n == tree.rightNode(p)){
                leftSibling = tree.leftNode(p);
                sum += counts[p] + aggregateCounts[leftSibling];
            }
        }
        return sum;
    }





}
