package frc.robot.commands;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.networktables.GenericEntry;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.Constants.AutoAimConstants;
import frc.robot.subsystems.ShooterSubsystem;
import frc.robot.subsystems.Swerve;
import frc.robot.subsystems.TransportSubsystem;
import frc.robot.util.TunableNumber;

/**
 * 自動瞄準並射擊的 Command
 * 
 * 功能：
 * 1. 根據機器人目前座標，計算面向目標（Speaker）所需的旋轉角度
 * 2. 用 PID 控制底盤旋轉到正確角度（同時駕駛員仍可控制平移）
 * 3. 根據距離自動調整射手轉速
 * 4. 當角度對齊且射手達速時，自動啟動 Transport 送球發射
 * 
 * 使用方式：綁定到按鈕的 whileTrue()，放開即停止
 */
public class AutoAimAndShoot extends Command {

    private final Swerve m_swerve;
    private final ShooterSubsystem m_shooter;
    private final TransportSubsystem m_transport;

    // 旋轉 PID 控制器
    private final PIDController m_rotationPID;

    // ── 即時調參 ──
    private final TunableNumber tunableRotKP;
    private final TunableNumber tunableRotKI;
    private final TunableNumber tunableRotKD;

    // ── Shuffleboard 遙測 ──
    private GenericEntry distanceEntry, targetAngleEntry, currentAngleEntry;
    private GenericEntry angleErrorEntry, rotOutputEntry, targetRpsEntry, currentRpsEntry;
    private GenericEntry isAlignedEntry, isAtSpeedEntry, isFeedingEntry;

    // 目標位置
    private Translation2d m_targetPosition;

    // 狀態追蹤
    private boolean m_isFeeding = false;

    public AutoAimAndShoot(Swerve swerve, ShooterSubsystem shooter, TransportSubsystem transport) {
        this(swerve, shooter, transport, null);
    }

    public AutoAimAndShoot(Swerve swerve, ShooterSubsystem shooter, TransportSubsystem transport, ShuffleboardTab tab) {
        m_swerve = swerve;
        m_shooter = shooter;
        m_transport = transport;

        // ── 調參：使用 Shuffleboard Tab 或 SmartDashboard ──
        if (tab != null) {
            tunableRotKP = new TunableNumber(tab, "Rotation kP", AutoAimConstants.kRotation_kP);
            tunableRotKI = new TunableNumber(tab, "Rotation kI", AutoAimConstants.kRotation_kI);
            tunableRotKD = new TunableNumber(tab, "Rotation kD", AutoAimConstants.kRotation_kD);

            distanceEntry    = tab.add("Distance", 0).withWidget(BuiltInWidgets.kTextView).withSize(2, 1).withPosition(0, 2).getEntry();
            targetAngleEntry = tab.add("Target Angle", 0).withWidget(BuiltInWidgets.kTextView).withSize(2, 1).withPosition(2, 2).getEntry();
            currentAngleEntry= tab.add("Current Angle", 0).withWidget(BuiltInWidgets.kTextView).withSize(2, 1).withPosition(4, 2).getEntry();
            angleErrorEntry  = tab.add("Angle Error", 0).withWidget(BuiltInWidgets.kGraph).withSize(3, 2).withPosition(0, 3).getEntry();
            rotOutputEntry   = tab.add("Rot Output", 0).withWidget(BuiltInWidgets.kGraph).withSize(3, 2).withPosition(3, 3).getEntry();
            targetRpsEntry   = tab.add("Target RPS", 0).withWidget(BuiltInWidgets.kTextView).withSize(2, 1).withPosition(6, 2).getEntry();
            currentRpsEntry  = tab.add("Current RPS", 0).withWidget(BuiltInWidgets.kTextView).withSize(2, 1).withPosition(6, 3).getEntry();
            isAlignedEntry   = tab.add("Aligned?", false).withWidget(BuiltInWidgets.kBooleanBox).withSize(1, 1).withPosition(8, 2).getEntry();
            isAtSpeedEntry   = tab.add("At Speed?", false).withWidget(BuiltInWidgets.kBooleanBox).withSize(1, 1).withPosition(8, 3).getEntry();
            isFeedingEntry   = tab.add("Feeding?", false).withWidget(BuiltInWidgets.kBooleanBox).withSize(1, 1).withPosition(8, 4).getEntry();
        } else {
            tunableRotKP = new TunableNumber("AutoAim/Rotation kP", AutoAimConstants.kRotation_kP);
            tunableRotKI = new TunableNumber("AutoAim/Rotation kI", AutoAimConstants.kRotation_kI);
            tunableRotKD = new TunableNumber("AutoAim/Rotation kD", AutoAimConstants.kRotation_kD);
        }

        m_rotationPID = new PIDController(
            AutoAimConstants.kRotation_kP,
            AutoAimConstants.kRotation_kI,
            AutoAimConstants.kRotation_kD
        );
        // 角度是連續的 (-π ~ π)，避免在 ±180° 附近震盪
        m_rotationPID.enableContinuousInput(-Math.PI, Math.PI);
        m_rotationPID.setTolerance(Math.toRadians(AutoAimConstants.kRotationToleranceDeg));

        // 需要控制 swerve (透過 setAimSpeed) 和 shooter 和 transport
        addRequirements(m_shooter, m_transport);
        // 注意：不 addRequirements(m_swerve) 因為我們透過 setAimSpeed 疊加，
        // ManualDrive 仍然需要控制 swerve 的平移
    }

    @Override
    public void initialize() {
        m_rotationPID.reset();
        m_isFeeding = false;

        // 根據聯盟顏色決定目標位置
        if (m_swerve.isAllianceRed()) {
            m_targetPosition = new Translation2d(
                AutoAimConstants.kRedSpeakerX,
                AutoAimConstants.kRedSpeakerY
            );
        } else {
            m_targetPosition = new Translation2d(
                AutoAimConstants.kBlueSpeakerX,
                AutoAimConstants.kBlueSpeakerY
            );
        }
    }

    @Override
    public void execute() {
        // ── 即時 PID 調參 ──
        if (tunableRotKP.hasChanged() || tunableRotKI.hasChanged() || tunableRotKD.hasChanged()) {
            m_rotationPID.setPID(tunableRotKP.get(), tunableRotKI.get(), tunableRotKD.get());
        }

        // 1. 取得機器人目前位置
        Pose2d robotPose = m_swerve.getPose();
        Translation2d robotPosition = robotPose.getTranslation();

        // 2. 計算到目標的向量
        Translation2d toTarget = m_targetPosition.minus(robotPosition);
        double distanceToTarget = toTarget.getNorm(); // 距離 (m)

        // 3. 計算需要面向的角度 (場地座標系)
        double targetAngleRad = Math.atan2(toTarget.getY(), toTarget.getX());

        // 4. 目前機器人朝向
        double currentAngleRad = robotPose.getRotation().getRadians();

        // 5. PID 計算旋轉速度
        double rotationOutput = m_rotationPID.calculate(currentAngleRad, targetAngleRad);
        // 限制最大旋轉速度 (rad/s)
        rotationOutput = Math.max(-3.0, Math.min(3.0, rotationOutput));

        // 6. 透過 setAimSpeed 疊加旋轉（不影響駕駛員的平移控制）
        m_swerve.setAimSpeed(new edu.wpi.first.math.kinematics.ChassisSpeeds(0, 0, rotationOutput));

        // 7. 根據距離查表取得目標射手 RPS
        double targetRps = interpolateRps(distanceToTarget);

        // 8. 設定射手速度
        m_shooter.setTargetVelocity(targetRps);

        // 9. 判斷是否可以發射
        boolean isAligned = m_rotationPID.atSetpoint();
        boolean isAtSpeed = m_shooter.isAtSpeed(targetRps, AutoAimConstants.kShooterToleranceRps);

        // 10. 角度對齊 + 射手達速 → 送球
        if (isAligned && isAtSpeed) {
            if (!m_isFeeding) {
                m_isFeeding = true;
            }
            m_transport.runTransport(); // 啟動 Transport 送球
        } else {
            if (m_isFeeding) {
                m_transport.stopTransport(); // 還沒準備好就停止送球
                m_isFeeding = false;
            }
        }

        // Debug 資訊
        if (distanceEntry != null) {
            distanceEntry.setDouble(distanceToTarget);
            targetAngleEntry.setDouble(Math.toDegrees(targetAngleRad));
            currentAngleEntry.setDouble(Math.toDegrees(currentAngleRad));
            angleErrorEntry.setDouble(Math.toDegrees(targetAngleRad - currentAngleRad));
            rotOutputEntry.setDouble(rotationOutput);
            targetRpsEntry.setDouble(targetRps);
            currentRpsEntry.setDouble(m_shooter.getCurrentRps());
            isAlignedEntry.setBoolean(isAligned);
            isAtSpeedEntry.setBoolean(isAtSpeed);
            isFeedingEntry.setBoolean(m_isFeeding);
        } else {
            SmartDashboard.putNumber("AutoAim/Distance", distanceToTarget);
            SmartDashboard.putNumber("AutoAim/TargetAngle", Math.toDegrees(targetAngleRad));
            SmartDashboard.putNumber("AutoAim/CurrentAngle", Math.toDegrees(currentAngleRad));
            SmartDashboard.putNumber("AutoAim/AngleError", Math.toDegrees(targetAngleRad - currentAngleRad));
            SmartDashboard.putNumber("AutoAim/RotationOutput", rotationOutput);
            SmartDashboard.putNumber("AutoAim/TargetRPS", targetRps);
            SmartDashboard.putNumber("AutoAim/CurrentRPS", m_shooter.getCurrentRps());
            SmartDashboard.putBoolean("AutoAim/IsAligned", isAligned);
            SmartDashboard.putBoolean("AutoAim/IsAtSpeed", isAtSpeed);
            SmartDashboard.putBoolean("AutoAim/IsFeeding", m_isFeeding);
        }
    }

    @Override
    public void end(boolean interrupted) {
        // 停止所有動作
        m_swerve.setAimSpeed(new edu.wpi.first.math.kinematics.ChassisSpeeds(0, 0, 0));
        m_shooter.stopShooter();
        m_transport.stopTransport();
        m_isFeeding = false;
    }

    @Override
    public boolean isFinished() {
        // 此 Command 持續執行，靠 whileTrue 放開按鈕來結束
        return false;
    }

    /**
     * 根據距離線性內插查表取得目標 RPS
     * @param distance 到目標的距離 (m)
     * @return 目標射手 RPS
     */
    private double interpolateRps(double distance) {
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
}
