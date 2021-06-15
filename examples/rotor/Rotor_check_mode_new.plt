	
	
	program Rotor_check_mode_new;
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
			roll,pitch,yaw,mode at every 0.1 sec;
		  errors
			e1: roll;
			e2: pitch;
			e3: yaw;
		  modes
			m0: e2 [P_LOW .. P_HIGH] or (e2 < P_LOW and e3 [Y_LOW .. Y_HIGH]) 
					or (e2 < P_LOW and e3 > Y_HIGH and e1 [R_LOW .. R_HIGH]) "Normal";
			m1: e2 < P_LOW and e3 < Y_LOW "Rotor 1 failure";
			m2: e2 < P_LOW and e3 > Y_HIGH and e1 < R_LOW "Rotor 2 failure";
			m3: e2 < P_LOW and e3 > Y_HIGH and e1 > R_HIGH "Rotor 6 failure";
		end;
 




