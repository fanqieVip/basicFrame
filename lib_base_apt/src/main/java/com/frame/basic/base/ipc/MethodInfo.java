package com.frame.basic.base.ipc;

import java.util.List;

import javax.lang.model.type.TypeMirror;

/**
 * @Description:
 * @Author: fanj
 * @CreateDate: 2022/8/2 14:22
 * @Version:
 */
public class MethodInfo {
    private String name;
    private List<TypeMirror> paramsTypes;
    private List<String> paramsTags;
    private List<List<String>> annotationTypes;
    private TypeMirror returnType;

    public List<List<String>> getAnnotationTypes() {
        return annotationTypes;
    }

    public void setAnnotationTypes(List<List<String>> annotationTypes) {
        this.annotationTypes = annotationTypes;
    }

    public List<String> getParamsTags() {
        return paramsTags;
    }

    public void setParamsTags(List<String> paramsTags) {
        this.paramsTags = paramsTags;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<TypeMirror> getParamsTypes() {
        return paramsTypes;
    }

    public void setParamsTypes(List<TypeMirror> paramsTypes) {
        this.paramsTypes = paramsTypes;
    }

    public TypeMirror getReturnType() {
        return returnType;
    }

    public void setReturnType(TypeMirror returnType) {
        this.returnType = returnType;
    }
}
