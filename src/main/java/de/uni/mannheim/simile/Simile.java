package de.uni.mannheim.simile;

import com.google.common.base.Strings;
import de.uni.mannheim.simile.services.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class Simile {

	@Async
	public void searchForComponents(String repo, String branch, String folder) throws IOException, InterruptedException {
		Cloner cloner = new Cloner(repo, branch, folder);

		cloner.cloneRepository();

		File maintDir = new File(String.format("%s/src/main", folder));
		JavaClassHandler jch = new JavaClassHandler();
		new DirectoryExplorer(jch, new JavaClassFilter()).explore(maintDir);

		File testDir = new File(String.format("%s/src/test", folder));
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
