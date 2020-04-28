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
package org.apache.ibatis.cache.decorators;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

/**
 * 二级缓存事务缓冲区
 * The 2nd level cache transactional buffer.
 * <p>
 * This class holds all cache entries that are to be added to the 2nd level cache during a Session.
 * Entries are sent to the cache when commit is called or discarded if the Session is rolled back.
 * Blocking cache support has been added. Therefore any get() that returns a cache miss
 * will be followed by a put() so any lock associated with the key can be released.
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 *
 * commit: 提交待提交的缓存(entriesToAddOnCommit)
 * rollback: 清空待提交的缓存(entriesToAddOnCommit), 清空在真实缓存(delegate)中未命中(entriesMissedInCache)的值
 */
public class TransactionalCache implements Cache {

  private static final Log log = LogFactory.getLog(TransactionalCache.class);
  // 缓存对象
  private final Cache delegate;
  // 是否需要清空待提交空间的标识
  private boolean clearOnCommit;
  // 本地缓存区：所有待提交的缓存
  private final Map<Object, Object> entriesToAddOnCommit;
  // 错误修改
  // 未命中的缓存集合，用于统计缓存命中率，防止缓存穿透：防止一个为null的key一直访问数据库
  private final Set<Object> entriesMissedInCache;



  public TransactionalCache(Cache delegate) {
    this.delegate = delegate;
    this.clearOnCommit = false;
    this.entriesToAddOnCommit = new HashMap<>();
    this.entriesMissedInCache = new HashSet<>();
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public int getSize() {
    return delegate.getSize();
  }

  @Override
  public Object getObject(Object key) {
    // issue #116
    // 通过key去缓存对象中获取
    Object object = delegate.getObject(key);
    if (object == null) {
      // 获取不到, 存入未命中缓存集合
      entriesMissedInCache.add(key);
    }
    // issue #146
    // 如果为true，表示是需要清空的，可能是事务失败，回滚，所以返回null
    if (clearOnCommit) {
      return null;
    } else {
      return object;
    }
  }


  /**
   * 本来应该put到缓存里面去，这里先put到需要待提交的空间里面去,
   * 解决脏读的问题: 避免事务回滚后, 缓存和数据库数据不一致, 通过使用待提交空间缓存来解决
   *
   * @param key Can be any object but usually it is a {@link CacheKey}
   * @param object
   */
  @Override
  public void putObject(Object key, Object object) {
    entriesToAddOnCommit.put(key, object);
  }

  @Override
  public Object removeObject(Object key) {
    return null;
  }

  @Override
  public void clear() {
    clearOnCommit = true;
    entriesToAddOnCommit.clear();
  }

  // 事务提交
  public void commit() {
    if (clearOnCommit) {
      delegate.clear();
    }
    // 把待提交的缓存刷到真实缓存去
    flushPendingEntries();
    reset();
  }

  // 事务回滚
  public void rollback() {
    unlockMissedEntries();
    reset();
  }

  // 重置
  private void reset() {
    clearOnCommit = false;
    // 清空待提交缓存
    entriesToAddOnCommit.clear();
    // 清空未命中缓存
    entriesMissedInCache.clear();
  }

  // 刷新待提交缓存到真实缓存
  private void flushPendingEntries() {
    // 遍历待提交的缓存
    for (Map.Entry<Object, Object> entry : entriesToAddOnCommit.entrySet()) {
      //put到真实缓存
      delegate.putObject(entry.getKey(), entry.getValue());
    }
    for (Object entry : entriesMissedInCache) {
      //也会把未命中的一起put
      if (!entriesToAddOnCommit.containsKey(entry)) {
        delegate.putObject(entry, null);
      }
    }
  }

  private void unlockMissedEntries() {
    for (Object entry : entriesMissedInCache) {
      try {
        //清空在真实缓存区里面的未命中的缓存
        delegate.removeObject(entry);
      } catch (Exception e) {
        log.warn("Unexpected exception while notifiying a rollback to the cache adapter."
            + "Consider upgrading your cache adapter to the latest version.  Cause: " + e);
      }
    }
  }

}
