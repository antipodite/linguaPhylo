package lphy.core.functions;

import lphy.graphicalModel.DeterministicFunction;
import lphy.graphicalModel.GeneratorInfo;
import lphy.graphicalModel.Value;
import lphy.graphicalModel.types.IntegerArrayValue;

public class IntegerArray extends DeterministicFunction<Integer[]> {

    Value<Integer>[] x;

    public IntegerArray(Value<Integer>... x) {

        int length = x.length;
        this.x = x;

        for (int i = 0; i < length; i++) {
            setParam(i + "", x[i]);
        }
    }

    @GeneratorInfo(name = "integerArray", description = "The constructor function for an array of integers.")
    public Value<Integer[]> apply() {

        Integer[] values = new Integer[x.length];

        for (int i = 0; i < x.length; i++) {
            values[i] = x[i].value();
        }

        return new IntegerArrayValue(null, values, this);
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

    private String ref(Value<?> val) {
        if (val.isAnonymous()) return val.codeString();
        return val.getId();
    }
}
