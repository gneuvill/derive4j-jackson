package fr.gn.derive4j;

public final class Constants {
  private Constants() {}


  public static final class FieldNameFor {
    private FieldNameFor() {}

    public static final String
      valueConstructor = "_tag", value = "value";
  }

  public static final class FieldValueFor {
    private FieldValueFor() {}

    public static final class Option {
      private Option() {}

      public static final String
        someValueConstructor = "some"
        , noneValueConstructor = "none";
    }

    public static final class Either {
      private Either() {}

      public static final String
        leftValueConstructor = "left"
        , rightValueConstructor = "right";
    }
  }
}
