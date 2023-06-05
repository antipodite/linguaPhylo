package lphy.io.mcmc;

import lphy.core.model.component.RandomVariable;

import java.util.List;

public interface Operator<T> {

    /**
     *
     * @return Green-Hastings ratio
     */
    double operate();

    List<RandomVariable<T>> getVariables();
}
