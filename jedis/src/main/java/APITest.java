import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Qh
 * @version 1.0
 * @date 2021-06-03-22:33
 */
public class APITest {
    public static void main(String[] args) {
        Jedis jedis = new Jedis("192.168.116.129", 6379);

        jedis.set("k1","v1");
        jedis.set("k2","v2");
        jedis.set("k3","v3");
        jedis.set("k4","v4");

        System.out.println(jedis.get("k1"));
        //key
        System.out.println("key................");
        Set<String> keys = jedis.keys("*");
        System.out.println(keys.size());
        /*for (String key : keys){
            System.out.println(key + " : " + jedis.get(key));
        }*/
        //String
        System.out.println("String........");
        jedis.set("k5","v5");
        //jedis.append("k6","v6");
        System.out.println(jedis.get("k6"));
        jedis.mset("k1","v11","k7","v7");
        System.out.println(jedis.get("k1"));
        System.out.println(jedis.get("k7"));
        //不存在才添加
        jedis.msetnx("k1","v111","k8","v8");
        System.out.println(jedis.get("k1"));
        System.out.println(jedis.get("k8"));

        //list
        System.out.println("list............");
        jedis.lpush("myList","zs","ls","ww");
        List<String> myList = jedis.lrange("myList", 0, -1);
        System.out.println(myList);

        //set
        System.out.println("set............");
        jedis.sadd("mySet","west","allen");
        jedis.sadd("mySet","james");
        Set<String> mySet = jedis.smembers("mySet");
        System.out.println(mySet);
        //set 删除元素
        jedis.srem("mySet","allen");
        System.out.println(jedis.smembers("mySet"));

        //hash
        System.out.println("hash..........");
        Map<String,String> hashMap = new HashMap<>();
        hashMap.put("k1","v1");
        hashMap.put("k2","v2");
        jedis.hset("hash1",hashMap);
        System.out.println(jedis.hget("hash1", "k1"));
        jedis.hset("hash2","username","zs");
        System.out.println(jedis.hget("hash2","username"));
        hashMap.put("k3","v3");
        jedis.hmset("hash3",hashMap);
        System.out.println(jedis.hmget("hash3","k1","k2","k3"));

        //zset
        System.out.println("zset.....");
        jedis.zadd("zset01",60,"v1");
        jedis.zadd("zset01",70,"v2");
        jedis.zadd("zset01",80,"v3");
        Set<String> zset01 = jedis.zrange("zset01", 0, -1);
        System.out.println(zset01);


    }
}
