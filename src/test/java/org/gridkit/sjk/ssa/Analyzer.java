package org.gridkit.sjk.ssa;

import java.awt.Dialog.ModalityType;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.JDialog;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.gridkit.jvmtool.StackTraceCodec;
import org.gridkit.jvmtool.StackTraceReader;
import org.gridkit.jvmtool.StackTraceCodec.StackTraceWriter;
import org.jdesktop.swingx.plaf.LookAndFeelUtils;
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
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
    
    @Test
    public void start() throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
        AnalyzerPane pane = new AnalyzerPane();
        pane.setTraceDump(source);

        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//        UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
        
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
