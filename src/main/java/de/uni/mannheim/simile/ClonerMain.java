package de.uni.mannheim.simile;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.common.base.Strings;
import de.uni.mannheim.simile.services.Cloner;
import de.uni.mannheim.simile.services.DirectoryExplorer;
import de.uni.mannheim.simile.services.JavaClassHandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ClonerMain {

	private static final String REPO = "https://github.com/morph3o/simile.git";
	private static final String BRANCH = "master";
	private static final String FOLDER = "tmp";

	static final ArrayList<String> methods = new ArrayList<String>();

	public static void listMethods(File projectDir) {
		new DirectoryExplorer(new JavaClassHandler(), (level, path, file) -> {
			System.out.println(path);
			System.out.println(Strings.repeat("=", path.length()));
			try {
				new VoidVisitorAdapter<Object>() {
					@Override
					public void visit(MethodDeclaration n, Object arg) {
						super.visit(n, arg);
						System.out.println(String.format("L[%s] - %s", n.getBegin().get(), n.getDeclarationAsString()));
						methods.add(n.getDeclarationAsString());
					}
				}.visit(JavaParser.parse(file), null);
				System.out.println(); // empty line
			} catch (IOException e) {
				new RuntimeException(e);
			}
			return true;
		}).explore(projectDir);
	}

	public static void main(String[] args) throws IOException {
		Cloner cloner = new Cloner(REPO, BRANCH, FOLDER);

		cloner.cloneRepository();

		File projectDir = new File(String.format("%s/src", FOLDER));
		listMethods(projectDir);
	}

}
