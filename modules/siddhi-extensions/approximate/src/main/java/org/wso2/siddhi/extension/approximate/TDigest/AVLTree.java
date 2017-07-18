package org.wso2.siddhi.extension.approximate.TDigest;

import com.sun.org.apache.xml.internal.utils.IntStack;

import java.util.Arrays;

/**
 * Created by chamod on 7/17/17.
 */
public abstract class AVLTree {

    protected static final int NIL = 0;

    private final NodeAllocator nodeAllocator;

    private int root;
    private int[] parentNodes;
    private int[] leftNodes;
    private int[] rightNodes;
    private int[] depths;

    public AVLTree(int size) {
        this.nodeAllocator = new NodeAllocator();
        this.root = NIL;
        this.parentNodes = new int[size];
        this.leftNodes = new int[size];
        this.rightNodes = new int[size];
        this.depths = new int[size];
    }

    /**
     * default tree size = 16
     */
    public AVLTree(){
        this(16);
    }

    public int root() {
        return root;
    }

    public int capacity(){
        return parentNodes.length;
    }

    /**
     * Resize internal storage in order to be able to store data for nodes up to
     * <code>newCapacity</code> (excluded).
     */
    protected void resize(int newSize){
        parentNodes = Arrays.copyOf(parentNodes,newSize);
        leftNodes = Arrays.copyOf(leftNodes,newSize);
        rightNodes = Arrays.copyOf(rightNodes,newSize);
        depths = Arrays.copyOf(depths,newSize);
    }

    /** Grow a size of the tree by 1/8 times */
    private int oversize(int size){
        return size + (size >>> 3);
    }

    public int size(){
        return nodeAllocator.size();
    }

    /**
     * Return the parent of the provided node.
     */
    public int parentNode(int node) {
        return parentNodes[node];
    }

    /**
     * Return the left child of the provided node.
     */
    public int leftNode(int node) {
        return leftNodes[node];
    }

    /**
     * Return the right child of the provided node.
     */
    public int rightNode(int node) {
        return rightNodes[node];
    }

    /**
     * Return the depth of nodes that are stored below <code>node</code> including itself.
     */
    public int depth(int node) {
        return depths[node];
    }


    /**
     * Return the least node under node
     */
    public int leastNode(int node) {
        while (true){
            final int left = leftNode(node);
            if(left == NIL){
                break;
            }
            node = left;
        }
        return node;
    }

    /**
     * Return the largest node under <code>node</code>.
     */
    public int largestNode(int node) {
        while (true){
            final int right = rightNode(node);
            if(right == NIL){
                break;
            }
            node = right;
        }
        return node;
    }

    /**
     * Return the least node that is strictly greater than <code>node</code>.
     */
    public int nextNode(int node) {
        final int right = rightNode(node);
        if(right != NIL){
            return leastNode(right);
        }
        else{
            int parent = parentNode(node);
            while (parent != NIL && node == rightNode(node)){
                node = parent;
                parent = parentNode(parent);
            }
            return parent;
        }
    }

    /**
     * Return the highest node that is strictly less than <code>node</code>.
     */
    public final int previousNode(int node) {
        final int left = leftNode(node);
        if(left != NIL){
            return largestNode(left);
        }
        else{
            int parent = parentNode(node);
            while (parent != NIL && node == leftNode(node)){
                node = parent;
                parent = parentNode(parent);
            }
            return parent;
        }
    }


    /**
     * release an existing node
     */
    private void releaseNode(int node){
        setParentNode(node, NIL);
        setLeftNode(node, NIL);
        setRightNode(node, NIL);
        nodeAllocator.release(node);
    }

    public void setLeftNode(int node, int leftNode){
        assert node != NIL;
        leftNodes[node] = leftNode;
    }
    public void setRightNode(int node, int rightNode){
        assert node != NIL;
        rightNodes[node] = rightNode;
    }
    public void setParentNode(int node, int parentNode){
        assert node != NIL;
        parentNodes[node] = parentNode;
    }

    public void setDepth(int node, int depth){
        assert node != NIL;
        depths[node] = depth;
    }

    private void swapNodes(int n1, int n2){
        final int p1 = parentNode(n1);
        final int p2 = parentNode(n2);
//      set children of parents of p1 and p2
        if(p1 == NIL){
            assert root == n1;
            root = n2;
        }
        else{
            if(n1 == leftNode(p1)){
                setLeftNode(p1, n2);
            }
            else{
                assert n1 == rightNode(p1);
                setRightNode(p1, n2);
            }
        }

        if(p2 == NIL){
            assert root == n2;
            root = n1;
        }
        else{
            if(n2 == leftNode(p2)){
                setLeftNode(p2, n1);
            }
            else{
                assert n2 == rightNode(p2);
                setRightNode(p2, n1);
            }
        }
//        set parents of n1 and n2
        setParentNode(n1, p2);
        setParentNode(n2, p1);

        int l1 = leftNode(n1);
        int l2 = leftNode(n2);
        int r1 = rightNode(n1);
        int r2 = rightNode(n2);

//        set children of n1 and n2
        setLeftNode(n1, l2);
        setRightNode(n1, r2);
        setLeftNode(n2, l1);
        setRightNode(n2, r1);


//        set parents of chlidren of n1 and n2
        if(l2 != NIL){
            setParentNode(l2, n1);
        }
        if(r2 != NIL){
            setParentNode(r2, n1);
        }
        if(l1 != NIL){
            setParentNode(l1, n2);
        }
        if(r1 != NIL){
            setParentNode(r1, n2);
        }

//        set depths of n1 and n2
        final int d1 = depth(n1);
        final int d2 = depth(n2);

        setDepth(n1, d2);
        setDepth(n2, d1);

    }

    /**
     * return the difference of depths of left sub tree and right sub tree
     * @param node
     * @return
     */
    private int balanceFactor(int node){
        return depth(leftNode(node)) - depth(rightNode(node));
    }

    /**
     * Rotate the tree arround n to clockwise direction
     * @param n is the node
     */
    private void rotateLeft(int n) {
        final int p = parentNode(n);
        final int r = rightNode(n);
        final int lr = leftNode(r);

        setRightNode(n, lr);
        if(lr != NIL){
            setParentNode(n, lr);
        }

        setParentNode(r, p);
        if(p == NIL){
            root = r;
        }
        else if(n == leftNode(p)){
            setLeftNode(p, r);
        }
        else{
            assert n == rightNode(p);
            setRightNode(p, r);
        }

        setLeftNode(r, n);
        setParentNode(n, r);

//        fix the depth counts after rotation
        fixAggregateCounts(n);
        fixAggregateCounts(r);
    }

    /**
     * Rotate the tree arround n to anti-clockwise direction
     * @param n is the node
     */
    private void rotateRight(int n){
        final int p = parentNode(n);
        final int l = leftNode(n);
        final int rl = rightNode(l);

        setLeftNode(n, rl);
        if(rl != NIL){
            setParentNode(rl, n);
        }

        setParentNode(l, p);
        if(p == NIL){
            root = l;
        }
        else if(n == leftNode(p)){
            setLeftNode(p, l);
        }
        else{
            assert n == rightNode(p);
            setRightNode(p, l);
        }

        setRightNode(l, n);
        setParentNode(n, l);
//        fix the depth counts after rotation
        fixAggregateCounts(n);
        fixAggregateCounts(l);
    }

    /**
     * rebalance the tree to minimize the difference of depths between right and left sub trees
     * @param node arround which the rebalancing occurs
     */
    private void rebalanceTree(int node){
        for(int n = node; n != NIL ; ){
            final int p = parentNode(n);
            fixAggregateCounts(n);
            switch (balanceFactor(n)){
//                right sub tree is too deep
                case -2:
                    final int right = rightNode(n);
                    if(balanceFactor(right) == 1){
                        rotateRight(right);
                    }
                    rotateLeft(n);
                    break;
//                 left sub tree is too long
                case 2:
                    final int left = leftNode(n);
                    if(balanceFactor(left) == -1){
                        rotateLeft(left);
                    }
                    rotateRight(n);
                    break;
//                    other instances balanced or almost balanced
                case -1:
                    break;
                case 0:
                    break;
                case 1:
                    break;
                default:
                    throw new AssertionError();

            }
            n = p;
        }
    }


    /**
     * set the aggregate count of the node based on the maximum aggregate count of left and right child
     * @param node is the node
     */
    protected void fixAggregateCounts(int node) {
        setDepth(node, Math.max(depth(leftNode(node)),depth(rightNode(node))) + 1);
    }


    /**
     * Add current data to the tree and return <tt>true</tt> if a new node was added
     * to the tree or <tt>false</tt> if the node was merged into an existing node.
     */
    public boolean addNode() {
//        if the tree is empty
        if(root == NIL){
            root = nodeAllocator.newNode();
            copyNewData(root);
            fixAggregateCounts(root);
            return true;
        }
//        tree is not empty
        else{
            int node = root;
            assert parentNode(node) == NIL;
            int parent; // to track the final node
            int cmp;

            do{
                cmp = compare(node);
                if(cmp < 0){
                    parent = node;
                    node = leftNode(node);
                }
                else if(cmp > 0){
                    parent = node;
                    node = rightNode(node);
                }
                else{
                    return false; // TODO : find the reason
                }
            }while (node != NIL);

            node = nodeAllocator.newNode();

            if(node >= capacity()){
                resize(oversize(size() + 1));
            }

            copyNewData(node);
//            link the node to the tree
            setParentNode(node, parent);
            if(cmp < 0){
                setLeftNode(parent, node);
            }
            else{
                assert cmp > 0;
                setRightNode(parent, node);
            }

//          rebalance the tree after insertion of new node
            rebalanceTree(node);

            return true;
        }
    }

    /**
     * Update <code>node</code> with the current data.
     */
    public void updateNode(int node) {
//        find the closest nodes from both sides
        final int prev = previousNode(node);
        final int next = nextNode(node);
//      if node value is in between prev and next values
        if((prev == NIL || compare(prev) > 0) && (next == NIL || compare(next) < 0)){
            copyNewData(node);
            for(int n = node; n != NIL; n = parentNode(n)){
                fixAggregateCounts(n);
            }
        }
        else{
            // TODO: it should be possible to find the new node position without starting from scratch
            removeNode(node);
            addNode();
        }
    }

    /**
     * Remove the specified node from the tree.
     * @param node
     */
    private void removeNode(int node) {
        if(node == NIL){
            throw new IllegalArgumentException();
        }
//        have both children
        if(leftNode(node) != NIL && rightNode(node) != NIL){
            swapNodes(nextNode(node), node);
        }

//        have less than 2 children
        assert leftNode(node) == NIL || rightNode(node) == NIL;

        final int parent = parentNode(node);
        int child = leftNode(node);
        if(child == NIL){
            child = rightNode(node);
        }

//        have no children
        if(child == NIL){
            if(node == root){
                root = NIL;
            }
            else if(node == leftNode(parent)){
                setLeftNode(parent, NIL);
            }
            else{
                assert node == rightNode(parent);
                setRightNode(parent, NIL);
            }
        }
//        have 1 child
        else{
            if(node == root){
                root = child;
            }
            else if(node == leftNode(parent)){
                setLeftNode(parent, child);
            }
            else{
                assert node == rightNode(parent);
                setRightNode(parent, child);
            }
            setParentNode(child, parent);
        }

        releaseNode(node);
        rebalanceTree(parent);
    }


    /**
     * Compare data against data which is stored in <code>node</code>.
     */
    protected abstract int compare(int node);

    /**
     * Copy data into <code>node</code>.
     */
    protected abstract void copyNewData(int node);


    //    allocate new nodes and release nodes
    private static class NodeAllocator{

        private int nextNode;
        private IntStack releasableNodes;

        NodeAllocator(){
            nextNode = NIL + 1;
            releasableNodes = new IntStack();
        }

        int newNode(){
//          release an existing one
            if(releasableNodes.size() > 0){
                return releasableNodes.pop();
            }
            return nextNode++;
        }

//      used node is made available for reuse
        void release(int node){
            assert node < nextNode;
            releasableNodes.push(node);
        }

//      number of nodes in the tree
        int size(){
            return nextNode - releasableNodes.size() - 1;
        }
    }
}
