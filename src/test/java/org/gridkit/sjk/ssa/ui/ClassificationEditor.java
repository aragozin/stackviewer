package org.gridkit.sjk.ssa.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
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

    PropertyChangeSupport classificationUpdateEvent = new PropertyChangeSupport(this);
    PropertyChangeSupport internalUpdateEvent = new PropertyChangeSupport(this);
    
    ClassificationTreeModel treeEditorModel = new ClassificationTreeModel();
    boolean isInSync;
    boolean isParsable;
    boolean isValid;
    boolean isSaved;
    
    boolean textIsShown;
    
    String editorContent = "";
    String lastParsed = null;
    List<String> editorParseErrors = new ArrayList<String>();
    
    EditorPanel editorPanel = new EditorPanel();
    
    public void loadFromFile(File file) throws IOException {
        String content = loadFile(file); 
        treeEditorModel.load(new StringReader(content));
        loadedFile = file;   
        editorContent = content;
        treeEditorModel.validate();
        isValid = treeEditorModel.hasErrors();
        isSaved = true;
        pushTreeChanges();
    }

    public void loadFromReader(Reader reader) throws IOException {
        String content = loadFile(reader); 
        treeEditorModel.load(new StringReader(content));
        loadedFile = null;
        editorContent = content;
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
        classificationUpdateEvent.addPropertyChangeListener(listener);
    }

    @Override
    public void removeClassificationListener(PropertyChangeListener listener) {
        classificationUpdateEvent.removePropertyChangeListener(listener);
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
        classificationUpdateEvent.firePropertyChange("classification", null, null);
    }

    String loadFile(File file) throws IOException {
        if (file.length() > 4 << 20) {
            throw new IOException("File to large");
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("UTF8")));
        StringBuilder sb = new StringBuilder();
        while(true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    String loadFile(Reader source) throws IOException {
        BufferedReader reader = new BufferedReader(source);
        StringBuilder sb = new StringBuilder();
        while(true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            sb.append(line).append('\n');
        }
        return sb.toString();
    }
    
    void openFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File("."));
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        int option = chooser.showOpenDialog(getEditorComponent());
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            String lastContent = editorContent;
            try {
                String text = loadFile(file);
                validateEditorText(text);
                if (isValid) {
                    loadedFile = file;
                    isSaved = true;
                    editorPanel.textEditor.textArea.setText(text);
                    pushTreeChanges();
                    fireInternalUpdate();
                }
                else {
                    editorContent = lastContent;
                    JOptionPane.showMessageDialog(getEditorComponent(), "Cannot load file\n" + editorParseErrors.toString(), "Cannot load to file!", JOptionPane.ERROR_MESSAGE);
                }
            }
            catch(Exception e) {
                editorContent = lastContent;
                JOptionPane.showMessageDialog(getEditorComponent(), "Cannot load file\n" + e.toString(), "Cannot load to file!", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    void saveFile() {
        if (loadedFile != null) {
            int r = JOptionPane.showConfirmDialog(getEditorComponent(), "Do you want to override file content\n\"" + loadedFile.getPath() + "\"", "Save classification", JOptionPane.YES_NO_OPTION);
            if (r == JOptionPane.OK_OPTION) {
                writeToFile(loadedFile);
            }
        }
        else {
            saveFileAs();
        }
    }

    void saveFileAs() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.setCurrentDirectory(new File("."));
        int option = chooser.showSaveDialog(getEditorComponent());
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            writeToFile(file);
        }
    }

    protected void writeToFile(File file) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(fos, Charset.forName("UTF8"));
            writer.append(editorContent);
            writer.close();
            loadedFile = file;
            isSaved = true;
            fireInternalUpdate();
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(getEditorComponent(), "Cannot save to file\n" + e.toString(), "Cannot save to file!", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    void switchToText() {
        editorPanel.textEditor.textArea.setText(editorContent);
        editorPanel.showText();
    }

    void switchToTree() {
        validateEditorText(editorPanel.textEditor.textArea.getText());
        if (isValid) {
            editorPanel.showTree();
        }
    }
    
    void syncToModel() {
        if (textIsShown) {
            validateEditorText(editorPanel.textEditor.textArea.getText());            
        }
        if (isValid) {
            pushTreeChanges();
        }
    }
    
    void validateEditorText(String text) {
        if (!text.equals(lastParsed)) {
            lastParsed = text;
            editorParseErrors.clear();
            try {
                treeEditorModel.load(new StringReader(text));
            }
            catch(Exception e) {
                Throwable x = e;
                while(!(x instanceof IllegalArgumentException)) {
                    x = x.getCause();
                    if (x == null) {
                        break;                        
                    }
                }
                String msg = x == null ? e.getMessage() : x.getMessage();
                editorParseErrors.add(msg);
                isParsable = false;
                isValid = false;
                fireInternalUpdate();
                return;
            }
            
            isParsable = true;
            
            treeEditorModel.validate();
            editorParseErrors.addAll(treeEditorModel.getAllErrors());
            
            isValid = editorParseErrors.isEmpty();
            editorContent = text;
            isSaved = false;
            fireInternalUpdate();
        }
    }
    
    void fireInternalUpdate() {
        internalUpdateEvent.firePropertyChange("event", null, null);
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
        
        EditorToolBar toolbar = new EditorToolBar();
        TreeEditorPanel treeEditor = new TreeEditorPanel();
        TextEditorPanel textEditor = new TextEditorPanel();
        CardLayout card = new CardLayout();
        
        public EditorPanel() {
            
            setLayout(new BorderLayout());
            JPanel editor = new JPanel();
            editor.setLayout(card);
            editor.add(treeEditor, "tree");
            editor.add(textEditor, "text");
            
            add(toolbar, BorderLayout.NORTH);
            add(editor, BorderLayout.CENTER);
        
            setBorder(BorderFactory.createTitledBorder("Classification"));
            
            showTree();
        }
        
        public void showTree() {
            card.show(treeEditor.getParent(), "tree");
            textIsShown = false;
            fireInternalUpdate();
        }

        public void showText() {
            card.show(textEditor.getParent(), "text");                        
            textIsShown = true;
            fireInternalUpdate();
        }
    }
    
    class EditorToolBar extends JPanel {
        
        Action open = new OpenAction();
        Action save = new SaveAction();
        Action tree = new TreeAction();
        Action text = new TextAction();
        Action sync = new SyncAction();
        boolean showTree;
        
        EditorToolBar() {
            Box box = Box.createHorizontalBox();
            box.add(new JButton(open));
            box.add(new JButton(save));
            box.add(Box.createHorizontalGlue());
            box.add(new JToggleButton(tree));
            box.add(new JToggleButton(text));
            box.add(Box.createHorizontalStrut(4));
            box.add(new JButton(sync));
            
            setLayout(new BorderLayout());
            add(box, BorderLayout.CENTER);
            
            internalUpdateEvent.addPropertyChangeListener(new PropertyChangeListener() {                
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    updateActions();                    
                }
            });
        }

        private void updateActions() {
            
            // update toggle buttons
            tree.putValue(Action.SELECTED_KEY, !textIsShown);
            text.putValue(Action.SELECTED_KEY, textIsShown);

            // TODO allow to switch back even if model is not valid, but parsable
            tree.setEnabled(isValid);

            // sync state
            sync.setEnabled(isValid && !isInSync);

            save.setEnabled(!isSaved);
        }
        
        class OpenAction extends AbstractAction {
            
            public OpenAction() {
                super("Open");
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                openFile();                
            }
        }

        class SaveAction extends AbstractAction {
            
            public SaveAction() {
                super("Save");
            }
            
            @Override
            public void actionPerformed(ActionEvent e) {
                saveFile();                
            }
        }

        class TreeAction extends AbstractAction {
            
            public TreeAction() {
                super("Tree");
            }
            
            @Override
            public void actionPerformed(ActionEvent e) {
                switchToTree();                
            }
        }
        
        class TextAction extends AbstractAction {
            
            public TextAction() {
                super("Text");
            }
            
            @Override
            public void actionPerformed(ActionEvent e) {
                switchToText();                
            }
        }        

        class SyncAction extends AbstractAction {
            
            public SyncAction() {
                super("Sync >>");
            }
            
            @Override
            public void actionPerformed(ActionEvent e) {
                syncToModel();                
            }
        }        
    }

    class TextEditorPanel extends JPanel {
        
        TextArea textArea = new TextArea();
        JLabel status = new JLabel();
        
        public TextEditorPanel() {
            
            setLayout(new BorderLayout());           
            add(textArea, BorderLayout.CENTER);
            Font font = Font.decode("Monospaced-14");
            textArea.setFont(font);
            
            setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            JPanel statusArea = new JPanel();
            statusArea.setLayout(new BorderLayout());
            statusArea.add(status, BorderLayout.CENTER);
            add(statusArea, BorderLayout.SOUTH);
            
            ToolTipManager.sharedInstance().registerComponent(status);
            
            internalUpdateEvent.addPropertyChangeListener(new PropertyChangeListener() {
                
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    updateStatus();
                }
            });
            
            textArea.addTextListener(new TextListener() {
                
                @Override
                public void textValueChanged(TextEvent e) {
                    validateEditorText(textArea.getText());                    
                }
            });
        }

        protected void updateStatus() {
            if (editorParseErrors.isEmpty()) {
                status.setText("Parsed");
                status.setForeground(Color.BLUE);
            }
            else {
                status.setText("Cannot parse: " + editorParseErrors.get(0));
                status.setForeground(Color.RED.darker());
                StringBuilder sb = new StringBuilder();
                for(String error: editorParseErrors) {
                    if (sb.length() > 0) {
                        sb.append('\n');
                    }
                    sb.append(error);
                }
                status.setToolTipText(sb.toString());                
            }
        }        
    }
    
    class TreeEditorPanel extends JPanel {
        
        JTree tree = new JTree();
        
        public TreeEditorPanel() {
            tree.setModel(treeEditorModel);
            tree.setRootVisible(false);
            tree.setShowsRootHandles(true);
            tree.setCellRenderer(new ClassificationNodeRenderer());
            
            setLayout(new BorderLayout());           
            add(new JScrollPane(tree), BorderLayout.CENTER);
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
