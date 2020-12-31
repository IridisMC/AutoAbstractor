package io.github.astrarre.abstracter.abs;

import static io.github.astrarre.abstracter.util.ArrayUtil.map;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.function.Function;

import com.google.common.reflect.TypeToken;
import io.github.astrarre.abstracter.func.elements.ConstructorSupplier;
import io.github.astrarre.abstracter.func.elements.FieldSupplier;
import io.github.astrarre.abstracter.func.elements.MethodSupplier;
import io.github.astrarre.abstracter.func.inheritance.InterfaceFunction;
import io.github.astrarre.abstracter.func.inheritance.SuperFunction;
import io.github.astrarre.abstracter.func.map.TypeMappingFunction;
import io.github.astrarre.abstracter.util.asm.FieldUtil;
import io.github.astrarre.abstracter.util.asm.InvokeUtil;
import io.github.astrarre.abstracter.util.asm.MethodUtil;
import io.github.astrarre.abstracter.util.reflect.TypeUtil;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

@SuppressWarnings ("UnstableApiUsage")
public class InterfaceAbstracter extends AbstractAbstracter {
	private static final int REMOVE_FLAGS = ACC_ENUM | ACC_FINAL;
	private static final int ADD_FLAGS = ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT;

	public InterfaceAbstracter(Class<?> cls) {
		this(cls,
				getName(cls, "", 0),
				InterfaceFunction.INTERFACE_DEFAULT,
				ConstructorSupplier.INTERFACE_DEFAULT,
				FieldSupplier.INTERFACE_DEFAULT,
				MethodSupplier.INTERFACE_DEFAULT);
	}

	public InterfaceAbstracter(Class<?> cls,
			String name,
			InterfaceFunction interfaces,
			ConstructorSupplier supplier,
			FieldSupplier fieldSupplier,
			MethodSupplier methodSupplier) {
		super(cls, name, interfaces, SuperFunction.EMPTY, supplier, fieldSupplier, methodSupplier);
	}

	public InterfaceAbstracter(Class<?> cls, int version) {
		this(cls, getName(cls, "", version));
	}

	public InterfaceAbstracter(Class<?> cls, String name) {
		this(cls,
				name,
				InterfaceFunction.INTERFACE_DEFAULT,
				ConstructorSupplier.INTERFACE_DEFAULT,
				FieldSupplier.INTERFACE_DEFAULT,
				MethodSupplier.INTERFACE_DEFAULT);
	}

	@Override
	public int getAccess(int modifiers) {
		return (modifiers & ~REMOVE_FLAGS) | ADD_FLAGS;
	}

	@Override
	public void abstractConstructor(ClassNode node, Constructor<?> constructor, boolean impl) {
		Function<Type, TypeToken<?>> resolve = TypeMappingFunction.resolve(this.cls);
		TypeToken<?>[] params = map(constructor.getGenericParameterTypes(), resolve::apply, TypeToken[]::new);
		TypeToken<?> returnType = resolve.apply(this.cls);
		MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC,
				"newInstance",
				TypeUtil.methodDescriptor(params, returnType),
				TypeUtil.methodSignature(constructor.getTypeParameters(), params, returnType),
				null);
		if (impl) {
			InvokeUtil.invokeConstructor(this.name, method, constructor, true);
		} else {
			InvokeUtil.visitStub(method);
		}
		node.methods.add(method);
	}

	@Override
	public void abstractMethod(ClassNode node, Method method, boolean impl) {
		MethodUtil.abstractMethod(node, this.cls, method, impl, true);
	}

	@Override
	public void abstractField(ClassNode node, Field field, boolean impl) {
		int access = field.getModifiers();
		if ((access & ACC_ENUM) != 0) {
			FieldUtil.createConstant(node, this.cls, field, impl);
		} else {
			if (!Modifier.isFinal(access)) {
				MethodNode setter = FieldUtil.createSetter(this.name, this.cls, field, impl, true);
				if (!MethodUtil.conflicts(setter.name, setter.desc, node)) {
					setter.access &= ~ACC_FINAL;
					node.methods.add(setter);
				}
			}

			MethodNode getter = FieldUtil.createGetter(this.name, this.cls, field, impl, true);
			if (!MethodUtil.conflicts(getter.name, getter.desc, node)) {
				getter.access &= ~ACC_FINAL;
				node.methods.add(getter);
			}
		}
	}
}