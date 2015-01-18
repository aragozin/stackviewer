package org.gridkit.sjk.ssa.ui;

import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;

public class ModelUtils {

    public static void updateComboBoxModel(ComboBoxModel model, List<?> values) {
        DefaultComboBoxModel dcbm = (DefaultComboBoxModel) model;
        Object selected = dcbm.getSelectedItem();
        dcbm.removeAllElements();
        for(Object e: values) {
            dcbm.addElement(e);
            if (e.equals(selected)) {
                dcbm.setSelectedItem(e);
            }
        }
    }    
}
