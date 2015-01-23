package org.gridkit.sjk.ssa.ui;

import org.gridkit.jvmtool.StackTraceFilter;

public class BucketFilter implements StackTraceFilter {

    private final StackTraceClassifier classifier;
    private final String bucket;
    
    public BucketFilter(StackTraceClassifier classifier) {
        this(classifier, null);
    }

    public BucketFilter(StackTraceClassifier classifier, String bucketName) {
        this.classifier = classifier;
        this.bucket = bucketName;
    }

    @Override
    public boolean evaluate(StackTraceElement[] trace) {
        String hit = classifier.classify(trace);
        if (bucket == null) {
            return hit != null;
        }
        else {
            return bucket.equals(hit);
        }
    }
}
