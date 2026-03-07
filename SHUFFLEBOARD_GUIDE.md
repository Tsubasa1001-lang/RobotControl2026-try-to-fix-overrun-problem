# 🤖 Simulate Robot Code + Shuffleboard 使用教學

> **適用版本**：WPILib 2026、VS Code + WPILib Extension

---

## 目錄

1. [環境準備](#1-環境準備)
2. [啟動模擬器](#2-啟動模擬器-simulate-robot-code)
3. [啟動 Shuffleboard](#3-啟動-shuffleboard)
4. [連線確認](#4-連線確認)
5. [各分頁功能說明](#5-各分頁功能說明)
6. [即時修改 PID 參數](#6-即時修改-pid-參數)
7. [Field2d 場地地圖](#7-field2d-場地地圖)
8. [常見問題](#8-常見問題)
9. [🔫 Shooter 完整系統架構與參數調整指南](#9--shooter-完整系統架構與參數調整指南)

---

## 1. 環境準備

確認以下軟體已安裝：

- ✅ **VS Code** + **WPILib 2026 Extension**
- ✅ 專案可以成功執行 `.\gradlew.bat build`（Build Successful）

---

## 2. 啟動模擬器 (Simulate Robot Code)

### 步驟

**① 開啟指令面板**

按下鍵盤快捷鍵：
```
Ctrl + Shift + P
```

**② 搜尋並執行**

輸入以下指令並按 Enter：
```
WPILib: Simulate Robot Code
```

> 💡 也可以點選 VS Code 右上角的 **WPILib 圖示（W）**，在選單中找到 `Simulate Robot Code`

**③ 選擇模擬模式（彈出視窗）**

出現視窗後，勾選以下選項後按 **OK**：

| 選項 | 說明 |
|------|------|
| ✅ `halsim_gui` | 開啟 **Glass** 視窗（DS 面板 + 感測器模擬） |

**④ 等待 Glass 視窗出現**

程式編譯完成後，會自動跳出 **Glass（SimGUI）** 視窗。

### Glass 視窗操作

```
Robot State 面板：
  ┌──────────────────────────┐
  │  Disabled  │  Teleop  │  │  ← 點選 "Teleop" 讓機器人進入遙控模式
  │  Auto      │  Test    │  │
  └──────────────────────────┘
```

> ⚠️ **必須切換到 Teleop 或 Auto 模式**，PID 控制和馬達指令才會執行

---

## 3. 啟動 Shuffleboard

**① 開啟指令面板**
```
Ctrl + Shift + P
```

**② 搜尋並執行**
```
WPILib: Start Tool
```

**③ 選擇工具**

在下拉選單中選擇：
```
Shuffleboard
```

> 💡 Shuffleboard 需要在模擬器**已啟動**的狀態下開啟，否則無法自動連線

---

## 4. 連線確認

Shuffleboard 開啟後，檢查左上角狀態：

```
🟢 Connected to localhost   ← 模擬器連線成功
🔴 Disconnected             ← 尚未連線，等待或重新啟動
```

### 手動設定連線位址（如需要）

```
File → Preferences → Server → 
  模擬器填入: localhost
  實體機器人填入: 10.TE.AM.2  (例如隊號1234 → 10.12.34.2)
```

---

## 5. 各分頁功能說明

本專案共有 **6 個分頁**，點選 Shuffleboard 上方標籤切換：

### 🏠 Main Tab

| Widget | 說明 |
|--------|------|
| **Field** | Field2d 場地地圖，機器人位置即時顯示 |
| **Auto Mode** | 下拉選擇自動模式 |
| **Loop ms** | 機器人每圈執行時間（波形圖），正常應 < 20ms |
| **Loop Max ms** | 歷史最大迴圈時間 |
| **Overrun?** | 🟢 正常 / 🔴 超過 20ms 警告 |

---

### 🔫 Shooter PID Tab

| Widget | 說明 |
|--------|------|
| `kV` / `kP` / `kI` / `kD` / `kS` | **可編輯**的 PID 參數 |
| `Current RPS` | 目前馬達轉速（Graph 波形） |
| `Target RPS` | 目標轉速（Graph 波形） |
| `Error RPS` | 誤差值（Graph 波形） |
| `Output Voltage` | 輸出電壓 |
| `Stator Current` | 電流 |

---

### 🦾 IntakeArm PID Tab

| Widget | 說明 |
|--------|------|
| `kP` / `kI` / `kD` / `kG` | **可編輯**的 PID 參數（kG = 重力補償） |
| `Arm Rotations` | 手臂圈數（換算後） |
| `Motor Rotations` | 馬達圈數（原始值） |
| `Output Voltage` | 輸出電壓 |
| `Stator Current` | 電流 |

---

### 🔄 IntakeRoller PID Tab

| Widget | 說明 |
|--------|------|
| `kV` / `kP` / `kI` / `kD` | **可編輯**的 PID 參數 |
| `Actual RPS` | 實際轉速波形 |
| `Target RPS` | 目標轉速波形 |
| `Leader Current` | 主馬達電流 |

---

### 🚗 Swerve Tab

| Widget | 說明 |
|--------|------|
| `Chassis vx` | X 方向速度波形（m/s） |
| `Chassis vy` | Y 方向速度波形（m/s） |
| `Chassis omega` | 旋轉速度波形（rad/s） |
| `Gyro Angle` | 陀螺儀當前角度（度） |
| `Mod0 Speed/Angle` | 左前模組速度與角度 |

---

### 🎯 AutoAim Tab

| Widget | 說明 |
|--------|------|
| `Rotation kP` / `kI` / `kD` | **可編輯**的旋轉 PID |
| `Distance` | 機器人到目標距離（m） |
| `Target/Current Angle` | 目標角度 vs 當前角度 |
| `Angle Error` | 角度誤差波形 |
| `Target/Current RPS` | 射手轉速 |
| `Aligned?` | 🟢 已對齊 / 🔴 未對齊 |
| `At Speed?` | 🟢 達速 / 🔴 未達速 |
| `Feeding?` | 🟢 正在送球 |

---

## 6. 即時修改 PID 參數

這是本系統最核心的功能，可以**不重新部署**直接調整 PID。

### 操作步驟

```
① 切換到對應的 PID Tab（例如 "Shooter PID"）

② 找到要修改的參數輸入框（例如 kP）

③ 直接點擊數字框 → 輸入新數值 → 按 Enter

④ 程式在下一個 20ms 週期自動套用到馬達控制器

⑤ 觀察波形圖，確認響應變化
```

### 調參流程建議

```
1. 先把 kI、kD 設為 0，只調 kP
2. 慢慢增加 kP，直到剛好有輕微震盪
3. 加入 kD 抑制震盪
4. 最後微調 kI 消除穩態誤差
5. 確認效果後，把數值寫回 Constants.java！
```

> ⚠️ **重要提醒**：重新部署程式碼或重開模擬器後，Shuffleboard 上的修改會**消失**，一定要手動更新原始碼！

### 寫回 Constants.java 的位置

| 參數 | 檔案位置 |
|------|---------|
| Shooter kP/kV/kS | `ShooterSubsystem.java` 建構式（預設值直接寫在 `new TunableNumber(tab, "kP", 0.12)` 裡） |
| IntakeArm kP/kG | `IntakeArmSubsystem.java` 建構式 |
| IntakeRoller kV | `IntakeRollerSubsystem.java` 建構式 |
| Swerve Rotor kP | `Constants.java` → `SwerveConstants.kRotor_kP` |
| AutoAim Rotation kP | `Constants.java` → `AutoAimConstants.kRotation_kP` |
| 距離-RPS 對照表 | `Constants.java` → `AutoAimConstants.kDistanceToRpsTable` |
| 射擊模式速度倍率 | `Constants.java` → `AutoAimConstants.kShootingModeSpeedMultiplier` |
| 射手容許誤差 | `Constants.java` → `AutoAimConstants.kShooterToleranceRps` |
| 角度容許誤差（首次觸發） | `Constants.java` → `AutoAimConstants.kRotationToleranceDeg` |
| 遲滯角度（連射保持） | `Constants.java` → `AutoAimConstants.kFeedingHysteresisDeg` |
| Hub 座標 (2026 REBUILT) | `Constants.java` → `AutoAimConstants.kBlueHubX/Y`, `kRedHubX/Y` |

---

## 9. 🔫 Shooter 完整系統架構與參數調整指南

### 9.1 系統架構概覽

```
┌───────────────────────────────────────────────────────────────┐
│                        操作者控制流程                           │
│                                                               │
│  ┌─────────────┐    ┌──────────────────┐    ┌──────────────┐ │
│  │  Xbox 手把   │───▶│   ManualDrive    │───▶│    Swerve    │ │
│  │  左搖桿=平移 │    │  setSpeed(x,y,z) │    │ 底盤移動控制  │ │
│  │  右搖桿=旋轉 │    │                  │    │              │ │
│  └─────────────┘    └──────────────────┘    └──────────────┘ │
│                                                    ▲          │
│  ┌─────────────────────────────────────────────────┤          │
│  │           按住 Left Bumper 時                    │          │
│  │  ┌──────────────────────┐                       │          │
│  │  │  AutoAimAndShoot     │  setAimSpeed(旋轉PID) │          │
│  │  │                      │───────────────────────┘          │
│  │  │  ① 判斷區域(己方/中場) │                                  │
│  │  │  ② 選擇目標(Hub/     │                                  │
│  │  │     回傳點)           │                                  │
│  │  │  ③ PID 控制底盤旋轉   │                                  │
│  │  │  ④ 依區域調整射手速度  │                                  │
│  │  │  ⑤ 達速+對準→自動送球  │                                  │
│  │  └──────┬──────┬────────┘                                  │
│  │         │      │                                           │
│  │         ▼      ▼                                           │
│  │   ┌──────────┐ ┌───────────────┐                           │
│  │   │ Shooter  │ │  Transport    │                           │
│  │   │ 射手馬達  │ │  送球+上膛     │                           │
│  │   └──────────┘ └───────────────┘                           │
│  └────────────────────────────────────────────────────────────│
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │  按住 Right Trigger 時（手動射擊，不自動瞄準）            │  │
│  │  Shooter → 50 RPS → 達速後自動 Transport 送球            │  │
│  └─────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────┘
```

### 9.2 射擊模式 (Shooting Mode)

**按住 Left Bumper** 啟動 `AutoAimAndShoot` 時，`ManualDrive` 會自動進入射擊模式：

| 行為 | 正常模式 | 射擊模式 |
|------|---------|---------|
| 左搖桿平移 | 100%（不按 Boost）/ 50% | **×0.3** (30%)，避免移動慣性讓球射偏 |
| 右搖桿旋轉 | 正常操控 | **鎖定為 0**（由 AutoAim PID 全速控制） |
| Boost (Right Bumper) | 2x 加速 | 仍可使用（但效果 ×0.3） |
| 底盤旋轉速度 | 手動控制 | **不限速**，PID 輸出直接驅動底盤旋轉 |

> 放開 Left Bumper 後自動恢復正常操控。

### 9.3 自動瞄準流程 (AutoAimAndShoot)

每個 20ms 週期執行以下步驟：

```
 1. 取得機器人目前位置 (Swerve.getPose())
 2. 判斷區域：
    ├── 己方聯盟區 → 計算面向 Hub 的角度 (atan2)
    └── 中立區（中場）→ 使用固定角度朝向己方聯盟區
 3. 計算到 Hub 的距離（己方區域用實際距離）
 4. 取得目前角度 (Swerve.getHeading())
 5. PID 計算旋轉速度（不限速）→ setAimSpeed() 疊加到底盤
 6. 根據區域選擇射手速度：
    ├── 己方聯盟區 → 根據距離查表（線性內插）
    └── 中立區     → 固定 kMidFieldReturnRps (70 RPS)
 7. 設定 Shooter 目標速度（按下按鈕就開始轉，不等對齊）
 8. 判斷角度對齊（遲滯邏輯）
 9. Transport 輸送帶持續運轉，預送球到待發位置
10. 角度對齊? AND 射手達速?
    ✅ → 啟動 up_to_shoot 推球進射手飛輪（開始發射）
    ❌ → 停止 up_to_shoot（輸送帶繼續預送球）
```

### 9.3.1 遲滯防抖 (Hysteresis)

為避免機器人移動過程中些許角度偏差導致連射中斷，使用**雙閾值遲滯邏輯**：

```
                    ┌───── kFeedingHysteresisDeg (5°) ─────┐
                    │                                       │
  ──────────────────┤        送球中保持發射                   ├──────
                    │                                       │
          ┌── kRotationToleranceDeg (2°) ──┐                │
          │                                │                │
  ────────┤      初次觸發門檻               ├────────────────┤
          │                                │                │
          └────────────────────────────────┘                │
                    └───────────────────────────────────────┘
```

| 狀態 | 觸發條件 | 說明 |
|------|---------|------|
| 尚未送球 → 開始送球 | 角度誤差 ≤ **2°** AND 射手達速 | 嚴格門檻，確保首發精準 |
| 正在送球 → 繼續送球 | 角度誤差 ≤ **5°** AND 射手達速 | 寬鬆門檻，連射不中斷 |
| 正在送球 → 停止送球 | 角度誤差 > **5°** OR 射手失速 | 偏差太大才暫停 |

### 9.3.2 中場區域雙目標切換 (Mid-Field Zone Logic)

機器人在**中立區（中場）**時，射擊行為與在己方聯盟區不同：

```
場地座標示意圖 (wpiBlue 座標系) — 2026 REBUILT
X = 0m                    X ≈ 8.27m (中線)             X = 16.541m
 ┌────────────────────────────┬────────────────────────────┐
 │         藍方聯盟區          │         紅方聯盟區          │
 │                            │                            │
 │     🔵 藍方 Hub             │            🔴 紅方 Hub     │
 │     (4.63, 4.04)           │            (11.92, 4.04)   │
 │                            │                            │
 │  ⬅ 藍方回傳方向 (180°)      │   ➡ 紅方回傳方向 (0°)      │
 │                            │                            │
 └────────────────────────────┴────────────────────────────┘
  ※ Hub 在場地中央區域，不在牆壁邊！
  ※ 中立區回傳使用固定角度，不瞄準特定座標點
```

**區域判斷邏輯**：

| 聯盟顏色 | 己方聯盟區 | 中立區（中場） |
|---------|-----------|--------------|
| **藍方** | robotX < 8.27m | robotX ≥ 8.27m |
| **紅方** | robotX ≥ 8.27m | robotX < 8.27m |

**不同區域的行為差異**：

| | 己方聯盟區 | 中立區（中場） |
|---|---|---|
| **瞄準方式** | atan2 計算面向 Hub 的角度 | 固定角度朝向己方聯盟區 |
| **目的** | 射入 Hub 得分 (Fuel) | 將球回傳至己方區域 |
| **射手速度** | 依距離線性內插 (35~70 RPS) | 固定 kMidFieldReturnRps (70 RPS) |
| **旋轉 PID** | 相同 | 相同 |
| **送球邏輯** | 相同（遲滯防抖） | 相同（遲滯防抖） |

> 💡 **實際場景**：比賽中在中場拿到球時，按住 Left Bumper 不會瞄向 Hub（太遠射不進），
> 而是面向己方聯盟區方向（固定角度），將球高速射回，讓隊友或自己回到近距離再射擊得分。
>
> ⚠️ 回傳角度 (`kBlue/RedReturnAngleRad`) 和回傳速度 (`kMidFieldReturnRps`) 需要根據場地實際測試調整！

### 9.4 所有可調參數一覽表

#### 🔫 Shooter 馬達 PID（Shuffleboard "Shooter PID" Tab 即時調整）

| 參數 | 預設值 | 調整位置 | 效果說明 |
|------|-------|---------|---------|
| **kV** | `0.12` | Shuffleboard → Shooter PID Tab → kV | 前饋速度增益：每 1 RPS 需要多少伏特。直接影響穩態速度。**第一個要調的參數**。計算方式：`12V / 空載最大RPS` |
| **kP** | `0.12` | Shuffleboard → Shooter PID Tab → kP | 比例增益：誤差越大，補償越多。太大會震盪，太小達速慢 |
| **kI** | `0.0` | Shuffleboard → Shooter PID Tab → kI | 積分增益：消除穩態誤差。通常設很小或為 0，太大會造成積分飽和 |
| **kD** | `0.0` | Shuffleboard → Shooter PID Tab → kD | 微分增益：抑制震盪。對飛輪類機構通常設 0 |
| **kS** | `0.0` | Shuffleboard → Shooter PID Tab → kS | 靜摩擦補償：克服馬達靜止摩擦所需的最小電壓。可用 SysId 量測 |

> 📌 寫回位置：`ShooterSubsystem.java` 建構式中的 `new TunableNumber(tab, "kV", 0.12)` 等

#### 🎯 自動瞄準旋轉 PID（Shuffleboard "AutoAim" Tab 即時調整）

| 參數 | 預設值 | 調整位置 | 效果說明 |
|------|-------|---------|---------|
| **Rotation kP** | `5.0` | Shuffleboard → AutoAim Tab → Rotation kP | 旋轉比例增益：角度偏差越大轉越快。太大會來回震盪 |
| **Rotation kI** | `0.0` | Shuffleboard → AutoAim Tab → Rotation kI | 旋轉積分：消除微小角度偏差。通常設 0 即可 |
| **Rotation kD** | `0.1` | Shuffleboard → AutoAim Tab → Rotation kD | 旋轉微分：抑制旋轉震盪，讓對齊更平滑 |

> 📌 寫回位置：`Constants.java` → `AutoAimConstants.kRotation_kP / kI / kD`

#### 📏 距離 → 射手 RPS 對照表

| 距離 (m) | 目標 RPS | 說明 |
|----------|---------|------|
| 1.0 | 35 | 最近距離，低速 |
| 1.5 | 40 | |
| 2.0 | 45 | |
| 2.5 | 50 | 中距離 |
| 3.0 | 55 | |
| 3.5 | 60 | |
| 4.0 | 65 | |
| 5.0 | 70 | 最遠距離，高速 |

> ⚠️ 這些數值需要在**實際場地**測試後調整！中間距離使用線性內插。
>
> 📌 調整位置：`Constants.java` → `AutoAimConstants.kDistanceToRpsTable`
>
> **怎麼調**：
> 1. 把機器人放在已知距離（用皮尺量）
> 2. 手動給不同 RPS 射擊，找到能穩定進球的 RPS
> 3. 記錄下來填入表格
> 4. 中間的距離由程式自動線性內插

#### ⚙️ 容許誤差 & 閾值

| 參數 | 預設值 | 調整位置 | 效果說明 |
|------|-------|---------|---------|
| **kRotationToleranceDeg** | `2.0°` | `Constants.java` → `AutoAimConstants` | **首次觸發**送球的角度門檻。太小會一直不觸發射擊，太大會打歪 |
| **kFeedingHysteresisDeg** | `5.0°` | `Constants.java` → `AutoAimConstants` | **連射中**保持送球的寬鬆角度門檻。防止移動中微小偏差中斷連射。太大可能打歪，太小連射容易中斷 |
| **kShooterToleranceRps** | `3.0 RPS` | `Constants.java` → `AutoAimConstants` | 射手速度在目標 ± 此值內視為「達速」。太小會等很久才射，太大會射出不穩定的球 |
| **kShootingModeSpeedMultiplier** | `0.3` | `Constants.java` → `AutoAimConstants` | 射擊模式下平移速度倍率。降低移動慣性防止球射偏。`0.3` = 30% 速度 |

#### 🎯 Hub 座標 (2026 REBUILT — 從 AprilTag JSON 計算，Hub 在場地中央)

| 參數 | 預設值 | 說明 |
|------|-------|------|
| **kBlueHubX** | `4.626 m` | 藍方 Hub X 座標（場地左半部中央） |
| **kBlueHubY** | `4.035 m` | 藍方 Hub Y 座標（場地中央偏下） |
| **kRedHubX** | `11.915 m` | 紅方 Hub X 座標（場地右半部中央） |
| **kRedHubY** | `4.035 m` | 紅方 Hub Y 座標（場地中央偏下） |

> ⚠️ **必須根據你的比賽場地量測這些值！**
>
> 📌 調整位置：`Constants.java` → `AutoAimConstants.kBlueHubX/Y`, `kRedHubX/Y`

#### 🔄 中場回傳角度（固定角度，不瞄準特定座標點）

| 參數 | 預設值 | 說明 |
|------|-------|------|
| **kRedReturnAngleRad** | `0.0 rad (0°)` | 紅方中立區回傳：面向場地正右 (+X 方向) |
| **kBlueReturnAngleRad** | `π rad (180°)` | 藍方中立區回傳：面向場地正左 (-X 方向) |

> 📌 調整位置：`Constants.java` → `AutoAimConstants.kRed/BlueReturnAngleRad`

#### 🗺️ 中場區域判斷

| 參數 | 預設值 | 調整位置 | 說明 |
|------|-------|---------|------|
| **kFieldLengthMeters** | `16.541 m` | `Constants.java` → `AutoAimConstants` | 場地全長（官方 2026 REBUILT） |
| **kFieldWidthMeters** | `8.069 m` | `Constants.java` → `AutoAimConstants` | 場地全寬 |
| **kFieldMidX** | `≈8.27 m` | 自動計算 (`kFieldLengthMeters / 2`) | 場地中線 X 座標，用來判斷己方/中立區 |
| **kMidFieldReturnRps** | `70.0 RPS` | `Constants.java` → `AutoAimConstants` | 中場回傳球時的射手固定速度。需實測調整 |

> 💡 **中場回傳角度怎麼調**：
> 1. 在中場發球，觀察球落點方向
> 2. 如果球偏左/偏右 → 微調 `kBlue/RedReturnAngleRad` 的角度值
> 3. 如果球飛不到位 → 提高 `kMidFieldReturnRps`
> 4. 如果球飛太遠出界 → 降低 `kMidFieldReturnRps`
> 4. Y 座標調整左右偏移

#### 🚗 ManualDrive 相關

| 參數 | 預設值 | 調整位置 | 效果說明 |
|------|-------|---------|---------|
| **X_MULTIPLIER** | `1.0` | `ManualDrive.java` | X 方向搖桿靈敏度倍率 |
| **Y_MULTIPLIER** | `1.0` | `ManualDrive.java` | Y 方向搖桿靈敏度倍率 |
| **Z_MULTIPLIER** | `-0.4` | `ManualDrive.java` | 旋轉搖桿靈敏度倍率（負號 = 反轉方向）。`0.4` = 旋轉最大速度是平移的 40% |
| **NULL_ZONE** | `0.05` | `ManualDrive.java` | 搖桿死區：低於 5% 的輸入視為 0，避免搖桿飄移 |
| **boostTranslation** | `0.5 / 1.0` | `ManualDrive.java` | 不按 Right Bumper = 50% 速度，按住 = 100% 速度 |

#### 🔄 Transport 馬達

| 參數 | 預設值 | 調整位置 | 效果說明 |
|------|-------|---------|---------|
| **TRANSPORT_SPEED** | `0.4` | `TransportSubsystem.java` | 輸送帶速度 (0~1)。自動瞄準時持續運轉預送球 |
| **TO_SHOOT_SP** | `1.0` | `TransportSubsystem.java` | up_to_shoot 馬達速度 (推球進射手飛輪)。自動瞄準時只有對齊+達速後才啟動 |
| **SLOW_TRANSPORT_SPEED** | `0.2` | `TransportSubsystem.java` | 慢速吸球時的輸送帶速度 |

> 💡 **自動瞄準時 Transport 拆分控制**：
> - `transport`（輸送帶）一按下按鈕就開始跑 → 預送球到射手附近
> - `up_to_shoot`（上膛推球）等對齊 + 達速後才啟動 → 把球推進飛輪發射

#### 🚀 Swerve 加速度限制 (SlewRateLimiter)

| 參數 | 預設值 | 調整位置 | 效果說明 |
|------|-------|---------|---------|
| **xSpeedLimiter** | `20 m/s²` | `Swerve.java` 建構式 | X 平移加速度上限。越大加速越猛 |
| **ySpeedLimiter** | `20 m/s²` | `Swerve.java` 建構式 | Y 平移加速度上限 |
| **zSpeedLimiter** | `15 rad/s²` | `Swerve.java` 建構式 | 旋轉加速度上限。越大轉越快但可能不穩 |

### 9.5 手動射擊 vs 自動瞄準射擊

| | 手動射擊 (Right Trigger) | 自動瞄準射擊 (Left Bumper) |
|---|---|---|
| **觸發方式** | 按住右板機 | 按住左肩鍵 |
| **底盤行為** | 正常操控 | 旋轉鎖定（PID 接管），平移 ×0.3 防慣性射偏 |
| **旋轉控制** | 手動右搖桿 | PID 自動面向目標（不限速） |
| **瞄準目標** | 無自動瞄準 | 己方區 → Speaker；中場 → 己方回傳點 |
| **射手速度** | 固定 50 RPS | 己方區 → 依距離自動調整 (35~70 RPS)；中場 → 固定 70 RPS |
| **送球機制** | 達速後 transport + up_to_shoot 同時啟動 | transport 一直跑預送球；up_to_shoot 在對齊+達速後才啟動 |
| **連射保護** | 無 | 遲滯邏輯：首次 2°，連射中放寬到 5° |
| **適用場景** | 近距離、已手動對好角度 | 任意距離、自動對齊；中場回傳球 |

### 9.6 調參步驟建議

#### Step 1: 調 Shooter 馬達 PID

```
1. 開啟 Shuffleboard → "Shooter PID" Tab
2. 設定 kI=0, kD=0, kS=0
3. 先算 kV：量測空載最大 RPS（例如 95 RPS）
   kV = 12V / 95 = 0.126
4. 設定一個目標 RPS（例如 50），觀察 Current RPS 波形
5. 慢慢增加 kP 直到響應夠快但不震盪
6. 如果有穩態誤差 → 微調 kI（建議 0.01 起步）
7. 調好後寫回 ShooterSubsystem.java 的 TunableNumber 預設值
```

#### Step 2: 調自動瞄準旋轉 PID

```
1. 開啟 Shuffleboard → "AutoAim" Tab
2. 按住 Left Bumper 觸發自動瞄準
3. 觀察 "Angle Error" 波形
4. 如果震盪嚴重 → 降低 Rotation kP 或增加 kD
5. 如果收斂太慢 → 增加 Rotation kP
6. 調好後寫回 Constants.java → AutoAimConstants
```

#### Step 3: 校準距離-RPS 表

```
1. 把機器人放在 1m、2m、3m、4m、5m 處
2. 每個距離手動測試不同 RPS（用 Right Trigger + Shuffleboard 調整）
3. 找到每個距離能穩定進球的 RPS
4. 寫入 Constants.java → kDistanceToRpsTable
```

#### Step 4: 調容許誤差與遲滯

```
1. 觀察 Shuffleboard AutoAim Tab 的 "Aligned?" 和 "At Speed?" 指示燈
2. 如果 "Aligned?" 一直不亮 → 放寬 kRotationToleranceDeg（例如 2° → 3°）
3. 如果進球率低 → 收緊 kRotationToleranceDeg
4. 如果移動中連射頻繁中斷 → 增加 kFeedingHysteresisDeg（例如 5° → 7°）
5. 如果連射中打歪 → 減小 kFeedingHysteresisDeg（例如 5° → 3°）
6. 角度首次觸發建議：1°~3° 之間
7. 遲滯寬鬆門檻建議：3°~8° 之間
8. 射手速度建議：2~5 RPS 之間
```

---

## 7. Field2d 場地地圖

### 在 Main Tab 查看

切換到 **Main** 分頁後，`Field` Widget 會顯示：

```
┌─────────────────────────────────────────┐
│                                         │
│    [藍方]              [紅方]            │
│                                         │
│           ▶ 機器人圖示                   │  ← 箭頭方向 = 機器人朝向
│                                         │
└─────────────────────────────────────────┘
```

### 說明

- **機器人圖示位置** = 由 Limelight + IMU 融合計算的估計位置
- **機器人箭頭方向** = 陀螺儀角度
- 模擬模式下，位置從 `(0,0)` 開始，需要手動控制才會移動

### 讓機器人在地圖上移動（模擬模式）

```
1. Glass 視窗 → 切換到 Teleop
2. Glass → Joysticks 面板 → 設定模擬搖桿輸入
   - 或使用真實手把（Xbox Controller 插入電腦）
3. 在 Shuffleboard Main Tab 觀察機器人在地圖上移動
```

---

## 8. 常見問題

### Q: Shuffleboard 顯示 Disconnected？
```
✅ 確認模擬器已啟動（Glass 視窗有出現）
✅ File → Preferences → Server 設定為 localhost
✅ 重新啟動 Shuffleboard
```

### Q: 找不到我的 PID 參數 Widget？
```
✅ 左側 Sources 面板 → 展開 Shuffleboard → 找到對應 Tab
✅ 把數值欄位拖曳到畫面上
✅ 或等待幾秒，Widget 有時需要時間出現
```

### Q: 修改 kP 之後沒有反應？
```
✅ 確認機器人在 Teleop 模式（不是 Disabled）
✅ 確認有啟動對應的 Command（例如按住按鈕）
✅ 觀察波形圖，確認數值有更新
```

### Q: 波形圖沒有顯示數據？
```
✅ 右鍵 Widget → Edit Properties → 確認資料來源正確
✅ 確認馬達 Command 有在執行中
✅ 模擬模式下部分感測器值為 0 屬正常
```

### Q: 每次重開都要重新排版？
```
✅ Shuffleboard → File → Save Layout → 存成 .json 檔
✅ 下次開啟 → File → Load Layout
```

---

## 快速參考卡

```
啟動流程：
  Ctrl+Shift+P → "Simulate Robot Code" → 勾選 halsim_gui → OK
  Ctrl+Shift+P → "Start Tool" → Shuffleboard
  Glass 視窗 → 點選 "Teleop"
  Shuffleboard → 確認 Connected → 切換到對應 Tab

PID 調參：
  Tab 上直接點擊數值 → 輸入 → Enter → 即時生效
  調好後一定要寫回 Constants.java！

看場地位置：
  Main Tab → Field Widget → 機器人圖示即時更新

看迴圈超時：
  Main Tab → Loop ms Graph + Overrun 指示燈
  超過 20ms = 🔴 紅燈 = 需要優化程式碼
```

---

*最後更新：2026-03-07（新增中場區域雙目標切換邏輯）*
