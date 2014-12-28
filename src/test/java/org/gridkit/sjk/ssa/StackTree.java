package org.gridkit.sjk.ssa;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public class StackTree {

    private static final StackTraceElement[] ROOT = new StackTraceElement[0];
    private static final FrameComparator FRAME_COMPARATOR = new FrameComparator();
    
    private Node root;
    
    public StackTree() {
        this.root = new Node();
        this.root.path = new StackTraceElement[0];
    }

    /**
     * @param trace - root frame is last
     */
    public void append(StackTraceElement[] trace) {
        root.totalCount++;
        append(root, trace, trace.length - 1);
    }
    
    public StackTraceElement[] getDescendants(StackTraceElement[] path) {
        Node node = lookup(path);
        return node == null ? ROOT : node.children.keySet().toArray(ROOT);
    }

    public int getTotalCount(StackTraceElement[] path) {
        Node node = lookup(path);
        return node == null ? 0 : node.totalCount;        
    }

    public int getTerminalCount(StackTraceElement[] path) {
        Node node = lookup(path);
        return node == null ? 0 : node.terminalCount;        
    }
    
    private Node lookup(StackTraceElement[] path) {
        Node n = root;
        for(int i = 0; i != path.length; ++i) {
            n = n.children.get(path[i]);
            if (n == null) {
                return null;
            }
        }
        return n;
    }

    private void append(Node node, StackTraceElement[] trace, int at) {
        StackTraceElement frame = trace[at];
        Node next = node.children.get(frame);
        if (next == null) {
            next = new Node();
            next.path = Arrays.copyOf(node.path, node.path.length + 1);
            next.path[next.path.length - 1] = frame;
            node.children.put(frame, next);
        }
        next.totalCount++;
        if (at == 0) {
            next.terminalCount++;
        }
        else {
            append(next, trace, --at);
        }
    }

    private static class Node {
        
        StackTraceElement[] path;
        int totalCount;
        int terminalCount;
        Map<StackTraceElement, Node> children = new TreeMap<StackTraceElement, Node>(FRAME_COMPARATOR);
        
    }
    
    private static class FrameComparator implements Comparator<StackTraceElement> {

        @Override
        public int compare(StackTraceElement o1, StackTraceElement o2) {
            int n = compare(o1.getClassName(), o2.getClassName());
            if (n != 0) {
                return n;
            }
            n = compare(o1.getLineNumber(), o2.getLineNumber());
            if (n != 0) {
                return n;
            }
            n = compare(o1.getMethodName(), o2.getMethodName());
            if (n != 0) {
                return n;
            }
            n = compare(o1.getFileName(), o2.getFileName());
            return 0;
        }

        private int compare(int n1, int n2) {            
            return Long.signum(((long)n1) - ((long)n2));
        }

        private int compare(String str1, String str2) {
            if (str1 == str2) {
                return 0;
            }
            else if (str1 == null) {
                return -1;
            }
            else if (str2 == null) {
                return 1;
            }
            return str1.compareTo(str2);
        }
    }
}
