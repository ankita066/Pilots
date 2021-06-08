



	program ThresholdCheck;
		  inputs
			a, b(t) using closest(t);
		  constants
			A_LOW = -0.0884;
			A_HIGH = 0.0884;
		  outputs
			a,b,mode at every 1 sec;
		  errors
			e1: b - a;
		  modes
			m0: (e1 > A_LOW) and e1 < A_HIGH "Normal";
			m1: e1 <= A_LOW "Failure";
		end;






