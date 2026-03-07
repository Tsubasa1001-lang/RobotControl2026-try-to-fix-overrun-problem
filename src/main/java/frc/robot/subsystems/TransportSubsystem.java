package frc.robot.subsystems;

import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.VelocityVoltage;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class TransportSubsystem extends SubsystemBase {
    // 宣告兩顆馬達
    private final TalonFX up_to_shoot;
    private final TalonFX transport;

    // 建立 VelocityVoltage 閉環控制請求物件（取代原本的 DutyCycleOut 開環控制）
    // 這樣無論電池電壓多低，馬達都會維持恆定轉速
    private final VelocityVoltage velocityRequest = new VelocityVoltage(0);

    // ==========================================
    // 設定參數 (單位: RPS — rotations per second)
    // ==========================================
    // transport 輸送帶目標轉速
    //   原始 DutyCycleOut(0.4) ≈ 12V×0.4 = 4.8V → Kraken 空載~100RPS → 負載估 ~30-40 RPS
    //   TODO: 請在實際機器上量測並微調
    private static final double TRANSPORT_RPS = 35.0;

    // up_to_shoot 上膛推球目標轉速
    //   原始 DutyCycleOut(1.0) = 全速 → 負載估 ~80 RPS
    //   TODO: 請在實際機器上量測並微調
    private static final double UP_TO_SHOOT_RPS = 80.0;

    // 慢速輸送帶轉速 (原始 0.2 佔空比 → 約一半速度)
    private static final double SLOW_TRANSPORT_RPS = 18.0;
    
    // 電流限制
    private static final double STATOR_CURRENT_LIMIT = 60.0;
    private static final double SUPPLY_CURRENT_LIMIT = 40.0;

    public TransportSubsystem() {
        // 請修改為你實際的 CAN ID
        up_to_shoot = new TalonFX(26); 
        transport = new TalonFX(30);

        // ==========================================
        // up_to_shoot 馬達設定
        // ==========================================
        TalonFXConfiguration shootConfig = new TalonFXConfiguration();

        // Coast 模式：停止時自然滑行，不瞬間煞車，避免球卡住或機構衝擊
        shootConfig.MotorOutput.NeutralMode = NeutralModeValue.Coast;

        // 閉環 PID 設定 (Slot 0) — 用於 VelocityVoltage
        //   kV: 前饋，12V / ~100RPS ≈ 0.12
        //   kP: 比例增益，修正轉速誤差
        //   TODO: 請在實機上微調
        shootConfig.Slot0.kV = 0.12;
        shootConfig.Slot0.kP = 0.2;
        shootConfig.Slot0.kI = 0.0;
        shootConfig.Slot0.kD = 0.0;

        // 電流限制
        shootConfig.CurrentLimits.StatorCurrentLimitEnable = true;
        shootConfig.CurrentLimits.StatorCurrentLimit = STATOR_CURRENT_LIMIT;
        shootConfig.CurrentLimits.SupplyCurrentLimitEnable = true;
        shootConfig.CurrentLimits.SupplyCurrentLimit = SUPPLY_CURRENT_LIMIT;

        // 轉向設定
        shootConfig.MotorOutput.Inverted = InvertedValue.Clockwise_Positive;
        up_to_shoot.getConfigurator().apply(shootConfig);

        // ==========================================
        // transport 輸送帶馬達設定
        // ==========================================
        TalonFXConfiguration transportConfig = new TalonFXConfiguration();

        // Coast 模式（與原始設定一致）
        transportConfig.MotorOutput.NeutralMode = NeutralModeValue.Coast;

        // 閉環 PID 設定
        transportConfig.Slot0.kV = 0.12;
        transportConfig.Slot0.kP = 0.2;
        transportConfig.Slot0.kI = 0.0;
        transportConfig.Slot0.kD = 0.0;

        // 電流限制
        transportConfig.CurrentLimits.StatorCurrentLimitEnable = true;
        transportConfig.CurrentLimits.StatorCurrentLimit = STATOR_CURRENT_LIMIT;
        transportConfig.CurrentLimits.SupplyCurrentLimitEnable = true;
        transportConfig.CurrentLimits.SupplyCurrentLimit = SUPPLY_CURRENT_LIMIT;

        // 轉向設定（與 up_to_shoot 對向安裝）
        transportConfig.MotorOutput.Inverted = InvertedValue.CounterClockwise_Positive;
        transport.getConfigurator().apply(transportConfig);
    }

    /**
     * 同時設定兩顆馬達的目標轉速 (RPS)
     * @param transportRps transport 輸送帶目標轉速
     * @param shootRps     up_to_shoot 上膛推球目標轉速
     */
    public void setSpeed(double transportRps, double shootRps) {
        up_to_shoot.setControl(velocityRequest.withVelocity(shootRps));
        transport.setControl(velocityRequest.withVelocity(transportRps));
    }

    /**
     * 只啟動 transport 輸送帶 (閉環目標轉速)
     */
    public void setSpeed_transport() {
        transport.setControl(velocityRequest.withVelocity(TRANSPORT_RPS));
    }

    /**
     * 只啟動 up_to_shoot 上膛推球 (閉環目標轉速)
     */
    public void setSpeed_to_shoot() {
        up_to_shoot.setControl(velocityRequest.withVelocity(UP_TO_SHOOT_RPS));
    }

    /**
     * 停止兩顆馬達（Coast 自然滑行，不瞬間煞車）
     */
    public void stop() {
        up_to_shoot.stopMotor();
        transport.stopMotor();
    }

    /**
     * 啟動 Transport 送球（供外部 Command 直接呼叫）
     * 同時啟動 transport（輸送帶）和 up_to_shoot（上膛推球）
     */
    public void runTransport() {
        setSpeed(TRANSPORT_RPS, UP_TO_SHOOT_RPS);
    }

    /**
     * 停止 Transport（供外部 Command 直接呼叫）
     */
    public void stopTransport() {
        stop();
    }

    /**
     * 建立 Command: 按住按鈕時運轉，放開停止
     */
    public Command sys_runTransport() {
        return this.runEnd(
            () -> {
                // 執行動作：以閉環目標轉速運轉
                setSpeed(TRANSPORT_RPS, UP_TO_SHOOT_RPS);
            }, 
            () -> {
                // 結束動作：停止
                stop();
            }
        );
    }

    public Command sys_slowRunTransport() {
        return this.runEnd(
            () -> {
                // 執行動作：慢速，兩顆馬達同時以慢速運轉
                setSpeed(SLOW_TRANSPORT_RPS, SLOW_TRANSPORT_RPS);
            }, 
            () -> {
                // 結束動作：停止（Coast 自然滑行）
                stop();
            }
        );
    }
    
    /**
     * (選配) 反轉吐球的 Command
     */
    public Command sys_reverseTransport() {
        return this.runEnd(
            () -> setSpeed(-TRANSPORT_RPS, -UP_TO_SHOOT_RPS), 
            this::stop
        );
    }
}