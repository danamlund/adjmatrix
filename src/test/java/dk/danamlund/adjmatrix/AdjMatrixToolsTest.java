package dk.danamlund.adjmatrix;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.Assert;
import org.junit.Test;

public class AdjMatrixToolsTest {

    @Test
    public void testFoo() {
        // AdjMatrix adj = AdjMatrixTools.readClassycleRaw("src/test/resources/classycleraw1.txt");

        // AdjMatrix adj = AdjMatrixTools.readClassycleRaw("src/test/resources/classycleraw3.txt");

        AdjMatrix adj = 
            AdjMatrixTools.readClassycleRaw("/home/vk/down/Classycle1.4.2/test/out.rt.txt");
        // AdjMatrix adj = 
        //     AdjMatrixTools.readClassycleRaw("/home/vk/down/Classycle1.4.2/test/out.gwt.txt");
        // AdjMatrix adj = 
        //     AdjMatrixTools.readClassycleRaw("/home/vk/down/Classycle1.4.2/test/out.hibernate.txt");
        // AdjMatrix adj = 
        //     AdjMatrixTools.readClassycleRaw("/home/vk/down/Classycle1.4.2/test/out.commons-lang.txt");
        // AdjMatrix adj = 
        //     AdjMatrixTools.readClassycleRaw("/home/vk/down/Classycle1.4.2/test/out.minecraft.txt");
        // AdjMatrix adj = 
        //     AdjMatrixTools.readClassycleRaw("/home/vk/down/Classycle1.4.2/test/out.guava.txt");
        // AdjMatrix adj = 
        //     AdjMatrixTools.readClassycleRaw("/home/vk/down/Classycle1.4.2/test/out.eclipse.txt");
        
        //System.out.println(adj.toMap().get("junit.extensions.ActiveTestSuite"));
        // System.out.println(adj.toMap().get("org.junit.runners.model.TestClass"));
        
        //System.out.println(adj.toMap().get("org.junit.runners.Parameterized"));
        // adj.toHtml("test.html");

        Map<String, Supplier<AdjMatrix>> groupBy = new LinkedHashMap<>();
        groupBy.put("Class", () -> adj);
        groupBy.put("Package", () -> adj.groupedByPackage());
        groupBy.put("Jar", () -> adj.groupedByNodeData("jar"));
        AdjMatrix.toHtml("test.html", groupBy);
        
        // System.out.println(adj.sortedBySimilarity().stringify());

        // AdjMatrix adj = 
        //     AdjMatrixTools.readClassycleRaw("/home/vk/down/Classycle1.4.2/test/out.rt.txt");
        // System.out.println("## " + adj.getSize());
        // System.out.println("## " + adj.sortedBySimilarity().getSize());
    }

    @Test
    public void testTgf() throws Exception {
        Path tmp = Files.createTempFile("AdjMatrixToolsTest", "tgf");
        try {
            Files.write(tmp, Arrays.asList("1 First node",
                                           "2 Second node",
                                           "#",
                                           "1 2 Edge between the two"));
            AdjMatrix adj = AdjMatrixTools.readTgf(tmp.toAbsolutePath().toString());
            
            Assert.assertEquals("{First node=[Second node], Second node=[]}", 
                                adj.toMap().toString());

        } finally {
            Files.delete(tmp);
        }
    }

    @Test
    public void testClassycleRaw() throws Exception {
        Path tmp = Files.createTempFile("AdjMatrixToolsTest", "txt");
        try {
            Files.write(tmp, Arrays.asList("============= Classycle V1.4.2 =============",
                                           "========== by Franz-Josef Elmer ==========",
                                           "read class files and create class graph ... done after 684 ms: 467 classes analysed.",
                                           "condense class graph ... done after 66 ms: 334 strong components found.",
                                           "calculate class layer indices ... done after 4 ms.",
                                           "create package graph ... done after 45 ms: 17 packages.",
                                           "condense package graph ... done after 1 ms: 17 strong components found.",
                                           "calculate package layer indices ... done after 0 ms.",
                                           "interface com.google.common.annotations.Beta (586 bytes) sources: /home/vk/down/guava-19.0.jar",
                                           "    unknown external class java.lang.Object",
                                           "    unknown external class java.lang.annotation.Annotation",
                                           "    unknown external class java.lang.annotation.Retention",
                                           "    unknown external class java.lang.annotation.RetentionPolicy",
                                           "    unknown external class java.lang.annotation.Target",
                                           "    unknown external class java.lang.annotation.ElementType",
                                           "    unknown external class java.lang.annotation.Documented",
                                           "    interface com.google.common.annotations.GwtCompatible (640 bytes) sources: /home/vk/down/guava-19.0.jar",
                                           "interface com.google.common.annotations.GwtCompatible (640 bytes) sources: /home/vk/down/guava-19.0.jar",
                                           "    unknown external class java.lang.Object",
                                           "    unknown external class java.lang.annotation.Annotation",
                                           "    unknown external class java.lang.annotation.Retention"
                                           ));
            AdjMatrix adj = AdjMatrixTools.readClassycleRaw(tmp.toAbsolutePath().toString());
            
            Assert.assertEquals("{com.google.common.annotations.Beta="
                                +"[com.google.common.annotations.GwtCompatible], "
                                +"com.google.common.annotations.GwtCompatible=[]}", 
                                adj.toMap().toString());

        } finally {
            Files.delete(tmp);
        }
    }
}
