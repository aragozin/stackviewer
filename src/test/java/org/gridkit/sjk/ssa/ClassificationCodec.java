package org.gridkit.sjk.ssa;

import java.io.Reader;

import org.gridkit.jvmtool.Cascade;
import org.gridkit.jvmtool.StackFilterParser;
import org.gridkit.jvmtool.StackFilterParser.AllNode;
import org.gridkit.jvmtool.StackFilterParser.AnyNode;
import org.gridkit.jvmtool.StackFilterParser.LastNode;
import org.gridkit.jvmtool.StackFilterParser.LiteralNode;
import org.gridkit.sjk.ssa.ClassifierModel.Classification;
import org.gridkit.sjk.ssa.ClassifierModel.CompositePredicateNode;
import org.gridkit.sjk.ssa.ClassifierModel.ConjunctionNode;
import org.gridkit.sjk.ssa.ClassifierModel.DisjunctionNode;
import org.gridkit.sjk.ssa.ClassifierModel.LastQuantor;
import org.gridkit.sjk.ssa.ClassifierModel.RootNode;
import org.gridkit.sjk.ssa.ClassifierModel.Subclass;

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
    public ConjunctionParser section(String line) {
        line = line.trim();
        if (line.startsWith("[") && line.endsWith("]")) {
            String name = line.substring(1, line.length() - 1);
            Classification c = root.newClassification(name);
            return new ConjunctionParser(c.getRootFilter());
        }
        else if (line.startsWith("+")) {
            String name = line.substring(1);
            if (lastClassification == null) {
                throw new IllegalArgumentException("Subcategory should follow a category section: " + name);
            }
            Subclass sc = lastClassification.newSubclass(name);
            return new ConjunctionParser(sc);
        }
        else {
            throw new IllegalArgumentException("Expected section name: " + line);
        }
    }
    
    public class FilterParser {
        
        CompositePredicateNode node;
        
        public FilterParser(CompositePredicateNode node) {
            this.node = node;
        }
        
        @Cascade.Section
        public FilterParser child(String line) {
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
                    return new LastParser(subnode);
                }
                else {
                    throw new IllegalArgumentException("Enexpected directive: " + dir);
                }
            }
            else {
                
                subnodes.add(node);
                return node;
            }
        }
    }
    
    static String normalizeDirective(String line) {
        return line.substring(1).replace("\\s+", " ").toUpperCase().trim();
    }    
}