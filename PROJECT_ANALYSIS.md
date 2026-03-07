# RobotControl2026 Analysis Report

**Updated**: 2026-03-07
**Game**: 2026 FRC REBUILT
**Scoring**: Hub (center of field)
**Field**: 16.541m x 8.069m

## Bugs Fixed This Session

1. **Far Auto Shoot isAtSpeed mismatch** - Was waiting for 65 RPS but shooter set to 62. Fixed to isAtSpeed(62).
2. **Shooter angle offset missing** - Added kShooterAngleOffsetRad constant for shooter mounting direction.
3. **Rumble sequence incomplete** - Added final waitSeconds + setRumble(0) to teleop rumble.
4. **DriveSubsystem empty shell** - Removed unused instantiation.

## Key Constants (AutoAim)

- Blue Hub: (4.626, 4.035)
- Red Hub: (11.915, 4.035)
- Field Mid X: 8.27m
- Shooter Angle Offset: PI (assumes rear-mounted, TODO confirm)
- Red Return Angle: 0 rad
- Blue Return Angle: PI rad
- Rotation PID: kP=5.0, kI=0, kD=0.1
- Tolerance: 2 deg (first trigger), 5 deg (hysteresis)
- Shooter Tolerance: 3 RPS
- Mid-Field Return: 70 RPS

## CAN Bus

DRIVETRAIN bus: Swerve rotors 1,3,5,7 / throttles 2,4,6,8 / CANcoders 11-14
Default bus: Pigeon2(0), Shooter(22/21), Transport(26/30), Intake(29/35)

## Remaining Issues

- IntakeArm CAN 3,4 conflicts with Swerve (not active)
- setChassisSpeeds has magic numbers (dead code)
- SwerveModuleNeo throws RuntimeException on failure
- Drive2Tag + AutoAimAndShoot should not be used simultaneously
- Phoenix 6 deprecation warnings on CANcoder/TalonFX constructors
- Confirm kShooterAngleOffsetRad direction on actual robot
