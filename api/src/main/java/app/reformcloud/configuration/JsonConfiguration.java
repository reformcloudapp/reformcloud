/*
 * This file is part of reformcloud, licensed under the MIT License (MIT).
 *
 * Copyright (c) ReformCloud <https://github.com/reformcloudapp>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package app.reformcloud.configuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import app.reformcloud.configuration.json.Element;
import app.reformcloud.configuration.json.JsonFactories;
import app.reformcloud.configuration.json.JsonParser;
import app.reformcloud.configuration.json.adapter.JsonAdapter;
import app.reformcloud.configuration.json.types.Object;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class JsonConfiguration implements Configurable<Element, JsonConfiguration> {

    public static final JsonAdapter DEFAULT_ADAPTER = JsonAdapter.builder()
            .disableHtmlEscaping()
            .enablePrettyPrinting()
            .enableNullSerialisation()
            .build();

    protected static final Predicate<?> ALWAYS_TRUE = ignored -> true;

    protected final transient Object json;
    protected final transient JsonAdapter adapter;

    protected JsonConfiguration() {
        this(JsonFactories.newObject());
    }

    protected JsonConfiguration(Object json) {
        this(json, DEFAULT_ADAPTER);
    }

    protected JsonConfiguration(JsonAdapter adapter) {
        this(JsonFactories.newObject(), adapter);
    }

    protected JsonConfiguration(Object json, JsonAdapter adapter) {
        this.json = json;
        this.adapter = adapter;
    }

    @NotNull
    public static JsonConfiguration newJsonConfiguration() {
        return new JsonConfiguration();
    }

    @NotNull
    public static JsonConfiguration newJsonConfiguration(@NotNull String json) {
        var backing = JsonParser.defaultParser().parse(json);
        return backing.isObject()
                ? new JsonConfiguration(backing.getAsObject())
                : new JsonConfiguration();
    }

    @NotNull
    public static JsonConfiguration newJsonConfiguration(@NotNull Object backingObject) {
        return new JsonConfiguration(backingObject);
    }

    @NotNull
    public static JsonConfiguration newJsonConfiguration(@NotNull JsonAdapter jsonAdapter) {
        return new JsonConfiguration(jsonAdapter);
    }

    @NotNull
    public static JsonConfiguration newJsonConfiguration(@NotNull Object backingObject, @NotNull JsonAdapter jsonAdapter) {
        return new JsonConfiguration(backingObject, jsonAdapter);
    }

    @NotNull
    public static JsonConfiguration newJsonConfiguration(@NotNull File file) {
        return newJsonConfiguration(file.toPath());
    }

    @NotNull
    public static JsonConfiguration newJsonConfiguration(@NotNull Path path) {
        return newJsonConfiguration(path, DEFAULT_ADAPTER);
    }

    @NotNull
    public static JsonConfiguration newJsonConfiguration(byte[] bytes) {
        return newJsonConfiguration(bytes, DEFAULT_ADAPTER);
    }

    @NotNull
    public static JsonConfiguration newJsonConfiguration(@NotNull InputStream stream) {
        return newJsonConfiguration(stream, DEFAULT_ADAPTER);
    }

    @NotNull
    public static JsonConfiguration newJsonConfiguration(@NotNull Reader reader) {
        return newJsonConfiguration(reader, DEFAULT_ADAPTER);
    }

    @NotNull
    public static JsonConfiguration newJsonConfiguration(@NotNull String path, @NotNull JsonAdapter jsonAdapter) {
        return newJsonConfiguration(Paths.get(path), jsonAdapter);
    }

    @NotNull
    public static JsonConfiguration newJsonConfiguration(@NotNull File file, @NotNull JsonAdapter jsonAdapter) {
        return newJsonConfiguration(file.toPath(), jsonAdapter);
    }

    @NotNull
    public static JsonConfiguration newJsonConfiguration(@NotNull Path path, @NotNull JsonAdapter jsonAdapter) {
        if (Files.exists(path)) {
            try (var stream = Files.newInputStream(path)) {
                return newJsonConfiguration(stream, jsonAdapter);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
        return newJsonConfiguration(jsonAdapter);
    }

    @NotNull
    public static JsonConfiguration newJsonConfiguration(byte[] bytes, @NotNull JsonAdapter jsonAdapter) {
        try (var stream = new ByteArrayInputStream(bytes)) {
            return newJsonConfiguration(stream, jsonAdapter);
        } catch (IOException exception) {
            exception.printStackTrace();
            return newJsonConfiguration();
        }
    }

    @NotNull
    public static JsonConfiguration newJsonConfiguration(@NotNull InputStream stream, @NotNull JsonAdapter jsonAdapter) {
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return newJsonConfiguration(reader, jsonAdapter);
        } catch (IOException exception) {
            exception.printStackTrace();
            return newJsonConfiguration(jsonAdapter);
        }
    }

    @NotNull
    public static JsonConfiguration newJsonConfiguration(@NotNull Reader reader, @NotNull JsonAdapter jsonAdapter) {
        var backingElement = JsonParser.defaultParser().parse(reader);
        return backingElement.isObject()
                ? newJsonConfiguration(backingElement.getAsObject(), jsonAdapter)
                : newJsonConfiguration(jsonAdapter);
    }

    @SuppressWarnings("unchecked")
    private static <T> Predicate<T> alwaysTrue() {
        return (Predicate<T>) ALWAYS_TRUE;
    }

    @Override
    public @NotNull JsonConfiguration add(@NotNull String key, @Nullable JsonConfiguration value) {
        if (value != null) {
            this.json.add(key, value.json);
        }
        return this;
    }

    @Override
    public <T> @NotNull JsonConfiguration add(@NotNull String key, @Nullable T value) {
        this.json.add(key, value == null ? JsonFactories.newNull() : this.adapter.toTree(value));
        return this;
    }

    @Override
    public @NotNull JsonConfiguration add(@NotNull String key, @Nullable String value) {
        json.add(key, value);
        return this;
    }

    @Override
    public @NotNull JsonConfiguration add(@NotNull String key, @Nullable Number value) {
        json.add(key, value);
        return this;
    }

    @Override
    public @NotNull JsonConfiguration add(@NotNull String key, @Nullable Boolean value) {
        json.add(key, value);
        return this;
    }

    @Override
    public @NotNull JsonConfiguration add(@NotNull String key, @Nullable Character value) {
        json.add(key, value);
        return this;
    }

    @Override
    public @NotNull JsonConfiguration remove(@NotNull String key) {
        json.remove(key);
        return this;
    }

    @Override
    public @NotNull JsonConfiguration get(@NotNull String key) {
        return getOrDefault(key, newJsonConfiguration());
    }

    @Override
    public <T> @Nullable T get(@NotNull String key, @NotNull Class<T> type) {
        return getOrDefault(key, type, null);
    }

    @Override
    public <T> @Nullable T get(@NotNull String key, @NotNull Type type) {
        return getOrDefault(key, type, null);
    }

    @Override
    public @NotNull String getString(@NotNull String key) {
        return getOrDefault(key, "");
    }

    @Override
    public @NotNull Integer getInteger(@NotNull String key) {
        return getOrDefault(key, 0);
    }

    @Override
    public @NotNull Long getLong(@NotNull String key) {
        return getOrDefault(key, 0L);
    }

    @Override
    public @NotNull Short getShort(@NotNull String key) {
        return getOrDefault(key, (short) 0);
    }

    @Override
    public @NotNull Byte getByte(@NotNull String key) {
        return getOrDefault(key, (byte) 0);
    }

    @Override
    public @NotNull Double getDouble(@NotNull String key) {
        return getOrDefault(key, 0D);
    }

    @Override
    public @NotNull Float getFloat(@NotNull String key) {
        return getOrDefault(key, 0F);
    }

    @Override
    public @NotNull Boolean getBoolean(@NotNull String key) {
        return getOrDefault(key, false);
    }

    @Override
    public @NotNull Character getCharacter(@NotNull String key) {
        return getOrDefault(key, ' ');
    }

    @Override
    public JsonConfiguration getOrDefault(@NotNull String key, JsonConfiguration def) {
        return getOrDefaultIf(key, def, alwaysTrue());
    }

    @Override
    public <T> T getOrDefault(@NotNull String key, Type type, T def) {
        return getOrDefaultIf(key, type, def, alwaysTrue());
    }

    @Override
    public <T> T getOrDefault(@NotNull String key, Class<T> type, T def) {
        return getOrDefaultIf(key, type, def, alwaysTrue());
    }

    @Override
    public String getOrDefault(@NotNull String key, String def) {
        return getOrDefaultIf(key, def, alwaysTrue());
    }

    @Override
    public Integer getOrDefault(@NotNull String key, Integer def) {
        return getOrDefaultIf(key, def, alwaysTrue());
    }

    @Override
    public Long getOrDefault(@NotNull String key, Long def) {
        return getOrDefaultIf(key, def, alwaysTrue());
    }

    @Override
    public Short getOrDefault(@NotNull String key, Short def) {
        return getOrDefaultIf(key, def, alwaysTrue());
    }

    @Override
    public Byte getOrDefault(@NotNull String key, Byte def) {
        return getOrDefaultIf(key, def, alwaysTrue());
    }

    @Override
    public Boolean getOrDefault(@NotNull String key, Boolean def) {
        return getOrDefaultIf(key, def, alwaysTrue());
    }

    @Override
    public Double getOrDefault(@NotNull String key, Double def) {
        return getOrDefaultIf(key, def, alwaysTrue());
    }

    @Override
    public Float getOrDefault(@NotNull String key, Float def) {
        return getOrDefaultIf(key, def, alwaysTrue());
    }

    @Override
    public Character getOrDefault(@NotNull String key, Character def) {
        return getOrDefaultIf(key, def, alwaysTrue());
    }

    @Override
    public JsonConfiguration getOrDefaultIf(@NotNull String key, JsonConfiguration def,
                                            @NotNull Predicate<JsonConfiguration> predicate) {
        return getInternal(key)
                .filter(Element::isObject)
                .map(el -> newJsonConfiguration(el.getAsObject(), this.adapter))
                .filter(predicate)
                .orElse(def);
    }

    @Override
    public <T> T getOrDefaultIf(@NotNull String key, Type type, T def, @NotNull Predicate<T> predicate) {
        return getInternal(key)
                .map(el -> adapter.<T>fromJson(el, type))
                .filter(predicate)
                .orElse(def);
    }

    @Override
    public <T> T getOrDefaultIf(@NotNull String key, Class<T> type, T def, @NotNull Predicate<T> predicate) {
        return getOrDefaultIf(key, (Type) type, def, predicate);
    }

    @Override
    public String getOrDefaultIf(@NotNull String key, String def, @NotNull Predicate<String> predicate) {
        return json.get(key)
                .map(Element::getAsString)
                .filter(predicate)
                .orElse(def);
    }

    @Override
    public Integer getOrDefaultIf(@NotNull String key, Integer def, @NotNull Predicate<Integer> predicate) {
        return json.get(key)
                .map(Element::getAsInt)
                .filter(predicate)
                .orElse(def);
    }

    @Override
    public Long getOrDefaultIf(@NotNull String key, Long def, @NotNull Predicate<Long> predicate) {
        return json.get(key)
                .map(Element::getAsLong)
                .filter(predicate)
                .orElse(def);
    }

    @Override
    public Short getOrDefaultIf(@NotNull String key, Short def,
                                @NotNull Predicate<Short> predicate) {
        return json.get(key)
                .map(Element::getAsShort)
                .filter(predicate)
                .orElse(def);
    }

    @Override
    public Byte getOrDefaultIf(@NotNull String key, Byte def, @NotNull Predicate<Byte> predicate) {
        return json.get(key)
                .map(Element::getAsByte)
                .filter(predicate)
                .orElse(def);
    }

    @Override
    public Boolean getOrDefaultIf(@NotNull String key, Boolean def, @NotNull Predicate<Boolean> predicate) {
        return json.get(key)
                .map(Element::getAsBoolean)
                .filter(predicate)
                .orElse(def);
    }

    @Override
    public Double getOrDefaultIf(@NotNull String key, Double def, @NotNull Predicate<Double> predicate) {
        return json.get(key)
                .map(Element::getAsDouble)
                .filter(predicate)
                .orElse(def);
    }

    @Override
    public Float getOrDefaultIf(@NotNull String key, Float def, @NotNull Predicate<Float> predicate) {
        return json.get(key)
                .map(Element::getAsFloat)
                .filter(predicate)
                .orElse(def);
    }

    @Override
    public Character getOrDefaultIf(@NotNull String key, Character def,
                                    @NotNull Predicate<Character> predicate) {
        return json.get(key)
                .map(e -> e.getAsString().charAt(0))
                .filter(predicate)
                .orElse(def);
    }

    @Override
    public boolean has(@NotNull String key) {
        return json.has(key);
    }

    @Override
    public void write(@NotNull Path path) {
        var parent = path.getParent();
        if (parent != null && Files.notExists(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (IOException exception) {
                exception.printStackTrace();
                return;
            }
        }

        try (var writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            adapter.toJson(json, writer);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void write(@NotNull String path) {
        write(Paths.get(path));
    }

    @Override
    public void write(@NotNull File file) {
        write(file.toPath());
    }

    @Override
    public @NotNull String toPrettyString() {
        return adapter.toJson(json);
    }

    @Override
    public byte[] toPrettyBytes() {
        return toPrettyString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public @NotNull Map<String, Element> asMap() {
        return json.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public void clear() {
        var iterator = json.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            iterator.remove();
        }
    }

    @Override
    public @NotNull JsonConfiguration clone() {
        return newJsonConfiguration(json.clone(), adapter);
    }

    @NotNull
    private Optional<Element> getInternal(@NotNull String key) {
        return json.get(key);
    }

    @NotNull
    public Object getBackingObject() {
        return json;
    }

    @NotNull
    public JsonAdapter getJsonAdapter() {
        return adapter;
    }
}