package org.gridkit.sjk.ssa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gridkit.util.formating.TextTable;

/**
 * Stack frame histogram.
 *  
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class StackHisto {

    public static Comparator<SiteInfo> BY_HITS = new HitComparator();
    public static Comparator<SiteInfo> BY_OCCURENCE = new OccurenceComparator();
    
    private Map<StackTraceElement, SiteInfo> histo = new HashMap<StackTraceElement, SiteInfo>();
    private long traceCount = 0;
    private long frameCount = 0;
    
    public void feed(StackTraceElement[] trace) {
        ++traceCount;
        Set<StackTraceElement> seen = new HashSet<StackTraceElement>();
        for(StackTraceElement e: trace) {
            SiteInfo si = histo.get(e);
            if (si == null) {
                si = new SiteInfo();
                si.site = e;
                histo.put(e, si);
            }
            si.hitCount += 1;
            ++frameCount;
            if (seen.add(si.site)) {
                si.occurences += 1;
            }
        }
    }
    
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
