package org.gridkit.sjk.ssa.ui;

import java.util.ArrayList;
import java.util.List;

import org.gridkit.jvmtool.stacktrace.analytics.ClassificatorAST;
import org.gridkit.jvmtool.stacktrace.analytics.ClassificatorAST.Root;
import org.gridkit.jvmtool.stacktrace.analytics.ClassificatorParser;
import org.gridkit.jvmtool.stacktrace.util.IndentParser.ParseException;

class ClassificationCodec {

    public ParseResult parse(List<String> text) {
        Root root = null;
        List<ParseError> errors = new ArrayList<ParseError>();
        try {
            Parser parser = new Parser();
            for(String line: text) {
                parser.push(line);
            }
            parser.finish();
            root = parser.getResult();
        }
        catch(ParseException e) {
            errors.add(new ParseError(e.getLine(), e.getPosition(), e.getMessage(), e));
        }
        return new ParseResult(root, errors);
    }

    static class Parser extends ClassificatorParser {
        
    }
        
    public static class ParseResult {
        
        public ClassificatorAST.Root ast;
        public List<ParseError> errors = new ArrayList<ParseError>();
        
        public ParseResult(Root ast, List<ParseError> errors) {
            this.ast = ast;
            this.errors = errors;
        }
    }
    
    public static class ParseError {
        
        public int line;
        public int pos;
        public String error;
        public Exception trace;
        
        public ParseError(int line, int pos, String error, Exception trace) {
            this.line = line;
            this.pos = pos;
            this.error = error;
            this.trace = trace;
        }
        
        @Override
        public String toString() {
            return "[" + line + ":" + pos + "] " + error;
        }
    }
}