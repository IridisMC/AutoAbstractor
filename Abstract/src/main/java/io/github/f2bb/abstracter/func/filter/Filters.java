package io.github.f2bb.abstracter.func.filter;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import io.github.f2bb.abstracter.AbstracterConfig;
import io.github.f2bb.abstracter.ex.InvalidClassException;
import io.github.f2bb.abstracter.util.asm.TypeUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.SignatureRemapper;
import org.objectweb.asm.signature.SignatureVisitor;

public interface Filters extends Opcodes {
	SignatureVisitor EMPTY_VISITOR = new SignatureVisitor(ASM9) {};
	Predicate<Type> IS_ABSTRACTED = t -> {
		SignatureRemapper remapper = new SignatureRemapper(EMPTY_VISITOR, TypeUtil.REMAPPER);
		try {
			TypeUtil.visit(remapper, t, false);
			return true;
		} catch (InvalidClassException e) {
			return false;
		}
	};

	Predicate<Class<?>> IS_CLASS_ABSTRACTED = AbstracterConfig::isInterfaceAbstracted;

	IntPredicate PROTECTED = Modifier::isProtected;
	IntPredicate ABSTRACT = Modifier::isAbstract;
	IntPredicate STATIC = Modifier::isStatic;
	IntPredicate PUBLIC = Modifier::isPublic;
	IntPredicate SYNTHETIC = i -> (i & ACC_SYNTHETIC) != 0;
	IntPredicate BRIDGE = i -> (i & ACC_BRIDGE) != 0;
	IntPredicate FINAL = Modifier::isFinal;
}