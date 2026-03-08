package frc.robot.commands;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.Constants;
import frc.robot.Constants.AutoAimConstants;
import frc.robot.subsystems.ShooterSubsystem;
import frc.robot.subsystems.Swerve;
import frc.robot.subsystems.TransportSubsystem;

/**
 * 自動階段用的瞄準射擊 Command（PathPlanner NamedCommand 用）
 * 
 * 與手操版 AutoAimAndShoot 的差異：
 * - addRequirements(swerve)：完全控制底盤（PathPlanner 路徑結束後才執行）
 * - 只做旋轉對齊，不做平移（機器人停在路徑終點原地轉）
 * - 有 isFinished()：射出球後或超時自動結束，讓 PathPlanner 繼續下一段
 * - 不需要 ManualDrive 的 shootingMode
 * 
 * 流程：
 * 1. 原地旋轉對齊 Hub（PID 控制）
 * 2. 同時依距離設定射手 RPS
 * 3. 角度對齊 + 射手達速 → 啟動 Transport 送球
 * 4. 送球持續 kFeedDurationSec 後自動結束
 */
public class AutoAimShootAuto extends Command {

    private final Swerve m_swerve;
    private final ShooterSubsystem m_shooter;
    private final TransportSubsystem m_transport;

    private final PIDController m_rotationPID;

    // 狀態
    private boolean m_isFeeding = false;
    private final Timer m_feedTimer = new Timer();      // 送球計時器
    private final Timer m_totalTimer = new Timer();     // 總超時計時器

    // ── 自動階段專用常數 ──
    /** 送球持續時間 (s)：開始送球後持續多久才算完成 */
    private static final double kFeedDurationSec = 0.8;
    /** 總超時 (s)：防止卡死，超過此時間強制結束 */
    private static final double kTotalTimeoutSec = 4.0;

    public AutoAimShootAuto(Swerve swerve, ShooterSubsystem shooter, TransportSubsystem transport) {
        m_swerve = swerve;
        m_shooter = shooter;
        m_transport = transport;

        m_rotationPID = new PIDController(
            AutoAimConstants.kRotation_kP,
            AutoAimConstants.kRotation_kI,
            AutoAimConstants.kRotation_kD
        );
        m_rotationPID.enableContinuousInput(-Math.PI, Math.PI);
        m_rotationPID.setTolerance(Math.toRadians(AutoAimConstants.kRotationToleranceDeg));

        // 自動階段：完全佔用 swerve + shooter + transport
        addRequirements(m_swerve, m_shooter, m_transport);
    }

    @Override
    public void initialize() {
        m_rotationPID.reset();
        m_isFeeding = false;
        m_feedTimer.stop();
        m_feedTimer.reset();
        m_totalTimer.restart();
    }

    @Override
    public void execute() {
        // 1. 取得機器人位置
        Pose2d robotPose = m_swerve.getPose();
        Translation2d robotPosition = robotPose.getTranslation();
        boolean isRed = m_swerve.isAllianceRed();

        // 2. 計算目標角度（與手操版相同邏輯）
        double targetAngleRad;
        double distanceToTarget;
        boolean isInOwnZone = AutoAimConstants.isInOwnZone(robotPosition.getX(), isRed);

        if (isInOwnZone) {
            // 己方區域：瞄準 Hub
            Translation2d hubPos = AutoAimConstants.getHubPosition(isRed);
            Translation2d toTarget = hubPos.minus(robotPosition);
            targetAngleRad = Math.atan2(toTarget.getY(), toTarget.getX())
                + AutoAimConstants.kShooterAngleOffsetRad;
            distanceToTarget = toTarget.getNorm();
        } else {
            // 中立區：朝固定角度射回己方聯盟區
            targetAngleRad = AutoAimConstants.getReturnAngleRad(isRed)
                + AutoAimConstants.kShooterAngleOffsetRad;
            distanceToTarget = AutoAimConstants.getHubPosition(isRed).minus(robotPosition).getNorm();
        }
        targetAngleRad = MathUtil.angleModulus(targetAngleRad);

        // 3. PID 旋轉對齊（只做旋轉，vx=vy=0，機器人停在原地轉）
        double currentAngleRad = robotPose.getRotation().getRadians();
        double rotationOutput = m_rotationPID.calculate(currentAngleRad, targetAngleRad);
        m_swerve.setSpeed(0, 0, rotationOutput, false);

        // 4. 依距離設定射手 RPS
        double targetRps;
        if (isInOwnZone) {
            targetRps = ShooterSubsystem.interpolateRps(distanceToTarget);
        } else {
            targetRps = AutoAimConstants.kMidFieldReturnRps;
        }
        m_shooter.setTargetVelocity(targetRps);

        // 5. 判斷是否可以送球
        double angleErrorDeg = Math.abs(Math.toDegrees(
            MathUtil.angleModulus(targetAngleRad - currentAngleRad)));

        boolean isAligned;
        if (m_isFeeding) {
            isAligned = angleErrorDeg <= AutoAimConstants.kFeedingHysteresisDeg;
        } else {
            isAligned = angleErrorDeg <= AutoAimConstants.kRotationToleranceDeg;
        }
        boolean isAtSpeed = m_shooter.isAtSpeed(targetRps, AutoAimConstants.kShooterToleranceRps);

        // 6. 對齊 + 達速 → 送球
        if (isAligned && isAtSpeed) {
            if (!m_isFeeding) {
                // 首次進入送球：啟動計時器
                m_feedTimer.restart();
                m_isFeeding = true;
            }
            m_transport.runTransport();
        } else {
            m_transport.stopTransport();
            if (!m_isFeeding) {
                // 還沒開始送球就失去對齊，重置（但如果已經在送球中就保持遲滯）
            } else if (angleErrorDeg > AutoAimConstants.kFeedingHysteresisDeg || !isAtSpeed) {
                // 送球中但偏離過大 → 暫停送球
                m_isFeeding = false;
                m_feedTimer.stop();
                m_feedTimer.reset();
            }
        }
    }

    @Override
    public void end(boolean interrupted) {
        m_swerve.setSpeed(0, 0, 0, false);
        m_shooter.stopShooter();
        m_transport.stopTransport();
        m_totalTimer.stop();
        m_feedTimer.stop();
    }

    @Override
    public boolean isFinished() {
        // 送球持續足夠時間 → 完成
        if (m_isFeeding && m_feedTimer.hasElapsed(kFeedDurationSec)) {
            return true;
        }
        // 總超時 → 強制結束
        if (m_totalTimer.hasElapsed(kTotalTimeoutSec)) {
            return true;
        }
        return false;
    }
}
