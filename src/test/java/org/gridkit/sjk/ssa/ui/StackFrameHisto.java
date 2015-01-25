package org.gridkit.sjk.ssa.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gridkit.util.formating.TextTable;

/**
 * Stack frame histogram.
 *  
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class StackFrameHisto {

    public static Comparator<SiteInfo> BY_HITS = new HitComparator();
    public static Comparator<SiteInfo> BY_OCCURENCE = new OccurenceComparator();
    
    private Map<StackTraceElement, SiteInfo> histo = new HashMap<StackTraceElement, SiteInfo>();
    private long traceCount = 0;
    private long frameCount = 0;
    
    public void feed(StackTraceElement[] trace) {
        feed(trace, 1);
    }
    
    public void feed(StackTraceElement[] trace, int count) {
        traceCount += count;
        Set<StackTraceElement> seen = new HashSet<StackTraceElement>();
        for(StackTraceElement e: trace) {
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
//        int tc = 0;
//        int dp = 0;
//        Set<String> paths = new HashSet<String>();
//        for(StackTraceElement[] trace: tree.enumTerminalPaths(classification, bucket)) {
//            ++tc;  
//            if (tc == 33) {
//                new String("");
//            }
//            paths.add(fmt(trace));
//        }
//        int nn = 0;
//        for(StackTraceElement[] trace: tree.enumDeepPaths(classification, bucket)) {
//            ++dp;
//            String path = fmt(trace);
//            if (nn < 20 && !paths.contains(path)) {
//                System.out.println("Miss DP: " + path);
//                ++nn;
//            }
//        }
//        System.out.println("HISTO " + classification + " / " + bucket + " TP: " + tc + " DP: " + dp);
//        int n = 0;
//        Iterator<StackTraceElement[]> ti = tree.enumTerminalPaths(classification, bucket).iterator(); 
//        Iterator<StackTraceElement[]> di = tree.enumDeepPaths(classification, bucket).iterator(); 
//        while(n < 100 && ti.hasNext() && di.hasNext()) {
//            String tt = fmt(ti.next());
//            String dt = fmt(di.next());
//            if (!tt.equals(dt)) {
//                System.out.println("TP[" + n + "]: " + tt);
//                System.out.println("DP[" + n + "]: " + dt);
//            }
//            ++n;
//        }
        
        for(StackTraceElement[] trace: tree.enumTerminalPaths(classification, bucket)) {            
            int count = tree.getBucketTerminalCount(classification, bucket, trace);
            feed(trace, count);
        }
    }
    
//    private String fmt(StackTraceElement[] next) {
//        StringBuilder sb = new StringBuilder();
//        for(StackTraceElement e: next) {
//            if (sb.length() > 0) {
//                sb.append(" > ");                
//            }
//            String className = e.getClassName();
//            int c = className.lastIndexOf('.');
//            if (c > 0) {
//                className = className.substring(c + 1, className.length());
//            }
//            sb.append(className).append('.').append(e.getMethodName()).append("(").append(e.getLineNumber()).append(")");
//        }
//        return sb.toString();
//    }

    public SiteInfo get(StackTraceElement frame) {
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
    
    public String formatHisto() {
        TextTable tt = new TextTable();
        List<SiteInfo> h = new ArrayList<SiteInfo>(histo.values());
        Collections.sort(h, BY_OCCURENCE);
        for(SiteInfo si: h) {
            long pc = (100 * si.getOccurences()) / traceCount;
            tt.addRow("" + si.getOccurences(), " " + pc + "%", " " + si.getHitCount(), " " + si.getSite());
        }
        return tt.formatTextTableUnbordered(200);
    }

    public String formatHisto(int limit) {
        TextTable tt = new TextTable();
        List<SiteInfo> h = new ArrayList<SiteInfo>(histo.values());
        Collections.sort(h, BY_OCCURENCE);
        int n = 0;
        for(SiteInfo si: h) {            
            long pc = (100 * si.getOccurences()) / traceCount;
            tt.addRow("" + si.getOccurences(), " " + pc + "%", " " + si.getHitCount(), " " + si.getSite());
            if (limit <= ++n) {
                break;
            }
        }
        return tt.formatTextTableUnbordered(200);
    }
    
    public void clear() {
        traceCount = 0;
        frameCount = 0;
        histo.clear();
    }
    
    public static class SiteInfo {
        
        StackTraceElement site;
        int hitCount;
        int occurences;
        
        public StackTraceElement getSite() {
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
