package com.plugin;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Invocation;

import java.util.Properties;

/**
 * @author zhaojinliang
 * @version 1.0
 * @description TODO
 * @date 2020-03-20 21:24
 */
public class MyPagePlugin implements Interceptor {
  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    return null;
  }

  @Override
  public Object plugin(Object target) {
    return null;
  }

  @Override
  public void setProperties(Properties properties) {

  }
}
