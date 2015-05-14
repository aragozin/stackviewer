package org.gridkit.sjk.ssa.ui;

import java.io.FileReader;
import java.io.IOException;

import org.gridkit.sjk.ssa.ui.ClassificationCodec.ParseError;
import org.gridkit.sjk.ssa.ui.ClassificationCodec.ParseResult;
import org.junit.Test;

public class ClassificationCodecTest {
    
    @Test
    public void parse() throws IOException {
        FileReader reader = new FileReader("src/test/resources/jboss-seam.scf");
        ClassificationCodec codec = new ClassificationCodec();
        ParseResult result = codec.parse(IOHelper.read(reader));
        for(ParseError e: result.errors) {
            System.err.println(e);
        }
        System.out.println(result.ast);
    }
    
}
