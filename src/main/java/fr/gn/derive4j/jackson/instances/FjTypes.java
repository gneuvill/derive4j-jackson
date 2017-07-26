package fr.gn.derive4j.jackson.instances;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import fj.data.List;

import java.io.IOException;

public final class FjTypes {
  private FjTypes() {}

  // ## Serializers

  public static <T> StdSerializer<List<T>> listStdSerializer(StdSerializer<T> tSer) {
    return new _ListSer<>(tSer);
  }

  private static final class _ListSer<T> extends StdSerializer<List<T>> {
    private final StdSerializer<T> tSer;
    private  _ListSer(StdSerializer<T> tSer) {
      super(TypeFactory.defaultInstance().constructType(new TypeReference<List<T>>() {}));
      this.tSer = tSer;
    }

    @Override
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
  }

  // ## Deserializers

  public static <T> StdDeserializer<List<T>> listStdDeserializer(StdDeserializer<T> tDeser) {
    return new _ListDeser<>(tDeser);
  }

  private static final class _ListDeser<T> extends StdDeserializer<List<T>> {
    private final StdDeserializer<T> tDeser;
    private _ListDeser(StdDeserializer<T> tDeser) {
      super(TypeFactory.defaultInstance().constructType(new TypeReference<List<T>>() {}));
      this.tDeser = tDeser;
    }

    @Override
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
  }
}
