package com.example.service.impl;

import com.example.entity.RedPacket;
import com.example.entity.RedPacketRecord;
import com.example.lock.RedissLockUtil;
import com.example.queue.DynamicQuery;
import com.example.service.RedPacketService;
import com.example.util.RedisUtil;
import com.example.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RedPacketServiceImpl implements RedPacketService {
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private DynamicQuery dynamicQuery;

    @Override
    public RedPacket get(long redPacketId) {
        return null;
    }

    @Override
    public Result redPacketInit(long redPacketId, long money, long number) {
        /**
         * 初始化红包数据，抢红包拦截
         */
        redisUtil.cacheValue(redPacketId + "-num", number);
        /**
         * 初始化剩余人数，拆红包拦截
         */
//        redisUtil.cacheValue(redPacketId + "-restPeople", number);
        /**
         * 初始化红包金额，单位为分
         */
        redisUtil.cacheValue(redPacketId + "-money", money * 100);
        return Result.ok();
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result startTwoSeckil(long redPacketId, int userId) {
        Integer money = 0;
        boolean res=false;
        try {
            /**
             * 获取锁 保证红包数量和计算红包金额的原子性操作
             */
            res = RedissLockUtil.tryLock(redPacketId+"", TimeUnit.SECONDS, 3, 10);
            if(res){
                long restPeople = redisUtil.decr(redPacketId+"-num",1);
                if(restPeople<0){
                    return Result.error("手慢了，红包派完了");
                }else{
                    /**
                     * 如果是最后一人
                     */
                    if(restPeople==0){
                        money = Integer.parseInt(redisUtil.getValue(redPacketId+"-money").toString());
                    }else{
                        Integer restMoney = Integer.parseInt(redisUtil.getValue(redPacketId+"-money").toString());
                        Random random = new Random();
                        //随机范围：[1,剩余人均金额的两倍]
                        money = random.nextInt((int) (restMoney / (restPeople+1) * 2 - 1)) + 1;
                    }
                    redisUtil.decr(redPacketId+"-money",money);
                    /**
                     * 异步入库
                     */
                    RedPacketRecord record = new RedPacketRecord();
                    record.setMoney(money);
                    record.setRedPacketId(redPacketId);
                    record.setUid(userId);
                    record.setCreateTime(new Timestamp(System.currentTimeMillis()));
                    saveRecord(record);
                    /**
                     * 异步入账
                     */
                }
            }else{
                /**
                 * 获取锁失败相当于抢红包失败
                 */
                return Result.error("手慢了，红包派完了");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            //释放锁
            if(res){
                RedissLockUtil.unlock(redPacketId+"");
            }
        }
        return Result.ok(money);
    }


    @Async
    void saveRecord(RedPacketRecord record) {
        dynamicQuery.save(record);
    }
}
