package lphy.graphicalModel;


import lphy.graphicalModel.types.StringArrayValue;
import lphy.graphicalModel.types.StringValue;

import javax.swing.*;
import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public abstract class Func implements Generator, Viewable {

    private String name = null;
    private String description = null;

    public String getName() {
        if (name == null) {
            GeneratorInfo fInfo = Generator.getGeneratorInfo(getClass());
            if (fInfo != null) {
                name = fInfo.name();
            } else name = getClass().getSimpleName();
        }
        return name;
    }

    public String getDescription() {
        if (description == null) {
            GeneratorInfo fInfo = Generator.getGeneratorInfo(getClass());
            if (fInfo != null) {
                description = fInfo.name();
            } else description = getClass().getSimpleName();
        }
        return description;
    }

    protected TreeMap<String, Value> paramMap = new TreeMap<>();

    public Map<String, Value> getParams() {
        return paramMap;
    }

    public void setParam(String paramName, Value value) {
        Value newValue = parseStringToArray(value);
        paramMap.put(paramName, newValue);
    }

    // allow "[3-629\3, 4-629\3, 5-629\3]", ["3-629\3", "4-629\3", "5-629\3"], "3-629\3"
    // parse Value<String> containing "[]" into Value<StringArrayValue>
    protected Value parseStringToArray(Value value) {
        if ( ! (value instanceof StringValue) )
            return value; // directly return other types including StringArrayValue
        String str = value.value().toString();
        // parse str
        if (str.contains("[")) { // "[3-629\3, 4-629\3, 5-629\3]"
            str = str.replaceAll("\\s+", "");
            str = str.replaceAll("\"|\'|\\[|\\]", "");
            String[] strArr = str.split(",");
            return new StringArrayValue(value.getId(), strArr, value.function);
        }
        return value; // charset="3-629\3"
    }

    public String getRichDescription() {

        String html = "<html><h3>" + getName() + " function</h3> <ul>";
        html += "<li>" + getDescription();
        html += "</ul></html>";
        return html;
    }

    public JComponent getViewer() {
        return new JLabel(getRichDescription());
    }

    public String codeString() {
        Map<String, Value> map = getParams();

        StringBuilder builder = new StringBuilder();
        builder.append(getName());
        builder.append("(");

        Constructor[] constructors = getClass().getConstructors();

        if (constructors.length == 1) {
            List<ParameterInfo> parameterInfoList = getParameterInfo(0);
            if (parameterInfoList.size() > 0) {
                int paramCount = 0;

                String name = parameterInfoList.get(0).name();

                if (parameterInfoList.get(0).optional() && map.get(name) == null) {
                    // DO NOTHING - this is an optional parameter with no value
                } else {
                    builder.append(Generator.getArgumentCodeString(name, map.get(name)));
                    paramCount += 1;
                }
                for (int i = 1; i < parameterInfoList.size(); i++) {
                    name = parameterInfoList.get(i).name();
                    if (parameterInfoList.get(i).optional() && map.get(name) == null) {
                        // DO NOTHING - this is an optional parameter with no value
                    } else {
                        if (paramCount > 0) builder.append(", ");
                        builder.append(Generator.getArgumentCodeString(name, map.get(name)));
                        paramCount += 1;
                    }
                }
            }
        } else {
            Iterator<Map.Entry<String, Value>> iterator = map.entrySet().iterator();
            if (iterator.hasNext()) {
                Map.Entry<String, Value> entry = iterator.next();

                builder.append(Generator.getArgumentCodeString(entry));
                while (iterator.hasNext()) {
                    entry = iterator.next();
                    builder.append(", ");
                    builder.append(Generator.getArgumentCodeString(entry));
                }
//            }
            }
        }
        builder.append(")");
        return builder.toString();
    }
}
