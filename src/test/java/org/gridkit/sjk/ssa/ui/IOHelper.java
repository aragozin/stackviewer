package org.gridkit.sjk.ssa.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

class IOHelper {

    public static List<String> read(Reader reader) throws IOException {
        BufferedReader br = toBuffered(reader);
        List<String> list = new ArrayList<String>();
        while(true) {
            String line = br.readLine();
            if (line == null) {
                break;
            }
            else {
                list.add(line);
            }
        }
        return list;
    }

    public static BufferedReader toBuffered(Reader reader) {
        return reader instanceof BufferedReader ? (BufferedReader)reader : new BufferedReader(reader);
    }
    
}
