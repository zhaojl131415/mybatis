/**
 *    Copyright ${license.git.copyrightYears} the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

public class ParamNameResolver {

  private static final String GENERIC_NAME_PREFIX = "param";

  /**
   * <p>
   * The key is the index and the value is the name of the parameter.<br />
   * The name is obtained from {@link Param} if specified. When {@link Param} is not specified,
   * the parameter index is used. Note that this index could be different from the actual index
   * when the method has special parameters (i.e. {@link RowBounds} or {@link ResultHandler}).
   * </p>
   * <ul>
   * <li>aMethod(@Param("M") int a, @Param("N") int b) -&gt; {{0, "M"}, {1, "N"}}</li>
   * <li>aMethod(int a, int b) -&gt; {{0, "0"}, {1, "1"}}</li>
   * <li>aMethod(int a, RowBounds rb, int b) -&gt; {{0, "0"}, {2, "1"}}</li>
   * </ul>
   */
  private final SortedMap<Integer, String> names;

  private boolean hasParamAnnotation;

  /**
   * 解析Mapper对象中方法的参数名
   * @param config
   * @param method
   */
  public ParamNameResolver(Configuration config, Method method) {
    // 获取参数类型
    final Class<?>[] paramTypes = method.getParameterTypes();
    // 获取参数注解
    final Annotation[][] paramAnnotations = method.getParameterAnnotations();
    final SortedMap<Integer, String> map = new TreeMap<>();
    int paramCount = paramAnnotations.length;
    // get names from @Param annotations 从@Param注释获取名称
    for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
      if (isSpecialParameter(paramTypes[paramIndex])) {
        // skip special parameters 跳过特殊参数
        continue;
      }
      // 参数名
      String name = null;
      // 遍历参数注解，找到@Param，并将@Param的value赋值给name
      for (Annotation annotation : paramAnnotations[paramIndex]) {
        if (annotation instanceof Param) {
          hasParamAnnotation = true;
          name = ((Param) annotation).value();
          break;
        }
      }
      // 如果注解的value没拿到name，再通过反射去拿
      if (name == null) {
        // @Param was not specified. 没有指定@Param。
        /**
         * Spring MVC 底层调用的不是JDK的API  Spring MVC底层是去解析class字节码
         *
         * 在jdk8以前 调用这个getActualParamName() 会有问题 arg0
         *
         * jdk 8且Mybatis必须是3.4.1版本以上
         * File->Settings->Build,Execution,Deployment->Compiler->Java Compiler 在 Additional command line parameters: 后面填上 -parameters
         * maven compile
         *
         * 官方手册说自Mybatis3.4.1版本后，允许使用方法签名中实际的参数名作为他们的别名，
         * 也就是说在有个参数的方法中，你不再需要添加@Param注解便可以直接在sql语句中使用参数本身的名字。
         * 为了使用这个特性，有个很重要的前提：你的项目必须采用Java8编译并且加上 -parameters 选项。
         */
        if (config.isUseActualParamName()) {
          // 底层是java反射：Parameter.getName()
          name = getActualParamName(method, paramIndex);
        }
        if (name == null) {
          // use the parameter index as the name ("0", "1", ...)
          // gcode issue #71
          name = String.valueOf(map.size());
        }
      }
      map.put(paramIndex, name);
    }
    names = Collections.unmodifiableSortedMap(map);
  }

  private String getActualParamName(Method method, int paramIndex) {
    return ParamNameUtil.getParamNames(method).get(paramIndex);
  }

  private static boolean isSpecialParameter(Class<?> clazz) {
    return RowBounds.class.isAssignableFrom(clazz) || ResultHandler.class.isAssignableFrom(clazz);
  }

  /**
   * Returns parameter names referenced by SQL providers.
   */
  public String[] getNames() {
    return names.values().toArray(new String[0]);
  }

  /**
   * <p>
   * A single non-special parameter is returned without a name.
   * Multiple parameters are named using the naming rule.
   * In addition to the default names, this method also adds the generic names (param1, param2,
   * ...).
   * </p>
   */
  public Object getNamedParams(Object[] args) {
    final int paramCount = names.size();
    // 如果传参为空
    if (args == null || paramCount == 0) {
      return null;
    // 如果没有参数注解@Param("id")，且参数数量只有一个
    } else if (!hasParamAnnotation && paramCount == 1) {
      return args[names.firstKey()];
    } else {
      // 多参数
      // 用于缓存方法上所有的参数
      final Map<String, Object> param = new ParamMap<>();
      int i = 0;
      // 遍历方法上所有的参数名称
      for (Map.Entry<Integer, String> entry : names.entrySet()) {
        // jdk获取参数的问题，需要File | Settings | Build, Execution, Deployment | Compiler | Java Compiler中配置-parameters：names : arg0,arg1
        param.put(entry.getValue(), args[entry.getKey()]);
        // 添加通用参数名称 add generic param names (param1, param2, ...)
        final String genericParamName = GENERIC_NAME_PREFIX + String.valueOf(i + 1);
        // ensure not to overwrite parameter named with @Param
        if (!names.containsValue(genericParamName)) {
          param.put(genericParamName, args[entry.getKey()]);
        }
        i++;
      }
      return param;
    }
  }
}
