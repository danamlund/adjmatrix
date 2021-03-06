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

    // @Test
    // public void foo() {
    //     AdjMatrix adj = AdjMatrixTools.readClassycleRaw("~/down/Classycle1.4.2/test/out.rt.txt");
    //     // AdjMatrix adj = AdjMatrixTools.readClassycleRaw("~/Classycle1.4.2/test/foo.txt"); 
    //     Map<String, Supplier<AdjMatrix>> groupBy = new LinkedHashMap<>();
    //     groupBy.put("Name", () -> adj);
    //     groupBy.put("Package", () -> adj.groupedByPackage());
    //     adj.toHtml("test.html", groupBy);
    // }

    @Test
    public void testOutput() throws Exception {
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
                                           "interface A (586 bytes) sources: /jar.jar",
                                           "    interface B (640 bytes) sources: /jar.jar",
                                           "    interface C (640 bytes) sources: /jar.jar",
                                           "interface C (586 bytes) sources: /jar.jar",
                                           "    interface E (640 bytes) sources: /jar.jar",
                                           "interface E (586 bytes) sources: /jar.jar",
                                           "    interface D (640 bytes) sources: /jar.jar"
                                           ));
            AdjMatrix adj = AdjMatrixTools.readClassycleRaw(tmp.toAbsolutePath().toString());
            // adj.toHtml("test2.html");
        } finally {
            Files.delete(tmp);
        }
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
