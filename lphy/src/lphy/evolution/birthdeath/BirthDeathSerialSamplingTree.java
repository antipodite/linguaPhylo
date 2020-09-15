package lphy.evolution.birthdeath;

import lphy.core.distributions.Utils;
import lphy.evolution.TaxaAges;
import lphy.evolution.tree.TaxaConditionedTreeGenerator;
import lphy.evolution.tree.TimeTree;
import lphy.evolution.tree.TimeTreeNode;
import lphy.graphicalModel.GeneratorInfo;
import lphy.graphicalModel.ParameterInfo;
import lphy.graphicalModel.RandomVariable;
import lphy.graphicalModel.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;

import static lphy.graphicalModel.ValueUtils.doubleValue;

/**
 * A Birth-death tree generative distribution
 */
public class BirthDeathSerialSamplingTree extends TaxaConditionedTreeGenerator {

    final String birthRateParamName;
    final String deathRateParamName;
    final String rhoParamName;
    final String psiParamName;
    final String rootAgeParamName;
    final String agesParamName;
    private Value<Number> birthRate;
    private Value<Number> deathRate;
    private Value<Number> psiVal;
    private Value<Number> rhoVal;
    private Value<Number> rootAge;
    private Value<Double[]> ages;

    private double c1;
    private double c2;
    private double gt;

    private double lambda;
    private double mu;
    private double rho;
    private double psi;
    private double tmrca;

    public BirthDeathSerialSamplingTree(@ParameterInfo(name = "lambda", description = "per-lineage birth rate.") Value<Number> birthRate,
                                        @ParameterInfo(name = "mu", description = "per-lineage death rate.") Value<Number> deathRate,
                                        @ParameterInfo(name = "rho", description = "proportion of extant taxa sampled.") Value<Number> rhoVal,
                                        @ParameterInfo(name = "psi", description = "per-lineage sampling-through-time rate.") Value<Number> psiVal,
                                        @ParameterInfo(name = "n", description = "the number of taxa. optional.", optional = true) Value<Integer> n,
                                        @ParameterInfo(name = "taxaAges", description = "TaxaAges object, (e.g. TaxaAges or TimeTree)", optional = true) Value<TaxaAges> taxaAges,
                                        @ParameterInfo(name = "ages", description = "an array of leaf node ages.", optional = true) Value<Double[]> ages,
                                        @ParameterInfo(name = "rootAge", description = "the age of the root.") Value<Number> rootAge) {

        super(n, taxaAges);

        this.birthRate = birthRate;
        this.deathRate = deathRate;
        this.rhoVal = rhoVal;
        this.psiVal = psiVal;
        this.rootAge = rootAge;
        this.ages = ages;
        this.random = Utils.getRandom();

        birthRateParamName = getParamName(0);
        deathRateParamName = getParamName(1);
        rhoParamName = getParamName(2);
        psiParamName = getParamName(3);
        nParamName = getParamName(4);
        taxaParamName = getParamName(5);
        agesParamName = getParamName(6);
        rootAgeParamName = getParamName(7);

        checkTaxaParameters(false);
    }

    @GeneratorInfo(name = "BirthDeathSerialSampling", description = "A tree of extant species and those sampled through time, which is conceptually embedded in a full species tree produced by a speciation-extinction (birth-death) branching process.<br>" +
            "Conditioned on root age and on number of taxa and their ages (Stadler and Yang, 2013).")
    public RandomVariable<TimeTree> sample() {

        lambda = doubleValue(birthRate);
        mu = doubleValue(deathRate);
        rho = doubleValue(rhoVal);
        psi = doubleValue(psiVal);
        tmrca = doubleValue(rootAge);

        // calculate the constants in the simulating functions
        c1 = Math.sqrt(Math.pow(lambda - mu - psi, 2) + 4 * lambda * psi);
        c2 = -(lambda - mu - 2 * lambda * rho - psi) / c1;
        gt = 1 / (Math.exp(-c1 * tmrca) * (1 - c2) + (1 + c2));

        TimeTree tree = randomTreeTopology();
        tree.getRoot().setAge(tmrca);
        drawDivTimes(tree);

        //repositionNodeWhenInvalid(tree);
        reconstructTree(tree);

        return new RandomVariable<>("\u03C8", tree, this);
    }

    private TimeTree randomTreeTopology() {
        TimeTree tree = new TimeTree();
        List<TimeTreeNode> activeNodes = createLeafTaxa(tree);

        while (activeNodes.size() > 1) {
            TimeTreeNode a = drawRandomNode(activeNodes);
            TimeTreeNode b = drawRandomNode(activeNodes);
            TimeTreeNode parent = new TimeTreeNode(Math.max(a.getAge(), b.getAge()), new TimeTreeNode[]{a, b});
            activeNodes.add(parent);
        }

        tree.setRoot(activeNodes.get(0));
        return tree;
    }

    private List<TimeTreeNode> createLeafTaxa(TimeTree tree) {
        List<TimeTreeNode> leafNodes = new ArrayList<>();

        if (ages != null) {

            Double[] leafAges = ages.value();

            for (int i = 0; i < leafAges.length; i++) {
                TimeTreeNode node = new TimeTreeNode(i + "", tree);
                node.setAge(leafAges[i]);
                leafNodes.add(node);
            }
            return leafNodes;

        } else if (taxa != null) {

            TaxaAges taxaAges = (TaxaAges)taxa.value();
            String[] taxaNames = taxaAges.getTaxa();
            Double[] ages = taxaAges.getAges();

            for (int i = 0; i < taxaNames.length; i++) {
                TimeTreeNode node = new TimeTreeNode(taxaNames[i], tree);
                node.setAge(ages[i]);
                leafNodes.add(node);
            }
            return leafNodes;

        } else {
            for (int i = 0; i < n(); i++) {
                TimeTreeNode node = new TimeTreeNode(i + "", tree);
                node.setAge(0.0);
                leafNodes.add(node);
            }
            return leafNodes;
        }
    }

    /*
     * This method traverses the tree from left to right (inorder)
     * and returns the order of index for internal node
     */
    private int traverseTree(TimeTreeNode node, int i, int[] index) {
        if (!node.isLeaf()) {
            i = traverseTree(node.getChild(0), i, index);
            index[i] = node.getIndex();
            i += 1;
            i = traverseTree(node.getChild(1), i, index);
        }
        return i;
    }


    private void drawDivTimes(TimeTree tree) {
        // index of leaf nodes
        int k;
        int[] index = new int[tree.ntaxa() - 1];
        traverseTree(tree.getRoot(), 0, index);

        // iterate internal nodes except the root
        for (int j : index) {
            if (j != tree.getRoot().getIndex()) {
                // step1: get z^* in Equation (4) in Stadler and Yang 2013
                // find tip on the left
                for (k = tree.getNodeByIndex(j).getChild(1).getIndex(); k >= tree.ntaxa(); k = tree.getNodeByIndex(k).getChild(0).getIndex())
                    ;
                double z0 = tree.getNodeByIndex(k).getAge();
                // find tip on the right
                for (k = tree.getNodeByIndex(j).getChild(0).getIndex(); k >= tree.ntaxa(); k = tree.getNodeByIndex(k).getChild(1).getIndex())
                    ;
                double z1 = tree.getNodeByIndex(k).getAge();
                double zstar = Math.max(z0, z1);

                // step2
                // calculate 1/g(z*)
                double gzstar = 1 / (Math.exp(-c1 * zstar) * (1 - c2) + (1 + c2));
                // a2 = (1/g(t_mrca)) - (1/g(z^*))
                double a2 = gt - gzstar;


                // step4
                // the constant part in the integral, which is H(zstar) and H is CDF of divergence times
                double constantChildren = 1.0 / (a2 * (((1 - c2) * Math.exp(-c1 * zstar)) + (1 + c2)));

                // step5: drawn a random number of Uniform(0,1)
                double y = random.nextDouble();

                // calculate the inverse function, i.e. H^(-1)
                double x;
                x = Math.log((1 / (a2 * (y + constantChildren) * (1 - c2))) - ((1 + c2) / (1 - c2))) / (-c1);

                // set the simulated divergence time
                tree.getNodeByIndex(j).setAge(x);
            }
        }
    }

    private void reconstructTree(TimeTree tree) {

        List<TimeTreeNode> nodes = tree.getNodes();
        // collect heights
        final double[] heights = new double[nodes.size()];
        final int[] reverseOrder = new int[nodes.size()];
        collectHeights(tree.getRoot(), heights, reverseOrder, 0);

        System.out.println("Heights = " + Arrays.toString(heights));
        System.out.println("reverseOrder = " + Arrays.toString(reverseOrder));

        TimeTreeNode root = reconstructTree(nodes, heights, reverseOrder, 0, heights.length, new boolean[heights.length]);
        tree.setRoot(root);
    }

    private TimeTreeNode reconstructTree(List<TimeTreeNode> nodes, final double[] heights, final int[] reverseOrder, final int from, final int to, final boolean[] hasParent) {
        //nodeIndex = maxIndex(heights, 0, heights.length);
        int nodeIndex = -1;
        double max = Double.NEGATIVE_INFINITY;
        for (int j = from; j < to; j++) {
            if (max < heights[j] && !nodes.get(reverseOrder[j]).isLeaf()) {
                max = heights[j];
                nodeIndex = j;
            }
        }
        if (nodeIndex < 0) {
            return null;
        }
        final TimeTreeNode node = nodes.get(reverseOrder[nodeIndex]);

        //int left = maxIndex(heights, 0, nodeIndex);
        int left = -1;
        max = Double.NEGATIVE_INFINITY;
        for (int j = from; j < nodeIndex; j++) {
            if (max < heights[j] && !hasParent[j]) {
                max = heights[j];
                left = j;
            }
        }

        //int right = maxIndex(heights, nodeIndex+1, heights.length);
        int right = -1;
        max = Double.NEGATIVE_INFINITY;
        for (int j = nodeIndex + 1; j < to; j++) {
            if (max < heights[j] && !hasParent[j]) {
                max = heights[j];
                right = j;
            }
        }

        node.setLeft(nodes.get(reverseOrder[left]));
        node.getLeft().setParent(node);
        node.setRight(nodes.get(reverseOrder[right]));
        node.getRight().setParent(node);
        if (node.getLeft().isLeaf()) {
            heights[left] = Double.NEGATIVE_INFINITY;
        }
        if (node.getRight().isLeaf()) {
            heights[right] = Double.NEGATIVE_INFINITY;
        }
        hasParent[left] = true;
        hasParent[right] = true;
        heights[nodeIndex] = Double.NEGATIVE_INFINITY;


        reconstructTree(nodes, heights, reverseOrder, from, nodeIndex, hasParent);
        reconstructTree(nodes, heights, reverseOrder, nodeIndex, to, hasParent);
        return node;
    }

    private int collectHeights(final TimeTreeNode node, final double[] heights, final int[] reverseOrder, int current) {

        if (node.isLeaf()) {
            heights[current] = node.getAge();
            reverseOrder[current] = node.getIndex();
            current++;
        } else {
            current = collectHeights(node.getLeft(), heights, reverseOrder, current);
            heights[current] = node.getAge();
            reverseOrder[current] = node.getIndex();
            current++;
            current = collectHeights(node.getRight(), heights, reverseOrder, current);
        }
        return current;
    }

    /*
     * modify the simulated tree when an invalid node time appears
     * this method is called after drawDivTimes()
     */
    private void repositionNodeWhenInvalid(TimeTree tree){
        int[] index = new int[tree.ntaxa()-1];
        traverseTree(tree.getRoot(), 0, index);

        for(int nodeIdx : index) {
            TimeTreeNode node = tree.getNodeByIndex(nodeIdx);
            if(!node.isRoot() && !node.getParent().isRoot()) {
                TimeTreeNode parent = node.getParent();

                double tc = node.getAge();
                double tp = node.getParent().getAge();

                if (tc >= tp) {
                    if (node == parent.getLeft()) {
                        exchangeLeftChild(parent, node);
                    } else {
                        exchangeRightChild(parent, node);
                    }
                }
            }
        }
    }
    // This method applies to an internal node (Node child) is the LEFT child of its parent (Node parent)
    // child: aNode
    // parent: parent of aNode
    private void exchangeLeftChild(TimeTreeNode parent, TimeTreeNode child){
        // exchange heights so than parent is older than child
        double tc = child.getAge();
        double tp = parent.getAge();
        child.setAge(tp);
        parent.setAge(tc);

        TimeTreeNode leftGrandChild = child.getChild(0); // gc1
        TimeTreeNode rightGrandChild = child.getChild(1); // gc2
        TimeTreeNode sibling = parent.getChild(1); // right child of parent, sibling of aNde (child)

        // operator on the tree topology
        replace(parent, child, leftGrandChild);
        replace(parent, sibling, child);

        // operator on the order of involved nodes
        replace(child, rightGrandChild, sibling);
        replace(child, leftGrandChild, rightGrandChild);
        exchangeNodes(child.getChild(0), child.getChild(1),child,child);

        // make sure the node times are valid after the previous operations
        if (child.getAge() <= child.getLeft().getAge()){
            exchangeLeftChild(child, child.getLeft());
        }

        if (child.getAge() <= child.getRight().getAge()){
            exchangeRightChild(child, child.getRight());
        }

        if (parent.getAge() >= parent.getParent().getAge()){
            if(parent == parent.getParent().getChild(0)) {
                exchangeLeftChild(parent.getParent(), parent);
            } else {
                exchangeRightChild(parent.getParent(), parent);
            }
        }

    }

    // This method applies to an internal node (Node child) is the RIGHT child of its parent (Node parent)
    private void exchangeRightChild(TimeTreeNode parent, TimeTreeNode child) {
        // exchange heights so than parent is older than child
        double tc = child.getAge();
        double tp = parent.getAge();
        child.setAge(tp);
        parent.setAge(tc);


        TimeTreeNode leftGrandChild = child.getChild(0); // gc1
        TimeTreeNode rightGrandChild = child.getChild(1); // gc2
        TimeTreeNode sibling = parent.getChild(0); // left child of parent, sibling of aNde (child)

        // operate on topology
        replace(parent, child, rightGrandChild);
        replace(parent, sibling, child);

        // operator on node order
        replace(child, leftGrandChild, sibling);
        replace(child, rightGrandChild, leftGrandChild);
        exchangeNodes(parent.getChild(0), parent.getChild(1), parent, parent);

        // make sure the node times are valid after the previous operations
        if (child.getAge() <= child.getLeft().getAge()){
            exchangeLeftChild(child, child.getLeft());
        }

        if (child.getAge() <= child.getRight().getAge()){
            exchangeRightChild(child, child.getRight());
        }

        if (parent.getAge() >= parent.getParent().getAge()){
            if(parent == parent.getParent().getChild(0)) {
                exchangeLeftChild(parent.getParent(), parent);
            } else {
                exchangeRightChild(parent.getParent(), parent);
            }
        }
    }

    // this method exchanges node i and node j
    // ip: parent of i
    // jp: parent of j
    private void exchangeNodes(TimeTreeNode i, TimeTreeNode j, TimeTreeNode ip, TimeTreeNode jP) {
        replace(ip, i, j);
        replace(jP, j, i);
    }

    /*
     * this method is taken from TreeOperator
     */
    private void replace(final TimeTreeNode node, final TimeTreeNode child, final TimeTreeNode replacement) {
        node.removeChild(child);
        node.addChild(replacement);
    }

    @Override
    public double logDensity(TimeTree timeTree) {
        throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public SortedMap<String, Value> getParams() {
        SortedMap<String, Value> map = super.getParams();
        map.put(birthRateParamName, birthRate);
        map.put(deathRateParamName, deathRate);
        map.put(rhoParamName, rhoVal);
        map.put(psiParamName, psiVal);
        if (ages != null) map.put(agesParamName, ages);
        map.put(rootAgeParamName, rootAge);
        return map;
    }

    @Override
    public void setParam(String paramName, Value value) {
        if (paramName.equals(birthRateParamName)) birthRate = value;
        else if (paramName.equals(deathRateParamName)) deathRate = value;
        else if (paramName.equals(rhoParamName)) rhoVal = value;
        else if (paramName.equals(psiParamName)) psiVal = value;
        else if (paramName.equals(agesParamName)) ages = value;
        else if (paramName.equals(rootAgeParamName)) rootAge = value;
        else super.setParam(paramName, value);
    }
}
