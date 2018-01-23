/*
 * Copyright (c) 2017 Outsource Cafe, Inc.
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

package com.javagen.schema.model


class MType extends MBase
{
	boolean primitive = false
	boolean builtIn = false
	int arrayDimensions = 0 //needed to handle Java array types: byte[], byte[][], etc.
	String val // default value

	String toString() { name }
	
	static MType lookupType(String name) {
		MTypeRegistry.instance().lookupType(name)
	}
	static void registerType(String name, MType type) {
		MTypeRegistry.instance().registerType(name, type)
	}
	static void registerType(MClass type) {
		MTypeRegistry.instance().registerType(type.name, type)
	}

}
