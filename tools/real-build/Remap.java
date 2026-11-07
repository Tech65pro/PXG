import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Remaps one jar between two namespaces using Tiny Remapper - the same library
 * Fabric Loom uses - driven from the command line so it needs no Gradle.
 *
 *   Remap <from> <to> <mappings.tiny> <in.jar> <out.jar> [cpJar...]
 */
public class Remap {
	public static void main(String[] args) throws Exception {
		String from = args[0];
		String to = args[1];
		Path mappings = Paths.get(args[2]);
		Path in = Paths.get(args[3]);
		Path out = Paths.get(args[4]);

		List<Path> classpath = new ArrayList<>();
		for (int i = 5; i < args.length; i++) classpath.add(Paths.get(args[i]));

		if (out.getParent() != null) Files.createDirectories(out.getParent());

		long t0 = System.currentTimeMillis();
		TinyRemapper remapper = TinyRemapper.newRemapper()
				.withMappings(TinyUtils.createTinyMappingProvider(mappings, from, to))
				.ignoreConflicts(true)
				.threads(Math.max(1, Runtime.getRuntime().availableProcessors() - 1))
				.build();
		System.out.println("mappings loaded in " + (System.currentTimeMillis() - t0) / 1000 + "s");

		if (!classpath.isEmpty()) {
			t0 = System.currentTimeMillis();
			remapper.readClassPath(classpath.toArray(new Path[0]));
			System.out.println("classpath read in " + (System.currentTimeMillis() - t0) / 1000 + "s");
		}

		t0 = System.currentTimeMillis();
		remapper.readInputs(in);

		final TreeMap<String, byte[]> collected = new TreeMap<>();
		remapper.apply((name, bytes) -> collected.put(name, bytes));
		remapper.finish();
		System.out.println("remapped in " + (System.currentTimeMillis() - t0) / 1000 + "s (" + collected.size() + " entries)");

		try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(out))) {
			for (var e : collected.entrySet()) {
				// Tiny Remapper hands back class entries as internal names without the
				// .class suffix; resources keep their original path. A class internal name
				// never contains a '.' in its last segment, so that is how they are told
				// apart.
				String name = e.getKey();
				int slash = name.lastIndexOf('/');
				if (name.indexOf('.', slash + 1) < 0) name = name + ".class";

				jos.putNextEntry(new JarEntry(name));
				jos.write(e.getValue());
				jos.closeEntry();
			}
		}
		System.out.println("wrote " + out);
	}
}
