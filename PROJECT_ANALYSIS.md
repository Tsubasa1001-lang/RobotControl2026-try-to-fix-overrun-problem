# RobotControl2026 專案完整分析報告

**最後更新**：2026-02-26

## 📋 專案概覽

| 項目 | 說明 |
|------|------|
| **專案類型** | FRC (FIRST Robotics Competition) 機器人控制程式 |
| **框架** | WPILib TimedRobot + Command-Based 架構 |
| **語言** | Java |
| **建置工具** | Gradle (GradleRIO 2026.2.1) |
| **底盤類型** | MK4i L3 Swerve Drive（全向輪底盤） |
| **馬達控制器** | CTRE TalonFX (Kraken X60 / X44) |
| **視覺系統** | Limelight（AprilTag MegaTag2 追蹤） |
| **路徑規劃** | PathPlanner 2026.1.2 |
| **IMU** | CTRE Pigeon2 |

---

## 🏗️ 專案結構

```
src/main/java/frc/robot/
├── Main.java                    # 程式進入點
├── Robot.java                   # TimedRobot 主類別 (20ms 週期迴圈) + Loop 計時監控
├── RobotContainer.java          # 指令綁定、子系統初始化、自動模式選擇
├── Constants.java               # 全域常數定義 (CAN ID、PID 參數、機械常數、AutoAim 參數)
├── LimelightHelpers.java        # Limelight 視覺輔助工具類別 (v1.11)
├── commands/
│   ├── ManualDrive.java         # 手動搖桿駕駛指令
│   ├── Drive2Tag.java           # 自動對位 AprilTag 指令 (Robot-relative)
│   └── AutoAimAndShoot.java     # 🆕 自動瞄準射擊指令 (Field-relative，依距離調速)
└── subsystems/
    ├── Swerve.java              # Swerve 底盤子系統（核心）+ Limelight 融合
    ├── SwerveModule.java        # Swerve 模組抽象類別（已優化 CAN 讀取）
    ├── SwerveModuleKraken.java  # Kraken X60 馬達實作（目前使用）+ 模擬物理
    ├── SwerveModuleNeo.java     # NEO 馬達實作（備用）
    ├── DriveSubsystem.java      # 已清空（AutoBuilder 統一在 RobotContainer 管理）
    ├── ShooterSubsystem.java    # 射球飛輪子系統 (VelocityVoltage)
    ├── IntakeArmSubsystem.java  # Intake 手臂子系統（目前未啟用）
    ├── IntakeRollerSubsystem.java # 🔄 Intake 滾輪子系統（已改為 VelocityVoltage 閉環控制）
    ├── TransportSubsystem.java  # 輸送帶子系統
    ├── RoboArm.java             # 機器手臂子系統（空殼）
    ├── LightPollution.java      # LED 燈條子系統（已降頻至 60ms）
    └── AutoAim.java             # 舊版自動瞄準（已全部註解，由 AutoAimAndShoot 取代）
```

---

## 🔧 子系統詳細說明

### 1. Swerve 底盤 (`Swerve.java`)
- **核心功能**：4 模組全向輪底盤控制
- **模組配置**：使用 `SwerveModuleKraken` (TalonFX on DRIVETRAIN CAN bus)
- **位置追蹤**：
  - 使用 `SwerveDrivePoseEstimator`（當 Limelight 啟用時）
  - 使用 `SwerveDriveOdometry`（無 Limelight 時）
- **速度控制**：SlewRateLimiter 加速度限制 (平移 20 m/s², 旋轉 15 rad/s²)
- **最大速度**：`kMaxPhysicalSpeedMps ≈ 5.13 m/s`（由齒輪比與輪徑計算：`(6000/60/6.12) × 0.1 × π`）
- **IMU**：Pigeon2 (CAN ID: 0)
- **CAN Bus**：`"DRIVETRAIN"`
- **底盤尺寸**：0.62865m × 0.62865m
- **Limelight 融合**：MegaTag2 AprilTag，XY 標準差 0.5m，**角度不融合**（stddev=999999，僅信任 IMU）
- **IMU 同步**：`resetPose()` 和 `resetIMU()` 會同時同步 Pigeon2 yaw 與 poseEstimator
- **Limelight 校正**：`resetPoseToLimelight()` 只校正 XY 位置，角度保持 IMU
- **模擬支援**：sim gyro 積分、Field2d 顯示

### 2. Swerve 模組 (`SwerveModule.java` / `SwerveModuleKraken.java`)
- **轉向控制**：使用 `ProfiledPIDController` (kP=0.005, kI=0.001, kD=0.0001)
- **編碼器**：CANcoder 絕對位置編碼器（DRIVETRAIN bus）
- **齒輪比**：
  - Throttle: 6.12:1 (MK4i L3 Very Fast)
  - Rotor: 150/7:1
- **輪徑**：0.1m
- **優化**：CAN 讀取快取（每次 setState 只讀一次 CANcoder）
- **模擬**：simThrottlePosition/Velocity 追蹤、simRotorPosition 追蹤

### 3. 射球飛輪 (`ShooterSubsystem.java`)
- **馬達**：TalonFX (CAN ID: 22)
- **控制方式**：VelocityVoltage 閉環速度控制
- **PID**：kV=0.12, kP=0.12
- **目標速度**：50 RPS（近距離）/ 62 RPS（遠距離）/ 動態（AutoAimAndShoot 依距離調整）
- **速度判斷容差**：5 RPS
- **新增方法**：`setTargetVelocity(rps)`, `stopShooter()`, `getCurrentRps()`

### 4. Intake 滾輪 (`IntakeRollerSubsystem.java`) 🔄 已重寫
- **馬達**：X44 TalonFX x2 (CAN ID: 29 Leader, 35 Follower)
- **控制方式**：~~DutyCycleOut 40%~~ → **VelocityVoltage 閉環轉速控制**
- **目標轉速**：吸球 60 RPS / 吐球 -30 RPS
- **PID**：kV=0.12, kP=0.2, kI=0.01, kD=0
- **電流限制**：Stator 60A, Supply 40A
- **優點**：遇到阻力時 PID 自動補償電壓維持轉速
- **Follower**：反轉跟隨 (Opposed)
- **Dashboard 監控**：`Intake/Actual RPS`, `Intake/Leader Current`

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
| **左搖桿** | 底盤平移控制 (X/Y)，乘以 `kMaxPhysicalSpeedMps` |
| **右搖桿 X** | 底盤旋轉控制，乘以 `kMaxPhysicalSpeedMps × Z_MULTIPLIER(-0.4)` |
| **右搖桿按下** | 切換場地導向/機器導向模式 |
| **Right Bumper** (按住) | 加速模式 (100% vs 50% 平移速度) |
| **Left Bumper** (按住) | 🆕 自動瞄準射擊 (AutoAimAndShoot) |
| **Menu 按鈕 (8)** | 重置 IMU（同步 poseEstimator） |
| **A 鍵** (按住) | Drive2Tag 自動對位 (正前方 1.15m) |
| **B 鍵** (按住) | 輸送帶反轉（吐球） |
| **X 鍵** (按住) | 輸送帶正轉 |
| **右板機** (按住) | 射球飛輪 (50 RPS) + 達速後自動推球 |
| **左板機** (按住) | Intake 滾輪吸球 (VelocityVoltage 60 RPS) |

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

## ⚠️ 已修復問題與優化紀錄

> 詳細修復過程請見 [OVERRUN_FIX_LOG.md](./OVERRUN_FIX_LOG.md)

### ✅ Loop Overrun 修復（5 項）

| # | 問題 | 修改檔案 | 狀態 |
|---|------|----------|------|
| 1 | `SetRobotOrientation()` flush 阻塞主迴圈 | `Swerve.java` | ✅ 改用 `_NoFlush` |
| 2 | `setState()` 重複讀取 CAN (12次/迴圈) | `SwerveModule.java` | ✅ 快取角度值 |
| 3 | LED 每 20ms 更新 | `LightPollution.java` | ✅ 降頻至 60ms |
| 4 | `AutoBuilder.configure()` 重複呼叫 | `DriveSubsystem.java` | ✅ 移除重複 |
| 5 | Command 實例共用導致排程衝突 | `RobotContainer.java` | ✅ 改用工廠方法 |

### ✅ 速度與加速度優化

| 修改 | 說明 |
|------|------|
| TrapezoidProfile → SlewRateLimiter | 原本 TrapezoidProfile 誤用（把速度當位置控制），導致提前減速。改用 SlewRateLimiter(20, 20, 15) |
| 統一最大速度常數 | 所有 `×6.0` 硬編碼改為 `kMaxPhysicalSpeedMps ≈ 5.13 m/s`（由齒輪比計算） |
| `getMaxVelocity()` 修正 | 從硬編碼 2 m/s 改為 `kMaxPhysicalSpeedMps` |
| SwerveModuleKraken 同步 | `setThrottleSpeed()` 使用 `kMaxPhysicalSpeedMps` 計算百分比 |

### ✅ IMU 與位姿同步

| 修改 | 說明 |
|------|------|
| `resetPose()` 同步 IMU | PathPlanner auto 開始時會同步設定 Pigeon2 yaw，auto 結束後角度自動正確 |
| `resetIMU()` 同步 poseEstimator | 手動重設 IMU 時同時更新 poseEstimator 航向 |
| `resetPoseToLimelight()` 只校正 XY | Limelight 只用來修正 XY 位置，角度完全由 IMU 決定 |
| `teleopInit()` 自動校正 | 進入 teleop 時自動呼叫 `resetPoseToLimelight()` 校正 XY 位置 |

### ✅ 新增功能

| 功能 | 說明 |
|------|------|
| **AutoAimAndShoot** | 自動旋轉面向 Speaker + 依距離調整射手 RPS + 對準達速後自動送球 |
| **Intake 閉環控制** | X44 馬達改為 VelocityVoltage 控制，遇阻力自動補償電壓 |
| **模擬支援** | sim gyro、sim motor physics、Field2d 顯示 |
| **Loop 計時監控** | `Robot.java` 記錄每次迴圈時間並顯示在 Dashboard |

### ✅ 其他修正

| 修改 | 說明 |
|------|------|
| `RuntimeException` → `DriverStation.reportWarning` | SwerveModuleKraken 初始化失敗不再直接崩潰 |
| `build.gradle` | `includeDesktopSupport = true`（啟用模擬器） |

---

## 📐 機器人物理參數

| 參數 | 值 |
|------|------|
| 底盤軌距 | 0.62865 m |
| 輪距 | 0.62865 m |
| 輪徑 | 0.1 m |
| 齒輪比 (Throttle) | 6.12:1 (L3 Very Fast) |
| 齒輪比 (Rotor) | 150/7:1 |
| 最大物理速度 | **5.13 m/s** (`(6000/60/6.12) × 0.1 × π`) |
| 平移加速度限制 | 20 m/s² (SlewRateLimiter) |
| 旋轉加速度限制 | 15 rad/s² (SlewRateLimiter) |
| Boost 模式速度 | 100% (5.13 m/s) |
| 一般模式速度 | 50% (2.57 m/s) |

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
| Intake Roller Leader (X44) | 29 | 預設 |
| Transport | 30 | 預設 |
| Intake Roller Follower (X44) | 35 | 預設 |

---

## 🆕 AutoAimAndShoot 功能說明

### 工作原理
1. **位置感知**：使用 `swerve.getPose()` 取得場地座標中的機器人位置
2. **角度計算**：`atan2(toTarget.Y, toTarget.X)` 計算面向 Speaker 所需角度
3. **PID 旋轉**：透過 `setAimSpeed()` 疊加旋轉速度（不影響手動平移控制）
4. **距離查表**：依距離線性內插取得目標射手 RPS
5. **自動發射**：角度對齊 (±2°) + 射手達速 → 啟動 Transport 送球

### 距離-RPS 對照表

| 距離 (m) | 目標 RPS |
|----------|----------|
| 1.0 | 40 |
| 2.0 | 50 |
| 3.0 | 60 |
| 4.0 | 70 |
| 5.0 | 80 |

### 按鍵綁定
- **Left Bumper** (按住) → 啟動自動瞄準射擊
- 放開即停止所有動作（旋轉、射手、Transport）

### Constants 設定 (`AutoAimConstants`)
- 藍方 Speaker: `(0.0, 5.55)`
- 紅方 Speaker: `(16.54, 5.55)`
- 旋轉 PID: kP=5.0, kI=0, kD=0.1
- 角度容差: 2°
- 射手速度容差: 5 RPS

---

*報告產生日期：2026-02-26*
