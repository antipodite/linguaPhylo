package lphy.app;

import lphy.graphicalModel.Value;
import lphy.graphicalModel.Vector;
import lphy.graphicalModel.types.VectorValue;

import javax.swing.*;
import java.awt.*;

public class VectorComponent extends JComponent {

    Vector vectorValue;

    public  VectorComponent(Vector vectorValue) {
        this.vectorValue = vectorValue;

        int size = vectorValue.size();

        GridLayout gridLayout = new GridLayout((int)Math.ceil(size/2.0),2,5,5);
        setLayout(gridLayout);

        for (int i = 0; i < size; i++) {
            Object component = vectorValue.getComponent(i);
            if (component instanceof HasComponentView) {
                add(((HasComponentView) component).getComponent(new Value(i+"", component)));
            }
        }
    }
}
