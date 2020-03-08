package james.app.graphicalmodelcomponent;

import james.TimeTree;
import james.core.Alignment;
import james.core.LPhyParser;
import james.graphicalModel.*;
import james.utils.Message;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.Map;

public class LayeredGNode extends LayeredNode.Default {

    private JButton button;
    private Object value;
    String name = null;

    static double VAR_WIDTH = 90;
    static double VAR_HEIGHT = 50;

    static double FACTOR_SIZE = 7;
    static double FACTOR_LABEL_GAP = 7;

    static DecimalFormat format = new DecimalFormat();

    LPhyParser parser;
    boolean showValue;

    public LayeredGNode(Object value, LPhyParser parser) {
        super(0,0);

        this.value = value;
        this.parser = parser;

        if (value instanceof Value) {
            name = ((Value)value).getId();
            createValueButton();

        } else if (value instanceof Parameterized) {
            createParameterizedButton();
        }
    }

    public void addOutput(LayeredGNode output) {
        getSuccessors().add(output);

        if (getSuccessors().size() == 1 && (value instanceof Value) && ((Value) value).isAnonymous()) {
            name = "[" + ((Parameterized)output.value).getParamName(((Value) value)) + "]";
            button.setText(getButtonString((Value)value, true));
        }
    }
    
    public boolean hasButton() {
        return button != null;
    }

    private String getButtonString(Value v, boolean showValue) {

        Object value = v.value();

        if (name == null) name = "null";

        String displayName = name;

        if (displayName.length() > 7) {
            displayName = "<small>" + displayName + "</small>";
        }

        if (multiDimensional(value)) {
            return "<html><center><p><b>" + displayName + "</b></p></center></html>";
        }

        String valueString = "";
        if (showValue) {
            if (v.value() instanceof Double) {
                valueString = format.format(v.value());
            } else {
                valueString = v.value().toString();
                if (v.value() instanceof String) {
                    if (valueString.length() > 8) {
                        valueString = valueString.length() + " chars";
                        if (valueString.length() > 10) {
                            valueString = "string";
                        }
                    }
                }
            }
            valueString = "<p><font color=\"#808080\" ><small>" + valueString + "</small></font></p>";
        }

        return "<html><center><p>" + displayName + "</p>" + valueString + "</center></html>";
    }

    private boolean multiDimensional(Object v) {
        return (v instanceof Map || v instanceof Alignment || v instanceof TimeTree || v instanceof Integer[] ||
                v instanceof Double[] || v instanceof Double[][] || v instanceof Integer[][]);
    }

    private void createValueButton() {
        Color backgroundColor = new Color(0.0f, 1.0f, 0.0f, 0.5f);
        Color borderColor = new Color(0.0f, 0.75f, 0.0f, 1.0f);

        if (ValueUtils.isFixedValue((Value) value)) {
            backgroundColor = Color.white;
            borderColor = Color.black;
        } else if (ValueUtils.isValueOfFunction((Value) value)) {
            backgroundColor = new Color(1.0f, 0.0f, 0.0f, 0.5f);
            borderColor = new Color(0.75f, 0.0f, 0.0f, 1.0f);
        }

        if (!((Value)value).isAnonymous() && parser.getDictionary().get(((Value)value).getId()) != value) {
            backgroundColor = backgroundColor.darker();
            borderColor = borderColor.darker();
        }

        String str = getButtonString((Value)value, true);

        if (button == null) {
            if (((Value) value).getFunction() != null) {
                button = new DiamondButton(str, backgroundColor, borderColor);
            } else if (!(value instanceof RandomVariable)) {
                button = new SquareButton(str, backgroundColor, borderColor);
            } else {
                button = new CircleButton(str, backgroundColor, borderColor);
            }
        }
        button.setSize((int) VAR_WIDTH, (int) VAR_HEIGHT);

        // keep button string up to date.
        ((Value)value).addValueListener(() -> button.setText(getButtonString((Value)value, true)));
    }

    private void createParameterizedButton() {
        button = new JButton("");
        button.setSize((int) FACTOR_SIZE * 2, (int) FACTOR_SIZE * 2);
    }

    public Object value() {
        return value;
    }

    void setLayer() {
        int maxLayer = -1;
        for (LayeredNode node : getSuccessors()) {
            if (node.getLayer() >= maxLayer) maxLayer = node.getLayer();
        }
        layer = maxLayer + 1;
    }

    public int siblingCount(LayeredGNode parent) {
        if (parent.getPredecessors().contains(this)) {
            return parent.getPredecessors().size() - 1;
        } else {
            return -1;
        }
    }

    public double getRelativeIndex(LayeredGNode parent) {
        double index = parent.getPredecessors().indexOf(this);

        return index - ((parent.getPredecessors().size()-1.0) / 2.0);
    }

    public double getPreferredX(double preferredSpacing) {
        double x = 0;

        for (LayeredNode parent : getSuccessors()) {
            x += getPreferredX((LayeredGNode)parent, preferredSpacing);
        }
        return x / getSuccessors().size();
    }

    public double getPreferredX(LayeredGNode parent, double preferredSpacing) {
        return getRelativeIndex(parent) * preferredSpacing + parent.getX();
    }

    public JButton getButton() {
        return button;
    }

    public void setX(double x) {
        if (!Double.isNaN(x)) {
            setLocation(x, getY());
        } else {
            Message.error("Tried to set x coordinate of node " + this + " to NaN!", this);
        }
    }

    public void setY(double y) {
        setLocation(getX(), y);
    }

    public void setLocation(double x, double y) {
        this.x = x;
        this.y = y;
        if (hasButton()) button.setLocation((int) (x - button.getWidth()/2), (int) (y - button.getHeight()/2));
    }

    public String toString() {

        return "v(" + getLayer() + ", " + getIndex() + ") = " + value.toString();
    }
}