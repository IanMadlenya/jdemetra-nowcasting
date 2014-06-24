/*
 * Copyright 2013 National Bank of Belgium
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved 
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and 
 * limitations under the Licence.
 */
package be.nbb.demetra.dfm;

import be.nbb.demetra.dfm.output.ConfidenceGraph;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.util.various.swing.BasicSwingLauncher;
import java.awt.BorderLayout;
import javax.swing.JPanel;

/**
 *
 * @author Mats Maggi
 */
public class ConfidenceGraphDemo extends JPanel {
    
    public ConfidenceGraphDemo() {
        setLayout(new BorderLayout());
        ConfidenceGraph graph = new ConfidenceGraph();
        
        TsData stdev = Data.P.times(0.1);
        
        graph.setData(Data.P, stdev, null);
        
        add(graph, BorderLayout.CENTER);
        
    }
    

    public static void main(String[] args) {

        new BasicSwingLauncher()
                .content(ConfidenceGraphDemo.class)
                        .launch();
    }
}
