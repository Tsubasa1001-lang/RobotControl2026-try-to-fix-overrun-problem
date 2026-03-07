package frc.robot.subsystems;

import java.util.Map;
import java.util.function.DoubleSupplier;

import com.ctre.phoenix6.configs.Slot0Configs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.Follower;
import com.ctre.phoenix6.controls.VelocityVoltage;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.NeutralModeValue;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.networktables.GenericEntry;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import com.ctre.phoenix6.signals.MotorAlignmentValue;
import frc.robot.Constants.AutoAimConstants;
import frc.robot.Constants.ShooterConstants;
import frc.robot.util.TunableNumber;

public class ShooterSubsystem extends SubsystemBase {
    private final TalonFX leaderMotor;
    private final TalonFX followerMotor;
    private final VelocityVoltage velocityRequest = new VelocityVoltage(0);

    // ── Shuffleboard 即時調參 ──
    private TunableNumber tunableKV;
    private TunableNumber tunableKP;
    private TunableNumber tunableKI;
    private TunableNumber tunableKD;
    private TunableNumber tunableKS;

    // ── Shuffleboard 遙測 ──
    private GenericEntry currentRpsEntry;
    private GenericEntry targetRpsEntry;
    private GenericEntry errorRpsEntry;
    private GenericEntry outputVoltageEntry;
    private GenericEntry statorCurrentEntry;

    public ShooterSubsystem() {
        this(null);
    }

    public ShooterSubsystem(ShuffleboardTab tab) {
        leaderMotor = new TalonFX(ShooterConstants.kLeaderMotorID);
        followerMotor = new TalonFX(ShooterConstants.kFollowerMotorID);

        // ── 初始化可調參數 ──
        if (tab != null) {
            tunableKV = new TunableNumber(tab, "kV", ShooterConstants.kDefaultKV);
            tunableKP = new TunableNumber(tab, "kP", ShooterConstants.kDefaultKP);
            tunableKI = new TunableNumber(tab, "kI", ShooterConstants.kDefaultKI);
            tunableKD = new TunableNumber(tab, "kD", ShooterConstants.kDefaultKD);
            tunableKS = new TunableNumber(tab, "kS", ShooterConstants.kDefaultKS);

            // ── 遙測示波圖 ──
            currentRpsEntry = tab.add("Current RPS", 0)
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
            statorCurrentEntry = tab.add("Stator A", 0)
                .withWidget(BuiltInWidgets.kTextView)
                .withSize(1, 1).withPosition(3, 3).getEntry();
        } else {
            tunableKV = new TunableNumber("Shooter/kV", ShooterConstants.kDefaultKV);
            tunableKP = new TunableNumber("Shooter/kP", ShooterConstants.kDefaultKP);
            tunableKI = new TunableNumber("Shooter/kI", ShooterConstants.kDefaultKI);
            tunableKD = new TunableNumber("Shooter/kD", ShooterConstants.kDefaultKD);
            tunableKS = new TunableNumber("Shooter/kS", ShooterConstants.kDefaultKS);
        }

        TalonFXConfiguration config = new TalonFXConfiguration();
        config.MotorOutput.NeutralMode = NeutralModeValue.Coast;
        
        config.Slot0.kV = tunableKV.get(); 
        config.Slot0.kP = tunableKP.get();
        config.Slot0.kI = tunableKI.get();
        config.Slot0.kD = tunableKD.get();
        config.Slot0.kS = tunableKS.get();

        leaderMotor.getConfigurator().apply(config);
        followerMotor.getConfigurator().apply(config);

        followerMotor.setControl(new Follower(leaderMotor.getDeviceID(), MotorAlignmentValue.Opposed));
    }

    private void setVelocity(double rps){  
        leaderMotor.setControl(velocityRequest.withVelocity(rps));
    }

    private void stop() {
        leaderMotor.stopMotor();
        followerMotor.stopMotor();
    }

    /**
     * 設定射手馬達目標速度 (public，供外部 Command 呼叫)
     * @param rps 目標轉速 (rotations per second)
     */
    public void setTargetVelocity(double rps) {
        setVelocity(rps);
    }

    /**
     * 停止射手馬達 (public，供外部 Command 呼叫)
     */
    public void stopShooter() {
        stop();
    }

    /**
     * 取得目前實際轉速
     * @return 目前 RPS
     */
    public double getCurrentRps() {
        return leaderMotor.getVelocity().getValueAsDouble();
    }

    /**
     * 建立一個手動控制 Shooter 的 Command
     * 這是最安全的寫法，因為當 Command 結束(手指放開)時，會自動呼叫 stop()
     * 
     * @param triggerInput 來自手把板機的輸入 (0.0 到 1.0)
     * @return 控制 Shooter 的 Command
     */
    public Command sys_manualShoot(int TargetRPS) {
        // 使用 runEnd: 一直執行 execute 裡的程式，直到被打斷或放開後執行 end 裡的 stop
        return this.runEnd(
            () -> {
                // 1. 讀取板機數值
                // double rawValue = triggerInput.getAsDouble();

                // 2. 套用死區 (Deadband)
                // 如果 rawValue 小於 0.05，就回傳 0，否則回傳 rawValue
                // double adjustedValue = MathUtil.applyDeadband(rawValue, TRIGGER_DEADBAND);

                // 3. 計算目標速度
                // 將 0.0~1.0 的數值 映射到 0 ~ Target_RPS
                // double targetRps = adjustedValue * Target_RPS;

                // 4. 執行
                setVelocity(TargetRPS);

                // 顯示目前目標與實際速度在 Dashboard 方便除錯
                // SmartDashboard.putNumber("Shooter/Target RPS", TargetRPS);
            },
            () -> {
                // Command 結束時強制停止馬達
                stop();
                // SmartDashboard.putNumber("Shooter/Target RPS", 0);
            }
        );
    }

    /**
     * 檢查目前轉速是否達到目標 (允許一點點誤差)
     * @param targetRps 目標轉速
     * @param tolerance 容許誤差
     * @return true 代表速度夠了，可以發射
     */
    public boolean isAtSpeed(double targetRps, double tolerance) {
        // 取得目前實際速度
        double currentRps = leaderMotor.getVelocity().getValueAsDouble();
        return Math.abs(currentRps - targetRps) < tolerance;
    }
    /**
     * 回傳目前是否達到指定速度 (使用預設誤差 5 RPS)
     * 這是為了讓 Command 寫起來簡潔一點
     */
    public boolean isAtSpeed(int targetRps) {
        return isAtSpeed(targetRps, 5.0); // 預設容許誤差 5 RPS
    }

    /**
     * 根據距離線性內插查表取得目標 RPS
     * 使用 AutoAimConstants.kDistanceToRpsTable 進行查表
     * @param distance 到目標的距離 (m)
     * @return 目標射手 RPS
     */
    public static double interpolateRps(double distance) {
        double[][] table = AutoAimConstants.kDistanceToRpsTable;

        // 距離小於表中最小值 → 回傳最小 RPS
        if (distance <= table[0][0]) {
            return table[0][1];
        }
        // 距離大於表中最大值 → 回傳最大 RPS
        if (distance >= table[table.length - 1][0]) {
            return table[table.length - 1][1];
        }

        // 線性內插
        for (int i = 0; i < table.length - 1; i++) {
            if (distance >= table[i][0] && distance <= table[i + 1][0]) {
                double ratio = (distance - table[i][0]) / (table[i + 1][0] - table[i][0]);
                return table[i][1] + ratio * (table[i + 1][1] - table[i][1]);
            }
        }

        // 理論上不會到這裡
        return table[table.length - 1][1];
    }

    @Override
    public void periodic() {
        // ── 即時 PID 調參：偵測 Shuffleboard 上的數值變更，自動套用到馬達 ──
        if (tunableKV.hasChanged() || tunableKP.hasChanged() 
            || tunableKI.hasChanged() || tunableKD.hasChanged()
            || tunableKS.hasChanged()) {
            var newSlot0 = new Slot0Configs();
            newSlot0.kV = tunableKV.get();
            newSlot0.kP = tunableKP.get();
            newSlot0.kI = tunableKI.get();
            newSlot0.kD = tunableKD.get();
            newSlot0.kS = tunableKS.get();
            leaderMotor.getConfigurator().apply(newSlot0);
        }

        // ── 遙測數據 ──
        double currentRps = leaderMotor.getVelocity().getValueAsDouble();
        double targetRps = velocityRequest.Velocity;
        double errorRps = targetRps - currentRps;
        double outputV = leaderMotor.getMotorVoltage().getValueAsDouble();
        double statorA = leaderMotor.getStatorCurrent().getValueAsDouble();

        if (currentRpsEntry != null) {
            // Shuffleboard 模式
            currentRpsEntry.setDouble(currentRps);
            targetRpsEntry.setDouble(targetRps);
            errorRpsEntry.setDouble(errorRps);
            outputVoltageEntry.setDouble(outputV);
            statorCurrentEntry.setDouble(statorA);
        } else {
            // SmartDashboard 後備
            SmartDashboard.putNumber("Shooter/Current RPS", currentRps);
            SmartDashboard.putNumber("Shooter/Target RPS", targetRps);
            SmartDashboard.putNumber("Shooter/Error RPS", errorRps);
            SmartDashboard.putNumber("Shooter/Output Voltage", outputV);
            SmartDashboard.putNumber("Shooter/Stator Current", statorA);
        }
    }
}