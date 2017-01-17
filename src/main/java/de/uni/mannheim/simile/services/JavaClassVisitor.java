package de.uni.mannheim.simile.services;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class JavaClassVisitor extends VoidVisitorAdapter {

	@Getter
	private ClassOrInterfaceDeclaration classObj;
	@Getter
	private List<MethodDeclaration> methods = new ArrayList<>();
	@Getter
	private List<String> testClasses = new ArrayList<>();

	@Override
	public void visit(ClassOrInterfaceDeclaration n, Object arg) {
		super.visit(n, arg);
		this.classObj = n;
		if(n.toString().contains("@Test")) {
			testClasses.add(n.toString());
		}
	}

	@Override
	public void visit(MethodDeclaration n, Object arg) {
		super.visit(n, arg);
		System.out.println(String.format("L[%s] - %s", n.getBegin().get(), n.getDeclarationAsString()));
		methods.add(n);
	}

}
