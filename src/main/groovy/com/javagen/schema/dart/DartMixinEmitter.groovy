/*
 * Copyright (c) 2019 Outsource Cafe, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.javagen.schema.dart

import com.javagen.schema.common.CodeEmitter
import com.javagen.schema.dart.DartTypeRegistry
import com.javagen.schema.model.MEnum
import com.javagen.schema.model.MModule
import com.javagen.schema.model.MTypeRegistry

/**
 * Just adds an import statement to a 'mixin' module.
 *
 * @author Richard Easterling
 */
class DartMixinEmitter extends CodeEmitter
{

    DartMixinEmitter()
    {
         if ( ! MTypeRegistry.isInitialized() )
            new DartTypeRegistry()
    }

    @Override
    def visit(MModule m) {
        if (m.isRoot()) {
            importMixinSourceHook(m)
        }
    }

    def importMixinSourceHook(MModule rootModule)
    {
        File mixinPath = DartUtil.toGeneratedSourceFileName(rootModule.sourceFile, '.mixin')
        String importName = gen.projectPath(mixinPath)
        rootModule.getImports() << importName
    }

}
