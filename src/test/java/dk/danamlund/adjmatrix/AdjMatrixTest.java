package dk.danamlund.adjmatrix;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class AdjMatrixTest {
    private List<String> nodes1 = Arrays.asList("A", "B", "C", "D", "E");
    private List<Set<Integer>> edges1 = 
        Arrays.asList(set(1, 2),
                      set(0, 3),
                      set(0, 3, 4),
                      set(1, 2, 4),
                      set(2, 3));
    private AdjMatrix adj1 = AdjMatrix.newInstance(nodes1, edges1);

    private List<String> nodes2 = Arrays.asList("A", "B", "C", "D", "E", "F", "G");
    private List<Set<Integer>> edges2 = 
        Arrays.asList(set(1, 2),
                      set(0, 2),
                      set(0, 1),
                      set(4, 5),
                      set(3, 5),
                      set(3, 4),
                      set());
    private AdjMatrix adj2 = AdjMatrix.newInstance(nodes2, edges2);

    @Test
    public void testShuffled() {
        for (AdjMatrix adj : Arrays.asList(adj1, adj1.selected(Arrays.asList(3, 2, 1)),
                                           adj2, adj2.selected(Arrays.asList(3, 2, 1)))) {
            for (int i = 0; i < 10; i++) {
                Assert.assertEquals(adj.toMap(), adj.shuffled().toMap());
            }
        }
    }

    @Test
    public void testSortedByName() {
        for (AdjMatrix adj : Arrays.asList(adj1, adj1.selected(Arrays.asList(3, 2, 1)),
                                           adj2, adj2.selected(Arrays.asList(3, 2, 1)))) {
            for (int i = 0; i < 10; i++) {
                Assert.assertEquals(adj.sortedByName().toMap(), 
                                    adj.shuffled().sortedByName().toMap());
            }
        }
    }

    @Test
    public void testSortedByDegree() {
        for (int i = 0; i < 10; i++) {
            List<String> nodes = adj1.shuffled().sortedByDegree().getSelectedNodes();
            Assert.assertTrue(nodes.toString(), nodes.indexOf("C") < nodes.indexOf("A"));
            Assert.assertTrue(nodes.indexOf("D") < nodes.indexOf("B"));
        }
    }

    @Test
    public void testSortedBySimilarity() {
        for (int i = 0; i < 10; i++) {
            AdjMatrix adj = adj2.shuffled().sortedBySimilarity();
            List<String> ns = adj.getSelectedNodes();
            Set<String> g0 = sets(ns.get(0), ns.get(1), ns.get(2));
            Set<String> g1 = sets(ns.get(3), ns.get(4), ns.get(5));
            Set<String> e0 = sets("A", "B", "C");
            Set<String> e1 = sets("D", "E", "F");
            Assert.assertTrue(e0.equals(g0) ^ e0.equals(g1));
            Assert.assertTrue(e1.equals(g0) ^ e1.equals(g1));
            Assert.assertEquals("G", ns.get(6));
        }        
    }

    @Test
    public void testSortedBySimilarity2() {
        for (int i = 0; i < 10; i++) {
            AdjMatrix adj = adj2.shuffled().sortedBySimilarity2();
            List<String> ns = adj.getSelectedNodes();
            Set<String> g0 = sets(ns.get(0), ns.get(1), ns.get(2));
            Set<String> g1 = sets(ns.get(3), ns.get(4), ns.get(5));
            Set<String> e0 = sets("A", "B", "C");
            Set<String> e1 = sets("D", "E", "F");
            Assert.assertTrue(e0.equals(g0) ^ e0.equals(g1));
            Assert.assertTrue(e1.equals(g0) ^ e1.equals(g1));
            Assert.assertEquals("G", ns.get(6));
        }        
    }

    @Test
    public void testGroupedByPackage() {
        AdjMatrix adj = AdjMatrix.newInstance
            (Arrays.asList("foo.A", "foo.B", "bar.C", "baz.D"),
             Arrays.asList(set(1, 2), set(0, 2, 3), set(3), set(2, 1)));

        Assert.assertEquals("{foo=[bar, baz], bar=[baz], baz=[bar, foo]}", 
                            adj.groupedByPackage().toMap().toString());
    }

    @Test
    public void testGroupedByNodeData() {
        AdjMatrix adj = AdjMatrix.newInstance
            (Arrays.asList("foo.A", "foo.B", "bar.C", "baz.D"),
             Arrays.asList(set(1, 2), set(0, 2, 3), set(3), set(2, 1)));
        Map<String, String> jarMap = new HashMap<>();
        jarMap.put("foo.A", "a.jar");
        jarMap.put("foo.B", "a.jar");
        jarMap.put("bar.C", "a.jar");
        jarMap.put("baz.D", "b.jar");
        adj.addNodeStringData("jar", jarMap); 

        Assert.assertEquals("{a.jar=[b.jar], b.jar=[a.jar]}", 
                            adj.groupedByNodeData("jar").toMap().toString());
    }
    
    private static Set<Integer> set(int... ints) {
        Set<Integer> output = new HashSet<>();
        for (int i : ints) {
            output.add(i);
        }
        return output;
    }

    private static Set<String> sets(String... elements) {
        Set<String> output = new HashSet<>();
        for (String e : elements) {
            output.add(e);
        }
        return output;
    }
}
