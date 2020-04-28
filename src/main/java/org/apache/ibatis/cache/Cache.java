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

import java.util.concurrent.locks.ReadWriteLock;

/**
 * SPI for cache providers.
 * <p>
 * One instance of cache will be created for each namespace.
 * <p>
 * The cache implementation must have a constructor that receives the cache id as an String parameter.
 * <p>
 * MyBatis will pass the namespace as id to the constructor.
 *
 * <pre>
 * public MyCache(final String id) {
 *  if (id == null) {
 *    throw new IllegalArgumentException("Cache instances require an ID");
 *  }
 *  this.id = id;
 *  initialize();
 * }
 * </pre>
 *
 * @author Clinton Begin
 *
 * 实现类：装饰者模式
 *
 * 1.SynchronizedCache：同步的缓存装饰器，synchronized修饰方法, 用于防止多线程并发访问
 * 2.LoggingCache：输出缓存命中的日志信息, 如果开启了DEBUG模式，则会输出命中率日志。
 * 3.SerializedCache：序列化功能，将值序列化后存到缓存中。该功能用于缓存返回一份实例的Copy，用于保存线程安全。
 * 4.ScheduledCache：定时清空Cache，但是并没有开始一个定时任务，而是在使用Cache的时候，才去检查时间是否到了。
 * 5.LruCache：最近最少使用算法，缓存回收策略,在内部保存一个LinkedHashMap
 * 6.FIFOCache：先进先出算法 回收策略，装饰类，内部维护了一个队列，来保证FIFO，一旦超出指定的大小，则从队列中获取Key并从被包装的Cache中移除该键值对。
 * 7.SoftCache：基于软引用实现的缓存管理策略,软引用回收策略，软引用只有当内存不足时才会被垃圾收集器回收
 * 8.WeakCache：基于弱引用实现的缓存管理策略
 * 9.PerpetualCache 作为最基础的缓存类, 永久缓存，一旦存入就一直保持，内部就是一个HashMap
 * 10.TransactionalCache 事务缓存，一次性存入多个缓存，移除多个缓存
 * 11.BlockingCache 可阻塞的缓存,内部实现是ConcurrentHashMap
 *
 * 第5、6、7、8种都是缓存清理的管理策略，可以通过修改xml配置指定按那种算法方式清理缓存，示例：
 * <cache eviction="LRU" flushInterval="10000" size="1024" readOnly="true"/>
 * eviction：代表的是缓存收回策略，有以下策略：
 *    LRU，最近最少使用的，移除最长时间不用的对象。对应LruCache
 *    FIFO，先进先出，按对象进入缓存的顺序来移除他们。对应FIFOCache
 *    SOFT，软引用，移除基于垃圾回收器状态和软引用规则的对象。对应SoftCache
 *    WEAK，弱引用，更积极的移除基于垃圾收集器状态和若引用规则的对象。对应WeakCache
 *
 * flushInterval：刷新间隔时间，单位为毫秒，默认是当sql执行(默认增删改操作)的时候才会去刷新。
 * size：引用数目，一个正整数，代表缓存最多可以存储多少对象，不宜设置过大，过大会造成内存溢出。
 * readOnly：只读，意味着缓存数据只能读取，不能修改，这样设置的好处是我们可以快速读取缓存，去诶但是我们没有办法修改缓存。默认值为false，不允许我们修改。
 */

public interface Cache {

  /**
   * @return The identifier of this cache
   */
  String getId();

  /**
   * @param key Can be any object but usually it is a {@link CacheKey}
   * @param value The result of a select.
   */
  void putObject(Object key, Object value);

  /**
   * @param key The key
   * @return The object stored in the cache.
   */
  Object getObject(Object key);

  /**
   * As of 3.3.0 this method is only called during a rollback
   * for any previous value that was missing in the cache.
   * This lets any blocking cache to release the lock that
   * may have previously put on the key.
   * A blocking cache puts a lock when a value is null
   * and releases it when the value is back again.
   * This way other threads will wait for the value to be
   * available instead of hitting the database.
   *
   *
   * @param key The key
   * @return Not used
   */
  Object removeObject(Object key);

  /**
   * Clears this cache instance.
   */
  void clear();

  /**
   * Optional. This method is not called by the core.
   *
   * @return The number of elements stored in the cache (not its capacity).
   */
  int getSize();

  /**
   * Optional. As of 3.2.6 this method is no longer called by the core.
   * <p>
   * Any locking needed by the cache must be provided internally by the cache provider.
   *
   * @return A ReadWriteLock
   */
  default ReadWriteLock getReadWriteLock() {
    return null;
  }

}
