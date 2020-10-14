package lphy.evolution.tree;

import lphy.graphicalModel.*;
import lphy.utils.LoggerUtils;

import java.util.*;

import static lphy.evolution.EvolutionConstants.treeParamName;
import static lphy.evolution.tree.TimeTreeUtils.*;

/**
 * A function to return a tree pruned from a larger tree by retaining only the tips at time zero
 */
public class ExtantTree extends DeterministicFunction<TimeTree> {

    public ExtantTree(@ParameterInfo(name = treeParamName, description = "the full tree to extract extant tree from.") Value<TimeTree> tree) {
        setParam(treeParamName, tree);
    }

    @GeneratorInfo(name = "extantTree", description = "A tree pruned from a larger tree by retaining only the tips at time zero.")
    public Value<TimeTree> apply() {

        Value<TimeTree> tree = getParams().get(treeParamName);

        // do deep copy
        TimeTree extantTree = new TimeTree(tree.value());

        List<TimeTreeNode> sampleTips = new ArrayList<>();

        while (sampleTips.size() == 0) {
            for (TimeTreeNode node : extantTree.getNodes()) {
                if (node.isLeaf() && node.getAge() == 0.0) {
                    sampleTips.add(node);
                }
            }
        }
        LoggerUtils.log.info("Extant tree has " + sampleTips.size() + " tips.");

        for (TimeTreeNode tip : sampleTips) {
            markNodeAndDirectAncestors(tip);
        }

        removeUnmarkedNodes(extantTree.getRoot());

        TimeTreeNode newRoot = getFirstNonSingleChildNode(extantTree.getRoot());
        if (!newRoot.isRoot()) {
            newRoot.getParent().removeChild(newRoot);
        }

        removeSingleChildNodes(newRoot);

        extantTree.setRoot(newRoot, true);

        return new Value<>(null, extantTree, this);
    }
}
