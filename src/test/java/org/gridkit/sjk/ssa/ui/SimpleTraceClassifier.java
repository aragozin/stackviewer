package org.gridkit.sjk.ssa.ui;

import org.gridkit.jvmtool.stacktrace.StackFrame;

public interface SimpleTraceClassifier extends SimpleTraceFilter {

    public String classify(StackFrame[] list);

}
