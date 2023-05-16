//JAVA 11+

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.FileVisitResult.CONTINUE;

public class generate_navigation {
    public static void main(String... args) throws Exception {
        if (null == args || args.length != 1) {
            System.err.println("❌ Usage: java generate_navigation.java [DIRECTORY]");
            System.exit(1);
        }

        var pwd = Paths.get(args[0]);
        var featuresPath = Paths.get(pwd + "/features/");
        var categoriesPath = Paths.get(pwd + "/docs/modules/categories").normalize().toAbsolutePath();
        var versionsPath = Paths.get(pwd + "/docs/modules/versions").normalize().toAbsolutePath();
        var partialsPath = Paths.get(pwd + "/docs/modules/features/partials").normalize().toAbsolutePath();

        var categoriesMap = new TreeMap<String, Map<String, Path>>();
        var versionsMap = new TreeMap<String, Map<String, Path>>();

        FeatureFinder featureFinder = new FeatureFinder();
        Files.walkFileTree(featuresPath, featureFinder);

        if (!featureFinder.success) {
            System.err.println("❌ Unexpected error occurred while searching for feature files");
            System.exit(1);
        }

        if (!featureFinder.duplicates.isEmpty()) {
            System.err.println("❌ Found duplicate feature files. Filenames must be unique. Rename and retry");
            featureFinder.duplicates.forEach(System.err::println);
            System.exit(1);
        }

        var features = new LinkedHashSet<Feature>();
        for (Map.Entry<String, Path> e : featureFinder.features.entrySet()) {
            var page = e.getKey();
            var feature = e.getValue();

            System.out.printf("➡️  Processing %s%n", page);
            var version = extractAttribute(feature, ":database-version:");
            var categories = extractAttribute(feature, ":database-category:").split(" ");

            for (String category : categories) {
                categoriesMap.computeIfAbsent(category, c -> new TreeMap<>())
                    .put(page, feature);
            }

            versionsMap.computeIfAbsent(version, v -> new TreeMap<>())
                .put(page, feature);

            features.add(new Feature(feature, version, categories));
        }

        System.out.printf("🛠  Generating pages%n");
        generateNavigation(categoriesPath, categoriesMap);
        generateNavigation(versionsPath, versionsMap);
        copyFeatures(features, partialsPath);
    }

    private static void generateNavigation(Path path, Map<String, Map<String, Path>> data) throws IOException {
        StringBuilder b = new StringBuilder("* xref:index.adoc[]");
        b.append(lineSeparator());

        for (Map.Entry<String, Map<String, Path>> e : data.entrySet()) {
            var k = e.getKey();
            var v = e.getValue();

            System.out.printf("🔖 Generating %s%n", k);
            var index = path.resolve("pages/" + k + "/index.adoc");
            // create page if it does not exist
            if (!Files.exists(index)) {
                Files.createDirectories(index.getParent());
                Files.write(index, ("= " + k + lineSeparator()).getBytes(UTF_8));
            }

            b.append("** xref:" + k + "/index.adoc[]")
                .append(lineSeparator());
            for (Map.Entry<String, Path> g : v.entrySet()) {
                var f = g.getKey();
                var p = g.getValue();

                b.append("*** xref:" + k + "/" + f + "[]")
                    .append(lineSeparator());

                // create partial
                System.out.printf("📝 Generating %s/%s%n", k, f);
                var partial = path.resolve("pages/" + k + "/" + f);
                Files.write(partial, ("include::features:partial$" + f + "[]" + lineSeparator()).getBytes(UTF_8));
            }
        }
        Files.write(path.resolve("nav.adoc"), b.toString().getBytes(UTF_8));
    }

    private static void copyFeatures(LinkedHashSet<Feature> features, Path partials) throws IOException {
        deleteFiles(partials);
        Files.createDirectories(partials);

        for (Feature feature : features) {
            var content = new String(Files.readAllBytes(feature.path));
            content = content.replace("[[feature_summary]]", feature.asSummary());
            Files.write(partials.resolve(feature.path.getFileName()), content.getBytes(UTF_8));
        }
    }

    private static String extractAttribute(Path file, String attributeName) throws IOException {
        Optional<String> line = Files.lines(file)
            .filter(s -> s.startsWith(attributeName))
            .findFirst();

        if (line.isEmpty()) {
            System.err.printf("❌ Missing %s in %s%n", attributeName, file.toAbsolutePath());
            System.exit(1);
        }

        return line.get().substring(attributeName.length() + 1).trim();
    }

    private static void deleteFiles(Path path) throws IOException {
        if (Files.exists(path)) {
            try (Stream<Path> stream = Files.walk(path)) {
                stream
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
            Files.deleteIfExists(path);
        }
    }

    private static class FeatureFinder implements FileVisitor<Path> {
        private final Map<String, Path> features = new LinkedHashMap<>();
        private final Set<Path> duplicates = new LinkedHashSet<>();
        private boolean success = true;

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            String filename = file.getFileName().toString();
            if (filename.endsWith(".adoc")) {
                if (!features.containsKey(filename)) {
                    features.put(filename, file);
                } else {
                    duplicates.add(file);
                }
            }
            return CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException e) {
            success = false;
            return CONTINUE;
        }
    }

    private static class Feature {
        private final Path path;
        private final String version;
        private final String[] categories;

        private Feature(Path path, String version, String[] categories) {
            this.path = path;
            this.version = version;
            this.categories = categories;
        }

        private String asSummary() {
            StringBuilder b = new StringBuilder("[horizontal]");
            b.append(lineSeparator());

            b.append("Version:: ")
                .append("xref:versions:")
                .append(version)
                .append("/index.adoc[]")
                .append(lineSeparator());

            b.append("Categories:: ");
            for (int i = 0; i < categories.length; i++) {
                var category = categories[i];
                if (i > 0) b.append(", ");
                b.append("xref:categories:")
                    .append(category)
                    .append("/index.adoc[]");
            }
            b.append(lineSeparator());

            return b.toString();
        }
    }
}