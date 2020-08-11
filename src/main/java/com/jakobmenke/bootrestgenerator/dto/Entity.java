package com.jakobmenke.bootrestgenerator.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Entity {
    private String tableName;
    private String entityName;
    private List<ColumnToField> columns = new ArrayList<>();
}
