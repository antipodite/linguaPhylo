package lphy.doc;

import lphy.core.lightweight.LGenerativeDistribution;
import lphy.core.lightweight.LGenerator;
import lphy.graphicalModel.DeterministicFunction;
import lphy.graphicalModel.GenerativeDistribution;
import lphy.graphicalModel.Generator;
import lphy.graphicalModel.MethodInfo;
import lphy.parser.ParserUtils;
import net.steppschuh.markdowngenerator.link.Link;
import net.steppschuh.markdowngenerator.list.UnorderedList;
import net.steppschuh.markdowngenerator.text.Text;
import net.steppschuh.markdowngenerator.text.emphasis.BoldText;
import net.steppschuh.markdowngenerator.text.heading.Heading;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class GenerateDocs {

    public static void main(String[] args) throws IOException {

        List<Class<GenerativeDistribution>> generativeDistributions = ParserUtils.getGenerativeDistributions();
        generativeDistributions.sort(Comparator.comparing(Class::getSimpleName));

        List<Class<DeterministicFunction>> functions = ParserUtils.getDeterministicFunctions();
        functions.sort(Comparator.comparing(Class::getSimpleName));

        functions.sort(Comparator.comparing(Class::getSimpleName));

//        GenerateDocs generateDocs = getInstance();
//        String version = generateDocs.getProperty("version");
        final String version = "0.0.4";

        String indexMD = generateIndex(generativeDistributions, functions, ParserUtils.types, version);

        FileWriter writer = new FileWriter("index.md");
        writer.write(indexMD);
        writer.close();
    }

//    private static final GenerateDocs instance = new GenerateDocs();
//    private GenerateDocs() {
//        try {
//            getMavenProperties();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static GenerateDocs getInstance(){
//        return instance;
//    }
//
//    private Properties properties;
//
//    public void getMavenProperties() throws IOException {
//        InputStream is = getClass().getClassLoader().getResourceAsStream("pom.xml");
//        this.properties = new Properties();
//        this.properties.load(is);
//    }
//
//    public String getProperty(String propertyName) {
//        return Objects.requireNonNull(this.properties).getProperty(propertyName);
//    }

    private static String generateIndex(List<Class<GenerativeDistribution>> generativeDistributions,
                                        List<Class<DeterministicFunction>> functions, Set<Class<?>> types,
                                        String version) throws IOException {
        StringBuilder builder = new StringBuilder();

        String h1 = "LPhy Language Reference";
        if (version != null || !version.trim().isEmpty())
            h1 += " (version " + version + ")";
        builder.append(new Heading(h1, 1)).append("\n");

        builder.append(new Text("This an automatically generated language reference of the LinguaPhylo (LPhy) statistical phylogenetic modeling language."));
        builder.append("\n\n");

        builder.append(new Heading("Generative distributions", 2)).append("\n");

        List<Link> genDistLinks = new ArrayList<>();

        Set<String> names = new TreeSet<>();

        File file = new File("distributions");
        if (!file.exists()) file.mkdir();

        for (Class<GenerativeDistribution> genDist : generativeDistributions) {
            String name = Generator.getGeneratorName(genDist);
            String fileURL = "distributions/" + name + ".md";

            if (!names.contains(name)) {
                genDistLinks.add(new Link(name, fileURL));
                names.add(name);

                FileWriter writer = new FileWriter(fileURL);

                generateGenerativeDistributions(writer, name, generativeDistributions.stream().filter(o -> Generator.getGeneratorName(o).equals(name)).collect(Collectors.toList()));
                writer.close();
            }
        }

        builder.append(new UnorderedList<>(genDistLinks)).append("\n\n");

        builder.append(new Heading("Functions", 2)).append("\n");

        List<Link> functionLinks = new ArrayList<>();

        Set<String> funcNames = new TreeSet<>();

        file = new File("functions");
        if (!file.exists()) file.mkdir();

        for (Class<DeterministicFunction> function : functions) {
            String name = Generator.getGeneratorName(function);
            String fileURL = "functions/" + name + ".md";

            if (!funcNames.contains(name)) {
                functionLinks.add(new Link(name, fileURL));
                funcNames.add(name);

                FileWriter writer = new FileWriter(fileURL);

                generateFunctions(writer, name, functions.stream().filter(o -> Generator.getGeneratorName(o).equals(name)).collect(Collectors.toList()));
                writer.close();
            }
        }

        builder.append(new UnorderedList<>(functionLinks)).append("\n\n");

        builder.append(new Heading("Types", 2)).append("\n");

        List<Link> typeLinks = new ArrayList<>();

        Set<String> typeNames = new TreeSet<>();

        file = new File("types");
        if (!file.exists()) file.mkdir();

        for (Class<?> type : types) {
            String name = type.getSimpleName();
            String fileURL = "types/" + name + ".md";

            if (!typeNames.contains(name)) {
                typeLinks.add(new Link(name, fileURL));
                typeNames.add(name);

                FileWriter writer = new FileWriter(fileURL);

                writer.write(generateType(type));
                writer.close();
            }
        }

        builder.append(new UnorderedList<>(typeLinks)).append("\n\n");

        return builder.toString();
    }

    private static String generateType(Class<?> type) {
        StringBuilder builder = new StringBuilder();

        builder.append(new Heading(type.getSimpleName(),1)).append("\n");

        TreeMap<String, MethodInfo> methodInfoTreeMap = new TreeMap<>();
        for (Method method : type.getMethods()) {
            MethodInfo methodInfo = method.getAnnotation(MethodInfo.class);
            if (methodInfo != null) {
                methodInfoTreeMap.put(method.getName(), methodInfo);
            }
        }
        if (methodInfoTreeMap.size() > 0) {
            builder.append(new Heading("Methods",2)).append("\n\n");
            List<Object> methodText = new ArrayList<>();

            for (Map.Entry<String,MethodInfo> methodInfoEntry : methodInfoTreeMap.entrySet()) {
                methodText.add(new BoldText(methodInfoEntry.getKey()) + "\n  - " + methodInfoEntry.getValue().description());
            }
            builder.append(new UnorderedList<>(methodText));
        }

        return builder.toString();
    }

    private static void generateGenerativeDistributions(FileWriter writer, String name, List<Class<?>> classes) throws IOException {

        StringBuilder builder = new StringBuilder();

        builder.append(new Heading(name + " distribution",1)).append("\n");

        for (Class<?> c : classes) {

            if (LGenerator.class.isAssignableFrom(c)) {
                builder.append(LGenerativeDistribution.getLightweightGeneratorMarkdown((Class<LGenerator>)c)).append("\n\n");
            } else {
                builder.append(Generator.getGeneratorMarkdown((Class<Generator>)c)).append("\n\n");
            }
        }

        writer.write(builder.toString());
    }

    private static void generateFunctions(FileWriter writer, String name, List<Class<DeterministicFunction>> classes) throws IOException {

        StringBuilder builder = new StringBuilder();

        builder.append(new Heading(name + " function",1)).append("\n");

        for (Class<DeterministicFunction> c : classes) {
            builder.append(Generator.getGeneratorMarkdown(c)).append("\n\n");
        }

        writer.write(builder.toString());
    }

}
