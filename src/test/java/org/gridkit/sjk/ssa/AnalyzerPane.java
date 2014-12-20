package org.gridkit.sjk.ssa;

import static java.util.Arrays.asList;

import java.awt.BorderLayout;
import java.io.File;

import javax.swing.Box;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.gridkit.sjk.ssa.MultiSplitLayout.Divider;
import org.gridkit.sjk.ssa.MultiSplitLayout.Leaf;
import org.gridkit.sjk.ssa.MultiSplitLayout.Node;
import org.gridkit.sjk.ssa.MultiSplitLayout.Split;

@SuppressWarnings("serial")
public class AnalyzerPane extends JPanel {

    
    private StackTraceSource source;
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
    
    public void setSource(StackTraceSource source) {
        this.source = source;
    }

    public void setClassificationFile(File file) {
        this.classificationFile = file;
    }
    
    private class ClassificationPane extends JPanel {
        
        JLabel label = new JLabel("Classification");
        JEditorPane editor = new JEditorPane();
        
        public ClassificationPane() {
            Box box = Box.createVerticalBox();
            box.add(label);
            box.add(editor);
            add(box);
        }        
    }

    private class BarPane extends JPanel {
        
        JLabel label = new JLabel("Distribution");
        
        public BarPane() {
            Box box = Box.createVerticalBox();
            box.add(label);
            add(box);
        }        
    }

    private class ExplorerPane extends JPanel {
        
        JLabel label = new JLabel("Samples");
        
        public ExplorerPane() {
            Box box = Box.createVerticalBox();
            box.add(label);
            add(box);
        }        
    }    
}
