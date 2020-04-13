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
package org.apache.ibatis.scripting.xmltags;

import java.util.regex.Pattern;

import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.parsing.TokenHandler;
import org.apache.ibatis.scripting.ScriptingException;
import org.apache.ibatis.type.SimpleTypeRegistry;

/**
 * @author Clinton Begin
 */
public class TextSqlNode implements SqlNode {
  private final String text;
  private final Pattern injectionFilter;

  public TextSqlNode(String text) {
    this(text, null);
  }

  public TextSqlNode(String text, Pattern injectionFilter) {
    this.text = text;
    this.injectionFilter = injectionFilter;
  }

  /**
   * 判读是否为动态sql：${  }
   * @return
   */
  public boolean isDynamic() {
    DynamicCheckerTokenParser checker = new DynamicCheckerTokenParser();
    // "${  }"
    GenericTokenParser parser = createParser(checker);
    // 方法中调用org.apache.ibatis.scripting.xmltags.TextSqlNode.DynamicCheckerTokenParser.handleToken()方法：将isDynamic设置为true，没有其他操作
    parser.parse(text);
    // 返回isDynamic
    return checker.isDynamic();
  }

  @Override
  public boolean apply(DynamicContext context) {
    GenericTokenParser parser = createParser(new BindingTokenParser(context, injectionFilter));
    context.appendSql(parser.parse(text));
    return true;
  }

  private GenericTokenParser createParser(TokenHandler handler) {
    return new GenericTokenParser("${", "}", handler);
  }

  private static class BindingTokenParser implements TokenHandler {

    private DynamicContext context;
    private Pattern injectionFilter;

    public BindingTokenParser(DynamicContext context, Pattern injectionFilter) {
      this.context = context;
      this.injectionFilter = injectionFilter;
    }


    /**
     * 字符串替换：将${id}直接替换成参数值，不做参数类型检查和转换，
     * 如果传参为String，但数据库字段为int，会报错。
     * @param content
     * @return
     */
    @Override
    public String handleToken(String content) {
      Object parameter = context.getBindings().get("_parameter");
      if (parameter == null) {
        context.getBindings().put("value", null);
      } else if (SimpleTypeRegistry.isSimpleType(parameter.getClass())) {
        context.getBindings().put("value", parameter);
      }
      //OGNL
      Object value = OgnlCache.getValue(content, context.getBindings());
      String srtValue = value == null ? "" : String.valueOf(value); // issue #274 return "" instead of "null"
      checkInjection(srtValue);
      return srtValue;
    }

    private void checkInjection(String value) {
      if (injectionFilter != null && !injectionFilter.matcher(value).matches()) {
        throw new ScriptingException("Invalid input. Please conform to regex" + injectionFilter.pattern());
      }
    }
  }

  private static class DynamicCheckerTokenParser implements TokenHandler {

    private boolean isDynamic;

    public DynamicCheckerTokenParser() {
      // Prevent Synthetic Access
    }

    public boolean isDynamic() {
      return isDynamic;
    }


    //PreparedStatement 一个是预编译 一个不是预编译 Statement
    //当扫描到${}的时候调用此方法  其实就是不解析 在运行时候在替换成具体的值
    @Override
    public String handleToken(String content) {
      this.isDynamic = true;
      return null;
    }
  }

}
