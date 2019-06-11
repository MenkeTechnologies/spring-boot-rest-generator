package com.jakobmenke.bootrestgenerator;

import java.io.*;
import java.util.*;

public class Main {
    private void writeTemplates(ArrayList<Entity> entities) {
        Templates templates = new Templates();
        for (Entity entityName : entities) {
            String entityTemplate = templates.getEntityTemplate(entityName, EntityToRESTConstants.PACKAGE);
            createFile("entity", entityName.getEntityName() + ".java", entityTemplate);

            String serviceTemplate = templates.getResourceTemplate(EntityToRESTConstants.PACKAGE, entityName.getEntityName());
            createFile("rest", entityName.getEntityName() + "Resource.java", serviceTemplate);

            String daoTemplate = templates.getDaoTemplate(EntityToRESTConstants.PACKAGE, entityName.getEntityName());
            createFile("dao", entityName.getEntityName() + "Dao.java", daoTemplate);

            String repositoryTemplate = templates.getRepositoryTemplate(EntityToRESTConstants.PACKAGE, entityName.getEntityName());
            createFile("repository", entityName.getEntityName() + "Repository.java", repositoryTemplate);
        }

        String constantsTemplate = templates.getConstantsTemplate(EntityToRESTConstants.PACKAGE, null);
        createFile("utils", "GlobalConstants.java", constantsTemplate);
    }

    private void createFile(String folderName, String fileName, String fileTemplate) {
        PrintWriter writer;

        try {

            String path = EntityToRESTConstants.SRC_FOLDER + EntityToRESTConstants.PACKAGE + "/" + folderName;
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
        EntityToRESTConstants.PACKAGE = configuration.getTargetPackage();
        EntityToRESTConstants.SRC_FOLDER = configuration.getSrcFolder();
        EntityToRESTConstants.FILE_NAME = configuration.getFileName();

        ArrayList<Entity> entities = new ArrayList<>();

        ArrayList<String> words = new ArrayList<>();

        Util.getWords(words, Objects.requireNonNull(Main.class.getClassLoader().getResourceAsStream(EntityToRESTConstants.FILE_NAME)));

        Util.parseWords(entities, words);

        Main main = new Main();

        main.writeTemplates(entities);
    }
}
