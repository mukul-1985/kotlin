/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal

import kotlin.reflect.*

public class ClassReference(override val jClass: Class<*>) : KClass<Any>, ClassBasedDeclarationContainer {
    override val simpleName: String?
        get() = getClassSimpleName(jClass)

    override val qualifiedName: String?
        get() = getClassQualifiedName(jClass)

    override val members: Collection<KCallable<*>>
        get() = error()

    override val constructors: Collection<KFunction<Any>>
        get() = error()

    override val nestedClasses: Collection<KClass<*>>
        get() = error()

    override val annotations: List<Annotation>
        get() = error()

    override val objectInstance: Any?
        get() = error()

    @SinceKotlin("1.1")
    override fun isInstance(value: Any?): Boolean =
        isInstance(value, jClass)

    @SinceKotlin("1.1")
    override val typeParameters: List<KTypeParameter>
        get() = error()

    @SinceKotlin("1.1")
    override val supertypes: List<KType>
        get() = error()

    @SinceKotlin("1.3")
    override val sealedSubclasses: List<KClass<out Any>>
        get() = error()

    @SinceKotlin("1.1")
    override val visibility: KVisibility?
        get() = error()

    @SinceKotlin("1.1")
    override val isFinal: Boolean
        get() = error()

    @SinceKotlin("1.1")
    override val isOpen: Boolean
        get() = error()

    @SinceKotlin("1.1")
    override val isAbstract: Boolean
        get() = error()

    @SinceKotlin("1.1")
    override val isSealed: Boolean
        get() = error()

    @SinceKotlin("1.1")
    override val isData: Boolean
        get() = error()

    @SinceKotlin("1.1")
    override val isInner: Boolean
        get() = error()

    @SinceKotlin("1.1")
    override val isCompanion: Boolean
        get() = error()

    private fun error(): Nothing = throw KotlinReflectionNotSupportedError()

    override fun equals(other: Any?) =
        other is ClassReference && javaObjectType == other.javaObjectType

    override fun hashCode() =
        javaObjectType.hashCode()

    override fun toString() =
        jClass.toString() + Reflection.REFLECTION_NOT_AVAILABLE

    companion object {
        private val FUNCTION_CLASSES =
            listOf(
                Function0::class.java, Function1::class.java, Function2::class.java, Function3::class.java, Function4::class.java,
                Function5::class.java, Function6::class.java, Function7::class.java, Function8::class.java, Function9::class.java,
                Function10::class.java, Function11::class.java, Function12::class.java, Function13::class.java, Function14::class.java,
                Function15::class.java, Function16::class.java, Function17::class.java, Function18::class.java, Function19::class.java,
                Function20::class.java, Function21::class.java, Function22::class.java
            ).mapIndexed { i, clazz -> clazz to i }.toMap()

        private val primitiveFqNames = hashMapOf(
            "boolean" to "kotlin.Boolean",
            "char" to "kotlin.Char",
            "byte" to "kotlin.Byte",
            "short" to "kotlin.Short",
            "int" to "kotlin.Int",
            "float" to "kotlin.Float",
            "long" to "kotlin.Long",
            "double" to "kotlin.Double"
        )

        private val primitiveWrapperFqNames = hashMapOf(
            "java.lang.Boolean" to "kotlin.Boolean",
            "java.lang.Character" to "kotlin.Char",
            "java.lang.Byte" to "kotlin.Byte",
            "java.lang.Short" to "kotlin.Short",
            "java.lang.Integer" to "kotlin.Int",
            "java.lang.Float" to "kotlin.Float",
            "java.lang.Long" to "kotlin.Long",
            "java.lang.Double" to "kotlin.Double"
        )

        // See JavaToKotlinClassMap.
        private val classFqNames = hashMapOf(
            "java.lang.Object" to "kotlin.Any",
            "java.lang.String" to "kotlin.String",
            "java.lang.CharSequence" to "kotlin.CharSequence",
            "java.lang.Throwable" to "kotlin.Throwable",
            "java.lang.Cloneable" to "kotlin.Cloneable",
            "java.lang.Number" to "kotlin.Number",
            "java.lang.Comparable" to "kotlin.Comparable",
            "java.lang.Enum" to "kotlin.Enum",
            "java.lang.annotation.Annotation" to "kotlin.Annotation",
            "java.lang.Iterable" to "kotlin.collections.Iterable",
            "java.util.Iterator" to "kotlin.collections.Iterator",
            "java.util.Collection" to "kotlin.collections.Collection",
            "java.util.List" to "kotlin.collections.List",
            "java.util.Set" to "kotlin.collections.Set",
            "java.util.ListIterator" to "kotlin.collections.ListIterator",
            "java.util.Map" to "kotlin.collections.Map",
            "java.util.Map\$Entry" to "kotlin.collections.Map.Entry",
            "kotlin.jvm.internal.StringCompanionObject" to "kotlin.String.Companion",
            "kotlin.jvm.internal.EnumCompanionObject" to "kotlin.Enum.Companion"
        ).apply {
            putAll(primitiveFqNames)
            putAll(primitiveWrapperFqNames)
            primitiveFqNames.values.associateTo(this) { kotlinName ->
                "kotlin.jvm.internal.${kotlinName.substringAfterLast('.')}CompanionObject" to "$kotlinName.Companion"
            }
            for ((klass, arity) in FUNCTION_CLASSES) {
                put(klass.name, "kotlin.Function$arity")
            }
        }

        private val simpleNames = classFqNames.mapValues { (_, fqName) -> fqName.substringAfterLast('.') }

        public fun getClassSimpleName(jClass: Class<*>): String? = when {
            jClass.isAnonymousClass -> null
            jClass.isLocalClass -> {
                val name = jClass.simpleName
                jClass.enclosingMethod?.let { method -> name.substringAfter(method.name + "$") }
                    ?: jClass.enclosingConstructor?.let { constructor -> name.substringAfter(constructor.name + "$") }
                    ?: name.substringAfter('$')
            }
            jClass.isArray -> {
                val componentType = jClass.componentType
                when {
                    componentType.isPrimitive -> simpleNames[componentType.name]?.plus("Array")
                    else -> null
                } ?: "Array"
            }
            else -> simpleNames[jClass.name] ?: jClass.simpleName
        }

        public fun getClassQualifiedName(jClass: Class<*>): String? = when {
            jClass.isAnonymousClass -> null
            jClass.isLocalClass -> null
            jClass.isArray -> {
                val componentType = jClass.componentType
                when {
                    componentType.isPrimitive -> classFqNames[componentType.name]?.plus("Array")
                    else -> null
                } ?: "kotlin.Array"
            }
            else -> classFqNames[jClass.name] ?: jClass.canonicalName
        }

        public fun isInstance(value: Any?, jClass: Class<*>): Boolean {
            FUNCTION_CLASSES[jClass]?.let { arity ->
                return TypeIntrinsics.isFunctionOfArity(value, arity)
            }
            val objectType = if (jClass.isPrimitive) jClass.kotlin.javaObjectType else jClass
            return objectType.isInstance(value)
        }
    }
}
