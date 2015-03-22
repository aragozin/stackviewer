package org.gridkit.sjk.ssa.ui;

import java.awt.Dialog.ModalityType;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.Segment;

import org.fife.ui.RScrollPane;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.ParameterizedCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.junit.Test;

public class SyntaxPane {

    @Test
    public void show() throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        RSyntaxTextArea pane = new RSyntaxTextArea();
        pane.setTabsEmulated(true);
        
        AutoCompletion ac = new AutoCompletion(new Completer());
        ac.setAutoActivationEnabled(true);
        ac.setAutoCompleteEnabled(true);
        ac.install(pane);
        
        JDialog dialog = new JDialog();
        dialog.add(new RScrollPane(pane));
        dialog.setTitle("Editor test");
        dialog.setModal(true);
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        dialog.setBounds(200, 200, 800, 600);
        dialog.setVisible(true);
    }
    
    class Completer extends DefaultCompletionProvider {

        public Completer() {
            addCompletion(new BasicCompletion(this, "qwerty"));
            addCompletion(new BasicCompletion(this, "!LAST FRAME"));
            addCompletion(new BasicCompletion(this, "!FOLLOWED"));
            addCompletion(new BasicCompletion(this, "!NOT FOLLOWED"));
            addCompletion(new BasicCompletion(this, "!ALL"));
            addCompletion(new BasicCompletion(this, "!ANY"));
        }
    }
}
