package org.gridkit.sjk.ssa;

import static java.util.Arrays.asList;
import static javax.swing.BorderFactory.createCompoundBorder;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.BorderFactory.createEtchedBorder;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.gridkit.jvmtool.StackTraceReader;
import org.gridkit.sjk.ssa.MultiSplitLayout.Divider;
import org.gridkit.sjk.ssa.MultiSplitLayout.Leaf;
import org.gridkit.sjk.ssa.MultiSplitLayout.Node;
import org.gridkit.sjk.ssa.MultiSplitLayout.Split;
import org.gridkit.sjk.ssa.StackTreeModel.FrameInfo;

@SuppressWarnings("serial")
public class AnalyzerPane extends JPanel {

    
    private StackTraceSource source;
    private StackTree tree;
    private StackHisto histo;
    private File classificationFile;
    
    private ClassificationPane classificationPane = new ClassificationPane();
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
    }

    public void setClassificationFile(File file) {
        this.classificationFile = file;
    }
    
    private class ClassificationPane extends JPanel {
        
        JEditorPane editor = new JEditorPane();
        
        public ClassificationPane() {
            setLayout(new BorderLayout());
            add(editor, BorderLayout.CENTER);
            setBorder(BorderFactory.createTitledBorder("Classification"));
        }        
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
        JLabel frameHisto = new JLabel("HISTO");
        JPanel cards = new JPanel();
        JComboBox categoryCombo = new JComboBox();
        JToggleButton treeButton = new JToggleButton("Tree");
        JToggleButton histoButton = new JToggleButton("Histo");
        
        public ExplorerPane() {
            setLayout(new BorderLayout());
            Box controls = Box.createHorizontalBox();
            JLabel categoryLabel = new JLabel("Scope:");
            categoryLabel.setLabelFor(categoryCombo);
            controls.add(categoryLabel);
            controls.add(Box.createHorizontalStrut(5));
            controls.add(categoryCombo);
            controls.add(Box.createHorizontalGlue());
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
        
        public StackTreePane() {
            tree.setRootVisible(false);
            tree.setModel(treeModel);
            tree.setMinimumSize(new Dimension(100, 100));
            tree.setCellRenderer(new StackFrameTreeNodeRenderer());
            ToolTipManager.sharedInstance().registerComponent(tree);
            setLayout(new BorderLayout());
            add(new JScrollPane(tree), BorderLayout.CENTER);
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
}
