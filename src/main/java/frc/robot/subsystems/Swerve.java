package frc.robot.subsystems;

import com.ctre.phoenix6.hardware.Pigeon2;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.filter.SlewRateLimiter;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.networktables.GenericEntry;
import edu.wpi.first.util.sendable.SendableBuilder;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.SwerveConstants;
import frc.robot.LimelightHelpers;

public class Swerve extends SubsystemBase {
    public enum SwerveMode {
        ROBOT_CENTRIC,
        CAMERA_CENTRIC
    }
    private boolean isUseLimelight = false;
    private String limelightName;
    // Swerve Modules
    public SwerveModule mLeftFrontModule, mRightFrontModule, mLeftRearModule, mRightRearModule;
    private SwerveDriveOdometry mOdometry;
    private SwerveDrivePoseEstimator poseEstimator;
    // IMU
    private Pigeon2 mPigeonIMU;
    // For Acceleration Constraints
    private ChassisSpeeds mTargetChassisSpeeds = new ChassisSpeeds(0, 0, 0);
    private ChassisSpeeds mAimChassisSpeeds = new ChassisSpeeds(0, 0, 0);
    // SlewRateLimiter: 限制速度變化率（加速度），單位 m/s per second
    // 數值越大 = 加速越快，例如 20 表示每秒速度最多變化 20 m/s
    private SlewRateLimiter xSpeedLimiter = new SlewRateLimiter(20); // X 平移加速度限制
    private SlewRateLimiter ySpeedLimiter = new SlewRateLimiter(20); // Y 平移加速度限制
    private SlewRateLimiter zSpeedLimiter = new SlewRateLimiter(15); // 旋轉加速度限制

    public static final String kCANBusName = "DRIVETRAIN"; 

    private SwerveMode mSwerveMode = SwerveMode.ROBOT_CENTRIC;
    private boolean autoUpdateIMU = false;

    // 模擬用：追蹤陀螺儀角度
    private double simGyroAngleDeg = 0;

    private final Field2d m_field = new Field2d();

    // ═══════════════ Shuffleboard (Swerve Tab) ═══════════════
    private ShuffleboardTab swerveTab;
    private GenericEntry chassisVxEntry, chassisVyEntry, chassisOmegaEntry;
    private GenericEntry gyroAngleEntry;
    private GenericEntry mod0SpeedEntry, mod0AngleEntry;

    public Swerve() {
        initFields();
        mOdometry = new SwerveDriveOdometry(
            SwerveConstants.kSwerveKinematics, 
            getGyroAngle(), 
            getModulePositions()
        );
    }

    public Swerve(String limelightName) {
        this.limelightName = limelightName;
        isUseLimelight = true;
        initFields();
        poseEstimator = new SwerveDrivePoseEstimator(
                SwerveConstants.kSwerveKinematics,
                getGyroAngle(),
                getModulePositions(),
                new Pose2d(),
                VecBuilder.fill(0.05, 0.05, Units.degreesToRadians(5)),
                VecBuilder.fill(0.5, 0.5, Units.degreesToRadians(30)));
    }

    /**
     * 設定 Shuffleboard Swerve 分頁（由 RobotContainer 在建構後呼叫）。
     */
    public void setupShuffleboardTab(ShuffleboardTab tab) {
        this.swerveTab = tab;

        chassisVxEntry = tab.add("Chassis vx", 0)
                .withWidget(BuiltInWidgets.kGraph).withSize(3, 2).withPosition(0, 0).getEntry();
        chassisVyEntry = tab.add("Chassis vy", 0)
                .withWidget(BuiltInWidgets.kGraph).withSize(3, 2).withPosition(3, 0).getEntry();
        chassisOmegaEntry = tab.add("Chassis omega", 0)
                .withWidget(BuiltInWidgets.kGraph).withSize(3, 2).withPosition(6, 0).getEntry();
        gyroAngleEntry = tab.add("Gyro Angle", 0)
                .withWidget(BuiltInWidgets.kTextView).withSize(2, 1).withPosition(0, 2).getEntry();
        mod0SpeedEntry = tab.add("Mod0 Speed", 0)
                .withWidget(BuiltInWidgets.kTextView).withSize(2, 1).withPosition(2, 2).getEntry();
        mod0AngleEntry = tab.add("Mod0 Angle", 0)
                .withWidget(BuiltInWidgets.kTextView).withSize(2, 1).withPosition(4, 2).getEntry();
    }

    /**
     * 取得 Field2d 物件（供 ShuffleboardManager.setupMainTab 使用）。
     */
    public Field2d getField2d() {
        return m_field;
    }

    private void initFields() {
        mPigeonIMU = new Pigeon2(SwerveConstants.kPigeonID);


        mLeftFrontModule = new SwerveModuleKraken(
            SwerveConstants.kLeftFrontThrottleID, 
            SwerveConstants.kLeftFrontRotorID, 
            SwerveConstants.kLeftFrontThrottleInverted,
            SwerveConstants.kLeftFrontRotorInverted,
            SwerveConstants.kLeftFrontCANCoderID, 
            "left front"
        );

        mRightFrontModule = new SwerveModuleKraken(
            SwerveConstants.kRightFrontThrottleID, 
            SwerveConstants.kRightFrontRotorID, 
            SwerveConstants.kRightFrontThrottleInverted,
            SwerveConstants.kRightFrontRotorInverted,
            SwerveConstants.kRightFrontCANCoderID, 
            "right front"
        );

        mLeftRearModule = new SwerveModuleKraken(
            SwerveConstants.kLeftRearThrottleID, 
            SwerveConstants.kLeftRearRotorID, 
            SwerveConstants.kLeftRearThrottleInverted,
            SwerveConstants.kLeftRearRotorInverted,
            SwerveConstants.kLeftRearCANCoderID, 
            "left rear"
        );

        mRightRearModule = new SwerveModuleKraken(
            SwerveConstants.kRightRearThrottleID, 
            SwerveConstants.kRightRearRotorID, 
            SwerveConstants.kRightRearThrottleInverted,
            SwerveConstants.kRightRearRotorInverted,
            SwerveConstants.kRightRearCANCoderID, 
            "right rear"
        );

        setSpeed(0, 0, 0, false);
    }

    @Override
    public void periodic() {
        // 取得基本感測器數據
        Rotation2d gyroAngle = getGyroAngle();
        SwerveModulePosition[] modulePositions = getModulePositions();
        // isUseLimelight = 0;
        if (isUseLimelight && poseEstimator != null) {
            poseEstimator.update(gyroAngle, modulePositions);

            if (limelightName != null) {
                LimelightHelpers.SetRobotOrientation_NoFlush(limelightName, 
                    gyroAngle.getDegrees(), getGyroRateDps(), 0, 0, 0, 0);
                
                LimelightHelpers.PoseEstimate mt2;
                // if(isAllianceRed()){
                //     mt2 = LimelightHelpers.getBotPoseEstimate_wpiRed(limelightName);
                // }
                // else{
                    mt2 = LimelightHelpers.getBotPoseEstimate_wpiBlue(limelightName);
                // }
                
                boolean doRejectUpdate = false;

                if (mt2 != null && mt2.tagCount > 0 && mt2.rawFiducials != null && mt2.rawFiducials.length > 0) {
                    
                    if (mt2.tagCount == 1) {
                        // 檢查第一個 Tag 是否可靠
                        if (mt2.rawFiducials[0].ambiguity > 0.7 || mt2.rawFiducials[0].distToCamera > 4.0) {
                            doRejectUpdate = true;
                        }
                    }
                    
                    if (Math.abs(getGyroRateDps()) > 720) doRejectUpdate = true;

                    if (!doRejectUpdate) {
                        // 正常更新
                        poseEstimator.setVisionMeasurementStdDevs(VecBuilder.fill(0.5, 0.5, 999999));
                        poseEstimator.addVisionMeasurement(mt2.pose, mt2.timestampSeconds);

                        // 儀表板更新寫在 if 裡面，避免 mt2 為空時崩潰
                        // SmartDashboard.putNumber("Vision/Tag Count", mt2.tagCount);
                        // SmartDashboard.putNumber("Vision/MT2 Pose X", mt2.pose.getX());
                    }
                }
                // SmartDashboard.putBoolean("Vision/Is Rejected", doRejectUpdate);
            }
        } else if (mOdometry != null) {
            mOdometry.update(gyroAngle, modulePositions);
        }

        // 更新 Field2d
        m_field.setRobotPose(getPose());

        // 更新 Swerve Tab 陀螺儀角度
        if (gyroAngleEntry != null) {
            gyroAngleEntry.setDouble(gyroAngle.getDegrees());
        }

        // 每個週期都執行馬達控制（原本靠獨立的 setStateCommand 排程，容易漏掉）
        if (edu.wpi.first.wpilibj.DriverStation.isEnabled()) {
            runSetStates();
        }
    }

    public boolean isAllianceRed() {
        var alliance = DriverStation.getAlliance();
        if (alliance.isPresent()) {
            return alliance.get() == DriverStation.Alliance.Red;
        }
        return false; //預設藍色
    }

    private void runSetStates() {
        var sumChassisSpeeds = new ChassisSpeeds(
            mTargetChassisSpeeds.vxMetersPerSecond + mAimChassisSpeeds.vxMetersPerSecond,
            mTargetChassisSpeeds.vyMetersPerSecond + mAimChassisSpeeds.vyMetersPerSecond,
            mTargetChassisSpeeds.omegaRadiansPerSecond + mAimChassisSpeeds.omegaRadiansPerSecond
        );

        // SlewRateLimiter: 直接限制速度變化率（加速度），不會提前減速
        double xSpeed = xSpeedLimiter.calculate(sumChassisSpeeds.vxMetersPerSecond);
        double ySpeed = ySpeedLimiter.calculate(sumChassisSpeeds.vyMetersPerSecond);
        double zSpeed = zSpeedLimiter.calculate(sumChassisSpeeds.omegaRadiansPerSecond);

        var chassisSpeeds = new ChassisSpeeds(xSpeed, ySpeed, zSpeed);

        if (chassisVxEntry != null) {
            chassisVxEntry.setDouble(chassisSpeeds.vxMetersPerSecond);
            chassisVyEntry.setDouble(chassisSpeeds.vyMetersPerSecond);
            chassisOmegaEntry.setDouble(chassisSpeeds.omegaRadiansPerSecond);
        } else {
            SmartDashboard.putNumber("Chassis/vx", chassisSpeeds.vxMetersPerSecond);
            SmartDashboard.putNumber("Chassis/vy", chassisSpeeds.vyMetersPerSecond);
            SmartDashboard.putNumber("Chassis/omega", chassisSpeeds.omegaRadiansPerSecond);
        }

        SwerveModuleState[] mStates;
        if (mSwerveMode == SwerveMode.ROBOT_CENTRIC) 
            mStates = SwerveConstants.kSwerveKinematics.toSwerveModuleStates(chassisSpeeds);
        else
            mStates = SwerveConstants.kSwerveKinematicsCamera.toSwerveModuleStates(chassisSpeeds);
        setModuleStates(mStates);

        // 模擬環境：用 omega 積分更新模擬陀螺儀角度
        if (RobotBase.isSimulation()) {
            simGyroAngleDeg += Math.toDegrees(zSpeed) * 0.02;
        }
    }
 
    public void run() {
        // runSetStates() 已移至 periodic() 中，不再需要獨立排程
        resetPID();
    }

    public SwerveMode getSwerveMode() {
        return mSwerveMode;
    }

    public void setSwerveMode(SwerveMode mode) {
        if (mode != mSwerveMode){
            mSwerveMode = mode;
        }
    }

    private Rotation2d getGyroAngle() {
        if (RobotBase.isSimulation()) {
            return Rotation2d.fromDegrees(simGyroAngleDeg);
        }
        return mPigeonIMU.getRotation2d();
        // return mPigeonIMU.getRotation2d().plus(Rotation2d.fromDegrees(180)); //260220
        // return mPigeonIMU.getRotation2d().unaryMinus().plus(Rotation2d.fromDegrees(180));
    }

    private double getGyroRateDps() {
        if (RobotBase.isSimulation()) {
            return 0; // 模擬中不需要精確的角速度
        }
        return mPigeonIMU.getAngularVelocityZDevice().getValueAsDouble();
    }

    /***
     * 
     * @return gyro degrees between 180 and -180
     */
    private double getGyroAngleDegrees() {
        return MathUtil.inputModulus(getGyroAngle().getDegrees(), -180, 180);
    }

    /**
     * set the moving speed - Input range: [-1, 1]
     * 
     * @param xSpeed percent power in the X direction (X 方向的功率百分比)
     * @param ySpeed percent power in the Y direction (Y 方向的功率百分比)
     * @param zSpeed percent power for rotation (旋轉的功率百分比)
     * @param fieldOriented configure robot movement style (設置機器運動方式) (field or robot oriented)
     */

    public void setSpeed(double xSpeed, double ySpeed, double zSpeed, boolean fieldOriented) {
        if (fieldOriented) {
            // IMU used for field oriented control
            // IMU 用於 Field Oriented Control
            // 如果我們在紅方，駕駛員的「前進」和「向左」相對於場地座標是顛倒的
            // 這還沒測試!!!!! 需要測試!!
            if (isAllianceRed()) {
                xSpeed = -xSpeed;
                ySpeed = -ySpeed;
            }
            mTargetChassisSpeeds = ChassisSpeeds.fromFieldRelativeSpeeds(-xSpeed, -ySpeed, zSpeed, getGyroAngle());
            // SmartDashboard.putNumber("Gyro/Logic Angle (Deg)", logicAngle);
        } else {
            mTargetChassisSpeeds = new ChassisSpeeds(xSpeed, ySpeed, zSpeed);
        }
    }

    public void setAimSpeed(ChassisSpeeds speeds) {
        mAimChassisSpeeds = speeds;
    }
    
    /**
     * Sets the target chassis speeds for autonomous control
     *
     * @param speeds The desired chassis speeds, represented as a ChassisSpeeds object.
     *               - vxMetersPerSecond: The forward velocity in meters per second.
     *               - vyMetersPerSecond: The sideways velocity in meters per second.
     *               - omegaRadiansPerSecond: The angular velocity in radians per second.
     */
    public void setChassisSpeeds(ChassisSpeeds speeds) {
        mTargetChassisSpeeds = new ChassisSpeeds(speeds.vxMetersPerSecond*0.185, speeds.vyMetersPerSecond*0.185, speeds.omegaRadiansPerSecond*0.21);
    }

    public void zeroRotor() {
        mLeftFrontModule.setRotorAngle(0);
        mLeftFrontModule.setThrottleSpeed(0);
        mRightFrontModule.setRotorAngle(0);
        mRightFrontModule.setThrottleSpeed(0);
        mLeftRearModule.setRotorAngle(0);
        mLeftRearModule.setThrottleSpeed(0);
        mRightRearModule.setRotorAngle(0);
        mRightRearModule.setThrottleSpeed(0);
    }
    /**
     * Get current swerve module states
     * 輸出 4 個 Swerve Module 的當前狀態 modules
     * 
     * @return swerve module states
     */
    public SwerveModuleState[] getModuleStates() {
        return new SwerveModuleState[]{
            mLeftFrontModule.getState(), 
            mRightFrontModule.getState(), 
            mLeftRearModule.getState(), 
            mRightRearModule.getState()
        };
    }

    /**
     * Get current swerve module positions
     * 
     * @return swerve module positions 
     */
    public SwerveModulePosition[] getModulePositions() {
        return new SwerveModulePosition[] {
            mLeftFrontModule.getPosition(), 
            mRightFrontModule.getPosition(), 
            mLeftRearModule.getPosition(), 
            mRightRearModule.getPosition()
        };
    }

    /**
     * Sets swerve module states
     * 設置 4 個 Swerve module 的狀態。
     * 
     * @param desiredStates array of desired states, order: [leftFront, leftRear, rightFront, rightRear]
     */
    public void setModuleStates(SwerveModuleState[] desiredStates) {
        SwerveDriveKinematics.desaturateWheelSpeeds(desiredStates, getMaxVelocity());

        if (mod0SpeedEntry != null) {
            mod0SpeedEntry.setDouble(desiredStates[0].speedMetersPerSecond);
            mod0AngleEntry.setDouble(desiredStates[0].angle.getDegrees());
        } else {
            SmartDashboard.putNumber("Module0/speed", desiredStates[0].speedMetersPerSecond);
            SmartDashboard.putNumber("Module0/angle", desiredStates[0].angle.getDegrees());
        }

        mLeftFrontModule.setState(desiredStates[0]);
        mRightFrontModule.setState(desiredStates[1]);
        mLeftRearModule.setState(desiredStates[2]);
        mRightRearModule.setState(desiredStates[3]);
    }

    /**
     * Get predicted pose
     * 獲取機器人的當前位置
     * 
     * @return pose
     */
    public Pose2d getPose() {
        if (isUseLimelight) {
            return poseEstimator.getEstimatedPosition();
        }
        return mOdometry.getPoseMeters();
    }

    /**
     * Set robot pose
     * 將測程法（odometry）位置設置為給與的 x、y、位置和角度
     * 
     * @param pose robot pose
     */
    public void setPose(Pose2d pose) {
        if (isUseLimelight) {
            poseEstimator.resetPosition(getGyroAngle(), getModulePositions(), pose);
        } else {
            mOdometry.resetPosition(getGyroAngle(), getModulePositions(), pose);
        }
    }

    public void resetPose(Pose2d pose) {
        // 將 IMU 的 yaw 同步為目標位姿的角度
        // 這是 PathPlanner 在 auto 開始時呼叫的方法（resetOdom: true）
        // 同步 IMU 後，auto 結束進入 teleop 時角度就已經是場地座標，不需要手動校正
        double headingDeg = pose.getRotation().getDegrees();
        mPigeonIMU.setYaw(headingDeg);
        if (RobotBase.isSimulation()) {
            simGyroAngleDeg = headingDeg;
        }

        // 用已經同步好的 gyro 角度來重設 poseEstimator / odometry
        Rotation2d newGyroAngle = getGyroAngle();
        if (poseEstimator != null) {
            poseEstimator.resetPosition(newGyroAngle, getModulePositions(), pose);
        }
        
        if (mOdometry != null) {
            mOdometry.resetPosition(newGyroAngle, getModulePositions(), pose);
        }
    }
    public double getMaxVelocity() {
        // 使用 Constants 中根據齒輪比和輪徑計算出的物理最大速度
        return SwerveConstants.kMaxPhysicalSpeedMps;
    }

    /**
     * 專門給 PathPlanner 使用的驅動方法
     * 接收機器人相對速度 (Robot Relative ChassisSpeeds)
     */
    public void drive(ChassisSpeeds speeds) {        
        SwerveModuleState[] swerveModuleStates = 
            SwerveConstants.kSwerveKinematics.toSwerveModuleStates(speeds);

        // 進行速度飽和限制 (Desaturate)
        SwerveDriveKinematics.desaturateWheelSpeeds(swerveModuleStates, getMaxVelocity());

        // 設定模組狀態
        setModuleStates(swerveModuleStates);
    }

    /**
     * 用 Limelight 校正 XY 位置，但角度保持使用 IMU
     * （因為 Limelight 角度可能有偏差，IMU 的角度更可靠）
     */
    public void resetPoseToLimelight() {
        LimelightHelpers.PoseEstimate mt2;
        mt2 = LimelightHelpers.getBotPoseEstimate_wpiBlue(limelightName);
        if (mt2 != null && mt2.tagCount > 0) {
            // 只使用 Limelight 的 XY 位置，角度保持 IMU 的值
            Pose2d correctedPose = new Pose2d(
                mt2.pose.getTranslation(),  // XY 來自 Limelight
                getGyroAngle()              // 角度來自 IMU
            );
            this.setPose(correctedPose);
            SmartDashboard.putString("Vision/LimelightReset", 
                "XY校正成功! x=" + mt2.pose.getX() + " y=" + mt2.pose.getY() + " (角度保持IMU)");
        } else {
            SmartDashboard.putString("Vision/LimelightReset", "找不到 AprilTag，無法校正位置");
        }
    }

    public ChassisSpeeds getChassisSpeeds() {
        return SwerveConstants.kSwerveKinematics.toChassisSpeeds(
            mLeftFrontModule.getState(), 
            mRightFrontModule.getState(), 
            mLeftRearModule.getState(), 
            mRightRearModule.getState()
        );
    }

    public void resetIMU() {
        resetIMU(0);
    }

    public void resetIMU(double angle) {
        mPigeonIMU.reset();
        mPigeonIMU.setYaw(angle);
        if (RobotBase.isSimulation()) {
            simGyroAngleDeg = angle;
        }
        // 同步重設 poseEstimator / odometry 的航向，避免 IMU 與位姿估計不一致
        Pose2d currentPose = getPose();
        Pose2d correctedPose = new Pose2d(currentPose.getTranslation(), Rotation2d.fromDegrees(angle));
        setPose(correctedPose);
    }

    public void updateSmartDashboard() {
        // mLeftFrontModule.updateSmartDashboard();
        // mRightFrontModule.updateSmartDashboard();
        // mLeftRearModule.updateSmartDashboard();
        // mRightRearModule.updateSmartDashboard();
    }

    public void resetPID() {
        System.out.println("Swerve.resetPID");
        mLeftFrontModule.resetPID();
        mRightFrontModule.resetPID();
        mLeftRearModule.resetPID();
        mRightRearModule.resetPID();
        xSpeedLimiter.reset(0);
        ySpeedLimiter.reset(0);
        zSpeedLimiter.reset(0);
    }

    public void disabledInit() {
        mLeftFrontModule.setRotorSpeed(0);
        mLeftFrontModule.setThrottleSpeed(0);
        mRightFrontModule.setRotorSpeed(0);
        mRightFrontModule.setThrottleSpeed(0);
        mLeftRearModule.setRotorSpeed(0);
        mLeftRearModule.setThrottleSpeed(0);
        mRightRearModule.setRotorSpeed(0);
        mRightRearModule.setThrottleSpeed(0);
    }

    public boolean isAutoUpdateIMU() {
        return autoUpdateIMU;
    }

    public void setAutoUpdateIMU(boolean autoUpdateIMU) {
        this.autoUpdateIMU = autoUpdateIMU;
    }

    @Override
    public void initSendable(SendableBuilder builder) {
        builder.addBooleanProperty("AutoUpdateIMU", this::isAutoUpdateIMU, this::setAutoUpdateIMU);
    }
}
