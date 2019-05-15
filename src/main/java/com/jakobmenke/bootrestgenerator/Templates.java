package com.jakobmenke.bootrestgenerator;

import java.io.InputStream;
import java.util.Objects;
import java.util.Scanner;

public class Templates {
    public String getServiceTemplate(String mainPackage, String entityName) {
        String mainPackageName = mainPackage.replaceAll("/", ".");
        String fileTemplate = getFile("templates/rest.resource.tmpl");
        fileTemplate = fileTemplate.replace("{{mainPackageName}}", mainPackageName);
        fileTemplate = fileTemplate.replace("{{entityName}}", entityName);
        fileTemplate = fileTemplate.replace("{{serviceName}}", entityName.toLowerCase());
        fileTemplate = fileTemplate.replace("{{restServicePrefix}}", "/rest");

        return fileTemplate;
    }

    public String getDaoTemplate(String mainPackage, String entityName) {
        String mainPackageName = mainPackage.replaceAll("/", ".");
        String fileTemplate = getFile("templates/dao.tmpl");
        fileTemplate = fileTemplate.replace("{{mainPackageName}}", mainPackageName);
        fileTemplate = fileTemplate.replace("{{entityName}}", entityName);

        return fileTemplate;
    }

    public String getRepositoryTemplate(String mainPackage, String entityName) {
        String mainPackageName = mainPackage.replaceAll("/", ".");
        String fileTemplate = getFile("templates/repository.tmpl");
        fileTemplate = fileTemplate.replace("{{mainPackageName}}", mainPackageName);
        fileTemplate = fileTemplate.replace("{{entityName}}", entityName);

        return fileTemplate;
    }

    private String getFile(String fileName) {
        StringBuilder result = new StringBuilder();
        ClassLoader classLoader = Templates.class.getClassLoader();
        InputStream in = classLoader.getResourceAsStream(fileName);

        try (Scanner scanner = new Scanner(Objects.requireNonNull(in))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                result.append(line).append("\n");
            }
        }

        return result.toString();
    }

    public String getEntityTemplate(Table entity, String mainPackage) {
        String mainPackageName = mainPackage.replaceAll("/", ".");
        String fileTemplate = getFile("templates/entity.tmpl");
        fileTemplate = fileTemplate.replace("{{mainPackageName}}", mainPackageName);
        fileTemplate = fileTemplate.replace("{{entityName}}", entity.getEntityName());
        fileTemplate = fileTemplate.replace("{{tableName}}", entity.getTableName());

        StringBuilder stringBuilder = new StringBuilder();
        for (Column column : entity.getColumns()) {
            String indent = "    ";
            stringBuilder.append(indent).append("@Column(name = \"").append(column.getDbName()).append("\")\n");
            stringBuilder.append(indent).append(column.getJavaType()).append(" ").append(column.getCamelName()).append(";\n\n");
        }

        stringBuilder.append("}");

        return fileTemplate + stringBuilder.toString();
    }
}
