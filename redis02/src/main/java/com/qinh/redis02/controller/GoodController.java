package com.qinh.redis02.controller;

import com.qinh.redis02.util.RedisUtils;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Qh
 * @version 1.0
 * @date 2021-11-21 22:29
 */
@RestController
public class GoodController {

    private static final String REDIS_LOCK_KEY = "lockRedis";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${server.port}")
    private String serverPort;

    @Autowired
    private Redisson redisson;

    /**
     * 单机版本，加锁可以解决超卖的问题
     * @return
     */
    //@GetMapping("/buy_goods")
    public String buy_Goods() {
        // 从 redis 中获取商品的剩余数量
        String result = stringRedisTemplate.opsForValue().get("goods:001");
        int goodsNumber = result == null ? 0 : Integer.parseInt(result);
        String retStr = null;

        // 商品数量大于零才能出售
        if (goodsNumber > 0) {
            int realNumber = goodsNumber - 1;
            stringRedisTemplate.opsForValue().set("goods:001", realNumber + "");
            retStr = "你已经成功秒杀商品，此时还剩余：" + realNumber + "件" + "\t 服务器端口: " + serverPort;
        } else {
            retStr = "商品已经售罄/活动结束/调用超时，欢迎下次光临" + "\t 服务器端口: " + serverPort;
        }
        System.out.println(retStr);
        return retStr;
    }

    /**
     * 分布式环境中，会出现超卖问题
     * @return
     */
    //@GetMapping("/buy_goods")
    public String buy_GoodsV2() {
        synchronized (this) {
            // 从 redis 中获取商品的剩余数量
            String result = stringRedisTemplate.opsForValue().get("goods:001");
            int goodsNumber = result == null ? 0 : Integer.parseInt(result);
            String retStr = null;

            // 商品数量大于零才能出售
            if (goodsNumber > 0) {
                int realNumber = goodsNumber - 1;
                stringRedisTemplate.opsForValue().set("goods:001", realNumber + "");
                retStr = "你已经成功秒杀商品，此时还剩余：" + realNumber + "件" + "\t 服务器端口: " + serverPort;
            } else {
                retStr = "商品已经售罄/活动结束/调用超时，欢迎下次光临" + "\t 服务器端口: " + serverPort;
            }
            System.out.println(retStr);
            return retStr;
        }
    }

    /**
     * Redis加锁，解决超卖问题
     * @return
     */
    //@GetMapping("/buy_goods")
    public String buy_GoodsV3() {
        // 当前请求的 UUID + 线程名
        String value = UUID.randomUUID().toString()+Thread.currentThread().getName();
        // setIfAbsent() 就相当于 setnx，如果不存在就新建锁
        Boolean lockFlag = stringRedisTemplate.opsForValue().setIfAbsent(REDIS_LOCK_KEY, value);

        // 抢锁失败
        if(lockFlag == false){
            System.out.println("抢占锁失败");
            return "抢锁失败 o(╥﹏╥)o";
        }

        // 从 redis 中获取商品的剩余数量
        String result = stringRedisTemplate.opsForValue().get("goods:001");
        int goodsNumber = result == null ? 0 : Integer.parseInt(result);
        String retStr = null;

        // 商品数量大于零才能出售
        if (goodsNumber > 0) {
            int realNumber = goodsNumber - 1;
            stringRedisTemplate.opsForValue().set("goods:001", realNumber + "");
            retStr = "你已经成功秒杀商品，此时还剩余：" + realNumber + "件" + "\t 服务器端口: " + serverPort;
        } else {
            retStr = "商品已经售罄/活动结束/调用超时，欢迎下次光临" + "\t 服务器端口: " + serverPort;
        }
        System.out.println(retStr);
        stringRedisTemplate.delete(REDIS_LOCK_KEY); // 释放分布式锁
        return retStr;
    }

    /**
     * 优化，如果代码异常，最后要释放锁
     * 但是这段代码还是有问题，如果代码还未走到finally服务器就down了，锁一直未释放
     * @return
     */
    //@GetMapping("/buy_goods")
    public String buy_GoodsV4() {
        // 当前请求的 UUID + 线程名
        String value = UUID.randomUUID().toString() + Thread.currentThread().getName();
        try {
            // setIfAbsent() 就相当于 setnx，如果不存在就新建锁
            Boolean lockFlag = stringRedisTemplate.opsForValue().setIfAbsent(REDIS_LOCK_KEY, value);

            // 抢锁失败
            if (lockFlag == false) {
                return "抢锁失败 o(╥﹏╥)o";
            }

            // 从 redis 中获取商品的剩余数量
            String result = stringRedisTemplate.opsForValue().get("goods:001");
            int goodsNumber = result == null ? 0 : Integer.parseInt(result);
            String retStr = null;

            // 商品数量大于零才能出售
            if (goodsNumber > 0) {
                int realNumber = goodsNumber - 1;
                stringRedisTemplate.opsForValue().set("goods:001", realNumber + "");
                retStr = "你已经成功秒杀商品，此时还剩余：" + realNumber + "件" + "\t 服务器端口: " + serverPort;
            } else {
                retStr = "商品已经售罄/活动结束/调用超时，欢迎下次光临" + "\t 服务器端口: " + serverPort;
            }
            System.out.println(retStr);
            return retStr;
        } finally {
            stringRedisTemplate.delete(REDIS_LOCK_KEY); // 释放分布式锁
        }
    }

    /**
     * 解决服务器down机，分布式锁还未释放问题
     * @return
     */
    //@GetMapping("/buy_goods")
    public String buy_GoodsV5() {
        // 当前请求的 UUID + 线程名
        String value = UUID.randomUUID().toString() + Thread.currentThread().getName();
        try {
            // setIfAbsent() 就相当于 setnx，如果不存在就新建锁
            Boolean lockFlag = stringRedisTemplate.opsForValue().setIfAbsent(REDIS_LOCK_KEY, value);
            // 设置过期时间为 10s
            stringRedisTemplate.expire(REDIS_LOCK_KEY, 10L, TimeUnit.SECONDS);

            // 抢锁失败
            if (lockFlag == false) {
                return "抢锁失败 o(╥﹏╥)o";
            }

            // 从 redis 中获取商品的剩余数量
            String result = stringRedisTemplate.opsForValue().get("goods:001");
            int goodsNumber = result == null ? 0 : Integer.parseInt(result);
            String retStr = null;

            // 商品数量大于零才能出售
            if (goodsNumber > 0) {
                int realNumber = goodsNumber - 1;
                stringRedisTemplate.opsForValue().set("goods:001", realNumber + "");
                retStr = "你已经成功秒杀商品，此时还剩余：" + realNumber + "件" + "\t 服务器端口: " + serverPort;
            } else {
                retStr = "商品已经售罄/活动结束/调用超时，欢迎下次光临" + "\t 服务器端口: " + serverPort;
            }
            System.out.println(retStr);
            return retStr;
        } finally {
            stringRedisTemplate.delete(REDIS_LOCK_KEY); // 释放分布式锁
        }
    }

    /**
     * 加锁和设置过期时间不是原子操作，如果加锁后服务器就down了，锁还是未释放
     * @return
     */
    //@GetMapping("/buy_goods")
    public String buy_GoodsV6() {
        // 当前请求的 UUID + 线程名
        String value = UUID.randomUUID().toString() + Thread.currentThread().getName();
        try {
            // setIfAbsent() 就相当于 setnx，如果不存在就新建锁，同时加上过期时间保证原子性
            Boolean lockFlag = stringRedisTemplate.opsForValue().setIfAbsent(REDIS_LOCK_KEY, value, 10L, TimeUnit.SECONDS);

            // 抢锁失败
            if (lockFlag == false) {
                return "抢锁失败 o(╥﹏╥)o";
            }

            // 从 redis 中获取商品的剩余数量
            String result = stringRedisTemplate.opsForValue().get("goods:001");
            int goodsNumber = result == null ? 0 : Integer.parseInt(result);
            String retStr = null;

            // 商品数量大于零才能出售
            if (goodsNumber > 0) {
                int realNumber = goodsNumber - 1;
                stringRedisTemplate.opsForValue().set("goods:001", realNumber + "");
                retStr = "你已经成功秒杀商品，此时还剩余：" + realNumber + "件" + "\t 服务器端口: " + serverPort;
            } else {
                retStr = "商品已经售罄/活动结束/调用超时，欢迎下次光临" + "\t 服务器端口: " + serverPort;
            }
            System.out.println(retStr);
            return retStr;
        } finally {
            stringRedisTemplate.delete(REDIS_LOCK_KEY); // 释放分布式锁
        }
    }


    /**
     * 解决 出现释放了别人的锁 的问题
     * @return
     */
    //@GetMapping("/buy_goods")
    public String buy_GoodsV7() {
        // 当前请求的 UUID + 线程名
        String value = UUID.randomUUID().toString() + Thread.currentThread().getName();
        try {
            // setIfAbsent() 就相当于 setnx，如果不存在就新建锁，同时加上过期时间保证原子性
            Boolean lockFlag = stringRedisTemplate.opsForValue().setIfAbsent(REDIS_LOCK_KEY, value, 10L, TimeUnit.SECONDS);

            // 抢锁失败
            if (lockFlag == false) {
                return "抢锁失败 o(╥﹏╥)o";
            }

            // 从 redis 中获取商品的剩余数量
            String result = stringRedisTemplate.opsForValue().get("goods:001");
            int goodsNumber = result == null ? 0 : Integer.parseInt(result);
            String retStr = null;

            // 商品数量大于零才能出售
            if (goodsNumber > 0) {
                int realNumber = goodsNumber - 1;
                stringRedisTemplate.opsForValue().set("goods:001", realNumber + "");
                retStr = "你已经成功秒杀商品，此时还剩余：" + realNumber + "件" + "\t 服务器端口: " + serverPort;
            } else {
                retStr = "商品已经售罄/活动结束/调用超时，欢迎下次光临" + "\t 服务器端口: " + serverPort;
            }
            System.out.println(retStr);
            return retStr;
        } finally {
            //判断是否是自己加的锁，如果是才释放锁
            if (value.equalsIgnoreCase(stringRedisTemplate.opsForValue().get(REDIS_LOCK_KEY))){
                stringRedisTemplate.delete(REDIS_LOCK_KEY); // 释放分布式锁
            }
        }
    }


    /**
     * 在 finally 代码块中的判断与删除并不是原子操作，假设执行 if 判断的时候，这把锁还是属于当前业务，
     * 但是有可能刚执行完 if 判断，这把锁就被其他业务给释放了，还是会出现误删锁的情况
     * @return
     */
    //@GetMapping("/buy_goods")
    public String buy_GoodsV8() {
        // 当前请求的 UUID + 线程名
        String value = UUID.randomUUID().toString() + Thread.currentThread().getName();
        try {
            // setIfAbsent() 就相当于 setnx，如果不存在就新建锁，同时加上过期时间保证原子性
            Boolean lockFlag = stringRedisTemplate.opsForValue().setIfAbsent(REDIS_LOCK_KEY, value, 10L, TimeUnit.SECONDS);

            // 抢锁失败
            if (lockFlag == false) {
                return "抢锁失败 o(╥﹏╥)o";
            }

            // 从 redis 中获取商品的剩余数量
            String result = stringRedisTemplate.opsForValue().get("goods:001");
            int goodsNumber = result == null ? 0 : Integer.parseInt(result);
            String retStr = null;

            // 商品数量大于零才能出售
            if (goodsNumber > 0) {
                int realNumber = goodsNumber - 1;
                stringRedisTemplate.opsForValue().set("goods:001", realNumber + "");
                retStr = "你已经成功秒杀商品，此时还剩余：" + realNumber + "件" + "\t 服务器端口: " + serverPort;
            } else {
                retStr = "商品已经售罄/活动结束/调用超时，欢迎下次光临" + "\t 服务器端口: " + serverPort;
            }
            System.out.println(retStr);
            return retStr;
        } finally {
            while (true){
                //添加对锁的监视
                stringRedisTemplate.watch(REDIS_LOCK_KEY);
                //判断是否是自己加的锁，如果是才释放锁
                if (value.equalsIgnoreCase(stringRedisTemplate.opsForValue().get(REDIS_LOCK_KEY))){
                    //是自己的锁，此时开启事务
                    stringRedisTemplate.setEnableTransactionSupport(true);
                    stringRedisTemplate.multi();
                    // 释放分布式锁
                    stringRedisTemplate.delete(REDIS_LOCK_KEY);
                    //执行命令
                    List<Object> result = stringRedisTemplate.exec();
                    //如果为null，表明锁被动过，需要再次判断
                    if (Objects.isNull(result)){
                        continue;
                    }
                }
                //释放监视
                stringRedisTemplate.unwatch();
                break;
            }
        }
    }


    //@GetMapping("/buy_goods")
    public String buy_GoodsV9() throws Exception {
        // 当前请求的 UUID + 线程名
        String value = UUID.randomUUID().toString() + Thread.currentThread().getName();
        try {
            // setIfAbsent() 就相当于 setnx，如果不存在就新建锁，同时加上过期时间保证原子性
            Boolean lockFlag = stringRedisTemplate.opsForValue().setIfAbsent(REDIS_LOCK_KEY, value, 10L, TimeUnit.SECONDS);

            // 抢锁失败
            if (lockFlag == false) {
                return "抢锁失败 o(╥﹏╥)o";
            }

            // 从 redis 中获取商品的剩余数量
            String result = stringRedisTemplate.opsForValue().get("goods:001");
            int goodsNumber = result == null ? 0 : Integer.parseInt(result);
            String retStr = null;

            // 商品数量大于零才能出售
            if (goodsNumber > 0) {
                int realNumber = goodsNumber - 1;
                stringRedisTemplate.opsForValue().set("goods:001", realNumber + "");
                retStr = "你已经成功秒杀商品，此时还剩余：" + realNumber + "件" + "\t 服务器端口: " + serverPort;
            } else {
                retStr = "商品已经售罄/活动结束/调用超时，欢迎下次光临" + "\t 服务器端口: " + serverPort;
            }
            System.out.println(retStr);
            return retStr;
        } finally {
            // 获取连接对象
            Jedis jedis = RedisUtils.getJedis();
            // lua 脚本，摘自官网
            String script = "if redis.call('get', KEYS[1]) == ARGV[1]" + "then "
                    + "return redis.call('del', KEYS[1])" + "else " + "  return 0 " + "end";
            try {
                // 执行 lua 脚本
                Object result = jedis.eval(script, Collections.singletonList(REDIS_LOCK_KEY), Collections.singletonList(value));
                // 获取 lua 脚本的执行结果
                if ("1".equals(result.toString())) {
                    System.out.println("------del REDIS_LOCK_KEY success");
                } else {
                    System.out.println("------del REDIS_LOCK_KEY error");
                }
            } finally {
                // 关闭链接
                if (null != jedis) {
                    jedis.close();
                }
            }
        }
    }

    //@GetMapping("/buy_goods")
    public String buy_GoodsV10() throws Exception {
        // 当前请求的 UUID + 线程名
        String value = UUID.randomUUID().toString() + Thread.currentThread().getName();

        //上锁
        RLock lock = redisson.getLock(REDIS_LOCK_KEY);
        lock.lock();

        try {
            // 从 redis 中获取商品的剩余数量
            String result = stringRedisTemplate.opsForValue().get("goods:001");
            int goodsNumber = result == null ? 0 : Integer.parseInt(result);
            String retStr = null;

            // 商品数量大于零才能出售
            if (goodsNumber > 0) {
                int realNumber = goodsNumber - 1;
                stringRedisTemplate.opsForValue().set("goods:001", realNumber + "");
                retStr = "你已经成功秒杀商品，此时还剩余：" + realNumber + "件" + "\t 服务器端口: " + serverPort;
            } else {
                retStr = "商品已经售罄/活动结束/调用超时，欢迎下次光临" + "\t 服务器端口: " + serverPort;
            }
            System.out.println(retStr);
            return retStr;
        } finally {
            lock.unlock();
        }
    }


    @GetMapping("/buy_goods")
    public String buy_GoodsV11() throws Exception {
        // 当前请求的 UUID + 线程名
        String value = UUID.randomUUID().toString() + Thread.currentThread().getName();

        //上锁
        RLock lock = redisson.getLock(REDIS_LOCK_KEY);
        lock.lock();

        try {
            // 从 redis 中获取商品的剩余数量
            String result = stringRedisTemplate.opsForValue().get("goods:001");
            int goodsNumber = result == null ? 0 : Integer.parseInt(result);
            String retStr = null;

            // 商品数量大于零才能出售
            if (goodsNumber > 0) {
                int realNumber = goodsNumber - 1;
                stringRedisTemplate.opsForValue().set("goods:001", realNumber + "");
                retStr = "你已经成功秒杀商品，此时还剩余：" + realNumber + "件" + "\t 服务器端口: " + serverPort;
            } else {
                retStr = "商品已经售罄/活动结束/调用超时，欢迎下次光临" + "\t 服务器端口: " + serverPort;
            }
            System.out.println(retStr);
            return retStr;
        } finally {
            // 还在持有锁的状态，并且是当前线程持有的锁再解锁
            if (lock.isLocked() && lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }
    }




}
