package org.gridkit.sjk.ssa.ui;

import java.awt.Dialog.ModalityType;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.junit.Test;
import org.noos.xing.mydoggy.ToolWindow;
import org.noos.xing.mydoggy.ToolWindowAnchor;
import org.noos.xing.mydoggy.ToolWindowManager;
import org.noos.xing.mydoggy.plaf.MyDoggyToolWindowManager;

public class Analyzer2 {

    @Test
    public void start() throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, IOException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        ToolWindowManager toolWindowManager;
        MyDoggyToolWindowManager myDoggyToolWindowManager = new MyDoggyToolWindowManager();
        toolWindowManager = myDoggyToolWindowManager;

        // Register a Tool.
        toolWindowManager.registerToolWindow("Debug",       // Id
                                             "Debug Tool",                 // Title
                                             null,                         // Icon
                                             new JButton("Debug Tool"),    // Component
                                             ToolWindowAnchor.LEFT);       // Anchor

        toolWindowManager.registerToolWindow("Debug2",       // Id
                "Debug2 Tool",                 // Title
                null,                         // Icon
                new JButton("Debug2 Tool"),    // Component
                ToolWindowAnchor.LEFT);       // Anchor

        toolWindowManager.registerToolWindow("Debug3",       // Id
                "Debug3 Tool",                 // Title
                null,                         // Icon
                new JButton("Debug3 Tool"),    // Component
                ToolWindowAnchor.LEFT);       // Anchor
        
        // Made all tools available
        for (ToolWindow window : toolWindowManager.getToolWindows())
            window.setAvailable(true);

        JDialog dialog = new JDialog();
        dialog.setTitle("Stack Sample Analyzer");
        dialog.setModal(true);
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        dialog.setBounds(200, 200, 800, 600);
//        dialog.pack();        
        
        // Add myDoggyToolWindowManager to the frame. MyDoggyToolWindowManager is an extension of a JPanel
        dialog.getContentPane().add(myDoggyToolWindowManager);

        dialog.setVisible(true);
    }
    
}
