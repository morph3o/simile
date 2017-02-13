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

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.google.common.base.Strings;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class JavaClassHandler implements FileHandler {

	private static final Logger logger = LoggerFactory.getLogger(JavaClassHandler.class);

	private List<String> methods = new ArrayList<>();
	@Getter
	private List<String> testClasses = new ArrayList<>();
	@Getter
	private Map<String, List<String>> classes = new HashMap<>();

	/**
	 * Handles the current Java class and for each method it contains,
	 * it transforms the method into Merobase Query Language format.
	 */
	@Override
	public void handle(int level, String path, File file) {
		logger.debug(path);
		logger.debug(Strings.repeat("=", path.length()));
		try {
			JavaClassVisitor jcv = new JavaClassVisitor();
			jcv.visit(JavaParser.parse(file), null);
			jcv.getMethods().forEach(method -> methods.add(this.getMethodMQLNotation(method.getNameAsString(), method.getParameters(), method.getType().toString())));
			classes.put(jcv.getClassObj().getNameAsString(), methods);
			methods = new ArrayList<>();
			if(jcv.getTestClasses().size() > 0)
				testClasses.addAll(jcv.getTestClasses());
		} catch (IOException e) {
			new RuntimeException(e);
		}
	}

	private String getMethodMQLNotation(String methodName, NodeList<Parameter> params, String returnType) {
		List<String> paramTypes = new ArrayList<>();
		params.forEach(param -> paramTypes.add(param.getType().toString()));
		logger.debug(
			String.format("%s(%s):%s", methodName, StringUtils.join(paramTypes, ','), returnType)
		);
		return String.format("%s(%s):%s", methodName, StringUtils.join(paramTypes, ','), returnType);
	}

	/**
	 * Returns the classes and the methods it cotains in MQL notation.
	 *
	 * <p>For example for the following class:
	 * <pre>
	 * public class TestClass {
	 * 	public String methodOne(int param){
	 * 	}
	 * }
	 * </pre>
	 * <p> This method will return:
	 * <pre>
	 * TestClass(methodOne(int):String;)
	 * </pre>
	 * */
	public List<String> getClassesMQLNotation() {
		List<String> classesMQLNotation = new ArrayList<>();
		if (classes.size() > 0) {
			classes.forEach((c, ms) -> {
				StrBuilder strBuilder = new StrBuilder();
				strBuilder.append(String.format("%s(", c));
				strBuilder.append(StringUtils.join(ms, ';'));
				strBuilder.append(String.format(";)"));
				logger.debug(strBuilder.toString());
				classesMQLNotation.add(strBuilder.toString());
			});
			return classesMQLNotation;
		}
		return Collections.emptyList();
	}
}
