program Rotor;
  inputs
    roll,pitch,yaw (t) using closest(t);
  constants
    R_LOW = -0.0153;
	R_HIGH = 0.006;
	P_LOW = -0.0884;
    P_HIGH = -0.0703;
	Y_LOW = -0.0040;
	Y_HIGH = 0.0022;
  outputs
    roll,pitch,yaw at every 0.1 sec;
end;