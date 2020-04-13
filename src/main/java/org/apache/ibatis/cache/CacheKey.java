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
package org.apache.ibatis.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.apache.ibatis.reflection.ArrayUtil;

/**
 * @author Clinton Begin
 */
public class CacheKey implements Cloneable, Serializable {

  private static final long serialVersionUID = 1146682552656046210L;

  public static final CacheKey NULL_CACHE_KEY = new NullCacheKey();
  //周瑜 hashMap  31
  private static final int DEFAULT_MULTIPLYER = 37;
  private static final int DEFAULT_HASHCODE = 17;

  // 默认DEFAULT_MULTIPLYER 37
  private final int multiplier;
  // 计算后的hashCode
  private int hashcode;
  // 累加所有条件的hashCode
  private long checksum;
  // 条件累加值
  private int count;
  // 8/21/2017 - Sonarlint flags this as needing to be marked transient.  While true if content is not serializable, this is not always true and thus should not be marked transient.
  private List<Object> updateList;

  public CacheKey() {
    this.hashcode = DEFAULT_HASHCODE;
    this.multiplier = DEFAULT_MULTIPLYER;
    this.count = 0;
    this.updateList = new ArrayList<>();
  }

  public CacheKey(Object[] objects) {
    this();
    updateAll(objects);
  }

  public int getUpdateCount() {
    return updateList.size();
  }

  /**
   * 生成CacheKey的条件有多个, 所以方法会被调用多次
   * @param object
   */
  public void update(Object object) {
    // 获取对象的hashCode
    int baseHashCode = object == null ? 1 : ArrayUtil.hashCode(object);

//  java判断两个对象：  = =  判断的是内存地址
// map 判断两个key: 首先判断hashcode hashcode 相同的情况下  判断equals



    // 判断依据累加值: 其实就是这个方法的调用次数
    count++;
    // 累加所有条件的hashCode
    checksum += baseHashCode;
    baseHashCode *= count;

    // 计算hashCode
    // multiplier默认为37 , hashcode第一次调用初始为17
    hashcode = multiplier * hashcode + baseHashCode;
    //
    updateList.add(object);
  }

  public void updateAll(Object[] objects) {
    for (Object o : objects) {
      update(o);
    }
  }

  //Student   name = 1   age   =1   // student == student1   false
  //  //Student1  name = 1  age =1


  /**
   * 判断CacheKey是否相等
   * @param object
   * @return
   */
  @Override
  public boolean equals(Object object) {
    // 判断两个对象的内存地址是否相同
    if (this == object) {
      return true;
    }
    // 判断类型是否相同
    if (!(object instanceof CacheKey)) {
      return false;
    }

    final CacheKey cacheKey = (CacheKey) object;

    // 判断hashcode是否相同
    if (hashcode != cacheKey.hashcode) {
      return false;
    }
    // 判断累加所有条件的hashCode是否相同
    if (checksum != cacheKey.checksum) {
      return false;
    }
    // 判断update方法调用次数是否相同
    if (count != cacheKey.count) {
      return false;
    }
    // 遍历条件对象是否相同
    for (int i = 0; i < updateList.size(); i++) {
      Object thisObject = updateList.get(i);
      Object thatObject = cacheKey.updateList.get(i);
      if (!ArrayUtil.equals(thisObject, thatObject)) {
        return false;
      }
    }
    // 以上如果都相同, 两个CacheKey是同一个
    return true;
  }

  /**
   * 重写hashCode
   * @return
   */
  @Override
  public int hashCode() {
    return hashcode;
  }

  @Override
  public String toString() {
    StringJoiner returnValue = new StringJoiner(":");
    returnValue.add(String.valueOf(hashcode));
    returnValue.add(String.valueOf(checksum));
    updateList.stream().map(ArrayUtil::toString).forEach(returnValue::add);
    return returnValue.toString();
  }

  @Override
  public CacheKey clone() throws CloneNotSupportedException {
    CacheKey clonedCacheKey = (CacheKey) super.clone();
    clonedCacheKey.updateList = new ArrayList<>(updateList);
    return clonedCacheKey;
  }

}
