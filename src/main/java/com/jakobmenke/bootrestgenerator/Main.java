package com.jakobmenke.bootrestgenerator;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Main {
    private void writeTemplates(ArrayList<Entity> entities) {
        Templates templates = new Templates();
        for (Entity entityName : entities) {
            String entityTemplate = templates.getEntityTemplate(entityName, EntityToRESTResource.PACKAGE);
            createFile("entity", entityName.getEntityName() + ".java", entityTemplate);

            String serviceTemplate = templates.getResourceTemplate(EntityToRESTResource.PACKAGE, entityName.getEntityName());
            createFile("rest", entityName.getEntityName() + "Resource.java", serviceTemplate);

            String daoTemplate = templates.getDaoTemplate(EntityToRESTResource.PACKAGE, entityName.getEntityName());
            createFile("dao", entityName.getEntityName() + "Dao.java", daoTemplate);

            String repositoryTemplate = templates.getRepositoryTemplate(EntityToRESTResource.PACKAGE, entityName.getEntityName());
            createFile("repository", entityName.getEntityName() + "Repository.java", repositoryTemplate);
        }

        String constantsTemplate = templates.getConstantsTemplate(EntityToRESTResource.PACKAGE, null);
        createFile("utils", "GlobalConstants.java", constantsTemplate);
    }

    private void createFile(String folderName, String fileName, String fileTemplate) {
        PrintWriter writer;

        try {

            String path = EntityToRESTResource.SRC_FOLDER + EntityToRESTResource.PACKAGE + "/" + folderName;
            File file = new File(path);

            if (!file.exists()) {
                boolean mkdirStatus = file.mkdir();
            }
            writer = new PrintWriter(path + "/" + fileName, "UTF-8");
            writer.print(fileTemplate);
            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        Configuration configuration = new Configuration(Configuration.readConfig("config.properties"));
        EntityToRESTResource.PACKAGE = configuration.getTargetPackage();
        EntityToRESTResource.SRC_FOLDER = configuration.getSrcFolder();
        EntityToRESTResource.FILE_NAME = configuration.getFileName();

        ArrayList<Entity> entities = new ArrayList<>();

        ArrayList<String> words = new ArrayList<>();

        getWords(words, Objects.requireNonNull(Main.class.getClassLoader().getResourceAsStream(EntityToRESTResource.FILE_NAME)));

        parseWords(entities, words);

        Main main = new Main();

        main.writeTemplates(entities);
    }

    private static void parseWords(ArrayList<Entity> entities, ArrayList<String> words) {
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            if (word.equalsIgnoreCase("create")) {
                String nextWord = words.get(i + 1);
                if (nextWord.equalsIgnoreCase("table")) {
                    String tableWord = words.get(i + 2);
                    List<String> comps = Arrays.stream(tableWord.replace(EntityToRESTResource.DB_ESCAPE_CHARACTER, "").split(EntityToRESTResource.UNDERSCORE)).collect(Collectors.toList());
                    String entityName = comps.stream().map(w -> w.substring(0, 1).toUpperCase() + w.substring(1).toLowerCase()).collect(Collectors.joining(""));
                    Entity entity = new Entity();
                    entity.setEntityName(entityName);
                    entity.setTableName(tableWord.replaceAll(EntityToRESTResource.DB_ESCAPE_CHARACTER, ""));
                    entities.add(entity);
                }
            }

            Pattern pattern = Pattern.compile(EntityToRESTResource.SUPPORTED_DATA_TYPES_REGEX);

            if (pattern.matcher(word).matches()) {
                String columnName = words.get(i - 1).replaceAll(EntityToRESTResource.DB_ESCAPE_CHARACTER, "");
                String datatype = word;
                String javaType = getJavaType(datatype);
                List<String> comps = Arrays.stream(columnName.replace(EntityToRESTResource.DB_ESCAPE_CHARACTER, "").split("_")).collect(Collectors.toList());
                String camelName = comps.stream().map(w -> w.substring(0, 1).toUpperCase() + w.substring(1).toLowerCase()).collect(Collectors.joining(""));
                camelName = camelName.substring(0, 1).toLowerCase() + camelName.substring(1);

                entities.get(entities.size() - 1).getColumns().add(new ColumnToField(columnName, camelName, datatype, javaType));
            }

            Pattern keyPattern = Pattern.compile(EntityToRESTResource.PRIMARY_FOREIGN_REGEX);
            if (keyPattern.matcher(word.toUpperCase()).matches()) {
                String keyString = words.subList(i, i + 10).stream().collect(Collectors.joining(" "));
                ColumnToField keyColumn = getId(keyString);
                Entity entity = entities.get(entities.size() - 1);
                List<ColumnToField> columns = entity.getColumns();
                for (int i1 = 0; i1 < columns.size(); i1++) {
                    ColumnToField column = columns.get(i1);
                    if (column.getDatabaseIdType() == null) {

                        if (column.getDatabaseColumnName() != null && column.getDatabaseColumnName().equalsIgnoreCase(keyColumn.getDatabaseColumnName())) {
                            //foreign key
                            keyColumn.setCamelCaseFieldName(column.getCamelCaseFieldName());
                            entity.getColumns().set(i1, keyColumn);
                        } else {
                            //primary key
                            if (column.getCamelCaseFieldName() != null && column.getCamelCaseFieldName().equalsIgnoreCase(keyColumn.getCamelCaseFieldName())) {
                                entity.getColumns().set(i1, keyColumn);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void getWords(ArrayList<String> words, InputStream in) {
        try (Scanner scanner = new Scanner(in)) {
            while (scanner.hasNext()) {

                String line = scanner.nextLine();

                //ignore commented lines
                if (!line.trim().startsWith("#") && !line.trim().startsWith("--")) {

                    words.addAll(Arrays.stream(line.split(" ")).filter(w -> w.length() > 0).collect(Collectors.toList()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getJavaType(String datatype) {

        Pattern pattern = Pattern.compile(EntityToRESTResource.VARCHAR_REGEX);
        if (pattern.matcher(datatype).matches()) {
            return "String";
        }
        pattern = Pattern.compile(EntityToRESTResource.INT_REGEX);
        if (pattern.matcher(datatype).matches()) {
            return "Integer";
        }
        pattern = Pattern.compile(EntityToRESTResource.BIGINT_REGEX);
        if (pattern.matcher(datatype).matches()) {
            return "Long";
        }
        pattern = Pattern.compile(EntityToRESTResource.DATETIME_REGEX);
        if (pattern.matcher(datatype).matches()) {
            return "LocalDate";
        }
        pattern = Pattern.compile(EntityToRESTResource.BIT_REGEX);
        if (pattern.matcher(datatype).matches()) {
            return "String";
        }
        pattern = Pattern.compile(EntityToRESTResource.FLOAT_REGEX);
        if (pattern.matcher(datatype).matches()) {
            return "Float";
        }
        pattern = Pattern.compile(EntityToRESTResource.DOUBLE_REGEX);
        if (pattern.matcher(datatype).matches()) {
            return "Double";
        }
        pattern = Pattern.compile(EntityToRESTResource.TIME_REGEX);
        if (pattern.matcher(datatype).matches()) {
            return "LocalTime";
        }
        pattern = Pattern.compile(EntityToRESTResource.TIMESTAMP_REGEX);
        if (pattern.matcher(datatype).matches()) {
            return "LocalDateTime";
        }

        //default to string
        return "String";
    }

    static String firstLetterToCaps(String string) {
        if (string.length() == 0)
            return "";
        return string.toUpperCase().substring(0, 1) + string.substring(1);
    }

    static String camelName(String string) {
        StringBuffer buffer = new StringBuffer(string.toLowerCase());
        for (int i = 0; i < buffer.length(); i++) {
            if (buffer.charAt(i) == '_') {
                buffer.replace(i, i + 2, buffer.substring(i + 1, i + 2).toUpperCase());
            }
        }
        return buffer.toString();
    }

    static ColumnToField getId(String key) {
        String camelName = null;
        String dbName = null;
        String dataType = null;
        String javaType = null;
        String idType;
        Pattern pattern = Pattern.compile(EntityToRESTResource.FOREIGN_KEY_REFERENCES_REGEX);
        Matcher matcher = pattern.matcher(key.toUpperCase());
        if (matcher.find()) {
            String foreignKey = matcher.group(1);
            String otherTableName = matcher.group(2);
            String primaryKeyOtherTable = matcher.group(3);
            javaType = firstLetterToCaps(camelName(otherTableName.replaceAll(EntityToRESTResource.DB_ESCAPE_CHARACTER, "")));
            camelName = camelName(foreignKey.replaceAll(EntityToRESTResource.DB_ESCAPE_CHARACTER, ""));
            dbName = primaryKeyOtherTable.replaceAll(EntityToRESTResource.DB_ESCAPE_CHARACTER, "");
        }
        pattern = Pattern.compile(EntityToRESTResource.PRIMARY_KEY_S_S);
        matcher = pattern.matcher(key.toUpperCase());
        if (matcher.matches()) {
            idType = "@Id";
            dbName = matcher.group(1).replaceAll(EntityToRESTResource.DB_ESCAPE_CHARACTER, "");
            javaType = "Integer";
        } else
            idType = "@ManyToOne";
        return new ColumnToField(idType, dbName, camelName, dataType, javaType);
    }
}
