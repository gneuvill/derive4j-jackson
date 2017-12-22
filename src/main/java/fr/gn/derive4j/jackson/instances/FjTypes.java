package fr.gn.derive4j.jackson.instances;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import fj.Unit;
import fj.data.*;
import fj.function.TryEffect0;

import java.io.IOException;
import java.util.Arrays;

public final class FjTypes {
  private FjTypes() {}

  private static final class FieldNameFor {
    static final String
      valueConstructor = "_tag", value = "value";
  }

  private static final class FieldValueFor {
    static final class Option {
      static final String
        someValueConstructor = "some"
        , noneValueConstructor = "none";
    }

    static final class Either {
      static final String
        leftValueConstructor = "left"
        , rightValueConstructor = "right";
    }
  }

  // ## Serializers

  public static <T> StdSerializer<Option<T>> optionStdSerializer(StdSerializer<T> tSer) {
    return new StdSerializer<Option<T>>(Option.class, true) {
      public void serialize(Option<T> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();

        value
          .<IO<Unit>>option(() -> writeValueConstructor(gen, FieldValueFor.Option.noneValueConstructor)
            , t -> sequence(writeValueConstructor(gen, FieldValueFor.Option.someValueConstructor)
              , writeValue(gen, provider, tSer, t)))
          .run();

        gen.writeEndObject();
      }
    };
  }

  public static <T> StdSerializer<List<T>> listStdSerializer(StdSerializer<T> tSer) {
    return new StdSerializer<List<T>>(List.class, true) {
      public void serialize(List<T> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartArray();

        value.foreachDoEffect(t -> {
          try {
            tSer.serialize(t, gen, provider);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });

        gen.writeEndArray();
      }
    };
  }

  public static <A, B> StdSerializer<Either<A, B>> eitherStdSerializer(StdSerializer<A> leftSer, StdSerializer<B> rightSer) {
    return new StdSerializer<Either<A, B>>(Either.class, true) {
      public void serialize(Either<A, B> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();

        value
          .either(left ->
              sequence(writeValueConstructor(gen, FieldValueFor.Either.leftValueConstructor)
                , writeValue(gen, provider, leftSer, left))
            , right ->
              sequence(writeValueConstructor(gen, FieldValueFor.Either.rightValueConstructor)
                , writeValue(gen, provider, rightSer, right)))
          .run();

        gen.writeEndObject();
      }

    };
  }

  // ## Deserializers

  public static <T> StdDeserializer<List<T>> listStdDeserializer(StdDeserializer<T> tDeser) {
    return new StdDeserializer<List<T>>(List.class) {
      public List<T> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (!p.isExpectedStartArrayToken())
          throw new JsonParseException(p, "Current token is not the start of an array");

        final JsonNode jsonNode = p.readValueAsTree();
        final List.Buffer<T> buffer = new List.Buffer<>();

        jsonNode.elements().forEachRemaining(jsn -> {
          final JsonParser eltParser = jsn.traverse(p.getCodec());
          try {
            eltParser.nextToken();
            final T t = tDeser.deserialize(eltParser, ctxt);
            buffer.snoc(t);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });

        return buffer.toList();
      }
    };
  }

  public static <A, B> StdDeserializer<Either<A, B>> eitherStdDeserializer(StdDeserializer<A> leftDeser, StdDeserializer<B> rightDeser) {
    return new StdDeserializer<Either<A, B>>(Either.class) {
      public Either<A, B> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        if (!p.isExpectedStartObjectToken())
          throw new JsonParseException(p, "Current token is not the start of an object");

        final JsonNode jsonNode = p.readValueAsTree();
        final ObjectCodec codec = p.getCodec();
        final String valueConstructor =
          jsonNode.findPath(FieldNameFor.valueConstructor).asText();

        switch (valueConstructor) {

          case FieldValueFor.Either.leftValueConstructor:
            return Either.left(readValue(ctxt, jsonNode, codec, leftDeser));

          case FieldValueFor.Either.rightValueConstructor:
            return Either.right(readValue(ctxt, jsonNode, codec, rightDeser));

          default: throw new JsonParseException(p, "Unknown value constructor");
        }
      }
    };
  }


  private static <T> IO<Unit> writeValueConstructor(JsonGenerator gen, String valueConstructor) {
    return fromTryEffect(() -> {
      gen.writeStringField(FieldNameFor.valueConstructor, valueConstructor);
    });
  }

  private static <T> IO<Unit> writeValue(JsonGenerator gen
    , SerializerProvider provider
    , StdSerializer<T> ser
    , T left) {
    return fromTryEffect(() -> {
      gen.writeFieldName(FieldNameFor.value);
      ser.serialize(left, gen, provider);
    });
  }

  private static <T> T readValue(DeserializationContext ctxt
    , JsonNode jsonNode
    , ObjectCodec codec
    , StdDeserializer<T> deser) throws IOException {
    final JsonParser valueParser = jsonNode.findPath(FieldNameFor.value).traverse(codec);
    valueParser.nextToken();
    return deser.deserialize(valueParser, ctxt);
  }

  private static IO<Unit> sequence(IO<?>... ios) {
    return IOFunctions
      .map(Arrays
          .stream(ios)
          .reduce(IOFunctions.unit(Unit.unit()), (io1, io2) -> IOFunctions.bind(io1, __ -> io2))
        , __ -> Unit.unit());
  }

  private static IO<Unit> fromTryEffect(TryEffect0<? extends IOException> t) {
    return () -> { t.f(); return Unit.unit(); };
  }
}
