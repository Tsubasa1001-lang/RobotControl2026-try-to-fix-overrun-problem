package frc.robot.subsystems;

import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.DutyCycleOut;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class TransportSubsystem extends SubsystemBase {
    // 宣告兩顆馬達
    private final TalonFX up_to_shoot;
    private final TalonFX transport;

    // 建立一個控制請求物件 (用來設定電壓百分比)
    private final DutyCycleOut outputRequest = new DutyCycleOut(0);

    // ==========================================
    // 設定參數
    // ==========================================
    // 輸送帶速度 (0.0 ~ 1.0)
    private final double TRANSPORT_SPEED = 0.4; 
    private final double TO_SHOOT_SP = 1; 
    private final double SLOW_TRANSPORT_SPEED = 0.2;
    
    // 電流限制 (輸送帶如果卡球，40A 是一個安全的保護值)
    private final double CURRENT_LIMIT = 60.0;

    public TransportSubsystem() {
        // 請修改為你實際的 CAN ID
        up_to_shoot = new TalonFX(26); 
        transport = new TalonFX(30);

        // 建立馬達設定
        TalonFXConfiguration config = new TalonFXConfiguration();

        // 1. 設定煞車模式 (Brake)：停止時立刻鎖住，防止球滑落
        config.MotorOutput.NeutralMode = NeutralModeValue.Brake;

        // 2. 設定電流限制
        config.CurrentLimits.StatorCurrentLimitEnable = true;
        config.CurrentLimits.StatorCurrentLimit = 60.0;

        // 為了避免推太大力把 Main Breaker 燒掉
        config.CurrentLimits.SupplyCurrentLimitEnable = true;
        config.CurrentLimits.SupplyCurrentLimit = 40.0; 
        // 允許它瞬間(0.5秒內)衝到 60A 沒關係
        // config.CurrentLimits.SupplyCurrentThreshold = 60.0;
        // config.CurrentLimits.SupplyTimeThreshold = 0.5;

        // ==========================================
        // 3. 設定馬達轉向 (重要！)
        // ==========================================
        // 因為沒有用 Follower，我們要個別設定它們的轉向。
        // 假設兩顆馬達是左右面對面安裝，通常一顆要反轉，一顆不用。
        
        // 設定 Left 馬達 (假設它是逆時針正轉)
        config.MotorOutput.Inverted = InvertedValue.Clockwise_Positive;
        up_to_shoot.getConfigurator().apply(config);
        config.MotorOutput.NeutralMode = NeutralModeValue.Coast;


        // 設定 Right 馬達 (假設它需要反轉，設為順時針正轉)
        // 這樣當我們給兩顆馬達都輸入 +0.5 時，它們會一起往同一個方向送球
        config.MotorOutput.Inverted = InvertedValue.CounterClockwise_Positive;
        transport.getConfigurator().apply(config);
    }

    /**
     * @param speed 速度百分比 (-1.0 ~ 1.0)
     */
    public void setSpeed(double speed, double shoot_sp) {
        // 因為我們已經在 Config 裡設定好轉向(Inverted)了
        // 所以這裡只要給一樣的正數，它們就會配合得很好
        up_to_shoot.setControl(outputRequest.withOutput(shoot_sp));
        transport.setControl(outputRequest.withOutput(speed));
    }

    /**
     * @param speed 速度百分比 (-1.0 ~ 1.0)
     */
    public void setSpeed_transport() {
        transport.setControl(outputRequest.withOutput(TRANSPORT_SPEED));
    }

    /**
     * @param speed 速度百分比 (-1.0 ~ 1.0)
     */
    public void setSpeed_to_shoot() {
        up_to_shoot.setControl(outputRequest.withOutput(TO_SHOOT_SP));
    }

    /**
     * 停止兩顆馬達
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
        setSpeed(TRANSPORT_SPEED, TO_SHOOT_SP);
    }

    /**
     * 只啟動 transport 輸送帶（預送球到待發射位置），不啟動 up_to_shoot
     * 用於自動瞄準期間：還沒對齊前先把球送到射手附近待命
     */
    public void runTransportOnly() {
        transport.setControl(outputRequest.withOutput(TRANSPORT_SPEED));
    }

    /**
     * 只啟動 up_to_shoot 上膛推球馬達（把球推入射手飛輪）
     * 用於自動瞄準對齊後：角度+射手轉速都到位時才推球發射
     */
    public void runUpToShoot() {
        up_to_shoot.setControl(outputRequest.withOutput(TO_SHOOT_SP));
    }

    /**
     * 只停止 up_to_shoot 上膛推球馬達
     * ⚠ DutyCycleOut 是 set-and-hold 模式，不主動停止它會繼續轉
     */
    public void stopUpToShoot() {
        up_to_shoot.stopMotor();
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
                // 執行動作：設定速度
                setSpeed(TRANSPORT_SPEED,TO_SHOOT_SP);
                
                // 顯示電流數據 (除錯用)
                // SmartDashboard.putNumber("Transport/Left Current", up_to_shoot.getStatorCurrent().getValueAsDouble());
                // SmartDashboard.putNumber("Transport/Right Current", transport.getStatorCurrent().getValueAsDouble());
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
                // 執行動作：設定速度
                setSpeed(SLOW_TRANSPORT_SPEED,0);
                
                // 顯示電流數據 (除錯用)
                // SmartDashboard.putNumber("Transport/Left Current", up_to_shoot.getStatorCurrent().getValueAsDouble());
                // SmartDashboard.putNumber("Transport/Right Current", transport.getStatorCurrent().getValueAsDouble());
            }, 
            () -> {
                // 結束動作：停止
                stop();
            }
        );
    }
    
    /**
     * (選配) 反轉吐球的 Command
     */
    public Command sys_reverseTransport() {
        return this.runEnd(
            () -> setSpeed(-TRANSPORT_SPEED,-TO_SHOOT_SP), 
            this::stop
        );
    }
}