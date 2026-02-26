package frc.robot.subsystems;

import edu.wpi.first.wpilibj2.command.SubsystemBase;

/**
 * DriveSubsystem - 已移除重複的 AutoBuilder.configure()
 * AutoBuilder 的配置統一在 RobotContainer 中執行，
 * 避免重複呼叫造成行為不可預測。
 */
public class DriveSubsystem extends SubsystemBase {
    public DriveSubsystem(Swerve swerve) {
        // AutoBuilder.configure() 已移至 RobotContainer 統一管理
    }
}