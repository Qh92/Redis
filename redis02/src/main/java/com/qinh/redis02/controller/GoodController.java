package com.qinh.redis02.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Qh
 * @version 1.0
 * @date 2021-11-21 22:29
 */
@RestController
public class GoodController {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${server.port}")
    private String serverPort;

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

    @GetMapping("/buy_goods")
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
}
