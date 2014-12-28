package org.gridkit.sjk.ssa;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

@SuppressWarnings("serial")
public class StackTreeModel extends DefaultTreeModel {

    static final Comparator<Node> FREQ_COMPARATOR = new FrequencyComparator();
    
    public StackTreeModel() {
        super(new Empty());
    }

    public void setTree(StackTree tree) {
        if (tree == null) {
            setRoot(new Empty());
        }
        else {
            setRoot(new Node(tree, new StackTraceElement[0]));
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
    
    public static class Node implements TreeNode, FrameInfo {

        StackTree tree;
        StackTraceElement[] path;
        
        public Node(StackTree tree, StackTraceElement[] path) {
            this.tree = tree;
            this.path = path;
                    
        }
        
        @Override
        public StackTraceElement getFrame() {
            return path.length == 0 ? null : path[path.length - 1];
        }
        
        @Override
        public int getTreeHitCount() {
            return tree.getTotalCount(new StackTraceElement[0]);
        }

        @Override
        public int getHitCount() {
            return tree.getTotalCount(path);
        }

        @Override
        public int getParentHitCount() {
            return path.length == 0 ? 0 : tree.getTotalCount(parent(path));
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
            return tree.getDescendants(path).length;
        }

        @Override
        public TreeNode getParent() {
            return path.length == 0 ? null : new Node(tree, Arrays.copyOf(path, path.length - 1));
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

        private Node[] childList() {
            StackTraceElement[] d = tree.getDescendants(path);
            Node[] list = new Node[d.length];
            for(int i = 0; i != list.length; ++i) {
                list[i] = new Node(tree, child(path, d[i]));
            }
            Arrays.sort(list, FREQ_COMPARATOR);
            return list;
        }
        
        @Override
        @SuppressWarnings("rawtypes")
        public Enumeration children() {
            return new Vector<Node>(Arrays.asList(childList())).elements();
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
            Node other = (Node) obj;
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
        
        StackTraceElement getFrame();
        
        int getTreeHitCount();
        
        int getHitCount();
        
        int getParentHitCount();
        
    }
    
    private static class FrequencyComparator implements Comparator<Node> {
        @Override
        public int compare(Node o1, Node o2) {
            return Long.signum(o2.getHitCount() - o1.getHitCount());
        }
    }
}
