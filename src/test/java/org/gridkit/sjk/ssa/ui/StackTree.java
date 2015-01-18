package org.gridkit.sjk.ssa.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SortedMap;
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

    public Iterable<StackTraceElement[]> enumDeepPaths() {
        return new Iterable<StackTraceElement[]>() {
            @Override
            public Iterator<StackTraceElement[]> iterator() {
                return new DeepPathIterator();
            }
        };
    }
    
    public void addClassification() {
        
    }
    
    public int getTotalCount(StackTraceElement[] path) {
        Node node = lookup(path);
        return node == null ? 0 : node.totalCount;        
    }

    public int getTerminalCount(StackTraceElement[] path) {
        Node node = lookup(path);
        return node == null ? 0 : node.terminalCount;        
    }
    
    public int getBucketCount(String classification, String bucket, StackTraceElement[] path) {
        Node node = lookup(path);
        return node == null ? 0 : node.getHisto(classification).get(bucket);                
    }

    public int[] getBucketCount(String classification, String[] buckets, StackTraceElement[] path) {
        Node node = lookup(path);
        int[] result = new int[buckets.length];
        if (node != null) {
            Subhistogram subhisto = node.getHisto(classification);
            for(int i = 0; i != buckets.length; ++i) {
                result[i] = subhisto.get(buckets[i]);
            }
        }
        return result;                
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
        SortedMap<StackTraceElement, Node> children = new TreeMap<StackTraceElement, Node>(FRAME_COMPARATOR);
        
        Subhistogram[] subhistos = new Subhistogram[0];
        
        public void addHisto(String classification) {
            Subhistogram sh = new Subhistogram();
            sh.classificationId = classification;
            for(int i = 0; i != subhistos.length; ++i) {
                if (subhistos[i] == null) {
                    subhistos[i] = sh;
                    return;
                }
            }
            subhistos = Arrays.copyOf(subhistos, subhistos.length + 1);
            subhistos[subhistos.length - 1] = sh;
        }

        public void removeHisto(String classification) {
            for(int i = 0; i != subhistos.length; ++i) {
                if (subhistos[i] != null && subhistos[i].classificationId.equals(classification)) {
                    subhistos[i] = null;
                    return;
                }
            }
        }
        
        public Subhistogram getHisto(String classification) {
            for(Subhistogram sh: subhistos) {
                if (sh.classificationId.equals(classification)) {
                    return sh;
                }
            }
            return EMPTY;
        }
        
    }
    
    private static final Subhistogram EMPTY = new Subhistogram();
    
    private static class Subhistogram {

        private String classificationId;
        private String[] buckets = new String[0];
        private int[] traceCounts = new int[0];
        
        public int get(String bucket) {
            for(int i = 0; i != buckets.length; ++i) {
                if (bucket.equals(buckets[i])) {
                    return traceCounts[i];
                }
            }
            return 0;
        }

        public void adjust(String bucket, int delta) {
            for(int i = 0; i != buckets.length; ++i) {
                if (bucket.equals(buckets[i])) {
                    traceCounts[i] += delta;
                    return;
                }
            }
            buckets = Arrays.copyOf(buckets, buckets.length + 1);
            traceCounts = Arrays.copyOf(traceCounts, traceCounts.length + 1);
            buckets[buckets.length - 1] = bucket;
            traceCounts[traceCounts.length - 1] = delta;
        }        
    }
    
    private class DeepPathIterator implements Iterator<StackTraceElement[]> {
        
        List<Node> pointer = new ArrayList<Node>();

        public DeepPathIterator() {
            walkDown(root);
        }

        private void walkDown(Node node) {
            while(true) {
                pointer.add(node);
                if (node.children.isEmpty()) {
                    break;
                }
                else {
                    node = node.children.entrySet().iterator().next().getValue();
                }
            }            
        }

        private void seek() {
            while(pointer.size() > 1) {
                Node last = pointer.remove(pointer.size() - 1);
                Node preLast = pointer.get(pointer.size() - 1);
                SortedMap<StackTraceElement, Node> rest = preLast.children.tailMap(last.path[last.path.length - 1]);
                if (rest.size() > 1) {
                    Iterator<Node> it = rest.values().iterator();
                    it.next(); // skip last
                    walkDown(it.next());
                    return;
                }                
            }
        }
        
        @Override
        public boolean hasNext() {
            return pointer.size() > 1;
        }

        @Override
        public StackTraceElement[] next() {
            if (pointer.size() == 1) {
                throw new NoSuchElementException();
            }
            else {
                StackTraceElement[] path = pointer.get(pointer.size() - 1).path;
                seek();
                return path;
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
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