package jwf.soupladle;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

public class AnnotationProcessor extends AbstractProcessor {
    private Messager mMessager;
    private Filer mFiler;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mMessager = processingEnv.getMessager();
        mFiler = processingEnv.getFiler();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        HashSet<String> result = new HashSet<>();
        result.add(Bind.class.getCanonicalName());
        return result;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return true;
        }

        Map<TypeElement, List<VariableElement>> bindingClasses = new HashMap<>();
        for (Element e : roundEnv.getElementsAnnotatedWith(Bind.class)) {
            if (!(e instanceof VariableElement)) {
                // all elements annotated with @Bind should be variables...
                mMessager.printMessage(Diagnostic.Kind.WARNING, "Cannot use @Bind with non-variables, skipping.", e);
                continue;
            }
            VariableElement variable = (VariableElement) e;
            Element parent = variable.getEnclosingElement();

            if (!(parent instanceof TypeElement)) {
                // we will only allow binding to fields that are part of a class
                mMessager.printMessage(Diagnostic.Kind.WARNING, "Cannot use @Bind with variables which are not members of a class.", e);
                continue;
            }
            TypeElement parentClass = (TypeElement) parent;

            // Append the variable element to the list of variables which require binding on its
            // parent class.
            List<VariableElement> members;
            if (bindingClasses.containsKey(parentClass)) {
                members = bindingClasses.get(parentClass);
            } else {
                members = new ArrayList<>();
                bindingClasses.put(parentClass, members);
            }
            members.add(variable);
        }
        // Now that we're done rounding up all the bind targets and their parent classes, let's
        // generate SoupLadle.java
        try {
            JavaFileObject jfo = mFiler.createSourceFile("jwf.soupladle.SoupLadle");

            TypeSpec.Builder soupLadleBuilder = TypeSpec.classBuilder("SoupLadle")
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

            for (Map.Entry<TypeElement, List<VariableElement>> binding : bindingClasses.entrySet()) {
                TypeName typeParameter = ClassName.get(binding.getKey());

                MethodSpec.Builder bindingBuilder = MethodSpec.methodBuilder("bind")
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                        .addParameter(typeParameter, "target");

                List<VariableElement> members = binding.getValue();
                for (VariableElement member : members) {
                    Bind annotation = member.getAnnotation(Bind.class);
                    TypeName castClass = ClassName.get(member.asType());
                    bindingBuilder.addStatement("target.$L = ($T) target.findViewById($L)", member.getSimpleName().toString(), castClass, annotation.value());
                }

                soupLadleBuilder.addMethod(bindingBuilder.build());
            }

            //@SuppressWarnings("ResourceType")
            AnnotationSpec suppressIdentifierWarningAnnotation = AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "$S", "ResourceType").build();
            soupLadleBuilder.addAnnotation(suppressIdentifierWarningAnnotation);

            JavaFile file = JavaFile.builder("jwf.soupladle", soupLadleBuilder.build())
                    .indent("    ") // personally, I prefer 4-space tabs... JavaPoet defaults to 2-space
                    .build();

            Writer out = jfo.openWriter();
            file.writeTo(out);
            out.flush();
            out.close();
        } catch (IOException e) {
            mMessager.printMessage(Diagnostic.Kind.ERROR, "Could not generate SoupLadle.java");
            throw new RuntimeException(e);
        }

        return true;
    }
}
