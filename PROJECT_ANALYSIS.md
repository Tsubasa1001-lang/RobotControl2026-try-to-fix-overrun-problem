# RobotControl2026 專案完整分析報告

## 📋 專案概覽

| 項目 | 說明 |
|------|------|
| **專案類型** | FRC (FIRST Robotics Competition) 機器人控制程式 |
| **框架** | WPILib TimedRobot + Command-Based 架構 |
| **語言** | Java |
| **建置工具** | Gradle (GradleRIO) |
| **底盤類型** | MK4i Swerve Drive（全向輪底盤） |
| **馬達控制器** | CTRE TalonFX (Kraken X60) + REV SparkMax (NEO) |
| **視覺系統** | Limelight（AprilTag 追蹤） |
| **路徑規劃** | PathPlanner 2026.1.2 |
| **IMU** | CTRE Pigeon2 |

---

## 🏗️ 專案結構

```
src/main/java/frc/robot/
├── Main.java                    # 程式進入點
├── Robot.java                   # TimedRobot 主類別 (20ms 週期迴圈)
├── RobotContainer.java          # 指令綁定、子系統初始化、自動模式選擇
├── Constants.java               # 全域常數定義 (CAN ID、PID 參數、機械常數)
├── LimelightHelpers.java        # Limelight 視覺輔助工具類別 (v1.11)
├── commands/
│   ├── ManualDrive.java         # 手動搖桿駕駛指令
│   └── Drive2Tag.java           # 自動對位 AprilTag 指令
└── subsystems/
    ├── Swerve.java              # Swerve 底盤子系統（核心）
    ├── SwerveModule.java        # Swerve 模組抽象類別
    ├── SwerveModuleKraken.java  # Kraken X60 馬達實作（目前使用）
    ├── SwerveModuleNeo.java     # NEO 馬達實作（備用）
    ├── DriveSubsystem.java      # PathPlanner AutoBuilder 配置（有重複問題）
    ├── ShooterSubsystem.java    # 射球飛輪子系統
    ├── IntakeArmSubsystem.java  # Intake 手臂子系統（目前未啟用）
    ├── IntakeRollerSubsystem.java # Intake 滾輪子系統
    ├── TransportSubsystem.java  # 輸送帶子系統
    ├── RoboArm.java             # 機器手臂子系統（空殼）
    ├── LightPollution.java      # LED 燈條子系統
    └── AutoAim.java             # 自動瞄準（已全部註解）
```

---

## 🔧 子系統詳細說明

### 1. Swerve 底盤 (`Swerve.java`)
- **核心功能**：4 模組全向輪底盤控制
- **模組配置**：使用 `SwerveModuleKraken` (TalonFX on DRIVETRAIN CAN bus)
- **位置追蹤**：
  - 使用 `SwerveDrivePoseEstimator`（當 Limelight 啟用時）
  - 使用 `SwerveDriveOdometry`（無 Limelight 時）
- **速度控制**：TrapezoidProfile 加速度限制 (平移 10m/s², 旋轉 5rad/s²)
- **最大速度**：2 m/s
- **IMU**：Pigeon2 (CAN ID: 0)
- **CAN Bus**：`"DRIVETRAIN"`
- **底盤尺寸**：0.62865m × 0.62865m

### 2. Swerve 模組 (`SwerveModule.java` / `SwerveModuleKraken.java`)
- **轉向控制**：使用 `ProfiledPIDController` (kP=0.005, kI=0.001, kD=0.0001)
- **編碼器**：CANcoder 絕對位置編碼器（DRIVETRAIN bus）
- **齒輪比**：
  - Throttle: 6.12:1 (MK4i L3 Very Fast)
  - Rotor: 150/7:1
- **輪徑**：0.1m

### 3. 射球飛輪 (`ShooterSubsystem.java`)
- **馬達**：TalonFX (CAN ID: 22)
- **控制方式**：VelocityVoltage 閉環速度控制
- **PID**：kV=0.12, kP=0.12
- **目標速度**：50 RPS（近距離）/ 62 RPS（遠距離）
- **速度判斷容差**：5 RPS

### 4. Intake 滾輪 (`IntakeRollerSubsystem.java`)
- **馬達**：TalonFX x2 (CAN ID: 29 Leader, 35 Follower)
- **控制方式**：DutyCycleOut 百分比控制
- **最大輸出**：40%
- **電流限制**：40A (Stator)
- **Follower**：反轉跟隨 (Opposed)

### 5. 輸送帶 (`TransportSubsystem.java`)
- **馬達**：TalonFX x2 (CAN ID: 26 推球, 30 輸送)
- **輸送速度**：40% / 慢速 20%
- **推球速度**：100%
- **電流限制**：Stator 60A, Supply 40A

### 6. Intake 手臂 (`IntakeArmSubsystem.java`)（目前未啟用）
- **馬達**：TalonFX x2 (CAN ID: 3 Leader, 4 Follower)
- **控制方式**：PositionVoltage 位置控制
- **減速比**：20:1
- **軟體限位**：0 ~ 8 圈

### 7. LED 燈條 (`LightPollution.java`)
- **類型**：AddressableLED
- **模式**：彩虹滾動、固定色、閃爍

---

## 🎮 操控器按鍵配置

| 按鍵 | 功能 |
|------|------|
| **左搖桿** | 底盤平移控制 (X/Y) |
| **右搖桿 X** | 底盤旋轉控制 |
| **右搖桿按下** | 切換場地導向/機器導向模式 |
| **Right Bumper** (按住) | 加速模式 (100% vs 50%) |
| **Menu 按鈕 (8)** | 重置 IMU |
| **A 鍵** (按住) | Drive2Tag 自動對位 (正中) |
| **B 鍵** (按住) | 輸送帶反轉（吐球） |
| **X 鍵** (按住) | 輸送帶正轉 |
| **右板機** (按住) | 射球飛輪 + 自動推球 |
| **左板機** (按住) | Intake 滾輪吸球 |

---

## 🤖 自動模式

- 使用 PathPlanner 的 `AutoBuilder` + `autoChooser`
- 已註冊的 NamedCommands：
  - `"Auto Shoot"` — 飛輪啟動 + 等速度到達後推球 (50 RPS)
  - `"Far Auto Shoot"` — 遠距射擊 (62 RPS)
  - `"Auto Intake"` — 同時吸球 + 慢速輸送
  - `"Start Intake"` / `"Stop Intake"` — 控制吸球開關
  - `"shoot work"` / `"transport wait shoot"` — 射擊相關

---

## ⚠️ 已知問題與延遲分析

### 🔴 嚴重問題（可能導致 Loop Overrun 的根因）

---

### 問題 1：`Swerve.periodic()` 中 Limelight 的 NetworkTables Flush 操作

**位置**：`Swerve.java` 第 162 行

```java
LimelightHelpers.SetRobotOrientation(limelightName, 
    gyroAngle.getDegrees(), getGyroRateDps(), 0, 0, 0, 0);
```

**問題分析**：
- `SetRobotOrientation()` 內部呼叫 `SetRobotOrientation_INTERNAL()` 並帶有 `flush = true`
- 這會觸發 `NetworkTableInstance.getDefault().flush()`
- **`flush()` 是一個阻塞（blocking）操作**，它會強制將所有 NetworkTables 數據立即發送出去
- 這個操作在**每一個 20ms 的 periodic 迴圈**中都會執行
- 當網路延遲較高或 Limelight 回應慢時，flush 會造成迴圈超時 (overrun)

**嚴重程度**：🔴 **高 — 這是最可能導致 overrun 的主因**

**建議修復**：
```java
// 改用 NoFlush 版本，避免每次 periodic 都阻塞
LimelightHelpers.SetRobotOrientation_NoFlush(limelightName, 
    gyroAngle.getDegrees(), getGyroRateDps(), 0, 0, 0, 0);
```

---

### 問題 2：`Swerve.periodic()` 中大量 CAN Bus 讀取操作

**位置**：`Swerve.java` 第 155-196 行

**問題分析**：
每次 `periodic()` 執行時，會觸發以下 CAN 讀取鏈：

1. `getGyroAngle()` → `mPigeonIMU.getRotation2d()` (1 次 CAN 讀取)
2. `getGyroRateDps()` → `mPigeonIMU.getAngularVelocityZDevice().getValueAsDouble()` (1 次 CAN 讀取)
3. `getModulePositions()` → 4 個模組各讀取 2 次 (CANcoder + TalonFX) = **8 次 CAN 讀取**
4. `poseEstimator.update()` 內部又會存取模組狀態
5. `LimelightHelpers.getBotPoseEstimate_wpiBlue()` → NetworkTables 讀取

**總計：每次 periodic 迴圈約 12+ 次 CAN Bus 通訊 + NetworkTables 操作**

**嚴重程度**：🟡 **中 — CAN Bus 堵塞會累積延遲**

**建議修復**：
- 使用 Phoenix6 的 `BaseStatusSignal.refreshAll()` 批量刷新
- 或使用 `waitForAll()` 在迴圈開頭一次性讀取所有感測器

---

### 問題 3：`LightPollution.periodic()` 每次迴圈都更新 LED

**位置**：`LightPollution.java` 第 68-72 行

```java
@Override
public void periodic() {
    ledPattern.applyTo(buffer);
    led.setData(buffer);
    super.periodic();
}
```

**問題分析**：
- `led.setData(buffer)` 每 20ms 都會將完整 LED 資料透過 DIO 傳送
- 如果 LED 數量多（例如 > 60 顆），資料傳輸本身就會消耗可觀時間
- `ledPattern.applyTo(buffer)` 對滾動彩虹等動態模式會做額外數學運算
- 雖然單次時間不長，但會**疊加**到已經很忙的 periodic 迴圈中

**嚴重程度**：🟡 **中 — 會額外增加迴圈時間**

**建議修復**：降低 LED 更新頻率
```java
private int ledCounter = 0;
@Override
public void periodic() {
    if (++ledCounter >= 3) { // 每 60ms 才更新一次
        ledPattern.applyTo(buffer);
        led.setData(buffer);
        ledCounter = 0;
    }
}
```

---

### 問題 4：`AutoBuilder.configure()` 被呼叫了兩次

**位置**：
- `RobotContainer.java` 第 126 行
- `DriveSubsystem.java` 第 21 行

**問題分析**：
- `RobotContainer` 建構子中先 `new DriveSubsystem(swerve)`（裡面呼叫了 `AutoBuilder.configure()`）
- 然後 `RobotContainer` 建構子自己又呼叫了一次 `AutoBuilder.configure()`
- 兩次配置的 PID 參數和回調函數**不同**，可能導致自動模式行為不可預測
- `DriveSubsystem` 用的是 `Constants.AutoConstants` 的 PID
- `RobotContainer` 用的是硬編碼的 `PIDConstants(3.0, 0, 0)` 和 `PIDConstants(1.8, 0, 0)`
- 兩者還注冊了不同的 subsystem requirement（一個是 `DriveSubsystem`，一個是 `Swerve`）

**嚴重程度**：🟠 **中高 — 行為不可預測，不會直接造成延遲但會造成路徑跟蹤異常**

**建議修復**：刪除其中一個 `AutoBuilder.configure()`，保留 `RobotContainer` 中的即可，並移除 `DriveSubsystem` 類別或清空其建構子。

---

### 問題 5：`SwerveModule.setState()` 中重複呼叫 `getState()`

**位置**：`SwerveModule.java` 第 117-130 行

```java
public void setState(SwerveModuleState state) {
    state.optimize(getState().angle);      // 第 1 次 getState()
    state.cosineScale(getState().angle);   // 第 2 次 getState()
    runRotorPID(state.angle.getDegrees()); // 內部又呼叫 getState()
    ...
}
```

**問題分析**：
- `getState()` 每次呼叫都會讀取 CANcoder（CAN 通訊）
- 一次 `setState()` 至少觸發 **3 次** CAN 讀取
- 4 個模組 = 每個 periodic 迴圈額外 **12 次不必要的 CAN 讀取**

**嚴重程度**：🟠 **中高 — 直接加倍 CAN Bus 負載**

**建議修復**：快取角度值
```java
public void setState(SwerveModuleState state) {
    Rotation2d currentAngle = getState().angle; // 只讀一次
    state.optimize(currentAngle);
    state.cosineScale(currentAngle);
    runRotorPID(state.angle.getDegrees());
    setThrottleSpeed(state.speedMetersPerSecond);
}
```

---

### 問題 6：`runRotorPID()` 內部再次呼叫 `getState()`

**位置**：`SwerveModule.java` 第 139-142 行

```java
private void runRotorPID(double angle) {
    double rotorOutput = mRotorPID.calculate(getState().angle.getDegrees(), angle);
    setRotorSpeed(mRotorPID.atSetpoint() ? 0 : rotorOutput);
}
```

**問題分析**：
- 這裡又再讀取一次 `getState().angle`
- 結合問題 5，每個模組每次設定狀態就有 **3-4 次** CAN 讀取
- 應該將角度值作為參數傳入

**嚴重程度**：🟠 **中高 — 與問題 5 疊加**

---

### 問題 7：`RobotContainer` 中 Command 共用問題

**位置**：`RobotContainer.java` 第 93-96 行 + 第 226-230 行

```java
// 欄位宣告中建立的 Command
private Command shoot_command = Commands.sequence(...);

// 又在 configureBindings 中重複使用
driverController.rightTrigger(0.1).whileTrue(shoot_command);
```

同時在 `NamedCommands` 也註冊了同一個物件：
```java
NamedCommands.registerCommand("transport wait shoot", shoot_command);
```

**問題分析**：
- WPILib Command 是有狀態的物件，同一個 Command 實例**不能同時被多個 Trigger 或 NamedCommand 使用**
- 如果 Teleop 和 Auto 同時嘗試使用同一個 command 實例，會導致意外的排程衝突
- `auto_shoot_command`、`far_auto_shoot_command`、`auto_intake_command` 也有同樣問題

**嚴重程度**：🟡 **中 — 會導致 Command 執行異常**

---

## 📊 延遲來源摘要

| 優先級 | 問題 | 位置 | 影響 |
|--------|------|------|------|
| 🔴 P0 | `SetRobotOrientation()` 帶 flush 阻塞 | `Swerve.periodic()` | **最可能的 overrun 主因** |
| 🟠 P1 | `setState()` 重複讀取 CAN (12次/迴圈) | `SwerveModule.setState()` | CAN Bus 負載過重 |
| 🟠 P1 | `runRotorPID()` 額外 CAN 讀取 | `SwerveModule.runRotorPID()` | 疊加 CAN Bus 負載 |
| 🟡 P2 | LED 每 20ms 更新 | `LightPollution.periodic()` | 增加迴圈時間 |
| 🟡 P2 | `AutoBuilder.configure()` 重複呼叫 | `RobotContainer` + `DriveSubsystem` | 行為不可預測 |
| 🟡 P2 | Command 實例共用 | `RobotContainer` | 排程衝突 |

---

## 🛠️ 建議修復優先順序

### 立即修復 (P0)
1. **將 `SetRobotOrientation` 改為 `SetRobotOrientation_NoFlush`**
   - 檔案：`Swerve.java` 第 162 行
   - 預計效果：消除每次迴圈的 NetworkTables 阻塞等待

### 優先修復 (P1)
2. **快取 `SwerveModule.setState()` 中的角度讀取**
   - 檔案：`SwerveModule.java` 第 117-142 行
   - 預計效果：每個迴圈減少 ~12 次 CAN 讀取

3. **使用 Phoenix6 批量刷新 API**
   - 在 `Swerve.periodic()` 開頭使用 `BaseStatusSignal.refreshAll()`
   - 預計效果：將多次 CAN 讀取合併為一次

### 建議修復 (P2)
4. **降低 LED 更新頻率**至每 50-100ms
5. **移除重複的 `AutoBuilder.configure()`**
6. **修正 Command 共用問題**，改用 factory method 每次建立新實例

---

## 📐 機器人物理參數

| 參數 | 值 |
|------|------|
| 底盤軌距 | 0.62865 m |
| 輪距 | 0.62865 m |
| 輪徑 | 0.1 m |
| 齒輪比 (Throttle) | 6.12:1 (L3 Very Fast) |
| 齒輪比 (Rotor) | 150/7:1 |
| 最大速度 | 2 m/s |
| 電壓補償 | 12V |

---

## 📡 CAN Bus 配置

| 裝置 | CAN ID | Bus |
|------|--------|-----|
| Pigeon2 IMU | 0 | 預設 |
| 左後 Rotor | 1 | DRIVETRAIN |
| 左後 Throttle | 2 | DRIVETRAIN |
| 左前 Rotor | 3 | DRIVETRAIN |
| 左前 Throttle | 4 | DRIVETRAIN |
| 右前 Rotor | 5 | DRIVETRAIN |
| 右前 Throttle | 6 | DRIVETRAIN |
| 右後 Rotor | 7 | DRIVETRAIN |
| 右後 Throttle | 8 | DRIVETRAIN |
| 左前 CANcoder | 12 | DRIVETRAIN |
| 右前 CANcoder | 13 | DRIVETRAIN |
| 左後 CANcoder | 11 | DRIVETRAIN |
| 右後 CANcoder | 14 | DRIVETRAIN |
| Shooter | 22 | 預設 |
| Transport (up_to_shoot) | 26 | 預設 |
| Intake Roller Leader | 29 | 預設 |
| Transport | 30 | 預設 |
| Intake Roller Follower | 35 | 預設 |

---

*報告產生日期：2026-02-26*
