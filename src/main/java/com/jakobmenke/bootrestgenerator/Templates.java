package com.jakobmenke.bootrestgenerator;

import java.io.InputStream;
import java.util.Objects;
import java.util.Scanner;

public class Templates {
    public String getResourceTemplate(String mainPackage, String entityName) {
        String mainPackageName = mainPackage.replaceAll("/", ".");
        String fileTemplate = getFile("templates/rest.resource.tmpl");
        fileTemplate = fileTemplate.replace("{{mainPackageName}}", mainPackageName);
        fileTemplate = fileTemplate.replace("{{entityName}}", entityName);
        fileTemplate = fileTemplate.replace("{{serviceName}}", entityName.toLowerCase());
        fileTemplate = fileTemplate.replace("{{restServicePrefix}}", "/api");

        return fileTemplate;
    }

    public String getDaoTemplate(String mainPackage, String entityName) {
        String mainPackageName = mainPackage.replaceAll("/", ".");
        String fileTemplate = getFile("templates/dao.tmpl");
        fileTemplate = fileTemplate.replace("{{mainPackageName}}", mainPackageName);
        fileTemplate = fileTemplate.replace("{{entityName}}", entityName);
        String camelRepoName = entityName.substring(0, 1).toLowerCase() + entityName.substring(1) + "Repository";
        fileTemplate = fileTemplate.replace("{{camelRepositoryName}}", camelRepoName);

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
            String indentation = "    ";
            if (column.getIdType() != null) {
                stringBuilder.append(indentation).append(column.getIdType()).append("\n");
            }
            if (column.getIdType() != null && column.getIdType().equalsIgnoreCase("@ManyToOne")) {
                stringBuilder.append(indentation).append("@JoinColumn(name = \"").append(column.getDbName()).append("\")\n");
            } else {
                stringBuilder.append(indentation).append("@Column(name = \"").append(column.getDbName()).append("\")\n");
            }
            String accessModifier = "private";
            stringBuilder.append(indentation).append(accessModifier).append(" ").append(column.getJavaType()).append(" ").append(column.getCamelName()).append(";\n\n");
        }

        stringBuilder.append("}");

        return fileTemplate + stringBuilder.toString();
    }
}
