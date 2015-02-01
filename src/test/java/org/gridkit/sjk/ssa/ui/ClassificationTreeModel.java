package org.gridkit.sjk.ssa.ui;

import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.gridkit.jvmtool.StackTraceFilter;
import org.gridkit.jvmtool.StackTraceFilter.ElementMatcher;
import org.gridkit.jvmtool.StackTraceFilterHelper;

@SuppressWarnings("serial")
public class ClassificationTreeModel extends DefaultTreeModel {

    
    public ClassificationTreeModel() {
        super(new RootNode());
    }

    @Override
    public RootNode getRoot() {
        return (RootNode)super.getRoot();
    }

    public void load(Reader source) {
        RootNode node = new RootNode();
        new ClassificationCodec(node).parse(source);
        setRoot(node);
    }

    public void store(Writer target) {
        
    }
    
    public void validate() {
        getRoot().errorMessages.clear();
        
        List<Classification> cl = getRoot().getChildren();
        for(Classification c: cl) {
            c.makeClassifier();
        }
    }
    
    public boolean hasErrors() {
        return !getRoot().errorMessages.isEmpty();
    }

    public List<String> getAllErrors() {
        return new ArrayList<String>(getRoot().errorMessages.values());
    }

    public String getErrorFor(CommonNode node) {
        return getRoot().errorMessages.get(node);
    }

    private static StackTraceFilter markErrorNode(CommonNode node, String error) {
        while(!(node instanceof RootNode)) {
            node = (CommonNode) node.getParent();
        }
        ((RootNode)node).errorMessages.put(node, error);
        return new ErrorFilterStub();
    }
    
    public List<ClassificationEditor.FilterRef> getFilters() {
        List<ClassificationEditor.FilterRef> filters = new ArrayList<ClassificationEditor.FilterRef>();
        List<Classification> cl = getRoot().getChildren();
        for(Classification c: cl) {
            String cn = c.getName();
            filters.add(new ClassificationEditor.FilterRef(cn));
            List<CommonNode> sl = c.getChildren();
            for(CommonNode s: sl) {
                if (s instanceof Subclass) {
                        String sn = ((Subclass) s).getName();
                        filters.add(new ClassificationEditor.FilterRef(cn, sn));
                }
            }
        }
        return filters;
    }

    public List<ClassificationEditor.FilterRef> getCategories() {
        List<ClassificationEditor.FilterRef> filters = new ArrayList<ClassificationEditor.FilterRef>();
        List<Classification> cl = getRoot().getChildren();
        for(Classification c: cl) {
            String cn = c.getName();
            List<CommonNode> sl = c.getChildren();
            for(CommonNode s: sl) {
                if (s instanceof Subclass) {
                    // has at least one sub category
                    filters.add(new ClassificationEditor.FilterRef(cn));
                    break;
                }
            }
        }
        return filters;
    }
    
    public StackTraceFilter getFilter(ClassificationEditor.FilterRef ref) {
        String cn = ref.getClassificationName();
        List<Classification> cl = getRoot().getChildren();
        for(Classification c: cl) {
            if (cn.equals(c.getName())) {
                StackTraceClassifier stc = c.makeClassifier();
                return new BucketFilter(stc, ref.getSubclassName());
            }
        }
        throw new IllegalArgumentException("Invalid ref: " + ref);
    }

    public StackTraceClassifier getClassifier(ClassificationEditor.FilterRef ref) {
        String cn = ref.getClassificationName();
        List<Classification> cl = getRoot().getChildren();
        for(Classification c: cl) {
            if (cn.equals(c.getName())) {
                StackTraceClassifier stc = c.makeClassifier();
                return stc;
            }
        }
        throw new IllegalArgumentException("Invalid ref: " + ref);        
    }
    
    public static class RootNode extends CommonNode {
        
        private Map<CommonNode, String> errorMessages = new LinkedHashMap<ClassificationTreeModel.CommonNode, String>();
        
        private Map<String, Classification> sections = new LinkedHashMap<String, Classification>();
        
        public Classification newClassification(String name) {
            if (sections.containsKey(name)) {
                throw new IllegalArgumentException("Duplicate category name: " + name);
            }
            Classification c = new Classification(name);
            sections.put(name, c);
            add(c);
            return c;
        }
    }
    
    public static class Classification extends CommonNode {

        private String name;
        
        private RootFilter rootFilter;
        
        public Classification(String name) {
            this.name = name;
            rootFilter = new RootFilter();
            add(rootFilter);
        }
        
        public String getName() {
            return name;
        }

        public RootFilter getRootFilter() {
            return rootFilter;
        }
        
        public List<Subclass> getSubclasses() {
            List<Subclass> result = getChildren();
            result = result.subList(1, result.size());
            return result;
        }

        public Subclass newSubclass(String name) {
            Subclass sc = new Subclass(name);
            add(sc);
            return sc;
        }
        
        public StackTraceClassifier makeClassifier() {
            StackTraceFilter filter = rootFilter.makeFilter();
            List<Subclass> subclasses = getSubclasses();            
            if (subclasses.isEmpty()) {
                return new StackClassifier(filter, Collections.singletonMap("other", StackTraceFilterHelper.trueFilter()));
            }
            else {
                Map<String, StackTraceFilter> buckets = new LinkedHashMap<String, StackTraceFilter>();
                for(Subclass sc: subclasses) {
                    String name = sc.getName();
                    if (buckets.containsKey(name)) {
                        markErrorNode(sc, "Subclass '" + name + "' is already exists");
                    }
                    buckets.put(sc.name, sc.makeFilter());
                }
                return new StackClassifier(filter, buckets);
            }
        }
    }

    public static class RootFilter extends ConjunctionNode {

        @Override
        public StackTraceFilter makeFilter() {
            if (getChildCount() == 0) {
                return StackTraceFilterHelper.trueFilter();
            }
            else {
                return super.makeFilter();
            }
        }
    }
    
    public static class Subclass extends ConjunctionNode {

        private String name;
        
        public Subclass(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public StackTraceFilter makeFilter() {
            if (getChildCount() == 0) {
                return StackTraceFilterHelper.trueFilter();
            }
            else {
                return super.makeFilter();
            }
        }
    }

    public static class FramePattern extends CommonNode implements StackTracePredicate {
        
        private String pattern;
        
        public FramePattern(String pattern) {
            this.pattern = pattern;
        }

        public String getPattern() {
            return pattern;
        }                

        @Override
        public StackTraceFilter makeFilter() {
            return StackTraceFilterHelper.createElementMatcherFilter(StackTraceFilterHelper.createElementMatcher(pattern));
        }
        
    }

    public static abstract class CompositePredicateNode extends CommonNode implements StackTracePredicate {

        public DisjunctionNode newDisjunction() {
            DisjunctionNode node = new DisjunctionNode();
            add(node);
            return node;
        }

        public ConjunctionNode newConjunction() {
            ConjunctionNode node = new ConjunctionNode();
            add(node);
            return node;
        }

        public FramePattern newFramePattern(String pattern) {
            FramePattern node = new FramePattern(pattern);
            add(node);
            return node;
        }

        public LastQuantor newLastQuantor() {
            LastQuantor node = new LastQuantor();
            add(node);
            return node;
        }        
    }

    public static interface StackTracePredicate {

        public StackTraceFilter makeFilter();
        
    }
    
    public static class DisjunctionNode extends CompositePredicateNode {

        @Override
        public StackTraceFilter makeFilter() {
            List<CommonNode> children = getChildren();
            List<StackTraceFilter> filters = new ArrayList<StackTraceFilter>();
            for(CommonNode cn: children) {
                if (cn instanceof StackTracePredicate) {
                    filters.add(((StackTracePredicate)cn).makeFilter());
                }
            }
            
            if (filters.isEmpty()) {
                return markErrorNode(this, "AND operator has no arguments");
            }
            
            return StackTraceFilterHelper.createFilterDisjunction(filters);
        }
    }

    public static class ConjunctionNode extends CompositePredicateNode {

        @Override
        public StackTraceFilter makeFilter() {
            List<CommonNode> children = getChildren();
            List<StackTraceFilter> filters = new ArrayList<StackTraceFilter>();
            for(CommonNode cn: children) {
                if (cn instanceof StackTracePredicate) {
                    filters.add(((StackTracePredicate)cn).makeFilter());
                }
            }
            
            if (filters.isEmpty()) {
                return markErrorNode(this, "OR operator has no arguments");
            }
            
            return StackTraceFilterHelper.createFilterConjunction(filters);
        }        
    }

    public static class LastQuantor extends CommonNode implements StackTracePredicate {

        private StackFragment stackFragment = new StackFragment();
        private FollowPredicate followPredicate = new FollowPredicate();
        
        public LastQuantor() {
            add(stackFragment);
            add(followPredicate);
        }
        
        public void addFramePattern(String pattern) {
            stackFragment.add(new FramePattern(pattern));
        }

        public void setNotFollowedSemantic(boolean negated) {
            followPredicate.negative = negated;
        }
        
        public CompositePredicateNode getPredicate() {
            return followPredicate;
        }
        
        @Override
        public StackTraceFilter makeFilter() {
            List<FramePattern> framePatterns = stackFragment.getChildren();
            List<String> patterns = new ArrayList<String>();
            
            for(FramePattern cn: framePatterns) {
                patterns.add(cn.getPattern());
            }
            
            if (patterns.isEmpty()) {
                return markErrorNode(this, "No element pattern for LAST node");
            }
            
            ElementMatcher matcher = StackTraceFilterHelper.createElementMatcher(patterns);

            StackTraceFilter predicate = followPredicate.makeFilter();
            
            if (followPredicate.negative) {
                return StackTraceFilterHelper.createLastNotFollowedMatcher(matcher, predicate);
            }
            else {
                return StackTraceFilterHelper.createLastFollowedMatcher(matcher, predicate);                
            }
        }        
    }

    public static class StackFragment extends CommonNode {

    }
    
    public static class FollowPredicate extends ConjunctionNode {

        private boolean negative;
        
        public boolean isNegative() {
            return negative;
        }
    }

    public static class LastCommentNode extends CommonNode {

    }    
    
    public static abstract class CommonNode extends DefaultMutableTreeNode {
        
        String comment = "";
        
        public String getComment() {
            return comment;
        }
        
        @SuppressWarnings("unchecked")
        public <T> List<T> getChildren() {
            List<T> list = new ArrayList<T>();
            for(int i = 0; i != getChildCount(); ++i) {
                list.add((T) getChildAt(i));
            }
            return list;
        }
    }
    
    static class StackClassifier extends org.gridkit.jvmtool.StackTraceClassifier implements StackTraceClassifier {
     
        public StackClassifier(StackTraceFilter root, Map<String, StackTraceFilter> filters) {
            super(root, filters);
        }

        @Override
        public String classify(StackTraceElement[] trace) {
            return super.classify(trace);
        }        
    }
    
    static class ErrorFilterStub implements StackTraceFilter {

        @Override
        public boolean evaluate(StackTraceElement[] trace) {
            throw new UnsupportedOperationException("Error node invoked");
        }        
    }
}
