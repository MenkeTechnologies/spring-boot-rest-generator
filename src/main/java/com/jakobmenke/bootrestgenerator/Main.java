package com.jakobmenke.bootrestgenerator;

import com.jakobmenke.bootrestgenerator.dto.Entity;
import com.jakobmenke.bootrestgenerator.templates.Templates;
import com.jakobmenke.bootrestgenerator.utils.Configuration;
import com.jakobmenke.bootrestgenerator.utils.Globals;
import com.jakobmenke.bootrestgenerator.utils.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Objects;

public class Main {
    private void writeTemplates(ArrayList<Entity> entities) {
        Templates templates = new Templates();
        for (Entity entityName : entities) {
            String entityTemplate = templates.getEntityTemplate(entityName, Globals.PACKAGE);
            createFile("entity", entityName.getEntityName() + ".java", entityTemplate);

            String serviceTemplate = templates.getResourceTemplate(Globals.PACKAGE, entityName.getEntityName());
            createFile("rest", entityName.getEntityName() + "Resource.java", serviceTemplate);

            String daoTemplate = templates.getDaoTemplate(Globals.PACKAGE, entityName.getEntityName());
            createFile("dao", entityName.getEntityName() + "Dao.java", daoTemplate);

            String repositoryTemplate = templates.getRepositoryTemplate(Globals.PACKAGE, entityName.getEntityName());
            createFile("repository", entityName.getEntityName() + "Repository.java", repositoryTemplate);
        }

        String constantsTemplate = templates.getFileTemplateByName(Globals.PACKAGE, "constants");
        createFile("utils", "GlobalConstants.java", constantsTemplate);
        String daotemplate = templates.getFileTemplateByName(Globals.PACKAGE, "genericdao");
       createFile("dao", "GenericDao.java", daotemplate);
    }

    private void createFile(String folderName, String fileName, String fileTemplate) {
        PrintWriter writer;

        try {

            String path = Globals.SRC_FOLDER + File.separator + Globals.PACKAGE + File.separator + folderName;
            File file = new File(path);

            if (!file.exists()) {
                boolean mkdirStatus = file.mkdirs();
                if (!mkdirStatus) {
                    System.err.println(new StringBuilder().append("\n_____________Class:").append(Thread.currentThread().getStackTrace()[1].getClassName()).append("____Method:").append(Thread.currentThread().getStackTrace()[1].getMethodName()).append("___Line:").append(Thread.currentThread().getStackTrace()[1].getLineNumber()).append("____\n_____________mkdirStatus = ").append(mkdirStatus).append("_____________\n"));
                }
            }
            writer = new PrintWriter(path + "/" + fileName, "UTF-8");
            writer.print(fileTemplate);
            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        Configuration configuration = new Configuration(Objects.requireNonNull(Configuration.readConfig("config.properties")));
        Globals.PACKAGE = configuration.getTargetPackage();
        Globals.SRC_FOLDER = configuration.getSrcFolder();
        Globals.FILE_NAME = configuration.getFileName();

        ArrayList<Entity> entities = new ArrayList<>();

        ArrayList<String> words = new ArrayList<>();

        Util.getWords(words, Objects.requireNonNull(Main.class.getClassLoader().getResourceAsStream(Globals.FILE_NAME)));

        Util.parseWords(entities, words);

        Main main = new Main();

        main.writeTemplates(entities);
    }
}
