package org.gridkit.sjk.ssa.ui;

import java.util.Arrays;

public class StackTreeTest {

    StackTraceElement[] traceA;
    StackTraceElement[] traceB;
    StackTraceElement[] traceC;
    
    private static StackTraceElement[] terminalA() {
        Exception e = new Exception();
        return cut(e.getStackTrace());
    }

    private static StackTraceElement[] terminalB() {
        Exception e = new Exception();
        return cut(e.getStackTrace());
    }

    private static StackTraceElement[] terminalC() {
        Exception e = new Exception();
        return cut(e.getStackTrace());
    }
    
    private static StackTraceElement[] callA() {
        return terminalA();
    }

    private static StackTraceElement[] callB() {
        return terminalB();
    }

    private static StackTraceElement[] cut(StackTraceElement[] trace) {
        int n;
        for(n = 0; n != trace.length; ++n) {
            if (!trace[n].getClassName().equals(StackTreeTest.class.getName()) || trace[n].getMethodName().equals("setup")) {
                break;
            }
        }
        return Arrays.copyOf(trace, n);
    }

    
    
}
