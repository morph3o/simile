package de.uni.mannheim.simile.services;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class JavaMethodVisitor extends VoidVisitorAdapter {

	@Getter
	private String name;
	@Getter
	private String returnType;
	@Getter
	private String parameterTypes;
	private List<String> params = new ArrayList<>();
	@Getter
	private NodeList<Parameter> parameters;

	@Override
	public void visit(MethodDeclaration n, Object arg) {
		super.visit(n, arg);
		System.out.println(String.format("L[%s] - %s", n.getBegin().get(), n.getDeclarationAsString()));
		this.name = n.getName().toString();
		this.returnType = n.getType().toString();
		n.getParameters().forEach(param -> params.add(param.getType().toString()));
		parameterTypes = StringUtils.join(params, ',');
		parameters = n.getParameters();
	}

}
