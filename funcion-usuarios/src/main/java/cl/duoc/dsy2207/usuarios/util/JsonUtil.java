package cl.duoc.dsy2207.usuarios.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public final class JsonUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private JsonUtil() {
    }

    public static String toJson(Object valor) {
        try {
            return MAPPER.writeValueAsString(valor);
        } catch (Exception e) {
            throw new RuntimeException("Error al serializar a JSON", e);
        }
    }

    public static Map<String, Object> aMapa(Object valor) {
        return MAPPER.convertValue(valor, new TypeReference<Map<String, Object>>() {
        });
    }

    public static <T> T fromJson(String json, Class<T> tipo) {
        try {
            return MAPPER.readValue(json, tipo);
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON inválido: " + e.getMessage(), e);
        }
    }
}
