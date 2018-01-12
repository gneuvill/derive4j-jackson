package fr.gn.derive4j.jackson.instances;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;

import static fr.gn.derive4j.Functions.stdDeserializer_;
import static fr.gn.derive4j.Functions.stdSerializer_;

public final class JavaTypes {
  private JavaTypes() {}

  // ## Serializers

  public static final StdSerializer<String> stringSerializer =
    stdSerializer_(String.class, (value, gen) -> gen.writeString(value));

  public static final StdSerializer<Integer> integerSerializer =
    stdSerializer_(Integer.class, (value, gen) -> gen.writeNumber(value));

  public static final StdSerializer<LocalDate> localDateSerializer =
    stdSerializer_(LocalDate.class, (value, gen) -> gen.writeString(DateTimeFormatter.ISO_DATE.format(value)));

  public static final StdSerializer<LocalDateTime> localDateTimeSerializer =
    stdSerializer_(LocalDateTime.class, (value, gen) -> gen.writeString(DateTimeFormatter.ISO_DATE_TIME.format(value)));

  public static final StdSerializer<Instant> instantSerializer =
    stdSerializer_(Instant.class, (value, gen) -> gen.writeString(DateTimeFormatter.ISO_INSTANT.format(value)));

  public static final StdSerializer<Period> periodSerializer =
    stdSerializer_(Period.class, (value, gen) -> gen.writeString(value.toString()));

  // ## Deserializers

  public static final StdDeserializer<String> stringDeserializer =
    stdDeserializer_(String.class, JsonParser::getValueAsString);

  public static final StdDeserializer<Integer> integerDeserializer =
    stdDeserializer_(Integer.class, JsonParser::getValueAsInt);

  public static final StdDeserializer<LocalDate> localDateDeserializer =
    stdDeserializer_(LocalDate.class, p -> DateTimeFormatter.ISO_DATE.parse(p.getValueAsString(), LocalDate::from));

  public static final StdDeserializer<LocalDateTime> localDateTimeDeserializer =
    stdDeserializer_(LocalDateTime.class, p -> DateTimeFormatter.ISO_DATE_TIME.parse(p.getValueAsString(), LocalDateTime::from));

  public static final StdDeserializer<Instant> instantDeserializer =
    stdDeserializer_(Instant.class, p -> DateTimeFormatter.ISO_INSTANT.parse(p.getValueAsString(), Instant::from));

  public static final StdDeserializer<Period> periodDeserializer =
    stdDeserializer_(Period.class, p -> Period.parse(p.getValueAsString()));
}
