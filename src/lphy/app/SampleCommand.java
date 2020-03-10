package lphy.app;

import lphy.core.AlignmentFileLogger;
import lphy.core.Arguments;
import lphy.core.TreeFileLogger;
import lphy.core.VarFileLogger;
import lphy.graphicalModel.Command;
import lphy.graphicalModel.RandomVariableLogger;
import lphy.graphicalModel.Value;

import java.util.*;

/**
 * Created by adru001 on 10/03/20.
 */
public class SampleCommand implements Command {

    LinguaPhyloStudio app;

    static String[] arguments = {"n", "logFile", "treeFiles", "alignmentFiles", "name"};
    static Object[] defaults = {1, false, false, false, "model"};

    public SampleCommand(LinguaPhyloStudio app) {
        this.app = app;
    }

    public String getName() {
        return "sample";
    }

    public String getSignature() {
        StringBuilder builder = new StringBuilder();
        builder.append(getName());
        builder.append("(");
        builder.append(arguments[0]);
        builder.append("= ");
        builder.append(defaults[0]);
        for (int i = 1; i < arguments.length; i++) {
            builder.append(", ");
            builder.append(arguments[i]);
            builder.append("= ");
            builder.append(defaults[i]);
        }
        builder.append(");");
        return builder.toString();
    }

    public void execute(Map<String, Value<?>> params) {

        Arguments args = new Arguments(params);

        int n = args.getInteger(arguments[0], defaults[0]);
        boolean writeVarsToFile = args.getBoolean(arguments[1], defaults[1]);
        boolean writeTreesToFile = args.getBoolean(arguments[2], defaults[2]);
        boolean writeAlignmentsToFile = args.getBoolean(arguments[3], defaults[3]);
        String name = args.getString(arguments[4], defaults[4]);

        List<RandomVariableLogger> loggers = new ArrayList<>();

        if (writeVarsToFile) {
            System.out.println("writing to file!");
            loggers.add(new VarFileLogger(name));
        }
        if (writeTreesToFile) loggers.add(new TreeFileLogger(name));
        if (writeAlignmentsToFile) loggers.add(new AlignmentFileLogger(name));

        app.panel.sample(n, loggers);
    }
}
