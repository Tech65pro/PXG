import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

/**
 * Rewrites the class-file version of every class in a jar, leaving everything
 * else - names, descriptors, constant pool, frames - untouched.
 *
 * This exists so that a JDK 17 javac can compile against the real Minecraft
 * 1.21.11 classes, which Mojang ships as Java 21 bytecode (version 65). The
 * content is byte-for-byte the real API; only the version header changes, so
 * javac 17 can parse it. Class files emitted by javac 17 (version 61) load
 * fine on the Java 21 runtime Minecraft 1.21.11 requires.
 *
 *   Downgrade <in.jar> <out.jar> [targetVersion]
 */
public class Downgrade {
	public static void main(String[] args) throws Exception {
		Path in = Paths.get(args[0]);
		Path out = Paths.get(args[1]);
		int target = args.length > 2 ? Integer.parseInt(args[2]) : Opcodes.V17;

		if (out.getParent() != null) Files.createDirectories(out.getParent());

		int count = 0;
		int resources = 0;
		try (JarInputStream jis = new JarInputStream(Files.newInputStream(in));
				JarOutputStream jos = new JarOutputStream(Files.newOutputStream(out))) {
			JarEntry entry;
			while ((entry = jis.getNextJarEntry()) != null) {
				if (entry.isDirectory()) continue;
				byte[] data = readAll(jis);

				if (entry.getName().endsWith(".class")) {
					ClassReader cr = new ClassReader(data);
					ClassWriter cw = new ClassWriter(0);
					cr.accept(new VersionForcer(cw, target), 0);
					data = cw.toByteArray();
					count++;
				} else {
					resources++;
				}

				jos.putNextEntry(new JarEntry(entry.getName()));
				jos.write(data);
				jos.closeEntry();
			}
		}
		System.out.println("downgraded " + count + " classes (+" + resources + " resources) -> " + out);
	}

	/** Passes everything through except the version word in visit(). */
	static final class VersionForcer extends ClassVisitor {
		private final int version;

		VersionForcer(ClassVisitor cv, int version) {
			super(Opcodes.ASM9, cv);
			this.version = version;
		}

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			super.visit(this.version, access, name, signature, superName, interfaces);
		}
	}

	static byte[] readAll(InputStream is) throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buf = new byte[65536];
		int n;
		while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
		return bos.toByteArray();
	}
}
