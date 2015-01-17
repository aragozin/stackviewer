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


    public void reset() {
        RootNode node = new RootNode();
        setRoot(node);
    }        
    
    public void load(Reader source) {
        
    }

    public void store(Writer target) {
        
    }
    
    public static class RootNode extends CommonNode {
        
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

        @Override
        public String getHtmlCaption() {
            return "";
        }
    }
    
    public static class Classification extends CommonNode {

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

        @Override
        public String getHtmlCaption() {
            return "<html><b>" + name + "</b></html>";
        }        
    }

    public static class RootFilter extends ConjunctionNode {

        @Override
        public String getHtmlCaption() {
            return "<html><i>filter</i></html>";
        }        
    }
    
    public static class Subclass extends ConjunctionNode {

        private String name;
        
        public Subclass(String name) {
            this.name = name;
        }

        @Override
        public String getHtmlCaption() {
            return "<html>" + name + "</html>";
        }        
    }

    public static class FramePattern extends CommonNode {
        
        private String pattern;
        
        public FramePattern(String pattern) {
            this.pattern = pattern;
        }

        @Override
        public String getHtmlCaption() {
            return "<html><span style=\"font=monospace\">" + pattern + "</span></html>";
        }                
    }

    public static abstract class CompositePredicateNode extends CommonNode {

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

        @Override
        public String getHtmlCaption() {
            return "<html><span style=\"color: #B44\"><b>AND</b></span></html>";
        }
    }

    public static class ConjunctionNode extends CompositePredicateNode {

        @Override
        public String getHtmlCaption() {
            return "<html><span style=\"color: #B44\"><b>OR</b></span></html>";
        }
    }

    public static class LastQuantor extends CommonNode {

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
        public String getHtmlCaption() {
            return "<html><span style=\"color: #B44\"><b>LAST</b></span></html>";
        }        
    }

    public static class StackFragment extends CommonNode {

        private List<FramePattern> pattern = new ArrayList<FramePattern>();

        @Override
        public String getHtmlCaption() {
            return "<html><i>fragment</i></html>";
        }        
    }
    
    public static class FollowPredicate extends ConjunctionNode {

        private boolean negative;
        
        public boolean isNegative() {
            return negative;
        }

        public String getHtmlCaption() {
            return negative 
                    ? "<html><i>not followed by</i></html>" 
                    : "<html><i>followed by</i></html>";
        }        
    }

    public static class LastCommentNode extends CommonNode {

        @Override
        public String getHtmlCaption() {
            return "";
        }
    }    
    
    public static abstract class CommonNode extends DefaultMutableTreeNode {
        
        String comment = "";
        
        public String getComment() {
            return comment;
        }
        
        public abstract String getHtmlCaption();
        
        public String getGlyphName() {
            return null;
        }
    }
}
