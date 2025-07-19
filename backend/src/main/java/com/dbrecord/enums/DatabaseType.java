package com.dbrecord.enums;

/**
 * 数据库类型枚举
 */
public enum DatabaseType {
    MYSQL("mysql", "MySQL数据库"),
    POSTGRESQL("postgresql", "PostgreSQL数据库"),
    KINGBASE("kingbase", "人大金仓数据库"),
    ORACLE("oracle", "Oracle数据库"),
    SQLSERVER("sqlserver", "SQL Server数据库");
    
    private final String code;
    private final String description;
    
    DatabaseType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据代码获取数据库类型
     */
    public static DatabaseType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (DatabaseType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * 判断是否支持该数据库类型
     */
    public static boolean isSupported(String code) {
        return fromCode(code) != null;
    }
}