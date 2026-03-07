package frc.robot.subsystems;

import com.ctre.phoenix6.configs.Slot0Configs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.VelocityVoltage;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;
import com.ctre.phoenix6.signals.MotorAlignmentValue;
import com.ctre.phoenix6.controls.Follower;

import edu.wpi.first.networktables.GenericEntry;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.util.TunableNumber;


public class IntakeRollerSubsystem extends SubsystemBase {
    private final TalonFX leaderMotor;
    private final TalonFX followerMotor;

    private final VelocityVoltage velocityRequest = new VelocityVoltage(0);

    private static final double INTAKE_TARGET_RPS = 60.0;
    private static final double OUTTAKE_TARGET_RPS = -30.0;

    private static final double STATOR_CURRENT_LIMIT = 60.0;
    private static final double SUPPLY_CURRENT_LIMIT = 40.0;

    // ── Shuffleboard 即時調參 ──
    private TunableNumber tunableKV;
    private TunableNumber tunableKP;
    private TunableNumber tunableKI;
    private TunableNumber tunableKD;

    // ── Shuffleboard 遙測 ──
    private GenericEntry actualRpsEntry;
    private GenericEntry targetRpsEntry;
    private GenericEntry errorRpsEntry;
    private GenericEntry leaderCurrentEntry;
    private GenericEntry outputVoltageEntry;

    public IntakeRollerSubsystem() {
        this(null);
    }

    public IntakeRollerSubsystem(ShuffleboardTab tab) {
        leaderMotor = new TalonFX(29);
        followerMotor = new TalonFX(35);

        // ── 初始化可調參數 ──
        if (tab != null) {
            tunableKV = new TunableNumber(tab, "kV", 0.12);
            tunableKP = new TunableNumber(tab, "kP", 0.2);
            tunableKI = new TunableNumber(tab, "kI", 0.01);
            tunableKD = new TunableNumber(tab, "kD", 0.0);

            actualRpsEntry = tab.add("Actual RPS", 0)
                .withWidget(BuiltInWidgets.kGraph)
                .withSize(3, 2).withPosition(0, 2).getEntry();
            targetRpsEntry = tab.add("Target RPS", 0)
                .withWidget(BuiltInWidgets.kTextView)
                .withSize(1, 1).withPosition(3, 2).getEntry();
            errorRpsEntry = tab.add("Error RPS", 0)
                .withWidget(BuiltInWidgets.kTextView)
                .withSize(1, 1).withPosition(4, 2).getEntry();
            outputVoltageEntry = tab.add("Output V", 0)
                .withWidget(BuiltInWidgets.kGraph)
                .withSize(3, 2).withPosition(5, 2).getEntry();
            leaderCurrentEntry = tab.add("Leader A", 0)
                .withWidget(BuiltInWidgets.kTextView)
                .withSize(1, 1).withPosition(3, 3).getEntry();
        } else {
            tunableKV = new TunableNumber("IntakeRoller/kV", 0.12);
            tunableKP = new TunableNumber("IntakeRoller/kP", 0.2);
            tunableKI = new TunableNumber("IntakeRoller/kI", 0.01);
            tunableKD = new TunableNumber("IntakeRoller/kD", 0.0);
        }

        TalonFXConfiguration config = new TalonFXConfiguration();

        // 1. 閉環 PID 設定 (Slot 0) — 用於 VelocityVoltage
        //    kV: 前饋，讓馬達大致達到目標轉速所需的電壓
        //         計算方式: 12V / 最大RPS ≈ 12/100 = 0.12
        //    kP: 比例增益，修正轉速誤差
        //    kI: 積分增益，消除持續性誤差（例如持續的摩擦阻力）
        //    kD: 微分增益，抑制震盪
        config.Slot0.kV = tunableKV.get();
        config.Slot0.kP = tunableKP.get();
        config.Slot0.kI = tunableKI.get();
        config.Slot0.kD = tunableKD.get();

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
        followerConfig.Slot0.kV = tunableKV.get();
        followerConfig.Slot0.kP = tunableKP.get();
        followerConfig.Slot0.kI = tunableKI.get();
        followerConfig.Slot0.kD = tunableKD.get();
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
        // ── 即時 PID 調參 ──
        if (tunableKV.hasChanged() || tunableKP.hasChanged() 
            || tunableKI.hasChanged() || tunableKD.hasChanged()) {
            var newSlot0 = new Slot0Configs();
            newSlot0.kV = tunableKV.get();
            newSlot0.kP = tunableKP.get();
            newSlot0.kI = tunableKI.get();
            newSlot0.kD = tunableKD.get();
            leaderMotor.getConfigurator().apply(newSlot0);
            followerMotor.getConfigurator().apply(newSlot0);
        }

        // ── 遙測數據 ──
        double actualRps = getCurrentRps();
        double targetRps = velocityRequest.Velocity;
        double errorRps = targetRps - actualRps;
        double outputV = leaderMotor.getMotorVoltage().getValueAsDouble();
        double leaderA = leaderMotor.getStatorCurrent().getValueAsDouble();

        if (actualRpsEntry != null) {
            actualRpsEntry.setDouble(actualRps);
            targetRpsEntry.setDouble(targetRps);
            errorRpsEntry.setDouble(errorRps);
            outputVoltageEntry.setDouble(outputV);
            leaderCurrentEntry.setDouble(leaderA);
        } else {
            SmartDashboard.putNumber("IntakeRoller/Actual RPS", actualRps);
            SmartDashboard.putNumber("IntakeRoller/Target RPS", targetRps);
            SmartDashboard.putNumber("IntakeRoller/Error RPS", errorRps);
            SmartDashboard.putNumber("IntakeRoller/Leader Current", leaderA);
            SmartDashboard.putNumber("IntakeRoller/Output Voltage", outputV);
        }
    }
}