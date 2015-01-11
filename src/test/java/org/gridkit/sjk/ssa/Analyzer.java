package org.gridkit.sjk.ssa;

import java.awt.Dialog.ModalityType;
import java.io.FileInputStream;
import java.io.IOException;

import javax.swing.JDialog;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.gridkit.jvmtool.StackTraceCodec;
import org.gridkit.jvmtool.StackTraceReader;
import org.junit.Before;
import org.junit.Test;

public class Analyzer {

    StackTraceSource source;
    
    @Before
    public void load() {
        source = new StackTraceSource() {
            @Override
            public StackTraceReader getReader() {
                try {
                    return StackTraceCodec.newReader(new FileInputStream("C:/WarZone/spaces/blog/blog-docs/_tmp/case1.stp"));
//                    return StackTraceCodec.newReader(new FileInputStream("C:/WarZone/docs/_tmp/case1.stp"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
    
    @Test
    public void start() throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        AnalyzerPane pane = new AnalyzerPane();
        pane.setTraceDump(source);

        JDialog dialog = new JDialog();
        dialog.add(pane);
        dialog.setTitle("Stack Sample Analyzer");
        dialog.setModal(true);
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        dialog.setBounds(200, 200, 400, 300);
//        dialog.pack();        
        dialog.setVisible(true);        
    }     
}
