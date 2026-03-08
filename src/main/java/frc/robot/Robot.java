// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import frc.robot.Constants;
import frc.robot.util.ShuffleboardManager;

/**
 * The methods in this class are called automatically corresponding to each mode, as described in
 * the TimedRobot documentation. If you change the name of this class or the package after creating
 * this project, you must also update the Main.java file in the project.
 */
public class Robot extends TimedRobot {
  private Command m_autonomousCommand;

  private final RobotContainer m_robotContainer;
  private final ShuffleboardManager m_shuffleboard;

  // ── Loop 計時器：接上實體機器後可在 SmartDashboard 看到迴圈耗時 ──
  // 使用「上一次進入 → 這一次進入」的間隔測量，包含完整迴圈（含 LiveWindow、SmartDashboard flush 等）
  private double lastLoopTimestamp = 0;
  private double maxLoopTime = 0;
  private int telemetryCounter = 0;

  /**
   * This function is run when the robot is first started up and should be used for any
   * initialization code.
   */
  public Robot() {
    // CameraServer.startAutomaticCapture("Coral CAM", 0);
    // Instantiate our RobotContainer.  This will perform all our button bindings, and put our
    // autonomous chooser on the dashboard.
    m_robotContainer = new RobotContainer();
    m_shuffleboard = m_robotContainer.getShuffleboardManager();
  }

  /**
   * This function is called every 20 ms, no matter the mode. Use this for items like diagnostics
   * that you want ran during disabled, autonomous, teleoperated and test.
   *
   * <p>This runs after the mode specific periodic functions, but before LiveWindow and
   * SmartDashboard integrated updating.
   */
  @Override
  public void robotPeriodic() {
    // ── 迴圈間隔測量：從「上一次進入 robotPeriodic」到「這一次進入」的完整間隔 ──
    // 這包含了 CommandScheduler.run()、teleopPeriodic()、LiveWindow、SmartDashboard flush 等
    // 比舊版只測 CommandScheduler.run() 更準確
    double currentTimestamp = Timer.getFPGATimestamp();
    double loopTime = (lastLoopTimestamp > 0)
        ? (currentTimestamp - lastLoopTimestamp) * 1000.0  // ms
        : 0.0;  // 第一次呼叫沒有上一次的時間戳
    lastLoopTimestamp = currentTimestamp;

    // Runs the Scheduler.  This is responsible for polling buttons, adding newly-scheduled
    // commands, running already-scheduled commands, removing finished or interrupted commands,
    // and running subsystem periodic() methods.  This must be called from the robot's periodic
    // block in order for anything in the Command-based framework to work.
    CommandScheduler.getInstance().run();

    // ── 迴圈耗時統計 ──
    if (loopTime > maxLoopTime && loopTime < 200.0) {
      // 忽略 > 200ms 的異常值（例如 Disabled → Enabled 的模式切換間隔）
      maxLoopTime = loopTime;
    }
    boolean overrun = loopTime > 20.5; // 20.5ms 容許微小的排程抖動

    // 遙測輸出節流（計時每週期都算，但 NT 寫入降頻）
    if (++telemetryCounter >= Constants.kTelemetryDivider) {
      telemetryCounter = 0;
      m_shuffleboard.updateLoopTime(loopTime, maxLoopTime, overrun);
      SmartDashboard.putNumber("Loop/CurrentMs", loopTime);
      SmartDashboard.putNumber("Loop/MaxMs", maxLoopTime);
      SmartDashboard.putBoolean("Loop/Overrun", overrun);
    }
  }

  /** This function is called once each time the robot enters Disabled mode. */
  @Override
  public void disabledInit() {
    m_robotContainer.disabledInit();
  }

  @Override
  public void disabledPeriodic() {}

  /** This autonomous runs the autonomous command selected by your {@link RobotContainer} class. */
  @Override
  public void autonomousInit() {
    m_autonomousCommand = m_robotContainer.getAutonomousCommand();

    // schedule the autonomous command (example)
    if (m_autonomousCommand != null) {
      CommandScheduler.getInstance().schedule(m_autonomousCommand);
    }
  }

  /** This function is called periodically during autonomous. */
  @Override
  public void autonomousPeriodic() {}

  @Override
  public void teleopInit() {
    // This makes sure that the autonomous stops running when
    // teleop starts running. If you want the autonomous to
    // continue until interrupted by another command, remove
    // this line or comment it out.
    if (m_autonomousCommand != null) {
      m_autonomousCommand.cancel();
    }
    m_robotContainer.teleopInit();
  }

  /** This function is called periodically during operator control. */
  @Override
  public void teleopPeriodic() {}

  @Override
  public void testInit() {
    // Cancels all running commands at the start of test mode.
    CommandScheduler.getInstance().cancelAll();
  }

  /** This function is called periodically during test mode. */
  @Override
  public void testPeriodic() {}

  /** This function is called once when the robot is first started up. */
  @Override
  public void simulationInit() {}

  /** This function is called periodically whilst in simulation. */
  @Override
  public void simulationPeriodic() {}
}
