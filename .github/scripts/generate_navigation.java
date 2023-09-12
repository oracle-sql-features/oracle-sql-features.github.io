//JAVA 11+

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.util.Locale.ENGLISH;
import static java.util.stream.Collectors.groupingBy;

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

        var categoriesMap = new TreeMap<String, Set<Feature>>();
        var versionsMap = new TreeMap<String, Set<Feature>>();

        SourceFinder sourceFinder = new SourceFinder();
        Files.walkFileTree(featuresPath, sourceFinder);

        if (!sourceFinder.success) {
            System.err.println("❌ Unexpected error occurred while searching for feature files");
            System.exit(1);
        }

        if (!sourceFinder.duplicates.isEmpty()) {
            System.err.println("❌ Found duplicate feature files. Filenames must be unique. Rename and retry");
            sourceFinder.duplicates.forEach(System.err::println);
            System.exit(1);
        }

        // copy categories & versions
        System.out.printf("🛠  Generating categories%n");
        copyPartials(sourceFinder.categories.values(), "Categories", categoriesPath);
        System.out.printf("🛠  Generating versions%n");
        copyPartials(sourceFinder.versions.values(), "Versions", versionsPath);

        // generate include matrix for categories x versions and vice-versa
        generateIncludes(categoriesPath, sourceFinder.categories, sourceFinder.versions, "categories", "versions");
        generateIncludes(versionsPath, sourceFinder.versions, sourceFinder.categories, "versions", "categories");

        var features = new LinkedHashSet<Feature>();
        for (Map.Entry<String, Path> e : sourceFinder.features.entrySet()) {
            var page = e.getKey();
            var f = e.getValue();

            System.out.printf("➡️  Processing %s%n", page);
            var version = extractAttribute(f, ":database-version:");
            var categories = extractAttribute(f, ":database-category:").split(" ");
            var feature = new Feature(f, version, categories);
            features.add(feature);

            for (String category : categories) {
                categoriesMap.computeIfAbsent(category.toLowerCase(ENGLISH), c -> new TreeSet<>()).add(feature);
            }

            versionsMap.computeIfAbsent(version, v -> new TreeSet<>()).add(feature);
        }

        System.out.printf("🛠  Generating pages%n");
        generateNavigation(categoriesPath, categoriesMap, true);
        generateNavigation(versionsPath, versionsMap, false);
        copyFeatures(features, partialsPath);
    }

    private static void generateIncludes(Path path, Map<String, ? extends Item> outer, Map<String, ? extends Item> inner,
                                         String outerLabel, String innerLabel) throws IOException {
        var pages = path.resolve("pages");
        Files.createDirectories(pages);

        for (Item o : outer.values()) {
            var index = pages.resolve(o.getId().toLowerCase(ENGLISH)).resolve("index.adoc");
            Files.createDirectories(index.getParent());
            Files.write(index, ("include::" + outerLabel + ":partial$" + o.getId().toLowerCase(ENGLISH) + ".adoc[]" + lineSeparator()).getBytes(UTF_8));
            for (Item i : inner.values()) {
                index = pages.resolve(o.getId().toLowerCase(ENGLISH)).resolve(i.getId().toLowerCase(ENGLISH)).resolve("index.adoc");
                Files.createDirectories(index.getParent());
                Files.write(index, ("include::" + innerLabel + ":partial$" + i.getId().toLowerCase(ENGLISH) + ".adoc[]" + lineSeparator()).getBytes(UTF_8));
            }
        }
    }

    private static void generateNavigation(Path path, Map<String, Set<Feature>> data, boolean versions) throws IOException {
        StringBuilder indexNav = new StringBuilder("* xref:index.adoc[]");
        indexNav.append(lineSeparator());

        for (Map.Entry<String, Set<Feature>> e : data.entrySet()) {
            var classifier = e.getKey();
            var normalizedClassifier = classifier.toLowerCase(ENGLISH);
            var features = e.getValue();

            System.out.printf("🔖 Generating %s%n", normalizedClassifier);
            var index = path.resolve("pages/" + normalizedClassifier + "/index.adoc");
            // create page if it does not exist
            if (!Files.exists(index)) {
                Files.createDirectories(index.getParent());
                Files.write(index, ("= " + classifier + lineSeparator()).getBytes(UTF_8));
            }

            indexNav.append("** xref:")
                .append(normalizedClassifier)
                .append("/index.adoc[]")
                .append(lineSeparator());

            System.out.printf("🔖 Generating %s/features%n", normalizedClassifier);

            indexNav.append("*** All")
                .append(lineSeparator());

            for (Feature feature : features) {
                var featureFilename = feature.path.getFileName().toString();

                indexNav.append("**** xref:")
                    .append(normalizedClassifier)
                    .append("/features/")
                    .append(featureFilename)
                    .append("[]")
                    .append(lineSeparator());

                // create partial
                System.out.printf("📝 Generating %s/features/%s%n", normalizedClassifier, featureFilename);
                var partial = path.resolve("pages/" + normalizedClassifier + "/features/" + featureFilename);
                Files.createDirectories(partial.getParent());
                Files.write(partial, ("include::features:partial$" + featureFilename + "[]" + lineSeparator()).getBytes(UTF_8));
            }

            if (versions) {
                // versions
                Map<String, List<Feature>> versionedFeatures = features.stream()
                    .collect(groupingBy(Feature::getVersion));

                for (Map.Entry<String, List<Feature>> ve : versionedFeatures.entrySet()) {
                    String version = ve.getKey();

                    System.out.printf("🔖 Generating %s/%s%n", normalizedClassifier, version);

                    indexNav.append("*** xref:")
                        .append(normalizedClassifier)
                        .append("/")
                        .append(version)
                        .append("/index.adoc[]")
                        .append(lineSeparator());

                    for (Feature feature : ve.getValue()) {
                        var featureFilename = feature.path.getFileName().toString();

                        indexNav.append("**** xref:").
                            append(classifier)
                            .append("/")
                            .append(version)
                            .append("/")
                            .append(featureFilename)
                            .append("[]")
                            .append(lineSeparator());

                        // create partial
                        System.out.printf("📝 Generating %s/%s/%s%n", normalizedClassifier, version, featureFilename);
                        var partial = path.resolve("pages/" + normalizedClassifier + "/" + version + "/" + featureFilename);
                        Files.createDirectories(partial.getParent());
                        Files.write(partial, ("include::features:partial$" + featureFilename + "[]" + lineSeparator()).getBytes(UTF_8));
                    }
                }
            } else {
                var categorized = new TreeMap<String, Set<Feature>>();
                for (Feature feature : features) {
                    for (String category : feature.categories) {
                        categorized.computeIfAbsent(category.toLowerCase(ENGLISH), k -> new TreeSet<>())
                            .add(feature);
                    }
                }

                for (Map.Entry<String, Set<Feature>> k : categorized.entrySet()) {
                    var category = k.getKey().toLowerCase(ENGLISH);

                    System.out.printf("🔖 Generating %s/%s%n", classifier, category);

                    indexNav.append("*** xref:")
                        .append(classifier)
                        .append("/")
                        .append(category)
                        .append("/index.adoc[]")
                        .append(lineSeparator());

                    for (Feature feature : k.getValue()) {
                        var featureFilename = feature.path.getFileName().toString();

                        indexNav.append("**** xref:").
                            append(classifier)
                            .append("/")
                            .append(category)
                            .append("/")
                            .append(featureFilename)
                            .append("[]")
                            .append(lineSeparator());

                        // create partial
                        System.out.printf("📝 Generating %s/%s/%s%n", classifier, category, featureFilename);
                        var partial = path.resolve("pages/" + classifier + "/" + category + "/" + featureFilename);
                        Files.createDirectories(partial.getParent());
                        Files.write(partial, ("include::features:partial$" + featureFilename + "[]" + lineSeparator()).getBytes(UTF_8));
                    }
                }
            }
        }
        Files.write(path.resolve("nav.adoc"), indexNav.toString().getBytes(UTF_8));
    }

    private static void copyFeatures(Set<Feature> features, Path partials) throws IOException {
        deleteFiles(partials);
        Files.createDirectories(partials);

        for (Feature feature : features) {
            var content = new String(Files.readAllBytes(feature.path));
            content = content.replace("[[feature_summary]]", feature.asSummary());
            Files.write(partials.resolve(feature.path.getFileName()), content.getBytes(UTF_8));
        }
    }

    private static <I extends Item> void copyPartials(Collection<I> items, String title, Path path) throws IOException {
        var partials = path.resolve("partials");
        var pages = path.resolve("pages");

        deleteFiles(path);
        Files.createDirectories(partials);
        Files.createDirectories(pages);

        for (I item : items) {
            var content = new String(Files.readAllBytes(item.getPath()));
            Files.write(partials.resolve(item.getPath().getFileName()), content.getBytes(UTF_8));
        }

        var index = pages.resolve("index.adoc");
        var content = new StringBuilder("= ")
            .append(title)
            .append(lineSeparator())
            .append(lineSeparator());
        for (Item item : items) {
            content.append("* xref:")
                .append(item.getId().toLowerCase(ENGLISH))
                .append("/index.adoc[]")
                .append(lineSeparator());
        }
        Files.write(index, content.toString().getBytes(UTF_8));
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

    private static String extractTitle(Path file) {
        try (Stream<String> stream = Files.lines(file)) {
            Optional<String> line = stream.map(String::trim)
                .filter(s -> s.startsWith("= "))
                .findFirst();
            if (line.isEmpty()) {
                System.err.printf("❌ Missing title in %s%n", file.toAbsolutePath());
                System.exit(1);
            }

            return line.get().substring(2).trim();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
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

    private static class SourceFinder implements FileVisitor<Path> {
        private final Map<String, Category> categories = new TreeMap<>();
        private final Map<String, Version> versions = new TreeMap<>();
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
                if (file.getParent().getFileName().toString().equals("_categories")) {
                    String id = filename.substring(0, filename.length() - 5);
                    Category category = new Category(file, id);
                    categories.put(category.getTitle(), category);
                } else if (file.getParent().getFileName().toString().equals("_versions")) {
                    String version = filename.substring(0, filename.length() - 5);
                    versions.put(version, new Version(file, version));
                } else if (!features.containsKey(filename)) {
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

    public interface Item {
        String getId();

        String getTitle();

        Path getPath();
    }

    private static class Feature implements Comparable<Feature>, Item {
        private final Path path;
        private final String id;
        private final String version;
        private final String[] categories;
        private final String title;

        private Feature(Path path, String version, String[] categories) {
            this.path = path;
            this.version = version;
            this.categories = categories;
            this.title = extractTitle(path);
            var id = path.getFileName().toString();
            this.id = id.substring(0, id.length() - 5);
        }


        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public Path getPath() {
            return path;
        }

        public String getVersion() {
            return version;
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
                    .append(category.toLowerCase(ENGLISH))
                    .append("/index.adoc[]");
            }
            b.append(lineSeparator());

            return b.toString();
        }

        @Override
        public int compareTo(Feature o) {
            return title.compareTo(o.title);
        }
    }

    private static class Category implements Comparable<Category>, Item {
        private final Path path;
        private final String id;
        private final String title;

        private Category(Path path, String id) {
            this.path = path;
            this.id = id;
            this.title = extractTitle(path);
        }

        @Override
        public Path getPath() {
            return path;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public int compareTo(Category o) {
            return title.compareTo(o.title);
        }
    }

    private static class Version implements Comparable<Version>, Item {
        private final Path path;
        private final String version;
        private final String title;

        private Version(Path path, String version) {
            this.path = path;
            this.version = version;
            this.title = extractTitle(path);
        }

        @Override
        public String getId() {
            return version;
        }

        @Override
        public Path getPath() {
            return path;
        }

        public String getVersion() {
            return version;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public int compareTo(Version o) {
            return title.compareTo(o.title);
        }
    }
}