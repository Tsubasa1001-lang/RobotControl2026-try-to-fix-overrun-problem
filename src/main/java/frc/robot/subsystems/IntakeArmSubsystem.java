package frc.robot.subsystems;

import com.ctre.phoenix6.configs.Slot0Configs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.DutyCycleOut;
import com.ctre.phoenix6.controls.Follower;
import com.ctre.phoenix6.controls.PositionVoltage;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.GravityTypeValue;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;

import edu.wpi.first.networktables.GenericEntry;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import com.ctre.phoenix6.signals.MotorAlignmentValue;
import frc.robot.Constants.IntakeArmConstants;
import frc.robot.util.TunableNumber;

import java.util.function.DoubleSupplier;

public class IntakeArmSubsystem extends SubsystemBase {
    private final TalonFX leaderMotor;
    private final TalonFX followerMotor;

    private final PositionVoltage positionRequest = new PositionVoltage(0).withSlot(0);
    private final DutyCycleOut manualRequest = new DutyCycleOut(0);

    // ── Shuffleboard 即時調參 ──
    private TunableNumber tunableKP;
    private TunableNumber tunableKI;
    private TunableNumber tunableKD;
    private TunableNumber tunableKG;

    // ── Shuffleboard 遙測 ──
    private GenericEntry armRotationsEntry;
    private GenericEntry motorRotationsEntry;
    private GenericEntry outputVoltageEntry;
    private GenericEntry statorCurrentEntry;

    public IntakeArmSubsystem() {
        this(null);
    }

    public IntakeArmSubsystem(ShuffleboardTab tab) {
        leaderMotor = new TalonFX(IntakeArmConstants.kLeaderMotorID);
        followerMotor = new TalonFX(IntakeArmConstants.kFollowerMotorID);

        // ── 初始化可調參數 ──
        if (tab != null) {
            tunableKP = new TunableNumber(tab, "kP", IntakeArmConstants.kDefaultKP);
            tunableKI = new TunableNumber(tab, "kI", IntakeArmConstants.kDefaultKI);
            tunableKD = new TunableNumber(tab, "kD", IntakeArmConstants.kDefaultKD);
            tunableKG = new TunableNumber(tab, "kG", IntakeArmConstants.kDefaultKG);

            armRotationsEntry = tab.add("Arm Rotations", 0)
                .withWidget(BuiltInWidgets.kGraph)
                .withSize(3, 2).withPosition(0, 2).getEntry();
            motorRotationsEntry = tab.add("Motor Rotations", 0)
                .withWidget(BuiltInWidgets.kTextView)
                .withSize(1, 1).withPosition(3, 2).getEntry();
            outputVoltageEntry = tab.add("Output V", 0)
                .withWidget(BuiltInWidgets.kGraph)
                .withSize(3, 2).withPosition(4, 2).getEntry();
            statorCurrentEntry = tab.add("Stator A", 0)
                .withWidget(BuiltInWidgets.kTextView)
                .withSize(1, 1).withPosition(3, 3).getEntry();
        } else {
            tunableKP = new TunableNumber("IntakeArm/kP", IntakeArmConstants.kDefaultKP);
            tunableKI = new TunableNumber("IntakeArm/kI", IntakeArmConstants.kDefaultKI);
            tunableKD = new TunableNumber("IntakeArm/kD", IntakeArmConstants.kDefaultKD);
            tunableKG = new TunableNumber("IntakeArm/kG", IntakeArmConstants.kDefaultKG);
        }

        TalonFXConfiguration config = new TalonFXConfiguration();

        // 1. 設定為 Brake (煞車) 模式：手臂這類抗重力機構，絕對要用 Brake，不然沒電會掉下來砸壞東西
        config.MotorOutput.NeutralMode = NeutralModeValue.Brake;
        
        // 2. 設定軟體限位 (Soft Limits) - 這是保護機構的關鍵
        // 為了安全，剛開始測試建議先關閉，等確認方向與範圍後再開啟
        
        config.SoftwareLimitSwitch.ForwardSoftLimitEnable = true;
        config.SoftwareLimitSwitch.ForwardSoftLimitThreshold = 8.0; // 例如轉 10 圈是極限
        config.SoftwareLimitSwitch.ReverseSoftLimitEnable = true;
        config.SoftwareLimitSwitch.ReverseSoftLimitThreshold = 0.0;  // 0 圈是收起位置
        

        // 3. PID 設定 (這些數值需要透過 Tuner X 調整)
        // kG (重力補償): 對於手臂非常重要，這是為了抵抗重力所需的最小電壓
        // kP (比例控制): 這是讓手臂準確停在目標位置的拉力
        config.Slot0.kP = tunableKP.get();
        config.Slot0.kI = tunableKI.get();
        config.Slot0.kD = tunableKD.get();
        config.Slot0.kG = tunableKG.get();
        config.Slot0.GravityType = GravityTypeValue.Arm_Cosine; // 告訴馬達這是一個旋轉手臂

        // 套用設定
        leaderMotor.getConfigurator().apply(config);
        followerMotor.getConfigurator().apply(config);

        // 4. 設定 Follower
        // 假設兩顆馬達是對裝 (面對面)，通常需要反轉其中一顆
        // 請先在 Tuner X 確認兩顆馬達是否互斥，如果互斥就把 true 改 false，或改 Config 的 Inverted
        followerMotor.setControl(new Follower(leaderMotor.getDeviceID(), MotorAlignmentValue.Opposed));
        
        // 歸零：假設機器人啟動時，Intake 是處於「收起」的狀態 (0度)
        leaderMotor.setPosition(0);
    }

    /**
     * 手動控制模式 (測試用)
     * @param speed -1.0 到 1.0 的速度 (來自搖桿)
     */
    public void setManualSpeed(double speed) {
        // 為了安全，我們可以把最大速度限制在 30% 以內，避免測試時打壞機構
        double safeSpeed = speed * 0.30; 
        leaderMotor.setControl(manualRequest.withOutput(safeSpeed));
    }

    /**
     * 自動控制模式 (前往指定角度)
     * @param armRotations 目標是 Intake 手臂轉幾圈 (不是馬達轉幾圈)
     */
    public void setTargetPosition(double armRotations) {
        // 公式：馬達目標圈數 = 手臂目標圈數 * 減速比
        double motorTargetRotations = armRotations * IntakeArmConstants.kGearRatio;
        leaderMotor.setControl(positionRequest.withPosition(motorTargetRotations));
    }

    public void stop() {
        leaderMotor.stopMotor();
    }

    // 這是給 RobotContainer 用的 Command：透過搖桿調整位置
    public Command sys_manualMove(DoubleSupplier joystickAxis) {
        return run(() -> {
            // 讀取搖桿數值 (通常會有死區處理)
            double axis = joystickAxis.getAsDouble();
            if (Math.abs(axis) < 0.1) axis = 0;
            
            setManualSpeed(axis);
        });
    }

    @Override
    public void periodic() {
        // ── 即時 PID 調參 ──
        if (tunableKP.hasChanged() || tunableKI.hasChanged() 
            || tunableKD.hasChanged() || tunableKG.hasChanged()) {
            var newSlot0 = new Slot0Configs();
            newSlot0.kP = tunableKP.get();
            newSlot0.kI = tunableKI.get();
            newSlot0.kD = tunableKD.get();
            newSlot0.kG = tunableKG.get();
            newSlot0.GravityType = GravityTypeValue.Arm_Cosine;
            leaderMotor.getConfigurator().apply(newSlot0);
            followerMotor.getConfigurator().apply(newSlot0);
        }

        // ── 遙測數據 ──
        double motorRotations = leaderMotor.getPosition().getValueAsDouble();
        double armRotations = motorRotations / IntakeArmConstants.kGearRatio;
        double outputV = leaderMotor.getMotorVoltage().getValueAsDouble();
        double statorA = leaderMotor.getStatorCurrent().getValueAsDouble();

        if (armRotationsEntry != null) {
            armRotationsEntry.setDouble(armRotations);
            motorRotationsEntry.setDouble(motorRotations);
            outputVoltageEntry.setDouble(outputV);
            statorCurrentEntry.setDouble(statorA);
        } else {
            SmartDashboard.putNumber("IntakeArm/Arm Rotations", armRotations);
            SmartDashboard.putNumber("IntakeArm/Motor Rotations", motorRotations);
            SmartDashboard.putNumber("IntakeArm/Output Voltage", outputV);
            SmartDashboard.putNumber("IntakeArm/Stator Current", statorA);
        }
    }
}