package lphy.evolution.alignment;

import jebl.evolution.sequences.SequenceType;
import lphy.graphicalModel.*;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author Walter Xie
 */
public class FilterMissingSites implements GenerativeDistribution<Alignment> {

    Value<Double> thresholdDecimal;
    Value<Alignment> originalAlignment;
    SequenceType sequenceType;
    public final String thresholdParamName = "threshold";
    public final String alignmentParamName = "alignment";


    public FilterMissingSites(@ParameterInfo(name = thresholdParamName, description = "the threshold (decimal form) to remove a site, if the proportion of missing data in this site is greater than the threshold.") Value<Double> thresholdDecimal,
                              @ParameterInfo(name = alignmentParamName, description = "the alignment without missing sites.") Value<Alignment> originalAlignment) {
        this.thresholdDecimal = thresholdDecimal;
        if (thresholdDecimal.value() >= 1)
            throw new IllegalArgumentException("Threshold must < 1 : " + thresholdDecimal.value());
        this.originalAlignment = originalAlignment;
        Alignment origAlg = originalAlignment.value();
        if (origAlg == null)
            throw new IllegalArgumentException("Cannot find Alignment ! " + originalAlignment.getId());
        sequenceType = origAlg.getSequenceType();
    }

    @Override
    public SortedMap<String, Value> getParams() {
        SortedMap<String, Value> map = new TreeMap<>();
        map.put(thresholdParamName, thresholdDecimal);
        map.put(alignmentParamName, originalAlignment);
        return map;
    }


    @GeneratorInfo(name = "FilterMissingSites",
            category = GeneratorCategory.TAXA_ALIGNMENT,
            description = "To filter out the sites having missing data > the threshold in a decimal form (e.g. 0.5).")
    public RandomVariable<Alignment> sample() {

        Alignment original = originalAlignment.value();

        List<int[]> newSites = new ArrayList<>();

        int[] aSite = new int[original.ntaxa()];
        for (int j = 0; j < original.nchar(); j++) {
            for (int i = 0; i < original.ntaxa(); i++) {
                aSite[i] = original.getState(i, j);
            }
            // filter
            if (isFiltered(aSite, thresholdDecimal.value()))
                newSites.add(aSite);
        }

        // have to know nchar before create a new alignment
        Alignment newAlignment = new SimpleAlignment(newSites.size(), original);
        for (int j = 0; j < newSites.size(); j++) {
            aSite = newSites.get(j);
            for (int i = 0; i < original.ntaxa(); i++) {
                newAlignment.setState(i, j, aSite[i]);
            }
        }

        return new RandomVariable<>("D", newAlignment, this);
    }

    private boolean isFiltered(int[] aSite, double threshold) {
        double missing = 0.0;
        for (int state : aSite) {
            if ( state == sequenceType.getUnknownState().getIndex() )
                missing++;
        }
        return missing/aSite.length > threshold;
    }
}
