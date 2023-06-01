package lphy.base.functions.tree;

import lphy.base.evolution.tree.TimeTree;
import lphy.core.model.components.*;
import lphy.core.model.types.IntegerValue;

/**
 * use {@link TimeTree#nodeCount()}
 */
@Deprecated
public class NodeCount extends DeterministicFunction<Integer> {

    final String paramName;

    public NodeCount(@ParameterInfo(name = "tree", description = "the tree.") Value<TimeTree> x) {
        paramName = getParamName(0);
        setParam(paramName, x);
    }

    @GeneratorInfo(name="nodecount",
            category = GeneratorCategory.TREE, examples = {"yuleRelaxed.lphy"},
            description = "The number of nodes in the tree")
    public Value<Integer> apply() {
        Value<TimeTree> v = (Value<TimeTree>)getParams().get(paramName);
        return new IntegerValue(v.value().getNodeCount(), this);
    }
}
