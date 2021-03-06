/*
 * Copyright Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the authors tag. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License version 2.
 * 
 * This particular file is subject to the "Classpath" exception as provided in the 
 * LICENSE file that accompanied this code.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package com.redhat.ceylon.compiler.loader.refl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.tools.JavaFileObject.Kind;

import com.redhat.ceylon.compiler.modelloader.AbstractModelLoader;
import com.redhat.ceylon.compiler.modelloader.refl.ReflAnnotation;
import com.redhat.ceylon.compiler.modelloader.refl.ReflClass;
import com.redhat.ceylon.compiler.modelloader.refl.ReflMethod;
import com.redhat.ceylon.compiler.modelloader.refl.ReflPackage;
import com.redhat.ceylon.compiler.modelloader.refl.ReflType;
import com.redhat.ceylon.compiler.modelloader.refl.ReflTypeParameter;
import com.redhat.ceylon.compiler.util.Util;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;

public class JavacClass implements ReflClass {

    public ClassSymbol classSymbol;

    public JavacClass(ClassSymbol classSymbol){
        this.classSymbol = classSymbol;
    }
    
    @Override
    public boolean isPublic() {
        return (classSymbol.flags() & Flags.PUBLIC) != 0;
    }

    @Override
    public String getQualifiedName() {
        // as taken from ClassSymbol.className():
        if(classSymbol.name.len == 0)
            return classSymbol.flatname.toString();
        else
            return classSymbol.getQualifiedName().toString();
    }

    @Override
    public ReflPackage getPackage() {
        return new JavacPackage(classSymbol.packge());
    }

    @Override
    public boolean isInterface() {
        return classSymbol.isInterface();
    }

    @Override
    public boolean isCeylonToplevelAttribute() {
        return getAnnotation(AbstractModelLoader.CEYLON_ATTRIBUTE_ANNOTATION) != null;
    }

    @Override
    public boolean isCeylonToplevelObject() {
        return getAnnotation(AbstractModelLoader.CEYLON_OBJECT_ANNOTATION) != null;
    }

    @Override
    public boolean isCeylonToplevelMethod() {
        return getAnnotation(AbstractModelLoader.CEYLON_METHOD_ANNOTATION) != null;
    }

    @Override
    public ReflAnnotation getAnnotation(String type) {
        return JavacUtil.getAnnotation(classSymbol, type);
    }

    @Override
    public boolean isLoadedFromSource() {
        return classSymbol.classfile.getKind() != Kind.CLASS;
    }

    @Override
    public String getSimpleName() {
        // FIXME Using flatName() instead of just getSimpleName() is for anonymous classes, but do we need them?
        return Util.getSimpleName(classSymbol.flatName().toString());
    }

    @Override
    public boolean isAbstract() {
        return (classSymbol.flags() & Flags.ABSTRACT) != 0;
    }

    @Override
    public List<ReflMethod> getDirectMethods() {
        List<ReflMethod> methods = new LinkedList<ReflMethod>();
        for(Symbol sym : classSymbol.getEnclosedElements()){
            if(sym instanceof MethodSymbol){
                methods.add(new JavacMethod((MethodSymbol)sym));
            }
        }
        return methods;
    }

    @Override
    public ReflType getSuperclass() {
        Type superclass = classSymbol.getSuperclass();
        return superclass != null ? new JavacType(superclass) : null;
    }

    @Override
    public List<ReflType> getInterfaces() {
        com.sun.tools.javac.util.List<Type> interfaces = classSymbol.getInterfaces();
        List<ReflType> ret = new ArrayList<ReflType>(interfaces.size());
        for(Type interfce : interfaces)
            ret.add(new JavacType(interfce));
        return ret;
    }

    @Override
    public List<ReflTypeParameter> getTypeParameters() {
        return JavacUtil.getTypeParameters(classSymbol);
    }

}
