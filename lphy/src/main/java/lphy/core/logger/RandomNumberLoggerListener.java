package lphy.core.logger;

import lphy.core.model.Value;
import lphy.core.model.ValueUtils;
import lphy.core.simulator.NamedRandomValueSimulator;
import lphy.core.simulator.SimulatorListener;
import lphy.core.spi.LoaderManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Particularly used to log the number values of a named random variable,
 * boolean is converted to 1.0 or 0.0.
 */
public class RandomNumberLoggerListener implements SimulatorListener {

    private static final ValueFormatResolver valueFormatResolver = LoaderManager.valueFormatResolver;

    public static final String COL_NAME_OF_INDEX = "Sample";

    /**
     * The key represents the value id, if value is array, then id will be appended by index of the element.
     * The value of map is the list of formatted value in string at that id.
     */
    private Map<String, List<Double>> formattedValuesById = new HashMap<>();

    private List<String> headers = new ArrayList<>();
    //TODO no footer?
//    public List<String> footers = new ArrayList<>();

    /**
     * the list of row names, size = sampleCount.
     */
    private List<String> rowNames = new ArrayList<>();

    int sampleCount;

    public RandomNumberLoggerListener() {

    }

    @Override
    public void start(Object... configs) {

    }

    @Override
    public void replicate(int index, List<Value> values) {
        if (index == 0) {
            sampleCount = 0;
            rowNames.clear();
            headers.clear();
            formattedValuesById.clear();
        }

        boolean isFirstValue = true;
        for (Value value : values) {

            if (isNamedRandomNumber(value)) {
                List<ValueFormatter> formatters = valueFormatResolver.getFormatter(value);

                // if it is array, then one ValueFormatter for one element
                for (int j = 0; j < formatters.size(); j++) {
                    ValueFormatter f = formatters.get(j);

                    // this covers f != null
                    if (f instanceof ValueFormatter.Base formatter) {
                        // If value is array, the id will be appended with index
                        String id = formatter.getValueID();

                        if (index == 0) {
                            String header = formatter.header();
                            headers.add(header);
                        }

                        // row names
                        String rowName = formatter.getRowName(index);
                        if (isFirstValue) {
                            rowNames.add(rowName);
                            isFirstValue = false;
                        }

                        // formatted value in string
                        List<Double> formattedValues = formattedValuesById
                                .computeIfAbsent(id, k -> new ArrayList<>());
                        // here require the original value if value is array,
                        // but return the formatted string at ith element
                        String body = formatter.format(value.value());

                        // TODO not sure if this can be improved
                        Double num;
                        try {
                            num = Double.valueOf(body);
                        } catch(NumberFormatException e) {
                            //not a double
                           if ("true".equalsIgnoreCase(body))
                               num = 1.0;
                           else if ("false".equalsIgnoreCase(body))
                               num = 0.0;
                           else
                               throw new RuntimeException("Number is required, but " + body);
                        }
                        formattedValues.add(num);

                    } // end if
                } // end for j

            } // end if isNamedRandomNumber
        }
        sampleCount = index + 1;
        if (sampleCount != rowNames.size())
            throw new IllegalArgumentException("Row names " + rowNames.size() +
                    " must be same to the sample count " + sampleCount + " !");
    }

    @Override
    public void complete() {

    }

    public boolean isNamedRandomNumber(Value randomValue) {
        boolean random = NamedRandomValueSimulator.isNamedRandomValue(randomValue);
        boolean number = ValueUtils.isNumberOrNumberArray(randomValue) ||
                ValueUtils.is2DNumberArray(randomValue) ||
                // 0|1
                randomValue.value() instanceof Boolean;

        return random && number && !randomValue.isAnonymous();
    }

    public ValueFormatter.Mode getMode() {
        return ValueFormatter.Mode.VALUE_PER_CELL;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public Map<String, List<Double>> getFormattedValuesById() {
        return formattedValuesById;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public List<String> getRowNames() {
        return rowNames;
    }
}