package com.jakobmenke.bootrestgenerator.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ColumnToField {
    private String databaseColumnName;
    private String camelCaseFieldName;
    private String databaseType;
    private String javaType;
    private String databaseIdType;

    @Builder
    public ColumnToField(String databaseIdType, String databaseColumnNam, String camelCaseFieldName, String databaseType, String javaType) {
        this.databaseIdType = databaseIdType;
        this.databaseColumnName = databaseColumnNam;
        this.camelCaseFieldName = camelCaseFieldName;
        this.databaseType = databaseType;
        this.javaType = javaType;
    }

    @Builder
    public ColumnToField(String databaseColumnNam, String camelCaseFieldName, String databaseType, String javaType) {
        this.databaseColumnName = databaseColumnNam;
        this.camelCaseFieldName = camelCaseFieldName;
        this.databaseType = databaseType;
        this.javaType = javaType;
    }
}
