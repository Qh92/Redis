package com.qinh.redis02.util;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author Qh
 * @version 1.0
 * @date 2021/11/22 15:01
 */
public class RedisUtils {

    private static JedisPool jedisPool;

    private static String hostAddr = "192.168.30.139";

    static {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(20);
        jedisPoolConfig.setMaxIdle(10);
        jedisPool = new JedisPool(jedisPoolConfig, hostAddr, 6379, 100000);
    }

    public static Jedis getJedis() throws Exception {
        if (null != jedisPool) {
            return jedisPool.getResource();
        }
        throw new Exception("Jedispool is not ok");
    }
}
