program Rotor_check_singleMode;
  inputs
    roll,pitch,yaw (t) using closest(t);
  constants
	P_LOW = -0.0884;
  outputs
    roll,pitch,yaw,mode at every 1 sec;
  errors
    e1: pitch;
  modes
	m0: e1 >= P_LOW "Normal";
	m1: e1 < P_LOW "Rotor failure";
end;