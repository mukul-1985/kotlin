/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirErrorTypeRef : FirResolvedTypeRef() {
    abstract override val psi: PsiElement?
    abstract override val annotations: List<FirAnnotationCall>
    abstract override val type: ConeKotlinType
    abstract val reason: String

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitErrorTypeRef(this, data)
}
