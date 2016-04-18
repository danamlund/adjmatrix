package dk.danamlund.adjmatrix;

import classycle.Analyser;
import classycle.AnalyserCommandLine;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.StringBuilder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class AdjMatrixTools {

    public static AdjMatrix classycle(String... classycleArgs) {
        AnalyserCommandLine acl = new AnalyserCommandLine(classycleArgs);

        Analyser analyser = new Analyser(acl.getClassFiles(), 
                                         acl.getPattern(), acl.getReflectionPattern(), true);
        
        StringWriter writer = new StringWriter();
        analyser.printRaw(new PrintWriter(writer));
        return readClassycleRaw(new BufferedReader(new StringReader(writer.toString())));
    }

    public static AdjMatrix readTgf(String tgfFile) {
        try {
            Map<Integer, String> idToNode = new HashMap<>();
            boolean seenHash = false;
            GraphBuilder graphBuilder = new GraphBuilder();
            for (String line : foreach(Files.lines(Paths.get(tgfFile)))) {
                if (line.isEmpty()) {
                    // nothing
                } else if (line.startsWith("#")) {
                    seenHash = true;
                } else if (!seenHash) {
                    int i = Integer.parseInt(line.substring(0, line.indexOf(" ")));
                    String node = line.substring(line.indexOf(" ")).trim();
                    idToNode.put(i, node);
                } else {
                    String[] split = line.split(" +");
                    int from = Integer.parseInt(split[0]);
                    int to = Integer.parseInt(split[1]);
                    graphBuilder.add(idToNode.get(from), idToNode.get(to));
                }
            }
            return graphBuilder.buildMatrix();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static AdjMatrix readClassycleRaw(String rawFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(rawFile));) {
                return AdjMatrixTools.readClassycleRaw(reader);
            } 
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
    public static AdjMatrix readClassycleRaw(BufferedReader reader) {
        Set<String> specials = new HashSet<>(Arrays.asList("class", "interface", "abstract"));
        String from = null;
        GraphBuilder graphBuilder = new GraphBuilder();
        Map<String, String> nodeToJar = new HashMap<>();
        for (String line : foreach(reader.lines())) {
            if (!specials.contains(line.split(" ")[0])
                && !(line.startsWith(" ")
                     && !line.startsWith("    unknown"))) {
                continue;
            }
            String className = null;
            String[] split = line.split(" +");
            for (String e : split) {
                if (!specials.contains(e)
                    && !e.isEmpty()) {
                    className = e;
                    break;
                }
            }
            if (className.contains("-")) {
                // ignore package-info
                continue;
            }
            if (className == null || className.isEmpty()) {
                throw new IllegalStateException("Could not find className for: " + line);
            }

            if (!nodeToJar.containsKey(className)) {
                String jar = split[split.length - 1];
                if (jar.endsWith(".jar")) {
                    for (String sep : Arrays.asList("/", "\\")) {
                        if (jar.contains(sep)) {
                            jar = jar.substring(jar.lastIndexOf(sep) + 1);
                        }
                    }
                    nodeToJar.put(className, jar);
                } else {
                    nodeToJar.put(className, "<unknown>");
                }
            }

            if (line.startsWith("   ")) {
                graphBuilder.add(from, className);
            } else {
                from = className;
            }
        }
        AdjMatrix adjMatrix = graphBuilder.buildMatrix();
        adjMatrix.addNodeStringData("jar", nodeToJar);
        return adjMatrix;
    }

    @SuppressWarnings("unchecked")
    private static <T> Iterable<T> foreach(Stream<T> stream) {
        return new Iterable<T>() {
            @Override
                public Iterator<T> iterator() {
                return stream.iterator();
            }
        };
    }

    private static class GraphBuilder {
        private final Map<String, Integer> nodeToIndex = new HashMap<>();
        private final Map<Integer, Set<Integer>> edgesFrom = new HashMap<>();
        private int nextNodeIndex = 0;

        public void add(String from, String to) {
            int fromIndex = getNodeIndex(from);
            Set<Integer> edges = edgesFrom.get(fromIndex);
            if (edges == null) {
                edges = new HashSet<>();
                edgesFrom.put(fromIndex, edges);
            }
            edges.add(getNodeIndex(to));
        }

        public AdjMatrix buildMatrix() {
            int size = nextNodeIndex;

            Map<Integer, String> indexToNode = new HashMap<>();
            for (Map.Entry<String, Integer> entry : nodeToIndex.entrySet()) {
                indexToNode.put(entry.getValue(), entry.getKey());
            }

            List<String> outNodes = new ArrayList<>();
            List<Set<Integer>> outEdgesFrom = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                outNodes.add(indexToNode.get(i));
                Set<Integer> edges = edgesFrom.get(i);
                if (edges == null) {
                    edges = new HashSet<>();
                }
                outEdgesFrom.add(edges);
            }
            return AdjMatrix.newInstance(outNodes, outEdgesFrom);
        }


        private int getNodeIndex(String node) {
            if (!nodeToIndex.containsKey(node)) {
                nodeToIndex.put(node, nextNodeIndex++);
            }
            return nodeToIndex.get(node);
        }
    }
}
