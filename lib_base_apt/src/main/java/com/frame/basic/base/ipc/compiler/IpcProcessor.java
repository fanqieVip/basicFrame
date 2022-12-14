package com.frame.basic.base.ipc.compiler;

import com.frame.basic.base.ipc.CallBlock;
import com.frame.basic.base.ipc.MethodInfo;
import com.frame.basic.base.ipc.annotations.IpcApi;
import com.frame.basic.base.ipc.annotations.IpcServer;
import com.frame.basic.base.ipc.annotations.IpcTarget;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;


/**
 * @Description:
 * @Author: fanj
 * @CreateDate: 2022/8/2 13:32
 * @Version:
 */
@AutoService(Processor.class)
public class IpcProcessor extends AbstractProcessor {
    Messager mMessager;
    Filer mFiler;
    Elements mElements;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mMessager = processingEnv.getMessager();
        mFiler = processingEnv.getFiler();
        mElements = processingEnv.getElementUtils();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<String>();
        annotations.add(IpcServer.class.getCanonicalName());
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        //???????????????IpcServer??????
        Set<? extends Element> ipcServerElements = roundEnv.getElementsAnnotatedWith(IpcServer.class);
        if (ipcServerElements.size() == 0) {
            return false;
        }
        //?????????????????????????????????????????????????????????
        Map<String, List<MethodInfo>> ipcServerTypes = new HashMap<>();
        for (Element element : ipcServerElements) {
            TypeElement typeElement = (TypeElement) element;
            //?????????????????????
            String targetClassName = typeElement.getQualifiedName().toString();
            //??????????????????????????????????????????????????????????????????IpcApi?????????
            List<Element> methods = new ArrayList();
            List<MethodInfo> methodInfos = new ArrayList<>();
            for (Element methodElement : typeElement.getEnclosedElements()) {
                if (methodElement.getAnnotation(IpcApi.class) != null) {
                    methods.add(methodElement);
                    ExecutableElement realElement = ((ExecutableElement) methodElement);
                    //?????????
                    String name = realElement.getSimpleName().toString();
                    //????????????
                    List<TypeMirror> paramTypes = new ArrayList<>();
                    List<String> paramTags = new ArrayList<>();
                    List<List<String>> annotationTypes = new ArrayList<>();
                    for (VariableElement param : realElement.getParameters()) {
                        TypeMirror typeName = param.asType();
                        String typeTag = param.toString();
                        paramTypes.add(typeName);
                        paramTags.add(typeTag);
                        List<String> annotationTypeList = new ArrayList<>();
                        List<? extends AnnotationMirror> annotationMirrors = param.getAnnotationMirrors();
                        if (annotationMirrors != null && !annotationMirrors.isEmpty()){
                            annotationMirrors.forEach(new Consumer<AnnotationMirror>() {
                                @Override
                                public void accept(AnnotationMirror annotationMirror) {
                                    annotationTypeList.add(annotationMirror.getAnnotationType().toString());
                                }
                            });
                        }
                        annotationTypes.add(annotationTypeList);
                    }
                    //????????????
                    TypeMirror returnType = realElement.getReturnType();
                    //????????????????????????
                    MethodInfo methodInfo = new MethodInfo();
                    methodInfo.setName(name);
                    methodInfo.setParamsTags(paramTags);
                    methodInfo.setParamsTypes(paramTypes);
                    methodInfo.setAnnotationTypes(annotationTypes);
                    methodInfo.setReturnType(returnType);
                    methodInfos.add(methodInfo);
                }
            }
            ipcServerTypes.put(targetClassName, methodInfos);
        }
        if (ipcServerTypes.isEmpty()) {
            return false;
        }
        //?????????????????????
        ipcServerTypes.forEach(new BiConsumer<String, List<MethodInfo>>() {
            @Override
            public void accept(String s, List<MethodInfo> methodInfos) {
                createIpcServerClass(s, methodInfos);
            }
        });
        return true;
    }

    /**
     * ???????????????
     *
     * @param name        ?????????????????????
     * @param methodInfos ?????????????????????????????????
     */
    private void createIpcServerClass(String name, List<MethodInfo> methodInfos) {
        try {
            AnnotationSpec.Builder annotationSpecBuilder = AnnotationSpec.builder(IpcTarget.class).addMember("value", name+".class");
            TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(getClassName(name) + "Call").addModifiers(Modifier.PUBLIC).addModifiers(Modifier.FINAL).addAnnotation(annotationSpecBuilder.build());
            for (MethodInfo methodInfo : methodInfos) {
                //?????????
                MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodInfo.getName()).addModifiers(Modifier.PUBLIC, Modifier.STATIC);
                //???????????????LifecycleOwner?????????????????????
                ParameterSpec.Builder ownerParameterSpecBuilder = ParameterSpec.builder(ClassName.get(Class.forName("androidx.lifecycle.LifecycleOwner")), "owner");
                ownerParameterSpecBuilder.addAnnotation(Nullable.class);
                methodBuilder.addParameter(ownerParameterSpecBuilder.build());
                //????????????????????????????????????
                for (int i = 0; i < methodInfo.getParamsTypes().size(); i++) {
                    TypeMirror paramType = methodInfo.getParamsTypes().get(i);
                    String paramTag = methodInfo.getParamsTags().get(i);
                    List<String> annotationTypes = methodInfo.getAnnotationTypes().get(i);
                    ParameterSpec.Builder parameterSpecBuilder = ParameterSpec.builder(ClassName.get(paramType), paramTag);
                    if (!annotationTypes.isEmpty()){
                        annotationTypes.forEach(new Consumer<String>() {
                            @Override
                            public void accept(String annotationType) {
                                try {
                                    parameterSpecBuilder.addAnnotation(Class.forName(annotationType));
                                } catch (ClassNotFoundException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                    ParameterSpec parameterSpec = parameterSpecBuilder.build();
                    methodBuilder.addParameter(parameterSpec);
                }
                //?????????????????????
                ParameterizedTypeName returnParams = ParameterizedTypeName.get(
                        ClassName.get(CallBlock.class),
                        ParameterizedTypeName.get(methodInfo.getReturnType()).box()
                );
                ParameterSpec callBlockParameter = ParameterSpec.builder(returnParams, "callBlock")
                        .addAnnotation(Nullable.class)
                        .build();
                methodBuilder.addParameter(callBlockParameter);
                //?????????
                methodBuilder.returns(TypeName.VOID);
                //?????????
                methodBuilder.addStatement("android.os.Bundle bundle = new android.os.Bundle()");
                methodBuilder.addStatement("com.frame.basic.base.ipc.MethodDesc methodDesc = new com.frame.basic.base.ipc.MethodDesc()");
                methodBuilder.addStatement("methodDesc.setName($L)", "\""+methodInfo.getName()+"\"");
                methodBuilder.addStatement("java.util.List<com.frame.basic.base.ipc.ParamsDesc> paramsDescs = new java.util.ArrayList<>()");
                for (int i = 0; i < methodInfo.getParamsTypes().size(); i++) {
                    TypeMirror paramType = methodInfo.getParamsTypes().get(i);
                    String paramTag = methodInfo.getParamsTags().get(i);
                    methodBuilder.addStatement("paramsDescs.add(new com.frame.basic.base.ipc.ParamsDesc($L, $L))", deleteGeneric(paramType.toString())+".class", paramTag);
                }
                methodBuilder.addStatement("methodDesc.setParams(paramsDescs)");
                methodBuilder.addStatement("methodDesc.setResult($L)", deleteGeneric(methodInfo.getReturnType().toString())+".class");
                methodBuilder.addStatement("bundle.putSerializable(\"method\", methodDesc)");
                methodBuilder.addStatement("com.frame.basic.base.ipc.IpcHelper.sendMsg($L, $L, bundle, callBlock)", name+".class", "owner");
                //?????????????????????
                typeSpecBuilder.addMethod(methodBuilder.build());
            }
            TypeSpec taCls = typeSpecBuilder.build();
            JavaFile javaFile = JavaFile.builder(getClassPackageName(name) + ".apt", taCls).build();
            javaFile.writeTo(mFiler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getClassPackageName(String name) {
        String temp = name.substring(0, name.lastIndexOf("."));
        return temp;
    }

    private String getClassName(String name) {
        String temp = name.substring(name.lastIndexOf(".") + 1);
        return temp;
    }

    private String deleteGeneric(String name) {
        int pos = name.indexOf("<");
        if (pos >= 0){
            return name.substring(0, pos);
        }else{
            return name;
        }
    }
}
