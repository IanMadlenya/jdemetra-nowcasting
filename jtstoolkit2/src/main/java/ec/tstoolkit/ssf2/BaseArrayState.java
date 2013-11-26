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
package ec.tstoolkit.ssf2;

import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.design.Development;

/**
 * 
 * @author Jean Palate
 */
@Development(status = Development.Status.Alpha)
public class BaseArrayState extends BaseState {

    /**
     *
     */
    public DataBlock K;

    /**
     *
     */
    public double r;

    /**
     * 
     */
    protected BaseArrayState()
    {
    }

    /**
     * 
     */
    @Override
    public BaseArrayState clone()
    {
        BaseArrayState state=(BaseArrayState) super.clone();
	state.K = K.deepClone();
	return state;
    }

    /**
     * 
     * @param n
     * @param hasdata
     */
    protected BaseArrayState(final int n, final boolean hasdata)
    {
	super(hasdata ? n : 0);
	K = new DataBlock(n);
    }

    /**
     * 
     * @param state
     */
    public void copy(final BaseArrayState state)
    {
	super.copy(state);
	K = state.K.deepClone();
	r = state.r;
    }
}
