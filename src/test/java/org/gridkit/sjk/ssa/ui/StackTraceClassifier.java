package org.gridkit.sjk.ssa.ui;

public interface StackTraceClassifier {

    public String classify(StackTraceElement[] trace);
    
}
