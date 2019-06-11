package com.jakobmenke.bootrestgenerator;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Column {
    private String dbName;
    private String camelName;
    private String dataType;
    private String javaType;
    private String idType;

    @Builder
    public Column(String idType, String dbName, String camelName, String dataType, String javaType) {
        this.idType = idType;
        this.dbName = dbName;
        this.camelName = camelName;
        this.dataType = dataType;
        this.javaType = javaType;
    }

    @Builder
    public Column(String dbName, String camelName, String dataType, String javaType) {
        this.dbName = dbName;
        this.camelName = camelName;
        this.dataType = dataType;
        this.javaType = javaType;
    }
}
