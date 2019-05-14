package com.jakobmenke.bootrestgenerator;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Table {
    String name;
    List<Column> columns = new ArrayList<>();

}
