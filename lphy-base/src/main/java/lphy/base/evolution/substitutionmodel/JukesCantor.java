package lphy.base.evolution.substitutionmodel;

import lphy.core.model.annotation.Citation;
import lphy.core.model.annotation.GeneratorInfo;
import lphy.core.model.component.GeneratorCategory;
import lphy.core.model.component.Value;
import lphy.core.model.component.ValueUtils;
import lphy.core.model.component.argument.ParameterInfo;
import lphy.core.model.datatype.DoubleArray2DValue;

/**
 * JC69: AC = AG = AT = CG = CT = GT, equal base frequencies, k free parameters
 * @author Alexei Drummond
 */
@Citation(value="Jukes, T. H., & Cantor, C. R. (1969). Evolution of protein molecules. " +
        "Mammalian protein metabolism, 3, 21-132.",
        title = "Evolution of protein molecules",
        year=1969,
        authors={"Jukes", "Cantor"},
        DOI="https://doi.org/10.1016/B978-1-4832-3211-9.50009-7")
public class JukesCantor extends RateMatrix {

    public JukesCantor(@ParameterInfo(name = meanRateParamName, description = "the rate of the Jukes-Cantor process. Default value is 1.0.", optional = true) Value<Number> rate) {
        super(rate);
    }

    @GeneratorInfo(name = "jukesCantor", verbClause = "is", narrativeName = "Jukes-Cantor model",
            category = GeneratorCategory.RATE_MATRIX, examples = {"jcCoalescent.lphy"},
            description = "The Jukes-Cantor Q matrix construction function. Takes a mean rate and produces a Jukes-Cantor Q matrix.")
    public Value<Double[][]> apply() {
        Value<Number> rateValue = getParams().get(meanRateParamName);
        double rate = (rateValue != null) ? ValueUtils.doubleValue(rateValue) : 1.0;
        return new DoubleArray2DValue(LewisMK.jc(rate, 4), this);
    }
}
