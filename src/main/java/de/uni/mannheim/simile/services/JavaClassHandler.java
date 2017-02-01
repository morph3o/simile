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

package de.uni.mannheim.simile.services;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.google.common.base.Strings;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JavaClassHandler implements FileHandler {

	@Getter
	private List<String> methods = new ArrayList<>();
	@Getter
	private List<String> testClasses = new ArrayList<>();

	@Override
	public void handle(int level, String path, File file) {
		System.out.println(path);
		System.out.println(Strings.repeat("=", path.length()));
		try {
			JavaClassVisitor jcv = new JavaClassVisitor();
			jcv.visit(JavaParser.parse(file), null);
			jcv.getMethods().forEach(method -> methods.add(this.getMQLNotation(jcv.getClassObj().getNameAsString(), method.getNameAsString(), method.getParameters(), method.getType().toString())));
			testClasses.addAll(jcv.getTestClasses());
			System.out.println();
		} catch (IOException e) {
			new RuntimeException(e);
		}
	}

	private String getMQLNotation(String classname, String methodName, NodeList<Parameter> params, String returnType) {
		List<String> paramTypes = new ArrayList<>();
		params.forEach(param -> paramTypes.add(param.getType().toString()));
		System.out.println(String.format("%s(%s(%s):%s;)", classname, methodName, StringUtils.join(paramTypes, ','), returnType));
		return String.format("%s(%s(%s):%s;)", classname, methodName, StringUtils.join(paramTypes, ','), returnType);
	}
}
