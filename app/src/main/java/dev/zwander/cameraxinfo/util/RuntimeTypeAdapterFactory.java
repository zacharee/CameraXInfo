package dev.zwander.cameraxinfo.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RuntimeTypeAdapterFactory<T> implements TypeAdapterFactory {
  private final Class<?> baseType;
  private final String typeFieldName;
  private final Map<String, Class<?>> labelToSubtype = new LinkedHashMap<>();
  private final Map<Class<?>, String> subtypeToLabel = new LinkedHashMap<>();
  private final boolean maintainType;
  private boolean recognizeSubtypes;

  private RuntimeTypeAdapterFactory(
      Class<?> baseType, String typeFieldName, boolean maintainType) {
    if (typeFieldName == null || baseType == null) {
      throw new NullPointerException();
    }
    this.baseType = baseType;
    this.typeFieldName = typeFieldName;
    this.maintainType = maintainType;
  }

  /**
   * Creates a new runtime type adapter using for {@code baseType} using {@code
   * typeFieldName} as the type field name. Type field names are case sensitive.
   *
   * @param maintainType true if the type field should be included in deserialized objects
   */
  public static <T> RuntimeTypeAdapterFactory<T> of(Class<T> baseType, String typeFieldName, boolean maintainType) {
    return new RuntimeTypeAdapterFactory<>(baseType, typeFieldName, maintainType);
  }
  
  /**
   * Creates a new runtime type adapter using for {@code baseType} using {@code
   * typeFieldName} as the type field name. Type field names are case sensitive.
   */
  public static <T> RuntimeTypeAdapterFactory<T> of(Class<T> baseType, String typeFieldName) {
    return new RuntimeTypeAdapterFactory<>(baseType, typeFieldName, false);
  }

  /**
   * Creates a new runtime type adapter for {@code baseType} using {@code "type"} as
   * the type field name.
   */
  public static <T> RuntimeTypeAdapterFactory<T> of(Class<T> baseType) {
    return new RuntimeTypeAdapterFactory<>(baseType, "type", false);
  }

  /**
   * Ensures that this factory will handle not just the given {@code baseType}, but any subtype
   * of that type.
   */
  public RuntimeTypeAdapterFactory<T> recognizeSubtypes() {
    this.recognizeSubtypes = true;
    return this;
  }

  /**
   * Registers {@code type} identified by {@code label}. Labels are case
   * sensitive.
   *
   * @throws IllegalArgumentException if either {@code type} or {@code label}
   *     have already been registered on this type adapter.
   */
  public RuntimeTypeAdapterFactory<T> registerSubtype(Class<? extends T> type, String label) {
    if (type == null || label == null) {
      throw new NullPointerException();
    }
    if (subtypeToLabel.containsKey(type) || labelToSubtype.containsKey(label)) {
      throw new IllegalArgumentException("types and labels must be unique");
    }
    labelToSubtype.put(label, type);
    subtypeToLabel.put(type, label);
    return this;
  }

  /**
   * Registers {@code type} identified by its {@link Class#getSimpleName simple
   * name}. Labels are case sensitive.
   *
   * @throws IllegalArgumentException if either {@code type} or its simple name
   *     have already been registered on this type adapter.
   */
  public RuntimeTypeAdapterFactory<T> registerSubtype(Class<? extends T> type) {
    return registerSubtype(type, type.getSimpleName());
  }

  @Override
  public <R> TypeAdapter<R> create(Gson gson, TypeToken<R> type) {
    if (type == null) {
      return null;
    }
    Class<?> rawType = type.getRawType();
    boolean handle =
        recognizeSubtypes ? baseType.isAssignableFrom(rawType) : baseType.equals(rawType);
    if (!handle) {
      return null;
    }

    final TypeAdapter<JsonElement> jsonElementAdapter = gson.getAdapter(JsonElement.class);
    final Map<String, TypeAdapter<?>> labelToDelegate = new LinkedHashMap<>();
    final Map<Class<?>, TypeAdapter<?>> subtypeToDelegate = new LinkedHashMap<>();
    for (Map.Entry<String, Class<?>> entry : labelToSubtype.entrySet()) {
      TypeAdapter<?> delegate = gson.getDelegateAdapter(this, TypeToken.get(entry.getValue()));
      labelToDelegate.put(entry.getKey(), delegate);
      subtypeToDelegate.put(entry.getValue(), delegate);
    }

    return new TypeAdapter<R>() {
      @Override public R read(JsonReader in) throws IOException {
        JsonElement jsonElement = jsonElementAdapter.read(in);
        JsonElement labelJsonElement;
        if (maintainType) {
            labelJsonElement = jsonElement.getAsJsonObject().get(typeFieldName);
        } else {
            labelJsonElement = jsonElement.getAsJsonObject().remove(typeFieldName);
        }
        
        if (labelJsonElement == null) {
          throw new JsonParseException("cannot deserialize " + baseType
              + " because it does not define a field named " + typeFieldName);
        }
        String label = labelJsonElement.getAsString();
        @SuppressWarnings("unchecked") // registration requires that subtype extends T
        TypeAdapter<R> delegate = (TypeAdapter<R>) labelToDelegate.get(label);
        if (delegate == null) {
          throw new JsonParseException("cannot deserialize " + baseType + " subtype named "
              + label + "; did you forget to register a subtype?");
        }
        return delegate.fromJsonTree(jsonElement);
      }

      @Override public void write(JsonWriter out, R value) throws IOException {
        Class<?> srcType = value.getClass();
        String label = subtypeToLabel.get(srcType);
        @SuppressWarnings("unchecked") // registration requires that subtype extends T
        TypeAdapter<R> delegate = (TypeAdapter<R>) subtypeToDelegate.get(srcType);
        if (delegate == null) {
          throw new JsonParseException("cannot serialize " + srcType.getName()
              + "; did you forget to register a subtype?");
        }
        JsonObject jsonObject = delegate.toJsonTree(value).getAsJsonObject();

        if (maintainType) {
          jsonElementAdapter.write(out, jsonObject);
          return;
        }

        JsonObject clone = new JsonObject();

        if (jsonObject.has(typeFieldName)) {
          throw new JsonParseException("cannot serialize " + srcType.getName()
              + " because it already defines a field named " + typeFieldName);
        }
        clone.add(typeFieldName, new JsonPrimitive(label));
        
        for (Map.Entry<String, JsonElement> e : jsonObject.entrySet()) {
          clone.add(e.getKey(), e.getValue());
        }
        jsonElementAdapter.write(out, clone);
      }
    }.nullSafe();
  }
}