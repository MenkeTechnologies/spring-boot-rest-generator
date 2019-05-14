package com.jakobmenke.bootrestgenerator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class App {
    private final static String SRC_FOLDER = "/Users/wizard/IdeaProjects/Tweedle/src/main/java/";
    private final static String PACKAGE = "com/jakobmenke/boot";
    private static final String FILE_NAME = "dump.sql";

    private void writeTemplates(ArrayList<Table> tables) {
        Templates templates = new Templates();
        for (Table entityName : tables) {
            String entityTemplate = templates.getEntityTemplate(entityName, PACKAGE);
            createFile("entity", entityName.getEntityName() + ".java", entityTemplate);

            String serviceTemplate = templates.getServiceTemplate(PACKAGE, entityName.getEntityName());
            createFile("rest", entityName.getEntityName() + "Resource.java", serviceTemplate);

            String daoTemplate = templates.getDaoTemplate(PACKAGE, entityName.getEntityName());
            createFile("dao", entityName.getEntityName() + "Dao.java", daoTemplate);

            String repositoryTemplate = templates.getRepositoryTemplate(PACKAGE, entityName.getEntityName());
            createFile("repository", entityName.getEntityName() + "Repository.java", repositoryTemplate);
        }
    }

    private void createFile(String folderName, String fileName, String fileTemplate) {
        PrintWriter writer;

        try {

            String path = SRC_FOLDER + PACKAGE + "/" + folderName;
            File file = new File(path);

            if (!file.exists()) {
                boolean mkdir = file.mkdir();
            }
            writer = new PrintWriter(path + "/" + fileName, "UTF-8");
            writer.print(fileTemplate);
            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        ArrayList<Table> tables = new ArrayList<>();

        ArrayList<String> words = new ArrayList<>();

        getWords(words);

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
                    List<String> comps = Arrays.stream(tableWord.replace("`", "").split("_")).collect(Collectors.toList());
                    String entityName = comps.stream().map(w -> w.substring(0, 1).toUpperCase() + w.substring(1).toLowerCase()).collect(Collectors.joining(""));
                    Table table = new Table();
                    table.setEntityName(entityName);
                    table.setTableName(tableWord.replaceAll("`", ""));
                    tables.add(table);
                }
            }

            Pattern pattern = Pattern.compile("\\b(varchar|tinyint|bigint|int|datetime|timestamp|bit)[()\\d]*");

            if (pattern.matcher(word).matches()) {
                String columnName = words.get(i - 1).replaceAll("`", "");
                String datatype = word;
                String javaType = getJavaType(datatype);
                List<String> comps = Arrays.stream(columnName.replace("`", "").split("_")).collect(Collectors.toList());
                String camelName = comps.stream().map(w -> w.substring(0, 1).toUpperCase() + w.substring(1).toLowerCase()).collect(Collectors.joining(""));
                camelName = camelName.substring(0, 1).toLowerCase() + camelName.substring(1);

                tables.get(tables.size() - 1).getColumns().add(new Column(columnName, camelName, datatype, javaType));
            }
        }
    }

    private static void getWords(ArrayList<String> words) {
        try (Scanner scanner = new Scanner(Objects.requireNonNull(App.class.getClassLoader().getResourceAsStream(FILE_NAME)))) {
            while (scanner.hasNext()) {

                String word = scanner.next();

                words.add(word);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getJavaType(String datatype) {

        Pattern pattern = Pattern.compile("\\bvarchar[()\\d]*");
        if (pattern.matcher(datatype).matches()) {
            return "String";
        }
        pattern = Pattern.compile("\\b(tinyint|int)[()\\d]*");
        if (pattern.matcher(datatype).matches()) {
            return "Integer";
        }
        pattern = Pattern.compile("\\bbigint[()\\d]*");
        if (pattern.matcher(datatype).matches()) {
            return "Long";
        }
        pattern = Pattern.compile("\\bdatetime[()\\d]*");
        if (pattern.matcher(datatype).matches()) {
            return "LocalDate";
        }
        pattern = Pattern.compile("\\bbit[()\\d]*");
        if (pattern.matcher(datatype).matches()) {
            return "String";
        }
        pattern = Pattern.compile("\\btimestamp[()\\d]*");
        if (pattern.matcher(datatype).matches()) {
            return "LocalDateTime";
        }

        //default to string
        return "String";
    }
}
