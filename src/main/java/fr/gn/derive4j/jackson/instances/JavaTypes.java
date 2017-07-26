package fr.gn.derive4j.jackson.instances;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public final class JavaTypes {
  private JavaTypes() {}

  // ## Serializers

  public static final StdSerializer<String> stringSerializer = new _StringSer();

  public static final StdSerializer<Integer> integerSerializer = new _IntegerSer();

  private static final class _StringSer extends StdSerializer<String> {
    private _StringSer() { super(String.class); }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
      gen.writeString(value);
    }
  }

  private static final class _IntegerSer extends StdSerializer<Integer> {
    private _IntegerSer() { super(Integer.class); }

    @Override
    public void serialize(Integer value, JsonGenerator gen, SerializerProvider provider) throws IOException {
      gen.writeNumber(value);
    }
  }

  // ## Deserializers

  public static final StdDeserializer<String> stringDeserializer = new _StringDeser();

  public static final StdDeserializer<Integer> integerDeserializer = new _IntegerDeser();

  private static final class _StringDeser extends StdDeserializer<String> {
    private _StringDeser() { super(String.class); }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      return p.getValueAsString();
    }
  }

  private static final class _IntegerDeser extends StdDeserializer<Integer> {
    private _IntegerDeser() { super(Integer.class); }

    @Override
    public Integer deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      return p.getValueAsInt();
    }
  }

}
