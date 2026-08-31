package org.example.common;

public enum DataViewType {
    ALL("全部数据"),
    APPFLYER("appflyer"),
    ADJUST("adjust"),
    SINGULAR("singular"),
    TENJIN("tenjin"),
    FROZEN("已冻结数据"),
    APPFLYER_FROZEN("appflyer · 已冻结"),
    ADJUST_FROZEN("adjust · 已冻结"),
    SINGULAR_FROZEN("singular · 已冻结"),
    TENJIN_FROZEN("tenjin · 已冻结");

    private final String description;

    DataViewType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
