/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.descriptors

import org.jetbrains.kotlin.backend.common.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.backend.common.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeBuilder
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.impl.originalKotlinType
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.SmartList

class IrBuiltIns(
    val builtIns: KotlinBuiltIns,
    private val typeTranslator: TypeTranslator,
    outerSymbolTable: SymbolTable? = null
) {
    val languageVersionSettings = typeTranslator.languageVersionSettings

    private val builtInsModule = builtIns.builtInsModule

    interface IrBuiltinWithMangle : IrDeclaration, IrSymbolOwner {
        val mangle: String
    }

    class IrBuiltInOperator(
        override val symbol: IrSimpleFunctionSymbol,
        name: Name,
        returnType: IrType,
        val suffix: String
    ) : IrSimpleFunction, IrBuiltinWithMangle, IrFunctionBase(
        UNDEFINED_OFFSET, UNDEFINED_OFFSET, BUILTIN_OPERATOR, name, Visibilities.PUBLIC, false, false, false, returnType
    ) {
        override val modality get() = Modality.FINAL
        override val isTailrec get() = false
        override val isSuspend get() = false
        override var correspondingProperty: IrProperty?
            get() = null
            set(_) {}
        override var correspondingPropertySymbol: IrPropertySymbol?
            get() = null
            set(_) {}
        override val descriptor: FunctionDescriptor get() = symbol.descriptor

        override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
            return visitor.visitSimpleFunction(this, data)
        }

        override val overriddenSymbols: MutableList<IrSimpleFunctionSymbol> = SmartList()
        override val mangle: String get() = "operator#$name@$suffix"

        init {
            symbol.bind(this)
        }
    }

    class IrBuiltInOperatorValueParameter(override val symbol: IrValueParameterSymbol, override val index: Int, override val type: IrType) :
        IrValueParameter, IrDeclarationBase(UNDEFINED_OFFSET, UNDEFINED_OFFSET, BUILTIN_OPERATOR) {
        override val descriptor: ParameterDescriptor get() = symbol.descriptor
        override val varargElementType: IrType? get() = null
        override val isCrossinline: Boolean get() = false
        override val isNoinline: Boolean get() = false
        override var defaultValue: IrExpressionBody?
            get() = null
            set(_) {}
        override val name: Name = Name.identifier("arg$index")

        override fun <D> transform(transformer: IrElementTransformer<D>, data: D) =
            transformer.visitValueParameter(this, data) as IrValueParameter

        override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R = visitor.visitValueParameter(this, data)
        override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {}
        override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {}

        init {
            symbol.bind(this)
        }
    }

    class IrBuiltInOperatorTypeParameter(
        override val symbol: IrTypeParameterSymbol,
        override val variance: Variance,
        override val index: Int,
        override val isReified: Boolean
    ) : IrTypeParameter, IrDeclarationBase(UNDEFINED_OFFSET, UNDEFINED_OFFSET, BUILTIN_OPERATOR) {
        override val descriptor: TypeParameterDescriptor get() = symbol.descriptor
        override val superTypes: MutableList<IrType> = SmartList()
        override val name: Name = Name.identifier("T$index")

        override fun <D> transform(transformer: IrElementTransformer<D>, data: D): IrTypeParameter =
            transformer.visitTypeParameter(this, data) as IrTypeParameter

        override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R = visitor.visitTypeParameter(this, data)
        override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {}
        override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {}

        init {
            symbol.bind(this)
        }
    }

    val irBuiltInsSymbols = mutableListOf<IrBuiltinWithMangle>()

    private val symbolTable = outerSymbolTable ?: SymbolTable()

    private val packageFragmentDescriptor = IrBuiltinsPackageFragmentDescriptorImpl(builtInsModule, KOTLIN_INTERNAL_IR_FQN)
    private val packageFragment =
        IrExternalPackageFragmentImpl(symbolTable.referenceExternalPackageFragment(packageFragmentDescriptor), KOTLIN_INTERNAL_IR_FQN)

    private fun ClassDescriptor.toIrSymbol() = symbolTable.referenceClass(this)
    private fun KotlinType.toIrType() = typeTranslator.translateType(this)

    fun defineOperator(name: String, returnType: IrType, valueParameterTypes: List<IrType>): IrSimpleFunctionSymbol {
        val descriptor = WrappedSimpleFunctionDescriptor()
        val symbol = symbolTable.declareSimpleFunction(UNDEFINED_OFFSET, UNDEFINED_OFFSET, BUILTIN_OPERATOR, descriptor) {
            val suffix = valueParameterTypes.joinToString(":", "[", "]") { t -> t.originalKotlinType?.toString() ?: "T" }
            val operator = IrBuiltInOperator(it, Name.identifier(name), returnType, suffix)
            operator.parent = packageFragment
            packageFragment.declarations += operator
            descriptor.bind(operator)

            valueParameterTypes.mapIndexedTo(operator.valueParameters) { i, t ->
                val vpd = WrappedValueParameterDescriptor()
                val vps = IrValueParameterSymbolImpl(vpd)
                IrBuiltInOperatorValueParameter(vps, i, t).apply {
                    vpd.bind(this)
                    parent = operator
                }
            }

            irBuiltInsSymbols += operator

            operator
        }

        return symbol.symbol
    }

    private fun defineEnumValueOfOperator(): IrSimpleFunctionSymbol {
        val name = Name.identifier("enumValueOf")
        val tpd: TypeParameterDescriptor
        val vpd: ValueParameterDescriptor
        val descriptor = SimpleFunctionDescriptorImpl.create(
            packageFragmentDescriptor,
            Annotations.EMPTY,
            name,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            SourceElement.NO_SOURCE
        ).apply {
            tpd = TypeParameterDescriptorImpl.createWithDefaultBound(
                this, Annotations.EMPTY, true, Variance.INVARIANT, Name.identifier("T0"), 0
            )

            vpd = ValueParameterDescriptorImpl(
                this, null, 0, Annotations.EMPTY, Name.identifier("arg0"), string,
                false, false, false, null, SourceElement.NO_SOURCE
            )

            val returnType = tpd.typeConstructor.makeNonNullType()

            initialize(null, null, listOf(tpd), listOf(vpd), returnType, Modality.FINAL, Visibilities.PUBLIC)
        }

        val returnKotlinType = descriptor.returnType
        val tps = IrTypeParameterSymbolImpl(tpd)
        val typeParameter = IrBuiltInOperatorTypeParameter(tps, Variance.INVARIANT, 0, true).apply {
            superTypes += anyNType
        }

        val returnIrType = IrSimpleTypeBuilder().run {
            classifier = tps
            kotlinType = returnKotlinType
            buildSimpleType()
        }

        return symbolTable.declareSimpleFunction(UNDEFINED_OFFSET, UNDEFINED_OFFSET, BUILTIN_OPERATOR, descriptor) {
            val operator = IrBuiltInOperator(it, name, returnIrType, ":enum")
            operator.parent = packageFragment
            packageFragment.declarations += operator

            val vps = IrValueParameterSymbolImpl(vpd)
            val valueParameter = IrBuiltInOperatorValueParameter(vps, 0, stringType)

            valueParameter.parent = operator
            typeParameter.parent = operator

            operator.valueParameters += valueParameter
            operator.typeParameters += typeParameter

            irBuiltInsSymbols += operator

            operator
        }.symbol
    }

    private fun defineCheckNotNullOperator(): IrSimpleFunctionSymbol {
        val name = Name.identifier("CHECK_NOT_NULL")
        val tpd: TypeParameterDescriptor
        val vpd: ValueParameterDescriptor

        val returnKotlinType: SimpleType
        val valueKotlinType: SimpleType

        val descriptor = SimpleFunctionDescriptorImpl.create(
            packageFragmentDescriptor,
            Annotations.EMPTY,
            name,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            SourceElement.NO_SOURCE
        ).apply {
            tpd = TypeParameterDescriptorImpl.createForFurtherModification(
                this, Annotations.EMPTY, false, Variance.INVARIANT, Name.identifier("T0"), 0, SourceElement.NO_SOURCE
            ).apply {
                addUpperBound(any)
                setInitialized()
            }

            valueKotlinType = tpd.typeConstructor.makeNullableType()

            vpd = ValueParameterDescriptorImpl(
                this, null, 0, Annotations.EMPTY, Name.identifier("arg0"), valueKotlinType,
                false, false, false, null, SourceElement.NO_SOURCE
            )

            returnKotlinType = tpd.typeConstructor.makeNonNullType()

            initialize(null, null, listOf(tpd), listOf(vpd), returnKotlinType, Modality.FINAL, Visibilities.PUBLIC)
        }

        val tps = IrTypeParameterSymbolImpl(tpd)
        val typeParameter = IrBuiltInOperatorTypeParameter(tps, Variance.INVARIANT, 0, true).apply {
            superTypes += anyType
        }

        val returnIrType = IrSimpleTypeBuilder().run {
            classifier = tps
            kotlinType = returnKotlinType
            hasQuestionMark = false
            buildSimpleType()
        }

        val valueIrType = IrSimpleTypeBuilder().run {
            classifier = tps
            kotlinType = valueKotlinType
            hasQuestionMark = true
            buildSimpleType()
        }

        return symbolTable.declareSimpleFunction(UNDEFINED_OFFSET, UNDEFINED_OFFSET, BUILTIN_OPERATOR, descriptor) {
            val operator = IrBuiltInOperator(it, name, returnIrType, ":!!")
            operator.parent = packageFragment
            packageFragment.declarations += operator

            val vps = IrValueParameterSymbolImpl(vpd)
            val valueParameter = IrBuiltInOperatorValueParameter(vps, 0, valueIrType)

            valueParameter.parent = operator
            typeParameter.parent = operator

            operator.valueParameters += valueParameter
            operator.typeParameters += typeParameter

            irBuiltInsSymbols += operator

            operator
        }.symbol
    }

    private fun defineComparisonOperator(name: String, operandType: IrType) =
        defineOperator(name, booleanType, listOf(operandType, operandType))

    private fun List<IrType>.defineComparisonOperatorForEachIrType(name: String) =
        associate { it.classifierOrFail to defineComparisonOperator(name, it) }

    val any = builtIns.anyType
    val anyN = builtIns.nullableAnyType
    val anyType = any.toIrType()
    val anyClass = builtIns.any.toIrSymbol()
    val anyNType = anyType.withHasQuestionMark(true)

    val bool = builtIns.booleanType
    val booleanType = bool.toIrType()
    val booleanClass = builtIns.boolean.toIrSymbol()

    val char = builtIns.charType
    val charType = char.toIrType()
    val charClass = builtIns.char.toIrSymbol()

    val number = builtIns.number.defaultType
    val numberType = number.toIrType()
    val numberClass = builtIns.number.toIrSymbol()

    val byte = builtIns.byteType
    val byteType = byte.toIrType()
    val byteClass = builtIns.byte.toIrSymbol()

    val short = builtIns.shortType
    val shortType = short.toIrType()
    val shortClass = builtIns.short.toIrSymbol()

    val int = builtIns.intType
    val intType = int.toIrType()
    val intClass = builtIns.int.toIrSymbol()

    val long = builtIns.longType
    val longType = long.toIrType()
    val longClass = builtIns.long.toIrSymbol()

    val float = builtIns.floatType
    val floatType = float.toIrType()
    val floatClass = builtIns.float.toIrSymbol()

    val double = builtIns.doubleType
    val doubleType = double.toIrType()
    val doubleClass = builtIns.double.toIrSymbol()

    val nothing = builtIns.nothingType
    val nothingN = builtIns.nullableNothingType
    val nothingType = nothing.toIrType()
    val nothingClass = builtIns.nothing.toIrSymbol()
    val nothingNType = nothingType.withHasQuestionMark(true)

    val unit = builtIns.unitType
    val unitType = unit.toIrType()
    val unitClass = builtIns.unit.toIrSymbol()

    val string = builtIns.stringType
    val stringType = string.toIrType()
    val stringClass = builtIns.string.toIrSymbol()

    val collectionClass = builtIns.collection.toIrSymbol()

    val arrayClass = builtIns.array.toIrSymbol()

    val throwableType = builtIns.throwable.defaultType.toIrType()
    val throwableClass = builtIns.throwable.toIrSymbol()

    val kCallableClass = builtIns.kCallable.toIrSymbol()
    val kPropertyClass = builtIns.kProperty.toIrSymbol()
    val kDeclarationContainerClass = builtIns.kDeclarationContainer.toIrSymbol()
    val kClassClass = builtIns.kClass.toIrSymbol()

    private val kProperty0Class = builtIns.kProperty0.toIrSymbol()
    private val kProperty1Class = builtIns.kProperty1.toIrSymbol()
    private val kProperty2Class = builtIns.kProperty2.toIrSymbol()
    private val kMutableProperty0Class = builtIns.kMutableProperty0.toIrSymbol()
    private val kMutableProperty1Class = builtIns.kMutableProperty1.toIrSymbol()
    private val kMutableProperty2Class = builtIns.kMutableProperty2.toIrSymbol()

    fun getKPropertyClass(mutable: Boolean, n: Int): IrClassSymbol = when (n) {
        0 -> if (mutable) kMutableProperty0Class else kProperty0Class
        1 -> if (mutable) kMutableProperty1Class else kProperty1Class
        2 -> if (mutable) kMutableProperty2Class else kProperty2Class
        else -> error("No KProperty for n=$n mutable=$mutable")
    }

    // TODO switch to IrType
    val primitiveTypes = listOf(bool, char, byte, short, int, float, long, double)
    val primitiveIrTypes = listOf(booleanType, charType, byteType, shortType, intType, floatType, longType, doubleType)
    private val primitiveIrTypesWithComparisons = listOf(charType, byteType, shortType, intType, floatType, longType, doubleType)
    private val primitiveFloatingPointIrTypes = listOf(floatType, doubleType)
    val primitiveArrays = PrimitiveType.values().map { builtIns.getPrimitiveArrayClassDescriptor(it).toIrSymbol() }
    val primitiveArrayElementTypes = primitiveArrays.zip(primitiveIrTypes).toMap()
    val primitiveArrayForType = primitiveArrayElementTypes.asSequence().associate { it.value to it.key }

    val primitiveTypeToIrType = mapOf(
        PrimitiveType.BOOLEAN to booleanType,
        PrimitiveType.CHAR to charType,
        PrimitiveType.BYTE to byteType,
        PrimitiveType.SHORT to shortType,
        PrimitiveType.INT to intType,
        PrimitiveType.FLOAT to floatType,
        PrimitiveType.LONG to longType,
        PrimitiveType.DOUBLE to doubleType
    )

    val lessFunByOperandType = primitiveIrTypesWithComparisons.defineComparisonOperatorForEachIrType(OperatorNames.LESS)
    val lessOrEqualFunByOperandType = primitiveIrTypesWithComparisons.defineComparisonOperatorForEachIrType(OperatorNames.LESS_OR_EQUAL)
    val greaterOrEqualFunByOperandType = primitiveIrTypesWithComparisons.defineComparisonOperatorForEachIrType(OperatorNames.GREATER_OR_EQUAL)
    val greaterFunByOperandType = primitiveIrTypesWithComparisons.defineComparisonOperatorForEachIrType(OperatorNames.GREATER)

    val ieee754equalsFunByOperandType =
        primitiveFloatingPointIrTypes.map {
            it.classifierOrFail to defineOperator(OperatorNames.IEEE754_EQUALS, booleanType, listOf(it.makeNullable(), it.makeNullable()))
        }.toMap()

    private val booleanNot = builtIns.boolean.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("not"), NoLookupLocation.FROM_BACKEND).single()
    val booleanNotSymbol = symbolTable.referenceSimpleFunction(booleanNot)

    val eqeqeqSymbol = defineOperator(OperatorNames.EQEQEQ, booleanType, listOf(anyNType, anyNType))
    val eqeqSymbol = defineOperator(OperatorNames.EQEQ, booleanType, listOf(anyNType, anyNType))
    val throwCceSymbol = defineOperator(OperatorNames.THROW_CCE, nothingType, listOf())
    val throwIseSymbol = defineOperator(OperatorNames.THROW_ISE, nothingType, listOf())
    val andandSymbol = defineOperator(OperatorNames.ANDAND, booleanType, listOf(booleanType, booleanType))
    val ororSymbol = defineOperator(OperatorNames.OROR, booleanType, listOf(booleanType, booleanType))
    val noWhenBranchMatchedExceptionSymbol = defineOperator(OperatorNames.NO_WHEN_BRANCH_MATCHED_EXCEPTION, nothingType, listOf())
    val illegalArgumentExceptionSymbol = defineOperator(OperatorNames.ILLEGAL_ARGUMENT_EXCEPTION, nothingType, listOf(stringType))

    val enumValueOfSymbol = defineEnumValueOfOperator()
    val checkNotNullSymbol = defineCheckNotNullOperator()

    val checkNotNull = checkNotNullSymbol.descriptor

    private fun TypeConstructor.makeNonNullType() = KotlinTypeFactory.simpleType(Annotations.EMPTY, this, listOf(), false)
    private fun TypeConstructor.makeNullableType() = KotlinTypeFactory.simpleType(Annotations.EMPTY, this, listOf(), true)

    val dataClassArrayMemberHashCodeSymbol = defineOperator("dataClassArrayMemberHashCode", intType, listOf(anyType))
    val dataClassArrayMemberHashCode = dataClassArrayMemberHashCodeSymbol.descriptor

    val dataClassArrayMemberToStringSymbol = defineOperator("dataClassArrayMemberToString", stringType, listOf(anyNType))
//    val dataClassArrayMemberToString = dataClassArrayMemberToStringSymbol.descriptor

    companion object {
        val KOTLIN_INTERNAL_IR_FQN = FqName("kotlin.internal.ir")
        val BUILTIN_OPERATOR = object : IrDeclarationOriginImpl("OPERATOR") {}
    }

    object OperatorNames {
        const val LESS = "less"
        const val LESS_OR_EQUAL = "lessOrEqual"
        const val GREATER = "greater"
        const val GREATER_OR_EQUAL = "greaterOrEqual"
        const val EQEQ = "EQEQ"
        const val EQEQEQ = "EQEQEQ"
        const val IEEE754_EQUALS = "ieee754equals"
        const val THROW_CCE = "THROW_CCE"
        const val THROW_ISE = "THROW_ISE"
        const val NO_WHEN_BRANCH_MATCHED_EXCEPTION = "noWhenBranchMatchedException"
        const val ILLEGAL_ARGUMENT_EXCEPTION = "illegalArgumentException"
        const val ANDAND = "ANDAND"
        const val OROR = "OROR"
    }
}
