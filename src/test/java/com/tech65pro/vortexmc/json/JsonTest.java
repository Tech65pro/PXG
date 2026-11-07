package com.tech65pro.vortexmc.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The JSON layer VortexMC ships instead of pulling in a dependency. */
class JsonTest {

    @Test
    @DisplayName("an object round-trips through text")
    void objectRoundTrips() {
        JsonObject original = JsonValue.newObject();
        original.put("name", "vortexmc");
        original.put("version", 1);
        original.put("ratio", 0.5);
        original.put("enabled", true);
        original.put("nothing", JsonNull.INSTANCE);

        JsonValue parsed = Json.parse(Json.write(original, true));
        Assertions.assertTrue(parsed.isObject());
        JsonObject object = parsed.asObject();
        Assertions.assertEquals("vortexmc", object.getOrNull("name").asString(null));
        Assertions.assertEquals(1, object.getOrNull("version").asInt(0));
        Assertions.assertEquals(0.5, object.getOrNull("ratio").asDouble(0d), 0.0001d);
        Assertions.assertTrue(object.getOrNull("enabled").asBoolean(false));
        Assertions.assertTrue(object.getOrNull("nothing").isNull());
    }

    @Test
    @DisplayName("arrays round-trip and keep their order")
    void arrayRoundTrips() {
        JsonArray original = JsonValue.newArray();
        original.add("a");
        original.add(2);
        original.add(false);
        JsonArray parsed = Json.parse(Json.write(original, false)).asArray();
        Assertions.assertEquals(3, parsed.size());
        Assertions.assertEquals("a", parsed.get(0).asString(null));
        Assertions.assertEquals(2, parsed.get(1).asInt(0));
        Assertions.assertFalse(parsed.get(2).asBoolean(true));
    }

    @Test
    @DisplayName("a broken document raises a parse exception instead of returning nonsense")
    void brokenDocumentRaises() {
        Assertions.assertThrows(JsonParseException.class, () -> Json.parse("{oops"));
        Assertions.assertThrows(JsonParseException.class, () -> Json.parse(""));
    }

    @Test
    @DisplayName("unknown keys are ignored rather than fatal")
    void unknownKeysAreIgnored() {
        JsonObject parsed = Json.parse("{\"known\":1,\"unknown\":\"whatever\"}").asObject();
        Assertions.assertEquals(1, parsed.getOrNull("known").asInt(0));
        Assertions.assertTrue(parsed.has("unknown"));
    }

    @Test
    @DisplayName("a missing key yields null rather than an error")
    void missingKeyIsNull() {
        JsonObject parsed = Json.parse("{\"n\":5}").asObject();
        Assertions.assertNull(parsed.getOrNull("missing").asString(null));
        Assertions.assertFalse(parsed.has("missing"));
        Assertions.assertEquals(7, parsed.getOrNull("missing").asInt(7),
                "a missing key falls back instead of throwing");
    }

    @Test
    @DisplayName("the fallback accessors never throw, even on a type mismatch")
    void fallbackAccessorsNeverThrow() {
        JsonObject parsed = Json.parse("{\"n\":5,\"s\":\"x\"}").asObject();
        Assertions.assertFalse(parsed.getOrNull("n").asBoolean(false),
                "a number is not a boolean, so the fallback is used rather than a guess");
        Assertions.assertEquals(0, parsed.getOrNull("s").asInt(0));
        Assertions.assertEquals("", parsed.getOrNull("n").asString(""));
        Assertions.assertTrue(parsed.getOrNull("n").asObjectOrEmpty().entries().isEmpty());
        Assertions.assertTrue(parsed.getOrNull("n").asArrayOrEmpty().items().isEmpty());
        Assertions.assertTrue(parsed.getOrNull("missing").asObjectOrEmpty().entries().isEmpty(),
                "a missing section degrades to an empty object");
        Assertions.assertTrue(parsed.getOrNull("missing").asArrayOrEmpty().items().isEmpty());
    }

    @Test
    @DisplayName("the strict accessors do report a type mismatch")
    void strictAccessorsReportMismatch() {
        JsonObject parsed = Json.parse("{\"n\":5,\"s\":\"x\"}").asObject();
        Assertions.assertThrows(JsonTypeException.class, () -> parsed.getOrNull("n").asObject());
        Assertions.assertThrows(JsonTypeException.class, () -> parsed.getOrNull("n").asArray());
        Assertions.assertThrows(JsonTypeException.class, () -> parsed.getOrNull("s").asLong());
        Assertions.assertTrue(parsed.getOrNull("n").asBoolean(),
                "a number is deliberately truthy when non-zero, so asBoolean does not throw");
    }

    @Test
    @DisplayName("string escapes survive a round trip")
    void escapesSurvive() {
        JsonObject original = JsonValue.newObject();
        original.put("text", "line\nbreak \"quoted\" back\\slash tab\there");
        String written = Json.write(original, false);
        JsonObject parsed = Json.parse(written).asObject();
        Assertions.assertEquals("line\nbreak \"quoted\" back\\slash tab\there",
                parsed.getOrNull("text").asString(null));
    }
}
