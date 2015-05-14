package org.gridkit.sjk.ssa.ui;

import java.awt.Dialog.ModalityType;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import org.gridkit.jvmtool.stacktrace.StackTraceCodec;
import org.gridkit.jvmtool.stacktrace.StackTraceReader;
import org.junit.Before;
import org.junit.Test;

public class AnalyzerD {

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
    public void start() throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, IOException, InterruptedException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//        UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel");
//        UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");

        AnalyzerDesktop desk = new AnalyzerDesktop();
        desk.setTraceDump(source);
//        pane.lboadClassification(new FileReader("src/test/resources/wagon.scf"));
        desk.loadClassification(new FileReader("src/test/resources/jboss-seam.scf"));

        JFrame frame = new JFrame();
        frame.setContentPane(desk);
        frame.setTitle("Stack Sample Analyzer");
        frame.setBounds(200, 200, 800, 600);
        frame.pack();        
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        
        while(true) {
            Thread.sleep(100);
        }
    }     
}
