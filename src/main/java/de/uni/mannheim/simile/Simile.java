package de.uni.mannheim.simile;

import com.google.common.base.Strings;
import com.sparkpost.exception.SparkPostException;
import de.uni.mannheim.simile.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class Simile {

	@Autowired
	private SocoraRequester socoraRequester;

	@Async
	public void searchForComponents(String repo, String branch, String folder) throws IOException, InterruptedException, SparkPostException {
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
		System.out.println(jch.getMethods().get(0));
		socoraRequester.searchComponent("SocoraRequester(searchComponent(String,String):void;)", SocoraRequester.INTERFACE_DRIVEN_SEARCH);
	}

}
