package edu.course.brigdelt.service;

import edu.course.brigdelt.domain.Country;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads country metadata from the classpath configuration.
 */
public class CountryConfigService {

    private static final String COUNTRY_CONFIG_RESOURCE = "/config/countries.json";

    public List<Country> loadCountries() {
        String json = readResource();
        String countriesArray = extractArray(json, "countries");
        List<String> countryObjects = extractObjects(countriesArray);
        List<Country> countries = new ArrayList<>();
        for (String countryObject : countryObjects) {
            countries.add(parseCountry(countryObject));
        }
        if (countries.isEmpty()) {
            throw new IllegalStateException("国家配置为空：" + COUNTRY_CONFIG_RESOURCE);
        }
        return List.copyOf(countries);
    }

    private String readResource() {
        try (InputStream inputStream = CountryConfigService.class.getResourceAsStream(COUNTRY_CONFIG_RESOURCE)) {
            if (inputStream == null) {
                throw new IllegalStateException("缺少国家配置资源：" + COUNTRY_CONFIG_RESOURCE);
            }
            try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                StringBuilder builder = new StringBuilder();
                char[] buffer = new char[4096];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    builder.append(buffer, 0, read);
                }
                return builder.toString();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("国家配置读取失败：" + COUNTRY_CONFIG_RESOURCE, exception);
        }
    }

    private String extractArray(String json, String fieldName) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\\[").matcher(json);
        if (!matcher.find()) {
            throw new IllegalStateException("国家配置缺少字段：" + fieldName);
        }
        int start = matcher.end();
        int depth = 1;
        boolean inString = false;
        boolean escaped = false;
        for (int index = start; index < json.length(); index++) {
            char current = json.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (current == '[') {
                depth++;
            } else if (current == ']') {
                depth--;
                if (depth == 0) {
                    return json.substring(start, index);
                }
            }
        }
        throw new IllegalStateException("国家配置数组未正确闭合：" + fieldName);
    }

    private List<String> extractObjects(String arrayJson) {
        List<String> objects = new ArrayList<>();
        int depth = 0;
        int objectStart = -1;
        boolean inString = false;
        boolean escaped = false;
        for (int index = 0; index < arrayJson.length(); index++) {
            char current = arrayJson.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (current == '{') {
                if (depth == 0) {
                    objectStart = index;
                }
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0 && objectStart >= 0) {
                    objects.add(arrayJson.substring(objectStart, index + 1));
                    objectStart = -1;
                }
            }
        }
        return objects;
    }

    private Country parseCountry(String countryObject) {
        return new Country(
                requiredString(countryObject, "cameoCode"),
                optionalString(countryObject, "isoCode"),
                requiredString(countryObject, "nameCn"),
                requiredString(countryObject, "nameEn"),
                requiredString(countryObject, "region"),
                optionalDouble(countryObject, "latitude"),
                optionalDouble(countryObject, "longitude"),
                true,
                optionalBoolean(countryObject, "isCoreCountry", false)
        );
    }

    private String requiredString(String jsonObject, String fieldName) {
        String value = optionalString(jsonObject, fieldName);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("国家配置缺少必填字段：" + fieldName);
        }
        return value;
    }

    private String optionalString(String jsonObject, String fieldName) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(fieldName)
                + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(jsonObject);
        return matcher.find() ? unescapeJsonString(matcher.group(1)) : null;
    }

    private Double optionalDouble(String jsonObject, String fieldName) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(fieldName)
                + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)").matcher(jsonObject);
        return matcher.find() ? Double.parseDouble(matcher.group(1)) : null;
    }

    private boolean optionalBoolean(String jsonObject, String fieldName, boolean defaultValue) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(fieldName)
                + "\"\\s*:\\s*(true|false)").matcher(jsonObject);
        return matcher.find() ? Boolean.parseBoolean(matcher.group(1)) : defaultValue;
    }

    private String unescapeJsonString(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current != '\\' || index == value.length() - 1) {
                builder.append(current);
                continue;
            }
            char escaped = value.charAt(++index);
            builder.append(switch (escaped) {
                case '"', '\\', '/' -> escaped;
                case 'b' -> '\b';
                case 'f' -> '\f';
                case 'n' -> '\n';
                case 'r' -> '\r';
                case 't' -> '\t';
                case 'u' -> readUnicodeEscape(value, index);
                default -> escaped;
            });
            if (escaped == 'u') {
                index += 4;
            }
        }
        return builder.toString();
    }

    private char readUnicodeEscape(String value, int escapeIndex) {
        int hexStart = escapeIndex + 1;
        int hexEnd = hexStart + 4;
        if (hexEnd > value.length()) {
            throw new IllegalStateException("国家配置包含无效 Unicode 转义。");
        }
        return (char) Integer.parseInt(value.substring(hexStart, hexEnd), 16);
    }
}
