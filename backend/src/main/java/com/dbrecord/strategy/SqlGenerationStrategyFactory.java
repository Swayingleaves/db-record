package com.dbrecord.strategy;

import com.dbrecord.strategy.impl.KingbaseGenerationStrategy;
import com.dbrecord.strategy.impl.MySqlGenerationStrategy;
import com.dbrecord.strategy.impl.PostgreSqlGenerationStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL生成策略工厂类
 * 根据数据库类型返回相应的SQL生成策略
 */
@Component
public class SqlGenerationStrategyFactory {
    
    private final Map<String, SqlGenerationStrategy> strategies = new HashMap<>();
    
    @Autowired
    public SqlGenerationStrategyFactory(List<SqlGenerationStrategy> strategyList) {
        for (SqlGenerationStrategy strategy : strategyList) {
            strategies.put(strategy.getDatabaseType(), strategy);
        }
    }
    
    /**
     * 根据数据库类型获取相应的SQL生成策略
     * @param databaseType 数据库类型（mysql, postgresql, kingbase）
     * @return SQL生成策略实例
     * @throws IllegalArgumentException 如果不支持该数据库类型
     */
    public SqlGenerationStrategy getStrategy(String databaseType) {
        if (databaseType == null || databaseType.trim().isEmpty()) {
            throw new IllegalArgumentException("数据库类型不能为空");
        }
        
        String normalizedType = databaseType.toLowerCase().trim();
        SqlGenerationStrategy strategy = strategies.get(normalizedType);
        
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的数据库类型: " + databaseType + 
                ". 支持的类型: " + String.join(", ", strategies.keySet()));
        }
        
        return strategy;
    }
    
    /**
     * 获取所有支持的数据库类型
     * @return 支持的数据库类型列表
     */
    public String[] getSupportedDatabaseTypes() {
        return strategies.keySet().toArray(new String[0]);
    }
    
    /**
     * 检查是否支持指定的数据库类型
     * @param databaseType 数据库类型
     * @return 是否支持
     */
    public boolean isSupported(String databaseType) {
        if (databaseType == null || databaseType.trim().isEmpty()) {
            return false;
        }
        return strategies.containsKey(databaseType.toLowerCase().trim());
    }
}