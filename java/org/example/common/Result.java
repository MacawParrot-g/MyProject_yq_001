package org.example.common;

import lombok.Data;
import org.example.common.DataViewType;

@Data
public class Result {
    private boolean success;
    private String message;
    private Object data;
    private DataViewType viewType;

    public static Result success(String message, Object data) {
        Result r = new Result();
        r.success = true;
        r.message = message;
        r.data = data;
        return r;
    }

    public static Result success(String message, Object data, DataViewType viewType) {
        Result r = new Result();
        r.success = true;
        r.message = message;
        r.data = data;
        r.viewType = viewType;
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
