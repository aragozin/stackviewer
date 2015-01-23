package org.gridkit.sjk.ssa.ui;

import java.io.FileNotFoundException;
import java.io.FileReader;

import org.gridkit.sjk.ssa.ui.ClassificationTreeModel.RootNode;
import org.junit.Test;

public class ClassificationCodecTest {
    
    @Test
    public void parse() throws FileNotFoundException {
        ClassificationTreeModel model = new ClassificationTreeModel();
        RootNode root = model.getRoot();
        FileReader reader = new FileReader("src/test/resources/jboss-seam.scf");
        ClassificationCodec codec = new ClassificationCodec(root);
        codec.parse(reader);
        System.out.println(root);
    }
    
}
