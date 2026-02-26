// package frc.robot.subsystems;

// import edu.wpi.first.math.controller.PIDController;
// import edu.wpi.first.math.controller.ProfiledPIDController;
// import edu.wpi.first.math.kinematics.ChassisSpeeds;
// import edu.wpi.first.math.trajectory.TrapezoidProfile;
// import edu.wpi.first.math.util.Units;
// import edu.wpi.first.util.sendable.SendableBuilder;
// import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
// import edu.wpi.first.wpilibj2.command.Command;
// import edu.wpi.first.wpilibj2.command.FunctionalCommand;
// import edu.wpi.first.wpilibj2.command.SubsystemBase;
// import frc.robot.LimelightHelpers;

// public class AutoAim extends SubsystemBase{
//     private Swerve swerve;
//     private String limelightName;
//     // private TrapezoidProfile.Constraints xPidConstraints = new TrapezoidProfile.Constraints(1, 1);
//     // private ProfiledPIDController xPidController = new ProfiledPIDController(0.001, 0, 0, xPidConstraints);
//     private PIDController xPidController = new PIDController(0.25, 0.01, 0.01);
//     // private TrapezoidProfile.Constraints yPidConstraints = new TrapezoidProfile.Constraints(1, 1);
//     // private ProfiledPIDController yPidController = new ProfiledPIDController(0.001, 0, 0, yPidConstraints);
//     private PIDController yPidController = new PIDController(0.25, 0.01, 0.01);
//     // private TrapezoidProfile.Constraints zPidConstraints = new TrapezoidProfile.Constraints(1, 1);
//     // private ProfiledPIDController zPidController = new ProfiledPIDController(0.01, 0.001, 0, zPidConstraints);
//     private PIDController zPidController = new PIDController(0.7, 0.1, 0.1);
//     private boolean activate = false;

//     public AutoAim(Swerve swerve, String limelightName) {
//         this.swerve = swerve;
//         this.limelightName = limelightName;
//         zPidController.enableContinuousInput(-Math.PI, Math.PI);
//         zPidController.setIZone(1);
//     }

//     public boolean isActivate() {
//         return activate;
//     }

//     @Override
//     public void periodic() {
//     }
    
//     public Command getAimCommand() {
//         return new FunctionalCommand(
//             () -> {
//                 // xPidController.reset(currentPose.getX());
//                 // yPidController.reset(currentPose.getY());
//                 // zPidController.reset(currentPose.getRotation().getRadians());
//                 xPidController.reset();
//                 yPidController.reset();
//                 zPidController.reset();
//             }, 
//             () -> {
//                 var fiducialID = LimelightHelpers.getFiducialID(limelightName);
//                 SmartDashboard.putString("getBotPose2d", LimelightHelpers.getBotPose2d_wpiBlue(limelightName).toString());
//                 SmartDashboard.putNumber("FiducialID", fiducialID);
//                 SmartDashboard.putData("xPidController", xPidController);
//                 SmartDashboard.putData("yPidController", yPidController);
//                 var targetPose = LimelightHelpers.getTargetPose_RobotSpace(limelightName); // x, y, z, pitch, yaw, roll
//                 var x = targetPose[0]*-1;
//                 var z = targetPose[2];
//                 var yaw = targetPose[4]*-1;
//                 SmartDashboard.putNumber("getTargetPose.z", z);
//                 SmartDashboard.putNumber("getTargetPose.x", x);
//                 SmartDashboard.putNumber("getTargetPose.yaw", yaw);
//                 var xOutput = xPidController.calculate(z, -0.6)*-1;
//                 var yOutput = yPidController.calculate(x, 0.2)*-1;
//                 var zOutput = zPidController.calculate(Units.degreesToRadians(yaw), 0);
//                 SmartDashboard.putNumber("AutoAim.xError", xPidController.getPositionError());
//                 SmartDashboard.putNumber("AutoAim.yError", yPidController.getPositionError());
//                 SmartDashboard.putNumber("AutoAim.zError", zPidController.getPositionError());
//                 SmartDashboard.putNumber("AutoAim.xOutput", xOutput);
//                 SmartDashboard.putNumber("AutoAim.yOutput", yOutput);
//                 SmartDashboard.putNumber("AutoAim.zOutput", zOutput);
                
//                 activate = !(fiducialID == -1 || Math.abs(z) > 1.5);
//                 if (activate) {
//                     swerve.setAimSpeed(new ChassisSpeeds(xOutput, yOutput, zOutput));
//                 }
//                 else {
//                     swerve.setAimSpeed(new ChassisSpeeds());
//                 }
//             }, 
//             (interrupted) -> {
//                 activate = false;
//                 swerve.setAimSpeed(new ChassisSpeeds());
//             }, 
//             () -> false);
//     }

//     @Override
//     public void initSendable(SendableBuilder builder) {
//         builder.setSmartDashboardType("AutoAim");
//         builder.addBooleanProperty("activate", this::isActivate, null);
//     }
// }
