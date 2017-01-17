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
