package lphy.core.vectorization.array;

import lphy.core.model.DeterministicFunction;
import lphy.core.model.Value;
import lphy.core.model.annotation.GeneratorInfo;
import lphy.core.model.datatype.NumberArrayValue;

public class NumberArray extends DeterministicFunction<Number[]> {

    Value<Number>[] x;

    public NumberArray(Value<Number>... x) {

        int length = x.length;
        this.x = x;

        for (int i = 0; i < length; i++) {
            setInput(i + "", x[i]);
        }
    }

    @GeneratorInfo(name = "numberArray", description = "The constructor function for an array of numbers.")
    public Value<Number[]> apply() {

        Number[] values = new Number[x.length];

        for (int i = 0; i < x.length; i++) {
            values[i] = x[i].value();
        }

        return new NumberArrayValue(null, values, this);
    }

    public void setParam(String param, Value value) {
        super.setParam(param, value);
        int i = Integer.parseInt(param);
        x[i] = value;
    }

    public String codeString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        builder.append(ref(x[0]));
        for (int i = 1; i < x.length; i++) {
            builder.append(", ");
            builder.append(ref(x[i]));
        }
        builder.append("]");
        return builder.toString();
    }

    private String ref(Value<Number> val) {
        if (val.isAnonymous()) return val.codeString();
        return val.getId();
    }
}
