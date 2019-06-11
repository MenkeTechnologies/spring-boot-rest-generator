package com.jakobmenke.bootrestgenerator;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class App {
    private static String PACKAGE;
    private static String SRC_FOLDER;
    private static String FILE_NAME;

    private void writeTemplates(ArrayList<Table> tables) {
        Templates templates = new Templates();
        for (Table entityName : tables) {
            String entityTemplate = templates.getEntityTemplate(entityName, PACKAGE);
            createFile("entity", entityName.getEntityName() + ".java", entityTemplate);

            String serviceTemplate = templates.getResourceTemplate(PACKAGE, entityName.getEntityName());
            createFile("rest", entityName.getEntityName() + "Resource.java", serviceTemplate);

            String daoTemplate = templates.getDaoTemplate(PACKAGE, entityName.getEntityName());
            createFile("dao", entityName.getEntityName() + "Dao.java", daoTemplate);

            String repositoryTemplate = templates.getRepositoryTemplate(PACKAGE, entityName.getEntityName());
            createFile("repository", entityName.getEntityName() + "Repository.java", repositoryTemplate);
        }

        String constantsTemplate = templates.getConstantsTemplate(PACKAGE, null);
        createFile("utils", "GlobalConstants.java", constantsTemplate);
    }

    private void createFile(String folderName, String fileName, String fileTemplate) {
        PrintWriter writer;

        try {

            String path = SRC_FOLDER + PACKAGE + "/" + folderName;
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
        PACKAGE = configuration.getTargetPackage();
        SRC_FOLDER = configuration.getSrcFolder();
        FILE_NAME = configuration.getFileName();

        ArrayList<Table> tables = new ArrayList<>();

        ArrayList<String> words = new ArrayList<>();

        getWords(words, Objects.requireNonNull(App.class.getClassLoader().getResourceAsStream(FILE_NAME)));

        parseWords(tables, words);

        App app = new App();

        app.writeTemplates(tables);
    }

    private static void parseWords(ArrayList<Table> tables, ArrayList<String> words) {
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            if (word.equalsIgnoreCase("create")) {
                String nextWord = words.get(i + 1);
                if (nextWord.equalsIgnoreCase("table")) {
                    String tableWord = words.get(i + 2);
                    List<String> comps = Arrays.stream(tableWord.replace(Constants.DB_ESCAPE_CHARACTER, "").split("_")).collect(Collectors.toList());
                    String entityName = comps.stream().map(w -> w.substring(0, 1).toUpperCase() + w.substring(1).toLowerCase()).collect(Collectors.joining(""));
                    Table table = new Table();
                    table.setEntityName(entityName);
                    table.setTableName(tableWord.replaceAll(Constants.DB_ESCAPE_CHARACTER, ""));
                    tables.add(table);
                }
            }

            Pattern pattern = Pattern.compile(Constants.SUPPORTED_DATA_TYPES_REGEX);

            if (pattern.matcher(word).matches()) {
                String columnName = words.get(i - 1).replaceAll(Constants.DB_ESCAPE_CHARACTER, "");
                String datatype = word;
                String javaType = getJavaType(datatype);
                List<String> comps = Arrays.stream(columnName.replace(Constants.DB_ESCAPE_CHARACTER, "").split("_")).collect(Collectors.toList());
                String camelName = comps.stream().map(w -> w.substring(0, 1).toUpperCase() + w.substring(1).toLowerCase()).collect(Collectors.joining(""));
                camelName = camelName.substring(0, 1).toLowerCase() + camelName.substring(1);

                tables.get(tables.size() - 1).getColumns().add(new Column(columnName, camelName, datatype, javaType));
            }

            Pattern keyPattern = Pattern.compile(Constants.PRIMARY_FOREIGN_REGEX);
            if (keyPattern.matcher(word.toUpperCase()).matches()) {
                String keyString = words.subList(i, i + 10).stream().collect(Collectors.joining(" "));
                Column keyColumn = getId(keyString);
                Table table = tables.get(tables.size() - 1);
                List<Column> columns = table.getColumns();
                for (int i1 = 0; i1 < columns.size(); i1++) {
                    Column column = columns.get(i1);
                    if (column.getIdType() == null) {

                        if (column.getDbName() != null && column.getDbName().equalsIgnoreCase(keyColumn.getDbName())) {
                            //foreign key
                            keyColumn.setCamelName(column.getCamelName());
                            table.getColumns().set(i1, keyColumn);
                        } else {
                            //primar key
                            if (column.getCamelName() != null && column.getCamelName().equalsIgnoreCase(keyColumn.getCamelName())) {
                                table.getColumns().set(i1, keyColumn);
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

        Pattern pattern = Pattern.compile(Constants.VARCHAR_REGEX);
        if (pattern.matcher(datatype).matches()) {
            return "String";
        }
        pattern = Pattern.compile(Constants.INT_REGEX);
        if (pattern.matcher(datatype).matches()) {
            return "Integer";
        }
        pattern = Pattern.compile(Constants.BIGINT_REGEX);
        if (pattern.matcher(datatype).matches()) {
            return "Long";
        }
        pattern = Pattern.compile(Constants.DATETIME_REGEX);
        if (pattern.matcher(datatype).matches()) {
            return "LocalDate";
        }
        pattern = Pattern.compile(Constants.BIT_REGEX);
        if (pattern.matcher(datatype).matches()) {
            return "String";
        }
        pattern = Pattern.compile(Constants.FLOAT_REGEX);
        if (pattern.matcher(datatype).matches()) {
            return "Float";
        }
        pattern = Pattern.compile(Constants.DOUBLE_REGEX);
        if (pattern.matcher(datatype).matches()) {
            return "Double";
        }
        pattern = Pattern.compile(Constants.TIME_REGEX);
        if (pattern.matcher(datatype).matches()) {
            return "LocalTime";
        }
        pattern = Pattern.compile(Constants.TIMESTAMP_REGEX);
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

    static Column getId(String key) {
        String camelName = null;
        String dbName = null;
        String dataType = null;
        String javaType = null;
        String idType;
        Pattern pattern = Pattern.compile(Constants.FOREIGN_KEY_REFERENCES_REGEX);
        Matcher matcher = pattern.matcher(key.toUpperCase());
        if (matcher.find()) {
            String foreignKey = matcher.group(1);
            String otherTableName = matcher.group(2);
            String primaryKeyOtherTable = matcher.group(3);
            javaType = firstLetterToCaps(camelName(otherTableName.replaceAll(Constants.DB_ESCAPE_CHARACTER, "")));
            camelName = camelName(foreignKey.replaceAll(Constants.DB_ESCAPE_CHARACTER, ""));
            dbName = primaryKeyOtherTable.replaceAll(Constants.DB_ESCAPE_CHARACTER, "");
        }
        pattern = Pattern.compile(Constants.PRIMARY_KEY_S_S);
        matcher = pattern.matcher(key.toUpperCase());
        if (matcher.matches()) {
            idType = "@Id";
            dbName = matcher.group(1).replaceAll(Constants.DB_ESCAPE_CHARACTER, "");
            javaType = "Integer";
        } else
            idType = "@ManyToOne";
        return new Column(idType, dbName, camelName, dataType, javaType);
    }
}
