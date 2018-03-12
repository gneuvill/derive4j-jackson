package fr.gn.derive4j.jackson.instances;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;

import java.time.*;
import java.time.format.DateTimeFormatter;

import static fr.gn.derive4j.Functions.stdDeserializer_;
import static fr.gn.derive4j.Functions.stdSerializer_;

public final class JavaTypes {
  private JavaTypes() {}

  // ## Serializers

  public static final JsonSerializer<String> stringSerializer =
    stdSerializer_(String.class, (value, gen) -> gen.writeString(value));

  public static final JsonSerializer<Integer> integerSerializer =
    stdSerializer_(Integer.class, (value, gen) -> gen.writeNumber(value));

  public static final JsonSerializer<Boolean> booleanSerializer =
    stdSerializer_(Boolean.class, (value, gen) -> gen.writeBoolean(value));

  public static final JsonSerializer<LocalDate> localDateSerializer =
    stdSerializer_(LocalDate.class, (value, gen) -> gen.writeString(DateTimeFormatter.ISO_DATE.format(value)));

  public static final JsonSerializer<LocalDateTime> localDateTimeSerializer =
    stdSerializer_(LocalDateTime.class, (value, gen) -> gen.writeString(DateTimeFormatter.ISO_DATE_TIME.format(value)));

  public static final JsonSerializer<Instant> instantSerializer =
    stdSerializer_(Instant.class, (value, gen) -> gen.writeString(DateTimeFormatter.ISO_INSTANT.format(value)));

  public static final JsonSerializer<Period> periodSerializer =
    stdSerializer_(Period.class, (value, gen) -> gen.writeString(value.toString()));

  public static final JsonSerializer<Year> yearSerializer =
    stdSerializer_(Year.class, (value, gen) -> gen.writeString(value.toString()));

  // ## Deserializers

  public static final JsonDeserializer<String> stringDeserializer =
    stdDeserializer_(String.class, JsonParser::getValueAsString);

  public static final JsonDeserializer<Integer> integerDeserializer =
    stdDeserializer_(Integer.class, JsonParser::getValueAsInt);

  public static final JsonDeserializer<Boolean> booleanDeserializer =
    stdDeserializer_(Boolean.class, JsonParser::getValueAsBoolean);

  public static final JsonDeserializer<LocalDate> localDateDeserializer =
    stdDeserializer_(LocalDate.class, p -> DateTimeFormatter.ISO_DATE.parse(p.getValueAsString(), LocalDate::from));

  public static final JsonDeserializer<LocalDateTime> localDateTimeDeserializer =
    stdDeserializer_(LocalDateTime.class, p -> DateTimeFormatter.ISO_DATE_TIME.parse(p.getValueAsString(), LocalDateTime::from));

  public static final JsonDeserializer<Instant> instantDeserializer =
    stdDeserializer_(Instant.class, p -> DateTimeFormatter.ISO_INSTANT.parse(p.getValueAsString(), Instant::from));

  public static final JsonDeserializer<Period> periodDeserializer =
    stdDeserializer_(Period.class, p -> Period.parse(p.getValueAsString()));

  public static final JsonDeserializer<Year> yearDeserializer =
    stdDeserializer_(Year.class, p -> Year.parse(p.getValueAsString()));
}
