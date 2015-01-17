package org.gridkit.sjk.ssa;

import java.io.Reader;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.gridkit.jvmtool.StackFilterParser.LastNode;
import org.gridkit.sjk.ssa.ClassifierModel.ConjunctionNode;
import org.gridkit.sjk.ssa.ClassifierModel.DisjunctionNode;
import org.gridkit.sjk.ssa.ClassifierModel.Subclass;

@SuppressWarnings("serial")
public class ClassifierModel extends DefaultTreeModel {

    private RootNode root;
    
    public ClassifierModel() {
        super(new RootNode());
        this.root = getRoot();
    }

    @Override
    public RootNode getRoot() {
        return (RootNode)super.getRoot();
    }

    public void load(Reader source) {
        
    }

    public void store(Writer target) {
        
    }
    
    public static class RootNode extends NodeWithComments {
        
        private Map<String, Classification> sections = new HashMap<String, Classification>();
        
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
    
    public static class Classification extends NodeWithComments {

        private String name;
        
        private RootFilter rootFilter;
        
        private Map<String, Subclass> subclasses = new HashMap<String, ClassifierModel.Subclass>();
        
        public Classification(String name) {
            this.name = name;
            rootFilter = new RootFilter();
            add(rootFilter);
        }
        
        public RootFilter getRootFilter() {
            return rootFilter;
        }

        public Subclass newSubclass(String name) {
            if (subclasses.containsKey(name)) {
                throw new IllegalArgumentException("Duplicate category name for '" + this.name + "' classification: " + name);
            }
            else {
                Subclass sc = new Subclass(name);
                subclasses.put(name, sc);
                add(sc);
                return sc;
            }
        }        
    }

    public static class RootFilter extends ConjunctionNode {
        
    }
    
    public static class Subclass extends ConjunctionNode {

        private String name;
        
        public Subclass(String name) {
            this.name = name;
        }

    }

    public static class FramePattern extends NodeWithComments {
        
        private String pattern;
        
        public FramePattern(String pattern) {
            this.pattern = pattern;
        }
        
    }

    public static class CompositePredicateNode extends NodeWithComments {

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

    public static class DisjunctionNode extends CompositePredicateNode {
        
    }

    public static class ConjunctionNode extends CompositePredicateNode {
        
    }

    public static class LastQuantor extends NodeWithComments {

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
    }

    public static class StackFragment extends NodeWithComments {

        private List<FramePattern> pattern = new ArrayList<FramePattern>();
        
    }
    
    public static class FollowPredicate extends ConjunctionNode {

        private boolean negative;
        
        public boolean isNegative() {
            return negative;
        }
    }

    public static class LastCommentNode extends NodeWithComments {
        
    }    
    
    public static class NodeWithComments extends DefaultMutableTreeNode {
        
        String comment = "";
        
    }    
}
