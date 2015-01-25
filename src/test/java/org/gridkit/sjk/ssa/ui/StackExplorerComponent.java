package org.gridkit.sjk.ssa.ui;

import static javax.swing.BorderFactory.createCompoundBorder;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.BorderFactory.createEtchedBorder;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import org.gridkit.sjk.ssa.ui.ClassificationEditor.FilterRef;
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
class StackExplorerComponent extends JPanel {

    private static final NaturalComparator NATURAL_COMPARATOR = new NaturalComparator();
    private static final PercentFormater PERCENT_FORMATER = new PercentFormater();
    private static final StringValue TO_STRING = new StringValue() {
        @Override
        public String getString(Object value) {
            return value == null ? null : value.toString();
        }
    };
    
    ClassificationModel classificationModel;
    StackExplorerModel explorerModel;
    
    StackTreePane stackTree;
    FrameHistoPane frameHisto;
    JPanel cards = new JPanel();
    JComboBox categoryCombo = new JComboBox();
    JToggleButton treeButton = new JToggleButton("Tree");
    JToggleButton histoButton = new JToggleButton("Histo");

    public StackExplorerComponent(ClassificationModel classificationModel) {
        this.classificationModel = classificationModel;
        this.explorerModel = new StackExplorerModel(classificationModel);
        
        setLayout(new BorderLayout());
        Box controls = Box.createHorizontalBox();
        JLabel categoryLabel = new JLabel("Scope:");
        categoryLabel.setLabelFor(categoryCombo);
        categoryCombo.setRenderer(Renderers.newFilterRefRenderer());
        controls.add(categoryLabel);
        controls.add(Box.createHorizontalStrut(5));
        controls.add(categoryCombo);
        controls.add(Box.createHorizontalStrut(5));
        // controls.add(Box.createHorizontalGlue());
        controls.add(treeButton);
        controls.add(histoButton);
        controls.setBorder(createCompoundBorder(createEtchedBorder(), createEmptyBorder(3, 3, 3, 3)));
        add(controls, BorderLayout.NORTH);
        add(cards, BorderLayout.CENTER);
        setBorder(BorderFactory.createTitledBorder("Samples"));

        stackTree = new StackTreePane();
        frameHisto = new FrameHistoPane();
        
        cards.setLayout(new CardLayout());
        cards.add(stackTree, "tree");
        cards.add(frameHisto, "histo");
        showCard("tree");

        setupTreeHistoSwitch();
        installClassificationListener();
        installFilterListener();
    }

    private void installFilterListener() {
        // temporary hack
        stackTree.toolbar.explorerPanel = this;

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
    }

    protected void updateFilter() {
        FilterRef filter = (FilterRef) categoryCombo.getSelectedItem();
        if (filter != null) {
            if (!filter.equals(explorerModel.getFilter())) {
                applyRootFilter(filter);
            }
        }
    }

    private void applyRootFilter(FilterRef filter) {
        System.out.println("Update tree filter: " + filter);
        explorerModel.setFilter(filter);
    }

    protected void setupTreeHistoSwitch() {
        treeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (treeButton.isSelected()) {
                    showCard("tree");
                } else {
                    showCard("histo");
                }
            }
        });
        histoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (histoButton.isSelected()) {
                    showCard("histo");
                } else {
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
        } else {
            treeButton.getModel().setSelected(false);
            histoButton.getModel().setSelected(true);
        }
    }
    
    class StackTreePane extends JPanel {
        
        private JTree tree = new JTree();
        StackTreeModel treeModel = new StackTreeModel();
        StackTreeToolbar toolbar = new StackTreeToolbar(this);
        
        private JPopupMenu contextMenu = new JPopupMenu();
        private StackTreeModel.FrameInfo rightClickNode;
        private List<ContextAction> contextActions = new ArrayList<ContextAction>();
        
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
            
            treeModel.setTree(explorerModel.getStackTree());
            // refresh tree model on source of filter changes
            explorerModel.addPropertyChangeListener(new PropertyChangeListener() {                
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    refreshTreeModel();
                }
            });
            
            installContextMenu();
        }


        private void refreshTreeModel() {
            FilterRef ref = explorerModel.getFilter();
            treeModel.updateRoot(ref.getClassificationName(), ref.getSubclassName());
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
        
        private void installContextMenu() {
            contextMenu.add(createCopyFrameAction());
            contextMenu.add(createCopyPathAction());
            tree.addMouseListener(new ContextMenuListener());
        }
        
        private Action createCopyFrameAction() {
            ContextAction action = new ContextAction("Copy frame") {

                @Override
                public void actionPerformed(ActionEvent e) {
                    StackTraceElement frame = rightClickNode.getFrame();
                    pushToClipboard(frame.toString());
                }

                @Override
                public void beforeShow() {
                    setEnabled(rightClickNode != null);
                }
            };
            contextActions.add(action);
            return action;
        }

        private Action createCopyPathAction() {
            ContextAction action = new ContextAction("Copy path") {

                @Override
                public void actionPerformed(ActionEvent e) {
                    StackTraceElement[] path = rightClickNode.getPath();
                    StringBuilder sb = new StringBuilder();
                    for(StackTraceElement f: path) {
                        if (sb.length() > 0) {
                            sb.append('\n');
                        }
                        sb.append(f.toString());
                    }
                    pushToClipboard(sb.toString());
                }

                @Override
                public void beforeShow() {
                    setEnabled(rightClickNode != null);
                }
            };
            contextActions.add(action);
            return action;
        }

        private final class ContextMenuListener extends MouseAdapter {
            @Override
            public void mousePressed(MouseEvent e) {
                onMouseEvent(e);
            }
        
            @Override
            public void mouseReleased(MouseEvent e) {
                onMouseEvent(e);
            }
        
            private void onMouseEvent(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = tree.getRowForLocation(e.getX(), e.getY());
                    if (row != -1) {
                        TreePath path = tree.getPathForRow(row);
                        rightClickNode = (FrameInfo) path.getLastPathComponent();
                    }
                    else {
                        rightClickNode = null;
                    }
                    for(ContextAction ca: contextActions) {
                        ca.beforeShow();
                    }
                    contextMenu.show(e.getComponent(), e.getX(), e.getY());
                }                    
            }
        }
    }    
    
    class StackTreeToolbar extends JPanel {
        
        StackExplorerComponent explorerPanel;
        StackTreePane treePane;
        
        JButton collapse = new JButton("[-]");
        JButton expand = new JButton("[+]");
        JButton expandFrame = new JButton("[=]");
        JButton configure = new JButton("[%]");
        JButton filters = new JButton("[f]");
        
        public StackTreeToolbar(StackTreePane pane) {
            this.treePane = pane;
            
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
            StackTree stree = treePane.treeModel.getStackTree();
            FilterRef activeFilter = explorerModel.getFilter();
            String classification = activeFilter.getClassificationName();
            String bucket = activeFilter.getSubclassName();
            for(StackTraceElement[] path: stree.enumDeepPaths(classification, bucket)) {
                treePane.expand(path);
            }            
        }

        void collapseAll() {
            treePane.collapseAll();
        }

        void expandSameFrame() {            
            TreePath selection = treePane.tree.getSelectionPath();
            if (selection != null) {
                FrameNode node = (FrameNode) selection.getLastPathComponent();
                StackTraceElement[] stack = node.getPath();
                if (stack.length > 0) {
                    StackTraceElement e = stack[stack.length - 1];
                    StackTree tree = treePane.treeModel.getStackTree();
                    FilterRef activeFilter = explorerModel.getFilter();
                    String classification = activeFilter.getClassificationName();
                    String bucket = activeFilter.getSubclassName();
                    for(StackTraceElement[] path: tree.enumDeepPaths(classification, bucket)) {
                        for(int n = path.length - 1; n > 0; --n) {
                            if (path[n].equals(e)) {
                                StackTraceElement[] par = Arrays.copyOf(path, n);
                                treePane.expand(par);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
    
    class FrameHistoPane extends JPanel {
     
        private JXTable table = new JXTable();
        private MatchingTextHighlighter highlighter = new MatchingTextHighlighter(newHighlightPainter());
        private JXSearchField filterField = new JXSearchField();
        private JButton unfilterButton = new JButton("x");
        private String activeFilter = "";
        
        public FrameHistoPane() {
            table.setModel(explorerModel.getFrameHistoModel());
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
                        StackFrameHisto histo = explorerModel.getFrameHisto();
                        int occures = histo.get(frame).occurences;
                        int hits = histo.get(frame).hitCount;
                        int traceCount = histo.getTraceCount();
                        tooltipText += "<br/> frame: <b>" + pctFormat.format(1d * occures / traceCount) + "</b> (" + occures + " /" + traceCount + "), hits per trace <b>" + decFormat.format(1d * hits / occures) + "</b>";
                    }
                    catch(Exception e) {
                        e.printStackTrace();
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
    
    @SuppressWarnings("unused")
    private abstract class ContextAction extends AbstractAction { 
        
        public ContextAction() {
            super();
        }

        public ContextAction(String name, Icon icon) {
            super(name, icon);
        }

        public ContextAction(String name) {
            super(name);
        }

        public abstract void beforeShow();         
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
    
    static ObjectToStringConverter newFilterRefToString() {
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
    
    static void pushToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
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