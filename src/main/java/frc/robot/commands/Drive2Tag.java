package frc.robot.commands;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.LimelightHelpers;
import frc.robot.subsystems.Swerve;

public class Drive2Tag extends Command {
    private final Swerve m_swerve;
    private final String m_limelightName;

    // 定義 PID Controller
    private final PIDController xController = new PIDController(0.8, 0.001, 0); // 需調整 kP
    private final PIDController yController = new PIDController(0.25, 0.001, 0); // 需調整 kP
    private final PIDController thetaController = new PIDController(0.008, 0, 0); // 需調整 kP

    // 新增變數：用來存儲我們想要的目標位置
    private final double m_targetX;
    private final double m_targetY;
    private final double m_targetYaw;

    /**
     * 建構子
     * @param swerve 底盤
     * @param limelightName Limelight 名稱
     * @param targetX 目標前後距離 (公尺) - 例如 -0.8 代表停在 Tag 前方 0.8m
     * @param targetY 目標左右偏移 (公尺) - 0 = 正對, 正數可能是向左, 負數向右 (視你的座標系而定)
     * @param targetYaw 目標角度 (度) - 通常是 0 或 180，視 Tag 角度而定
     */
    public Drive2Tag(Swerve swerve, String limelightName, double targetX, double targetY, double targetYaw) {
        m_swerve = swerve;
        m_limelightName = limelightName;
        
        // 儲存傳進來的目標參數
        m_targetX = targetX;
        m_targetY = targetY;
        m_targetYaw = targetYaw;

        addRequirements(m_swerve);

        // 設定容許誤差
        xController.setTolerance(0.01); 
        yController.setTolerance(0.01);
        thetaController.setTolerance(2.0);
    }

    @Override
    public void initialize() {
        LimelightHelpers.setPipelineIndex(m_limelightName, 1);
    }

    @Override
    public void execute() {
        // SmartDashboard.putBoolean("Drive2Tag/IsRunning", true);

        boolean hasTarget = LimelightHelpers.getTV(m_limelightName);
        // SmartDashboard.putBoolean("Drive2Tag/HasTarget", hasTarget);

        if (hasTarget) {
            double[] targetPose = LimelightHelpers.getTargetPose_RobotSpace(m_limelightName);
            
            if (targetPose != null && targetPose.length >= 6) {
                // 讀取 Limelight 數據 (根據你的註解對應)
                double currentX = targetPose[1];
                double currentY = targetPose[0];
                double currentYaw = targetPose[4]; 

                // SmartDashboard.putNumber("Drive2Tag/Dist_X", currentX);
                // SmartDashboard.putNumber("Drive2Tag/Dist_Y", currentY);
                // SmartDashboard.putNumber("Drive2Tag/Angle_Yaw", currentYaw);

                // --- 關鍵修改：使用建構子傳進來的目標變數 ---
                double xSpeed = xController.calculate(currentX, m_targetX);
                double ySpeed = yController.calculate(currentY, m_targetY); // 這裡不再是寫死的 0
                double thetaSpeed = thetaController.calculate(currentYaw, m_targetYaw); // 這裡不再是寫死的 -15

                // SmartDashboard.putBoolean("Drive2Tag/AtSetpoint", xController.atSetpoint());
                
                xSpeed*=6.0;
                ySpeed*=6.0;
                thetaSpeed*=6.0;
                
                // 限制輸出速度 (Clamp)
                xSpeed = Math.max(-1.8, Math.min(1.8, xSpeed));
                ySpeed = Math.max(-1.8, Math.min(1.8, ySpeed));
                thetaSpeed = Math.max(-1.5, Math.min(1.5, thetaSpeed));


                // xSpeed = 0;
                // ySpeed = 0;
                // thetaSpeed = 0;

                m_swerve.setSpeed(xSpeed, ySpeed, thetaSpeed, false);
            } else {
                // SmartDashboard.putString("Drive2Tag/Error", "Data Invalid");
                m_swerve.setSpeed(0, 0, 0, false);
            }
        }
    }

    @Override
    public void end(boolean interrupted) {
        m_swerve.setSpeed(0, 0, 0, false);
        LimelightHelpers.setPipelineIndex(m_limelightName, 0);
    }
}