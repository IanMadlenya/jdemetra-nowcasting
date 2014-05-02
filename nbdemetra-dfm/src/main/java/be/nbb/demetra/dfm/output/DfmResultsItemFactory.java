/*
 * Copyright 2013 National Bank of Belgium
 * 
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved 
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
package be.nbb.demetra.dfm.output;

import com.google.common.base.Optional;
import ec.tss.Dfm.DfmDocument;
import ec.tss.Dfm.DfmResults;
import ec.tstoolkit.utilities.DefaultInformationExtractor;
import ec.tstoolkit.utilities.Id;
import ec.ui.view.tsprocessing.ComposedProcDocumentItemFactory;
import ec.ui.view.tsprocessing.IProcDocumentView;
import ec.ui.view.tsprocessing.ItemUI;

/**
 *
 * @author Philippe Charles
 */
abstract class DfmResultsItemFactory extends ComposedProcDocumentItemFactory<DfmDocument, Optional<DfmResults>> {

    protected DfmResultsItemFactory(Id itemId, ItemUI<? extends IProcDocumentView<DfmDocument>, Optional<DfmResults>> itemUI) {
        super(DfmDocument.class, itemId, DfmResultsExtractor.INSTANCE, itemUI);
    }

    private static final class DfmResultsExtractor extends DefaultInformationExtractor<DfmDocument, Optional<DfmResults>> {

        public static final DfmResultsExtractor INSTANCE = new DfmResultsExtractor();

        @Override
        public Optional<DfmResults> retrieve(DfmDocument source) {
            return Optional.fromNullable(source.getResults().get("dfm", DfmResults.class));
        }
    }
}
