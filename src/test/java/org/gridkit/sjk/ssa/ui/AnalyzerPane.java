package org.gridkit.sjk.ssa.ui;

import static java.util.Arrays.asList;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
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
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import org.gridkit.jvmtool.StackTraceReader;
import org.gridkit.sjk.ssa.ui.MultiSplitLayout.Divider;
import org.gridkit.sjk.ssa.ui.MultiSplitLayout.Leaf;
import org.gridkit.sjk.ssa.ui.MultiSplitLayout.Node;
import org.gridkit.sjk.ssa.ui.MultiSplitLayout.Split;
import org.gridkit.sjk.ssa.ui.StackTreeModel.FrameInfo;
import org.gridkit.sjk.ssa.ui.StackTreeModel.FrameNode;
import org.jdesktop.swingx.JXSearchField;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.autocomplete.ObjectToStringConverter;
import org.jdesktop.swingx.decorator.ToolTipHighlighter;
import org.jdesktop.swingx.painter.MattePainter;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.renderer.StringValue;
import org.jdesktop.swingx.sort.RowFilters;

@SuppressWarnings("serial")
public class AnalyzerPane extends JPanel {

    private StackTraceSource source;

    private ClassificationEditor classificationEditor = new ClassificationEditor();
    private StackExplorerComponent stackExplorer = new StackExplorerComponent(classificationEditor);

    
    private ClassificationModel classificationModel = classificationEditor;
    private StackExplorerModel explorerModel = stackExplorer.explorerModel;
    
    private JComponent classificationPane = classificationEditor.getEditorComponent();
    private BarPane barPane = new BarPane();
    private JComponent explorerPane = stackExplorer;
    
    public AnalyzerPane() {
        
        Leaf nclassification = new Leaf("classification");
        Leaf nbar = new Leaf("bar");
        Leaf nsamples = new Leaf("samples");
        Node[] layout = {
            nclassification,
//            new Divider(), 
//            nbar,
            new Divider(), 
            nsamples,
        };
        nclassification.setWeight(0.4);
        nbar.setWeight(0.2);
        nsamples.setWeight(0.4);

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
        explorerModel.updateTraceSource(source);
    }
    
    public void loadClassification(Reader reader) throws IOException {
        classificationEditor.loadFromReader(reader);
    }

//    private void rebuildTree() throws IOException {
//        tree = new StackTree();
//        StackTraceReader reader = source.getReader();
//        if (!reader.isLoaded()) {
//            reader.loadNext();
//        }
//        while(true) {
//            tree.append(reader.getTrace());
//            if (!reader.loadNext()) {
//                break;
//            }
//        }        
//        explorerPane.stackTree.treeModel.setTree(tree);
//    }
//
//    private void rebuildHisto() throws IOException {
//        histo = new StackFrameHisto();
//        StackTraceReader reader = source.getReader();
//        if (!reader.isLoaded()) {
//            reader.loadNext();
//        }
//        while(true) {
//            histo.feed(reader.getTrace());
//            if (!reader.loadNext()) {
//                break;
//            }
//        }
//        explorerPane.frameHisto.histoModel.setHisto(histo);
//        explorerPane.frameHisto.table.packAll();
//    }
    
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
}
