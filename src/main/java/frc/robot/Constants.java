  // Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import com.ctre.phoenix6.CANBus;

import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.math.util.Units;

/**
 * The Constants class provides a convenient place for teams to hold robot-wide
 * numerical or boolean
 * constants. This class should not be used for any other purpose. All constants
 * should be declared
 * globally (i.e. public static). Do not put anything functional in this class.
 *
 * <p>
 * It is advised to statically import this class (or one of its inner classes)
 * wherever the
 * constants are needed, to reduce verbosity.
 */
public final class Constants {
  public static final String kLimelightName = "limelight";

  // ===== 自動瞄準射擊相關常數 =====
  public static final class AutoAimConstants {
    // 目標位置 (藍方 Speaker 開口中心，WPILib 場地座標系)
    // ⚠️ 請根據你的實際比賽場地量測後修改這些值！
    public static final double kBlueSpeakerX = 0.0;   // Speaker 在場地 X 座標 (m)
    public static final double kBlueSpeakerY = 5.55;  // Speaker 在場地 Y 座標 (m)
    // 紅方 Speaker 位置（場地對稱，X ≈ 16.54m）
    public static final double kRedSpeakerX = 16.54;
    public static final double kRedSpeakerY = 5.55;

    // 旋轉 PID（控制底盤面向目標）
    public static final double kRotation_kP = 5.0;
    public static final double kRotation_kI = 0.0;
    public static final double kRotation_kD = 0.1;
    public static final double kRotationToleranceDeg = 2.0; // 角度容許誤差 (度)

    // 距離 → 射手 RPS 對照表
    // 格式: {距離(m), RPS}，距離由近到遠排列
    // ⚠️ 這些值需要在實際場地測試後調整！
    public static final double[][] kDistanceToRpsTable = {
      {1.0, 35},   // 1m → 35 RPS
      {1.5, 40},   // 1.5m → 40 RPS
      {2.0, 45},   // 2m → 45 RPS
      {2.5, 50},   // 2.5m → 50 RPS
      {3.0, 55},   // 3m → 55 RPS
      {3.5, 60},   // 3.5m → 60 RPS
      {4.0, 65},   // 4m → 65 RPS
      {5.0, 70},   // 5m → 70 RPS
    };

    // 射手速度容許誤差 (RPS)
    public static final double kShooterToleranceRps = 3.0;
  }

  public static class OperatorConstants {
    public static final int kSwerveControllerPort = 0;
  }

  public static final class RoboArmConstants {
    public static final int kShooterLeftMotorID = 21;
    
    public static final boolean kShoulderRightMotorInverted = false;
  }

  CANBus canbus = new CANBus("DRIVETRAIN");


  // Swerve constants
  public static final class SwerveConstants {
    public static final int kPigeonID = 0;

    // Rotor IDs
    public static final int kLeftFrontRotorID = 3;
    public static final int kRightFrontRotorID = 5;
    public static final int kLeftRearRotorID = 1;
    public static final int kRightRearRotorID = 7;

    // Throttle IDs
    public static final int kLeftFrontThrottleID = 4;
    public static final int kRightFrontThrottleID = 6;
    public static final int kLeftRearThrottleID = 2;
    public static final int kRightRearThrottleID = 8;

    // Rotor encoder IDs
    public static final int kLeftFrontCANCoderID = 12;
    public static final int kRightFrontCANCoderID = 13;
    public static final int kLeftRearCANCoderID = 11;
    public static final int kRightRearCANCoderID = 14;

    // Rotor encoder & motor inversion
    public static final boolean kRotorEncoderDirection = false;

    public static final boolean kLeftFrontRotorInverted = true;
    public static final boolean kRightFrontRotorInverted = true;
    public static final boolean kLeftRearRotorInverted = true;
    public static final boolean kRightRearRotorInverted = true;

    public static final boolean kLeftFrontThrottleInverted = false;
    public static final boolean kRightFrontThrottleInverted = false;
    public static final boolean kLeftRearThrottleInverted = false;
    public static final boolean kRightRearThrottleInverted = false;

    // Distance between centers of right and left wheels on robot
    public static final double kTrackWidth = 0.62865000;
    // Distance between front and back wheels on robot
    public static final double kWheelBase = 0.62865000;

    // Swerve kinematics (order: left front, right front, left rear, right rear)
    // Swerve kinematics（順序：左前，右前，左後，右後）
    public static final SwerveDriveKinematics kSwerveKinematics = new SwerveDriveKinematics(
      new Translation2d(kWheelBase / 2, kTrackWidth / 2),
      new Translation2d(kWheelBase / 2, -kTrackWidth / 2),
      new Translation2d(-kWheelBase / 2, kTrackWidth / 2),
      new Translation2d(-kWheelBase / 2, -kTrackWidth / 2));

    // Camera centered kinematics
    public static final SwerveDriveKinematics kSwerveKinematicsCamera = new SwerveDriveKinematics(
      new Translation2d(0, kTrackWidth / 2),
      new Translation2d(0, -kTrackWidth / 2),
      new Translation2d(-kWheelBase, kTrackWidth / 2),
      new Translation2d(-kWheelBase, -kTrackWidth / 2));

    // Rotor PID constants
    // Ref: https://www.ni.com/zh-tw/shop/labview/pid-theory-explained.html#section-366173388
    public static final double kRotor_kP = 0.005; // critical damping: 0.024 ~ 0.025 
    public static final double kRotor_kI = 0.001; // period: 0.169s
    public static final double kRotor_kD = 0.0001;
    public static final double kRotor_maxVelocity = 360; //control max motor output
    public static final double kRotor_maxAcceleration = kRotor_maxVelocity * 5;

    // // Rotor PID constants soft
    // public static final double kRotor_kP = 0.002;
    // public static final double kRotor_kI = 0.005;
    // public static final double kRotor_kD = 0.00001;

    // Wheel diameter
    // 輪徑
    // OD(outer diameter) 4 inches = 0.1016 meters
    // Ref: https://www.swervedrivespecialties.com/collections/mk4i-parts/products/billet-wheel-4d-x-1-5w-bearing-bore
    // 要包含胎皮受到重量的厚度，建議重新量測並再次確認
    public static final double kWheelDiameterMeters = 0.1;

    // Throttle gear ratio
    // (number of turns it takes the motor to rotate the rotor one revolution)
    // Throttle 齒輪比率（馬達轉動輪子一圈所需的圈數）
    // MK4i底盤: 8.14 for L1 - Standard, 6.75 for L2 - Fast, 6.12 for L3 - Very Fast
    // Ref: https://www.swervedrivespecialties.com/products/mk4i-swerve-module
    public static final double kThrottleGearRatio = 6.12; 

    // Throttle velocity conversion constant
    // Throttle 速度轉換 Constant
    // 轉換Enocoder速度RPM -> m/s
    public static final double kThrottleVelocityConversionFactor = (1 / kThrottleGearRatio)
        * kWheelDiameterMeters * Math.PI / 60;

    // Trottle position conversion constant
    // Throttle 位置轉換 Constant
    // 轉換Enocoder位置 圈數 -> m
    public static final double kThrottlePositionConversionFactor = (1 / kThrottleGearRatio)
        * kWheelDiameterMeters * Math.PI;

    // Rotor position conversion constant
    // Rotor 位置轉換 Constant
    // 轉換Enocoder位置 圈數 -> 角度
    // The steering gear ratio of the MK4i is 150/7:1
    // Ref: https://www.swervedrivespecialties.com/products/mk4i-swerve-module
    public static final double kRotorPositionConversionFactor = (1 / (150.0 / 7.0)) * 360.0;

    // MK4i L3 + Kraken X60 理論最大速度
    // = (6000 RPM / 60 / kThrottleGearRatio) * kWheelDiameterMeters * PI
    // ≈ 5.13 m/s (用 0.1m 輪徑)
    // SDS 官方建議值約 5.21 m/s (用 4 inch = 0.1016m)
    // 這裡取計算值，所有速度相關的地方都引用這個常數
    public static final double kMaxPhysicalSpeedMps = 
        (6000.0 / 60.0 / kThrottleGearRatio) * kWheelDiameterMeters * Math.PI;
  }

  // Voltage compensation
  public static final double kVoltageCompensation = 12.0;
  
  public static final class AutoConstants {
    public static final double kMaxSpeedMetersPerSecond = SwerveConstants.kMaxPhysicalSpeedMps;
    public static final double kMaxAccelerationMetersPerSecondSquared = 4;
    public static final double kMaxAngularSpeedRadiansPerSecond = Math.PI/10;
    public static final double kMaxAngularSpeedRadiansPerSecondSquared = Math.PI/5;
    
    public static final double kTranslationController_kP = 1.3;
    public static final double kTranslationController_kI = 0.001;
    public static final double kTranslationController_kD = 0.005;

    public static final double kRotationController_kP = 0.2;
    public static final double kRotationController_kI = 0.005;
    public static final double kRotationController_kD = 0.001;

    // Constraint for the motion profiled robot angle controller
    public static final TrapezoidProfile.Constraints kThetaControllerConstraints =
        new TrapezoidProfile.Constraints(
            kMaxAngularSpeedRadiansPerSecond, kMaxAngularSpeedRadiansPerSecondSquared);
  }
  
}
