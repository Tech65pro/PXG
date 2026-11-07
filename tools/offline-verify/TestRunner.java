import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Runs the JUnit 5 tests in {@code src/test/java} without JUnit on the classpath.
 *
 * <p>This exists because the environment these sources were first verified in has no access to
 * Maven Central, so the real JUnit platform could not be downloaded. It reflects over classes
 * whose names end in {@code Test}, invokes every {@code @Test} method (with {@code @BeforeEach}
 * and {@code @AfterEach} honoured), and exits non-zero if anything fails. Under Gradle the same
 * test classes run on the real JUnit platform via {@code gradle test}.</p>
 *
 * <p>Not part of the mod; it lives under {@code tools/} and is never shipped in the jar.</p>
 */
public final class TestRunner {

    private TestRunner() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length > 0 ? args[0] : "src/test/java");
        List<Class<?>> classes = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> files = paths.filter(p -> p.toString().endsWith("Test.java"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
            for (Path file : files) {
                String name = root.relativize(file).toString()
                        .replace('/', '.').replace('\\', '.');
                name = name.substring(0, name.length() - ".java".length());
                classes.add(Class.forName(name));
            }
        }

        int passed = 0;
        List<String> failures = new ArrayList<>();
        for (Class<?> testClass : classes) {
            System.out.println("== " + testClass.getSimpleName() + " ==");
            List<Method> tests = new ArrayList<>();
            List<Method> before = new ArrayList<>();
            List<Method> after = new ArrayList<>();
            for (Method method : testClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Test.class)) {
                    tests.add(method);
                } else if (method.isAnnotationPresent(BeforeEach.class)) {
                    before.add(method);
                } else if (method.isAnnotationPresent(AfterEach.class)) {
                    after.add(method);
                }
            }
            tests.sort(Comparator.comparing(Method::getName));
            before.sort(Comparator.comparing(Method::getName));
            after.sort(Comparator.comparing(Method::getName));

            for (Method test : tests) {
                String label = test.getName();
                DisplayName display = test.getAnnotation(DisplayName.class);
                if (display != null) {
                    label = display.value();
                }
                var constructor = testClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                Object instance = constructor.newInstance();
                try {
                    for (Method setup : before) {
                        setup.setAccessible(true);
                        setup.invoke(instance);
                    }
                    test.setAccessible(true);
                    test.invoke(instance);
                    for (Method teardown : after) {
                        teardown.setAccessible(true);
                        teardown.invoke(instance);
                    }
                    System.out.println("  PASS  " + label);
                    passed++;
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    System.out.println("  FAIL  " + label);
                    System.out.println("        " + cause);
                    failures.add(testClass.getSimpleName() + "." + test.getName() + ": " + cause);
                }
            }
        }

        System.out.println();
        System.out.println(passed + " test(s) passed, " + failures.size() + " failed");
        if (!failures.isEmpty()) {
            for (String failure : failures) {
                System.out.println("  " + failure);
            }
            System.exit(1);
        }
    }
}
