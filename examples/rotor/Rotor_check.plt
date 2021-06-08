program Rotor_check;
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
    roll,pitch,yaw,mode at every 1 msec;
  errors
    e1: roll;
	e2: pitch;
	e3: yaw;
  signatures
    s0: e1 = k, R_LOW <= k, k <= R_HIGH "Normal";
    s1: e1 = k, k < R_LOW "Rotor 1 failure";
	s1: e1 = k, R_HIGH < k "Rotor 1 failure";
	
    s2: e2 = k, k < R_LOW "Rotor 1 failure";
	s2: e2 = k, R_HIGH < k "Rotor 1 failure";
	
	s0: e1 = k, R_LOW <= k, k <= R_HIGH "Normal";
    s1: e1 = k, k < R_LOW "Rotor 1 failure";
	s1: e1 = k, R_HIGH < k "Rotor 1 failure";
end;
