/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module

class ExternalDependenciesGenerator(
    moduleDescriptor: ModuleDescriptor,
    val symbolTable: SymbolTable,
    val irBuiltIns: IrBuiltIns,
    val deserializer: IrDeserializer? = null
) {
    private val stubGenerator = DeclarationStubGenerator(
        moduleDescriptor, symbolTable, IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB, irBuiltIns.languageVersionSettings, deserializer
    )

    fun generateUnboundSymbolsAsDependencies(irModule: IrModuleFragment, bindingContext: BindingContext? = null) {
        DependencyGenerationTask(irModule, bindingContext).run()
    }

    private inner class DependencyGenerationTask(val irModule: IrModuleFragment, val bindingContext: BindingContext?) {

        fun run() {
            stubGenerator.unboundSymbolGeneration = true
            ArrayList(symbolTable.unboundClasses).forEach {
                //deserializer?.findDeserializedDeclaration(it) ?:
                stubGenerator.generateClassStub(it.descriptor)
            }
            ArrayList(symbolTable.unboundConstructors).forEach {
                //deserializer?.findDeserializedDeclaration(it) ?:
                stubGenerator.generateConstructorStub(it.descriptor)
            }
            ArrayList(symbolTable.unboundEnumEntries).forEach {
                //deserializer?.findDeserializedDeclaration(it) ?:
                stubGenerator.generateEnumEntryStub(it.descriptor)

            }
            ArrayList(symbolTable.unboundFields).forEach {
                //deserializer?.findDeserializedDeclaration(it) ?:
                stubGenerator.generatePropertyStub(it.descriptor, bindingContext)

            }
            ArrayList(symbolTable.unboundSimpleFunctions).forEach {
                //deserializer?.findDeserializedDeclaration(it) ?:
                stubGenerator.generateFunctionStub(it.descriptor)

            }
            ArrayList(symbolTable.unboundTypeParameters).forEach {
                //deserializer?.findDeserializedDeclaration(it) ?:
                stubGenerator.generateOrGetTypeParameterStub(it.descriptor)

            }

            deserializer?.declareForwardDeclarations()

            assert(symbolTable.unboundClasses.isEmpty()) {
                ArrayList(symbolTable.unboundClasses).forEach {
                    println("unbound: ${it} ${it.descriptor} @${it.descriptor.hashCode()} ${it.descriptor.containingDeclaration} ${it.descriptor.module}")
                }
                error("that's all")
            }
            assert(symbolTable.unboundConstructors.isEmpty())
            assert(symbolTable.unboundEnumEntries.isEmpty())
            assert(symbolTable.unboundFields.isEmpty())
            assert(symbolTable.unboundSimpleFunctions.isEmpty())
            assert(symbolTable.unboundTypeParameters.isEmpty())
        }
    }
}
