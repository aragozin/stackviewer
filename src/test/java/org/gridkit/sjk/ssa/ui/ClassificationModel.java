package org.gridkit.sjk.ssa.ui;

import java.beans.PropertyChangeListener;
import java.util.List;

import org.gridkit.jvmtool.StackTraceFilter;
import org.gridkit.sjk.ssa.ui.ClassificationEditor.FilterRef;

public interface ClassificationModel {

    public void removeClassificationListener(PropertyChangeListener listener);

    public void addClassificationListener(PropertyChangeListener listener);

    public StackTraceClassifier getClassifier(FilterRef ref);

    public StackTraceFilter getFilter(FilterRef ref);

    public List<FilterRef> getAvailableClassifications();

    public List<FilterRef> getAvailableFilters();

}
