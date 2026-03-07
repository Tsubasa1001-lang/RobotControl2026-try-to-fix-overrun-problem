package frc.robot.subsystems;

import java.util.function.DoubleSupplier;

import com.ctre.phoenix6.configs.Slot0Configs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.Follower;
import com.ctre.phoenix6.controls.VelocityVoltage;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.NeutralModeValue;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import com.ctre.phoenix6.signals.MotorAlignmentValue;
import frc.robot.util.TunableNumber;

public class ShooterSubsystem extends SubsystemBase {
    private final TalonFX leaderMotor;
    private final TalonFX followerMotor;
    private final VelocityVoltage velocityRequest = new VelocityVoltage(0);

    private  double Target_RPS = 50.0;
    
    private final double TRIGGER_DEADBAND = 0.05; 

    // ── Glass 即時調參 ──
    private final TunableNumber tunableKV = new TunableNumber("Shooter/kV", 0.12);
    private final TunableNumber tunableKP = new TunableNumber("Shooter/kP", 0.12);
    private final TunableNumber tunableKI = new TunableNumber("Shooter/kI", 0.0);
    private final TunableNumber tunableKD = new TunableNumber("Shooter/kD", 0.0);

    public ShooterSubsystem() {
        leaderMotor = new TalonFX(22);
        followerMotor = new TalonFX(21);

        TalonFXConfiguration config = new TalonFXConfiguration();
        config.MotorOutput.NeutralMode = NeutralModeValue.Coast;
        
        // 記得填入你透過 Tuner X 測出的 PID 數值，否則閉環控制不會動
        config.Slot0.kV = tunableKV.get(); 
        config.Slot0.kP = tunableKP.get();
        config.Slot0.kI = tunableKI.get();
        config.Slot0.kD = tunableKD.get();

        leaderMotor.getConfigurator().apply(config);
        followerMotor.getConfigurator().apply(config);

        // 設定 Follower
        // MotorAlignmentValue.Opposed  = 對向夾擠安裝（兩顆馬達面對面），正轉方向相反，需要反轉
        // MotorAlignmentValue.SameDirection = 同向安裝，直接跟隨
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

    @Override
    public void periodic() {
        // ── 即時 PID 調參：偵測 Glass 上的數值變更，自動套用到馬達 ──
        if (tunableKV.hasChanged() || tunableKP.hasChanged() 
            || tunableKI.hasChanged() || tunableKD.hasChanged()) {
            var newSlot0 = new Slot0Configs();
            newSlot0.kV = tunableKV.get();
            newSlot0.kP = tunableKP.get();
            newSlot0.kI = tunableKI.get();
            newSlot0.kD = tunableKD.get();
            leaderMotor.getConfigurator().apply(newSlot0);
        }

        // ── 遙測數據：在 Glass 上觀察實際表現 ──
        SmartDashboard.putNumber("Shooter/Current RPS", leaderMotor.getVelocity().getValueAsDouble());
        SmartDashboard.putNumber("Shooter/Target RPS", velocityRequest.Velocity);
        SmartDashboard.putNumber("Shooter/Error RPS", 
            velocityRequest.Velocity - leaderMotor.getVelocity().getValueAsDouble());
        SmartDashboard.putNumber("Shooter/Output Voltage", leaderMotor.getMotorVoltage().getValueAsDouble());
        SmartDashboard.putNumber("Shooter/Stator Current", leaderMotor.getStatorCurrent().getValueAsDouble());
    }
}