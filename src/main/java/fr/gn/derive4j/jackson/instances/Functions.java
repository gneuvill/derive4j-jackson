package fr.gn.derive4j.jackson.instances;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import fj.function.Try1;
import fj.function.Try2;
import fj.function.TryEffect2;
import fj.function.TryEffect3;

import java.io.IOException;

final class Functions {
  private Functions() {}

  static <T> StdSerializer<T> stdSerialiser(Class<T> clazz
    , TryEffect3<T, JsonGenerator, SerializerProvider, IOException> effect) {
    return new StdSerializer<T>(clazz) {
      @Override
      public void serialize(T value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        effect.f(value, gen, provider);
      }
    };
  }

  static <T> StdSerializer<T> stdSerializer_(Class<T> clazz, TryEffect2<T, JsonGenerator, IOException> effect) {
    return stdSerialiser(clazz, (value, gen, prov) -> effect.f(value, gen));
  }

  static <T> StdDeserializer<T> stdDeserializer(Class<T> clazz
    , Try2<JsonParser, DeserializationContext, T, IOException> effect) {
    return new StdDeserializer<T>(clazz) {
      @Override
      public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return effect.f(p, ctxt);
      }
    };
  }

  static <T> StdDeserializer<T> stdDeserializer_(Class<T> clazz, Try1<JsonParser, T, IOException> effect) {
    return stdDeserializer(clazz, (p, ctx) -> effect.f(p));
  }
}
