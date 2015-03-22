package org.gridkit.sjk.ssa.ui;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.gridkit.jvmtool.stacktrace.StackFrame;

/**
 * Stack frame histogram.
 *  
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class StackFrameHisto {

    public static Comparator<SiteInfo> BY_HITS = new HitComparator();
    public static Comparator<SiteInfo> BY_OCCURENCE = new OccurenceComparator();
    
    private Map<StackFrame, SiteInfo> histo = new HashMap<StackFrame, SiteInfo>();
    private long traceCount = 0;
    private long frameCount = 0;
    
    public void feed(StackFrame[] trace) {
        feed(trace, 1);
    }
    
    public void feed(StackFrame[] trace, int count) {
        traceCount += count;
        Set<StackFrame> seen = new HashSet<StackFrame>();
        for(StackFrame e: trace) {
            SiteInfo si = histo.get(e);
            if (si == null) {
                si = new SiteInfo();
                si.site = e;
                histo.put(e, si);
            }
            si.hitCount += count;
            frameCount += count;
            if (seen.add(si.site)) {
                si.occurences += count;
            }
        }
    }
    
    public void feed(StackTree tree) {
        feed(tree, null, null);
    }
    
    public void feed(StackTree tree, String classification, String bucket) {
        for(StackFrame[] trace: tree.enumTerminalPaths(classification, bucket)) {            
            int count = tree.getBucketTerminalCount(classification, bucket, trace);
            feed(trace, count);
        }
    }
    
    public SiteInfo get(StackFrame frame) {
        return histo.get(frame);
    }
    
    public Collection<SiteInfo> getAllSites() {
        return histo.values();
    }
    
    public int getTraceCount() {
        return (int) traceCount;
    }

    public int getFrameCount() {
        return (int) frameCount;
    }
    
    public void clear() {
        traceCount = 0;
        frameCount = 0;
        histo.clear();
    }
    
    public static class SiteInfo {
        
        StackFrame site;
        int hitCount;
        int occurences;
        
        public StackFrame getSite() {
            return site;
        }

        public int getHitCount() {
            return hitCount;
        }

        public int getOccurences() {
            return occurences;
        }
    }
    
    private static class HitComparator implements Comparator<SiteInfo> {

        @Override
        public int compare(SiteInfo o1, SiteInfo o2) {
            return ((Integer)o2.hitCount).compareTo(o1.hitCount);
        }
    }

    private static class OccurenceComparator implements Comparator<SiteInfo> {

        @Override
        public int compare(SiteInfo o1, SiteInfo o2) {
            return ((Integer)o2.occurences).compareTo(o1.occurences);
        }
    }
}
