// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import frc.robot.Constants.OperatorConstants;
import frc.robot.Constants.AutoAimConstants;
import frc.robot.commands.Drive2Tag;
import frc.robot.commands.ManualDrive;
import frc.robot.commands.AutoAimAndShoot;
import frc.robot.subsystems.IntakeArmSubsystem;
import frc.robot.subsystems.IntakeRollerSubsystem;
import frc.robot.subsystems.Swerve;
import frc.robot.subsystems.TransportSubsystem;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Translation2d;

//import com.pathplanner.lib.auto.AutoBuilder;
//import com.pathplanner.lib.auto.NamedCommands;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.GenericHID.RumbleType;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;

// import com.pathplanner.lib.PathPlanner;
// import com.pathplanner.lib.PathPlannerTrajectory;
import com.pathplanner.lib.config.RobotConfig;
import com.pathplanner.lib.controllers.PPHolonomicDriveController;
import com.pathplanner.lib.commands.PathPlannerAuto;
import com.pathplanner.lib.config.PIDConstants;
import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.path.PathPlannerPath;
import com.pathplanner.lib.auto.NamedCommands;

import frc.robot.subsystems.ShooterSubsystem;
import frc.robot.util.ShuffleboardManager;

import java.util.logging.Logger;

import com.ctre.phoenix6.SignalLogger;

public class RobotContainer {

    // SignalLogger logger = new SignalLogger();
    // private final Timer timer = new Timer();
    private int printCounter = 0;    
    private final Swerve swerve = new Swerve(Constants.kLimelightName);
    private final CommandXboxController driverController =
    new CommandXboxController(OperatorConstants.kSwerveControllerPort);

    private final ManualDrive manualDriveCommand = new ManualDrive(swerve, driverController);
    private final SendableChooser<Command> autoChooser;

    // ═══════════════ Shuffleboard ═══════════════
    private final ShuffleboardManager shuffleboardManager = new ShuffleboardManager();

    private final ShooterSubsystem shooterSubsystem = new ShooterSubsystem(shuffleboardManager.getShooterTab());
    // private final IntakeArmSubsystem intakeArm = new IntakeArmSubsystem(shuffleboardManager.getIntakeArmTab());
    private final IntakeRollerSubsystem intakeRoller = new IntakeRollerSubsystem(shuffleboardManager.getIntakeRollerTab());
    private final TransportSubsystem transport = new TransportSubsystem();

    private Command autoCommand;
    
    public RobotContainer() {
        SignalLogger.enableAutoLogging(false);

        // ═══════════════ Shuffleboard 初始化 ═══════════════
        swerve.setupShuffleboardTab(shuffleboardManager.getSwerveTab());
        
        // ═══════════════ PathPlanner NamedCommands ═══════════════

        // "Start Intake"：吸球 + up_to_shoot 反轉擋球（防止球衝入射手）
        //   → parallel 使用：邊走邊吸球
        //   → sequential 使用：停下吸球
        NamedCommands.registerCommand("Start Intake", 
            intakeRoller.sys_intakeWithTrigger(transport)
        );

        // "Stop Intake"：立即停止吸球與 up_to_shoot
        NamedCommands.registerCommand("Stop Intake", 
            intakeRoller.runOnce(() -> {
                intakeRoller.stop();
                transport.stopTransport();
            })
        );

        // "UpToShoot 2s"：up_to_shoot + transport 正轉推球 2 秒後自動結束
        //   射手應已在比賽開始時啟動到待機轉速（sys_idle DefaultCommand）
        //   建議放在路徑結束後（sequential），讓機器人停下來射擊
        //   ⚠ 如需不同秒數，複製並修改秒數後再加一行 registerCommand
        NamedCommands.registerCommand("UpToShoot 2s",
            transport.sys_upToShootForSeconds(2.0)
        );
            
            
            try {
                RobotConfig config = RobotConfig.fromGUISettings();
                
                com.pathplanner.lib.util.PathPlannerLogging.setLogActivePathCallback(null);
                com.pathplanner.lib.util.PathPlannerLogging.setLogTargetPoseCallback(null);
            AutoBuilder.configure(
                swerve::getPose, 
                swerve::resetPose, 
                swerve::getChassisSpeeds, 
                swerve::drive, 
                
                new PPHolonomicDriveController(
                    new PIDConstants(3.0, 0.0, 0.0), // Translation PID
                    new PIDConstants(1.8, 0.0, 0.0)  // Rotation PID
                ),
                
                config, // 機器人配置
                
                swerve::isAllianceRed, // 決定是否翻轉路徑
                swerve // Subsystem
            );
            // com.pathplanner.lib.util.PathPlannerLogging.setLogEstimatedPoseCallback(null);

        } catch (Exception e) {
            e.printStackTrace();
        }

        autoChooser = AutoBuilder.buildAutoChooser();
        shuffleboardManager.setupMainTab(swerve.getField2d(), autoChooser);
        
        // autoCommand = AutoBuilder.buildAuto("Simple Left Auto");

        configureBindings();
        swerve.setDefaultCommand(manualDriveCommand);
        // 射手整場不停機：沒有射擊指令時以待機速度 (kIdleRps) 持續旋轉
        // 收到 AutoAimAndShoot / ShootOnTheMove 時會被接管，結束後自動回到 idle
        shooterSubsystem.setDefaultCommand(shooterSubsystem.sys_idle());
        driverController.button(8).onTrue(Commands.runOnce(swerve::resetIMU)); // menu button


        driverController.rightStick().onTrue(Commands.either(
            Commands.runOnce(() -> manualDriveCommand.setIsFieldOriented(false)),
            Commands.runOnce(() -> manualDriveCommand.setIsFieldOriented(true)),
            manualDriveCommand::getIsFieldOriented));

        
        // CommandScheduler.getInstance().schedule(
        //     Commands.run(() -> {
        //         // printCounter++;
        //         // if (printCounter >= 10) { // 每 10 個週期 (約 0.2秒) 才執行一次
        //         //     // SmartDashboard.putString("Position", swerve.getPose().toString());
        //         //     // putLimeLight();
        //         //     // 這裡也可以呼叫 swerve.updateSmartDashboard();
        //         //     printCounter = 0; // 重置計數器
        //         // }
        //         // SmartDashboard.putString("Position", swerve.getPose().toString());
        //         // putLimeLight();
        //         // SmartDashboard.putNumber("Match Time", timer.getMatchTime());
        //         // if (shooterSubsystem.isAtSpeed()) { // 假設目標是 80
        //         //     // 速度到了 -> 輕微震動左手把
        //         //     driverController.getHID().setRumble(RumbleType.kLeftRumble, 0.1);
        //         // } else {
        //         //     // 速度沒到 -> 關閉震動
        //         //     driverController.getHID().setRumble(RumbleType.kLeftRumble, 0);
        //         // }
        //     }).ignoringDisable(true)

        // );
    }

    public void putLimeLight() {
        LimelightHelpers.PoseEstimate mt2;
        // if (swerve.isAllianceRed()) {
        //     // 如果是紅方，拿以紅方為基準的 MegaTag2 座標
        //     mt2 = LimelightHelpers.getBotPoseEstimate_wpiRed(Constants.kLimelightName);
        // } else {
            // 預設拿藍方的
        // mt2 = LimelightHelpers.getBotPoseEstimate_wpiBlue(Constants.kLimelightName);
        // // }
        // if (mt2 != null) {
        //     // SmartDashboard.putNumber("TagCount", mt2.tagCount);
        //     double[] fiducialIds = new double[mt2.rawFiducials.length];
        //     for (int i = 0; i < mt2.rawFiducials.length; i++) {
        //         fiducialIds[i] = mt2.rawFiducials[i].id;
        //     }
        //     // SmartDashboard.putNumberArray("Fiducials", fiducialIds);
        // }
    }

    

    private void configureBindings() {
        // 1. 右邊模式 (Right Bumper)
        // 參數: swerve, limelightName, TargetX(-0.8), TargetY(-0.5 往右), TargetYaw(-15)
        // driverController.rightBumper().whileTrue(
        //     new Drive2Tag(swerve, Constants.kLimelightName, -1.15, 0.8, 15.0)
        // );

        // 2. 左邊模式 (Left Bumper)
        // 參數: swerve, limelightName, TargetX(-0.8), TargetY(0.5 往左), TargetYaw(-15)
        // driverController.leftBumper().whileTrue(
        //     new Drive2Tag(swerve, Constants.kLimelightName, -1.15, -0.8, -15.0)
        // );

        // 自動瞄準射擊：按住 Left Bumper 時自動旋轉面向目標 + 依距離調整射手速度 + 達速對準後自動發射
        // ⚠️ 與 Drive2Tag (A鍵) 互斥：
        //   - Drive2Tag addRequirements(swerve) → 會中斷 ManualDrive
        //   - AutoAimAndShoot 不佔 swerve（透過 setAimSpeed 疊加）
        //   - 若同時按 A + LB，兩者會同時控制底盤打架
        //   → 解法：Drive2Tag 綁定時額外 require shooter+transport，讓 scheduler 自動互斥
        driverController.leftBumper().whileTrue(
            new AutoAimAndShoot(swerve, shooterSubsystem, transport, manualDriveCommand, shuffleboardManager.getAutoAimTab())
        );

        // Drive2Tag：按住 A 鍵自動對位 AprilTag
        // 額外 require shooter + transport → 若 AutoAimAndShoot 正在運行會被自動取消
        // 不停射手馬達（DefaultCommand sys_idle 會繼續待機旋轉）
        driverController.a().whileTrue(
            new Drive2Tag(swerve, Constants.kLimelightName, -1.15, 0.0, 0.0)
                .alongWith(
                    Commands.runOnce(() -> {
                        transport.stopTransport();
                    }, shooterSubsystem, transport)
                )
        );

        // 手動射擊：按住右板機 → 自動瞄準 + 依距離調整 RPS + 達速對準後自動送球
        // 與 leftBumper 的 AutoAimAndShoot 功能相同
        driverController.rightTrigger(0.1).whileTrue(
            new AutoAimAndShoot(swerve, shooterSubsystem, transport, manualDriveCommand)
        );
            
        // shooterSubsystem.sys_manualShoot(1.0);

        // ==========================================
        // 1. 手動測試模式 (Manual Mode)
        // ==========================================
        // 設定：使用 "左搖桿 Y 軸" 來控制 Intake 上下
        // 當你在測試的時候，一直推搖桿，看 Dashboard 的數值
        // intakeArm.setDefaultCommand(
        //     intakeArm.sys_manualMove(() -> -operatorController.getLeftY()) // 注意 Y 軸通常要加負號才會符合直覺 (上推=正)
        // );
        // ==========================================
        // 2. 自動按鈕 (Automation)
        // ==========================================
        // 假設你測試出來，Intake 放下的最佳位置是 0.25 圈 (90度)
        // 按下 A 鍵，Intake 自動跑 到 0.25 圈的位置
        // driverController.a().onTrue(
        //     intakeArm.runOnce(() -> intakeArm.setTargetPosition(0.25))
        // );

        // // 按下 B 鍵，Intake 自動收回到 0 圈 (原點)
        driverController.b().whileTrue(
            transport.sys_reverseTransport()
        );

        // ==========================================
        // 設定：按住 "左板機 (Left Trigger)" 來控制 Intake 吸入
        // ==========================================

        // 當左板機按壓超過 0.1 時，啟動 sys_intakeWithTrigger 指令
        // 放開後自動停止
        // 當左板機按壓超過 0.1 時，啟動吸球：IntakeRoller 正轉 + up_to_shoot 反轉擋球
        // 放開後兩者同時停止
        driverController.leftTrigger(0.1).whileTrue(
            intakeRoller.sys_intakeWithTrigger(transport)
        );

        // transport
        driverController.x().whileTrue(
            transport.sys_runTransport()
        );
    }

    public Command getAutonomousCommand() {
        // swerve.run();

        return autoChooser.getSelected();
    }

    public ShuffleboardManager getShuffleboardManager() {
        return shuffleboardManager;
    }
    
    public void teleopInit() {
        // 自動使用 Limelight 校正位姿（包含航向），免去手動按 resetIMU
        swerve.resetPoseToLimelight();
        
        swerve.run();
        // 進入 Teleop 時震動手把提示（必須 schedule 才會執行）
        CommandScheduler.getInstance().schedule(
            Commands.sequence(
                Commands.runOnce(() -> driverController.setRumble(RumbleType.kBothRumble, 1)),
                Commands.waitSeconds(0.3),
                Commands.runOnce(() -> driverController.setRumble(RumbleType.kBothRumble, 0)),
                Commands.waitSeconds(0.1),
                Commands.runOnce(() -> driverController.setRumble(RumbleType.kBothRumble, 1)),
                Commands.waitSeconds(0.3),
                Commands.runOnce(() -> driverController.setRumble(RumbleType.kBothRumble, 0))
            ).finallyDo(() -> driverController.setRumble(RumbleType.kBothRumble, 0))
        );
    }

    public void disabledInit() {
        swerve.disabledInit();
    }
}
