package com.jakobmenke.bootrestgenerator;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Column {
    String dbName;
    String camelName;
    String dataType;
    String javaType;
}
