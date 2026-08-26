package org.example.common;

import lombok.Data;
import org.example.common.DataViewType;

@Data
public class Result {
    private boolean success;
    private String message;
    private Object data;
    private DataViewType viewType;
    private long total;
    private int page;
    private int size;

    public static Result success(String message, Object data) {
        Result r = new Result();
        r.success = true;
        r.message = message;
        r.data = data;
        return r;
    }

    public static Result success(String message, Object data, DataViewType viewType, long total, int page, int size) {
        Result r = new Result();
        r.success = true;
        r.message = message;
        r.data = data;
        r.viewType = viewType;
        r.total = total;
        r.page = page;
        r.size = size;
        return r;
    }

    public static Result success(String message) {
        return success(message, null);
    }

    public static Result fail(String message) {
        Result r = new Result();
        r.success = false;
        r.message = message;
        r.data = null;
        return r;
    }
}
