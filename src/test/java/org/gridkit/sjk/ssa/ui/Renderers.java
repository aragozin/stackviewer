package org.gridkit.sjk.ssa.ui;

import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JList;

import org.gridkit.sjk.ssa.ui.ClassifierModel.FilterRef;

public class Renderers {

    public static FilterRefListRenderer newFilterRefRenderer() {
        return new FilterRefListRenderer();
    }
    
    public static class FilterRefListRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof FilterRef) {
                FilterRef ref = (FilterRef) value;
                
                setIcon(null);
                if (ref.isSubclass()) {
                    setText("  " + ref.getSubclassName());                    
                }
                else {
                    setFont(getFont().deriveFont(Font.BOLD));
                    setText(ref.getClassificationName());
                }                
            }
            
            return this;
        }
    }
    
    private static class TransparentIcon implements Icon {
        
        private int width;
        private int height;
        
        public TransparentIcon(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
        }

        @Override
        public int getIconWidth() {
            return width;
        }

        @Override
        public int getIconHeight() {
            return height;
        }
    }
}
