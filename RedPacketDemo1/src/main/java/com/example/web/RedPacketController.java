package com.example.web;

import com.example.queue.jvm.RedPacketMessage;
import com.example.queue.jvm.RedPacketQueue;
import com.example.service.RedPacketService;
import com.example.util.DoubleUtil;
import com.example.util.RedisUtil;
import com.example.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/")
public class RedPacketController {

    private final static Logger LOGGER = LoggerFactory.getLogger(RedPacketController.class);

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private RedPacketService redPacketService;

    /**
     * 抢红包 拆红包 抢到不一定能拆到
     * 建议使用抢红包二的方式
     *
     * @param redPacketId
     * @param userId
     * @return
     */
    @PostMapping("/start")
    public Result start(long redPacketId, long userId) {
        /**
         * 判断用户是否多次参与枪红包，记录中存在，直接返回记录
         */


        /**
         * 抢红包 判断剩余金额
         */
        Integer money = (Integer) redisUtil.getValue(redPacketId + "-money");
        if (money > 0) {
            /**
             * 虽然能抢到 但是不一定能拆到
             * 类似于微信的 点击红包显示抢的按钮
             * 没有就直接显示无
             */
            Result result = redPacketService.startTwoSeckil(redPacketId, (int) userId);
            if (result.get("code").toString().equals("500")) {
                LOGGER.info("用户{}手慢了，红包派完了", userId);
            } else {
                Double amount = DoubleUtil.divide(Double.parseDouble(result.get("msg").toString()), (double) 100);
                LOGGER.info("用户{}抢红包成功，金额：{}", userId, amount);
            }
        } else {
            /**
             * 直接显示手慢了，红包派完了
             */
            LOGGER.info("用户{}手慢了，红包派完了", userId);
        }
        return Result.ok();
    }

    /**
     * 有人没抢 红包发多了
     * 红包进入延迟队列
     * 实现过期失效
     *
     * @param redPacketId
     * @param userId
     * @return
     */

    /**
     * 初始化红包
     *
     * @param redPacketId
     * @param number
     * @param count
     * @return
     */
    @PostMapping("/init")
    public Result init(long redPacketId, int number, int count) {
        //这里红包Id没有用UUID
        Result result = redPacketService.redPacketInit(redPacketId, number, count);
        /**
         * 加入延迟队列 24s秒过期
         * RedPacketMessage message = new RedPacketMessage(redPacketId, 24);
         * RedPacketQueue.getQueue().produce(message);
         */
        RedPacketMessage message = new RedPacketMessage(redPacketId, 24);
        RedPacketQueue.getQueue().produce(message);
        return result.ok();
    }
}
