package org.gridkit.sjk.ssa.ui;

import static java.util.Arrays.asList;
import static javax.swing.BorderFactory.createCompoundBorder;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.BorderFactory.createEtchedBorder;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import org.gridkit.jvmtool.StackTraceReader;
import org.gridkit.sjk.ssa.ui.ClassificationEditor.FilterRef;
import org.gridkit.sjk.ssa.ui.ClassificationTreeModel.CommonNode;
import org.gridkit.sjk.ssa.ui.MultiSplitLayout.Divider;
import org.gridkit.sjk.ssa.ui.MultiSplitLayout.Leaf;
import org.gridkit.sjk.ssa.ui.MultiSplitLayout.Node;
import org.gridkit.sjk.ssa.ui.MultiSplitLayout.Split;
import org.gridkit.sjk.ssa.ui.StackTreeModel.FrameInfo;
import org.gridkit.sjk.ssa.ui.StackTreeModel.FrameNode;
import org.jdesktop.swingx.JXSearchField;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import org.jdesktop.swingx.autocomplete.ObjectToStringConverter;
import org.jdesktop.swingx.decorator.ToolTipHighlighter;
import org.jdesktop.swingx.painter.MattePainter;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.renderer.StringValue;
import org.jdesktop.swingx.sort.RowFilters;

@SuppressWarnings("serial")
public class AnalyzerPane extends JPanel {

    private static final NaturalComparator NATURAL_COMPARATOR = new NaturalComparator();
    private static final PercentFormater PERCENT_FORMATER = new PercentFormater();
    private static final StringValue TO_STRING = new StringValue() {
        @Override
        public String getString(Object value) {
            return value == null ? null : value.toString();
        }
    };
    

    private ClassificationEditor classificationEditor = new ClassificationEditor(); 

    private StackTraceSource source;
    private StackTree tree;
    private StackHisto histo;
    private ClassificationModel classificationModel = classificationEditor;
    
    private JComponent classificationPane = classificationEditor.getEditorComponent();
    private BarPane barPane = new BarPane();
    private ExplorerPane explorerPane = new ExplorerPane();
    
    public AnalyzerPane() {
        
        Node[] layout = {
            new Leaf("classification"),
            new Divider(), 
            new Leaf("bar"),
            new Divider(), 
            new Leaf("samples"),
        };
        layout[0].setWeight(0.4);
        layout[2].setWeight(0.2);
        layout[4].setWeight(0.4);
        Split split = new Split();
        split.setChildren(asList(layout));
        
        MultiSplitPane splitPane = new MultiSplitPane();
        splitPane.getMultiSplitLayout().setModel(split);
        splitPane.add(classificationPane, "classification");
        splitPane.add(barPane, "bar");
        splitPane.add(explorerPane, "samples");
        splitPane.setDividerSize(5);
        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);
    }
    
    public void setTraceDump(StackTraceSource source) {
        this.source = source;
        try {
            rebuildHisto();
            rebuildTree();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void loadClassification(Reader reader) throws IOException {
        classificationEditor.loadFromReader(reader);
    }

    private void rebuildTree() throws IOException {
        tree = new StackTree();
        StackTraceReader reader = source.getReader();
        if (!reader.isLoaded()) {
            reader.loadNext();
        }
        while(true) {
            tree.append(reader.getTrace());
            if (!reader.loadNext()) {
                break;
            }
        }        
        explorerPane.stackTree.treeModel.setTree(tree);
    }

    private void rebuildHisto() throws IOException {
        histo = new StackHisto();
        StackTraceReader reader = source.getReader();
        if (!reader.isLoaded()) {
            reader.loadNext();
        }
        while(true) {
            histo.feed(reader.getTrace());
            if (!reader.loadNext()) {
                break;
            }
        }
        explorerPane.frameHisto.histoModel.setHisto(histo);
        explorerPane.frameHisto.table.packAll();
    }
    
    public void setClassificationFile(File file) throws IOException {
        classificationEditor.loadFromFile(file);
    }

    private class BarPane extends JPanel {
        
        public BarPane() {
            Box box = Box.createVerticalBox();
            add(box);
            setBorder(BorderFactory.createTitledBorder("Distribution"));
        }        
    }

    private class ExplorerPane extends JPanel {
        
        StackTreePane stackTree = new StackTreePane();
        FrameHistoPane frameHisto = new FrameHistoPane();
        JPanel cards = new JPanel();
        JComboBox categoryCombo = new JComboBox();
        JToggleButton treeButton = new JToggleButton("Tree");
        JToggleButton histoButton = new JToggleButton("Histo");
        
        FilterRef appliedFilter = new FilterRef(null);
        List<String> appliedClassifications = new ArrayList<String>();
        
        public ExplorerPane() {
            setLayout(new BorderLayout());
            Box controls = Box.createHorizontalBox();
            JLabel categoryLabel = new JLabel("Scope:");
            categoryLabel.setLabelFor(categoryCombo);
            categoryCombo.setRenderer(Renderers.newFilterRefRenderer());
            controls.add(categoryLabel);
            controls.add(Box.createHorizontalStrut(5));
            controls.add(categoryCombo);
            controls.add(Box.createHorizontalStrut(5));
//            controls.add(Box.createHorizontalGlue());
            controls.add(treeButton);
            controls.add(histoButton);
            controls.setBorder(createCompoundBorder(createEtchedBorder(), createEmptyBorder(3, 3, 3, 3)));
            add(controls, BorderLayout.NORTH);
            add(cards, BorderLayout.CENTER);
            setBorder(BorderFactory.createTitledBorder("Samples"));
            
            cards.setLayout(new CardLayout());
            cards.add(stackTree, "tree");
            cards.add(frameHisto, "histo");
            showCard("tree");

            setupTreeHistoSwitch();      
            installClassificationListener();
            installFilterListener();
        }

        private void installFilterListener() {
            categoryCombo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    System.out.println("Filter: " + categoryCombo.getSelectedItem());
                    updateFilter();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (categoryCombo.getSelectedItem() == null) {
                                categoryCombo.setSelectedIndex(0);
                            }                            
                        }
                    });
                }
            });
//            categoryCombo.addFocusListener(new FocusListener() {
//                
//                @Override
//                public void focusLost(FocusEvent e) {
//                    updateFilter();                    
//                }
//                
//                @Override
//                public void focusGained(FocusEvent e) {
//                    // do nothing
//                }
//            });            
        }

        protected void updateFilter() {
            FilterRef filter = (FilterRef) categoryCombo.getSelectedItem();
            if (filter != null) {
                if (!filter.equals(appliedFilter)) {
                    applyRootFilter(filter);
                }            
            }
        }
        
        private void applyRootFilter(FilterRef filter) {
            if (filter.getClassificationName() != null) {
                if (!appliedClassifications.contains(filter.getClassificationName())) {
                    StackTraceClassifier classifier = classificationModel.getClassifier(filter.classification());
                    tree.addClassification(filter.getClassificationName(), classifier);
                    appliedClassifications.add(filter.getClassificationName());
                }
            }
            System.out.println("Update tree filter: " + filter);
            stackTree.treeModel.setBucketFilter(filter.getClassificationName(), filter.getSubclassName());
            appliedFilter = filter;
        }

        protected void cleanCategorization() {
            for(String cat: appliedClassifications) {
                tree.removeClassification(cat);
            }
            appliedClassifications.clear();
            applyRootFilter(new FilterRef(null));
        }
        
        protected void setupTreeHistoSwitch() {
            treeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (treeButton.isSelected()) {
                        showCard("tree");
                    }
                    else {
                        showCard("histo");                        
                    }
                }
            });
            histoButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (histoButton.isSelected()) {
                        showCard("histo");
                    }
                    else {
                        showCard("tree");                        
                    }
                }
            });
        }

        private void installClassificationListener() {
            classificationModel.addClassificationListener(new PropertyChangeListener() {
                
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    onClassificationUpdated();
                }
            });
        }

        protected void onClassificationUpdated() {
            cleanCategorization();
            List<FilterRef> model = new ArrayList<ClassificationEditor.FilterRef>(classificationModel.getAvailableFilters());
            model.add(0, new FilterRef(null));
            ModelUtils.updateComboBoxModel(categoryCombo.getModel(), model);
            AutoCompleteDecorator.decorate(categoryCombo, newFilterRefToString());
        }        
        
        private void showCard(String card) {
            CardLayout l = (CardLayout) cards.getLayout();
            l.show(cards, card);
            if (card.equals("tree")) {
                treeButton.getModel().setSelected(true);
                histoButton.getModel().setSelected(false);
            }
            else {
                treeButton.getModel().setSelected(false);
                histoButton.getModel().setSelected(true);
            }
        }        
    }    
    
    private class StackTreePane extends JPanel {
        
        private JTree tree = new JTree();
        private StackTreeModel treeModel = new StackTreeModel();
        private StackTreeToolbar toolbar = new StackTreeToolbar(this);
        
        public StackTreePane() {            
            tree.setRootVisible(false);
            tree.setShowsRootHandles(true);
            tree.setModel(treeModel);
            tree.setMinimumSize(new Dimension(100, 100));
            tree.setCellRenderer(new StackFrameTreeNodeRenderer());
            ToolTipManager.sharedInstance().registerComponent(tree);
            setLayout(new BorderLayout());
            add(new JScrollPane(tree), BorderLayout.CENTER);
            add(toolbar, BorderLayout.NORTH);
        }
        
        public void expand(StackTraceElement[] path) {
            TreePath treePath = treeModel.toTreePath(path);
            if (treePath != null) {
                doExpand(treePath);
            }
        }

        public void collapse(StackTraceElement[] path) {
            TreePath treePath = treeModel.toTreePath(path);
            if (treePath != null) {
                tree.collapsePath(treePath);
            }
        }
        
        public void collapseAll() {
            for(int n = tree.getRowCount(); n > 0;) {
                tree.collapseRow(--n);
            }            
        }
        
        private void doExpand(TreePath path) {
            if (path.getParentPath() != null) {
                doExpand(path.getParentPath());
            }
            tree.expandPath(path);
        }
    }    
    
    private class StackTreeToolbar extends JPanel {
        
        StackTreePane pane;
        
        JButton collapse = new JButton("[-]");
        JButton expand = new JButton("[+]");
        JButton expandFrame = new JButton("[=]");
        JButton configure = new JButton("[%]");
        JButton filters = new JButton("[f]");
        
        public StackTreeToolbar(StackTreePane pane) {
            this.pane = pane;
            
            collapse.addActionListener(newCollapseAllAction());
            expand.addActionListener(newExpandAllAction());
            expandFrame.addActionListener(newExpandSameFrameAction());
            
            Box box = Box.createHorizontalBox();
            box.add(collapse);
            box.add(expand);
            box.add(expandFrame);
            box.add(Box.createHorizontalStrut(4));
            box.add(Box.createHorizontalGlue());
            box.add(configure);
            box.add(filters);
            
            setLayout(new BorderLayout());
            add(box, BorderLayout.CENTER);
        }

        public ActionListener newCollapseAllAction() {
            return new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    collapseAll();
                }
            };
        }

        public ActionListener newExpandAllAction() {
            return new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    expandAll();
                }
            };
        }
        
        public ActionListener newExpandSameFrameAction() {
            return new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    expandSameFrame();
                }
            };
        }
        
        void expandAll() {
            StackTree stree = pane.treeModel.getStackTree();
            for(StackTraceElement[] path: stree.enumDeepPaths()) {
                pane.expand(path);
            }            
        }

        void collapseAll() {
            pane.collapseAll();
        }

        void expandSameFrame() {            
            TreePath selection = pane.tree.getSelectionPath();
            if (selection != null) {
                FrameNode node = (FrameNode) selection.getLastPathComponent();
                StackTraceElement[] stack = node.getPath();
                if (stack.length > 0) {
                    StackTraceElement e = stack[stack.length - 1];
                    StackTree tree = pane.treeModel.getStackTree();
                    for(StackTraceElement[] path: tree.enumDeepPaths()) {
                        for(int n = path.length - 1; n > 0; --n) {
                            if (path[n].equals(e)) {
                                StackTraceElement[] par = Arrays.copyOf(path, n);
                                pane.expand(par);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
    
    private class FrameHistoPane extends JPanel {
     
        private JXTable table = new JXTable();
        private MatchingTextHighlighter highlighter = new MatchingTextHighlighter(newHighlightPainter());
        private JXSearchField filterField = new JXSearchField();
        private JButton unfilterButton = new JButton("x");
        private String activeFilter = "";
        
        private FrameHistoModel histoModel = new FrameHistoModel();
        
        public FrameHistoPane() {
            table.setModel(histoModel);
            ToolTipManager.sharedInstance().registerComponent(table);
            setLayout(new BorderLayout());
            add(new JScrollPane(table), BorderLayout.CENTER);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
            
            table.getColumnExt(0).setCellRenderer(new DefaultTableRenderer(PERCENT_FORMATER, JLabel.RIGHT));
            table.getColumnExt(0).setComparator(NATURAL_COMPARATOR);
            table.getColumnExt(1).setComparator(NATURAL_COMPARATOR);
            table.getColumnExt(2).setComparator(NATURAL_COMPARATOR);
            table.getColumnExt(3).addHighlighter(new ToolTipHighlighter(TO_STRING));
            
            table.setEditable(false);
            
            table.addHighlighter(highlighter);
            
            add(filterField, BorderLayout.SOUTH);
            
            unfilterButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            filterField.setText("");
                            updateFilter();
                        }
                    });                    
                }
            });
            
            unfilterButton.setEnabled(false);
            
            filterField.getDocument().addDocumentListener(new DocumentListener() {
                
                @Override
                public void removeUpdate(DocumentEvent e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            updateFilter();
                        }
                    });
                }
                
                @Override
                public void insertUpdate(DocumentEvent e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            updateFilter();
                        }
                    });
                }
                
                @Override
                public void changedUpdate(DocumentEvent e) {
                    updateFilterLater();
                }

                private void updateFilterLater() {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            updateFilter();
                        }
                    });
                }
            });
        }

        private void updateFilter() {
            String text = filterField.getText().trim();
            if (!text.equals(activeFilter)) {
                if (text.length() > 0) {
                    table.setRowFilter(RowFilters.regexFilter(".*" + Pattern.quote(text) + ".*", 3));
                    unfilterButton.setEnabled(true);
                    highlighter.setMatchPatter(Pattern.compile(Pattern.quote(text)));
                }
                else {
                    table.setRowFilter(null);
                    unfilterButton.setEnabled(false);
                    highlighter.setMatchPatter(null);
                }
            }            
            activeFilter = text;
        }
    }
    
    private class StackFrameTreeNodeRenderer extends DefaultTreeCellRenderer {
        
        DecimalFormat pctFormat = new DecimalFormat("##.#%");
        DecimalFormat decFormat = new DecimalFormat("##.#");

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            setIcon(null);
            
            if (value instanceof StackTreeModel.FrameInfo) {
                FrameInfo node = (FrameInfo) value;
                StackTraceElement frame = node.getFrame();
                if (frame != null) {
                    int all = node.getTreeHitCount();
                    int count = node.getHitCount();
                    int par = node.getParentHitCount();
                    
                    String shortClass = shortName(frame.getClassName());
                    double ptc = 1d * count / par;
                    double ptcAll = 1d * count / all;
                    
                    String nodeText = "<html><b style=\"color:#2222AA\">" + pctFormat.format(ptc) + "</b><span style=\"color:#2222EE\"> (" + pctFormat.format(ptcAll) + ") </span> ";
                    nodeText += frame.toString().replace(frame.getClassName(), shortClass);
                    nodeText += "</html>";
                    setText(nodeText);
                    
                    String tooltipText = "<html>";
                    tooltipText += "<b>" + frame.toString() + "</b>";
                    tooltipText += "<br/>Frequency";
                    tooltipText += "<br/> path: <b>" + pctFormat.format(1d * count / all) + "</b> (" + count + " /" + all + ")";
                    try {                        
                        int occures = histo.get(frame).occurences;
                        int hits = histo.get(frame).hitCount;
                        int traceCount = histo.getTraceCount();
                        tooltipText += "<br/> frame: <b>" + pctFormat.format(1d * occures / traceCount) + "</b> (" + occures + " /" + traceCount + "), hits per trace <b>" + decFormat.format(1d * hits / occures) + "</b>";
                    }
                    catch(Exception e) {
                        // ignore
                    }
                    tooltipText += "</html>";
                    setToolTipText(tooltipText);
                }
            }
            
            return this;
        }
    }
    
    private static class PercentFormater extends DecimalFormat implements StringValue {

        public PercentFormater() {
            super("#0.0%");
        }
        
        @Override
        public String getString(Object value) {
            return value == null ? "" : format(value);
        }
    }
    
    static String shortName(String className) {
        int n = className.lastIndexOf('.');
        if (n >= 0) {
            String name = className.substring(n + 1, className.length());
            return name;
        }
        else {
            return className;
        }
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Painter<JLabel> newHighlightPainter() {
        return (Painter)new MattePainter(Color.YELLOW);
    }
    
    private static ObjectToStringConverter newFilterRefToString() {
        return new ObjectToStringConverter() {
            
            @Override
            public String getPreferredStringForItem(Object item) {
                if (item instanceof ClassificationEditor.FilterRef) {
                    ClassificationEditor.FilterRef r = (ClassificationEditor.FilterRef) item;
                    if (r.isSubclass()) {
                        return r.getSubclassName();
                    }
                    else {
                        String name = r.getClassificationName();
                        if (name == null) {
                            return "";
                        }
                        else {
                            return name;
                        }
                    }
                }
                return String.valueOf(item);
            }
        };
    }
    
    private static class NaturalComparator implements Comparator<Comparable<Object>> {

        @Override
        public int compare(Comparable<Object> o1, Comparable<Object> o2) {
            if (o1 == null) {
                return o2 == null ? 0 : -1;
            }
            else if (o2 == null) {
                return 1;
            }
            return o1.compareTo(o2);
        }
    }
}
