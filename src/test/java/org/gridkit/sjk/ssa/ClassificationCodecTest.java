package org.gridkit.sjk.ssa;

import java.io.FileNotFoundException;
import java.io.FileReader;

import org.gridkit.sjk.ssa.ClassifierModel.RootNode;
import org.junit.Test;

public class ClassificationCodecTest {
    
    @Test
    public void parse() throws FileNotFoundException {
        ClassifierModel model = new ClassifierModel();
        RootNode root = model.getRoot();
        FileReader reader = new FileReader("src/test/resources/jboss-seam.scf");
        ClassificationCodec codec = new ClassificationCodec(root);
        codec.parse(reader);
        System.out.println(root);
    }
    
}
