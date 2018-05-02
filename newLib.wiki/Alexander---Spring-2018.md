# TK1 and RACECARJ Notes


## IMU Calibration

The calibration for the IMU was done following this [calibration](http://wiki.ros.org/razor_imu_9dof#Sensor_Calibration) guide. 

1. Open razor_imu_9dof/src/Razor_AHRS/Razor_AHRS.ino with Arduino, [(Installing the 9DoF Razor Arduino Core)](https://learn.sparkfun.com/tutorials/9dof-razor-imu-m0-hookup-guide?_ga=2.202271377.102418616.1524613608-803068393.1505939054#installing-the-9dof-razor-arduino-core), and find the section "USER SETUP AREA" / "SENSOR CALIBRATION". This is where you put the calibration values later!

2. Connect the Razor to your computer, set the correct serial port in Arduino and open the serial monitor

3. Set the firmware output mode to calibration by sending the string #oc. You should now see the output like this
`accel x,y,z (min/max) = -5.00/-1.00  25.00/29.00  225.00/232.00`

### Calibrate the Accelerometer

1. We'll try to find the minimum and maximum output values for the earth gravitation on each axis. When you move the board, move it real slowly, so the acceleration you apply to it is as small as possible. We only want pure gravity!
2. Take the board and point straight down with the x-axis (remember: x-axis = towards the short edge with the connector holes). While you do that, you can see the x-maximum (the second value) getting bigger.
3. Hold the board very still and reset the measurement by sending #oc again.
4. Now carefully tilt the board a little in every direction until the value does not get bigger any more and write down the x-maximum value.
5. Do the same thing for the opposite side (x-axis pointing up) to get the x-minimum: bring into position, send #oc to reset measurement, find x-minimum value and write it down.
6. Do the same thing for the z-axis (down and up) and the y-axis (right and left).
7. If you think you messed up the measurement by shaking or moving the board too fast, you can always reset by sending #oc.
8. You should now have all the min/max values. Put them into Razor_AHRS.ino.
9. CAUTION: You have to be really careful when doing this! Even slightly tapping the board with the finger messes up the measurement (try it!) and leads to wrong calibration. Use #oc very often and double check your min/max values)
10. **CyPhyHouse Notes for the TK1:** Just be careful and gentle when obtaining these calibrations values. You might want to do it a couple of times (3-5) and average the values!! That is what I did. 

### Calibrating the gyroscope

1. Lay the Razor AHRS still on the table.
2. We’re still in calibration mode for the accelerometer. Send #on twice, which will move calibration past the magnetometer to the gyroscope.
3. Wait for 10 seconds, and do not move the Razor AHRS. It will collect and average the noise of the gyroscope on all three axes.
4. You should now have output that looks like this:
`gyro x,y,z (current/average) = -29.00/-27.98  102.00/100.51  -5.00/-5.85`
5. If you think you messed up the measurement by shaking or moving the board, you can reset by sending #oc.
6. Take the second values of each pair and put them into Razor_AHRS.ino.

### Calibrating the magnetometer

This procedure compensates for hard and soft iron errors. Still, in both cases the source of distortion has to be fixed in the sensor coordinate system, i.e. moving and rotating with the sensor.

1. To start calibrating, put the sensor in the magnetic environment where it will be used later - e.g. in the exact spot on your robot. Robots have strong magnets in their motors, and magnetometers are frequently mounted above the robot to create a physical separation.
2. Use one of the procedures in the 'Testing the AHRS' section to display Razor AHRS output
3. Test whether the sensor is affected by proximity to the robot. Move the Razor AHRS closer and further away from the robot while holding its attitude constant, and make sure readings at your desired mounting location aren't affected by proximity to the robot.
4. Test whether the sensor is affected when the motors run. Run the robot motors forward and backward (with the robot up on a stand!). If the output changes, you need to move the Razor AHRS further away from the robot.
5. Download and install [Processing.](https://processing.org) We will use it to compile and run the test program. Any Processing versions 2.x should work fine, tested with version 2.0.3.
6. Quit all applications that read from the sensor (e.g. Serial Monitor, 3D Visualization GUI, …) and run the Processing magnetometer calibration sketch located in ` $ razor_imu_9dof/magnetometer_calibration/Processing/Magnetometer_calibration `
7. Note: you have to install the EJML library first, or else the sketch won’t run. How to do that? Have a look at the NOTE at the top of Magnetometer_calibration.pde.
8. Try to rotate the sensor in a way so that you cover all orientations so you produce dots that more or less evenly cover the sphere.
9. In a mostly undistorted environment this could look something like this:
10. **CyPhyHouse Notes for the TK1:**  [Processing](https://processing.org) was installed, but there were some other packages needed for the TK1. Nicole [Installed](https://sites.google.com/view/nicolechan/cyphyhouse-work/build-process) these packages and was able to calibrate the magnetometer. Also, be careful and make sure that the place where the IMU is mounted does not have strong magnetic fields (motors for instance)


## SVO Odometry and ORB_SLAM2

For the purpose of obtaining odometry and control as well as other benefits of computer vision for the F1/10 car, the library ORB_SLAM2 was explored. [ORB-SLAM2](https://github.com/raulmur/ORB_SLAM2) is a real-time SLAM library for Monocular, Stereo and RGB-D cameras that computes the camera trajectory and a sparse 3D reconstruction (in the stereo and RGB-D case with true scale).

This package runs OpenCV as well as linear algebra, visualization and user interfaces packages. 

### Prerequisites

1. Ubuntu 12.04, 14.04 and 16.04 will run this package succesfully.
2. C++11 or C++0x Compiler. **CyPhyHouse Notesfor the TK1:** I tried to install this on my Mac first since I was considering the lack of memory in the TK1. However, I had issues since the OS compiler is clang. The CMakeLists.txt. uses libraries for C++, so clang will give erros.
3. [Pangolin](https://github.com/stevenlovegrove/Pangolin), [OpenCV](https://opencv.org), [Eigen3](http://eigen.tuxfamily.org/index.php?title=Main_Page) **at least 3.1.0**
4. **Note** [DBoW2](https://github.com/dorian3d/DBoW2) and [g2o](https://github.com/RainerKuemmerle/g2o) are also required, **BUT** these are included in the ORB_SLAM2 package, so you do not need to install those seperately.
5. **CyPhyHouse Notes for the TK1:** These packages are somewhat large, and we would need a minimun of approximately 6GB available. I tried to install this on the TK1, but some of the libraries did not install due to memory issues. For better results, I higly recommend the TX2.

### Build
If you have all the prerequisites, then you only need to download the **ORB_SLAM2** and install it. The package has a build.sh script that takes care of building all the the ORB_SLAM, and the DBoW2 and g2o (Thirdparty libraries). Execute the following comands in the termianal.

1. `$ git clone https://github.com/raulmur/ORB_SLAM2.git ORB_SLAM2 `
2. `$ cd ORB_SLAM2 `
3. `$ chmod +x build.sh `
4. `$ ./build.sh `

**CyPhyHouse Notes for the TK1:**  Another way to do this, which is the way I did it, is to use Anaconda or miniconda, and installing the prerequired packages on a virtual environment and finally install the ORB_SLAM2 the executing the commands above. Keep in mind that if you decide to do this, on TK1, TX2 or other CPUs, you will need more meory (~ 3GB for Anaconda and 300MB for miniconda). I used Anaconda and it the installation of the ORB_SLAM2 packages were build succesfully. 

After installing the package you can run some examples included in the library.

## Car Build

For the sensor mount, the [RACECARJ](https://racecarj.com/collections/all) was used on the f1tenth. The original lidar base ![original lidar base](https://github.com/axander89/f1tenth_code/blob/master/RacecarJ/Images/LidarMountOriginal.png) was modified to fit aditional parts to make the mount taller. This file was 3D designed ![3D designed](https://github.com/axander89/f1tenth_code/blob/master/RacecarJ/Images/LidarMount_Modified2.PNG) 

using creo, and [3D printed](https://github.com/axander89/f1tenth_code/blob/master/RacecarJ/Images/LidarMount_Modified3.PNG) at the [Innovation Studio](http://innovationstudio.mechse.illinois.edu). 
In the 3D design mount, the outer slits are to fit the two aditional mounts which attach the sensor mount to the chassis. The files are at [creo](https://github.com/axander89/f1tenth_code/blob/master/RacecarJ/Creo.zip)

