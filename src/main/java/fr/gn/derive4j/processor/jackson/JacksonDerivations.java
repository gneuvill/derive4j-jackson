package fr.gn.derive4j.processor.jackson;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import fr.gn.derive4j.jackson.instances.FjTypes;
import fr.gn.derive4j.jackson.instances.JavaTypes;
import org.derive4j.processor.api.*;
import org.derive4j.processor.api.model.AlgebraicDataType;
import org.derive4j.processor.api.model.DataArgument;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.derive4j.processor.api.DerivatorSelections.selection;

@AutoService(DerivatorFactory.class)
public class JacksonDerivations implements DerivatorFactory {
  @Override
  public List<DerivatorSelection> derivators(DeriveUtils deriveUtils) {
    final ClassName
      stdSerClassName =
        ClassName.get("com.fasterxml.jackson.databind.ser.std", "StdSerializer"),

      stdDeserClassName =
        ClassName.get("com.fasterxml.jackson.databind.deser.std", "StdDeserializer");

    final TypeElement
      stdSerType = getTypeElement(deriveUtils, stdSerClassName),

      sdtDeserType = getTypeElement(deriveUtils, stdDeserClassName);

    final DeclaredType
      wildcardStdSerType = getDeclaredWildcardedType(deriveUtils, stdSerType),

      wildcardStdDeserType = getDeclaredWildcardedType(deriveUtils, sdtDeserType);

    final TypeElement
      javaTypesProvider =
        deriveUtils.elements().getTypeElement(JavaTypes.class.getCanonicalName()),

      fjTypesProvider =
        deriveUtils.elements().getTypeElement(FjTypes.class.getCanonicalName()),

      jackTypeRefType =
        deriveUtils.elements().getTypeElement("com.fasterxml.jackson.core.type.TypeReference"),

      jackTypeFactoryType =
        deriveUtils.elements().getTypeElement("com.fasterxml.jackson.databind.type.TypeFactory");

    return
      asList(selection(stdSerClassName, adt -> genInstance(deriveUtils
        , stdSerClassName
        , stdSerType
        , wildcardStdSerType
        , asList(javaTypesProvider, fjTypesProvider)
        , jackTypeRefType
        , jackTypeFactoryType
        , adt
        , JacksonDerivations::genSerializerCode))

      , selection(stdDeserClassName, adt -> genInstance(deriveUtils
        , stdDeserClassName
        , sdtDeserType
        , wildcardStdDeserType
        , asList(javaTypesProvider, fjTypesProvider)
        , jackTypeRefType
        , jackTypeFactoryType
        , adt
        , (drvUtils, instUtils, mspec) ->
            genDeserializerCode(drvUtils, instUtils, adt, mspec))));
  }

  private static DeriveResult<DerivedCodeSpec> genInstance(DeriveUtils deriveUtils
    , ClassName instanceClassName
    , TypeElement instanceType
    , DeclaredType wildCardInstanceType
    , List<TypeElement> typesProvider
    , TypeElement jackTypeRefType
    , TypeElement jackTypeFactoryType
    , AlgebraicDataType adt
    , F3<DeriveUtils, InstanceUtils, MethodSpec, MethodSpec> genCode) {
    return deriveUtils.generateInstance(adt
      , instanceClassName
      , typesProvider
      , instanceUtils -> {
        final DeclaredType instanceDeclType = deriveUtils
          .types()
          .getDeclaredType(instanceType, adt.typeConstructor().declaredType());

        final DeclaredType jackTypeRefDeclType = deriveUtils
          .types()
          .getDeclaredType(jackTypeRefType, adt.typeConstructor().declaredType());

        final Name adtName = adt.typeConstructor().typeElement().getSimpleName();

        final String instanceTypeName = adtName + instanceType.getSimpleName().toString();

        final String instanceVarName =
          instanceUtils.adtVariableName() + instanceType.getSimpleName().toString();

        return DerivedCodeSpec
          .codeSpec(TypeSpec
              .classBuilder(instanceTypeName)

              .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)

              .addTypeVariables(adt
                .typeConstructor()
                .typeVariables()
                .stream()
                .map(TypeVariableName::get)
                .collect(Collectors.toList()))

              .superclass(TypeName.get(instanceDeclType))

              .addMethod(MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addCode("super($T.defaultInstance().$N($L));\n"
                  , jackTypeFactoryType
                  , "constructType"
                  , TypeSpec
                    .anonymousClassBuilder("")
                    .superclass(TypeName.get(jackTypeRefDeclType))
                    .build())
                .build())

              .addMethods(deriveUtils
                .allAbstractMethods(instanceType)
                .stream()
                .map(m -> {
                  final MethodSpec methodSpec = deriveUtils
                    .overrideMethodBuilder(m, instanceDeclType)
                    .build();

                  return genCode.f(deriveUtils, instanceUtils, methodSpec);
                })
                .collect(Collectors.toList()))
              .build()

            , FieldSpec
              .builder(TypeName.get(wildCardInstanceType)
                , instanceVarName
                , Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)

              .initializer("new $N()", instanceTypeName)
              .build()

            , MethodSpec
              .methodBuilder(instanceVarName)

              .addModifiers(Modifier.PUBLIC, Modifier.STATIC)

              .addTypeVariables(instanceType
                .getTypeParameters()
                .stream()
                .map(TypeVariableName::get)
                .collect(Collectors.toList()))

              .returns(TypeName.get(instanceDeclType))

              .addAnnotation(AnnotationSpec
                .builder(SuppressWarnings.class)
                .addMember("value", "$S", "unchecked")
                .build())

              .addCode("return ($T) $N;\n", TypeName.get(instanceDeclType), instanceVarName)
              .build());
      });
  }

  private static MethodSpec genSerializerCode(DeriveUtils deriveUtils
    , InstanceUtils instanceUtils
    , MethodSpec methodSpec) {
    final ParameterSpec adtParam = methodSpec.parameters.get(0);
    final ParameterSpec jacksonGen = methodSpec.parameters.get(1);
    final ParameterSpec serProvider = methodSpec.parameters.get(2);

    final ClassName unitClassName = ClassName.get("fj", "Unit");

    return methodSpec
      .toBuilder()
      .addStatement("$N.writeStartObject()", jacksonGen)

      .addCode(CodeBlock.builder()
        .add("$N.", adtParam)
        .add(instanceUtils.matchImpl(dataConstructor -> deriveUtils
          .lambdaImpl(dataConstructor
            , CodeBlock
              .builder()
              .add("{\n")
              .indent()
              .beginControlFlow("try")

              .add("$N.writeStringField($S, $S);\n"
                , jacksonGen
                , "valueConstructor"
                , dataConstructor.name())

              .add(dataConstructor
                .arguments()
                .stream()
                .reduce(CodeBlock.of("\n")
                  , (cb, darg) -> cb.toBuilder()
                    .add("$N.writeFieldName($S);\n", jacksonGen, darg.fieldName())
                    .add("$L.serialize($N, $N, $N);\n\n"
                      , instanceUtils.instanceFor(darg)
                      , darg.fieldName()
                      , jacksonGen
                      , serProvider)
                    .build()
                  , (cb1, cb2) -> cb1.toBuilder().add(cb2).build()))

              .endControlFlow()
              .beginControlFlow("catch (IOException e)")
              .add("throw new $T(e);\n", RuntimeException.class)
              .endControlFlow()
              .add("return $T.unit();\n", unitClassName)
              .unindent()
              .add("}")
              .build())))
        .add(";\n")
        .build())

      .addStatement("$N.writeEndObject()", jacksonGen)

      .build();
  }

  private static MethodSpec genDeserializerCode(DeriveUtils deriveUtils
    , InstanceUtils instanceUtils
    , AlgebraicDataType adt
    , MethodSpec methodSpec) {
    final ClassName
      jsonNodeClassName =
        ClassName.get("com.fasterxml.jackson.databind", "JsonNode"),
      codecClassName =
        ClassName.get("com.fasterxml.jackson.core", "ObjectCodec"),
      jsonParseExceptionClassName =
        ClassName.get("com.fasterxml.jackson.core", "JsonParseException");

    final ParameterSpec jacksonParser = methodSpec.parameters.get(0);
    final ParameterSpec deserCtx = methodSpec.parameters.get(1);

    final String
      jsonNode = "jsonNode",
      valueConstructor = "valueConstructor",
      codec = "codec";

    return methodSpec
      .toBuilder()

      .addCode("final $T $N = $N.readValueAsTree();\n"
        , jsonNodeClassName
        , jsonNode
        , jacksonParser)

      .addCode("final String $N = $N.findPath($S).asText();\n"
        , valueConstructor
        , jsonNode
        , valueConstructor)

      .addCode("final $T $N = $N.getCodec();\n"
        , codecClassName
        , codec
        , jacksonParser)

      .beginControlFlow("\nswitch($N)", valueConstructor)

      .addCode(adt
        .dataConstruction()
        .constructors()
        .stream()
        .reduce(CodeBlock.of("")

          , (cb, dataConstructor) -> cb.toBuilder()
            .beginControlFlow("case $S: ", dataConstructor.name())

            .add(dataConstructor
              .arguments()
              .stream()
              .reduce(CodeBlock.of("")
                , (cb_, darg) -> {
                  final String dargParser = darg.fieldName() + "Parser";

                  return cb_.toBuilder()
                    .add("final $T $N = $N.findPath($S).traverse($N);\n"
                      , jacksonParser.type
                      , dargParser
                      , jsonNode
                      , darg.fieldName()
                      , codec)

                    .add("$N.nextToken();\n", dargParser)

                    .add("final $T $N =\n", darg.type(), darg.fieldName())
                    .indent()
                    .add("$L.deserialize($N, $N);\n\n"
                      , instanceUtils.instanceFor(darg)
                      , dargParser
                      , deserCtx)
                    .unindent()

                    .build();
                }
                , (cb1, cb2) -> cb1.toBuilder().add(cb2).build()))

            .add("return $T.$N($L);\n"
              , adt.deriveConfig().targetClass().className()
              , dataConstructor.name()
              , CodeBlock.of(dataConstructor
                .arguments()
                .stream()
                .map(DataArgument::fieldName)
                .collect(Collectors.joining(", "))))

            .endControlFlow()
            .build()

          , (cb1, cb2) -> cb1.toBuilder().add(cb2).build()))

      .beginControlFlow("default:")
      .addCode("throw new $T($N, $S);\n"
        , jsonParseExceptionClassName
        , jacksonParser
        , "Unknown value constructor")
      .endControlFlow()

      .endControlFlow()
      .build();
  }

  private static TypeElement getTypeElement(DeriveUtils deriveUtils, ClassName stdSerClassName) {
    return deriveUtils.elements().getTypeElement(stdSerClassName.toString());
  }

  private static DeclaredType getDeclaredWildcardedType(DeriveUtils deriveUtils, TypeElement stdSerType) {
    return deriveUtils.types().getDeclaredType(stdSerType
      , deriveUtils.types().getWildcardType(null, null));
  }

  private interface F3<A, B, C, D> {
    D f(A a, B b, C c);
  }
}


