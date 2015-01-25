package org.gridkit.sjk.ssa.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.gridkit.jvmtool.StackTraceFilter;
import org.gridkit.sjk.ssa.ui.ClassificationTreeModel.Classification;
import org.gridkit.sjk.ssa.ui.ClassificationTreeModel.FollowPredicate;
import org.gridkit.sjk.ssa.ui.ClassificationTreeModel.FramePattern;
import org.gridkit.sjk.ssa.ui.ClassificationTreeModel.Subclass;

@SuppressWarnings("serial")
public class ClassificationEditor implements ClassificationModel {

    File loadedFile;
    
    Map<FilterRef, StackTraceFilter> filters = new LinkedHashMap<FilterRef, StackTraceFilter>();
    Map<FilterRef, StackTraceClassifier> classifications = new LinkedHashMap<FilterRef, StackTraceClassifier>();

    PropertyChangeSupport eventSupport = new PropertyChangeSupport(this);
    
    ClassificationTreeModel treeEditorModel = new ClassificationTreeModel();
    boolean isInSync;
    boolean isValid;
    boolean isSaved;
    
    EditorPanel editorPanel = new EditorPanel();
    
    public void loadFromFile(File file) throws IOException {
        FileReader reader = new FileReader(file);
        treeEditorModel.load(reader);
        loadedFile = file;
        treeEditorModel.validate();
        isValid = treeEditorModel.hasErrors();
        isSaved = true;
        pushTreeChanges();
    }

    public void loadFromReader(Reader reader) throws IOException {
        treeEditorModel.load(reader);
        loadedFile = null;
        treeEditorModel.validate();
        isValid = treeEditorModel.hasErrors();
        isSaved = false;
        pushTreeChanges();
    }
    
    @Override
    public List<FilterRef> getAvailableFilters() {
        return new ArrayList<FilterRef>(filters.keySet());
    }

    @Override
    public List<FilterRef> getAvailableClassifications() {
        return new ArrayList<FilterRef>(classifications.keySet());
    }
    
    @Override
    public StackTraceFilter getFilter(FilterRef ref) {
        return filters.get(ref);
    }

    @Override
    public StackTraceClassifier getClassifier(FilterRef ref) {
        return classifications.get(ref);
    }
    
    @Override
    public void addClassificationListener(PropertyChangeListener listener) {
        eventSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void removeClassificationListener(PropertyChangeListener listener) {
        eventSupport.removePropertyChangeListener(listener);
    }
    
    public JComponent getEditorComponent() {
        return editorPanel;
    }
    
    private void pushTreeChanges() {
        if (treeEditorModel.hasErrors()) {
            throw new IllegalArgumentException("Tree model has errors");
        }
        filters.clear();
        for(FilterRef fr: treeEditorModel.getFilters()) {
            if (!fr.isSubclass()) {
                classifications.put(fr, treeEditorModel.getClassifier(fr));
            }
            filters.put(fr, treeEditorModel.getFilter(fr));
        }
        eventSupport.firePropertyChange("classification", null, null);
    }

    public static class FilterRef implements Comparable<FilterRef> {
        
        private String classificationName;
        private String subclassName;
        
        public FilterRef() {
            this(null);
        }

        public FilterRef(String classificationName) {
            this.classificationName = classificationName;
        }
    
        public FilterRef(String classificationName, String subclassName) {
            this.classificationName = classificationName;
            this.subclassName = subclassName;
        }
    
        public FilterRef classification() {
            return new FilterRef(classificationName);
        }
        
        public String getClassificationName() {
            return classificationName;
        }
    
        public String getSubclassName() {
            return subclassName;
        }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((classificationName == null) ? 0 : classificationName.hashCode());
            result = prime * result + ((subclassName == null) ? 0 : subclassName.hashCode());
            return result;
        }
    
        public boolean isSubclass() {
            return subclassName != null;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            FilterRef other = (FilterRef) obj;
            if (classificationName == null) {
                if (other.classificationName != null)
                    return false;
            } else if (!classificationName.equals(other.classificationName))
                return false;
            if (subclassName == null) {
                if (other.subclassName != null)
                    return false;
            } else if (!subclassName.equals(other.subclassName))
                return false;
            return true;
        }
    
        @Override
        public int compareTo(FilterRef o) {
            int n = classificationName.compareTo(o.classificationName);
            if (n != 0) {
                return n;
            }
            String s1 = subclassName == null ? "" : subclassName;
            String s2 = o.subclassName == null ? "" : o.subclassName;
            return s1.compareTo(s2);
        }
        
        @Override
        public String toString() {
            return classificationName + "/" + subclassName;
        }
    }
    
    class EditorPanel extends JPanel {
        
        JTree tree = new JTree();
        
        public EditorPanel() {
            tree.setModel(treeEditorModel);
            tree.setRootVisible(false);
            tree.setShowsRootHandles(true);
            tree.setCellRenderer(new ClassificationNodeRenderer());
            
            setLayout(new BorderLayout());           
            add(new JScrollPane(tree), BorderLayout.CENTER);
            setBorder(BorderFactory.createTitledBorder("Classification"));
        }        
    }
    
    private class ClassificationNodeRenderer extends DefaultTreeCellRenderer {
        
        Font defaultFont;
        Font monospaceFont;
        
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

            if (defaultFont == null) {
                defaultFont = getFont();
                if (defaultFont != null) {
                    monospaceFont = Font.decode(Font.MONOSPACED).deriveFont(defaultFont.getSize2D());
                }
            }
            if (getFont() != defaultFont) {
                setFont(defaultFont);
            }
            
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            setIcon(null);
            
            if (value instanceof ClassificationTreeModel.Classification) {
                Classification node = (Classification) value;
                setText("<html><b>" + node.getName() + "</b></html>");
            }
            else if (value instanceof ClassificationTreeModel.Subclass) {
                Subclass node = (Subclass) value;
                setText(node.getName());                
            }
            else if (value instanceof ClassificationTreeModel.FramePattern) {
                FramePattern node = (FramePattern) value;                
                setFont(monospaceFont);
                setText(node.getPattern());
            }
            else if (value instanceof ClassificationTreeModel.RootFilter) {
                setText("<html><i>filter</i></html>");
            }
            else if (value instanceof ClassificationTreeModel.LastQuantor) {
                setText("<html><span style=\"color: #B44\"><b>LAST</b></span></html>");
            }
            else if (value instanceof ClassificationTreeModel.StackFragment) {
                setText("<html><i>element</i></html>");
            }
            else if (value instanceof ClassificationTreeModel.FollowPredicate) {
                FollowPredicate fp = (FollowPredicate) value;
                if (fp.isNegative()) {
                    setText("<html><i>not followed by</i></html>");
                }
                else {
                    setText("<html><i>followed by</i></html>");
                }
            }
            else if (value instanceof ClassificationTreeModel.DisjunctionNode) {
                setText("<html><span style=\"color: #B44\"><b>AND</b></span></html>");
            }
            else if (value instanceof ClassificationTreeModel.ConjunctionNode) {
                setText("<html><span style=\"color: #B44\"><b>OR</b></span></html>");
            }
            
            return this;
        }
    }
}
