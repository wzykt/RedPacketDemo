package com.example;

import com.example.service.RedPacketService;
import com.example.util.RedisUtil;
import com.example.util.Result;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@RunWith(SpringRunner.class)
public class RedPacketTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(RedPacketTest.class);
    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private RedPacketService redPacketService;


    private static int corePoolSize = Runtime.getRuntime().availableProcessors();

    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(
            corePoolSize,
            corePoolSize + 1,
            10l,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000)

    );

    @Before
    public void init() {
        RestTemplate restTemplate = new RestTemplate();
        //初始化红包，redPacketId=100，红包金额100，十人份
        String url = "http://localhost:8080/init?redPacketId=100&number=100&count=10";
        Result result = restTemplate.postForObject(url, null, Result.class);
    }

    /**
     * 测试start
     */
    @Test
    public void Test1() {
        RestTemplate restTemplate = new RestTemplate();
        final CountDownLatch countDownLatch = new CountDownLatch(100);
        //模拟一百个人抢红包
        for (int i = 1; i <= 100; i++) {
            int userId = i;
            Runnable runnable = () -> {
                //String url = "http://localhost:80/start?redPacketId=100&userId=" + userId;
                String url = "http://localhost:8080/start?redPacketId=100&userId=" + userId;
                restTemplate.postForObject(url, null, Result.class);
                countDownLatch.countDown();
            };
            executor.execute(runnable);

        }
        try {
            countDownLatch.await();
            Integer restMoney = Integer.parseInt(redisUtil.getValue(100 + "-money").toString());
            LOGGER.info("剩余金额：{}", restMoney);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 测试start 红包未抢完的情况
     */
    @Test
    public void Test2() {
        RestTemplate restTemplate = new RestTemplate();
        final CountDownLatch countDownLatch = new CountDownLatch(9);
        //模拟九个人抢红包
        for (int i = 1; i <= 9; i++) {
            int userId = i;

            Runnable runnable = () -> {
                //String url = "http://localhost:80/startTwo?redPacketId=100&userId=" + userId;
                String url = "http://localhost:8080/start?redPacketId=100&userId=" + userId;
                restTemplate.postForObject(url, null, Result.class);
                countDownLatch.countDown();
            };
            executor.execute(runnable);
        }
        try {
            countDownLatch.await();
            Integer restMoney = Integer.parseInt(redisUtil.getValue(100 + "-money").toString());
            LOGGER.info("剩余金额：{}", restMoney);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
