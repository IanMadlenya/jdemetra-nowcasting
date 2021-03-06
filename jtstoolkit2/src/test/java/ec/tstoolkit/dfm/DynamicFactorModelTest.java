/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.tstoolkit.dfm;

import ec.tstoolkit.timeseries.information.TsInformationSet;
import ec.tstoolkit.timeseries.information.TsInformationUpdates;
import data.Data;
import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.data.DescriptiveStatistics;
import ec.tstoolkit.dfm.DynamicFactorModel.MeasurementDescriptor;
import ec.tstoolkit.dfm.DynamicFactorModel.MeasurementStructure;
import ec.tstoolkit.dfm.DynamicFactorModel.MeasurementType;
import ec.tstoolkit.eco.Likelihood;
import ec.tstoolkit.maths.matrices.Matrix;
import ec.tstoolkit.mssf2.ArrayFilter;
import ec.tstoolkit.mssf2.DefaultTimeInvariantMultivariateSsf;
import ec.tstoolkit.mssf2.FullM2UMap;
import ec.tstoolkit.mssf2.FullMSsf;
import ec.tstoolkit.mssf2.IArraySsf;
import ec.tstoolkit.mssf2.IMSsf;
import ec.tstoolkit.mssf2.M2UAdapter;
import ec.tstoolkit.mssf2.M2UData;
import ec.tstoolkit.mssf2.MFilter;
import ec.tstoolkit.mssf2.MPredictionErrorDecomposition;
import ec.tstoolkit.mssf2.MSmoother;
import ec.tstoolkit.mssf2.MSmoothingResults;
import ec.tstoolkit.mssf2.MultivariateSsfData;
import ec.tstoolkit.ssf2.DiffuseFilteringResults;
import ec.tstoolkit.ssf2.Filter;
import ec.tstoolkit.ssf2.PredictionErrorDecomposition;
import ec.tstoolkit.ssf2.ResidualsCumulator;
import ec.tstoolkit.ssf2.Smoother;
import ec.tstoolkit.ssf2.SmoothingResults;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import ec.tstoolkit.var.VarSpec;
import java.util.HashSet;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author palatej
 */
public class DynamicFactorModelTest {

    static final DynamicFactorModel dmodel = new DynamicFactorModel(12, 3);
    static final int N = 500;
    static final boolean stressTest = false;

    public static void evaluate(final ResidualsCumulator rslts,
            final Likelihood ll) {
        int n = rslts.getObsCount();
        double ssqerr = rslts.getSsqErr(), ldet = rslts.getLogDeterminant();
        ll.set(ssqerr, ldet, n);
    }
    private static Matrix T, TVar, MVar, D, O, Z, M, dd, ddrnd;

    private static void loadDavidModel() {
        T = Data.readMatrix(DynamicFactorModelTest.class, "/transition.csv");
        //System.out.println(T);
        TVar = Data.readMatrix(DynamicFactorModelTest.class, "/tcovar.csv");
        //System.out.println(TVar);
        MVar = Data.readMatrix(DynamicFactorModelTest.class, "/mcovar.csv");
        //System.out.println(MVar);
        D = Data.readMatrix(DynamicFactorModelTest.class, "/data.csv");
        O = Data.readMatrix(DynamicFactorModelTest.class, "/original.csv");
        //System.out.println(D);
        Z = Data.readMatrix(DynamicFactorModelTest.class, "/loadings.csv");
        //System.out.println(Z);
        M = Data.readMatrix(DynamicFactorModelTest.class, "/model.csv");
        //System.out.println(M);

        //transition equation
        int nb = 3, nl = 4, c = 24;
        DynamicFactorModel.TransitionDescriptor tdesc = new DynamicFactorModel.TransitionDescriptor(nb, nl);
        for (int i = 0; i < nb; ++i) {
            DataBlock trow = T.row(i * 12).range(0, nl);
            DataBlock prow = tdesc.varParams.row(i).range(0, nl);
            for (int j = 0; j < nb; ++j) {
                prow.copy(trow);
                trow.move(12);
                prow.move(nl);
            }
        }
        for (int i = 0; i < nb; ++i) {
            DataBlock trow = TVar.row(i * 12).extract(0, nb, 12);
            DataBlock prow = tdesc.covar.row(i);
            prow.copy(trow);
        }
        dmodel.setTransition(tdesc);

        // measurement equation
        int nv = 0;
        for (int i = 0; i < M.getRowsCount(); ++i) {
            if (!M.row(i).range(0, 3).isZero()) {
                ++nv;
            }
        }
        dd = new Matrix(nv, O.getRowsCount() + 15);
        dd.set(Double.NaN);
        for (int i = 0, j = 0; i < M.getRowsCount(); ++i) {
            if (!M.row(i).range(0, 4).isZero()) {
                DataBlock row = dd.row(j);
                row.range(0, O.getRowsCount()).copy(O.column(j));
                DescriptiveStatistics stats = new DescriptiveStatistics(O.column(j));
                double m = stats.getAverage();
                double e = stats.getStdev();
//                boolean log = M.get(i, 4) != 0, diff = M.get(i, 5) != 0;
//                if (log) {
//                    for (int k = 0; k < row.getLength(); ++k) {
//                        double w = row.get(k);
//                        if (DescriptiveStatistics.isFinite(w)) {
//                            row.set(k, Math.log(w));
//                        }
//                    }
//                }
//                if (diff) {
//                    double prev = row.get(0);
//                    for (int k = 1; k < row.getLength(); ++k) {
//                        double w = row.get(k);
//                        if (DescriptiveStatistics.isFinite(w)) {
//                            if (DescriptiveStatistics.isFinite(prev)) {
//                                row.set(k, w - prev);
//                            } else {
//                                row.set(k, Double.NaN);
//                            }
//                        }
//                        prev = w;
//                    }
//                }
//                double mm = M.get(i, 7), ee = M.get(i, 6);
                row.sub(m);
                row.mul(1 / e);
//                row.sub(mm);
//                row.mul(1 / ee);
                double[] q = new double[3];
                for (int k = 0; k < 3; ++k) {
                    if (M.get(i, k + 1) == 1) {
                        q[k] = Z.get(j, k * 12);
                    } else {
                        q[k] = Double.NaN;
                    }
                }
                DynamicFactorModel.MeasurementDescriptor desc = new DynamicFactorModel.MeasurementDescriptor(
                        measurement((int) M.get(i, 0)), q, MVar.get(j, j));
                dmodel.addMeasurement(desc);
                ++j;
            }
        }
        ddrnd = dd.clone();
        ddrnd.randomize();
        dmodel.setInitialization(VarSpec.Initialization.Unconditional);
    }

    private static DynamicFactorModel.IMeasurement measurement(int i) {
        if (i == 1) {
            return DynamicFactorModel.measurement(MeasurementType.M);
        } else if (i == 2) {
            return DynamicFactorModel.measurement(MeasurementType.Q);
        } else {
            return DynamicFactorModel.measurement(MeasurementType.YoY);
        }
    }

    static {
        loadDavidModel();
    }

    public DynamicFactorModelTest() {
    }

    //Test
    public void testModel() {
        IMSsf ssf = dmodel.ssfRepresentation();
        System.out.println(DefaultTimeInvariantMultivariateSsf.of(ssf));
    }

    @Test
    public void testDavidModel() {
        IMSsf ssf = dmodel.ssfRepresentation();
        Matrix t = new Matrix(ssf.getStateDim(), ssf.getStateDim());
        ssf.T(0, t.subMatrix());
        assertTrue(t.minus(T).nrm2() < 1e-9);
        Matrix tvar = new Matrix(ssf.getStateDim(), ssf.getStateDim());
        ssf.V(0, tvar.subMatrix());
        assertTrue(tvar.minus(TVar).nrm2() < 1e-9);
        Matrix z = new Matrix(ssf.getVarsCount(), ssf.getStateDim());
        ssf.Z(0, z.subMatrix());
        assertTrue(z.minus(Z).nrm2() < 1e-6);
    }

    @Test
    public void testTX() {
        IMSsf ssf = dmodel.ssfRepresentation();
        DefaultTimeInvariantMultivariateSsf defssf = DefaultTimeInvariantMultivariateSsf.of(ssf);
        DataBlock x = new DataBlock(ssf.getStateDim());
        x.randomize();
        DataBlock y = x.clone();
        ssf.TX(0, x);
        defssf.TX(0, y);
        ssf.TX(0, x);
        defssf.TX(0, y);
        assertTrue(x.distance(y) < 1e-9);

        if (stressTest) {
            x.randomize();
            long t0 = System.currentTimeMillis();
            for (int i = 0; i < 100000; ++i) {
                ssf.TX(0, x.deepClone());
            }
            long t1 = System.currentTimeMillis();
            long s0 = System.currentTimeMillis();
            for (int i = 0; i < 100000; ++i) {
                defssf.TX(0, x.deepClone());
            }
            long s1 = System.currentTimeMillis();

            System.out.println("TX");
            System.out.println(s1 - s0);
            System.out.println(t1 - t0);
        }
    }

    @Test
    public void testXT() {
        IMSsf ssf = dmodel.ssfRepresentation();
        DefaultTimeInvariantMultivariateSsf defssf = DefaultTimeInvariantMultivariateSsf.of(ssf);
        DataBlock x = new DataBlock(ssf.getStateDim());
        x.randomize();
        DataBlock y = x.clone();
        ssf.XT(0, x);
        defssf.XT(0, y);
        ssf.XT(0, x);
        defssf.XT(0, y);
        if (stressTest) {
            assertTrue(x.distance(y) < 1e-9);
            x.randomize();
            long t0 = System.currentTimeMillis();
            for (int i = 0; i < 100000; ++i) {
                ssf.XT(0, x.deepClone());
            }
            long t1 = System.currentTimeMillis();
            long s0 = System.currentTimeMillis();
            for (int i = 0; i < 100000; ++i) {
                defssf.XT(0, x.deepClone());
            }
            long s1 = System.currentTimeMillis();
            System.out.println("XT");
            System.out.println(s1 - s0);
            System.out.println(t1 - t0);
        }
    }

    @Test
    public void testZX() {
        IMSsf ssf = dmodel.ssfRepresentation();
        DefaultTimeInvariantMultivariateSsf defssf = DefaultTimeInvariantMultivariateSsf.of(ssf);
        Matrix x = new Matrix(ssf.getStateDim(), ssf.getStateDim());
        x.randomize();
        Matrix y = x.clone();
        Matrix zx = new Matrix(ssf.getVarsCount(), ssf.getStateDim());
        Matrix zy = new Matrix(ssf.getVarsCount(), ssf.getStateDim());

        ssf.ZM(0, x.subMatrix(), zx.subMatrix());
        defssf.ZM(0, y.subMatrix(), zy.subMatrix());
        assertTrue(zx.minus(zy).nrm2() < 1e-9);
        if (stressTest) {
            x.randomize();
            long t0 = System.currentTimeMillis();
            for (int i = 0; i < 10000; ++i) {
                ssf.ZM(0, x.subMatrix(), zx.subMatrix());
            }
            long t1 = System.currentTimeMillis();
            long s0 = System.currentTimeMillis();
            for (int i = 0; i < 10000; ++i) {
                defssf.ZM(0, x.subMatrix(), zx.subMatrix());
            }
            long s1 = System.currentTimeMillis();
            System.out.println("ZX");
            System.out.println(s1 - s0);
            System.out.println(t1 - t0);
        }
    }

    //@Test
    public void testVar2() {
        long q0 = System.currentTimeMillis();
        //for (int k = 0; k < 10; ++k) {
        ArrayFilter filter = new ArrayFilter();
        MPredictionErrorDecomposition results = new MPredictionErrorDecomposition(true);
        filter.process((IArraySsf) dmodel.ssfRepresentation(), new MultivariateSsfData(dd.subMatrix(), null), results);
        //}
        long q1 = System.currentTimeMillis();
        System.out.println("test2");
        //System.out.println(results.getLogDeterminant());
        //System.out.println(results.getSsqErr());
        Likelihood ll = new Likelihood();
        evaluate(results, ll);
        System.out.println(ll.getLogLikelihood());
        System.out.println(q1 - q0);
    }

    //@Test
    public void testVar2bis() {
        long q0 = System.currentTimeMillis();
        //for (int k = 0; k < 10; ++k) {
        MFilter filter = new MFilter();
        MPredictionErrorDecomposition results = new MPredictionErrorDecomposition(true);
        IMSsf ssf = FullMSsf.create(dmodel.ssfRepresentation());
        filter.process(ssf, new MultivariateSsfData(dd.subMatrix(), null), results);
        //}
        long q1 = System.currentTimeMillis();
        System.out.println("test2bis");
        //System.out.println(results.getLogDeterminant());
        //System.out.println(results.getSsqErr());
        Likelihood ll = new Likelihood();
        evaluate(results, ll);
        System.out.println(ll.getLogLikelihood());
        System.out.println(q1 - q0);
    }

    //@Test
    public void testVar3() {
        long q0 = System.currentTimeMillis();
        Likelihood ll = new Likelihood();
        //for (int k = 0; k < 10; ++k) {
        MFilter filter = new MFilter();
        MPredictionErrorDecomposition results = new MPredictionErrorDecomposition(true);
        DynamicFactorModel tmp = dmodel.clone();
        Matrix var = new Matrix(tmp.getFactorsCount() * tmp.getBlockLength(), tmp.getFactorsCount() * tmp.getBlockLength());
        var.diagonal().set(1e7);
        tmp.setInitialCovariance(var);
        filter.process(tmp.ssfRepresentation(), new MultivariateSsfData(dd.subMatrix(), null), results);
        evaluate(results, ll);
        System.out.println("test3");
        System.out.println(results.getLogDeterminant());
        System.out.println(results.getSsqErr());
        System.out.println(ll.getLogLikelihood());
        System.out.println(ll.getSigma());
        System.out.println(tmp);
        DynamicFactorModel md = tmp.clone();
        md.rescaleVariances(ll.getSigma());
        md.lnormalize();
        results = new MPredictionErrorDecomposition(true);
        filter.process(md.ssfRepresentation(), new MultivariateSsfData(dd.subMatrix(), null), results);
        evaluate(results, ll);
        //}
        long q1 = System.currentTimeMillis();
        System.out.println("test3bis");
        System.out.println(results.getLogDeterminant());
        System.out.println(results.getSsqErr());
        System.out.println(ll.getLogLikelihood());
        System.out.println(ll.getSigma());
        System.out.println(md);
        System.out.println(q1 - q0);
    }

    //@Test
    public void testVarU() {
        long q0 = System.currentTimeMillis();
        Likelihood ll = new Likelihood();
        for (int k = 0; k < 10; ++k) {
            Filter filter = new Filter();
            PredictionErrorDecomposition results = new PredictionErrorDecomposition(true);
            filter.setSsf(new M2UAdapter(dmodel.ssfRepresentation(), new FullM2UMap(dmodel.getMeasurementsCount())));
            filter.process(new M2UData(dd, null), results);
            evaluate(results, ll);
        }
        long q1 = System.currentTimeMillis();
        System.out.println("testU");
        //System.out.println(results.getLogDeterminant());
        //System.out.println(results.getSsqErr());
        System.out.println(ll.getLogLikelihood());
        System.out.println(q1 - q0);
    }

//    @Test
    public void testSVar3() {
        long q0 = System.currentTimeMillis();
//        for (int k = 0; k < 10; ++k) {
//        MFilter filter = new MFilter();
//        MFilteringResults results = new MFilteringResults(true);
//        filter.process(model.ssfRepresentation(), new MultivariateSsfData(d, null), results);
        DfmMonitor monitor = new DfmMonitor();
        DfmProcessor processor = new DfmProcessor();
        processor.setCalcVariance(true);
        monitor.setProcessor(processor);
        //           monitor.setCalcVariance(true);
        TsData[] s = new TsData[dd.getRowsCount()];
        for (int i = 0; i < s.length; ++i) {
            s[i] = new TsData(new TsPeriod(TsFrequency.Monthly, 1980, 0), dd.row(i));
        }
        monitor.process(dmodel, s);
//        }
        //        }
        long q1 = System.currentTimeMillis();
        System.out.println("test Smoothing 3");
        System.out.println(q1 - q0);
//        IMSsf ssf = dmodel.ssfRepresentation();
//        Matrix Z = new Matrix(ssf.getVarsCount(), ssf.getStateDim());
//        ssf.Z(0, Z.subMatrix());
        MSmoothingResults sresults = monitor.getSmoothingResults();

        double[] z = sresults.zcomponent(Z.row(25));
        double[] vz = sresults.zvariance(Z.row(25));

        for (int i = 0; i < dd.getColumnsCount(); ++i) {
            System.out.print(z[i]);
            if (vz != null) {
                System.out.print('\t');
                System.out.println(Math.sqrt(vz[i]));
            } else {
                System.out.println();
            }
        }
        q0 = System.currentTimeMillis();
        sresults = new MSmoothingResults();
        sresults.setSaveP(true);
        MSmoother smoother = new MSmoother();
        smoother.setCalcVariance(true);
        IMSsf fssf = FullMSsf.create(dmodel.ssfRepresentation());
        smoother.process(fssf, new MultivariateSsfData(dd.subMatrix(), null), sresults);
        q1 = System.currentTimeMillis();
        System.out.println("test Smoothing 3bis");
        System.out.println(q1 - q0);
//        IMSsf ssf = dmodel.ssfRepresentation();
//        Matrix Z = new Matrix(ssf.getVarsCount(), ssf.getStateDim());
//        ssf.Z(0, Z.subMatrix());
        DataBlock zz = new DataBlock(fssf.getStateDim());
        zz.range(0, Z.getColumnsCount()).copy(Z.row(25));
        zz.set(Z.getColumnsCount() + 25, 1);
        z = sresults.zcomponent(zz);
        vz = sresults.zvariance(zz);

        for (int i = 0; i < dd.getColumnsCount(); ++i) {
            System.out.print(z[i]);
            if (vz != null) {
                System.out.print('\t');
                System.out.println(vz[i] <= 0 ? 0 : Math.sqrt(vz[i]));
            } else {
                System.out.println();
            }
        }
    }

//      @Test
    public void testSVarU() {
        long q0 = System.currentTimeMillis();
        //for (int k = 0; k < 10; ++k) {
//            Filter filter = new Filter();
//            PredictionErrorDecomposition results = new PredictionErrorDecomposition(true);
//            filter.setSsf(new M2UAdapter((IMSsf2U) model.ssfRepresentation(), new FullM2UMap(model.getMeasurementsCount())));
//            filter.process(new M2UData(d, null), results);
        //}
        SmoothingResults sresults = new SmoothingResults();
        Smoother smoother = new Smoother();
        smoother.setSsf(new M2UAdapter(dmodel.ssfRepresentation(), new FullM2UMap(dmodel.getMeasurementsCount())));

        smoother.setCalcVar(false);
        smoother.process(new M2UData(dd, null), sresults);

        DiffuseFilteringResults results = smoother.getFilteringResults();
        long q1 = System.currentTimeMillis();
        Likelihood ll = new Likelihood();
        System.out.println("Test smoothing U");
        evaluate(results, ll);
        System.out.println(ll.getLogLikelihood());
        System.out.println(q1 - q0);
//        double[] component = sresults.component(0);
//        for (int i=0;i<component.length;i+=model.getMeasurementsCount())
//            System.out.println(component[i]);
    }

    @Test
    public void testNews() {
        long q0 = System.currentTimeMillis();
        DfmNews news = new DfmNews(dmodel);
        TsData[] s = new TsData[dd.getRowsCount()];
        TsData[] os = new TsData[dd.getRowsCount()];
        Random rnd = new Random(0);
        TsPeriod start = new TsPeriod(TsFrequency.Monthly, 1980, 0);
        for (int i = 0; i < s.length; ++i) {
            s[i] = new TsData(start, dd.row(i));
            os[i] = s[i].cleanExtremities();
            if (os[i].getEnd().minus(start) > dd.getColumnsCount() - 24) {
                os[i] = os[i].drop(0, rnd.nextInt(12));
            }
        }
        news.process(new TsInformationSet(os), new TsInformationSet(s));
        long q1 = System.currentTimeMillis();
        for (TsInformationUpdates.Update update : news.newsDetails().news()) {
            System.out.println(update);
        }
        DataBlock n = news.news();
        DataBlock w = news.weights(23, s[23].getLastPeriod());
          
        double e = n.dot(w);
        System.out.println("Test news");
        System.out.println(e);
        DfmMonitor monitor = new DfmMonitor();
        monitor.process(dmodel, s);
        double x = news.getOldForecast(23, s[23].getLastPeriod());
        System.out.println(x);
        x = news.getNewForecast(23, s[23].getLastPeriod());
        System.out.println(x);
        System.out.println(n);
        System.out.println(w);
        System.out.println(q1 - q0);
    }

    //@Test
    public void testMapping() {
        DfmMapping mapping = new DfmMapping(dmodel);
        DfmMapping mapping1 = new DfmMapping(dmodel, true, false);
        DfmMapping mapping2 = new DfmMapping(dmodel, false, true);
        DataBlock x = new DataBlock(mapping.map(dmodel.ssfRepresentation()));
        System.out.println(x);
        DynamicFactorModel m = ((DynamicFactorModel.Ssf) mapping1.map(mapping1.map(dmodel.ssfRepresentation()))).getModel();
        x = new DataBlock(mapping.map(m.ssfRepresentation()));
        System.out.println(x);
    }

    //@Test
    public void testMapping2() {
        DynamicFactorModel tmp = dmodel.clone();
        tmp.lnormalize();
        DfmMapping2 mapping = new DfmMapping2(tmp);
        DfmMapping2 mapping1 = new DfmMapping2(tmp, true, false);
        DfmMapping2 mapping2 = new DfmMapping2(tmp, false, true);
        DataBlock x = new DataBlock(mapping.map(tmp.ssfRepresentation()));
        System.out.println("Mapping 2");
        System.out.println(x);
        DynamicFactorModel m = ((DynamicFactorModel.Ssf) mapping.map(mapping.map(tmp.ssfRepresentation()))).getModel();
        x = new DataBlock(mapping.map(m.ssfRepresentation()));
        System.out.println(x);
    }

    //@Test
    public void testMeasurements() {
        HashSet<MeasurementStructure> set = new HashSet<>();
        for (MeasurementDescriptor mdesc : dmodel.getMeasurements()) {
            set.add(mdesc.getStructure());
        }
        for (MeasurementStructure s : set) {
            System.out.println(s);
        }
    }

}
