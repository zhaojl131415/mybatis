package com;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Spring MVC在controller接收参数时, 获取参数不是 arg0, arg1 格式, 原因是Spring MVC底层调用的不是JDK的API, 而是去解析class字节码
 *
 * 在jdk8以前 调用这个getActualParamName() 会有问题 arg0, arg1
 *
 * jdk 8且Mybatis必须是3.4.1版本以上
 * File->Settings->Build,Execution,Deployment->Compiler->Java Compiler 在 Additional command line parameters: 后面填上 -parameters
 * maven compile
 *
 * 官方手册说自Mybatis3.4.1版本后，允许使用方法签名中实际的参数名作为他们的别名，
 * 也就是说在有个参数的方法中，你不再需要添加@Param注解便可以直接在sql语句中使用参数本身的名字。
 * 为了使用这个特性，有个很重要的前提：你的项目必须采用Java8编译并且加上 -parameters 选项。
 */
public class TestArg {

  public void test(String name,String age){

  }

  public static void main(String[] args) throws Exception{
    Method test = TestArg.class.getMethod("test", String.class, String.class);
    for (Parameter parameter : test.getParameters()) {
      System.out.println(parameter.getName());
    }
  }
}
