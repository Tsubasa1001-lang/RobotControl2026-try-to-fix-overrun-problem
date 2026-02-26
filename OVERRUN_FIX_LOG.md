# Loop Overrun 修復紀錄

**修復日期**：2026-02-26  
**專案**：RobotControl2026  
**問題**：機器人執行動作時出現延遲 / WPILib Loop Overrun 警告

---

## 背景說明

FRC 機器人使用 `TimedRobot` 架構，預設每 **20ms** 執行一次 `robotPeriodic()`。  
當單次迴圈執行時間超過 20ms，Driver Station 就會顯示 **"Loop overrun"** 警告，  
導致控制訊號延遲、動作卡頓，甚至影響比賽安全。

本次共修復 **5 個問題**，涵蓋 5 個檔案。

---

## 修復清單

### 修復 1 ⭐ 最高優先 — `Swerve.java`

**問題**：`SetRobotOrientation()` 每次 periodic 都阻塞主迴圈

| 項目 | 內容 |
|------|------|
| **檔案** | `src/main/java/frc/robot/subsystems/Swerve.java` |
| **位置** | `periodic()` 方法內，呼叫 Limelight 的區塊 |
| **嚴重程度** | 🔴 高 — **最可能的 overrun 主因** |

**修改前**：
```java
LimelightHelpers.SetRobotOrientation(limelightName, 
    gyroAngle.getDegrees(), getGyroRateDps(), 0, 0, 0, 0);
```

**修改後**：
```java
LimelightHelpers.SetRobotOrientation_NoFlush(limelightName, 
    gyroAngle.getDegrees(), getGyroRateDps(), 0, 0, 0, 0);
```

**為什麼要這樣做**：

`SetRobotOrientation()` 內部會呼叫：
```
SetRobotOrientation_INTERNAL(..., flush = true)
  └─> NetworkTableInstance.getDefault().flush()  ← 阻塞！
```

`flush()` 是一個**同步阻塞（blocking）** 操作，它會讓 Java 執行緒停在那裡，  
等待所有 NetworkTables 資料完整發送到 Limelight 之後才繼續。  

這個操作在 `periodic()` 裡**每 20ms 執行一次**。  
當網路稍有延遲或 Limelight 忙碌時，flush 等待時間可能超過數毫秒，  
直接導致整個迴圈超過 20ms 的時間預算，觸發 **loop overrun**。

`_NoFlush` 版本會讓 NetworkTables 在背景自動發送資料，完全不阻塞主迴圈。

---

### 修復 2 — `SwerveModule.java`

**問題**：`setState()` 和 `runRotorPID()` 重複讀取 CAN Bus

| 項目 | 內容 |
|------|------|
| **檔案** | `src/main/java/frc/robot/subsystems/SwerveModule.java` |
| **位置** | `setState()` 和 `runRotorPID()` 方法 |
| **嚴重程度** | 🟠 中高 — 每個迴圈多 8 次不必要的 CAN 讀取 |

**修改前**：
```java
public void setState(SwerveModuleState state) {
    state.optimize(getState().angle);      // 第 1 次 CAN 讀取
    state.cosineScale(getState().angle);   // 第 2 次 CAN 讀取
    runRotorPID(state.angle.getDegrees()); // 進入 runRotorPID...
    setThrottleSpeed(state.speedMetersPerSecond);
}

private void runRotorPID(double angle) {
    double rotorOutput = mRotorPID.calculate(getState().angle.getDegrees(), angle); // 第 3 次 CAN 讀取
    setRotorSpeed(mRotorPID.atSetpoint() ? 0 : rotorOutput);
}
```

**修改後**：
```java
public void setState(SwerveModuleState state) {
    Rotation2d currentAngle = getState().angle;  // 只讀 1 次，快取結果

    state.optimize(currentAngle);
    state.cosineScale(currentAngle);

    runRotorPID(currentAngle, state.angle.getDegrees()); // 傳入已讀的角度
    setThrottleSpeed(state.speedMetersPerSecond);
}

private void runRotorPID(Rotation2d currentAngle, double targetAngle) {
    double rotorOutput = mRotorPID.calculate(currentAngle.getDegrees(), targetAngle); // 使用傳入值
    setRotorSpeed(mRotorPID.atSetpoint() ? 0 : rotorOutput);
}
```

**為什麼要這樣做**：

`getState().angle` 每次呼叫都會透過 **CAN Bus** 向 CANcoder 發出讀取請求。  
CAN Bus 的頻寬有限（預設 1Mbps），同一時間發出越多請求，每個設備的回應就越慢。

| 情況 | 每個模組 CAN 讀取次數 | 4 個模組總計 |
|------|----------------------|-------------|
| 修改前 | 3 次 | **12 次** |
| 修改後 | 1 次 | **4 次** |

每個迴圈節省了 **8 次不必要的 CAN 讀取**，直接降低 CAN Bus 負載。

---

### 修復 3 — `LightPollution.java`

**問題**：LED 每 20ms 更新一次，佔用迴圈時間

| 項目 | 內容 |
|------|------|
| **檔案** | `src/main/java/frc/robot/subsystems/LightPollution.java` |
| **位置** | `periodic()` 方法 |
| **嚴重程度** | 🟡 中 — 疊加增加迴圈時間 |

**修改前**：
```java
@Override
public void periodic() {
    ledPattern.applyTo(buffer);  // 每 20ms 計算一次動態效果
    led.setData(buffer);         // 每 20ms 序列化傳輸所有 LED 資料
    super.periodic();
}
```

**修改後**：
```java
private int ledUpdateCounter = 0;

@Override
public void periodic() {
    // 每 3 個迴圈 (約 60ms) 才更新一次 LED，減少 periodic 迴圈負擔
    if (++ledUpdateCounter >= 3) {
        ledPattern.applyTo(buffer);
        led.setData(buffer);
        ledUpdateCounter = 0;
    }
}
```

**為什麼要這樣做**：

`led.setData(buffer)` 需要透過 PWM/DIO 將每一顆 LED 的 R、G、B 值序列化傳出去，  
LED 數量越多，傳輸時間越長。`ledPattern.applyTo(buffer)` 對彩虹滾動等動態效果  
還需要做數學計算（時間插值、顏色混合等）。

這兩個操作對機器人動作控制**毫無幫助**，卻每 20ms 都佔用寶貴的迴圈時間。  
人眼對 60ms (約 16fps) 的 LED 更新完全感覺不到差異，  
但迴圈負擔降低了 **2/3**（從每 20ms 更新變為每 60ms 更新）。

---

### 修復 4 — `DriveSubsystem.java`

**問題**：`AutoBuilder.configure()` 被呼叫了兩次，設定互相衝突

| 項目 | 內容 |
|------|------|
| **檔案** | `src/main/java/frc/robot/subsystems/DriveSubsystem.java` |
| **位置** | 建構子 |
| **嚴重程度** | 🟡 中 — 造成自動路徑行為不可預測 |

**修改前**：
```java
// DriveSubsystem.java — 第一次呼叫（使用 Constants.AutoConstants 的 PID）
public DriveSubsystem(Swerve swerve) {
    AutoBuilder.configure(
        swerve::getPose, swerve::setPose, swerve::getChassisSpeeds,
        (speeds, feedforwards) -> swerve.setChassisSpeeds(speeds),
        new PPHolonomicDriveController(
            new PIDConstants(kTranslationController_kP, kI, kD),
            new PIDConstants(kRotationController_kP, kI, kD)
        ),
        config,
        () -> { ... },
        this  // subsystem = DriveSubsystem
    );
}

// RobotContainer.java — 第二次呼叫（覆蓋前者，使用不同 PID）
AutoBuilder.configure(
    swerve::getPose, swerve::resetPose, swerve::getChassisSpeeds, swerve::drive,
    new PPHolonomicDriveController(
        new PIDConstants(3.0, 0.0, 0.0),
        new PIDConstants(1.8, 0.0, 0.0)
    ),
    config,
    swerve::isAllianceRed,
    swerve  // subsystem = Swerve
);
```

**修改後**：
```java
// DriveSubsystem.java — 清空建構子，不再重複配置
public class DriveSubsystem extends SubsystemBase {
    public DriveSubsystem(Swerve swerve) {
        // AutoBuilder.configure() 已移至 RobotContainer 統一管理
    }
}
```

**為什麼要這樣做**：

`AutoBuilder.configure()` 是一個**全域的靜態設定**，整個程式只能有一份有效配置。  
第二次呼叫會直接覆蓋第一次的設定。  
兩次設定的差異：

| 項目 | DriveSubsystem（被覆蓋） | RobotContainer（實際生效） |
|------|------------------------|--------------------------|
| 平移 PID kP | `kTranslationController_kP = 1.3` | `3.0` |
| 旋轉 PID kP | `kRotationController_kP = 0.2` | `1.8` |
| drive 方法 | `setChassisSpeeds()` (有 0.185 縮放) | `drive()` (無縮放) |
| subsystem | `DriveSubsystem` | `Swerve` |

保留 `RobotContainer` 中的配置（它直接使用 `swerve::drive`，路徑跟蹤更準確），  
移除 `DriveSubsystem` 中多餘的呼叫，避免混淆。

---

### 修復 5 — `RobotContainer.java`

**問題**：Command 實例共用，可能導致排程衝突

| 項目 | 內容 |
|------|------|
| **檔案** | `src/main/java/frc/robot/RobotContainer.java` |
| **位置** | 欄位宣告與 `configureBindings()` |
| **嚴重程度** | 🟡 中 — 造成 Command 執行異常或意外取消 |

**修改前**：
```java
// 欄位：建立一個 Command 實例
private Command shoot_command = Commands.sequence(
    Commands.waitUntil(() -> shooterSubsystem.isAtSpeed(50)),
    transport.sys_runTransport()
);

// 同一個實例被兩處綁定
NamedCommands.registerCommand("transport wait shoot", shoot_command); // Auto 用
driverController.rightTrigger(0.1).whileTrue(shoot_command);         // Teleop 用
```

**修改後**：
```java
// 工廠方法：每次呼叫建立全新的 Command 實例
private Command createShootCommand() {
    return Commands.sequence(
        Commands.waitUntil(() -> shooterSubsystem.isAtSpeed(50)),
        transport.sys_runTransport()
    );
}

// 各自拿到獨立的實例
NamedCommands.registerCommand("transport wait shoot", createShootCommand()); // Auto 用
driverController.rightTrigger(0.1).whileTrue(createShootCommand());          // Teleop 用
```

同樣的修改套用到 `auto_shoot_command`、`far_auto_shoot_command`、`auto_intake_command`。

**為什麼要這樣做**：

WPILib 的 `Command` 是**有狀態的物件**，每個實例在 `CommandScheduler` 中只能有一個排程狀態（未啟動 / 執行中 / 已結束）。  

當同一個實例同時被 `NamedCommands`（自動模式）和 `Trigger`（手動模式）持有時：

```
問題情境：
1. 自動模式啟動 shoot_command（實例狀態 → 執行中）
2. teleopInit 切換到手動，Trigger 系統嘗試再次啟動同一個 shoot_command
3. CommandScheduler 發現它已在執行，對其發出 interrupt
4. Command 被意外取消 → 球沒有發射出去
```

改用工廠方法後，Auto 和 Teleop 各自持有**完全獨立的實例**，互不影響。

---

## 修復效果總結

| # | 問題 | 修改檔案 | 預期改善 |
|---|------|----------|----------|
| 1 | `flush()` 阻塞主迴圈 | `Swerve.java` | **消除最主要的 overrun 來源** |
| 2 | 重複 CAN 讀取 ×8 次/迴圈 | `SwerveModule.java` | 降低 CAN Bus 負載 |
| 3 | LED 每 20ms 更新 | `LightPollution.java` | 減少 2/3 的 LED 傳輸開銷 |
| 4 | `AutoBuilder` 重複設定 | `DriveSubsystem.java` | 自動路徑行為穩定 |
| 5 | Command 實例共用 | `RobotContainer.java` | 消除排程衝突 |

**建置結果**：`BUILD SUCCESSFUL` ✅（無新增錯誤，僅原有的 deprecation warnings）

---

## 補充：Loop Overrun 診斷方式

若未來再次出現 overrun，可透過以下方式定位：

1. **Driver Station Log Viewer**：開啟 `.dslog` 檔，找 "loop overrun" 時間點，對照執行的 Command
2. **SmartDashboard / Shuffleboard**：將可疑的 subsystem `putData()` 到儀表板，觀察哪個 `periodic()` 花最多時間
3. **Phoenix Tuner X**：檢查 CAN Bus 利用率，若超過 50% 就需要優化讀取頻率
4. **WPILib Profiling**：使用 `Tracer` 類別在各段程式碼前後記錄時間戳

```java
// 例：在 Swerve.periodic() 中加入追蹤
edu.wpi.first.wpilibj.Tracer.resetTimer();
// ... 你的程式碼 ...
edu.wpi.first.wpilibj.Tracer.printEpochs(); // 印出各段耗時
```
