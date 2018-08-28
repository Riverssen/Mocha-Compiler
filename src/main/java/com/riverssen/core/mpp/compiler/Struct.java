/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Riverssen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.riverssen.core.mpp.compiler;

import com.riverssen.core.mpp.Executable;

import java.util.*;

import static com.riverssen.core.mpp.compiler.GlobalSpace.getMethodName;

public class Struct
{
    public static long NULL = 0;
    public static final Struct VOID = new Struct();

    private String              __typename__;
    private long                __typesize__;
    private Set<Field>          __fields__;
    private Set<Method>         __methods__;
    private Struct              __parent__;
    private GlobalSpace         __glblspace__;
    private Set<String>         __templates__;
    private int                 __type__;

    public Struct()
    {
        this.__fields__         = new LinkedHashSet<>();
        this.__methods__        = new LinkedHashSet<>();
        this.__typesize__       = 0;
        this.__typename__       = "VOID";
        this.__glblspace__      = new GlobalSpace();
        this.__templates__      = new LinkedHashSet<>();
        this.__type__           = -2;
    }

    public Struct(String name, int size, GlobalSpace space, int type)
    {
        this.__fields__         = new LinkedHashSet<>();
        this.__methods__        = new LinkedHashSet<>();
        this.__typesize__       = size;
        this.__typename__       = name;
        this.__parent__         = VOID;
        this.__glblspace__      = space;
        this.__templates__      = new LinkedHashSet<>();
        this.__type__           = type;
    }

    public Struct setParent(Struct struct)
    {
        this.__parent__ = struct;
        return this;
    }

    public Struct(GlobalSpace space, Token token)
    {
        this.__fields__         = new LinkedHashSet<>();
        this.__methods__        = new LinkedHashSet<>();
        this.__typesize__       = 0;
        this.__typename__       = token.getTokens().get(0).toString();
        this.__parent__         = space.getGlobalTypes().get("Object");
        this.__glblspace__      = space;
        this.__templates__      = new LinkedHashSet<>();
        this.__type__           = -1;

        space.getGlobalTypes().put(__typename__, this);

//        System.out.println(space.getGlobalTypes().get("Object") + " " + __typename__);

        if (token.getType().equals(Token.Type.CLASS_DECLARATION)) nclass(token, token.getTokens().get(1), space);
        else if (token.getType().equals(Token.Type.TEMPLATE_CLASS_DECLARATION))
        {
        } else {
            System.err.println("unknown class type: " + token + ".");
            System.exit(0);
        }
    }

    private void nclass(Token token, Token body, GlobalSpace space)
    {
        Set<String> fields      = new LinkedHashSet<>();

        for (Token t : body.getTokens())
        {
            switch (t.getType())
            {
                case EMPTY_DECLARATION:
                    if (fields.contains(t.getTokens().get(1).toString()))
                    {
                        System.err.println("field __" + t.getTokens().get(1).toString() + "__ already exists in __" + __typename__ + "__.");
                        System.exit(0);
                    }

                    if (t.isModifier(Modifier.REFERENCE))
                    {
                        System.err.println("classes cannot hold references as fields. In__" + __typename__ + "__.");
                        System.exit(0);
                    }
                    Field field = new Field(space, t, t.isModifier(Modifier.STATIC) ? null : this);
                    field.setLocation(__typesize__);
                    __typesize__ += field.size(space);
                    if (t.isModifier(Modifier.STATIC))
                        space.getGlobalFields().put(field.getName(), field);
                    else
                        __fields__.add(field);
                    if (t.isModifier(Modifier.STATIC)) {}
                    else
                        fields.add(t.getTokens().get(1).toString());
                    break;
                case FULL_DECLARATION:
                    if (fields.contains(t.getTokens().get(1).toString()))
                    {
                        System.err.println("field __" + t.getTokens().get(1).toString() + "__ already exists in __" + __typename__ + "__.");
                        System.exit(0);
                    }

                    if (t.isModifier(Modifier.REFERENCE))
                    {
                        System.err.println("classes cannot hold references as fields. In__" + __typename__ + "__.");
                        System.exit(0);
                    }
                    Field _field_ = new Field(space, t, t.isModifier(Modifier.STATIC) ? null : this);
                    _field_.setLocation(__typesize__);
                    __typesize__ += _field_.size(space);
                    if (t.isModifier(Modifier.STATIC))
                        space.getGlobalFields().put(_field_.getName(), _field_);
                    else
                        __fields__.add(_field_);
                    if (t.isModifier(Modifier.STATIC)) {}
                    else
                        fields.add(t.getTokens().get(1).toString());
                    break;
                case METHOD_EMPTY_DECLARATION:
                case METHOD_DECLARATION:
                    Method method = new Method(space, this, t);

                    if (containsMethod(method.getName(), method.getArguments(), true) && getMethod(method.getName(), method.getArguments()).isDeclared())
                    {
                        System.err.println("method __" + t.getTokens().get(0).toString() + "__ already exists in __" + __typename__ + "__.");
                        System.err.println("at line: " + token.getLine());
                        System.exit(0);
                    }

                    if (method.getName().equals(__typename__))
                        space.addMethod(method.getName(), method);

                    addMethod(method.getName(), method);
                    break;
                case OPERATOR:
                    Method opMethod = new Method(space, this, t);

                    if (containsMethod(opMethod.getName(), opMethod.getArguments(), true) && getMethod(opMethod.getName(), opMethod.getArguments()).isDeclared())
                    {
                        System.err.println("method __" + t.getTokens().get(0).toString() + "__ already exists in __" + __typename__ + "__.");
                        System.err.println("at line: " + token.getLine());
                        System.exit(0);
                    }
                    addMethod(opMethod.getName(),  opMethod);
                    break;
                case CLASS_DECLARATION:
                    System.err.println("Class declaration not allowed inside of a class __" + __typename__ + "__.");
                default:
                    System.out.println("An error occured '" + t + "'.");
                    System.out.println(t.humanReadable(1));
                    System.out.println("in '" + __typename__ + "'");
                    System.exit(0);
                    break;
            }
        }
    }

    public long size()
    {
        return __typesize__;
    }

    public String getName()
    {
        return __typename__;
    }

    public boolean containsField(String reference, String accessor)
    {
        boolean must_be_public = !accessor.equals(__typename__);

        for (Field field : __fields__)
            if (field.getName().equals(reference) && (must_be_public ? field.isPublic() : true)) return true;

        if (__parent__ != null)
            if (__parent__.containsField(reference, accessor)) return true;

        return __glblspace__.getGlobalFields().containsKey(reference);
    }

    public Field getField(String reference, String accessor)
    {
        boolean must_be_public = !accessor.equals(__typename__);

        for (Field field : __fields__)
            if (field.getName().equals(reference) && (must_be_public ? field.isPublic() : true)) return field;

        if (__parent__ != null)
            if (__parent__.containsField(reference, accessor)) return __parent__.getField(reference, accessor);

        return __glblspace__.getGlobalFields().get(reference);
    }

    public void accessField(String name, Executable executable, String accessor)
    {
        Field field = getField(name, accessor);

        if (field != null)
        {
            executable.add(instructions.stack_read);
            executable.add(executable.convertLong(field.getLocation()));
        } else {
            System.err.println(name + " unaccessible.");
            System.exit(0);
        }
    }

    public long getFieldOffset(String variable_name)
    {
        return getField(variable_name, __typename__).getLocation();
    }

    @Override
    public String toString()
    {
        return getName();
    }

    public String spit()
    {
        String string = "" + __typename__ + "\n";

        for (Field field : __fields__)
            string += ("\tfield: " + field.getName() + " " + field.getTypeName() + " " + field.getLocation()) + "\n";

        return string;
    }

    public int getType()
    {
        return __type__;
    }

    public boolean contains(String reference, String accessor, GlobalSpace space)
    {
        return containsField(reference, accessor);
    }

    public long    getLocation(String reference, String accessor, GlobalSpace space)
    {
        if (containsField(reference, accessor))
            return getField(reference, accessor).getLocation();
//        else if (containsMethod(reference, space))
//            return getMethod(reference).getLocation();

        return -1;
    }

    public Field get(String string)
    {
        return null;
    }

    public Struct getParent()
    {
        return __parent__;
    }

    public Set<Method> getMethods()
    {
        return __methods__;
    }

    private void addMethod(String methodName, Method method)
    {
        String qualifiedMethodName = getMethodName(methodName, method.getArguments());

//        if (containsMethod(methodName, method.getArguments()) && getMethod(methodName, method.getArguments().size()).isDeclared())
//        {
//            System.err.println("err: method '" + qualifiedMethodName + "' already exists in '" + getName() + "'.");
//            System.exit(0);
//        }

        __methods__.add(method);
    }

    public boolean containsMethod(String methodName, Set<Field> args)
    {
        return containsMethod(methodName, args, false);
    }

    public boolean containsMethod(String methodName, Set<Field> args, boolean onlyThis)
    {
        for (Method method : __methods__)
            if (methodName.equals(method.getName()) && method.matches(__glblspace__, new ArrayList<>(args)))
                return true;

        if (onlyThis) return false;

        if (__parent__ != null)
            if (__parent__.containsMethod(methodName, args)) return true;

        return __glblspace__.containsMethod(methodName, args);
    }

    public Method getMethod(String methodName, Set<Field> arguments)
    {
        String method_ = getMethodName(methodName, arguments);

        for (Method method : __methods__)
            if (methodName.equals(method.getName()) && method.matches(__glblspace__, new ArrayList<>(arguments)))
                return method;

        if (__parent__ != null && __parent__.containsMethod(methodName, arguments)) return __parent__.getMethod(methodName, arguments);
        else if (__glblspace__.containsMethod(methodName, arguments)) return __glblspace__.getMethod(methodName, arguments);
        else {
            System.err.println("err: method '" + method_ + "' not found in '" + getName() + "'.");

            System.exit(0);
        }

        return null;
    }

    public boolean containsMethod(String methodName, int args)
    {
        return containsMethod(methodName, args, false);
    }

    public boolean containsMethod(String methodName, int args, boolean onlyThis)
    {
        for (Method method : __methods__)
            if (methodName.equals(method.getName()) && method.getArguments().size() == args)
                return true;

        if (onlyThis) return false;

        if (__parent__ != null)
            if (__parent__.containsMethod(methodName, args)) return true;

        return __glblspace__.containsMethod(methodName, args);
    }

    public Method getMethod(String methodName, int arguments)
    {
        for (Method method : __methods__)
            if (methodName.equals(method.getName()) && method.getArguments().size() == arguments)
                return method;

        if (__parent__ != null && __parent__.containsMethod(methodName, arguments)) return __parent__.getMethod(methodName, arguments);
        else if (__glblspace__.containsMethod(methodName, arguments)) return __glblspace__.getMethod(methodName, arguments);
        else {
            System.err.println("err: method '" + methodName + "' not found in '" + getName() + "'.");

            System.exit(0);
        }

        return null;
    }

    /**
     * @param struct; the struct to match agaisnt.
     * @return if this struct matches with or is child of 'struct'.
     */
    public boolean match(Struct struct)
    {
        if (__type__ == -2 || __typename__.equals("VOID")) return true;
        else if (struct.__type__ == -2 || struct.__typename__.equals("VOID")) return true;

        if (getName().equals(struct.getName())) return true;

        Struct parent = __parent__;

        while (parent != null)
        {
            if (parent.getName().equals(struct.getName())) return true;

            parent = parent.__parent__;
        }

        return false;
    }

    public boolean equals(Object obj)
    {
        if (obj instanceof Struct)
        {
            this.match((Struct) obj);
        }

        return super.equals(obj);
    }
}
