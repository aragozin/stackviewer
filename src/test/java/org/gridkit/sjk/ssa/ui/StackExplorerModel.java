package org.gridkit.sjk.ssa.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

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
    
    public StackExplorerModel(ClassificationModel model) {        
        tree = new StackTree();
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
    
    private void ensureClassification(FilterRef ref) {
        if (ref.getClassificationName() != null) {
            if (!cachedFilters.contains(ref.classification())) {
                StackTraceClassifier classifier = classification.getClassifier(ref.classification());
                cachedFilters.add(ref.getClassificationName());
                tree.addClassification(ref.getClassificationName(), classifier);
            }
        }
    }

    private void cleanCachedClassifications() {
        for(String c: cachedFilters) {
            tree.removeClassification(c);
        }
        cachedFilters.clear();
    }
    
    private void refreshClassification() {
        FilterRef f = activeFilter;
        cleanCachedClassifications();
        if (classification.getFilter(f) != null) {
            activeFilter = null;
            setFilter(f);
        }
        else {
            setFilter(new FilterRef());
        }        
    }

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
}
