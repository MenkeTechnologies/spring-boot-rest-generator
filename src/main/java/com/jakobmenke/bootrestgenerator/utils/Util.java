package com.jakobmenke.bootrestgenerator.utils;

import com.jakobmenke.bootrestgenerator.dto.ColumnToField;
import com.jakobmenke.bootrestgenerator.dto.Entity;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Util {
    public static void parseWords(ArrayList<Entity> entities, ArrayList<String> words) {
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            if (word.equalsIgnoreCase("create")) {
                String nextWord = words.get(i + 1);
                if (nextWord.equalsIgnoreCase("table")) {
                    String tableWord = words.get(i + 2);
                    List<String> comps = Arrays.stream(tableWord.replace(EntityToRESTConstants.DB_ESCAPE_CHARACTER, "").split(EntityToRESTConstants.UNDERSCORE)).collect(Collectors.toList());
                    String entityName = comps.stream().map(w -> w.substring(0, 1).toUpperCase() + w.substring(1).toLowerCase()).collect(Collectors.joining(""));
                    Entity entity = new Entity();
                    entity.setEntityName(entityName);
                    entity.setTableName(tableWord.replaceAll(EntityToRESTConstants.DB_ESCAPE_CHARACTER, ""));
                    entities.add(entity);
                }
            }

            Pattern pattern = Pattern.compile(EntityToRESTConstants.SUPPORTED_DATA_TYPES_REGEX);

            if (pattern.matcher(word).matches()) {
                String columnName = words.get(i - 1).replaceAll(EntityToRESTConstants.DB_ESCAPE_CHARACTER, "");
                String datatype = word;
                String javaType = getJavaType(datatype);
                List<String> comps = Arrays.stream(columnName.replace(EntityToRESTConstants.DB_ESCAPE_CHARACTER, "").split("_")).collect(Collectors.toList());
                String camelName = comps.stream().map(w -> w.substring(0, 1).toUpperCase() + w.substring(1).toLowerCase()).collect(Collectors.joining(""));
                camelName = camelName.substring(0, 1).toLowerCase() + camelName.substring(1);

                entities.get(entities.size() - 1).getColumns().add(new ColumnToField(columnName, camelName, datatype, javaType));
            }

            setPKorFKColumns(entities, words, i, word);
        }
    }

    private static void setPKorFKColumns(ArrayList<Entity> entities, ArrayList<String> words, int i, String word) {
        Pattern keyPattern = Pattern.compile(EntityToRESTConstants.PRIMARY_FOREIGN_REGEX);
        if (keyPattern.matcher(word.toUpperCase()).matches()) {
            String keyString = words.subList(i, i + 10).stream().collect(Collectors.joining(" "));
            ColumnToField keyColumn = getId(keyString);
            Entity entity = entities.get(entities.size() - 1);
            List<ColumnToField> columns = entity.getColumns();
            for (int j = 0; j < columns.size(); j++) {
                ColumnToField column = columns.get(j);
                if (column.getDatabaseIdType() == null) {

                    if (Objects.equals(column.getDatabaseColumnName(), keyColumn.getDatabaseColumnName())) {
                        //primary or foreign key
                        keyColumn.setCamelCaseFieldName(column.getCamelCaseFieldName().replaceFirst("[iI]d$", ""));
                        entity.getColumns().set(j, keyColumn);
                    }
                }
            }
        }
    }

    public static void getWords(ArrayList<String> words, InputStream in) {
        try (Scanner scanner = new Scanner(in)) {
            while (scanner.hasNext()) {

                String line = scanner.nextLine();

                //ignore commented lines
                if (!line.trim().startsWith("#") && !line.trim().startsWith("--")) {

                    words.addAll(Arrays.stream(line.split(EntityToRESTConstants.SPACE_CHAR)).filter(w -> w.length() > 0).collect(Collectors.toList()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getJavaType(String datatype) {

        Pattern pattern = Pattern.compile(EntityToRESTConstants.VARCHAR_REGEX);
        if (pattern.matcher(datatype).matches()) {
            return "String";
        }
        pattern = Pattern.compile(EntityToRESTConstants.INT_REGEX);
        if (pattern.matcher(datatype).matches()) {
            return "Integer";
        }
        pattern = Pattern.compile(EntityToRESTConstants.BIGINT_REGEX);
        if (pattern.matcher(datatype).matches()) {
            return "Long";
        }
        pattern = Pattern.compile(EntityToRESTConstants.DATETIME_REGEX);
        if (pattern.matcher(datatype).matches()) {
            return "LocalDate";
        }
        pattern = Pattern.compile(EntityToRESTConstants.BIT_REGEX);
        if (pattern.matcher(datatype).matches()) {
            return "String";
        }
        pattern = Pattern.compile(EntityToRESTConstants.FLOAT_REGEX);
        if (pattern.matcher(datatype).matches()) {
            return "Float";
        }
        pattern = Pattern.compile(EntityToRESTConstants.DOUBLE_REGEX);
        if (pattern.matcher(datatype).matches()) {
            return "Double";
        }
        pattern = Pattern.compile(EntityToRESTConstants.TIME_REGEX);
        if (pattern.matcher(datatype).matches()) {
            return "LocalTime";
        }
        pattern = Pattern.compile(EntityToRESTConstants.TIMESTAMP_REGEX);
        if (pattern.matcher(datatype).matches()) {
            return "LocalDateTime";
        }

        //default to string
        return "String";
    }

    public static String firstLetterToCaps(String string) {
        if (string.length() == 0)
            return "";
        return string.toUpperCase().charAt(0) + string.substring(1);
    }

    public static String camelName(String string) {
        StringBuffer buffer = new StringBuffer(string.toLowerCase());
        for (int i = 0; i < buffer.length(); i++) {
            if (buffer.charAt(i) == '_') {
                buffer.replace(i, i + 2, buffer.substring(i + 1, i + 2).toUpperCase());
            }
        }
        return buffer.toString();
    }

    public static ColumnToField getId(String key) {
        String camelName = null;
        String dbName = null;
        String dataType = null;
        String javaType = null;
        String idType;
        Pattern pattern = Pattern.compile(EntityToRESTConstants.FOREIGN_KEY_REFERENCES_REGEX);
        Matcher matcher = pattern.matcher(key);
        if (matcher.find()) {
            String foreignKey = matcher.group(1);
            String otherTableName = matcher.group(2);
            String primaryKeyOtherTable = matcher.group(3);
            javaType = firstLetterToCaps(camelName(otherTableName.replaceAll(EntityToRESTConstants.DB_ESCAPE_CHARACTER, "")));
            camelName = camelName(foreignKey.replaceAll(EntityToRESTConstants.DB_ESCAPE_CHARACTER, ""));
            dbName = primaryKeyOtherTable.replaceAll(EntityToRESTConstants.DB_ESCAPE_CHARACTER, "");
        }
        pattern = Pattern.compile(EntityToRESTConstants.PRIMARY_KEY_S_S);
        matcher = pattern.matcher(key);
        if (matcher.matches()) {
            idType = EntityToRESTConstants.PK_ID;
            dbName = matcher.group(1).replaceAll(EntityToRESTConstants.DB_ESCAPE_CHARACTER, "");
            javaType = EntityToRESTConstants.PK_DATA_TYPE;
        } else {
            idType = EntityToRESTConstants.FK_ID;
            javaType = EntityToRESTConstants.FK_DATA_TYPE;
        }
        return new ColumnToField(idType, dbName, camelName, dataType, javaType);
    }
}
