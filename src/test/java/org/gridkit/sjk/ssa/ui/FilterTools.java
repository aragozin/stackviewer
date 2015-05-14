package org.gridkit.sjk.ssa.ui;

import java.lang.Thread.State;
import java.util.LinkedHashMap;
import java.util.Map;

import org.gridkit.jvmtool.stacktrace.AbstractStackFrameArray;
import org.gridkit.jvmtool.stacktrace.CounterArray;
import org.gridkit.jvmtool.stacktrace.CounterCollection;
import org.gridkit.jvmtool.stacktrace.StackFrame;
import org.gridkit.jvmtool.stacktrace.StackFrameList;
import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;
import org.gridkit.jvmtool.stacktrace.analytics.BasicFilterFactory;
import org.gridkit.jvmtool.stacktrace.analytics.ClassificatorAST;
import org.gridkit.jvmtool.stacktrace.analytics.ThreadSnapshotFilter;

public class FilterTools {

    public static SimpleTraceClassifier toSimpleClassifier(ThreadSnapshotFilter rootFilter, Map<String, ThreadSnapshotFilter> subclasses) {
        Classifier cls = new Classifier();
        cls.rootFilter = rootFilter;
        cls.subclasses = new LinkedHashMap<String, ThreadSnapshotFilter>(subclasses);
        return cls;
    }

    public static SimpleTraceClassifier toSimpleClassifier(ThreadSnapshotFilter rootFilter, Map<String, ClassificatorAST.Filter> subclasses, BasicFilterFactory filterFactory) {
        Map<String, ThreadSnapshotFilter> subf = new LinkedHashMap<String, ThreadSnapshotFilter>();
        for(String k: subclasses.keySet()) {
            subf.put(k, filterFactory.build(subclasses.get(k)));
        }
        Classifier cls = new Classifier();
        cls.rootFilter = rootFilter;
        cls.subclasses = subf;
        return cls;
    }

    public static SimpleTraceFilter toSimpleFilter(ThreadSnapshotFilter filter) {
        Filter simpleFilter = new Filter();
        simpleFilter.filter = filter;
        return simpleFilter;
    }
    
    private static class Classifier implements SimpleTraceFilter, SimpleTraceClassifier {

        ThreadSnapshotFilter rootFilter;
        Map<String, ThreadSnapshotFilter> subclasses = new LinkedHashMap<String, ThreadSnapshotFilter>();
        DummyThreadSnapshot dummy = new DummyThreadSnapshot();
        
        @Override
        public String classify(StackFrame[] list) {
            dummy.array = list;
            if (rootFilter != null && !rootFilter.evaluate(dummy)) {
                return null;
            }
            for(String key: subclasses.keySet()) {
                ThreadSnapshotFilter filter = subclasses.get(key);
                if (filter.evaluate(dummy)) {
                    return key;
                }
            }
            return null;
        }

        @Override
        public boolean evaluate(StackFrame[] list) {
            return classify(list) != null;
        }
    }

    private static class Filter implements SimpleTraceFilter {

        ThreadSnapshotFilter filter;
        DummyThreadSnapshot dummy = new DummyThreadSnapshot();
        
        @Override
        public boolean evaluate(StackFrame[] list) {
            dummy.array = list;
            return filter.evaluate(dummy);
        }
    }
    
    private static class DummyThreadSnapshot extends AbstractStackFrameArray implements ThreadSnapshot {

        private StackFrame[] array;
        
        @Override
        protected StackFrame[] array() {
            return array;
        }

        @Override
        protected int from() {
            return 0;
        }

        @Override
        protected int to() {
            return array.length;
        }

        @Override
        public long threadId() {
            return 0;
        }

        @Override
        public String threadName() {
            return null;
        }

        @Override
        public long timestamp() {
            return 0;
        }

        @Override
        public StackFrameList stackTrace() {
            return this;
        }

        @Override
        public State threadState() {
            return null;
        }

        @Override
        public CounterCollection counters() {
            return CounterArray.EMPTY;
        }

        //@Override
        public boolean isEmpty() {
            return depth() == 0;
        }
    }    
}
