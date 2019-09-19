/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.fir.visitors.generator.org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder

sealed class Field {
    abstract val name: String
    abstract val type: String
    abstract val nullable: Boolean
}

// ----------- Simple field -----------

data class SimpleField(override val name: String, override val type: String, override val nullable: Boolean) : Field() {
    constructor(name: String, type: Type, nullable: Boolean) : this(name, type.type, nullable)
    constructor(type: Type, nullable: Boolean) : this(type.type.decapitalize(), type.type, nullable)
}

fun Element.field(name: String, type: String, nullable: Boolean = false) {
    fields += SimpleField(name, type, nullable)
}

fun Element.field(name: String, type: Type, nullable: Boolean = false) {
    fields += SimpleField(name, type, nullable)
}

fun Element.field(type: Type, nullable: Boolean = false) {
    fields += SimpleField(type, nullable)
}

fun field(name: String, type: String, nullable: Boolean = false): Field {
    return SimpleField(name, type, nullable)
}

fun field(name: String, type: Type, nullable: Boolean = false): Field {
    return SimpleField(name, type, nullable)
}

fun field(type: Type, nullable: Boolean = false): Field {
    return SimpleField(type, nullable)
}

// ----------- Fir field -----------

data class FirField(override val name: String, val element: Element, override val nullable: Boolean = false) : Field() {
    constructor(element: Element, nullable: Boolean) : this(element.name.decapitalize(), element, nullable)

    override val type: String = element.type + if (nullable) "?" else ""
}

fun booleanField(name: String): Field {
    return field(name, AbstractFirTreeBuilder.boolean)
}

fun stringField(name: String): Field {
    return field(name, AbstractFirTreeBuilder.string)
}

fun Element.field(name: String, element: Element, nullable: Boolean = false) {
    fields += FirField(name, element, nullable)
}

fun Element.field(element: Element, nullable: Boolean = false) {
    fields += FirField(element, nullable)
}

fun field(name: String, element: Element, nullable: Boolean = false): Field {
    return FirField(name, element, nullable)
}

fun field(element: Element, nullable: Boolean = false): Field {
    return FirField(element, nullable)
}

// ----------- Field list -----------

data class FieldList(override val name: String, val baseType: String) : Field() {
    constructor(name: String, element: Element) : this(name, element.type)
    constructor(baseType: String) : this(baseType.decapitalize() + "s", baseType)

    constructor(base: Element) : this(base.name.decapitalize() + "s", base.type)

    override val type: String
        get() = "List<$baseType>"
    override val nullable: Boolean
        get() = false

}

fun Element.fieldList(name: String, element: Element) {
    fields += FieldList(name, element)
}

fun Element.fieldList(element: Element) {
    fields += FieldList(element)
}

fun fieldList(name: String, element: Element): Field {
    return FieldList(name, element)
}

fun fieldList(element: Element): Field {
    return FieldList(element)
}

// ----------- Element -----------

class Element(val name: String) {
    val fields = mutableSetOf<Field>()
    val type: String = "Fir$name"
    val parents = mutableListOf<Element>()

    val allFields: List<Field> by lazy {
        val result = LinkedHashSet<Field>()
        parents.forEach {
            result.addAll(it.allFields)
        }
        result.addAll(fields)
        result.toList()
    }
}

class Type(val type: String)

// ----------- Field set -----------

class FieldSet(val fields: MutableList<Field>)

fun fieldSet(vararg fields: Field): FieldSet {
    return FieldSet(fields.toMutableList())
}

infix fun FieldSet.with(sets: List<FieldSet>): FieldSet {
    sets.forEach {
        fields += it.fields
    }
    return this
}

infix fun FieldSet.with(set: FieldSet): FieldSet {
    fields += set.fields
    return this
}

