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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

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
		Cloner cloner = new Cloner(repo, branch, folder);

		cloner.cloneRepository();

		File maintDir = new File(String.format("%s/src/main", folder));
		JavaClassHandler jch = new JavaClassHandler();
		new DirectoryExplorer(jch, new JavaClassFilter()).explore(maintDir);

		File testDir = new File(String.format("%s/src/test", folder));
		JavaClassHandler jch2 = new JavaClassHandler();
		new DirectoryExplorer(jch2, new JavaClassFilter()).explore(testDir);

		logger.info(String.format("Methods found in project: %s", jch.getMethods().size()));
		logger.info(String.format("Test classes found in project: %s", jch2.getTestClasses().size()));

		logger.info(String.format("Test class to search"));
		logger.info(Strings.repeat("=", "Test class to search".length()));

		socoraRequester.searchComponent(jch.getMethods().get(0), SocoraRequester.INTERFACE_DRIVEN_SEARCH, recipient);
		socoraRequester.searchComponent(jch2.getTestClasses().get(0), SocoraRequester.TEST_DRIVEN_SEARCH, recipient);
	}

}
