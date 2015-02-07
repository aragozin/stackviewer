package org.gridkit.sjk.ssa.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

@SuppressWarnings("serial")
public class StackTreeModel extends DefaultTreeModel {

    static final Comparator<FrameNode> FREQ_COMPARATOR = new FrequencyComparator();
    
    private StackTree tree;
    private String filterClassification;
    private String filterBucket;
    
    public StackTreeModel() {
        super(new Empty());
    }

    public StackTree getStackTree() {
        return tree;
    }
    
    public void setTree(StackTree tree) {
        this.tree = tree;
        if (tree == null) {
            setRoot(new Empty());
        }
        else {
            setRoot(new FrameNode(tree, new StackTraceElement[0], filterClassification, filterBucket));
        }
    }
    
    public void setBucketFilter(String filterClassification, String filterBucket) {
        this.filterClassification = filterClassification;
        this.filterBucket = filterBucket;
        updateRoot(filterClassification, filterBucket);
    }

    protected void updateRoot(String filterClassification, String filterBucket) {
        setRoot(new FrameNode(tree, new StackTraceElement[0], filterClassification, filterBucket));
    }
    
    public TreePath toTreePath(StackTraceElement[] path) {
        Object root =  getRoot();
        if (root instanceof FrameNode) {
            List<FrameNode> treePath = new ArrayList<FrameNode>();
            FrameNode node = (FrameNode) root;
            treePath.add(node);
            pathLoop:
            for(int i = 0; i != path.length; ++i) {
                for(FrameNode child: node.childList()) {
                    if (child.getFrame().equals(path[i])) {
                        treePath.add(child);
                        node = child;
                        continue pathLoop;
                    }
                }
                // path element is not found
                return null;
            }
            return new TreePath(treePath.toArray());
        }
        else {
            return null;
        }
    }
    
    public static class Empty implements TreeNode {

        @Override
        public TreeNode getChildAt(int childIndex) {
            return null;
        }

        @Override
        public int getChildCount() {
            return 0;
        }

        @Override
        public TreeNode getParent() {
            return null;
        }

        @Override
        public int getIndex(TreeNode node) {
            return -1;
        }

        @Override
        public boolean getAllowsChildren() {
            return true;
        }

        @Override
        public boolean isLeaf() {
            return true;
        }

        @Override
        @SuppressWarnings("rawtypes")
        public Enumeration children() {
            return new Vector().elements();
        }        
        
        public String toString() {
            return "";
        }
    }
    
    public static class FrameNode implements TreeNode, FrameInfo {

        StackTree tree;
        StackTraceElement[] path;
        String classification;
        String bucket;
        
        public FrameNode(StackTree tree, StackTraceElement[] path, String classification, String bucket) {
            this.tree = tree;
            this.path = path;
            this.classification = classification;
            this.bucket = bucket;                    
        }
        
        private int getHitCount(StackTraceElement[] path) {
            if (classification == null) {
                return tree.getTotalCount(path);
            }
            else {
                return tree.getBucketCount(classification, bucket, path);
            }
        }
        
        @Override
        public StackTraceElement[] getPath() {
            return path;
        }

        @Override
        public StackTraceElement getFrame() {
            return path.length == 0 ? null : path[path.length - 1];
        }
        
        @Override
        public int getTreeHitCount() {
            return getHitCount(new StackTraceElement[0]);
        }

        @Override
        public int getHitCount() {
            return getHitCount(path);
        }

        @Override
        public int getParentHitCount() {
            return path.length == 0 ? 0 : getHitCount(parent(path));
        }

        @Override
        public int getBucketCount(String classification, String bucket) {
            return tree.getBucketCount(classification, bucket, path);
        }

        @Override
        public int getTreeBucketCount(String classification, String bucket) {
            return tree.getBucketCount(classification, bucket, new StackTraceElement[0]);
        }
        
        @Override
        public int getParentBucketCount(String classification, String bucket) {
            return path.length == 0 ? 0 : tree.getBucketCount(classification, bucket, parent(path));
        }

        @Override
        public TreeNode getChildAt(int childIndex) {
            return childList()[childIndex];
        }

        private StackTraceElement[] child(StackTraceElement[] par, StackTraceElement child) {
            StackTraceElement[] path = Arrays.copyOf(par, par.length + 1);
            path[par.length] = child;
            return path;
        }

        private StackTraceElement[] parent(StackTraceElement[] path) {
            StackTraceElement[] par = Arrays.copyOf(path, path.length - 1);
            return par;
        }

        @Override
        public int getChildCount() {
            return childList().length;
        }

        @Override
        public TreeNode getParent() {
            return path.length == 0 ? null : new FrameNode(tree, Arrays.copyOf(path, path.length - 1), classification, bucket);
        }

        @Override
        public int getIndex(TreeNode node) {
            return Arrays.asList(childList()).indexOf(node);
        }

        @Override
        public boolean getAllowsChildren() {
            return getChildCount() != 0;
        }

        @Override
        public boolean isLeaf() {
            return getChildCount() == 0;
        }

        public FrameNode[] childList() {
            StackTraceElement[] d = tree.getDescendants(path);
            FrameNode[] list = new FrameNode[d.length];
            for(int i = 0; i != list.length; ++i) {
                list[i] = new FrameNode(tree, child(path, d[i]), classification, bucket);
            }
            Arrays.sort(list, FREQ_COMPARATOR);
            int n = list.length;
            while(n > 0) {
                int hc = list[n - 1].getHitCount();
                if (hc > 0) {
                    break;
                }
                else {
                    --n;
                }
            }
            
            if (n != list.length) {
                list = Arrays.copyOf(list, n);
            }
            
            return list;
        }
        
        @Override
        @SuppressWarnings("rawtypes")
        public Enumeration children() {
            return new Vector<FrameNode>(Arrays.asList(childList())).elements();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(path);
            result = prime * result + ((tree == null) ? 0 : tree.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            FrameNode other = (FrameNode) obj;
            if (!Arrays.equals(path, other.path))
                return false;
            if (tree == null) {
                if (other.tree != null)
                    return false;
            } else if (!tree.equals(other.tree))
                return false;
            return true;
        }

        public String toString() {
            return path.length == 0 ? "" : path[path.length - 1].toString();
        }
    }  
    
    public interface FrameInfo {
        
        StackTraceElement[] getPath();
        
        StackTraceElement getFrame();
        
        int getTreeHitCount();

        int getHitCount();
        
        int getParentHitCount();
        
        int getBucketCount(String classification, String bucket);

        int getTreeBucketCount(String classification, String bucket);

        int getParentBucketCount(String classification, String bucket);        
    }
    
    private static class FrequencyComparator implements Comparator<FrameNode> {
        @Override
        public int compare(FrameNode o1, FrameNode o2) {
            return Long.signum(o2.getHitCount() - o1.getHitCount());
        }
    }
    
    public interface StackTraceFilter {
        
        public boolean evaluate(FrameInfo frameInfo);
        
    }    
}
