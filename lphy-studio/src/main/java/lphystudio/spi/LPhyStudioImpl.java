package lphystudio.spi;

import lphy.core.model.components.Func;
import lphy.core.model.components.GenerativeDistribution;
import lphy.core.spi.LPhyExtension;

import java.util.ArrayList;
import java.util.List;

/**
 * Empty class to show studio ext in the Extension Manager.
 * @author Walter Xie
 */
public class LPhyStudioImpl implements LPhyExtension {

    /**
     * Required by ServiceLoader.
     */
    public LPhyStudioImpl() {
    }

    @Override
    public List<Class<? extends GenerativeDistribution>> getDistributions() {
        return new ArrayList<>();
    }

    @Override
    public List<Class<? extends Func>> getFunctions() {
        return new ArrayList<>();
    }

}
