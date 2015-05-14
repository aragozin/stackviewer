package org.gridkit.sjk.ssa.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.jdesktop.swingx.JXTable;

public class StackSourcePane extends JPanel {

    private JXTable table = new JXTable();
    private JToolBar toolbar = new JToolBar();
    
    private Action add = createAddAction();
    private Action remove = createRemoveAction();
    
    private AnalyzerDesktop desktop;
    
    public StackSourcePane() {
        
        toolbar.setFloatable(false);
        toolbar.add(add);
        toolbar.add(remove);
        
        setLayout(new BorderLayout());
        add(toolbar, BorderLayout.SOUTH);
        add(table, BorderLayout.CENTER);
        
    }
    
    protected Action createAddAction() {
        AbstractAction action = new AbstractAction("Add") {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                
            }
        };
        return action;
    }
    
    protected Action createRemoveAction() {
        AbstractAction action = new AbstractAction("Remove") {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                
            }
        };
        return action;
    }

    public void setAnalyzer(AnalyzerDesktop desktop) {
        this.desktop = desktop;
    }
}
