package com.jakobmenke.bootrestgenerator;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Table {
    private String tableName;
    private String entityName;
    private List<Column> columns = new ArrayList<>();
}
