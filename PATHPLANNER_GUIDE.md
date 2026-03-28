# 🤖 PathPlanner 自動程式撰寫指南# 🤖 PathPlanner 自動程式撰寫指南



**RobotControl2026 — FRC 2026 賽季****RobotControl2026 — FRC 2026 賽季****RobotControl2026 — FRC 2026 賽季**



> 本指南說明如何使用 PathPlanner GUI 為本機器人編寫自主期（Autonomous）程式，包含路徑規劃與觸發吸球、射擊等子系統功能。



---> 本指南說明如何使用 PathPlanner GUI 為本機器人編寫自主期（Autonomous）程式，包含路徑規劃與觸發吸球、射擊等子系統功能。> 本指南說明如何使用 PathPlanner GUI 為本機器人編寫自主期（Autonomous）程式，包含路徑規劃與觸發吸球、射擊等子系統功能。



## 目錄



1. [基本概念](#基本概念)------

2. [機器人設定參數](#機器人設定參數)

3. [可用的 Named Commands](#可用的-named-commands)

4. [在 PathPlanner GUI 中建立自動程式](#在-pathplanner-gui-中建立自動程式)

5. [Auto 結構類型](#auto-結構類型)## 目錄## 目錄

6. [重要設定：resetOdom](#重要設定resetodom)

7. [自動射擊流程](#自動射擊流程)

8. [自動吸球流程](#自動吸球流程)

9. [現有自動程式範例](#現有自動程式範例)1. [基本概念](#基本概念)1. [基本概念](#基本概念)

10. [建立新路徑的步驟](#建立新路徑的步驟)

11. [常見問題與注意事項](#常見問題與注意事項)2. [機器人設定參數](#機器人設定參數)2. [機器人設定參數](#機器人設定參數)



---3. [可用的 Named Commands（命名指令）](#可用的-named-commands)3. [可用的 Named Commands（命名指令）](#可用的-named-commands)



## 基本概念4. [核心技術：邊走邊射 (Shoot on the Move)](#核心技術邊走邊射-shoot-on-the-move)4. [在 PathPlanner GUI 中建立自動程式](#在-pathplanner-gui-中建立自動程式)



### PathPlanner 與機器人的連接方式5. [在 PathPlanner GUI 中建立自動程式](#在-pathplanner-gui-中建立自動程式)5. [Auto 結構類型](#auto-結構類型)



```6. [Auto 結構類型](#auto-結構類型)6. [重要設定：resetOdom](#重要設定resetodom)

PathPlanner GUI (.auto 檔案)

        ↓7. [重要設定：resetOdom](#重要設定resetodom)7. [自動射擊流程](#自動射擊流程)

AutoBuilder.configure() ← 設定於 RobotContainer.java

        ↓8. [自動射擊流程](#自動射擊流程)8. [自動吸球流程](#自動吸球流程)

NamedCommands.registerCommand() ← 連接 GUI 指令名稱與實際 Java Command

        ↓9. [自動吸球流程](#自動吸球流程)9. [現有自動程式範例](#現有自動程式範例)

子系統執行（Shooter / Transport / IntakeRoller）

```10. [Auto 設計範例](#auto-設計範例)10. [建立新路徑的步驟](#建立新路徑的步驟)



- **路徑 (.path)**：機器人行駛的軌跡（XY 平移 + 旋轉）11. [建立新路徑的步驟](#建立新路徑的步驟)11. [常見問題與注意事項](#常見問題與注意事項)

- **Auto (.auto)**：包含路徑與命令的完整自主期程序

- **Named Command**：在 Java 中預先定義好的指令，在 PathPlanner GUI 中以「名稱字串」呼叫12. [常見問題與注意事項](#常見問題與注意事項)



------



## 機器人設定參數---



在 PathPlanner Settings 中設定好的機器人規格（供參考，**不要修改**）：## 基本概念



| 參數 | 數值 |## 基本概念

|------|------|

| 機器人寬度 | 0.845 m |### PathPlanner 與機器人的連接方式

| 機器人長度 | 0.845 m |

| 驅動模式 | Holonomic（全向） |### PathPlanner 與機器人的連接方式

| 預設最大速度 | 1.5 m/s |

| 預設最大加速度 | 1.5 m/s² |```

| 最大角速度 | 25.0 deg/s |

| 驅動馬達 | Kraken X60 |```PathPlanner GUI (.auto 檔案)

| 最高馬達速度 | 5.3 m/s |

| 機器人質量 | 52.16 kg |PathPlanner GUI (.auto 檔案)        ↓

| 輪距 | 0.546 m |

        ↓AutoBuilder.configure() ← 設定於 RobotContainer.java

---

AutoBuilder.configure() ← 設定於 RobotContainer.java        ↓

## 可用的 Named Commands

        ↓NamedCommands.registerCommand() ← 連接 GUI 指令名稱與實際 Java Command

目前在 `RobotContainer.java` 中已註冊 **3 個** 命名指令，**名稱必須完全一致（含大小寫、空格）**：

NamedCommands.registerCommand() ← 連接 GUI 指令名稱與實際 Java Command        ↓

### 🟡 吸球指令

        ↓子系統執行（Shooter / Transport / IntakeRoller）

| 指令名稱 | 功能 | 超時 | 佔用子系統 |

|----------|------|------|-----------|子系統執行（Shooter / Transport / IntakeRoller / Swerve 旋轉覆寫）```

| `Start Intake` | 吸球 + up_to_shoot 反轉擋球 | 無（持續運轉） | IntakeRoller + Transport |

| `Stop Intake` | 停止吸球與 up_to_shoot | 立即完成 | IntakeRoller + Transport |```



**`Start Intake` 詳細行為：**- **路徑 (.path)**：機器人行駛的軌跡

- IntakeRoller 以 `kIntakeTargetRps` 正轉吸球

- Transport 以 `kTransportRps` 正轉輸送- **路徑 (.path)**：機器人行駛的軌跡（XY 平移 + 旋轉）- **Auto (.auto)**：包含路徑與命令的完整自主期程序

- **up_to_shoot 以 `-kUpToShootRps` 反轉擋球** ← 防止球直接衝進射手！

- 必須搭配 `Stop Intake` 停止，或在 PathPlanner 中設為 parallel（路徑走完自動停）- **Auto (.auto)**：包含路徑與命令的完整自主期程序- **Named Command**：在 Java 中預先定義好的指令，在 PathPlanner GUI 中以「名稱字串」呼叫



### ⏫ 推球指令- **Named Command**：在 Java 中預先定義好的指令，在 PathPlanner GUI 中以「名稱字串」呼叫



| 指令名稱 | 功能 | 超時 | 佔用子系統 |---

|----------|------|------|-----------|

| `UpToShoot 2s` | up_to_shoot + transport 正轉推球 2 秒後結束 | **2 秒** | Transport |---



**`UpToShoot 2s` 詳細行為：**## 機器人設定參數

- Transport + up_to_shoot **同時正轉**，將球推入射手飛輪

- 2 秒後自動停止## 機器人設定參數

- 射手必須已在旋轉（整場比賽 `sys_idle` 預設 55 RPS 待命）

- 適合放在路徑結束後（sequential）原地推球射擊在 PathPlanner Settings 中設定好的機器人規格（供參考，**不要修改**）：



> 💡 **射手在整場比賽開始時自動旋轉**，不需要額外的射手啟動 Command。在 PathPlanner Settings 中設定好的機器人規格（供參考，**不要修改**）：

> 自主期開始 → `ShooterSubsystem` 的 `DefaultCommand (sys_idle)` 自動以 55 RPS 待命。

> 需要射擊時只需推球進去即可。| 參數 | 數值 |



---| 參數 | 數值 ||------|------|



## 在 PathPlanner GUI 中建立自動程式|------|------|| 機器人寬度 | 0.845 m |



### 步驟一：建立或選擇路徑| 機器人寬度 | 0.845 m || 機器人長度 | 0.845 m |



1. 開啟 PathPlanner 應用程式| 機器人長度 | 0.845 m || 驅動模式 | Holonomic（全向） |

2. 連接到本專案的 `src/main/deploy/pathplanner/` 目錄

3. 點擊左側 **Paths** → **新增路徑** 或開啟現有路徑| 驅動模式 | Holonomic（全向） || 預設最大速度 | 1.5 m/s |

4. 設定路徑的起點、終點與中間控制點

5. 設定 **globalConstraints**（速度/加速度）| 預設最大速度 | 1.5 m/s || 預設最大加速度 | 1.5 m/s² |

6. 設定 **goalEndState**（到達終點時的方向）

7. 儲存路徑（`.path` 檔案）| 預設最大加速度 | 1.5 m/s² || 最大角速度 | 25.0 deg/s |



### 步驟二：建立 Auto 程式| 最大角速度 | 25.0 deg/s || 驅動馬達 | Kraken X60 |



1. 點擊左側 **Autos** → **新增 Auto**| 驅動馬達 | Kraken X60 || 最高馬達速度 | 5.3 m/s |

2. 點擊畫面右側的 **+** 按鈕，加入各項元素

| 最高馬達速度 | 5.3 m/s || 機器人質量 | 52.16 kg |

### 步驟三：加入路徑

| 機器人質量 | 52.16 kg || 輪距 | 0.546 m |

- 選擇 **Follow Path**（跟隨路徑）

- 從下拉選單選擇已建立的路徑名稱| 輪距 | 0.546 m |

- 路徑可串聯，也可平行執行

---

### 步驟四：加入 Named Command

---

1. 點擊 **+** 按鈕

2. 選擇 **Named Command**## 可用的 Named Commands

3. 在名稱欄輸入指令名稱（**必須與 Java 中的名稱完全一致**）

4. 設定是否需要超時## 可用的 Named Commands



---以下是在 `RobotContainer.java` 中已註冊的所有命名指令，**名稱必須完全一致（含大小寫、空格）**：



## Auto 結構類型目前在 `RobotContainer.java` 中已註冊 **3 個** 命名指令，**名稱必須完全一致（含大小寫、空格）**：



### 1. 循序執行（Sequential Group）### 🎯 射擊指令



一個接一個按順序執行，**前一個完成後才執行下一個**：### 🎯 射擊指令



```| 指令名稱 | 功能 | 超時限制 | 說明 |

[路徑 A] → [Stop Intake] → [UpToShoot 2s] → [路徑 B] → [Start Intake]

```| 指令名稱 | 功能 | 超時 | 佔用子系統 ||----------|------|----------|------|



用途：依序射擊、移動、吸球|----------|------|------|-----------|| `Auto Shoot` | 完整自動射擊 | **4 秒** | 同時啟動射手（依距離自動計算 RPS）+ 等待達速（最多 2 秒）+ 啟動輸送帶出球 |



---| `Auto Aim Shoot` | 邊走邊瞄準射擊 | 4 秒 | Shooter + Transport（**不佔用 Swerve**） || `Far Auto Shoot` | 完整自動射擊 | **4 秒** | 功能與 `Auto Shoot` 完全相同（保留兩個名稱以相容舊 Auto 檔案） |



### 2. 平行執行（Parallel Group）| `shoot work` | 僅啟動射手 | 無超時 | 只讓飛輪轉速達到目標值，**不**觸發輸送帶（常與 `transport wait shoot` 搭配） |



多個動作**同時執行**，等所有動作完成才繼續：**`Auto Aim Shoot` 的完整流程：**| `transport wait shoot` | 等速後出球 | 無超時 | 等射手達到目標 RPS 後才啟動輸送帶（需搭配 `shoot work` 使用） |



```1. 覆寫 PathPlanner 的旋轉控制 → 自動朝向 Hub 旋轉

┌─ [路徑：去吃球] ─────────────────────────┐

│                                          │ → 都完成後繼續2. 依據距離計算射手目標 RPS（多項式查表）> **推薦使用 `Auto Shoot`**，它包含完整的射擊流程並有 4 秒超時保護。

└─ [Start Intake：吸球+反轉擋球] ──────────┘

```3. 角度對齊（≤ 2°）+ 射手達速（誤差 < 3 RPS）→ 啟動 Transport + Up-to-shoot 送球



用途：邊移動邊吸球，節省時間4. 送球 0.8 秒後自動結束，或總超時 4 秒強制結束### 🟡 吸球指令



> ⚠️ 平行群組中路徑走完後，`Start Intake` 若仍在執行會被自動取消。5. 結束時歸還旋轉控制權給 PathPlanner

> 建議路徑結束後立刻接 `Stop Intake`（sequential），確保乾淨停止。

| 指令名稱 | 功能 | 超時限制 | 說明 |

---

> **✅ 這是唯一需要的射擊指令，與手操模式使用完全相同的瞄準和測距邏輯。**|----------|------|----------|------|

### 3. 推薦的完整自動流程

| `Auto Intake` | 完整自動吸球 | **2.5 秒** | 同時啟動吸球滾輪 + 慢速輸送帶，球會進入待射位置 |

```

[起始路徑（resetOdom）]### 🟡 吸球指令| `Start Intake` | 僅啟動吸球滾輪 | 無超時 | 只啟動滾輪，不含輸送帶，需另外停止 |

    → parallel { [移動到球] + [Start Intake] }

    → [Stop Intake]| `Stop Intake` | 停止吸球滾輪 | 立即完成 | 停止 IntakeRoller |

    → [UpToShoot 2s]

    → parallel { [移動到下一顆球] + [Start Intake] }| 指令名稱 | 功能 | 超時 | 說明 |

    → [Stop Intake]

    → [UpToShoot 2s]|----------|------|------|------|> **推薦使用 `Auto Intake`**，有 2.5 秒超時保護，不會讓吸球無限持續。

```

| `Start Intake` | 開始吸球 | 無（持續運轉） | 與手操左板機完全相同，需搭配 `Stop Intake` 停止 |

---

| `Stop Intake` | 停止吸球 | 立即完成 | 停止 IntakeRoller |---

## 重要設定：resetOdom



在 `.auto` 檔案頂部設定 `"resetOdom": true`：

**吸球用法：**## 在 PathPlanner GUI 中建立自動程式

```json

{- 在進入吸球區**前**觸發 `Start Intake`

  "version": "2025.0",

  "resetOdom": true,- 離開吸球區**後**觸發 `Stop Intake`### 步驟一：建立或選擇路徑

  ...

}- 不需要同時控制 Transport，只需開關 Intake 滾輪

```

1. 開啟 PathPlanner 應用程式

**效果**：

- 自主期開始時，機器人 Odometry（位置追蹤）自動重置到第一條路徑的**起點**---2. 連接到本專案的 `src/main/deploy/pathplanner/` 目錄

- 同時同步 Pigeon2 IMU 的 Yaw 角度

- 確保自主期路徑執行位置準確3. 點擊左側 **Paths** → **新增路徑** 或開啟現有路徑



**⚠️ 強烈建議所有 Auto 都設定 `resetOdom: true`**，確保定位正確。## 核心技術：邊走邊射 (Shoot on the Move)4. 設定路徑的起點、終點與中間控制點



---5. 設定 **globalConstraints**（速度/加速度）



## 自動射擊流程### 原理6. 設定 **goalEndState**（到達終點時的方向）



使用 `UpToShoot 2s` 時的執行流程：7. 儲存路徑（`.path` 檔案）



```本系統使用 `PPHolonomicDriveController.setRotationTargetOverride()` 實現邊走邊射：

射手已在 sys_idle 旋轉（55 RPS 待命）

    ↓### 步驟二：建立 Auto 程式

UpToShoot 2s 觸發

    ↓```

Transport + up_to_shoot 同時正轉（送球進射手飛輪）

    ↓PathPlanner 路徑跟隨1. 點擊左側 **Autos** → **新增 Auto**

2 秒後自動停止 Transport

    ↓    ├─ XY 平移：PathPlanner PID 控制 ← 繼續跑路徑！2. 點擊畫面右側的 **+** 按鈕，加入各項元素

射手繼續以 sys_idle 55 RPS 待命（等待下一次射擊）

```    └─ Z 旋轉：被 ShootOnTheMove 覆寫 ← 自動朝向 Hub



> 💡 **為什麼不需要等待射手達速？**```### 步驟三：加入路徑

> 因為射手整場不停機，一直維持 55 RPS 待命。

> 當球到達時，射手已接近目標速度，推球即可射出。



---**優勢：**- 選擇 **Follow Path**（跟隨路徑）



## 自動吸球流程- 底盤完全交由 PathPlanner 控制，平移與旋轉完美融合- 從下拉選單選擇已建立的路徑名稱



使用 `Start Intake` → `Stop Intake` 的執行流程：- 不會搶奪控制權導致機器人停下或抽搐- 路徑可串聯，也可平行執行



```- 機器人可以邊移動邊瞄準，節省寶貴的自主期時間

Start Intake 觸發（通常搭配 parallel 路徑）

    ↓### 步驟四：加入 Named Command

IntakeRoller 正轉（吸球）

Transport 正轉（往機器人內部輸送）### 與手操模式的對比

up_to_shoot 反轉（擋球，防止衝入射手）

    ↓1. 點擊 **+** 按鈕

路徑完成 / 到達目標位置

    ↓| 項目 | 手操 `AutoAimAndShoot` | 自動 `ShootOnTheMove` |2. 選擇 **Named Command**

Stop Intake 觸發

    ↓|------|----------------------|----------------------|3. 在名稱欄輸入指令名稱（**必須與 Java 中的名稱完全一致**）

IntakeRoller 停止

Transport 停止| 旋轉控制 | `setAimSpeed()` 疊加 | `setRotationTargetOverride()` 覆寫 |4. 設定是否需要超時（Named Command 本身的超時，獨立於 Java 中的超時）

up_to_shoot 停止

    ↓| XY 控制 | 駕駛員手動平移 | PathPlanner 自動平移 |

（接著執行 UpToShoot 2s 推球射擊）

```| 瞄準邏輯 | ✅ 相同 | ✅ 相同 |---



---| 距離查表 RPS | ✅ 相同 | ✅ 相同 |



## 現有自動程式範例| 送球判斷（遲滯） | ✅ 相同 | ✅ 相同 |## Auto 結構類型



目前 `src/main/deploy/pathplanner/autos/` 下有以下 Auto 檔案：| 結束方式 | 放開按鈕 | 射完自動結束 / 超時 |



| 檔案名稱 | 說明 |### 1. 循序執行（Sequential Group）

|----------|------|

| `manual right auto.auto` | 右側手動路徑 |---

| `New right Auto.auto` | 右側新版自動路徑 |

| `strange Right Auto.auto` | 右側特殊路徑 |一個接一個按順序執行，**前一個完成後才執行下一個**：



> ⚠️ 以上 Auto 可能使用舊版 Named Commands（如 `Auto Aim Shoot`）。## 在 PathPlanner GUI 中建立自動程式

> 請在 PathPlanner GUI 中更新為新版指令：`Start Intake`、`Stop Intake`、`UpToShoot 2s`。

```

---

### 步驟一：建立或選擇路徑[路徑 A] → [Auto Shoot] → [路徑 B] → [Auto Shoot]

## 建立新路徑的步驟

```

1. **規劃路線**：在場地地圖上設計機器人移動路徑

2. **設定約束**：設定最大速度、加速度（建議自動時降低至 1.5 m/s）1. 開啟 PathPlanner 應用程式

3. **設定旋轉**：在路徑上設定機器人面向（goalEndState rotation）

4. **儲存路徑**：確保路徑存在 `deploy/pathplanner/paths/` 中2. 連接到本專案的 `src/main/deploy/pathplanner/` 目錄用途：射擊後再移動，確保先完成射擊再走

5. **建立 Auto**：在 PathPlanner GUI 建立 `.auto` 檔案，引用路徑並加入 Named Commands

6. **選擇 Auto**：在 Shuffleboard 的 `Main` 分頁選擇要執行的 Auto3. 點擊左側 **Paths** → **新增路徑** 或開啟現有路徑



---4. 設定路徑的起點、終點與中間控制點---



## 常見問題與注意事項5. 設定 **globalConstraints**（速度/加速度）



### ❌ Named Command 名稱打錯6. 設定 **goalEndState**（到達終點時的速度和方向）### 2. 平行執行（Parallel Group）

PathPlanner 會靜默忽略找不到的指令（不會報錯），機器人在那個時間點什麼都不做。

**解決**：確認名稱大小寫與空格完全一致。7. 儲存路徑（`.path` 檔案）



### ❌ 吸球後球直接衝進射手多個動作**同時執行**，等所有動作完成才繼續：

`Start Intake` 已內建 up_to_shoot 反轉擋球機制，正常使用不會發生。

若自行寫 Command 直接呼叫 `IntakeRoller`，記得同時讓 up_to_shoot 反轉。### 步驟二：建立 Auto 程式



### ❌ 推球後射手速度不夠```

確認機器人已 Enable（射手 DefaultCommand `sys_idle` 需要 Enable 才會執行）。

射手 idle 速度為 55 RPS，可在 Shuffleboard `Shooter` 分頁確認 `Current RPS`。1. 點擊左側 **Autos** → **新增 Auto**┌─ [路徑：去吃球] ─────────┐



### ❌ 自主期路徑跑歪2. 點擊畫面右側的 **+** 按鈕，加入各項元素│                           │ → 都完成後繼續

確認 `.auto` 有設定 `resetOdom: true`，且機器人擺放位置與路徑起點一致。

└─ [Auto Intake：吸球] ────┘

### ❌ 自動期間想同時吸球和移動

使用 **Parallel Group**，將路徑和 `Start Intake` 同時執行。### 步驟三：加入 Named Command```

路徑結束後記得加 `Stop Intake`（sequential）。



### ⚠️ UpToShoot 2s 秒數不夠

可在 `RobotContainer.java` 複製一行並改秒數：1. 點擊 **+** 按鈕用途：邊移動邊吸球，節省時間

```java

NamedCommands.registerCommand("UpToShoot 3s",2. 選擇 **Named Command**

    transport.sys_upToShootForSeconds(3.0)

);3. 在名稱欄輸入指令名稱（**必須與下表完全一致**）：> ⚠️ 平行群組中，**路徑跟隨與 Named Command 同時進行**。

```

然後在 PathPlanner GUI 使用 `UpToShoot 3s`。   - `Auto Aim Shoot` — 邊走邊瞄準射擊> 通常路徑的行駛時間 > 指令時間（2.5s），讓吸球在移動中自然完成。



### ⚠️ 射手 idle 速度需要調整   - `Start Intake` — 開始吸球

在 `Constants.java` 的 `ShooterConstants.kIdleRps` 修改（預設 55 RPS）。

目標是讓射手在 idle 速度下收到球後能快速達到射擊速度。   - `Stop Intake` — 停止吸球---




---### 3. 混合結構（Sequential + Parallel）



## Auto 結構類型現有 Auto 常見模式：



### 1. 循序執行（Sequential Group）```

[起始路徑] → [Auto Shoot] → [平行：移動到球 + Auto Intake] → [返回路徑] → [Auto Shoot]

一個接一個按順序執行，**前一個完成後才執行下一個**：```



```---

[路徑 A] → [Auto Aim Shoot] → [路徑 B] → [Auto Aim Shoot]

```## 重要設定：resetOdom



用途：到定點後停下射擊在 `.auto` 檔案頂部設定 `"resetOdom": true`：



---```json

{

### 2. 平行執行（Parallel Group）  "version": "2025.0",

  "resetOdom": true,

多個動作**同時執行**，等所有動作完成才繼續：  ...

}

``````

┌─ [路徑：去射擊位置] ─────────┐

│                               │ → 邊跑邊射！**效果**：

└─ [Auto Aim Shoot] ──────────┘- 自主期開始時，機器人 Odometry（位置追蹤）自動重置到第一條路徑的**起點**

```- 同時同步 Pigeon2 IMU 的 Yaw 角度

- 確保自主期路徑執行位置準確

用途：**邊移動邊射擊**（推薦），節省時間

**⚠️ 強烈建議所有 Auto 都設定 `resetOdom: true`**，確保定位正確。

---

---

### 3. 混合結構（推薦的完整自動流程）

## 自動射擊流程

```

[起始路徑] → [Auto Aim Shoot]使用 `Auto Shoot`（或 `Far Auto Shoot`）時，Java 內部執行流程：

    → [Start Intake] → [路徑：撿球] → [Stop Intake]

    → parallel { [路徑：去射擊位置] + [Auto Aim Shoot] }```

```Auto Shoot 觸發

    ↓

---[平行執行，總超時 4 秒]

├─ 啟動射手飛輪

## 重要設定：resetOdom│   └─ getAdaptiveRps() 計算目標轉速

│       └─ 取得 Limelight 距離 d

在 `.auto` 檔案頂部設定 `"resetOdom": true`：│       └─ RPS = -0.686d² + 13.186d + 21.912

│       └─ 限制在 35–70 RPS 之間

```json│

{└─ 等待達速（最多 2 秒）→ 啟動輸送帶出球

  "version": "2025.0",    └─ 達速後：TransportSubsystem 開始送球

  "resetOdom": true,    └─ 未達速（2 秒後）：強制繼續輸送帶

  ...    └─ 整體 4 秒後超時停止

}```

```

### 使用時機

**效果**：- 機器人在好的射擊位置**停下後**使用

- 自主期開始時，機器人 Odometry（位置追蹤）自動重置到第一條路徑的**起點**- 建議：路徑的 `goalEndState` 速度設為 `0`，讓機器人停穩再射擊

- 同時同步 Pigeon2 IMU 的 Yaw 角度

- 確保自主期路徑執行位置準確---



**⚠️ 強烈建議所有 Auto 都設定 `resetOdom: true`**，確保定位正確。## 自動吸球流程



---使用 `Auto Intake` 時，Java 內部執行流程：



## 自動射擊流程```

Auto Intake 觸發

使用 `Auto Aim Shoot` 時，Java 內部執行流程（`ShootOnTheMove.java`）：    ↓

[平行執行，總超時 2.5 秒]

```├─ IntakeRoller 啟動（吸球滾輪持續轉動）

Auto Aim Shoot 觸發└─ TransportSubsystem 慢速正轉（將球推向待射位置）

    ↓    ↓

initialize()：2.5 秒後自動停止

    啟用 PPHolonomicDriveController.setRotationTargetOverride()```

    ↓

execute()（每 20ms 循環）：### 最佳搭配：邊移動邊吸球

    ├─ 取得機器人座標

    ├─ 判斷區域（己方 → 瞄準 Hub / 中立區 → 回傳角度）```

    ├─ 計算目標角度 → 更新 RotationOverride[平行群組]

    │   └─ PathPlanner 的旋轉 PID 自動把機器人轉到目標角度├─ [路徑：移動到球的位置]  ← 路徑行駛中

    ├─ 依距離計算目標 RPS → 設定射手└─ [Auto Intake]          ← 同時吸球（2.5 秒後停止）

    │   └─ RPS = kRpsA × d² + kRpsB × d + kRpsC```

    │   └─ 限制在 kRpsMin ~ kRpsMax 之間

    ├─ 判斷角度對齊（遲滯：未送球 ≤ 2° / 送球中 ≤ 5°）> 確保路徑設計使機器人能在移動過程中通過球的位置，讓吸球有足夠時間（建議路徑 > 2 秒）。

    ├─ 判斷射手達速（誤差 < 3 RPS）

    └─ 對齊 + 達速 → 啟動 Transport + Up-to-shoot 送球---

    ↓

isFinished()：## 現有自動程式範例

    送球 0.8 秒 → 完成 ✅

    總超時 4 秒 → 強制結束 ⏰### 📋 New right Auto.auto（推薦參考）

    ↓

end()：**結構**：

    歸還旋轉控制權 → Optional.empty()```

    停止射手、停止 Transport[重置定位 resetOdom: true]

```    ↓

[路徑：1_Start] → 移動到射擊位置

### 使用時機    ↓

[Auto Shoot] → 射出預裝球（4 秒）

**邊走邊射（推薦）：**    ↓

```[平行執行]

parallel {├─ [路徑：2_Go_2_mid] → 移動到球的位置

    path: "go to shoot position"└─ [Auto Intake] → 移動時吸球（2.5 秒）

    named: "Auto Aim Shoot"    ↓

}[路徑：3_Eat_ball_and_shoot] → 移動到射擊位置（含 Auto Shoot 觸發）

```    ↓

- 機器人邊移動邊瞄準 Hub，到位後立刻射擊[Auto Shoot] → 射出吸入的球（4 秒）

- 路徑不需要把終點速度設為 0```



**停下射擊：****設計亮點**：

```- 首先射出預裝球，再去吸更多球

sequential {- 利用移動時間同步吸球

    path: "go to shoot position"    ← goalEndState.velocity = 0- 使用 `resetOdom: true` 確保定位

    named: "Auto Aim Shoot"

}---

```

- 機器人先到位停穩，再旋轉瞄準射擊### 📋 manual right auto.auto

- 更穩定但較慢

**結構**：

---```

[路徑：Straight Right Path] → 移動到射擊位置

## 自動吸球流程    ↓

[Auto Shoot] → 射擊

使用 `Start Intake` / `Stop Intake` 控制吸球：    ↓

[路徑：go right forward] → 前進到球

```    ↓

Start Intake 觸發 → IntakeRoller 以目標 RPS 持續旋轉[平行執行]

    ↓├─ [路徑：right eat ball] → 對準球

（機器人移動經過球的位置，球被吸入）└─ [Auto Intake] → 吸球

    ↓    ↓

Stop Intake 觸發 → IntakeRoller 停止[路徑：right back] → 回到射擊位置

```    ↓

[路徑：Straight Right Path] → 定位

### 最佳搭配    ↓

[Auto Shoot] → 射擊吸入的球

``````

sequential {

    named: "Start Intake"            ← 先開啟吸球---

    path: "go pick up ball"          ← 移動經過球的位置

    named: "Stop Intake"             ← 停止吸球### 📋 strange Right Auto.auto（遠距射擊版）

}

```**結構**：

```

> ⚠️ `Start Intake` 會持續運轉直到 `Stop Intake` 被呼叫，記得一定要配對使用！[重置定位 resetOdom: true]

    ↓

---[路徑：Straight Right Path] → 移動

    ↓

## Auto 設計範例[Far Auto Shoot] → 遠距射擊（功能同 Auto Shoot）

    ↓

### 📋 推薦：兩球自動（邊走邊射版）[路徑：go forward] → 前進

    ↓

```[平行執行]

resetOdom: true├─ [路徑：eat ball] → 對準球

└─ [Auto Intake] → 吸球

[路徑：start → 射擊位置]    ↓

    ↓[路徑：back and shoot] → 返回並觸發射擊

[Auto Aim Shoot]                     ← 射出預裝球    ↓

    ↓[Far Auto Shoot] → 射擊

[Start Intake]                       ← 開始吸球```

    ↓

[路徑：射擊位置 → 球的位置]          ← 邊移動邊吸球---

    ↓

[Stop Intake]                        ← 停止吸球## 建立新路徑的步驟

    ↓

parallel {                           ← 邊移動邊瞄準射擊1. **在 PathPlanner GUI 中建立路徑**

    [路徑：球的位置 → 射擊位置]   - 設定合理的速度限制（建議最大 2.0 m/s，加速度 1.5–2.0 m/s²）

    [Auto Aim Shoot]   - 設定 `goalEndState.velocity = 0.0`（停下）如果之後要射擊

}   - 設定 `goalEndState.rotation`（面向 Hub 或球的方向）

```

2. **規劃球的撿取路徑**

### 📋 推薦：三球自動   - 讓路徑通過球的位置附近（0.3–0.5m 內）

   - 路徑行駛時間建議 > 2 秒（讓 `Auto Intake` 有時間完成）

```

resetOdom: true3. **在 Auto 中組合**

   - 使用 Parallel Group 同時跑路徑和 `Auto Intake`

[路徑：start → 近球位置]   - 使用 Sequential 確保到位後再射擊

    ↓

parallel { [路徑:近球] + [Auto Aim Shoot] }  ← 邊走邊射預裝球4. **設定起始點**

    ↓   - 第一條路徑的起點 = 機器人擺放位置

[Start Intake] → [路徑：去第二球] → [Stop Intake]   - 開啟 `resetOdom: true`

    ↓

parallel { [路徑:回射擊點] + [Auto Aim Shoot] }  ← 邊走邊射第二球---

    ↓

[Start Intake] → [路徑：去第三球] → [Stop Intake]## 常見問題與注意事項

    ↓

parallel { [路徑:回射擊點] + [Auto Aim Shoot] }  ← 邊走邊射第三球### ⚠️ Named Command 名稱錯誤

```

**問題**：Auto 中的指令沒有執行

---**原因**：PathPlanner GUI 中填入的名稱與 Java 中 `NamedCommands.registerCommand()` 的名稱不符

**解決**：

## 建立新路徑的步驟

對照下表確認名稱（**區分大小寫**）：

1. **在 PathPlanner GUI 中建立路徑**

   - 設定合理的速度限制（建議最大 2.0 m/s，加速度 1.5–2.0 m/s²）```

   - 邊走邊射時 `goalEndState.velocity` 可以不設為 0✅ 正確：  Auto Shoot

   - 停下射擊時 `goalEndState.velocity = 0.0`❌ 錯誤：  auto shoot / AutoShoot / Auto  Shoot

```

2. **規劃球的撿取路徑**

   - 讓路徑通過球的位置附近（0.3–0.5m 內）---

   - 在路徑**前方**觸發 `Start Intake`，給滾輪啟動時間

### ⚠️ `Auto Shoot` 與 `Far Auto Shoot` 的差異

3. **在 Auto 中組合**

   - 射擊：用 Parallel Group 包 路徑 + `Auto Aim Shoot`（邊走邊射）兩者功能**完全相同**，都是 4 秒超時的完整射擊指令。

   - 吸球：用 Sequential 包 `Start Intake` → 路徑 → `Stop Intake``Far Auto Shoot` 是為了相容舊 Auto 程式而保留的別名。

**新程式一律使用 `Auto Shoot`**。

4. **設定起始點**

   - 第一條路徑的起點 = 機器人擺放位置---

   - 開啟 `resetOdom: true`

### ⚠️ 射擊前確認停穩

---

如果機器人還在移動時觸發 `Auto Shoot`，可能導致射擊偏差。

## 常見問題與注意事項建議：

- 前一條路徑的 `goalEndState.velocity = 0.0`

### ⚠️ Named Command 名稱錯誤- 或在路徑與 `Auto Shoot` 之間加 `waitSeconds(0.2)` 等待穩定



**問題**：Auto 中的指令沒有執行---

**原因**：PathPlanner GUI 中填入的名稱與 Java 中不符

**解決**：### ⚠️ Limelight 必須看到 Hub



````Auto Shoot` 使用 Limelight 距離計算 RPS。

✅ 正確：  Auto Aim Shoot如果 Limelight 沒有偵測到目標，距離回傳 0，RPS 會被夾限到最小值（35 RPS）。

❌ 錯誤：  auto aim shoot / AutoAimShoot / Auto  Aim  Shoot確保射擊路徑規劃時機器人朝向 Hub。



✅ 正確：  Start Intake---

❌ 錯誤：  start intake / StartIntake

### ⚠️ `Auto Intake` 超時 2.5 秒

✅ 正確：  Stop Intake

❌ 錯誤：  stop intake / StopIntake如果移動路徑很短（< 2.5 秒），`Auto Intake` 會在路徑完成前就停止。

```反之，如果路徑很長，`Auto Intake` 先停止，機器人還在繼續移動（這是正常的）。



------



### ⚠️ `Start Intake` 沒有配對 `Stop Intake`### ⚠️ `resetOdom` 需要機器人擺放位置正確



**問題**：吸球滾輪一直在轉`resetOdom: true` 會將機器人位置重置到第一條路徑的起點座標。

**原因**：`Start Intake` 不會自動停止，需要手動觸發 `Stop Intake`放置機器人時必須對準場地標記，否則所有路徑都會偏移。

**解決**：永遠成對使用

---

```

✅ [Start Intake] → [路徑] → [Stop Intake]## 快速參考卡

❌ [Start Intake] → [路徑]（忘記停止！）

``````

新自動程式 Checklist：

---□ 路徑設計：起點 = 機器人放置點

□ 射擊路徑：goalEndState.velocity = 0.0

### ⚠️ `Auto Aim Shoot` 沒射出球□ Auto 設定：resetOdom: true

□ Named Command 名稱確認：

**可能原因 1：射手沒達速**    - 射擊：Auto Shoot

- 檢查射手馬達是否正常    - 吸球：Auto Intake

- Shuffleboard 查看 Target RPS vs Current RPS□ 平行群組：移動路徑 + Auto Intake

□ 在 Shuffleboard/FMS 選擇正確 Auto

**可能原因 2：角度沒對齊**□ 測試前確認 Limelight 有目標

- 機器人旋轉是否正常```

- 檢查 PathPlanner 的 Rotation PID 是否太慢

- 可在 `Constants.java` 放寬角度容許：`kRotationToleranceDeg`（預設 2°）---



**可能原因 3：超時***最後更新：RobotControl2026 Fix 17 後*

- 4 秒內沒完成射擊 → 強制結束
- 檢查射手加速時間是否 > 2 秒

---

### ⚠️ 邊走邊射時球打偏

**可能原因：移動中角度偏差**
- `kFeedingHysteresisDeg`（預設 5°）控制送球中的容許偏差
- 如果偏差太大，可以降低路徑速度讓旋轉有時間跟上
- 或在 `Constants.java` 調整 PathPlanner Rotation PID

---

### ⚠️ `resetOdom` 需要機器人擺放位置正確

`resetOdom: true` 會將機器人位置重置到第一條路徑的起點座標。
放置機器人時必須對準場地標記，否則所有路徑都會偏移。

---

### ⚠️ 舊版 Named Command 已移除

以下 Named Command **已不再存在**，請勿在新 Auto 中使用：

| 已移除 | 替代方案 |
|--------|---------|
| ~~`Auto Shoot`~~ | 改用 `Auto Aim Shoot` |
| ~~`Far Auto Shoot`~~ | 改用 `Auto Aim Shoot` |
| ~~`shoot work`~~ | 改用 `Auto Aim Shoot`（已包含射手控制） |
| ~~`transport wait shoot`~~ | 改用 `Auto Aim Shoot`（已包含 Transport 控制） |
| ~~`Auto Intake`~~ | 改用 `Start Intake` + `Stop Intake` 配對 |

> ⚠️ **如果現有 `.auto` 檔案還在使用舊名稱，必須更新！** 未註冊的 Named Command 會被 PathPlanner 靜默跳過，不會報錯。

---

## 快速參考卡

```
═══════════════════════════════════════
  PathPlanner Named Commands（共 3 個）
═══════════════════════════════════════

  射擊：Auto Aim Shoot
        → 邊走邊瞄準 + 依距離調速 + 達速送球
        → 可 parallel 搭配路徑使用

  吸球：Start Intake  →  Stop Intake
        → 成對使用！

═══════════════════════════════════════
  新自動程式 Checklist
═══════════════════════════════════════
  □ resetOdom: true
  □ 起點 = 機器人實際擺放位置
  □ Named Command 名稱完全一致
  □ Start Intake / Stop Intake 成對使用
  □ 邊走邊射：parallel { 路徑 + Auto Aim Shoot }
  □ 在 Shuffleboard Main 頁籤選擇正確 Auto
  □ 測試前確認 Limelight 有目標
═══════════════════════════════════════
```

---

## 相關檔案位置

| 檔案 | 路徑 | 說明 |
|------|------|------|
| ShootOnTheMove.java | `src/main/java/frc/robot/commands/` | Auto 射擊 Command |
| AutoAimAndShoot.java | `src/main/java/frc/robot/commands/` | Teleop 射擊 Command |
| RobotContainer.java | `src/main/java/frc/robot/` | NamedCommand 註冊 |
| Constants.java | `src/main/java/frc/robot/` | 所有常數（RPS、角度、距離） |
| PathPlanner 路徑 | `src/main/deploy/pathplanner/paths/` | .path 檔案 |
| PathPlanner Auto | `src/main/deploy/pathplanner/autos/` | .auto 檔案 |

---

*最後更新：2025 — ShootOnTheMove 架構（RotationTargetOverride）*
