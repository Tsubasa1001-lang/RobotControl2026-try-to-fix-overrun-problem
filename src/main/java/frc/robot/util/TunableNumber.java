package frc.robot.util;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * 可在 Glass / SmartDashboard / Shuffleboard 即時修改的數值。
 * 
 * 用法：
 *   TunableNumber kP = new TunableNumber("Shooter/kP", 0.12);
 *   // 在 periodic() 裡呼叫
 *   if (kP.hasChanged()) {
 *       // 重新套用 PID
 *   }
 * 
 * Glass 操作方式：
 *   1. 開啟 Glass → NetworkTables → SmartDashboard
 *   2. 找到對應的 key（例如 "Shooter/kP"）
 *   3. 直接修改數值，程式會在下一個週期偵測到變更並自動套用
 */
public class TunableNumber {
    private final String key;
    private double lastValue;

    /**
     * 建立一個可調參數，並將預設值發布到 SmartDashboard / NetworkTables。
     * @param key   顯示在 Glass 上的名稱（建議用 "Subsystem/ParamName" 格式）
     * @param defaultValue 預設值
     */
    public TunableNumber(String key, double defaultValue) {
        this.key = key;
        this.lastValue = defaultValue;
        // 將預設值發布到 NetworkTables（如果 key 已經存在，getNumber 會回傳現有值）
        SmartDashboard.putNumber(key, defaultValue);
    }

    /**
     * 取得目前在 NetworkTables 上的數值。
     */
    public double get() {
        return SmartDashboard.getNumber(key, lastValue);
    }

    /**
     * 檢查數值是否被修改過（與上次呼叫 hasChanged() 時相比）。
     * 如果有變更，內部會更新 lastValue。
     * @return true 如果數值有變更
     */
    public boolean hasChanged() {
        double current = get();
        if (current != lastValue) {
            lastValue = current;
            return true;
        }
        return false;
    }

    /**
     * 取得 NetworkTables key 名稱。
     */
    public String getKey() {
        return key;
    }
}
