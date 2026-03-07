# RobotControl2026 專案完整分析報告# RobotControl2026 專案完整分析報告# RobotControl2026 專案完整分析報告



**最後更新**：2026-03-07

**比賽年度**：2026 FRC — **REBUILT℠** presented by Haas

**遊戲物件**：Fuel（燃料球）**最後更新**：2026-02-27  **最後更新**：2026-02-26

**得分目標**：Hub（場地中央的六角形得分結構）

**場地元素**：Hub、Tower Wall、Outpost、Trenches、Barge、Cage**比賽年度**：2026 FRC — **REEFSCAPE℠** presented by Haas  



> ⚠️ Hub 位置在場地中央區域，**不在牆壁邊！** 座標由官方 AprilTag JSON 計算。**遊戲物件**：Coral（珊瑚）& Algae（藻類）  ## 📋 專案概覽



---**得分目標**：Reef（珊瑚架 L1-L4）、Processor（藻類處理器）、Net（高處網）、Barge（停泊區）  



## 📋 專案概覽| 項目 | 說明 |



| 項目 | 說明 |> ⚠️ **注意**：本程式碼中的「Hub」/「Speaker」變數名稱源自早期開發，目前正適配 2026 場地。|------|------|

|------|------|

| **專案類型** | FRC (FIRST Robotics Competition) 機器人控制程式 |> 場地座標 (`kBlueSpeakerX/Y`, `kRedSpeakerX/Y`) 需根據 2026 實際場地重新量測。| **專案類型** | FRC (FIRST Robotics Competition) 機器人控制程式 |

| **框架** | WPILib TimedRobot + Command-Based 架構 |

| **語言** | Java || **框架** | WPILib TimedRobot + Command-Based 架構 |

| **建置工具** | Gradle (GradleRIO 2026.2.1) |

| **底盤類型** | MK4i L3 Swerve Drive（全向輪底盤） |---| **語言** | Java |

| **馬達控制器** | CTRE TalonFX (Kraken X60 / X44) — Phoenix 6 |

| **視覺系統** | Limelight（AprilTag MegaTag2 追蹤） || **建置工具** | Gradle (GradleRIO 2026.2.1) |

| **路徑規劃** | PathPlanner 2026.1.2 |

| **IMU** | CTRE Pigeon2 |## 📋 專案概覽| **底盤類型** | MK4i L3 Swerve Drive（全向輪底盤） |

| **Dashboard** | Shuffleboard 六分頁 + TunableNumber 即時調參 |

| **AprilTag** | 36h11 family, 32 tags, 位於 Hub / Tower Wall / Outpost / Trenches || **馬達控制器** | CTRE TalonFX (Kraken X60 / X44) |



---| 項目 | 說明 || **視覺系統** | Limelight（AprilTag MegaTag2 追蹤） |



## 🏗️ 專案結構|------|------|| **路徑規劃** | PathPlanner 2026.1.2 |



```| **專案類型** | FRC (FIRST Robotics Competition) 機器人控制程式 || **IMU** | CTRE Pigeon2 |

src/main/java/frc/robot/

├── Main.java                    # 程式進入點| **框架** | WPILib TimedRobot + Command-Based 架構 |

├── Robot.java                   # TimedRobot 主類別 (20ms 週期迴圈) + Loop 計時監控

├── RobotContainer.java          # 指令綁定、子系統初始化、自動模式選擇| **語言** | Java |---

├── Constants.java               # 全域常數 (CAN ID、PID、Hub 座標、遲滯參數)

├── LimelightHelpers.java        # Limelight 視覺輔助工具類別 (v1.11)| **建置工具** | Gradle (GradleRIO 2026.2.1) |

├── commands/

│   ├── ManualDrive.java         # 手動搖桿駕駛 + 射擊模式（速度抑制 + 旋轉鎖定）| **底盤類型** | MK4i L3 Swerve Drive（全向輪底盤） |## 🏗️ 專案結構

│   ├── Drive2Tag.java           # 自動對位 AprilTag 指令 (Robot-relative)

│   └── AutoAimAndShoot.java     # 自動瞄準射擊 — 區域偵測 + 雙目標 + 遲滯送球| **馬達控制器** | CTRE TalonFX (Kraken X60 / X44) — Phoenix 6 |

└── subsystems/

    ├── Swerve.java              # Swerve 底盤核心 + Limelight 融合 + 速度疊加| **視覺系統** | Limelight（AprilTag MegaTag2 追蹤） |```

    ├── SwerveModule.java        # Swerve 模組抽象類別（CAN 快取優化）

    ├── SwerveModuleKraken.java  # Kraken X60 實作（目前使用）+ 模擬物理| **路徑規劃** | PathPlanner 2026.1.2 |src/main/java/frc/robot/

    ├── SwerveModuleNeo.java     # NEO 馬達實作（備用）

    ├── DriveSubsystem.java      # 已清空（AutoBuilder 統一在 RobotContainer）| **IMU** | CTRE Pigeon2 |├── Main.java                    # 程式進入點

    ├── ShooterSubsystem.java    # 射球飛輪 (VelocityVoltage 閉環)

    ├── IntakeArmSubsystem.java  # Intake 手臂（⚠️ 未啟用，CAN ID 衝突）| **Dashboard** | Shuffleboard 六分頁 + TunableNumber 即時調參 |├── Robot.java                   # TimedRobot 主類別 (20ms 週期迴圈) + Loop 計時監控

    ├── IntakeRollerSubsystem.java # Intake 滾輪 (VelocityVoltage 閉環)

    ├── TransportSubsystem.java  # 輸送帶 + 推球 (三段式控制)├── RobotContainer.java          # 指令綁定、子系統初始化、自動模式選擇

    ├── RoboArm.java             # 機器手臂（空殼）

    ├── LightPollution.java      # LED 燈條（降頻至 60ms）---├── Constants.java               # 全域常數定義 (CAN ID、PID 參數、機械常數、AutoAim 參數)

    ├── AutoAim.java             # 舊版自動瞄準（已全部註解）

    ├── ShuffleboardManager.java # Shuffleboard 六分頁管理├── LimelightHelpers.java        # Limelight 視覺輔助工具類別 (v1.11)

    └── TunableNumber.java       # NetworkTables 即時可調參數工具

```## 🏗️ 專案結構├── commands/



---│   ├── ManualDrive.java         # 手動搖桿駕駛指令



## 🔧 子系統詳細說明```│   ├── Drive2Tag.java           # 自動對位 AprilTag 指令 (Robot-relative)



### 1. Swerve 底盤 (`Swerve.java`)src/main/java/frc/robot/│   └── AutoAimAndShoot.java     # 🆕 自動瞄準射擊指令 (Field-relative，依距離調速)



| 項目 | 說明 |├── Main.java                    # 程式進入點└── subsystems/

|------|------|

| **模組** | 4 × SwerveModuleKraken (TalonFX on `"DRIVETRAIN"` CAN bus) |├── Robot.java                   # TimedRobot 主類別 (20ms 週期迴圈) + Loop 計時監控    ├── Swerve.java              # Swerve 底盤子系統（核心）+ Limelight 融合

| **位置追蹤** | SwerveDrivePoseEstimator (Limelight 啟用) / SwerveDriveOdometry (無 Limelight) |

| **加速度限制** | SlewRateLimiter — 平移 20 m/s², 旋轉 15 rad/s² |├── RobotContainer.java          # 指令綁定、子系統初始化、自動模式選擇    ├── SwerveModule.java        # Swerve 模組抽象類別（已優化 CAN 讀取）

| **最大速度** | `kMaxPhysicalSpeedMps ≈ 5.13 m/s` (`(6000/60/6.12) × 0.1 × π`) |

| **IMU** | Pigeon2 (CAN ID: 0, 預設 bus) |├── Constants.java               # 全域常數定義 (CAN ID、PID、AutoAim、遲滯參數)    ├── SwerveModuleKraken.java  # Kraken X60 馬達實作（目前使用）+ 模擬物理

| **底盤尺寸** | 0.62865m × 0.62865m |

| **速度疊加** | `mTargetChassisSpeeds` (ManualDrive) + `mAimChassisSpeeds` (AutoAim) 合併於 `runSetStates()` |├── LimelightHelpers.java        # Limelight 視覺輔助工具類別 (v1.11)    ├── SwerveModuleNeo.java     # NEO 馬達實作（備用）

| **Limelight** | MegaTag2, XY stdDev 0.5m, 角度 stdDev 999999（僅信任 IMU） |

| **IMU 同步** | `resetPose()` / `resetIMU()` 同時更新 Pigeon2 yaw + poseEstimator |├── commands/    ├── DriveSubsystem.java      # 已清空（AutoBuilder 統一在 RobotContainer 管理）

| **模擬** | sim gyro 積分、Field2d 顯示 |

│   ├── ManualDrive.java         # 手動搖桿駕駛 + 射擊模式（速度抑制 + 旋轉鎖定）    ├── ShooterSubsystem.java    # 射球飛輪子系統 (VelocityVoltage)

### 2. Swerve 模組 (`SwerveModuleKraken.java`)

│   ├── Drive2Tag.java           # 自動對位 AprilTag 指令 (Robot-relative)    ├── IntakeArmSubsystem.java  # Intake 手臂子系統（目前未啟用）

| 項目 | 說明 |

|------|------|│   └── AutoAimAndShoot.java     # 自動瞄準射擊 — 區域偵測 + 雙目標 + 遲滯送球    ├── IntakeRollerSubsystem.java # 🔄 Intake 滾輪子系統（已改為 VelocityVoltage 閉環控制）

| **轉向 PID** | ProfiledPIDController (kP=0.005, kI=0.001, kD=0.0001) |

| **編碼器** | CANcoder 絕對位置 (DRIVETRAIN bus) |└── subsystems/    ├── TransportSubsystem.java  # 輸送帶子系統

| **Throttle 齒輪比** | 6.12:1 (MK4i L3 Very Fast) |

| **Rotor 齒輪比** | 150/7:1 |    ├── Swerve.java              # Swerve 底盤核心 + Limelight 融合 + 速度疊加    ├── RoboArm.java             # 機器手臂子系統（空殼）

| **輪徑** | 0.1m |

| **CAN 優化** | 每次 `setState()` 快取 CANcoder，避免重複讀取 |    ├── SwerveModule.java        # Swerve 模組抽象類別（CAN 快取優化）    ├── LightPollution.java      # LED 燈條子系統（已降頻至 60ms）



### 3. 射球飛輪 (`ShooterSubsystem.java`)    ├── SwerveModuleKraken.java  # Kraken X60 實作（目前使用）+ 模擬物理    └── AutoAim.java             # 舊版自動瞄準（已全部註解，由 AutoAimAndShoot 取代）



| 項目 | 說明 |    ├── SwerveModuleNeo.java     # NEO 馬達實作（備用）```

|------|------|

| **馬達** | TalonFX Leader (CAN 22) + Follower (CAN 21, 反轉跟隨) |    ├── DriveSubsystem.java      # 已清空（AutoBuilder 統一在 RobotContainer）

| **控制** | VelocityVoltage 閉環 (kV=0.12, kP=0.12) |

| **速度範圍** | 35~80 RPS（由 AutoAimAndShoot 依距離查表設定） |    ├── ShooterSubsystem.java    # 射球飛輪 (VelocityVoltage 閉環)---

| **容差** | ±3 RPS 視為「達速」 |

| **方法** | `setTargetVelocity(rps)`, `stopShooter()`, `getCurrentRps()`, `isAtTargetSpeed()` |    ├── IntakeArmSubsystem.java  # Intake 手臂（⚠️ 未啟用，CAN ID 衝突）



### 4. Intake 滾輪 (`IntakeRollerSubsystem.java`)    ├── IntakeRollerSubsystem.java # Intake 滾輪 (VelocityVoltage 閉環)## 🔧 子系統詳細說明



| 項目 | 說明 |    ├── TransportSubsystem.java  # 輸送帶 + 推球 (runTransportOnly / runUpToShoot / stopUpToShoot)

|------|------|

| **馬達** | X44 TalonFX ×2 (CAN 29 Leader, 35 Follower 反轉) |    ├── RoboArm.java             # 機器手臂（空殼）### 1. Swerve 底盤 (`Swerve.java`)

| **控制** | VelocityVoltage 閉環 (kV=0.12, kP=0.2, kI=0.01) |

| **轉速** | 吸球 60 RPS / 吐球 -30 RPS |    ├── LightPollution.java      # LED 燈條（降頻至 60ms）- **核心功能**：4 模組全向輪底盤控制

| **電流** | Stator 60A, Supply 40A |

    ├── AutoAim.java             # 舊版自動瞄準（已全部註解，由 AutoAimAndShoot 取代）- **模組配置**：使用 `SwerveModuleKraken` (TalonFX on DRIVETRAIN CAN bus)

### 5. 輸送帶 (`TransportSubsystem.java`)

    ├── ShuffleboardManager.java # Shuffleboard 六分頁管理- **位置追蹤**：

| 項目 | 說明 |

|------|------|    └── TunableNumber.java       # NetworkTables 即時可調參數工具  - 使用 `SwerveDrivePoseEstimator`（當 Limelight 啟用時）

| **馬達** | up_to_shoot (CAN 26, Brake), transport (CAN 30, Coast) |

| **控制** | DutyCycleOut |```  - 使用 `SwerveDriveOdometry`（無 Limelight 時）

| **方法** | `runTransportOnly()` — 僅輸送帶 40% |

| | `runUpToShoot()` — 推球馬達 100% |- **速度控制**：SlewRateLimiter 加速度限制 (平移 20 m/s², 旋轉 15 rad/s²)

| | `stopUpToShoot()` — 顯式停止推球馬達 |

| | `stopTransport()` — 停止全部 |---- **最大速度**：`kMaxPhysicalSpeedMps ≈ 5.13 m/s`（由齒輪比與輪徑計算：`(6000/60/6.12) × 0.1 × π`）



### 6. Intake 手臂 (`IntakeArmSubsystem.java`) — ⚠️ 未啟用- **IMU**：Pigeon2 (CAN ID: 0)



- TalonFX ×2 (CAN 3, 4) — **與 Swerve 左前模組 CAN ID 衝突！**## 🔧 子系統詳細說明- **CAN Bus**：`"DRIVETRAIN"`

- 目前未在 RobotContainer 實例化

- **底盤尺寸**：0.62865m × 0.62865m

### 7. LED 燈條 (`LightPollution.java`)

### 1. Swerve 底盤 (`Swerve.java`)- **Limelight 融合**：MegaTag2 AprilTag，XY 標準差 0.5m，**角度不融合**（stddev=999999，僅信任 IMU）

- AddressableLED，降頻至 60ms

- **IMU 同步**：`resetPose()` 和 `resetIMU()` 會同時同步 Pigeon2 yaw 與 poseEstimator

### 8. Shuffleboard 管理 (`ShuffleboardManager.java`)

| 項目 | 說明 |- **Limelight 校正**：`resetPoseToLimelight()` 只校正 XY 位置，角度保持 IMU

六個分頁：Swerve Debug / Shooter PID / Intake PID / AutoAim PID / Vision / Robot Status

|------|------|- **模擬支援**：sim gyro 積分、Field2d 顯示

---

| **模組** | 4 × SwerveModuleKraken (TalonFX on `"DRIVETRAIN"` CAN bus) |

## 🎮 操控器按鍵配置 (Xbox Controller)

| **位置追蹤** | SwerveDrivePoseEstimator (Limelight 啟用) / SwerveDriveOdometry (無 Limelight) |### 2. Swerve 模組 (`SwerveModule.java` / `SwerveModuleKraken.java`)

| 按鍵 | 功能 |

|------|------|| **加速度限制** | SlewRateLimiter — 平移 20 m/s², 旋轉 15 rad/s² |- **轉向控制**：使用 `ProfiledPIDController` (kP=0.005, kI=0.001, kD=0.0001)

| **左搖桿** | 底盤平移 (X/Y) × `kMaxPhysicalSpeedMps` |

| **右搖桿 X** | 底盤旋轉 × `kMaxPhysicalSpeedMps × Z_MULTIPLIER(-0.4)` || **最大速度** | `kMaxPhysicalSpeedMps ≈ 5.13 m/s` (`(6000/60/6.12) × 0.1 × π`) |- **編碼器**：CANcoder 絕對位置編碼器（DRIVETRAIN bus）

| **右搖桿按下** | 切換場地導向/機器導向模式 |

| **Right Bumper** (按住) | 加速模式 (100% vs 50% 平移速度) || **IMU** | Pigeon2 (CAN ID: 0, 預設 bus) |- **齒輪比**：

| **Left Bumper** (按住) | 🎯 自動瞄準射擊 (AutoAimAndShoot) |

| **Menu 按鈕 (8)** | 重置 IMU（同步 poseEstimator） || **底盤尺寸** | 0.62865m × 0.62865m |  - Throttle: 6.12:1 (MK4i L3 Very Fast)

| **A 鍵** (按住) | Drive2Tag 自動對位 (正前方 1.15m) |

| **B 鍵** (按住) | 輸送帶反轉（吐球） || **速度疊加** | `mTargetChassisSpeeds` (ManualDrive) + `mAimChassisSpeeds` (AutoAim) 合併於 `runSetStates()` |  - Rotor: 150/7:1

| **X 鍵** (按住) | 輸送帶正轉 |

| **右板機** (按住) | 手動射球：飛輪 50 RPS + 達速後自動推球 || **Limelight** | MegaTag2, XY stdDev 0.5m, 角度 stdDev 999999（僅信任 IMU） |- **輪徑**：0.1m

| **左板機** (按住) | Intake 滾輪吸球 (VelocityVoltage 60 RPS) |

| **IMU 同步** | `resetPose()` / `resetIMU()` 同時更新 Pigeon2 yaw + poseEstimator |- **優化**：CAN 讀取快取（每次 setState 只讀一次 CANcoder）

---

| **模擬** | sim gyro 積分、Field2d 顯示 |- **模擬**：simThrottlePosition/Velocity 追蹤、simRotorPosition 追蹤

## 🔫 AutoAimAndShoot 完整功能說明



### 工作原理

### 2. Swerve 模組 (`SwerveModuleKraken.java`)### 3. 射球飛輪 (`ShooterSubsystem.java`)

```

每 20ms 執行：- **馬達**：TalonFX (CAN ID: 22)

 1. 取得機器人座標 (Swerve.getPose())

 2. 判斷區域：| 項目 | 說明 |- **控制方式**：VelocityVoltage 閉環速度控制

    ├── 己方聯盟區 → 計算面向 Hub 的角度 (atan2)

    └── 中立區（中場）→ 使用固定角度朝向己方聯盟區|------|------|- **PID**：kV=0.12, kP=0.12

 3. 計算到 Hub 的距離

 4. PID 計算旋轉速度 → setAimSpeed() 疊加到底盤| **轉向 PID** | ProfiledPIDController (kP=0.005, kI=0.001, kD=0.0001) |- **目標速度**：50 RPS（近距離）/ 62 RPS（遠距離）/ 動態（AutoAimAndShoot 依距離調整）

 5. 根據區域選擇射手速度：

    ├── 己方聯盟區 → 依距離查表線性內插| **編碼器** | CANcoder 絕對位置 (DRIVETRAIN bus) |- **速度判斷容差**：5 RPS

    └── 中立區     → 固定 kMidFieldReturnRps (70 RPS)

 6. Shooter 開始旋轉（不等對齊）| **Throttle 齒輪比** | 6.12:1 (MK4i L3 Very Fast) |- **新增方法**：`setTargetVelocity(rps)`, `stopShooter()`, `getCurrentRps()`

 7. 角度對齊 + 射手達速 → 啟動 Transport 送球

 8. 遲滯防抖：首次 ≤2°, 連射中 ≤5°| **Rotor 齒輪比** | 150/7:1 |

```

| **輪徑** | 0.1m |### 4. Intake 滾輪 (`IntakeRollerSubsystem.java`) 🔄 已重寫

### 射擊模式行為 (ManualDrive 協同)

| **CAN 優化** | 每次 `setState()` 快取 CANcoder，避免重複讀取 |- **馬達**：X44 TalonFX x2 (CAN ID: 29 Leader, 35 Follower)

按住 Left Bumper 時：

- ManualDrive `shootingMode = true`- **控制方式**：~~DutyCycleOut 40%~~ → **VelocityVoltage 閉環轉速控制**

- 旋轉輸入鎖零 (`zCtl = 0`)，完全由 PID 控制旋轉

- 平移速度 × 0.3（降低移動慣性，提高命中率）### 3. 射球飛輪 (`ShooterSubsystem.java`)- **目標轉速**：吸球 60 RPS / 吐球 -30 RPS



### 中場區域雙目標切換- **PID**：kV=0.12, kP=0.2, kI=0.01, kD=0



| 聯盟顏色 | 己方聯盟區 | 中立區（中場） || 項目 | 說明 |- **電流限制**：Stator 60A, Supply 40A

|---------|-----------|--------------|

| **藍方** | robotX < 8.27m → 瞄 Hub (atan2) | robotX ≥ 8.27m → 固定角度 180° 朝左 ||------|------|- **優點**：遇到阻力時 PID 自動補償電壓維持轉速

| **紅方** | robotX ≥ 8.27m → 瞄 Hub (atan2) | robotX < 8.27m → 固定角度 0° 朝右 |

| **馬達** | TalonFX Leader (CAN 22) + Follower (CAN 21, 反轉跟隨) |- **Follower**：反轉跟隨 (Opposed)

> 💡 中立區不瞄準特定座標點，而是面向固定角度射回己方聯盟區。

| **控制** | VelocityVoltage 閉環 (kV=0.12, kP=0.12) |- **Dashboard 監控**：`Intake/Actual RPS`, `Intake/Leader Current`

### 距離-RPS 對照表

| **速度範圍** | 35~80 RPS（由 AutoAimAndShoot 依距離查表設定） |

| 距離 (m) | 目標 RPS |

|----------|----------|| **容差** | ±3 RPS 視為「達速」(AutoAimConstants.kShooterToleranceRps) |### 5. 輸送帶 (`TransportSubsystem.java`)

| 1.0 | 35 |

| 1.5 | 40 || **方法** | `setTargetVelocity(rps)`, `stopShooter()`, `getCurrentRps()`, `isAtTargetSpeed()` |- **馬達**：TalonFX x2 (CAN ID: 26 推球, 30 輸送)

| 2.0 | 45 |

| 2.5 | 50 |- **輸送速度**：40% / 慢速 20%

| 3.0 | 55 |

| 3.5 | 60 |### 4. Intake 滾輪 (`IntakeRollerSubsystem.java`)- **推球速度**：100%

| 4.0 | 65 |

| 5.0 | 70 |- **電流限制**：Stator 60A, Supply 40A



### 遲滯送球邏輯 (Hysteresis)| 項目 | 說明 |



- **首次觸發**：角度誤差 ≤ 2° 且射手達速 → 開始送球|------|------|### 6. Intake 手臂 (`IntakeArmSubsystem.java`)（目前未啟用）

- **連射保持**：已在送球中，角度誤差 ≤ 5° → 繼續送球

- **停止條件**：角度誤差 > 5° → 呼叫 `stopUpToShoot()` 停止推球| **馬達** | X44 TalonFX ×2 (CAN 29 Leader, 35 Follower 反轉) |- **馬達**：TalonFX x2 (CAN ID: 3 Leader, 4 Follower)



---| **控制** | VelocityVoltage 閉環 (kV=0.12, kP=0.2, kI=0.01) |- **控制方式**：PositionVoltage 位置控制



## 🤖 自動模式 (PathPlanner)| **轉速** | 吸球 60 RPS / 吐球 -30 RPS |- **減速比**：20:1



- 使用 `AutoBuilder` + `autoChooser` + `PPHolonomicDriveController`| **電流** | Stator 60A, Supply 40A |- **軟體限位**：0 ~ 8 圈

- `isAllianceRed()` 支援紅方路徑鏡像

| **Dashboard** | `Intake/Actual RPS`, `Intake/Leader Current` |

### 已註冊 NamedCommands

### 7. LED 燈條 (`LightPollution.java`)

| 名稱 | 功能 |

|------|------|### 5. 輸送帶 (`TransportSubsystem.java`)- **類型**：AddressableLED

| `"Auto Shoot"` | 飛輪 50 RPS + 等達速 → 推球 |

| `"Far Auto Shoot"` | 飛輪 62 RPS + 等達速 → 推球 |- **模式**：彩虹滾動、固定色、閃爍

| `"Auto Intake"` | 吸球 + 慢速輸送同時運行 |

| `"Start Intake"` / `"Stop Intake"` | 控制吸球開關 || 項目 | 說明 |



---|------|------|---



## ⚠️ 已發現並修復的 Bug| **馬達** | up_to_shoot (CAN 26, Brake), transport (CAN 30, Coast) |



### 🔴 BUG #1（嚴重）：推球馬達不會停止| **控制** | DutyCycleOut |## 🎮 操控器按鍵配置



| 項目 | 說明 || **方法** | `runTransportOnly()` — 僅輸送帶 40% |

|------|------|

| **問題** | `runUpToShoot()` 使用 DutyCycleOut 100%，不會自動停止（set-and-hold 機制） || | `runUpToShoot()` — 推球馬達 100%（僅 AutoAim 對齊時呼叫） || 按鍵 | 功能 |

| **修復** | 新增 `stopUpToShoot()` 方法，在未對齊時顯式呼叫 |

| **狀態** | ✅ 已修復 || | `stopUpToShoot()` — **顯式停止推球馬達**（修復 DutyCycleOut set-and-hold 問題） ||------|------|



### 🟡 BUG #2（中等）：teleopInit 震動指令未排程| | `stopTransport()` — 停止全部 || **左搖桿** | 底盤平移控制 (X/Y)，乘以 `kMaxPhysicalSpeedMps` |



| 項目 | 說明 || **電流** | Stator 60A, Supply 40A || **右搖桿 X** | 底盤旋轉控制，乘以 `kMaxPhysicalSpeedMps × Z_MULTIPLIER(-0.4)` |

|------|------|

| **問題** | `Commands.parallel(...)` 只建立 Command 但未排程 || **右搖桿按下** | 切換場地導向/機器導向模式 |

| **修復** | 包裝為 `CommandScheduler.getInstance().schedule(...)` |

| **狀態** | ✅ 已修復 |### 6. Intake 手臂 (`IntakeArmSubsystem.java`) — ⚠️ 未啟用| **Right Bumper** (按住) | 加速模式 (100% vs 50% 平移速度) |



### 🟠 BUG #3（低風險）：IntakeArm CAN ID 衝突| **Left Bumper** (按住) | 🆕 自動瞄準射擊 (AutoAimAndShoot) |



| 項目 | 說明 || 項目 | 說明 || **Menu 按鈕 (8)** | 重置 IMU（同步 poseEstimator） |

|------|------|

| **問題** | IntakeArm CAN 3, 4 與 Swerve 左前 Rotor/Throttle 衝突 ||------|------|| **A 鍵** (按住) | Drive2Tag 自動對位 (正前方 1.15m) |

| **狀態** | ⚠️ 目前未啟用，暫不影響 |

| **馬達** | TalonFX ×2 (CAN 3 Leader, 4 Follower) || **B 鍵** (按住) | 輸送帶反轉（吐球） |

### 🔵 BUG #4（死碼）：setChassisSpeeds 魔數

| **控制** | PositionVoltage (減速比 20:1, 軟限位 0~8 圈) || **X 鍵** (按住) | 輸送帶正轉 |

- `Swerve.setChassisSpeeds()` 有 0.185/0.21 魔數，但從未被呼叫

| **⚠️ CAN 衝突** | CAN ID 3, 4 與 Swerve 左前 Rotor(3)/Throttle(4) 衝突！目前因未在 RobotContainer 中實例化而不影響，但若啟用將造成 CAN 匯流排衝突 || **右板機** (按住) | 射球飛輪 (50 RPS) + 達速後自動推球 |

### 🔵 BUG #5（待確認）：射手安裝方向

| **左板機** (按住) | Intake 滾輪吸球 (VelocityVoltage 60 RPS) |

- `atan2` 瞄準機器人**正前方**面向目標。若射手在背面需 +π 偏移

### 7. LED 燈條 (`LightPollution.java`)

---

- AddressableLED，彩虹滾動/固定色/閃爍---

## ✅ 已完成的優化

- 已降頻至 60ms（解決 Loop Overrun）

### Loop Overrun 修復（5 項）

## 🤖 自動模式

1. `SetRobotOrientation()` → `_NoFlush`

2. `setState()` CAN 快取（12次→1次/迴圈）### 8. Shuffleboard 管理 (`ShuffleboardManager.java`)

3. LED 降頻至 60ms

4. 移除重複 `AutoBuilder.configure()`- 使用 PathPlanner 的 `AutoBuilder` + `autoChooser`

5. Command 實例改用工廠方法

六個分頁：- 已註冊的 NamedCommands：

### 其他優化

1. **Swerve Debug** — 底盤速度、模組角度、Field2d  - `"Auto Shoot"` — 飛輪啟動 + 等速度到達後推球 (50 RPS)

- TrapezoidProfile → SlewRateLimiter (20, 20, 15)

- 統一最大速度常數 `kMaxPhysicalSpeedMps`2. **Shooter PID** — 射手 kP/kI/kD/kV 即時調整  - `"Far Auto Shoot"` — 遠距射擊 (62 RPS)

- IMU/poseEstimator 雙向同步

- `teleopInit()` 自動 Limelight XY 校正3. **Intake PID** — 吸球 kP/kI/kD/kV 即時調整  - `"Auto Intake"` — 同時吸球 + 慢速輸送

- Intake 改 VelocityVoltage 閉環

- Transport 三段式控制4. **AutoAim PID** — 旋轉 kP/kI/kD 即時調整  - `"Start Intake"` / `"Stop Intake"` — 控制吸球開關

- Shuffleboard 六分頁 + TunableNumber

- `RuntimeException` → `DriverStation.reportWarning`5. **Vision** — Limelight tagCount, stdDev, 動態信賴度  - `"shoot work"` / `"transport wait shoot"` — 射擊相關



---6. **Robot Status** — 聯盟顏色、底盤模式、Loop 時間



## 📐 機器人物理參數---



| 參數 | 值 |---

|------|------|

| 底盤軌距/輪距 | 0.62865 m × 0.62865 m |## ⚠️ 已修復問題與優化紀錄

| 輪徑 | 0.1 m |

| Throttle 齒輪比 | 6.12:1 (MK4i L3 Very Fast) |## 🎮 操控器按鍵配置 (Xbox Controller)

| Rotor 齒輪比 | 150/7:1 |

| 最大物理速度 | **5.13 m/s** |> 詳細修復過程請見 [OVERRUN_FIX_LOG.md](./OVERRUN_FIX_LOG.md)

| 平移加速度限制 | 20 m/s² |

| 旋轉加速度限制 | 15 rad/s² || 按鍵 | 功能 |

| Boost 模式 | 100% (5.13 m/s) |

| 一般模式 | 50% (2.57 m/s) ||------|------|### ✅ Loop Overrun 修復（5 項）

| 射擊模式 | 30% (1.54 m/s) |

| **左搖桿** | 底盤平移 (X/Y) × `kMaxPhysicalSpeedMps` |

---

| **右搖桿 X** | 底盤旋轉 × `kMaxPhysicalSpeedMps × Z_MULTIPLIER(-0.4)` || # | 問題 | 修改檔案 | 狀態 |

## 📡 CAN Bus 配置

| **右搖桿按下** | 切換場地導向/機器導向模式 ||---|------|----------|------|

### DRIVETRAIN Bus

| **Right Bumper** (按住) | 加速模式 (100% vs 50% 平移速度) || 1 | `SetRobotOrientation()` flush 阻塞主迴圈 | `Swerve.java` | ✅ 改用 `_NoFlush` |

| 裝置 | CAN ID |

|------|--------|| **Left Bumper** (按住) | 🎯 自動瞄準射擊 (AutoAimAndShoot) || 2 | `setState()` 重複讀取 CAN (12次/迴圈) | `SwerveModule.java` | ✅ 快取角度值 |

| 左後 Rotor / Throttle | 1 / 2 |

| 左前 Rotor / Throttle | 3 / 4 || **Menu 按鈕 (8)** | 重置 IMU（同步 poseEstimator） || 3 | LED 每 20ms 更新 | `LightPollution.java` | ✅ 降頻至 60ms |

| 右前 Rotor / Throttle | 5 / 6 |

| 右後 Rotor / Throttle | 7 / 8 || **A 鍵** (按住) | Drive2Tag 自動對位 (正前方 1.15m) || 4 | `AutoBuilder.configure()` 重複呼叫 | `DriveSubsystem.java` | ✅ 移除重複 |

| CANcoders | 11, 12, 13, 14 |

| **B 鍵** (按住) | 輸送帶反轉（吐球） || 5 | Command 實例共用導致排程衝突 | `RobotContainer.java` | ✅ 改用工廠方法 |

### 預設 Bus

| **X 鍵** (按住) | 輸送帶正轉 |

| 裝置 | CAN ID |

|------|--------|| **右板機** (按住) | 手動射球：飛輪 50 RPS + 達速後自動推球 |### ✅ 速度與加速度優化

| Pigeon2 IMU | 0 |

| Shooter (Leader / Follower) | 22 / 21 || **左板機** (按住) | Intake 滾輪吸球 (VelocityVoltage 60 RPS) |

| Transport (up_to_shoot / conveyor) | 26 / 30 |

| Intake Roller (Leader / Follower) | 29 / 35 || 修改 | 說明 |



------|------|------|



## 🎯 AutoAim 常數一覽 (`Constants.AutoAimConstants`)| TrapezoidProfile → SlewRateLimiter | 原本 TrapezoidProfile 誤用（把速度當位置控制），導致提前減速。改用 SlewRateLimiter(20, 20, 15) |



### Hub 座標（從官方 2026-rebuilt-welded.json AprilTag 計算）## 🔫 AutoAimAndShoot 完整功能說明| 統一最大速度常數 | 所有 `×6.0` 硬編碼改為 `kMaxPhysicalSpeedMps ≈ 5.13 m/s`（由齒輪比計算） |



| 參數 | 值 | 說明 || `getMaxVelocity()` 修正 | 從硬編碼 2 m/s 改為 `kMaxPhysicalSpeedMps` |

|------|------|------|

| kBlueHubX/Y | (4.626, 4.035) | 藍方 Hub 中心（場地左半部） |### 工作原理| SwerveModuleKraken 同步 | `setThrottleSpeed()` 使用 `kMaxPhysicalSpeedMps` 計算百分比 |

| kRedHubX/Y | (11.915, 4.035) | 紅方 Hub 中心（場地右半部） |

| kFieldLengthMeters | 16.541m | 場地全長（官方） |

| kFieldWidthMeters | 8.069m | 場地全寬（官方） |

| kFieldMidX | ≈8.27m | 場地中線 |```### ✅ IMU 與位姿同步



### 中場回傳角度（固定角度，不瞄準特定座標點）每 20ms 執行：



| 參數 | 值 | 說明 | 1. 取得機器人座標 (Swerve.getPose())| 修改 | 說明 |

|------|------|------|

| kRedReturnAngleRad | 0.0 (0°) | 紅方面向場地正右 (+X) | 2. 判斷區域：|------|------|

| kBlueReturnAngleRad | π (180°) | 藍方面向場地正左 (-X) |

    ├── 己方聯盟區 → 目標 = Hub 座標（得分目標）| `resetPose()` 同步 IMU | PathPlanner auto 開始時會同步設定 Pigeon2 yaw，auto 結束後角度自動正確 |

### PID 與容差

    └── 中立區（中場）→ 目標 = 己方聯盟回傳點| `resetIMU()` 同步 poseEstimator | 手動重設 IMU 時同時更新 poseEstimator 航向 |

| 參數 | 值 | 說明 |

|------|------|------| 3. 計算「機器人 → 目標」向量 (dx, dy)| `resetPoseToLimelight()` 只校正 XY | Limelight 只用來修正 XY 位置，角度完全由 IMU 決定 |

| kRotation_kP / kI / kD | 5.0 / 0 / 0.1 | 旋轉 PID |

| kRotationToleranceDeg | 2.0° | 首次觸發送球角度門檻 | 4. atan2 計算目標角度（場地座標系）| `teleopInit()` 自動校正 | 進入 teleop 時自動呼叫 `resetPoseToLimelight()` 校正 XY 位置 |

| kFeedingHysteresisDeg | 5.0° | 連射保持角度門檻 |

| kShooterToleranceRps | 3.0 RPS | 射手達速容差 | 5. PID 計算旋轉速度 → setAimSpeed() 疊加到底盤

| kShootingModeSpeedMultiplier | 0.3 | 射擊模式平移速度倍率 |

| kMidFieldReturnRps | 70.0 RPS | 中場回傳固定射手速度 | 6. 根據區域選擇射手速度：### ✅ 新增功能



---    ├── 己方聯盟區 → 依距離查表線性內插



## 📌 2026 場地適配待辦事項    └── 中立區     → 固定 kMidFieldReturnRps (70 RPS)| 功能 | 說明 |



- [ ] **驗證 Hub 座標精度**：目前由 AprilTag JSON 估算，需實測微調 7. Shooter 開始旋轉（不等對齊）|------|------|

- [ ] **調整距離-RPS 對照表**：根據 Hub 高度和射手機構重新測量

- [ ] **確認射手安裝方向**：若射手在背面需 +π 偏移 8. 角度對齊 + 射手達速 → 啟動 Transport 送球| **AutoAimAndShoot** | 自動旋轉面向 Speaker + 依距離調整射手 RPS + 對準達速後自動送球 |

- [ ] **更新 Limelight AprilTag Layout**：確認使用 `2026-rebuilt-welded` 或 `andymark`

- [ ] **修復 IntakeArm CAN ID**：若需啟用需重新分配 (建議 40+) 9. 遲滯防抖：首次 ≤2°, 連射中 ≤5°| **Intake 閉環控制** | X44 馬達改為 VelocityVoltage 控制，遇阻力自動補償電壓 |

- [ ] **重新規劃 PathPlanner 路徑**：根據 2026 場地佈局

- [ ] **中場回傳角度微調**：實測球飛行方向是否需要偏移```| **模擬支援** | sim gyro、sim motor physics、Field2d 顯示 |

- [ ] **變數重新命名**（可選）：統一使用 Hub 命名取代舊的 Speaker 名稱

| **Loop 計時監控** | `Robot.java` 記錄每次迴圈時間並顯示在 Dashboard |

---

### 射擊模式行為 (ManualDrive 協同)

*報告產生日期：2026-03-07*

### ✅ 其他修正

按住 Left Bumper 時：

- ManualDrive `shootingMode = true`| 修改 | 說明 |

- 旋轉輸入鎖零 (`zCtl = 0`)，完全由 PID 控制旋轉|------|------|

- 平移速度 × 0.3（降低移動慣性，提高命中率）| `RuntimeException` → `DriverStation.reportWarning` | SwerveModuleKraken 初始化失敗不再直接崩潰 |

| `build.gradle` | `includeDesktopSupport = true`（啟用模擬器） |

### 中場區域雙目標切換

---

| 聯盟顏色 | 己方聯盟區 | 中立區（中場） |

|---------|-----------|--------------|## 📐 機器人物理參數

| **藍方** | robotX < 8.27m → 瞄 Hub | robotX ≥ 8.27m → 瞄回傳點 |

| **紅方** | robotX ≥ 8.27m → 瞄 Hub | robotX < 8.27m → 瞄回傳點 || 參數 | 值 |

|------|------|

### 距離-RPS 對照表| 底盤軌距 | 0.62865 m |

| 輪距 | 0.62865 m |

| 距離 (m) | 目標 RPS || 輪徑 | 0.1 m |

|----------|----------|| 齒輪比 (Throttle) | 6.12:1 (L3 Very Fast) |

| 1.0 | 40 || 齒輪比 (Rotor) | 150/7:1 |

| 2.0 | 50 || 最大物理速度 | **5.13 m/s** (`(6000/60/6.12) × 0.1 × π`) |

| 3.0 | 60 || 平移加速度限制 | 20 m/s² (SlewRateLimiter) |

| 4.0 | 70 || 旋轉加速度限制 | 15 rad/s² (SlewRateLimiter) |

| 5.0 | 80 || Boost 模式速度 | 100% (5.13 m/s) |

| 一般模式速度 | 50% (2.57 m/s) |

### 遲滯送球邏輯 (Hysteresis)

---

防止角度在門檻附近時「送一下停一下」造成卡球：

- **首次觸發**：角度誤差 ≤ `kRotationToleranceDeg` (2°) 且射手達速 → 開始送球## 📡 CAN Bus 配置

- **連射保持**：已在送球中，角度誤差 ≤ `kFeedingHysteresisDeg` (5°) → 繼續送球

- **停止條件**：角度誤差 > 5° → 停止推球（呼叫 `stopUpToShoot()`）| 裝置 | CAN ID | Bus |

|------|--------|-----|

---| Pigeon2 IMU | 0 | 預設 |

| 左後 Rotor | 1 | DRIVETRAIN |

## 🤖 自動模式 (PathPlanner)| 左後 Throttle | 2 | DRIVETRAIN |

| 左前 Rotor | 3 | DRIVETRAIN |

- 使用 `AutoBuilder` + `autoChooser` + `PPHolonomicDriveController`| 左前 Throttle | 4 | DRIVETRAIN |

- `isAllianceRed()` 支援紅方路徑鏡像| 右前 Rotor | 5 | DRIVETRAIN |

| 右前 Throttle | 6 | DRIVETRAIN |

### 已註冊 NamedCommands| 右後 Rotor | 7 | DRIVETRAIN |

| 右後 Throttle | 8 | DRIVETRAIN |

| 名稱 | 功能 || 左前 CANcoder | 12 | DRIVETRAIN |

|------|------|| 右前 CANcoder | 13 | DRIVETRAIN |

| `"Auto Shoot"` | 飛輪 50 RPS + 等達速 → 推球 || 左後 CANcoder | 11 | DRIVETRAIN |

| `"Far Auto Shoot"` | 飛輪 62 RPS + 等達速 → 推球 || 右後 CANcoder | 14 | DRIVETRAIN |

| `"Auto Intake"` | 吸球 + 慢速輸送同時運行 || Shooter | 22 | 預設 |

| `"Start Intake"` / `"Stop Intake"` | 控制吸球開關 || Transport (up_to_shoot) | 26 | 預設 |

| `"shoot work"` / `"transport wait shoot"` | 射擊相關 || Intake Roller Leader (X44) | 29 | 預設 |

| Transport | 30 | 預設 |

### PathPlanner 路徑| Intake Roller Follower (X44) | 35 | 預設 |



- `manual right auto.auto` — 右側開局自動---

- `New right Auto.auto` — 右側新版自動

- `strange Right Auto.auto` — 右側備用自動## 🆕 AutoAimAndShoot 功能說明



---### 工作原理

1. **位置感知**：使用 `swerve.getPose()` 取得場地座標中的機器人位置

## ⚠️ 已發現並修復的 Bug2. **角度計算**：`atan2(toTarget.Y, toTarget.X)` 計算面向 Speaker 所需角度

3. **PID 旋轉**：透過 `setAimSpeed()` 疊加旋轉速度（不影響手動平移控制）

### 🔴 BUG #1（嚴重）：推球馬達不會停止4. **距離查表**：依距離線性內插取得目標射手 RPS

5. **自動發射**：角度對齊 (±2°) + 射手達速 → 啟動 Transport 送球

| 項目 | 說明 |

|------|------|### 距離-RPS 對照表

| **問題** | `TransportSubsystem.runUpToShoot()` 使用 `DutyCycleOut` 設定推球馬達 100% 後，當 `m_isFeeding` 變為 false 時，馬達繼續以 100% 運轉（DutyCycleOut 是 set-and-hold 機制） |

| **影響** | 連續不停地推球，可能造成機構損壞或浪費遊戲物件 || 距離 (m) | 目標 RPS |

| **修復** | 新增 `stopUpToShoot()` 方法，在 `AutoAimAndShoot.execute()` 的 else 分支顯式呼叫 ||----------|----------|

| **狀態** | ✅ 已修復 || 1.0 | 40 |

| 2.0 | 50 |

### 🟡 BUG #2（中等）：teleopInit 震動指令未排程| 3.0 | 60 |

| 4.0 | 70 |

| 項目 | 說明 || 5.0 | 80 |

|------|------|

| **問題** | `RobotContainer.teleopInit()` 中 `Commands.parallel(...)` 只建立了 Command 物件但未排程執行 |### 按鍵綁定

| **影響** | 進入 teleop 時手把不會震動提示 |- **Left Bumper** (按住) → 啟動自動瞄準射擊

| **修復** | 包裝為 `CommandScheduler.getInstance().schedule(Commands.sequence(...))` |- 放開即停止所有動作（旋轉、射手、Transport）

| **狀態** | ✅ 已修復 |

### Constants 設定 (`AutoAimConstants`)

### 🟠 BUG #3（低風險）：IntakeArm CAN ID 衝突- 藍方 Speaker: `(0.0, 5.55)`

- 紅方 Speaker: `(16.54, 5.55)`

| 項目 | 說明 |- 旋轉 PID: kP=5.0, kI=0, kD=0.1

|------|------|- 角度容差: 2°

| **問題** | `IntakeArmSubsystem` 使用 CAN ID 3, 4（DRIVETRAIN bus），與 Swerve 左前 Rotor(3)/Throttle(4) 衝突 |- 射手速度容差: 5 RPS

| **影響** | 目前 IntakeArm 未在 RobotContainer 實例化，不影響運行。但若啟用將造成 CAN 匯流排衝突 |

| **建議** | 重新分配 CAN ID（建議改為 40+），或確認 IntakeArm 使用不同的 CAN bus |---

| **狀態** | ⚠️ 未修復（暫不影響） |

*報告產生日期：2026-02-26*

### 🔵 BUG #4（死碼）：setChassisSpeeds 魔數

| 項目 | 說明 |
|------|------|
| **問題** | `Swerve.setChassisSpeeds()` 中有 0.185/0.21 魔數用於對角線位移補償，但此方法從未被呼叫 |
| **影響** | 無直接影響（dead code），但如果未來使用可能產生非預期行為 |
| **狀態** | ⚠️ 標記為死碼 |

### 🔵 BUG #5（待確認）：射手安裝方向

| 項目 | 說明 |
|------|------|
| **問題** | `atan2` 計算的是機器人**正前方**面向目標的角度。若射手安裝在機器人**背面**，需要 +π 偏移 |
| **建議** | 在 `AutoAimAndShoot` 的 `targetAngleRad` 計算後加入 `+ Math.PI` |
| **狀態** | ⚠️ 待團隊確認射手安裝方向 |

---

## ✅ 已完成的優化紀錄

### Loop Overrun 修復

| # | 問題 | 修改檔案 | 修復方式 |
|---|------|----------|---------|
| 1 | `SetRobotOrientation()` flush 阻塞 | `Swerve.java` | 改用 `_NoFlush` |
| 2 | `setState()` 重複讀取 CAN (12次/迴圈) | `SwerveModule.java` | 快取角度值 |
| 3 | LED 每 20ms 更新 | `LightPollution.java` | 降頻至 60ms |
| 4 | `AutoBuilder.configure()` 重複呼叫 | `DriveSubsystem.java` | 移除重複 |
| 5 | Command 實例共用排程衝突 | `RobotContainer.java` | 改用工廠方法 |

### 速度與加速度優化

| 修改 | 說明 |
|------|------|
| TrapezoidProfile → SlewRateLimiter | 原本誤用（把速度當位置控制），改用 SlewRateLimiter(20, 20, 15) |
| 統一最大速度常數 | 所有 `×6.0` 硬編碼改為 `kMaxPhysicalSpeedMps ≈ 5.13 m/s` |
| SwerveModuleKraken 同步 | `setThrottleSpeed()` 使用 `kMaxPhysicalSpeedMps` 計算百分比 |

### IMU 與位姿同步

| 修改 | 說明 |
|------|------|
| `resetPose()` 同步 IMU | PathPlanner auto 開始時同步 Pigeon2 yaw |
| `resetIMU()` 同步 poseEstimator | 手動重設 IMU 時更新 poseEstimator |
| `resetPoseToLimelight()` 只校正 XY | Limelight 修正 XY，角度信任 IMU |
| `teleopInit()` 自動校正 | 進入 teleop 自動呼叫 `resetPoseToLimelight()` |

### 新增功能

| 功能 | 說明 |
|------|------|
| **AutoAimAndShoot** | 自動旋轉面向目標 + 依距離調 RPS + 遲滯送球 + 雙目標區域偵測 |
| **射擊模式** | ManualDrive 速度抑制 (×0.3) + 旋轉鎖定 |
| **Intake 閉環** | X44 改 VelocityVoltage，遇阻力自動補償 |
| **Transport 分離** | `runTransportOnly()` / `runUpToShoot()` / `stopUpToShoot()` 三段式控制 |
| **Shuffleboard 六分頁** | Swerve Debug / Shooter PID / Intake PID / AutoAim PID / Vision / Robot Status |
| **TunableNumber** | NetworkTables 即時可調參數（不需重新部署） |
| **Loop 計時監控** | `Robot.java` 記錄迴圈時間顯示於 Dashboard |
| **模擬支援** | sim gyro 積分、sim motor physics、Field2d |

---

## 📐 機器人物理參數

| 參數 | 值 |
|------|------|
| 底盤軌距/輪距 | 0.62865 m × 0.62865 m |
| 輪徑 | 0.1 m |
| Throttle 齒輪比 | 6.12:1 (MK4i L3 Very Fast) |
| Rotor 齒輪比 | 150/7:1 |
| 最大物理速度 | **5.13 m/s** |
| 平移加速度限制 | 20 m/s² (SlewRateLimiter) |
| 旋轉加速度限制 | 15 rad/s² (SlewRateLimiter) |
| Boost 模式 | 100% (5.13 m/s) |
| 一般模式 | 50% (2.57 m/s) |
| 射擊模式 | 30% (1.54 m/s) |

---

## 📡 CAN Bus 配置

### DRIVETRAIN Bus

| 裝置 | CAN ID |
|------|--------|
| 左後 Rotor | 1 |
| 左後 Throttle | 2 |
| 左前 Rotor | 3 |
| 左前 Throttle | 4 |
| 右前 Rotor | 5 |
| 右前 Throttle | 6 |
| 右後 Rotor | 7 |
| 右後 Throttle | 8 |
| 左後 CANcoder | 11 |
| 左前 CANcoder | 12 |
| 右前 CANcoder | 13 |
| 右後 CANcoder | 14 |

### 預設 Bus

| 裝置 | CAN ID |
|------|--------|
| Pigeon2 IMU | 0 |
| Shooter Follower | 21 |
| Shooter Leader | 22 |
| Transport (up_to_shoot) | 26 |
| Intake Roller Leader (X44) | 29 |
| Transport (conveyor) | 30 |
| Intake Roller Follower (X44) | 35 |

> ⚠️ IntakeArmSubsystem (CAN 3, 4) 與 Swerve 左前模組衝突！目前未啟用。

---

## 🎯 AutoAim 常數一覽 (`Constants.AutoAimConstants`)

### 目標座標（⚠️ 需根據 2026 場地重新量測）

| 參數 | 值 | 說明 |
|------|------|------|
| kBlueSpeakerX/Y | (0.0, 5.55) | 藍方得分目標座標 |
| kRedSpeakerX/Y | (16.54, 5.55) | 紅方得分目標座標 |
| kBlueAllianceReturnX/Y | (2.0, 4.0) | 藍方中場回傳點 |
| kRedAllianceReturnX/Y | (14.54, 4.0) | 紅方中場回傳點 |
| kFieldLengthMeters | 16.54m | 場地全長 |
| kFieldMidX | 8.27m | 場地中線 (自動計算) |

### PID 與容差

| 參數 | 值 | 說明 |
|------|------|------|
| kRotation_kP / kI / kD | 5.0 / 0 / 0.1 | 旋轉 PID |
| kRotationToleranceDeg | 2.0° | 首次觸發送球角度門檻 |
| kFeedingHysteresisDeg | 5.0° | 連射保持角度門檻（遲滯） |
| kShooterToleranceRps | 3.0 RPS | 射手達速容差 |
| kShootingModeSpeedMultiplier | 0.3 | 射擊模式平移速度倍率 |
| kMidFieldReturnRps | 70.0 RPS | 中場回傳固定射手速度 |

---

## 📌 2026 場地適配待辦事項

> 以下是將本程式碼適配 2026 REEFSCAPE 場地需要完成的工作：

- [ ] **量測 2026 場地得分目標座標**：更新 `kBlueSpeakerX/Y`, `kRedSpeakerX/Y` 為實際 Reef/Processor 位置
- [ ] **量測回傳點座標**：更新 `kBlue/RedAllianceReturnX/Y` 為 2026 場地合理回傳位置
- [ ] **調整距離-RPS 對照表**：根據新的得分機構和目標高度，重新測量最佳射手轉速
- [ ] **確認射手安裝方向**：若射手在機器人背面，需在 `targetAngleRad` 加 `+Math.PI`
- [ ] **更新 AprilTag 佈局**：確認 Limelight 使用的 AprilTag Field Layout 為 2026 版本
- [ ] **修復 IntakeArm CAN ID**：若需啟用 IntakeArmSubsystem，需重新分配 CAN ID（避免 3, 4 衝突）
- [ ] **驗證 PathPlanner 路徑**：所有 auto 路徑需要根據 2026 場地重新規劃
- [ ] **變數重新命名**（可選）：將 `kBlueSpeakerX/Y` 等變數名稱改為更通用的名稱（如 `kBlueTargetX/Y`）

---

*報告產生日期：2026-02-27*
