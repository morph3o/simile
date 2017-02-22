/*
 * Copyright (c) 2017, Chair of Software Technology
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * •	Redistributions of source code must retain the above copyright notice,
 * 	this list of conditions and the following disclaimer.
 * •	Redistributions in binary form must reproduce the above copyright notice,
 * 	this list of conditions and the following disclaimer in the documentation
 * 	and/or other materials provided with the distribution.
 * •	Neither the name of the University Mannheim nor the names of its
 * 	contributors may be used to endorse or promote products derived from
 * 	this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package de.unimannheim.informatik.swt.simile;

import com.google.common.base.Strings;
import de.unimannheim.informatik.swt.simile.services.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class Simile {

	private static final Logger logger = LoggerFactory.getLogger(Simile.class);

	private final SocoraRequester socoraRequester;

	@Autowired
	public Simile(SocoraRequester socoraRequester) {
		this.socoraRequester = socoraRequester;
	}

	@Async
	public void searchForComponents(String repo, String branch, String folder, String recipient) throws IOException, InterruptedException {
		new Cloner(repo, branch, folder).cloneRepository();

		JavaClassFilter javaClassFilter = new JavaClassFilter();

		// Analyzing src/main directory looking for Classes
		File maintDir = new File(String.format("%s/src/main", folder));
		JavaClassHandler javaClassHandler = new JavaClassHandler();
		new DirectoryExplorer(javaClassHandler, javaClassFilter).explore(maintDir);

		// Analyzing src/test directory looking for test classes
		File testDir = new File(String.format("%s/src/test", folder));
		JavaClassHandler testClassHandler = new JavaClassHandler();
		new DirectoryExplorer(testClassHandler, javaClassFilter).explore(testDir);

		logger.info(String.format("Classes found in project: %s", javaClassHandler.getClasses().size()));
		logger.info(String.format("Test classes found in project: %s", testClassHandler.getTestClassesCode().size()));

		this.makeRequestWithClasses(javaClassHandler.getClassesMQLNotation(), recipient);
		this.makeRequestWithTestClasses(testClassHandler.getTestClassesCode(), recipient);
	}

	private void makeRequestWithClasses(List<String> classes, String recipientEmail) {
		logger.info("Interface-driven Search");
		logger.info(Strings.repeat("=", "Interface-driven Search".length()));
		classes.forEach(c -> {
			try {
				logger.info(String.format("Class to query: %s", c));
				socoraRequester.searchComponent(c, SocoraRequester.INTERFACE_DRIVEN_SEARCH, recipientEmail);
			} catch (Exception ex) {
				logger.error(ExceptionUtils.getStackTrace(ex));
			}
		});
	}

	private void makeRequestWithTestClasses(List<String> testClasses, String recipientEmail) {
		logger.info("Test-driven Search");
		logger.info(Strings.repeat("=", "Test-driven Search".length()));
		testClasses.forEach(tc -> {
			try {
				logger.info(String.format("Test Class to query: %s", tc));
				socoraRequester.searchComponent(tc, SocoraRequester.TEST_DRIVEN_SEARCH, recipientEmail);
			} catch (Exception ex) {
				logger.error(ExceptionUtils.getStackTrace(ex));
			}
		});
	}

}
