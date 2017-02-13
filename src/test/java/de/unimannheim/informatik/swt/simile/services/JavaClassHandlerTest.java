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

package de.unimannheim.informatik.swt.simile.services;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaClassHandlerTest {

	private static final Logger logger = LoggerFactory.getLogger(JavaClassHandlerTest.class);

	private File base64JavaClass;
	private File collectionsJavaClass;
	private File testClass;

	@Before
	public void setUp() {
		this.base64JavaClass = new File(getClass().getClassLoader().getResource("Base64.java").getFile());
		this.collectionsJavaClass = new File(getClass().getClassLoader().getResource("Collections.java").getFile());
		this.testClass = new File(getClass().getClassLoader().getResource("TestClass.java").getFile());
	}

	@Test
	public void getClassesMQLNotation() throws Exception {
		JavaClassHandler jch = new JavaClassHandler();
		jch.handle(1, this.base64JavaClass.getAbsolutePath(), this.base64JavaClass);
		jch.handle(1, this.collectionsJavaClass.getAbsolutePath(), this.collectionsJavaClass);
		assertThat(jch.getClassesMQLNotation().size()).isEqualTo(2);
		assertThat(jch.getClassesMQLNotation().get(0)).isEqualTo("Base64(encode(byte[]):char[];decode(char[]):byte[];)");
		assertThat(jch.getClassesMQLNotation().get(1)).contains(
			"sort(List<T>):void;",
			"sort(List<T>,Comparator<? super T>):void;",
			"indexedBinarySearch(List<? extends Comparable<? super T>>,T):int;"
		);
	}

	@Test
	public void getTestClasses() throws Exception {
		JavaClassHandler jch = new JavaClassHandler();
		jch.handle(1, this.testClass.getAbsolutePath(), this.testClass);
		assertThat(jch.getTestClassesCode().size()).isEqualTo(1);
	}

	@Test
	public void getClasses() throws Exception {
		JavaClassHandler jch = new JavaClassHandler();
		jch.handle(1, this.base64JavaClass.getAbsolutePath(), this.base64JavaClass);
		jch.handle(1, this.collectionsJavaClass.getAbsolutePath(), this.collectionsJavaClass);
		assertThat(jch.getClasses().containsKey("Base64")).isEqualTo(true);
		assertThat(jch.getClasses().containsKey("Collections")).isEqualTo(true);
	}

}