package lphy.core.distributions;

import lphy.core.ParameterNames;
import lphy.graphicalModel.*;
import lphy.util.RandomUtils;

import java.util.*;

public class Sample<T> implements GenerativeDistribution<T[]> {

    private final String xParamName = "arr";
    private final String replParamName = "replace";

    private Value<T[]> x;
    private Value<Integer> size;
    private Value<Boolean> replace;

    Random random;

    public Sample(@ParameterInfo(name = xParamName, description = "1d-array to be sampled.") Value<T[]> x,
                  @ParameterInfo(name = ParameterNames.SizeParamName,
                          description = "the number of elements to choose.") Value<Integer> size,
                  @ParameterInfo(name = replParamName, description = "If replace is true, " +
                          "the same element can be sampled multiple times, if false (as default), " +
                          "it can only appear once in the result.",
                          optional = true) Value<Boolean> replace) {

        this.x = x;
        if (x == null) throw new IllegalArgumentException("The array can't be null!");
        T[] value = x.value();
        if (value == null || value.length < 1)
            throw new IllegalArgumentException("Must have at least 1 element in the array!");
        this.size = size;
        if (size == null) throw new IllegalArgumentException("The size can't be null!");
        if (size.value() <= 0 || size.value() > value.length)
            throw new IllegalArgumentException("Invalid size : " + size.value());
        if (replace == null)
            replace = new Value<>(null, false);
        this.replace = replace;

        random = RandomUtils.getJavaRandom();
    }

    @GeneratorInfo(name = "sample", description = "The sample function uniformly sample the subset of " +
            "a given size from an array of the elements either with or without the replacement.")
    public RandomVariable<T[]> sample() {
        List<T> origArr = new ArrayList<>(List.of(x.value()));
        int s = size.value();
        // use List to handle generic
        List<T> list2Arr;
        if (replace.value()) {
            list2Arr = new ArrayList<>();
            int randomIndex;
            for (int i = 0; i < s; i++) {
                randomIndex = random.nextInt(origArr.size());
                list2Arr.add( origArr.get(randomIndex) );
            }
        } else { // no replacement
            Collections.shuffle(origArr, random);
            list2Arr = origArr.stream().limit(s).toList();
        }

        return VariableUtils.createRandomVariable( null, list2Arr, this);
    }

    public Map<String, Value> getParams() {
        return new TreeMap<>() {{
            put(xParamName, x);
            put(ParameterNames.SizeParamName, size);
            put(replParamName, replace);
        }};
    }
}