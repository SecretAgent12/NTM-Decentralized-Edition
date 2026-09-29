// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;

/**
 * 26.x net.minecraft.util.StrictJsonParser: one JSON document, strict syntax. Gson 2.10
 * (Minecraft 1.21.1) has no Strictness setting and JsonParser always parses leniently,
 * so the element adapter reads from a non-lenient reader instead.
 */
public final class Json {

    private static final Gson GSON = new Gson();

    private Json() {}

    public static JsonElement parseStrict(Reader reader) {
        try {
            JsonReader json = new JsonReader(reader);
            json.setLenient(false);
            JsonElement element = GSON.getAdapter(JsonElement.class).read(json);
            if (json.peek() != JsonToken.END_DOCUMENT) throw new JsonParseException("Trailing data after JSON document");
            return element;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
