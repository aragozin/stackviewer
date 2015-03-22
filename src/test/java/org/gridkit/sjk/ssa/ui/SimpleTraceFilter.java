package org.gridkit.sjk.ssa.ui;

import org.gridkit.jvmtool.stacktrace.StackFrame;

public interface SimpleTraceFilter {

    public boolean evaluate(StackFrame[] list);
    
}
