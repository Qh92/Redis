import redis.clients.jedis.Jedis;

/**
 * @author Qh
 * @version 1.0
 * @date 2021-06-02-23:07
 */
public class PingTest {

    public static void main(String[] args) {
        Jedis jedis = new Jedis("192.168.116.129", 6379);
        System.out.println(jedis.ping());
    }
}
