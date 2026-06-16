package com.crm.customer.service;

import com.crm.customer.entity.Customer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 客户加权评分模型
 *
 * 三维度：
 *   订单金额得分（40分）：反映客户价值体量
 *   复购频率得分（35分）：反映客户忠诚度与活跃度
 *   账期信用得分（25分）：反映客户付款可靠性
 *
 * 等级划分：S(80-100) / A(60-79) / B(40-59) / C(0-39)
 */
@Component
public class CustomerScoringService {

    public int calculate(Customer customer) {
        int amountScore    = scoreByAmount(customer.getTotalAmount());    // 0-40
        int frequencyScore = scoreByFrequency(customer.getOrderCount());  // 0-35
        int creditScore    = scoreByCredit(customer.getOverdueCount(),
                                           customer.getCreditDays());     // 0-25
        return amountScore + frequencyScore + creditScore;
    }

    public String levelOf(int score) {
        if (score >= 80) return "S";
        if (score >= 60) return "A";
        if (score >= 40) return "B";
        return "C";
    }

    /**
     * 按历史累计订单金额评分（满分40）
     * >=50万 → 40, >=20万 → 30, >=5万 → 20, >=1万 → 10, else 0
     */
    private int scoreByAmount(BigDecimal totalAmount) {
        if (totalAmount == null) return 0;
        double amount = totalAmount.doubleValue();
        if (amount >= 500000) return 40;
        if (amount >= 200000) return 30;
        if (amount >= 50000)  return 20;
        if (amount >= 10000)  return 10;
        return 0;
    }

    /**
     * 按历史订单笔数评分（满分35）
     * >=10笔 → 35, >=5笔 → 25, >=3笔 → 15, >=1笔 → 8, else 0
     */
    private int scoreByFrequency(int orderCount) {
        if (orderCount >= 10) return 35;
        if (orderCount >= 5)  return 25;
        if (orderCount >= 3)  return 15;
        if (orderCount >= 1)  return 8;
        return 0;
    }

    /**
     * 按账期信用评分（满分25）：逾期次数越多扣分越多
     * 0次逾期 → 25, 1次 → 18, 2次 → 10, >=3次 → 0
     * 账期为0（即时付款客户）额外加5分（上限25）
     */
    private int scoreByCredit(int overdueCount, int creditDays) {
        int base;
        if (overdueCount == 0)      base = 25;
        else if (overdueCount == 1) base = 18;
        else if (overdueCount == 2) base = 10;
        else                        base = 0;

        // 即时付款客户信用加成
        if (creditDays == 0) {
            base = Math.min(25, base + 5);
        }
        return base;
    }
}
