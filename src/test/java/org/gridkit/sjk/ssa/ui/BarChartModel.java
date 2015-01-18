package org.gridkit.sjk.ssa.ui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class BarChartModel {

    List<BarInfo> info = new ArrayList<BarInfo>();
    

    
    public static class BarInfo {
        
        private double weight;
        private String caption;
        private Color color;
        
        public double getWeight() {
            return weight;            
        }
        
        public String getCaption() {
            return caption;
        }
        
        public Color getColor() {
            return color;
        }        
    }
}
