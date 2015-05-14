package org.gridkit.sjk.ssa.ui;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.Reader;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.vldocking.swing.docking.DockKey;
import com.vldocking.swing.docking.Dockable;
import com.vldocking.swing.docking.DockingConstants;
import com.vldocking.swing.docking.DockingDesktop;

@SuppressWarnings("serial")
public class AnalyzerDesktop extends DockingDesktop {

    private StackTraceSource source;

    private ClassificationEditor classificationEditor = new ClassificationEditor();
    private StackExplorerComponent stackExplorer = new StackExplorerComponent(classificationEditor);

    
    private ClassificationModel classificationModel = classificationEditor;
    private StackExplorerModel explorerModel = stackExplorer.explorerModel;
    
    private JComponent classificationPane = classificationEditor.getEditorComponent();
    private BarPane barPane = new BarPane();
    private JComponent explorerPane = stackExplorer;
    
    private StackSourcePane sourcePane = new StackSourcePane();
    {
        sourcePane.setAnalyzer(this);
    }

    private Dockable taxonomy = new Dockable() {
        
        DockKey key = new DockKey("taxonomy", "Classification");
        {
            key.setFloatEnabled(true);
            key.setAutoHideEnabled(true);
            key.setCloseEnabled(false);
        }
        
        @Override
        public DockKey getDockKey() {
            return key;
        }
        
        @Override
        public Component getComponent() {
            return classificationPane;
        }
    };

    private Dockable dumpSource = new Dockable() {
        
        DockKey key = new DockKey("source", "Dump files");
        {
            key.setFloatEnabled(true);
            key.setAutoHideEnabled(true);
            key.setCloseEnabled(false);
        }
        
        @Override
        public DockKey getDockKey() {
            return key;
        }
        
        @Override
        public Component getComponent() {
            return sourcePane;
        }
    };
    
    private Dockable explorer = new Dockable() {
        
        DockKey key = new DockKey("stack.exporer", "Samples");
        {
            key.setFloatEnabled(true);
            key.setAutoHideEnabled(true);
            key.setCloseEnabled(false);
        }
        
        @Override
        public DockKey getDockKey() {
            return key;
        }
        
        @Override
        public Component getComponent() {
            return explorerPane;
        }
    };

    public AnalyzerDesktop() {
        addDockable(explorer);
        split(explorer, dumpSource, DockingConstants.SPLIT_LEFT);
        createTab(dumpSource, taxonomy, 1);
        
    }
    
    public void setTraceDump(StackTraceSource source) {
        this.source = source;
        explorerModel.updateTraceSource(source);
    }
    
    public void loadClassification(Reader reader) throws IOException {
        classificationEditor.loadFromReader(reader);
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
}
