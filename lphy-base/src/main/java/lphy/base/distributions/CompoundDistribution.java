package lphy.base.distributions;

import lphy.core.model.components.GenerativeDistribution;
import lphy.core.model.components.RandomVariable;
import lphy.core.model.components.Value;
import lphy.core.vectorization.RangeElement;

import java.util.Map;

public class CompoundDistribution<T> implements GenerativeDistribution<T[]> {

    public CompoundDistribution(GenerativeDistribution generativeDistribution, RangeElement element) {

    }

    public void add(RandomVariable variable, RangeElement element) {
        
    }

    @Override
    public RandomVariable<T[]> sample() {
        return null;
    }

    @Override
    public Map<String, Value> getParams() {
        return null;
    }
}
