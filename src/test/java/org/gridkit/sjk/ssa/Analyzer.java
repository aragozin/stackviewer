package org.gridkit.sjk.ssa;

import java.awt.Dialog.ModalityType;

import javax.swing.JDialog;

import org.junit.Test;

public class Analyzer {

    StackTraceSource source;
    
    @Test
    public void start() {
        AnalyzerPane pane = new AnalyzerPane();
        
        JDialog dialog = new JDialog();
        dialog.add(pane);
        dialog.setTitle("Stack Sample Analyzer");
        dialog.setModal(true);
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        dialog.pack();
        dialog.setVisible(true);        
    }    
}
