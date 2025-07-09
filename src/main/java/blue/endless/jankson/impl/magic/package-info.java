/*
 * MIT License
 *
 * Copyright (c) 2018-2025 Falkreon (Isaac Ellingson)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/**
 * Everything in this package does surprising, dangerous, or funky things with the JVM type system.
 * 
 * <p>The reflection class "Type" is a funny thing. "Type" is a marker class, it shares no common
 * ancestor with "Class", but every fully-erased "Type" is an instance of "Class". Non-erased types
 * are:
 * <ul>
 *   <li>ParameterizedType, which bundles a class with its filled-in generic type parameters
 *   <li>AnnotatedType, which bundles a class or ParameterizedType with its attached annotations
 *   <li>GenericArrayType, which represents an array type. While there is a unique, reified Class type
 *       for each array type
 *   <li>TypeVariable, which holds a placeholder for a type that's filled in elsewhere in the type hierarchy
 * </ul>
 * 
 */
package blue.endless.jankson.impl.magic;