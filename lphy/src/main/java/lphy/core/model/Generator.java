package lphy.core.model;

import lphy.core.model.annotation.Citation;
import lphy.core.model.annotation.CitationUtils;
import lphy.core.model.annotation.GeneratorInfo;
import lphy.core.model.annotation.ParameterInfo;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static lphy.core.model.GeneratorUtils.getGeneratorInfo;
import static lphy.core.model.GeneratorUtils.getReturnType;

/**
 * A generator generates values, either deterministically (DeterministicFunction) or stochastically (GenerativeDistribution).
 * A generator also takes named Parameters which are themselves Values, which may have been generated by a Generator.
 */
public interface Generator<T> extends GraphicalModelNode<T> {


    String getName();

    /**
     * @return a value generated by this generator.
     */
    Value<T> generate();

    String codeString();

    /**
     * @return the character symbol, for function '=' and for generative distribution '~'
     */
    char generatorCodeChar();

    @Override
    default List<GraphicalModelNode> getInputs() {
        return new ArrayList<>(getParams().values());
    }

    Map<String, Value> getParams();

    default String getInferenceStatement(Value value, Narrative narrative) {

        return NarrativeUtils.getGeneratorInferenceStatement(this, value, narrative);
    }

    default String getInferenceNarrative(Value value, boolean unique, Narrative narrative) {

        String narrativeName = getNarrativeName();

        GeneratorInfo info = getGeneratorInfo(this.getClass());
        Citation cite = CitationUtils.getCitation(this.getClass());
        String citationString = narrative.cite(cite);

        String verbClause = info != null ? info.verbClause() : "comes from";
        StringBuilder builder = new StringBuilder();
        builder.append(NarrativeUtils.getValueClause(value, unique, narrative));
        builder.append(" ");
        builder.append(verbClause);
        builder.append(" ");
        if (!(this instanceof ExpressionNode)) {
            if (this instanceof DeterministicFunction) {
                builder.append(NarrativeUtils.getDefiniteArticle(narrativeName, true));
            } else {
                builder.append(NarrativeUtils.getIndefiniteArticle(narrativeName, true));
            }
        }
        builder.append(" ");
        builder.append(narrativeName);
        if (citationString != null && citationString != "") {
            builder.append(" ");
            builder.append(citationString);
        }

        Map<String, Value> params = getParams();
        String currentVerb = "";
        List<ParameterInfo> parameterInfos = getParameterInfo(0);
        int count = 0;
        for (ParameterInfo parameterInfo : parameterInfos) {
            Value v = params.get(parameterInfo.name());
            if (v != null) {
                if (count == 0) builder.append(" ");
                if (count > 0) {
                    if (count == params.size() - 1) {
                        builder.append(" and ");
                    } else {
                        builder.append(", ");
                    }
                }
                if (!parameterInfo.verb().equals(currentVerb)) {
                    currentVerb = parameterInfo.verb();
                    builder.append(currentVerb);
                    builder.append(" ");
                }
                builder.append(NarrativeUtils.getValueClause(v, false, true, false, this, narrative));
                count += 1;
            }
        }
        builder.append(".");
        return builder.toString();
    }

    /**
     * Get the name of the type of object generated by this generator.
     * @return
     */
    default String getTypeName() {
        return getReturnType(this.getClass()).getSimpleName();
    }

    default void setParam(String paramName, Value<?> value) {

        String methodName = "set" + Character.toUpperCase(paramName.charAt(0)) + paramName.substring(1);

        try {
            Method method = getClass().getMethod(methodName, value.value().getClass());

            method.invoke(this, value.value());
        } catch (NoSuchMethodException e) {

            Method[] methods = getClass().getMethods();
            for (Method method : methods) {
                if (method.getName().equals(methodName)) {
                    try {
                        method.invoke(this, value.value());
                        break;
                    } catch (InvocationTargetException | IllegalAccessException ignored) {
                    }
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    default void setInput(String paramName, Value<?> value) {
        setParam(paramName, value);
        value.addOutput(this);
    }

    default void setInputs(Map<String, Value<?>> params) {
        params.forEach(this::setInput);
    }

    default String getParamName(Value value) {
        Map<String, Value> params = getParams();
        for (String key : params.keySet()) {
            if (params.get(key) == value) return key;
        }
        return null;
    }

    /**
     * @return true if any of the parameters are random variables,
     * or are themselves that result of a function with random parameters as arguments.
     */
    default boolean hasRandomParameters() {
        for (Map.Entry<String, Value> entry : getParams().entrySet()) {

            Value<?> v = entry.getValue();

            if (v == null) {
                throw new RuntimeException("Unexpected null value for param " + entry.getKey() + " in generator " + getName());
            }

            if (v.isRandom()) return true;
        }
        return false;
    }

    default String getParamName(int paramIndex, int constructorIndex) {
        return getParameterInfo(constructorIndex).get(paramIndex).name();
    }

    @Deprecated
    default String getParamName(int paramIndex) {
        return getParamName(paramIndex, 0);
    }

    default List<ParameterInfo> getParameterInfo(int constructorIndex) {
        return GeneratorUtils.getParameterInfo(this.getClass(), constructorIndex);
    }

    default Class<?> getParamType(String name) {
        return getParams().get(name).getType();
    }

    default GeneratorInfo getInfo() {

        Class<?> classElement = getClass();

        Method[] methods = classElement.getMethods();

        for (Method method : methods) {
            for (Annotation annotation : method.getAnnotations()) {
                if (annotation instanceof GeneratorInfo) {
                    return (GeneratorInfo) annotation;
                }
            }
        }
        return null;
    }

    default String getRichDescription(int index) {

        List<ParameterInfo> pInfo = getParameterInfo(index);

        Map<String, Value> paramValues = getParams();

        StringBuilder html = new StringBuilder("<html><h3>");
        html.append(getName());
        if (this instanceof GenerativeDistribution) {
            html.append(" distribution");
        }
        html.append("</h3>");
        GeneratorInfo info = getInfo();
        if (info != null) {
            html.append("<p>").append(getInfo().description()).append("</p><br>");
        }
        if (pInfo.size() > 0) {
            html.append("<p>parameters: <ul>");
            for (ParameterInfo pi : pInfo) {
                html.append("<li>").append(pi.name()).append(" (").append(paramValues.get(pi.name())).append("); <font color=\"#808080\">").append(pi.description()).append("</font></li>");
            }
            html.append("</ul></p>");
        }

        Citation citation = CitationUtils.getCitation(this.getClass());
        if (citation != null) {
            html.append("<h3>Reference</h3>");
            html.append(citation.value());
            String url = CitationUtils.getURL(citation);
            if (url.length() > 0) {
                html.append("<br><a href=\"" + url + "\">" + url + "</a><br>");
            }
        }

        html.append("</html>");
        return html.toString();
    }


    /**
     * @param value
     * @return the narrative name for the given value, being a parameter of this generator.
     */
    default String getNarrativeName(Value value) {
        return getNarrativeName(getParamName(value));
    }

    /**
     * @param paramName the parameter name
     * @return the narrative name for the given parameter name.
     */
    default String getNarrativeName(String paramName) {
        List<ParameterInfo> parameterInfos = getParameterInfo(0);
        for (ParameterInfo parameterInfo : parameterInfos) {
            if (parameterInfo.name().equals(paramName)) {
                if (parameterInfo.suppressNameInNarrative()) return "";
                if (parameterInfo.narrativeName().length() > 0) {
                    return parameterInfo.narrativeName();
                }
            }
        }
        return paramName;
    }

    /**
     * @return the narrative name of this generator.
     */
    default String getNarrativeName() {
        GeneratorInfo generatorInfo = getGeneratorInfo(this.getClass());
        if (generatorInfo != null) {
            if (generatorInfo.narrativeName().length() > 0) return generatorInfo.narrativeName();
        }
        return getName();
    }

    // all other static methods mv to GeneratorUtils

}
