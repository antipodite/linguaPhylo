package lphy.evolution.substitutionmodel;

import lphy.graphicalModel.*;
import lphy.graphicalModel.types.DoubleArray2DValue;

import java.util.Map;

/**
 * Created by Alexei Drummond on 2/02/20.
 */
public class K80 extends RateMatrix {

    public static final String kappaParamName = "kappa";

    public K80(@ParameterInfo(name = kappaParamName, description = "the kappa of the K80 process.") Value<Double> kappa,
               @ParameterInfo(name = meanRateParamName, description = "the mean rate of the K80 process. default 1.0", optional = true) Value<Number> meanRate) {

        super(meanRate);
        setParam(kappaParamName, kappa);
    }


    @GeneratorInfo(name = "k80", description = "The K80 instantaneous rate matrix. Takes a kappa and produces a K80 rate matrix.")
    public Value<Double[][]> apply() {
        Value<Double> kappa = getKappa();
        return new DoubleArray2DValue(k80(kappa.value()), this);
    }

    private Double[][] k80(double kappa) {

        int numStates = 4;

        Double[][] Q = new Double[numStates][numStates];

        double[] totalRates = new double[numStates];

        for (int i = 0; i < numStates; i++) {
            for (int j = 0; j < numStates; j++) {
                if (i != j) {
                    if (Math.abs(i-j) == 2) {
                        Q[i][j] = kappa;
                    } else {
                        Q[i][j] = 1.0;
                    }
                }
                totalRates[i] += Q[i][j];
            }
            Q[i][i] = -totalRates[i];
        }

        normalize(new Double[] {0.25, 0.25, 0.25, 0.25}, Q, totalRateDefault1());

        return Q;
    }

    public Value<Double> getKappa() {
        return getParams().get(kappaParamName);
    }

}
