# 2026 FRC 機器人程式碼審查與架構分析報告 (REBUILT 賽季)

本報告針對 `RobotControl2026` 專案進行深入分析。專案設定為 **2026 REBUILT** 賽季 (主要遊戲物件為 **Fuel**，得分目標為場地中央的 **Hub**)。

報告重點包含：
1. **2026 REBUILT 賽季邏輯與遺留字眼檢查**
2. **各子系統分析與控制邏輯 Bug 檢查**（底盤、視覺、射擊、進氣、輸送等）
3. **架構與框架誤用檢查**（WPILib Command-based 潛在問題）

---

## 1. 2026 賽季邏輯檢查與遺留程式碼分析

專案的核心邏輯 (如 `AutoAimAndShoot` 瞄準 `kBlueHubX` / `kRedHubX`) 非常符合 REBUILT 賽季設定的「朝場地中央六角形 Hub 射擊 Fuel」這個概念。使用 `Math.atan2` 計算角度對於圓對稱/六角形對稱的中心 Hub 來說是**完全正確**的做法。

### 1.1 賽季殘留字眼與不一致
儘管核心邏輯正確，但在部分地方仍有不一致的註解與測試碼殘留：
1. **`Robot.java`**
   - 第 35 行: `// CameraServer.startAutomaticCapture("Coral CAM", 0);`
     - **問題**："Coral" 是官方實際 2025/2026 Reefscape 的物件，與本專案的 Fuel/Hub 概念衝突，這可能是從其他專案或早期測試複製貼上遺留下來的。
     - **建議**：將名稱改為 "Fuel CAM" 或單純的 "Intake CAM"。

2. **舊賽季的 Speaker 殘留**
   - 根據專案文檔 (`PROJECT_ANALYSIS.md`)，有些舊的變數可能仍叫做 `Speaker`。在目前的 `Constants.java` 中，您已經很好地將變數改為 `kBlueHubX/Y`。這部分轉換做得很棒！

---

## 2. 子系統架構分析與 Bug 檢查

在尚未進行實測的情況下，我從程式碼的邏輯、數學與框架使用上，找出了以下需要注意的潛在 Bug 與優化建議：

### 2.1 Swerve 底盤子系統 (`Swerve.java` & `Drive2Tag.java`)
- **優點**：實作了 `SwerveDrivePoseEstimator` 融合 Limelight，並且正確處理了加速度限制 (`SlewRateLimiter`)。
- **🔴 潛在 Bug (中高風險)**：
  1. **PID 控制器缺少連續輸入處理 (`Drive2Tag.java`)**：
     在 `Drive2Tag` 的 `execute()` 中，用 `thetaController.calculate(currentYaw, m_targetYaw)` 計算旋轉速度，**但是** `thetaController` 並沒有設定為 Continuous Input。
     - **後果**：當機器人角度在 179 度，目標為 -179 度時，它不會轉短短的 2 度，而是會逆向旋轉 358 度！這在場上會導致機器人劇烈抽搐打轉。
     - **修復**：在 `Drive2Tag` 建構子中加入 `thetaController.enableContinuousInput(-180, 180);` 或使用弧度 `-Math.PI, Math.PI`。
  2. **魔術數字與不一致的速度上限 (`Drive2Tag.java`)**：
     - `execute` 中硬塞了 `xSpeed*=6.0;` 與 `Math.min(1.8, xSpeed)`，這完全脫離了 `SwerveConstants.kMaxPhysicalSpeedMps` (5.13 m/s) 的設計。如果未來更換齒輪比，這裡會壞掉。
  3. **模擬角度積分不準確 (`Swerve.java`)**：
     - `simGyroAngleDeg += Math.toDegrees(zSpeed) * 0.02;`。直接把 Control Input 當成真實速度做積分誤差很大。最好透過 Swerve Kinematics 算出的 `ChassisSpeeds.omegaRadiansPerSecond` 來積分。

### 2.2 射球飛輪 (`ShooterSubsystem.java`)
- **優點**：使用 TalonFX 的 `VelocityVoltage` 閉環控制，並且有 `Follower(Opposed)`，設定非常標準且優良。
- **🟡 潛在問題 (低風險)**：
  - `isAtSpeed(double targetRps, double tolerance)` 邏輯是 `Math.abs(currentRps - targetRps) < tolerance`。
    如果 Command 傳入了一個馬達物理上達不到的極高目標 (例如 100 RPS)，它會永遠回傳 false。若這個判斷被用在 Auto Mode (如 `AutoAimAndShoot` 裡的對齊等待)，會導致整個 Auto 流程卡死。
    - **建議**：在 Auto 的 Command 中 (`createAutoShootCommand`)，加入適當的 Timeout。目前的實作 `withTimeout(2.0)` 處理得很好！

### 2.3 輸送帶 (`TransportSubsystem.java`)
- **優點**：拆分了 `up_to_shoot` 與 `transport`，並且有獨立的三段控制方法。
- **🔴 潛在 Bug (中風險)**：
  - **DutyCycleOut 模式未正確關閉**：
    TalonFX 的 `DutyCycleOut` 是一個 Set-and-Hold 指令，一旦發送，馬達會一直保持該電壓。
    雖然您的 Command (`sys_runTransport`) 的 `end()` 區塊有呼叫 `stop()`，但請確保所有呼叫 `runUpToShoot()` 的地方，在不需要時都有明確呼叫 `stopUpToShoot()`。目前在 `AutoAimAndShoot.java` 裡的 else 區塊有寫 `m_transport.stopUpToShoot()`，這點做得很好，但要非常小心任何未處理的 Command Interrupt 狀態。
  - **保護限制被註解**：
    `CurrentLimits.SupplyCurrentThreshold` 被註解掉了，直接硬卡 `SupplyCurrentLimit = 40.0`。這對於吸入較重遊戲物件的瞬間啟動電流來說可能太嚴格，馬達可能一推球就軟掉。
    - **建議**：取消註解，允許 60A 的 0.5 秒 Burst 啟動電流。

### 2.4 Intake 手臂 (`IntakeArmSubsystem.java` - 未啟用但需注意)
- **🔴 潛在 Bug (高風險 - 若未來啟用)**：
  - `ForwardSoftLimitThreshold = 8.0` (8圈)
  - `setTargetPosition` 中：`double motorTargetRotations = armRotations * GEAR_RATIO;`
    **問題**：TalonFX 的 Soft Limit 通常是看馬達 Sensor 的位置 (也就是 Motor 圈數)。如果減速比 (`GEAR_RATIO`) 是 20.0，當您想要手臂轉 0.5 圈，馬達目標是 10 圈。此時它會直接撞到 `ForwardSoftLimitThreshold (8.0)` 而停擺！
    - **建議**：軟體限位的值必須乘上減速比。例如手臂極限是 0.5 圈，軟體限位應設為 `0.5 * 20.0 = 10.0` 圈。

### 2.5 自動瞄準與射擊 (`AutoAimAndShoot.java`)
- **優點**：
  - 動態分區 (己方區域瞄準 Hub，中立場地固定角度回傳) 是極好的實戰策略！
  - 引入了**遲滯現象 (Hysteresis)** 邏輯：首次觸發要求 2度，啟動後放寬到 5度，這能完美解決機器人邊移動邊射擊時，「推球馬達抽搐卡彈」的問題，非常高階的寫法！
- **🟡 潛在問題 (邏輯探討)**：
  - `isFinished()` 永遠回傳 `false`。
    這對於 Teleop 手動按住板機的模式 (whileTrue) 非常合適。但如果您打算在 PathPlanner 的 Auto Routine 中直接使用 `AutoAimAndShoot`，它會無限期阻塞後續的 Auto 指令。
    - **建議**：如果 Auto 需要使用它，建議寫一個子類別或重載建構子，當判斷 `m_isFeeding` 持續一定時間 (例如 0.5秒表示球已射出) 後，回傳 `true` 以結束指令。

---

## 3. 總結與修改行動建議 (Action Items)

在您實際上機測試之前，強烈建議進行以下修改：

1. **修復 `Drive2Tag` 的連續旋轉**：
   在 `Drive2Tag.java` 初始化 `thetaController` 處加入 `.enableContinuousInput(-Math.PI, Math.PI)` (若是使用弧度) 或 `(-180, 180)` (若是使用度數)。
2. **清除舊註解**：
   刪除 `Robot.java` 裡的 `Coral CAM` 註解，避免與現有 Fuel 遊戲物件概念混淆。
3. **確認手臂限位邏輯**：
   若打算重啟 `IntakeArmSubsystem`，除了要修改與 DRIVETRAIN 衝突的 CAN ID 外，必須修正 `ForwardSoftLimit` 的數值，將其設定為「馬達圈數」而非「手臂圈數」。
4. **檢查電流閥值**：
   在 `TransportSubsystem` 中，考慮把 `SupplyTimeThreshold` 與 `SupplyCurrentThreshold` 解除註解，以容忍推球瞬間的高電流需求。
