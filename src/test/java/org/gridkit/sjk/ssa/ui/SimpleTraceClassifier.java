package org.gridkit.sjk.ssa.ui;

import org.gridkit.jvmtool.stacktrace.StackFrame;

public interface SimpleTraceClassifier {

    public String classify(StackFrame[] list);

}
