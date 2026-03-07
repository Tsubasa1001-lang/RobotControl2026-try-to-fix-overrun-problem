# 2026 FRC 機器人程式碼審查與架構分析報告

本報告針對 `RobotControl2026` 專案進行深入分析，重點包含：
1. **各子系統分析**（底盤、視覺、射擊、進氣、輸送等）
2. **2026 賽季邏輯檢查**（舊賽季遺留與 2026 賽季的適配性）
3. **架構與邏輯 Bug 檢查**（潛在錯誤、WPILib 框架誤用等）

---

## 1. 2026 賽季邏輯檢查與遺留程式碼分析

目前 FRC 2026 賽季遊戲為 **REEFSCAPE** (主要遊戲物件為 **Coral** 珊瑚 與 **Algae** 藻類，得分目標包含 **Reef**, **Processor**, **Net**, **Barge**)。然而在程式碼中，混雜了部分舊有或自定義概念 (如 Hub, Fuel)。

### 1.1 賽季殘留字眼與邏輯不符
程式碼與常數中仍大量使用 `Hub` 與 `Fuel`：
- **`Constants.java`**
  - 第 30 行: `// 2026 REBUILT: 射入 Hub 得分（Fuel 遊戲物件）`
    *(註：2026 賽季為 Reefscape，沒有 Hub 和 Fuel)*
  - 第 37-46 行: 使用 `kBlueHubX`, `kBlueHubY`, `kRedHubX`, `kRedHubY` 作為目標座標。
    雖然有加上註解提到 "2026 REBUILT"，但這些名詞與座標邏輯需要更新以符合 Reefscape 中 Reef / Processor / Net 的實際位置與瞄準需求。
  - 第 64 行: `(2026 REBUILT Hub 距離)`
  - 第 80 行: `精準進 Hub`

- **`AutoAimAndShoot.java`**
  - 第 22 行: `1. 根據機器人目前座標，計算面向目標（Hub）所需的旋轉角度`
  - 第 129 行: `己方場域 → 瞄準 Hub 得分 (2026 REBUILT: Fuel 射入 Hub)`
  - 第 141, 156, 174 行皆有殘留的 Hub 相關邏輯。

- **`Robot.java`**
  - 第 35 行註解中出現了 `// CameraServer.startAutomaticCapture("Coral CAM", 0);` 這是唯一正確反映今年賽季 (Coral) 的註解。

### 1.2 今年賽事適配性檢查建議
1. **座標系與瞄準邏輯更新**：
   2026 年 Reef 是六角形結構，有多個得分面與高度 (L1-L4)，不像過去 Speaker 只有單一點。如果程式依然使用單點 `atan2` 計算角度 (如目前程式碼中的 `kBlueHubX/Y` 單一座標點)，將無法滿足 Reef 的多面精準放球/射球需求。
   **建議**：改寫 `AutoAimAndShoot`，使其能夠根據機器人當前位置，動態計算距離**最近的 Reef 面**，或是改為針對 Processor/Net 進行瞄準。
2. **重命名變數**：
   將所有 `Hub` 相關常數重命名為對應的 2026 目標 (例如 `ReefBranch`, `Processor`, `Net`)，以避免開發時混淆。

---

## 2. 子系統架構分析與 Bug 檢查

### 2.1 Swerve 底盤子系統 (`Swerve.java` & `ManualDrive.java`)
- **優點**：
  - 已經實作了 `SwerveDrivePoseEstimator` 融合 Limelight。
  - 搖桿輸入有計算 Null Zone (Deadband) 與速度 SlewRateLimiter，這是不錯的實踐。
- **潛在 Bug / 建議**：
  1. **魔術數字 (Magic Numbers)**：
     在 `Drive2Tag.java` 的 `execute` 中，最後乘上了 `6.0` 並且使用了 Hardcode 的 Clamp 限制 `-1.8` 到 `1.8`，這不符合 `SwerveConstants.kMaxPhysicalSpeedMps` 的設計。
  2. **不正確的連續旋轉控制**：
     在 `Drive2Tag.java` 的 `thetaController` 並沒有設定 `enableContinuousInput(-180, 180)` (或 -PI 到 PI)。當 Yaw 跨越 180 度到 -180 度邊界時，機器人會發生猛烈旋轉 (繞遠路)。
  3. **模擬支援的角速度積分**：
     `Swerve.java` 中的 `simGyroAngleDeg += Math.toDegrees(zSpeed) * 0.02;`。這在大旋轉下不一定準確，更好的做法是透過 ChassisSpeeds 和 Kinematics 計算真實角速度。

### 2.2 射球飛輪 (`ShooterSubsystem.java`)
- **優點**：使用 TalonFX 的 `VelocityVoltage` 閉環控制，並且有 `Follower(Opposed)`，設計正確。
- **潛在 Bug / 建議**：
  - `isAtSpeed()` 方法只檢查 Leader Motor，這通常沒問題，但在極端情況下 Follower 如果卡住不會被發現。
  - 在 `isAtSpeed(double targetRps, double tolerance)` 的實作中：
    ```java
    double currentRps = leaderMotor.getVelocity().getValueAsDouble();
    return Math.abs(currentRps - targetRps) < tolerance;
    ```
    邏輯正確，但若 `targetRps` 極大導致根本無法到達，會永遠回傳 false 導致卡在某個 Auto State。建議配合 Timeout。

### 2.3 Intake 滾輪 (`IntakeRollerSubsystem.java`)
- **優點**：使用 Stator 與 Supply Current Limit，並且設定 Brake 模式防掉球。
- **潛在 Bug / 建議**：
  - `INTAKE_TARGET_RPS = 60.0` 非常快，要確認真實物理測試是否會把珊瑚 (Coral) 或藻類 (Algae) 打壞。

### 2.4 輸送帶 (`TransportSubsystem.java`)
- **優點**：拆分了 `up_to_shoot` 與 `transport`，並且有獨立控制方法，方便送球到發射位置。
- **潛在 Bug / 建議**：
  - **DutyCycleOut 設定後未停止 (Bug風險)**：
    程式中使用 `DutyCycleOut`，它是一種 Set-and-Hold 模式。當你在 Command 結束時如果沒有正確呼叫 `stop()` (例如 Command 被打斷但沒有 `interrupted` 處理，或 `stopUpToShoot` 沒有被執行)，馬達會一直轉。
    目前 `sys_runTransport()` 和 `sys_slowRunTransport()` 有正確放入 `() -> { stop(); }` 的 end function，這部分沒問題。
  - **電流限制瞬間飆升**：註解中提到 `// 允許它瞬間(0.5秒內)衝到 60A 沒關係`，但程式碼中相關設定被註解掉了，目前 SupplyLimit 直接硬卡 40A。這可能會在剛啟動吸入較重物件時跳掉，建議將 Threshold 設定加回來。

### 2.5 機器手臂 (`IntakeArmSubsystem.java`)
- **已知問題**：CAN ID 衝突 (3, 4 與 Swerve 衝突)。如不使用應保持不實例化。
- **潛在 Bug / 建議**：
  - 設定了軟體限位 `ForwardSoftLimitThreshold = 8.0` 和 `ReverseSoftLimitThreshold = 0.0`。
  - 但在 `setTargetPosition` 中：`double motorTargetRotations = armRotations * GEAR_RATIO;` 這裡如果傳入 `armRotations` 為 1，目標變成 20 圈，會直接撞到 `ForwardSoftLimitThreshold (8.0)` 導致根本動不了。需確認軟體限位設定是依據 "Motor 圈數" 還是 "Arm 圈數"！(TalonFX 的 Limit 通常是看 Sensor 位置，也就是 Motor 圈數)。

### 2.6 自動瞄準 (`AutoAimAndShoot.java`)
- **優點**：遲滯邏輯 (Hysteresis) 寫得很好，避免了射擊邊緣反覆抖動的問題。
- **潛在 Bug / 建議**：
  - `execute()` 內每 20ms 不斷重新宣告 `Translation2d toTarget = m_targetPosition.minus(robotPosition);`，不會造成太大負擔，但角度計算：
    `targetAngleRad = Math.atan2(toTarget.getY(), toTarget.getX());`
    計算的是 **機器人原點** 到 **目標點** 的角度。若射擊口不在機器人正中心，而是在前後邊緣，會有視差角度誤差，導致遠距離射偏。
  - `isFinished()` 永遠回傳 `false`，必須依靠按鈕 `whileTrue()` 終止。這是標準做法，但在 Auto 模式中若想要排程這個指令，將會卡死 Auto。建議為 Auto 寫一個會自動結束的版本 (例如發射完後 0.5 秒結束)。

---

## 3. 總結與修改行動建議

1. **修正 2026 遊戲邏輯與名詞**：將 Hub, Fuel 全部取代為 Reef, Processor 等今年名詞，並重新設計 Reef 六角形多面瞄準的座標邏輯 (不要單點 atan2)。
2. **修正 `Drive2Tag.java` 的連續旋轉**：為 `thetaController` 加上 `enableContinuousInput(-180, 180)`。
3. **檢查 `IntakeArmSubsystem` 的軟限位**：修改 8.0 圈限位為考慮 GEAR_RATIO 後的馬達轉子限位，避免被鎖死。
4. **移除或替換 Magic Numbers**：特別是 `Drive2Tag` 裡面的 `6.0` 倍率，盡量使用 `Constants` 中的最大速度。
5. **確保 Auto 模式適配**：目前 `AutoAimAndShoot` 指令會卡死 Sequence，如果要在 PathPlanner 的 Auto 中使用，需要建立帶有 Timeout 或是判斷已送球便結束的 Wrapper Command。
