package org.gridkit.sjk.ssa.ui;

import java.util.Arrays;
import java.util.Collection;

import javax.swing.table.DefaultTableModel;

import org.gridkit.sjk.ssa.ui.StackHisto.SiteInfo;

@SuppressWarnings("serial")
public class FrameHistoModel extends DefaultTableModel {

    private SiteInfo[] histo;
    
    public FrameHistoModel() {
        addColumn("Frequency");
        addColumn("Trace count");
        addColumn("Hit count");
        addColumn("Frame");  
    }
    
    public void setHisto(StackHisto histo) {
        setRowCount(0);
        Collection<SiteInfo> ss = histo.getAllSites();
        SiteInfo[] sites = new SiteInfo[ss.size()];
        int n = 0;
        for(SiteInfo si: ss) {
            sites[n++] = si;
        }
        Arrays.sort(sites, StackHisto.BY_OCCURENCE);
        long total = histo.getTraceCount();
        this.histo = sites;
        for(SiteInfo si: sites) {            
            Object[] row = {1d * si.occurences / total, si.occurences, si.hitCount, si.site};
            addRow(row);
        }
    }
}
