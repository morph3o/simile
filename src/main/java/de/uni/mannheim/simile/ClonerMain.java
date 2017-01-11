package de.uni.mannheim.simile;

import de.uni.mannheim.simile.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class ClonerMain {

	private static final Logger LOG = LoggerFactory.getLogger(ClonerMain.class);

	private static final String REPO = "https://github.com/morph3o/simile.git";
	private static final String BRANCH = "master";
	private static final String FOLDER = "tmp";

	public static void main(String[] args) throws IOException {
		Cloner cloner = new Cloner(REPO, BRANCH, FOLDER);

		cloner.cloneRepository();

		File projectDir = new File(String.format("%s/src", FOLDER));
		JavaClassHandler jch = new JavaClassHandler();
		DirectoryExplorer dirExplorer = new DirectoryExplorer(jch, new JavaClassFilter());
		dirExplorer.explore(projectDir);

		System.out.println(jch.getMethods().get(0));
		SocoraRequester.searchComponent(jch.getMethods().get(0));
	}

}
