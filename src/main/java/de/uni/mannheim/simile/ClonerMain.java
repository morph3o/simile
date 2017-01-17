package de.uni.mannheim.simile;

import com.google.common.base.Strings;
import de.uni.mannheim.simile.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class ClonerMain {

	private static final Logger LOG = LoggerFactory.getLogger(ClonerMain.class);

	private static final String REPO = "https://github.com/morph3o/simile.git";
	private static final String BRANCH = "master";
	private static final String FOLDER = "project";

	public static void main(String[] args) throws IOException, InterruptedException {
		Cloner cloner = new Cloner(REPO, BRANCH, FOLDER);

		cloner.cloneRepository();

		File maintDir = new File(String.format("%s/src/main", FOLDER));
		JavaClassHandler jch = new JavaClassHandler();
		new DirectoryExplorer(jch, new JavaClassFilter()).explore(maintDir);

		File testDir = new File(String.format("%s/src/test", FOLDER));
		JavaClassHandler jch2 = new JavaClassHandler();
		new DirectoryExplorer(jch2, new JavaClassFilter()).explore(testDir);

		System.out.println(String.format("Methods found in project: %s", jch.getMethods().size()));
		System.out.println(String.format("Test classes found in project: %s", jch2.getTestClasses().size()));

		System.out.println(String.format("Test class to search"));
		System.out.println(Strings.repeat("=", "Test class to search".length()));
		System.out.println(jch2.getTestClasses().get(1));
		new SocoraRequester().searchComponent(jch2.getTestClasses().get(1), SocoraRequester.TEST_DRIVEN_SEARCH);
	}

}
