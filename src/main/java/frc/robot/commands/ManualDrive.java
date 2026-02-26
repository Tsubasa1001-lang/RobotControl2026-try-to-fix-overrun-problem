package frc.robot.commands;

import edu.wpi.first.util.sendable.SendableBuilder;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.Constants.SwerveConstants;
import frc.robot.subsystems.Swerve;
import frc.robot.subsystems.Swerve.SwerveMode;

public class ManualDrive extends Command {

    private static final double X_MULTIPLIER = 1;
    private static final double Y_MULTIPLIER = 1;
    private static final double Z_MULTIPLIER = -0.4;
    private static final double X_NULL_ZONE = 0.05;
    private static final double Y_NULL_ZONE = 0.05;
    private static final double Z_NULL_ZONE = 0.05;
    
    private final Swerve mSwerve;
    private final CommandXboxController mJoystick;
    private boolean isFieldOriented = true;
    
    public ManualDrive(Swerve drive, CommandXboxController joystick) {
        mSwerve = drive;
        mJoystick = joystick;

        // Adds the Swerve subsystem as a requirement to the command
        // 加入 swerve 為這條命令的必要條件
        addRequirements(mSwerve);
    }

    @Override
    public void initialize() {
        mSwerve.setSwerveMode(SwerveMode.ROBOT_CENTRIC);
    }

    @Override
    public void execute() {
        //input inverse
        double xCtl = mJoystick.getLeftY();
        double yCtl = mJoystick.getLeftX();
        double zCtl = mJoystick.getRightX();

        // SmartDashboard.putNumber("xCtl", xCtl);
        // SmartDashboard.putNumber("yCtl", yCtl);
        // SmartDashboard.putNumber("zCtl", zCtl);
        
        // DEBUG: 原始手把輸入
        SmartDashboard.putNumber("Raw/LeftY", mJoystick.getLeftY());
        SmartDashboard.putNumber("Raw/LeftX", mJoystick.getLeftX());
        
        double boostTranslation = mJoystick.rightBumper().getAsBoolean()?1:0.5;
        // double boostTranslation = 1;
        
        xCtl = calculateNullZone(xCtl, X_NULL_ZONE);
        xCtl *= X_MULTIPLIER;
        xCtl *= boostTranslation;
        yCtl = calculateNullZone(yCtl, Y_NULL_ZONE);
        yCtl *= Y_MULTIPLIER;
        yCtl *= boostTranslation;
        zCtl = calculateNullZone(zCtl, Z_NULL_ZONE);
        zCtl *= Z_MULTIPLIER;
        // 搖桿滿推 + Boost = kMaxPhysicalSpeedMps (MK4i L3 最大速度)
        // 搖桿滿推 不按Boost = kMaxPhysicalSpeedMps * 0.5
        xCtl *= SwerveConstants.kMaxPhysicalSpeedMps;
        yCtl *= SwerveConstants.kMaxPhysicalSpeedMps;
        zCtl *= SwerveConstants.kMaxPhysicalSpeedMps;

        mSwerve.setSpeed(xCtl, yCtl, zCtl, isFieldOriented);
        SmartDashboard.putNumber("Drive/xSpeed", xCtl);
        SmartDashboard.putNumber("Drive/ySpeed", yCtl);
        SmartDashboard.putNumber("Drive/zSpeed", zCtl);
        SmartDashboard.putBoolean("Drive/fieldOriented", isFieldOriented);
    }

    private double calculateNullZone(double input, double nullZone) {
        if (Math.abs(input) < nullZone) return 0;
        else {
            input += (input > 0 ? -nullZone : nullZone);
            input *= 1/(1-nullZone);
            return input;
        }
    }

    public void setIsFieldOriented(boolean isFieldOriented) {
        this.isFieldOriented = isFieldOriented;
    }

    public boolean getIsFieldOriented() {
        return isFieldOriented;
    }

    @Override
    public void initSendable(SendableBuilder builder) {
        builder.addBooleanProperty("Field Oriented", this::getIsFieldOriented, this::setIsFieldOriented);
    }
}   
