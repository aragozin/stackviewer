package org.gridkit.sjk.ssa.ui;

import java.io.Reader;

import org.gridkit.jvmtool.Cascade;
import org.gridkit.jvmtool.StackFilterParser;
import org.gridkit.sjk.ssa.ui.ClassificationTreeModel.Classification;
import org.gridkit.sjk.ssa.ui.ClassificationTreeModel.CompositePredicateNode;
import org.gridkit.sjk.ssa.ui.ClassificationTreeModel.ConjunctionNode;
import org.gridkit.sjk.ssa.ui.ClassificationTreeModel.DisjunctionNode;
import org.gridkit.sjk.ssa.ui.ClassificationTreeModel.LastQuantor;
import org.gridkit.sjk.ssa.ui.ClassificationTreeModel.RootNode;
import org.gridkit.sjk.ssa.ui.ClassificationTreeModel.Subclass;

class ClassificationCodec {

    public static final String D_REQUIRE = StackFilterParser.D_REQUIRE;
    public static final String D_ANY = StackFilterParser.D_ANY;
    public static final String D_LAST = StackFilterParser.D_LAST;
    public static final String D_FOLLOWED = StackFilterParser.D_FOLLOWED;
    public static final String D_NOT_FOLLOWED = StackFilterParser.D_NOT_FOLLOWED;
    
    RootNode root;
    Classification lastClassification;

    public ClassificationCodec(RootNode root) {
        this.root = root;
    }
    
    public void parse(Reader reader) {
        Cascade.parse(reader, this);
    }
    
    @Cascade.Section
    public FilterParser section(String line) {
        line = line.trim();
        if (line.startsWith("[") && line.endsWith("]")) {
            String name = line.substring(1, line.length() - 1);
            Classification c = root.newClassification(name);
            lastClassification = c;
            return new FilterParser(c.getRootFilter());
        }
        else if (line.startsWith("+")) {
            String name = line.substring(1);
            if (lastClassification == null) {
                throw new IllegalArgumentException("Subcategory should follow a category section: " + name);
            }
            Subclass sc = lastClassification.newSubclass(name);
            return new FilterParser(sc);
        }
        else {
            throw new IllegalArgumentException("Expected section name: " + line);
        }
    }
    
    public abstract class SectionParser {
        
        public abstract SectionParser child(String line);
        
    }
    
    public class FilterParser extends SectionParser {
        
        CompositePredicateNode node;
        
        public FilterParser(CompositePredicateNode node) {
            this.node = node;
        }
        
        @Cascade.Section
        public SectionParser child(String line) {
            if (line.startsWith("!")) {
                String dir = normalizeDirective(line);
                if (dir.equals(D_REQUIRE)) {
                    DisjunctionNode subnode = node.newDisjunction();
                    return new FilterParser(subnode);
                }
                else if (dir.equals(D_ANY)) {
                    ConjunctionNode subnode = node.newConjunction();
                    return new FilterParser(subnode);
                }
                else if (dir.equals(D_LAST)) {
                    LastQuantor subnode = node.newLastQuantor();
                    return new LastQuantorParser(subnode);
                }
                else {
                    throw new IllegalArgumentException("Enexpected directive: " + dir);
                }
            }
            else {
                node.newFramePattern(line);
                return null;
            }
        }
    }

    public class LastQuantorParser extends SectionParser {
        
        LastQuantor node;
        boolean hasTarget = false;
        boolean hasPredicate = false;
        
        public LastQuantorParser(LastQuantor node) {
            this.node = node;
        }
        
        @Cascade.Section
        public SectionParser child(String line) {
            if (line.startsWith("!")) {
                String dir = normalizeDirective(line);

                if (hasPredicate) {
                    throw new IllegalArgumentException("Could be only one directive");
                }
                if (!hasTarget) {
                    throw new IllegalArgumentException("Matcher is required");
                }

                hasPredicate = true;
                if (dir.equals(D_FOLLOWED)) {
                    return new FilterParser(node.getPredicate());
                }
                else if (dir.equals(D_NOT_FOLLOWED)) {
                    node.setNotFollowedSemantic(true);
                    return new FilterParser(node.getPredicate());
                }
                else {
                    throw new IllegalArgumentException("Enexpected directive: " + dir);
                }
            }
            else {
                if (hasPredicate) {
                    throw new IllegalArgumentException("Should be no nested line after predicate directive");
                }
                
                hasTarget = true;
                node.addFramePattern(line);
                
                return null;
            }
        }
    }
    
    
    static String normalizeDirective(String line) {
        return line.substring(1).replace("\\s+", " ").toUpperCase().trim();
    }    
}