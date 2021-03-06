package io.github.astrarre.abstracter.abs.method;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import io.github.astrarre.abstracter.AbstracterConfig;
import io.github.astrarre.abstracter.abs.AbstractAbstracter;
import io.github.astrarre.abstracter.util.AsmUtil;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import net.fabricmc.mapping.tree.MethodDef;

public class BaseMethodAbstracter extends MethodAbstracter<Method> {

	public BaseMethodAbstracter(AbstracterConfig config, AbstractAbstracter abstracter, Method method, boolean impl) {
		super(config, abstracter, method, impl);
	}

	@Override
	public MethodNode abstractMethod(ClassNode header) {
		MethodNode node = super.abstractMethod(header);
		if (this.impl) {
			int access = this.member.getModifiers();
			if (!Modifier.isFinal(access) && !Modifier.isStatic(access)) {
				this.visitBridge(header, this.member, node.name, node.desc);
			}
		} else {
			AsmUtil.visitStub(node);
		}
		return node;
	}

	@Override
	protected void invokeTarget(MethodNode node) {
		Class<?> target;
		if (Modifier.isStatic(this.member.getModifiers())) {
			target = this.member.getDeclaringClass();
		} else {
			target = this.abstracter.getCls(config);
		}

		this.invoke(node,
				Type.getInternalName(target),
				this.member.getName(),
				Type.getMethodDescriptor(this.member),
				this.getOpcode(this.member, INVOKESPECIAL));
	}

	@Override
	protected int loadThis(MethodNode node) {
		node.visitVarInsn(ALOAD, 0);
		return 1;
	}

	private MethodNode visitBridge(ClassNode header, Method method, String targetName, String targetDesc) {
		int access = method.getModifiers();
		MethodDef mappings = this.config.getMethod(method);
		MethodNode node = new MethodNode((access & ~ACC_ABSTRACT) | ACC_FINAL | ACC_BRIDGE | ACC_SYNTHETIC,
				targetName,
				targetDesc,
				null /*sign*/,
				null);

		// triangular method
		this.invoke(node, header.name, targetName, targetDesc, this.getOpcode(this.member, INVOKEVIRTUAL));
		node.name = mappings.getName("intermediary");

		if (!(node.name.equals(targetName) && node.desc.equals(targetDesc))) {
			header.methods.add(node);
		}
		return node;
	}
}
