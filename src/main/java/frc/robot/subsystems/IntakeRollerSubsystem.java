package frc.robot.subsystems;

import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.VelocityVoltage;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;
import com.ctre.phoenix6.signals.MotorAlignmentValue;
import com.ctre.phoenix6.controls.Follower;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;


public class IntakeRollerSubsystem extends SubsystemBase {
    private final TalonFX leaderMotor;
    private final TalonFX followerMotor;

    // 控制模式：VelocityVoltage (閉環轉速控制)
    // 遇到阻力時 PID 會自動補償電壓來維持目標轉速
    private final VelocityVoltage velocityRequest = new VelocityVoltage(0);

    // ==========================================
    // Intake 設定
    // ==========================================
    // X44 馬達的無負載最大轉速約 6000 RPM = 100 RPS
    // Intake 吸球不需要全速，設定一個合理的目標轉速
    private static final double INTAKE_TARGET_RPS = 60.0;  // 吸球目標轉速 (RPS)
    private static final double OUTTAKE_TARGET_RPS = -30.0; // 吐球目標轉速 (RPS)

    // 電流限制 (保護馬達，但不限制電壓百分比)
    private static final double STATOR_CURRENT_LIMIT = 60.0;
    private static final double SUPPLY_CURRENT_LIMIT = 40.0;

    public IntakeRollerSubsystem() {
        leaderMotor = new TalonFX(29);
        followerMotor = new TalonFX(35);

        TalonFXConfiguration config = new TalonFXConfiguration();

        // 1. 閉環 PID 設定 (Slot 0) — 用於 VelocityVoltage
        //    kV: 前饋，讓馬達大致達到目標轉速所需的電壓
        //         計算方式: 12V / 最大RPS ≈ 12/100 = 0.12
        //    kP: 比例增益，修正轉速誤差
        //    kI: 積分增益，消除持續性誤差（例如持續的摩擦阻力）
        //    kD: 微分增益，抑制震盪
        config.Slot0.kV = 0.12;   // 前饋 (需用 Tuner X 精調)
        config.Slot0.kP = 0.2;    // 比例 — Intake 需要快速反應，比 Shooter 稍大
        config.Slot0.kI = 0.01;   // 積分 — 小量補償持續阻力
        config.Slot0.kD = 0.0;

        // 2. 電流限制
        config.CurrentLimits.StatorCurrentLimitEnable = true;
        config.CurrentLimits.StatorCurrentLimit = STATOR_CURRENT_LIMIT;
        config.CurrentLimits.SupplyCurrentLimitEnable = true;
        config.CurrentLimits.SupplyCurrentLimit = SUPPLY_CURRENT_LIMIT;

        // 3. Brake 模式：停止吸球時立刻煞車，防止球慣性滑出
        config.MotorOutput.NeutralMode = NeutralModeValue.Brake;

        // 4. 套用設定給 Leader
        leaderMotor.getConfigurator().apply(config);

        // 5. Follower 設定
        TalonFXConfiguration followerConfig = new TalonFXConfiguration();
        followerConfig.Slot0.kV = config.Slot0.kV;
        followerConfig.Slot0.kP = config.Slot0.kP;
        followerConfig.Slot0.kI = config.Slot0.kI;
        followerConfig.Slot0.kD = config.Slot0.kD;
        followerConfig.CurrentLimits.StatorCurrentLimitEnable = true;
        followerConfig.CurrentLimits.StatorCurrentLimit = STATOR_CURRENT_LIMIT;
        followerConfig.CurrentLimits.SupplyCurrentLimitEnable = true;
        followerConfig.CurrentLimits.SupplyCurrentLimit = SUPPLY_CURRENT_LIMIT;
        followerConfig.MotorOutput.NeutralMode = NeutralModeValue.Brake;
        followerConfig.MotorOutput.Inverted = InvertedValue.Clockwise_Positive;
        followerMotor.getConfigurator().apply(followerConfig);

        // 設定 Follower 跟隨 Leader (對向安裝)
        followerMotor.setControl(new Follower(leaderMotor.getDeviceID(), MotorAlignmentValue.Opposed));
    }

    /**
     * 設定滾輪目標轉速
     * @param rps 目標轉速 (rotations per second)，正值=吸球，負值=吐球
     */
    public void setVelocity(double rps) {
        leaderMotor.setControl(velocityRequest.withVelocity(rps));
    }

    public void stop() {
        leaderMotor.stopMotor();
    }

    /**
     * 取得目前實際轉速
     * @return 目前 RPS
     */
    public double getCurrentRps() {
        return leaderMotor.getVelocity().getValueAsDouble();
    }

    /**
     * Intake 吸球 Command — 按住時以目標轉速吸球，放開停止
     * 使用 VelocityVoltage 閉環控制，遇到阻力會自動加大電壓維持轉速
     */
    public Command sys_intakeWithTrigger() {
        return this.runEnd(
            () -> {
                setVelocity(INTAKE_TARGET_RPS);
            },
            () -> {
                stop();
            }
        );
    }

    /**
     * Intake 吐球 Command
     */
    public Command sys_outtake() {
        return this.runEnd(
            () -> {
                setVelocity(OUTTAKE_TARGET_RPS);
            },
            () -> {
                stop();
            }
        );
    }

    @Override
    public void periodic() {
        // 顯示實際轉速與電流，方便在 Dashboard 調整 PID
        SmartDashboard.putNumber("Intake/Actual RPS", getCurrentRps());
        SmartDashboard.putNumber("Intake/Leader Current", leaderMotor.getStatorCurrent().getValueAsDouble());
    }
}