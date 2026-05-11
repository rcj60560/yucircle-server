package com.yucircle.dto;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private long total;
    private int page;
    private int limit;
    private List<T> records;

    public PageResult(long total, int page, int limit, List<T> records) {
        this.total = total;
        this.page = page;
        this.limit = limit;
        this.records = records;
    }
}
