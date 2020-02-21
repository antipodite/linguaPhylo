package james.app;

import james.graphicalModel.GraphicalModelParser;
import james.graphicalModel.*;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class GraphicalModelPanel extends JPanel {

    GraphicalModelComponent component;
    JLabel dummyLabel = new JLabel("");
    GraphicalModelInterpreter interpreter;
    JTabbedPane rightPane;
    Log log = new Log();
    TreeLog treeLog = new TreeLog();

    GraphicalModelParser parser;

    JButton sampleButton = new JButton("Sample");
    JButton shiftLeftButton = new JButton("<");
    JButton shiftRightButton = new JButton(">");

    JSplitPane splitPane;

    Object displayedElement;

    JScrollPane currentSelectionContainer = new JScrollPane();

    Command sampleCommand = new Command() {
        @Override
        public String getName() {
            return "sample";
        }

        public void execute(Map<String, Value> params) {
            Value val = params.values().iterator().next();
            if (val.value() instanceof Integer) {
                sample((Integer)val.value());
            }
        }
    };

    GraphicalModelPanel(GraphicalModelParser parser) {

        this.parser = parser;

        parser.addCommand(sampleCommand);

        parser.addCommand(new Command() {
            @Override
            public String getName() {
                return "log.clear";
            }

            public void execute(Map<String, Value> params) {
                log.clear();
            }
        });

        interpreter = new GraphicalModelInterpreter(parser);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.add(sampleButton);
        buttonPanel.add(shiftLeftButton);
        buttonPanel.add(shiftRightButton);

        sampleButton.addActionListener(e -> sample(1));
        shiftLeftButton.addActionListener(e -> component.shiftLeft());
        shiftRightButton.addActionListener(e -> component.shiftRight());

        component = new GraphicalModelComponent(parser);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(component);
        panel.add(buttonPanel);

        setLayout(new BorderLayout());

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panel, dummyLabel);
        splitPane.setResizeWeight(0.5);
        add(splitPane, BorderLayout.CENTER);

        GraphicalModelListener listener = new GraphicalModelListener() {
            @Override
            public void valueSelected(Value value) {

                showValue(value);
                rightPane.setSelectedIndex(0);
            }

            @Override
            public void generativeDistributionSelected(GenerativeDistribution g) {
                showParameterized(g);
            }

            @Override
            public void functionSelected(DeterministicFunction f) {

                showParameterized(f);
            }

        };

        component.addGraphicalModelListener(listener);
        parser.addGraphicalModelChangeListener(component);
        parser.addGraphicalModelListener(new GraphicalModelListener() {
            @Override
            public void valueSelected(Value value) {

                showValue(value);
            }

            @Override
            public void generativeDistributionSelected(GenerativeDistribution g) {

            }

            @Override
            public void functionSelected(DeterministicFunction f) {

            }
        });
        
        add(interpreter, BorderLayout.SOUTH);

        currentSelectionContainer.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        currentSelectionContainer.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        rightPane = new JTabbedPane();
        rightPane.addTab("Current", currentSelectionContainer);
        rightPane.addTab("Values", new StatePanel(parser, true, false, false));
        rightPane.addTab("Variables", new StatePanel(parser, false, true, false));
        rightPane.addTab("Model", new CanonicalModelPanel(parser));
        rightPane.addTab("Log", new JScrollPane(log));
        rightPane.addTab("Trees", new JScrollPane(treeLog));
        splitPane.setRightComponent(rightPane);

        showValue(parser.getRoots().iterator().next());
    }

    private void sample(int reps) {
        String id = null;
        if (displayedElement instanceof Value && !((Value)displayedElement).isAnonymous()) {
            id = ((Value)displayedElement).getId();
        }
        for (int i =0; i < reps; i++) {
            parser.sample();
            log.log(parser.getAllVariablesFromRoots());
            treeLog.log(parser.getAllVariablesFromRoots());
        }
        if (id != null && parser.genDistDictionary.get(id) != null) {
            showValue(parser.getDictionary().get(id));
        } else {
            showValue(parser.getRoots().iterator().next());
        }
    }

    public JComponent getViewer(Object object) {

        if (object instanceof Viewable) {
            return ((Viewable) object).getViewer();
        }

        return new JLabel(object.toString());
    }

    void showValue(Value value) {
        if (value != null) showObject(value.getLabel(), value);
    }

    private void showParameterized(Parameterized g) {
        showObject(g.codeString(), g);
    }

    private void showObject(String label, Object obj) {
        displayedElement = obj;

        JComponent viewer = getViewer(obj);

        if (viewer.getPreferredSize().height > 1) {
            JPanel viewerPanel = new JPanel();
            viewerPanel.setOpaque(false);
            viewerPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
            viewerPanel.add(viewer);
            viewer = viewerPanel;
        }
        currentSelectionContainer.setViewportView(viewer);
        currentSelectionContainer.setBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createMatteBorder(0,0,0,0, viewer.getBackground()),
                        "<html><font color=\"#808080\" >" + label + "</font></html>"));
        repaint();
    }

    public void readScript(File scriptFile) {
        Path path = Paths.get(scriptFile.getAbsolutePath());
        try {
            String mimeType = Files.probeContentType(path);

            if (mimeType.equals("text/plain")) {
                parser.clear();
                interpreter.clear();

                FileReader reader = new FileReader(scriptFile);
                BufferedReader bufferedReader = new BufferedReader(reader);
                String line = bufferedReader.readLine();
                while (line != null) {
                    interpreter.interpretInput(line);
                    line = bufferedReader.readLine();
                }
                bufferedReader.close();
                reader.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}