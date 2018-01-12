package fr.gn.derive4j.processor.jackson;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import fr.gn.derive4j.Constants.FieldNameFor;
import fr.gn.derive4j.jackson.instances.FjTypes;
import fr.gn.derive4j.jackson.instances.JavaTypes;
import org.derive4j.processor.api.*;
import org.derive4j.processor.api.model.AlgebraicDataType;
import org.derive4j.processor.api.model.DataArgument;

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

      stdDeserType = getTypeElement(deriveUtils, stdDeserClassName);

    final List<TypeElement> typesProvider =
      asList(getTypeElement(deriveUtils, ClassName.get(JavaTypes.class))
        , getTypeElement(deriveUtils, ClassName.get(FjTypes.class)));

    return
      asList(selection(stdSerClassName, adt -> genInstance(deriveUtils
        , stdSerClassName
        , stdSerType
        , deriveUtils
          .types()
          .getDeclaredType(stdSerType, adt.typeConstructor().declaredType())
        , TypeSpec
          .anonymousClassBuilder("$T.class, $L"
            , deriveUtils.types().erasure(adt.typeConstructor().declaredType())
            , true)
        , typesProvider
        , adt
        , JacksonDerivations::genSerializerCode))

        , selection(stdDeserClassName, adt -> genInstance(deriveUtils
          , stdDeserClassName
          , stdDeserType
          , deriveUtils
            .types()
            .getDeclaredType(stdDeserType, adt.typeConstructor().typeVariables().isEmpty()
              ? adt.typeConstructor().declaredType()
              : getDeclaredWildcardedType(deriveUtils, adt.typeConstructor().typeElement()))
          , TypeSpec
            .anonymousClassBuilder("$T.class"
              , deriveUtils.types().erasure(adt.typeConstructor().declaredType()))
          , typesProvider
          , adt
          , (drvUtils, instUtils, mspec) ->
            genDeserializerCode(drvUtils, instUtils, adt, mspec))));
  }

  private static DeriveResult<DerivedCodeSpec> genInstance(DeriveUtils deriveUtils
    , ClassName instanceClassName
    , TypeElement instanceType
    , DeclaredType instanceDeclType
    , TypeSpec.Builder instanceBuilder
    , List<TypeElement> typesProvider
    , AlgebraicDataType adt
    , F3<DeriveUtils, InstanceUtils, MethodSpec, MethodSpec> genMethodCode) {
    return deriveUtils.generateInstance(adt
      , instanceClassName
      , typesProvider
      , instanceUtils -> instanceUtils.generateInstanceFactory(CodeBlock
        .of("($L)\n", instanceType.getSimpleName())
        .toBuilder()
        .indent()
        .add("$L", instanceBuilder

          .superclass(TypeName.get(instanceDeclType))

          .addMethods(deriveUtils
            .allAbstractMethods(instanceType)
            .stream()
            .map(m -> {
              final MethodSpec methodSpec = deriveUtils
                .overrideMethodBuilder(m, instanceDeclType)
                .build();

              return genMethodCode.f(deriveUtils, instanceUtils, methodSpec);
            })
            .collect(Collectors.toList()))

          .build())
        .build()));
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
                , FieldNameFor.valueConstructor
                , dataConstructor.name())

              .add(dataConstructor
                .arguments()
                .stream()
                .reduce(CodeBlock.of("\n")
                  , (cb, darg) -> {
                    final String fieldName = darg.fieldName();

                    final CodeBlock.Builder prepBuilder = cb.toBuilder()
                      .add("$N.writeFieldName($S);\n", jacksonGen, fieldName);

                    final CodeBlock fieldRef = deriveUtils.isWildcarded(darg.type())
                      ? CodeBlock.of("($T) $N"
                      , deriveUtils.types().erasure(darg.type())
                      , fieldName)

                      : CodeBlock.of("$N", fieldName);

                    return prepBuilder
                      .add("$L.serialize(", instanceUtils.instanceFor(darg))
                      .add(fieldRef)
                      .add(", $N, $N);\n\n", jacksonGen, serProvider)
                      .build();
                  }
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

    final String jsonNode = "jsonNode", codec = "codec";

    return methodSpec
      .toBuilder()

      .addCode("final $T $N = $N.readValueAsTree();\n"
        , jsonNodeClassName
        , jsonNode
        , jacksonParser)

      .addCode("final String $N = $N.findPath($S).asText();\n"
        , FieldNameFor.valueConstructor
        , jsonNode
        , FieldNameFor.valueConstructor)

      .addCode("final $T $N = $N.getCodec();\n"
        , codecClassName
        , codec
        , jacksonParser)

      .beginControlFlow("\nswitch($N)", FieldNameFor.valueConstructor)

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

                  final CodeBlock.Builder prepBuilder = cb_.toBuilder()
                    .add("final $T $N = $N.findPath($S).traverse($N);\n"
                      , jacksonParser.type
                      , dargParser
                      , jsonNode
                      , darg.fieldName()
                      , codec)
                    .add("$N.nextToken();\n", dargParser);

                  final CodeBlock.Builder assignBuilder = deriveUtils.isWildcarded(darg.type())
                      ? prepBuilder.add("final $T $N = ($T)\n"
                      , darg.type()
                      , darg.fieldName()
                      , deriveUtils.types().erasure(darg.type()))

                      : prepBuilder.add("final $T $N =\n", darg.type(), darg.fieldName());

                  return assignBuilder
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

  private static TypeElement getTypeElement(DeriveUtils deriveUtils, ClassName className) {
    return deriveUtils.elements().getTypeElement(className.toString());
  }

  private static DeclaredType getDeclaredWildcardedType(DeriveUtils deriveUtils, TypeElement typeElement) {
    return deriveUtils.types().getDeclaredType(typeElement
      , deriveUtils.types().getWildcardType(null, null));
  }

  private interface F3<A, B, C, D> {
    D f(A a, B b, C c);
  }
}


