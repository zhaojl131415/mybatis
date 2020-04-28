/**
 * Copyright ${license.git.copyrightYears} the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com;

import com.alibaba.druid.pool.DruidDataSource;
import com.mysql.jdbc.Driver;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.logging.log4j.Log4jImpl;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Test {
  public static void main(String[] args) throws Exception {
    String resource = "mybatis.xml";
    InputStream inputStream = Resources.getResourceAsStream("mybatis.xml");
    //xml解析完成
    //其实我们mybatis初始化方法 除了XML意外 其实也可以0xml完成
//   new SqlSessionFactoryBuilder().b
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
//    Configuration configuration = sqlSessionFactory.getConfiguration();
    //使用者可以随时使用或者销毁缓存
    //默认sqlsession不会自动提交
    //从SqlSession对象打开开始 缓存就已经存在
    SqlSession sqlSession = sqlSessionFactory.openSession();
    SqlSession sqlSession1 = sqlSessionFactory.openSession();

    //从调用者角度来讲 与数据库打交道的对象 SqlSession
    //通过动态代理 去帮我们执行SQL
    // 拿到一个动态代理后的mapper，
    DemoMapper mapper = sqlSession.getMapper(DemoMapper.class);
    DemoMapper mapper1 = sqlSession1.getMapper(DemoMapper.class);
//    Map<String, Object> map = new HashMap<>();
//    map.put("id", "1");

    // 这里就相当于是调用动态代理(MapperProxy)的invoke方法
    System.out.println(mapper.selectAll("1", "1"));


    // 因为一级缓存 这里不会调用两次SQL，
    System.out.println(mapper.selectAll("1", "1"));
    System.out.println(mapper.selectAll("1", "1"));
    // 如果有二级缓存 这里就不会调用两次SQL
    System.out.println(mapper1.selectAll("1", "1"));
    //当调用 sqlSession.close() 或者说刷新缓存方法， 或者说配置了定时清空缓存方法  都会销毁缓存
    sqlSession.close();

  }
}
