package org.gridkit.sjk.ssa.ui;

import java.awt.Dialog.ModalityType;
import java.io.FileInputStream;
import java.io.FileReader;
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
//                    return StackTraceCodec.newReader(new FileInputStream("case1.stp"));
//                    return StackTraceCodec.newReader(new FileInputStream("C:/WarZone/docs/_tmp/threads-100u-1-1408727085233.stp"));
                    return StackTraceCodec.newReader(new FileInputStream("C:/fire_at_will/_samara/tss6/threads-100u-1-1408727085233.stp"));
//                    return StackTraceCodec.newReader(new FileInputStream("C:/fire_at_will/vt_bench/dump_index_all_or_12k_sort.std"));
//                    return StackTraceCodec.newReader(new FileInputStream("dump_index_all_or_12k_sort_left.std"));
//                    return StackTraceCodec.newReader(new FileInputStream("dump_index_all_or_12k_sort_new.std"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
    
    @Test
    public void start() throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, IOException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//        UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel");
//        UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");

        AnalyzerPane pane = new AnalyzerPane();
        pane.setTraceDump(source);
//        pane.lboadClassification(new FileReader("src/test/resources/wagon.scf"));
        pane.loadClassification(new FileReader("src/test/resources/jboss-seam.scf"));

        JDialog dialog = new JDialog();
        dialog.add(pane);
        dialog.setTitle("Stack Sample Analyzer");
        dialog.setModal(true);
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        dialog.setBounds(200, 200, 800, 600);
//        dialog.pack();        
        dialog.setVisible(true);        
    }     
}
