package org.gridkit.sjk.ssa.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.gridkit.jvmtool.stacktrace.AbstractStackFrameArray;
import org.gridkit.jvmtool.stacktrace.CounterArray;
import org.gridkit.jvmtool.stacktrace.CounterCollection;
import org.gridkit.jvmtool.stacktrace.StackFrame;
import org.gridkit.jvmtool.stacktrace.StackFrameList;
import org.gridkit.jvmtool.stacktrace.StackTraceReader;
import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;
import org.gridkit.jvmtool.stacktrace.analytics.ThreadSnapshotFilter;
import org.gridkit.sjk.ssa.ui.ClassificationEditor.FilterRef;
import org.gridkit.sjk.ssa.ui.StackFrameHisto.SiteInfo;

public class StackExplorerModel {

    public static final String PROP_FILTER = "filter";
    public static final String PROP_STACK_SOURCE = "stack-source";
    
    private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    private StackTree tree;
    private StackFrameHisto histo;
    private ClassificationModel classification;
    private FilterRef activeFilter = new FilterRef(null);
    private StackTreeModel treeModel = new StackTreeModel();
    private FrameHistoModel histoModel = new FrameHistoModel();
    private List<String> cachedFilters = new ArrayList<String>();
    private List<FilterRef> cachedRelativeFilters = new ArrayList<FilterRef>();
    
    public StackExplorerModel(ClassificationModel model) {        
        tree = new StackTree();
        histo = new StackFrameHisto();
        
        classification = model;
        classification.addClassificationListener(new PropertyChangeListener() {
            
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                refreshClassification();                
            }
        });
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(propertyName, listener);
    }
    
    public void updateTraceSource(StackTraceSource source) {
        tree.clear();
        cleanCachedClassifications();
        StackTraceReader reader = source.getReader();
        try {
            if (!reader.isLoaded()) {
                reader.loadNext();           
            }
            while(reader.isLoaded()) {
                tree.append(reader.getStackTrace().toArray());
                reader.loadNext();
            }
        }
        catch(IOException e) {
            throw new RuntimeException();
        }
        finally {        
            refreshClassification();
            changeSupport.firePropertyChange(PROP_STACK_SOURCE, null, null);
        }
    }
    
    public StackTree getStackTree() {
        return tree;
    }

    public StackFrameHisto getFrameHisto() {
        return histo;
    }
    
    public StackTreeModel getStackTreeModel() {
        return treeModel;
    }

    public TableModel getFrameHistoModel() {
        return histoModel;
    }

    public FilterRef getFilter() {
        return activeFilter;
    }
    
    public void setFilter(FilterRef ref) {
        if (!ref.equals(activeFilter)) {
            ensureClassification(ref);
            treeModel.setBucketFilter(ref.getClassificationName(), ref.getSubclassName());
            histo.clear();
            histo.feed(tree, ref.getClassificationName(), ref.getSubclassName());
            histoModel.update(histo);
            activeFilter = ref;
            changeSupport.firePropertyChange(PROP_FILTER, null, null);            
        }
    }
    
    public FilterRef toRelative(FilterRef ref) {
        if (ref.getClassificationName() == null) {
            return activeFilter;
        }
        else if (activeFilter.getClassificationName() == null) {
            return ref;
        }
        else {
            int n = cachedRelativeFilters.indexOf(ref.classification());
            if (n < 0) {
                n = cachedRelativeFilters.size();
                cachedRelativeFilters.add(ref.classification());
                SimpleTraceClassifier inner = classification.getClassifier(ref.classification());
                SimpleTraceFilter filter = classification.getFilter(activeFilter);
                if (filter == null || inner == null) {
                    throw new IllegalArgumentException("One of filters is missing: " + activeFilter + " " + ref);
                }
                SimpleTraceClassifier rel = new FilteredClassifier(filter, inner);
                String name = "{REL:" + n + "}" + ref.getClassificationName();
                ensureClassification(name, rel);
            }
            String name = "{REL:" + n + "}" + ref.getClassificationName();
            return new FilterRef(name, ref.getSubclassName());
        }
    }
    
    private void ensureClassification(FilterRef ref) {
        if (ref.getClassificationName() != null) {
            String cn = ref.getClassificationName();
            if (!cachedFilters.contains(cn)) {
                SimpleTraceClassifier classifier = classification.getClassifier(ref.classification());
                cachedFilters.add(cn);
                tree.addClassification(cn, classifier);
            }
        }
    }

    private void ensureClassification(String classification, SimpleTraceClassifier classifier) {
        String cn = classification;
        if (!cachedFilters.contains(cn)) {
            cachedFilters.add(cn);
            tree.addClassification(cn, classifier);
        }
    }

    private void cleanCachedClassifications() {
        for(String c: cachedFilters) {
            tree.removeClassification(c);
        }
        cachedFilters.clear();
        cachedRelativeFilters.clear();
    }
    
    private void refreshClassification() {
        FilterRef f = activeFilter;
        cleanCachedClassifications();
        // set activeFilter to null (which is invalid value to ensure unconditional filter application)
        activeFilter = null;
        if (classification.getFilter(f) != null) {
            setFilter(f);
        }
        else {
            setFilter(new FilterRef());
        }        
    }

    @SuppressWarnings("serial")
    private class FrameHistoModel extends AbstractTableModel {

        FrameHistoColumn[] columns = {
                new FrameHistoColumn("Frequency", Double.class) {
                    @Override
                    public Object getCellValue(SiteInfo frame) {
                        return ((double)frame.occurences) / getTotalTraceCount();
                    }
                },
                new FrameHistoColumn("Trace count", Integer.class) {
                    @Override
                    public Object getCellValue(SiteInfo frame) {
                        return frame.occurences;
                    }
                },
                new FrameHistoColumn("Hit count", Integer.class) {
                    @Override
                    public Object getCellValue(SiteInfo frame) {
                        return frame.hitCount;
                    }
                },
                new FrameHistoColumn("Frame", String.class) {
                    @Override
                    public Object getCellValue(SiteInfo frame) {
                        return frame.site;
                    }
                },
        };

        private SiteInfo[] rows = new SiteInfo[0];
        private int traceCount;
        
        void update(StackFrameHisto histo) {
            SiteInfo[] nrows = histo.getAllSites().toArray(new SiteInfo[0]);
            Arrays.sort(nrows, StackFrameHisto.BY_OCCURENCE);
            rows = nrows;
            traceCount = histo.getTraceCount();
            fireTableDataChanged();
        }
        
        @Override
        public int getRowCount() {
            return rows.length;
        }

        protected int getTotalTraceCount() {
            return traceCount;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column].name;
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return columns[column].type;
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return columns[columnIndex].getCellValue(rows[rowIndex]);
        }
        
        abstract class FrameHistoColumn {
            
            String name;
            Class<?> type;
            
            public FrameHistoColumn(String name, Class<?> type) {
                this.name = name;
                this.type = type;
            }
            
            public abstract Object getCellValue(SiteInfo frame);
        }
    }
    
    private static class Classifier implements SimpleTraceFilter, SimpleTraceClassifier {

        ThreadSnapshotFilter rootFilter;
        Map<String, ThreadSnapshotFilter> subclasses = new LinkedHashMap<String, ThreadSnapshotFilter>();
        DummyThreadSnapshot dummy = new DummyThreadSnapshot();
        
        @Override
        public String classify(StackFrame[] list) {
            dummy.array = list;
            if (rootFilter != null && !rootFilter.evaluate(dummy)) {
                return null;
            }
            for(String key: subclasses.keySet()) {
                ThreadSnapshotFilter filter = subclasses.get(key);
                if (filter.evaluate(dummy)) {
                    return key;
                }
            }
            return null;
        }

        @Override
        public boolean evaluate(StackFrame[] list) {
            return classify(list) != null;
        }
    }
    
    private static class FilteredClassifier implements SimpleTraceClassifier {
        
        private final SimpleTraceFilter filter;
        private final SimpleTraceClassifier classifier;
        
        public FilteredClassifier(SimpleTraceFilter filter, SimpleTraceClassifier classifier) {
            this.filter = filter;
            this.classifier = classifier;
        }

        @Override
        public String classify(StackFrame[] trace) {
            if (filter.evaluate(trace)) {
                return classifier.classify(trace);
            }
            else {
                return null;
            }
        }

        @Override
        public boolean evaluate(StackFrame[] list) {
            return classify(list) != null;
        }
    }
    
    private static class DummyThreadSnapshot extends AbstractStackFrameArray implements ThreadSnapshot {

        private StackFrame[] array;
        
        @Override
        protected StackFrame[] array() {
            return array;
        }

        @Override
        protected int from() {
            return 0;
        }

        @Override
        protected int to() {
            return array.length;
        }

        @Override
        public long threadId() {
            return 0;
        }

        @Override
        public String threadName() {
            return null;
        }

        @Override
        public long timestamp() {
            return 0;
        }

        @Override
        public StackFrameList stackTrace() {
            return this;
        }

        @Override
        public State threadState() {
            return null;
        }

        @Override
        public CounterCollection counters() {
            return CounterArray.EMPTY;
        }
    }
}
