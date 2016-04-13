package dk.danamlund.adjmatrix;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.StringBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AdjMatrix {
    private final List<String> nodes;
    private final List<Set<Integer>> edgesFrom;
    private final List<Set<Integer>> edgesTo;
    private final List<Integer> selected;
    private final Map<String, List<String>> nodesData = new LinkedHashMap<>();
    private Random random = new Random(42);

    public static AdjMatrix newInstance(List<String> nodes, List<Set<Integer>> edgesFrom) {
        return newInstance(nodes, edgesFrom, newIncList(nodes.size()));
    }

    public static AdjMatrix newInstance(List<String> nodes, List<Set<Integer>> edgesFrom, 
                                        List<Integer> selected) {
        return new AdjMatrix(nodes, edgesFrom, selected);
    }

    private AdjMatrix(List<String> nodes, List<Set<Integer>> edgesFrom, List<Integer> selected) {
        this.nodes = nodes;
        this.edgesFrom = edgesFrom;
        this.edgesTo = calculateEdgesTo(edgesFrom);
        this.selected = selected;
    }

    private AdjMatrix(AdjMatrix old, List<Integer> selected) {
        this.nodes = old.nodes;
        this.edgesFrom = old.edgesFrom;
        this.edgesTo = old.edgesTo;
        this.selected = selected;
    }

    private static List<Set<Integer>> calculateEdgesTo(List<Set<Integer>> edgesFrom) {
        List<Set<Integer>> edgesTo = new ArrayList<>();
        for (int i = 0; i < edgesFrom.size(); i++) {
            edgesTo.add(new HashSet<>());
        }
        
        for (int from = 0; from < edgesFrom.size(); from++) {
            for (int to : edgesFrom.get(from)) {
                edgesTo.get(to).add(from);
            }
        }
        return edgesTo;
    }

    public int getSize() {
        return nodes.size();
    }

    void setRandom(Random random) {
        this.random = random;
    }

    AdjMatrix selected(List<Integer> newSelected) {
        return new AdjMatrix(this, newSelected);
    }

    public AdjMatrix shuffled() {
        List<Integer> newIds = newIncList(nodes.size());
        Collections.shuffle(newIds, random);
        int[] newIdsIndexOf = new int[newIds.size()];
        for (int i = 0; i < newIdsIndexOf.length; i++) {
            newIdsIndexOf[newIds.get(i)] = i;
        }
        

        List<String> newNodes = new ArrayList<>(nodes.size());
        for (int oldId : newIds) {
            newNodes.add(nodes.get(oldId));
        }

        List<Set<Integer>> newEdgesFrom = new ArrayList<>(nodes.size());
        for (int oldId : newIds) {
            Set<Integer> newEdges = new HashSet<>();
            for (int to : edgesFrom.get(oldId)) {
                newEdges.add(newIdsIndexOf[to]);
            }
            newEdgesFrom.add(newEdges);
        }        
        
        List<Integer> newSelected = new ArrayList<>();
        for (Integer oldSelect : selected) {
            newSelected.add(newIdsIndexOf[oldSelect]);
        }
        Collections.shuffle(newSelected);

        return AdjMatrix.newInstance(newNodes, newEdgesFrom, newSelected);
    }

    public void addNodeStringData(String dataName, Map<String, String> nodeToNewData) {
        List<String> data = new ArrayList<>();
        for (String node : nodes) {
            data.add(nodeToNewData.get(node));
        }
        nodesData.put(dataName, data);
    }

    public AdjMatrix sortedByName() {
        List<Integer> newSelected = new ArrayList<>(selected);
        newSelected.sort((a, b) -> nodes.get(a).compareTo(nodes.get(b)));
        return new AdjMatrix(this, newSelected);
    }

    public AdjMatrix sortedByPackage() {
        List<Integer> newSelected = new ArrayList<>(selected);
        newSelected.sort((a, b) -> getPackage(nodes.get(a)).compareTo(getPackage(nodes.get(b))));
        return new AdjMatrix(this, newSelected);
    }

    public AdjMatrix sortedByDegree() {
        Map<Integer, Integer> degrees = new HashMap<>();
        for (int i : selected) {
            degrees.put(i, edgesFrom.get(i).size() + edgesTo.get(i).size());
        }
        
        List<Integer> newSelected = new ArrayList<>(selected);
        newSelected.sort((a, b) -> degrees.get(b).compareTo(degrees.get(a)));
        return new AdjMatrix(this, newSelected);
    }

    public AdjMatrix sortedByUses() {
        Map<Integer, Integer> degrees = new HashMap<>();
        for (int i : selected) {
            degrees.put(i, edgesFrom.get(i).size());
        }
        
        List<Integer> newSelected = new ArrayList<>(selected);
        newSelected.sort((a, b) -> degrees.get(b).compareTo(degrees.get(a)));
        return new AdjMatrix(this, newSelected);
    }

    public AdjMatrix sortedByUsed() {
        Map<Integer, Integer> degrees = new HashMap<>();
        for (int i : selected) {
            degrees.put(i, edgesTo.get(i).size());
        }
        
        List<Integer> newSelected = new ArrayList<>(selected);
        newSelected.sort((a, b) -> degrees.get(b).compareTo(degrees.get(a)));
        return new AdjMatrix(this, newSelected);
    }

    public AdjMatrix sortedByNodeData(String dataName) {
        List<Integer> dataSorted = new ArrayList<>(nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            dataSorted.add(i);
        }
        dataSorted.sort(Comparator.comparing(i -> nodesData.get(dataName).get(i)));
        return new AdjMatrix(this, dataSorted);
    }

    private int getMedianConnections() {
        List<Integer> nodeConnections = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            nodeConnections.add(edgesFrom.get(i).size() + edgesTo.get(i).size());
        }
        nodeConnections.sort(Comparator.naturalOrder());
        return nodeConnections.get(nodeConnections.size() / 2);
    }

    public AdjMatrix sortedBySimilarity() {
        class GroupScore implements Comparable<GroupScore> {
            Set<Integer> group;
            int groupSize;
            int internals = 0;
            int externals = 0;
            public GroupScore(Set<Integer> group) {
                this.group = group;
                groupSize = group.size();
                for (Integer i : group) {
                    Set<Integer> edges = edgesFrom.get(i);
                    int prevInternal = internals;
                    for (Integer j : group) {
                        if (edges.contains(j)) {
                            internals++;
                        }
                    }
                    externals += edges.size() - (internals - prevInternal);
                }
            }
            public GroupScore(GroupScore gs, Integer with) {
                internals = gs.internals;
                externals = gs.externals;
                groupSize = gs.groupSize + 1;
                for (Set<Integer> edges : Arrays.asList(edgesFrom.get(with), edgesTo.get(with))) {
                    for (Integer i : edges) {
                        if (gs.group.contains(i)) {
                            internals++;
                            externals--;
                        }
                    }
                }
            }

            public double getScore() {  
                return ((double) internals / groupSize) - ((double) externals / groupSize);
            }
            public double getScore(Integer with) {
                return new GroupScore(this, with).getScore();
            }
            public void add(Integer with) {
                GroupScore gs = new GroupScore(this, with);
                internals = gs.internals;
                externals = gs.externals;
                groupSize = gs.groupSize;
                group.add(with);
            }
            public int compareTo(GroupScore gs) {
                return Double.compare(gs.getScore(), getScore());
            }
        }

        List<Integer> selected = new ArrayList<>(this.selected);
        Collections.shuffle(selected);
        List<GroupScore> groups = new ArrayList<>();
        Set<Integer> left = new HashSet<>(selected);
        while (!left.isEmpty()) {
            Integer g = left.iterator().next();
            left.remove(g);
            GroupScore gs = new GroupScore(new HashSet<>(Collections.singleton(g)));
            while (true) {
                double score = gs.getScore();
                List<Integer> betters = new ArrayList<>();
                Map<Integer, Double> bettersScore = new HashMap<>();
                for (Integer i : left) {
                    double iScore = gs.getScore(i);
                    if (iScore > score) {
                        betters.add(i);
                        bettersScore.put(i, iScore);
                    }
                }
                if (betters.isEmpty()) {
                    groups.add(gs);
                    break;
                }
                betters.sort((a,b) -> Double.compare(bettersScore.get(b), bettersScore.get(a)));
                Integer best = betters.get(0);
                left.remove(best);
                gs.add(best);
            }
        }

        groups.sort(Comparator.comparing((GroupScore gs) -> gs.group.size()).reversed());
        List<Integer> order = new ArrayList<>();
        order.addAll(groups.stream().flatMap(gs -> gs.group.stream()).collect(Collectors.toList()));
        order.addAll(left);
        return new AdjMatrix(this, order);
    }

    public List<Color> getColorsByName() {
        return getDistinctColors(nodes.size());
    }

    public List<Color> getColorsByPackage() {
        Set<String> packages = new HashSet<>();
        for (String node : nodes) {
            packages.add(getPackage(node));
        }

        List<Color> distinctColors = getDistinctColors(packages.size());

        Map<String, Color> packageToColor = new HashMap<>();
        int pkgI = 0;
        for (String pkg : packages) {
            packageToColor.put(pkg, distinctColors.get(pkgI++));
        }
        
        List<Color> out = new ArrayList<>();
        for (String node : nodes) {
            out.add(packageToColor.get(getPackage(node)));
        }
        return out;
    }

    public List<Color> getColorsByNodeData(String dataName) {
        Set<String> dataSet = new HashSet<>();
        for (String data : nodesData.get(dataName)) {
            dataSet.add(data);
        }

        List<Color> distinctColors = getDistinctColors(dataSet.size());

        Map<String, Color> dataToColor = new HashMap<>();
        int dataI = 0;
        for (String data : dataSet) {
            dataToColor.put(data, distinctColors.get(dataI++));
        }
        
        List<Color> out = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            out.add(dataToColor.get(nodesData.get(dataName).get(i)));
        }
        return out;
    }

    public List<Color> getColorsByConnections() {
        int maxConnections = 0;
        for (int i = 0; i < nodes.size(); i++) {
            int connections = edgesFrom.get(i).size() + edgesTo.get(i).size();
            if (connections > maxConnections) {
                maxConnections = connections;
            }
        }
        List<Color> out = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            int connections = edgesFrom.get(i).size() + edgesTo.get(i).size();
            out.add(new Color(0.0f, 0.0f, 
                              Math.min(1.0f, 0.1f + (float) connections / maxConnections)));
        }
        return out;
    }

    private AdjMatrix groupedBy(Function<String, String> grouper) {
        Map<String, Integer> groupToGroupId = new LinkedHashMap<>();
        int nextGroupId = 0;
        Map<Integer, Integer> nodeIdToGroupId = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            String group = grouper.apply(nodes.get(i));
            if (!groupToGroupId.containsKey(group)) {
                groupToGroupId.put(group, nextGroupId++);
            }
            int groupId = groupToGroupId.get(group);
            nodeIdToGroupId.put(i, groupId);
        }

        List<String> groups = new ArrayList<>(groupToGroupId.keySet());
        List<Set<Integer>> groupsEdgesFrom = new ArrayList<>();
        for (int i = 0; i < groups.size(); i++) {
            groupsEdgesFrom.add(new HashSet<>());
        }

        for (int i = 0; i < nodes.size(); i++) {
            int fromGroup = nodeIdToGroupId.get(i);
            for (int toNode : edgesFrom.get(i)) {
                int toGroup = nodeIdToGroupId.get(toNode);
                if (fromGroup != toGroup) {
                    groupsEdgesFrom.get(fromGroup).add(toGroup);
                }
            }
        }
        return AdjMatrix.newInstance(groups, groupsEdgesFrom);
    }

    public AdjMatrix groupedByPackage() {
        return groupedBy(AdjMatrix::getPackage);
    }

    public AdjMatrix groupedByNodeData(String dataKey) {
        Map<String, Integer> nodeToNodeId = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            nodeToNodeId.put(nodes.get(i), i);
        }
        return groupedBy(node -> nodesData.get(dataKey).get(nodeToNodeId.get(node)));
    }

    private static String getPackage(String node) {
        if (node.contains(".")) {
            return node.substring(0, node.lastIndexOf("."));
        } else {
            return "";
        }
    }

    private List<Color> getDistinctColors(int amount) {
        List<Color> out = new ArrayList<>();

        if (amount > 200) {
            for (int i = 0; i < amount; i++) {
                Color color = new Color(random.nextFloat(), random.nextFloat(), random.nextFloat());
                out.add(color);
            }
            return out;
        }

        for (int i = 0; i < amount; i++) {
            Color best = null;
            float minDiff = 3.0f;
            // try some random colors
            for (int j = 0; j < Math.max(10, out.size() + 1); j++) {
                Color color = new Color(random.nextFloat(), random.nextFloat(), random.nextFloat());
                float curMaxDiff = 0.0f;
                for (Color c : out) {
                    float diff = getColorDiff(color, c);
                    if (diff > curMaxDiff) {
                        curMaxDiff = diff;
                    }
                }
                // the best color is the one that is farthest away from all current colors
                if (minDiff < curMaxDiff || best == null) {
                    best = color;
                    minDiff = curMaxDiff;
                }
                
            }
            out.add(best);
        }
        return out;
    }
    private static float getColorDiff(Color a, Color b) {
        float[] aRgb = a.getComponents(null);
        float[] bRgb = b.getComponents(null);
        return Math.abs(aRgb[0] - bRgb[0]) + 
            Math.abs(aRgb[1] - aRgb[1]) +
            Math.abs(aRgb[2] - aRgb[2]);
    }
    
    List<String> getSelectedNodes() {
        List<String> selectedNodes = new ArrayList<>();
        for (int i : selected) {
            selectedNodes.add(nodes.get(i));
        }
        return selectedNodes;
    }

    public String stringify() {
        StringBuilder sb = new StringBuilder();
        int maxNodesLength = selected.stream().map(nodes::get)
            .mapToInt(String::length).max().orElse(0);
        
        for (int i : selected) {
            sb.append(String.format("%" + maxNodesLength +"s", nodes.get(i)));
            sb.append(" ");
            for (int j : selected) {
                if (i == j || edgesFrom.get(i).contains(j)) {
                    sb.append("x");
                } else {
                    sb.append(" ");
                }
            }
            sb.append(String.format("%n"));
        }
        return sb.toString();
    }

    public Map<String, Set<String>> toMap() {
        Map<String, Set<String>> map = new LinkedHashMap<>();
        Set<Integer> selectedSet = new HashSet<>(selected);
        for (int from : selected) {
            Set<String> edges = new TreeSet<>();
            for (int to : edgesFrom.get(from)) {
                if (selectedSet.contains(to)) {
                    edges.add(nodes.get(to));
                }
            }
            map.put(nodes.get(from), edges);
        }
        return map;
    }

    public void toHtml(String htmlFile) {
        try (PrintStream out = new PrintStream(htmlFile);) {
                toHtml(new PrintStream(htmlFile));
            }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
    public void toHtml(PrintStream out) {
        Map<String, Supplier<AdjMatrix>> datas = new HashMap<>();
        datas.put("Name", () -> this);
        AdjMatrix.toHtml(out, datas);
    }

    public static void toHtml(String htmlFile, 
                              Map<String, Supplier<AdjMatrix>> dataNamesToAdjMatrix) {
        try (PrintStream out = new PrintStream(htmlFile);) {
                AdjMatrix.toHtml(new PrintStream(htmlFile), dataNamesToAdjMatrix);
            }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
    public static void toHtml(PrintStream out, 
                              Map<String, Supplier<AdjMatrix>> dataNamesToAdjMatrix) {
        try (BufferedReader htmlReader =
             new BufferedReader(new InputStreamReader
                                (AdjMatrix.class.getClassLoader()
                                 .getResourceAsStream("adjm/adjm.html")));
             BufferedReader jsReader =
             new BufferedReader(new InputStreamReader
                                (AdjMatrix.class.getClassLoader()
                                 .getResourceAsStream("adjm/adjm.js")));) {
                String line;
                while ((line = htmlReader.readLine()) != null) {
                    if (line.trim().equals("// {{{javascript}}}")) {
                        while ((line = jsReader.readLine()) != null) {
                            out.println(line);
                        }
                    } else if (line.equals("// {{{data}}}")) {
                        out.println("var data = {");

                        for (String dataName : dataNamesToAdjMatrix.keySet()) {
                            AdjMatrix adj = dataNamesToAdjMatrix.get(dataName).get();
                            out.println("\"" + dataName + "\" : {");

                            out.println("\"nodes\" : [");
                            for (String node : adj.nodes) {
                                out.println("\"" + node + "\",");
                            }
                            out.println("],");

                            out.println("\"edgesFrom\" : [");
                            for (Set<Integer> edges : adj.edgesFrom) {
                                out.print("[");
                                for (Integer edge : edges) {
                                    out.print(edge + ", ");
                                }
                                out.println("],");
                            }
                            out.println("],");

                            out.println("\"edgesTo\" : [");
                            for (Set<Integer> edges : adj.edgesTo) {
                                out.print("[");
                                for (Integer edge : edges) {
                                    out.print(edge + ", ");
                                }
                                out.println("],");
                            }
                            out.println("],");

                            out.println("\"nodesData\" : {");
                            for (Map.Entry<String, List<String>> entry : adj.nodesData.entrySet()) {
                                out.println("\"" + entry.getKey() + "\": [");
                                for (String data : entry.getValue()) {
                                    out.println("\"" + data + "\",");
                                }
                                out.println("],");
                            }
                            out.println("},");

                            out.println("\"orderings\" : {");
                            {
                                Map<String, Supplier<List<Integer>>> orderings = new LinkedHashMap<>();
                                orderings.put("Package", () -> adj.sortedByPackage().selected);
                                orderings.put("Name", () -> adj.sortedByName().selected);
                                orderings.put("Connections", () -> adj.sortedByDegree().selected);
                                orderings.put("Used", () -> adj.sortedByUsed().selected);
                                orderings.put("Uses", () -> adj.sortedByUses().selected);
                                orderings.put("Similarity", () -> adj.sortedBySimilarity().selected);

                                for (String dataKey : adj.nodesData.keySet()) {
                                    orderings.put(dataKey, () -> adj.sortedByNodeData(dataKey).selected);
                                }

                                for (String ordering : orderings.keySet()) {
                                    out.print("\"" + ordering + "\" : [");
                                    for (Integer i : orderings.get(ordering).get()) {
                                        out.print(i + ", ");
                                    }
                                    out.println("],");
                                }
                            }
                            out.println("},");

                            out.println("\"colors\" : {");
                            {
                                Map<String, Supplier<List<Color>>> colorings = new LinkedHashMap<>();
                                colorings.put("Package", () -> adj.getColorsByPackage());
                                colorings.put("Name", () -> adj.getColorsByName());
                                colorings.put("Connections", () -> adj.getColorsByConnections());

                                for (String dataKey : adj.nodesData.keySet()) {
                                    colorings.put(dataKey, () -> adj.getColorsByNodeData(dataKey));
                                }

                                for (String coloring : colorings.keySet()) {
                                    out.println("\"" + coloring + "\" : [");
                                    for (Color color : colorings.get(coloring).get()) {
                                        out.println("\"" + htmlColor(color) + "\",");
                                    }
                                    out.println("],");
                                }
                            }
                            out.println("},");
                            out.println("},");
                        }
                        out.println("};");
                    } else {
                        out.println(line);
                    }
                }
            }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String htmlColor(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static List<Integer> newIncList(int size) {
        List<Integer> out = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            out.add(i, i);
        }
        return out;
    }
}
