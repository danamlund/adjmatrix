package dk.danamlund.adjmatrix;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Main {
    public static void main(String[] argsArray) {
        List<String> args = Arrays.asList(argsArray);
        if (args.size() < 2 || args.contains("-h") || args.contains("--help")
            || args.contains("-help")) {
            System.out.println("Usage: java -jar adjmatrix.jar [-out output.html] [args] library-to-visualize.[jar|zip|class|folder]");
            System.out.println("   or: java -jar adjmatrix.jar [-out output.html] my-graph.tgf");
            System.out.println();
            System.out.println("Non-tgf files are passed to classycle, so these args work:");
            System.out.println("    [-includingClasses=<pattern1>[,<pattern2>,...]]");
            System.out.println("    [-excludingClasses=<pattern1>[,<pattern2>,...]]");
            System.out.println("    [-reflectionPattern=[<pattern1>,<pattern2>,...]]");
            System.out.println(" See: http://classycle.sourceforge.net/");
            return;
        }

        String output = "output.html";
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i).equals("-out")) {
                output = args.get(i + 1);
                args.remove(i + 1);
                args.remove(i);
                break;
            }
        }
        for (Iterator<String> it = args.iterator(); it.hasNext();) {
            if (it.next().equals("-out")) {
                it.remove();
                output = it.next();
                it.remove();
                break;
            }
        }

        if (args.size() == 1 && args.get(0).endsWith(".tgf")) {
            AdjMatrix adj = AdjMatrixTools.readTgf(args.get(0));
            Map<String, Supplier<AdjMatrix>> groupBy = new LinkedHashMap<>();
            groupBy.put("Node", () -> adj);
            AdjMatrix.toHtml(output, groupBy);
        } else {
            AdjMatrix adj = AdjMatrixTools.classycle(args.toArray(new String[0]));
            Map<String, Supplier<AdjMatrix>> groupBy = new LinkedHashMap<>();
            groupBy.put("Name", () -> adj);
            groupBy.put("Package", () -> adj.groupedByPackage());
            groupBy.put("jar", () -> adj.groupedByNodeData("jar"));
            AdjMatrix.toHtml(output, groupBy);
        }
    }
}
