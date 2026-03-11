package frc.robot.commands;

import java.util.Optional;

import com.pathplanner.lib.controllers.PPHolonomicDriveController;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.Constants.AutoAimConstants;
import frc.robot.subsystems.ShooterSubsystem;
import frc.robot.subsystems.Swerve;
import frc.robot.subsystems.TransportSubsystem;

/**
 * 邊走邊瞄準射擊 Command（PathPlanner Auto 專用）
 * 
 * 核心原理：
 * - 使用 PPHolonomicDriveController.setRotationTargetOverride() 覆寫 PathPlanner 的旋轉控制
 * - PathPlanner 繼續控制 XY 平移（跑路徑），此 Command 只接管旋轉軸
 * - 不 addRequirements(swerve)，所以不會中斷 PathPlanner 的路徑跟隨
 * 
 * 與手操版 AutoAimAndShoot 共用相同邏輯：
 * - 依距離查表 RPS
 * - 遲滯防抖送球
 * - 角度對齊 + 達速 → 自動送球
 * 
 * 使用方式（PathPlanner GUI）：
 * - 在路徑上放 "Auto Aim Shoot" NamedCommand
 * - 可與路徑平行執行（parallel），機器人邊跑邊瞄準
 * - 也可在路徑結束後單獨執行（sequential），原地旋轉射擊
 */
public class ShootOnTheMove extends Command {

    private final Swerve m_swerve;
    private final ShooterSubsystem m_shooter;
    private final TransportSubsystem m_transport;

    // 狀態
    private boolean m_isFeeding = false;
    private final Timer m_feedTimer = new Timer();
    private final Timer m_totalTimer = new Timer();

    // 目標角度（每週期更新，供 RotationOverride lambda 讀取）
    private volatile Rotation2d m_targetRotation = new Rotation2d();

    // ── 自動階段專用常數 ──
    /** 送球持續時間 (s) */
    private static final double kFeedDurationSec = 0.8;
    /** 總超時 (s) */
    private static final double kTotalTimeoutSec = 4.0;

    public ShootOnTheMove(Swerve swerve, ShooterSubsystem shooter, TransportSubsystem transport) {
        m_swerve = swerve;
        m_shooter = shooter;
        m_transport = transport;

        // 只佔用 shooter 和 transport，不佔用 swerve！
        // 讓 PathPlanner 繼續控制底盤 XY 移動
        addRequirements(m_shooter, m_transport);
    }

    @Override
    public void initialize() {
        m_isFeeding = false;
        m_feedTimer.stop();
        m_feedTimer.reset();
        m_totalTimer.restart();

        // ── 啟用旋轉覆寫：告訴 PathPlanner「旋轉由我控制」──
        PPHolonomicDriveController.setRotationTargetOverride(
            () -> Optional.of(m_targetRotation)
        );
    }

    @Override
    public void execute() {
        // 1. 取得機器人位置
        Pose2d robotPose = m_swerve.getPose();
        Translation2d robotPosition = robotPose.getTranslation();
        boolean isRed = m_swerve.isAllianceRed();

        // 2. 計算目標角度（與手操版 AutoAimAndShoot 完全相同的邏輯）
        double targetAngleRad;
        double distanceToTarget;
        boolean isInOwnZone = AutoAimConstants.isInOwnZone(robotPosition.getX(), isRed);

        if (isInOwnZone) {
            Translation2d hubPos = AutoAimConstants.getHubPosition(isRed);
            Translation2d toTarget = hubPos.minus(robotPosition);
            targetAngleRad = Math.atan2(toTarget.getY(), toTarget.getX())
                + AutoAimConstants.kShooterAngleOffsetRad;
            distanceToTarget = toTarget.getNorm();
        } else {
            // 中立區：機器人正面朝向己方聯盟區（不加射手偏移）
            targetAngleRad = AutoAimConstants.getReturnAngleRad(isRed);
            distanceToTarget = AutoAimConstants.getHubPosition(isRed).minus(robotPosition).getNorm();
        }
        targetAngleRad = MathUtil.angleModulus(targetAngleRad);

        // 3. 更新旋轉覆寫目標（PathPlanner 的 PID 會自動把機器人轉到這個角度）
        m_targetRotation = Rotation2d.fromRadians(targetAngleRad);

        // 4. 依距離設定射手 RPS
        double targetRps;
        if (isInOwnZone) {
            targetRps = m_shooter.interpolateRps(distanceToTarget);
        } else {
            targetRps = AutoAimConstants.kMidFieldReturnRps;
        }
        m_shooter.setTargetVelocity(targetRps);

        // 5. 判斷是否可以送球（與手操版相同的遲滯邏輯）
        double currentAngleRad = robotPose.getRotation().getRadians();
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
                m_feedTimer.restart();
                m_isFeeding = true;
            }
            m_transport.runTransport();
        } else {
            m_transport.stopTransport();
            if (m_isFeeding && (angleErrorDeg > AutoAimConstants.kFeedingHysteresisDeg || !isAtSpeed)) {
                m_isFeeding = false;
                m_feedTimer.stop();
                m_feedTimer.reset();
            }
        }
    }

    @Override
    public void end(boolean interrupted) {
        // ── 歸還旋轉控制權給 PathPlanner ──
        PPHolonomicDriveController.setRotationTargetOverride(() -> Optional.empty());

        m_shooter.stopShooter();
        m_transport.stopTransport();
        m_totalTimer.stop();
        m_feedTimer.stop();
    }

    @Override
    public boolean isFinished() {
        // 送球夠久 → 完成
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
