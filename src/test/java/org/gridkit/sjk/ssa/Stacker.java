package org.gridkit.sjk.ssa;

import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

public class Stacker {

    public static JComponent scroll(JComponent comp, int width, int height) {
        JScrollPane pane = new JScrollPane(comp);
        pane.setPreferredSize(new Dimension(width, height));
        return pane;        
    }
}
