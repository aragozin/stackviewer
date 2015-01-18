package org.gridkit.sjk.ssa.ui;

import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.gridkit.jvmtool.StackTraceFilter;

@SuppressWarnings("serial")
public class ClassifierModel extends DefaultTreeModel {

    public ClassifierModel() {
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

    public List<FilterRef> getFilters() {
        List<FilterRef> filters = new ArrayList<FilterRef>();
        List<Classification> cl = getRoot().getChildren();
        for(Classification c: cl) {
            String cn = c.getName();
            filters.add(new FilterRef(cn));
            List<CommonNode> sl = c.getChildren();
            for(CommonNode s: sl) {
                if (s instanceof Subclass) {
                        String sn = ((Subclass) s).getName();
                        filters.add(new FilterRef(cn, sn));
                }
            }
        }
        return filters;
    }
    
    public StackTraceFilter getFilter(FilterRef ref) {
        throw new UnsupportedOperationException();
    }
    
    public static class FilterRef implements Comparable<FilterRef> {
        
        private String classificationName;
        private String subclassName;
        
        public FilterRef(String classificationName) {
            this.classificationName = classificationName;
        }

        public FilterRef(String classificationName, String subclassName) {
            this.classificationName = classificationName;
            this.subclassName = subclassName;
        }

        public String getClassificationName() {
            return classificationName;
        }

        public String getSubclassName() {
            return subclassName;
        }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((classificationName == null) ? 0 : classificationName.hashCode());
            result = prime * result + ((subclassName == null) ? 0 : subclassName.hashCode());
            return result;
        }

        public boolean isSubclass() {
            return subclassName != null;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            FilterRef other = (FilterRef) obj;
            if (classificationName == null) {
                if (other.classificationName != null)
                    return false;
            } else if (!classificationName.equals(other.classificationName))
                return false;
            if (subclassName == null) {
                if (other.subclassName != null)
                    return false;
            } else if (!subclassName.equals(other.subclassName))
                return false;
            return true;
        }

        @Override
        public int compareTo(FilterRef o) {
            int n = classificationName.compareTo(o.classificationName);
            if (n != 0) {
                return n;
            }
            String s1 = subclassName == null ? "" : subclassName;
            String s2 = o.subclassName == null ? "" : o.subclassName;
            return s1.compareTo(s2);
        }
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
        
        public String getName() {
            return name;
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

        public String getName() {
            return name;
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
        
        @SuppressWarnings("unchecked")
        public <T> List<T> getChildren() {
            List<T> list = new ArrayList<T>();
            for(int i = 0; i != getChildCount(); ++i) {
                list.add((T) getChildAt(i));
            }
            return list;
        }
        
        public abstract String getHtmlCaption();
        
        public String getGlyphName() {
            return null;
        }
    }
}
