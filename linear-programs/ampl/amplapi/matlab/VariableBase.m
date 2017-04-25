classdef VariableBase < Entity
properties
end
methods
	function obj = VariableBase(impl)
		obj@Entity(impl);
	end

	function result = uslack(self)
		try
			result = self.impl.uslack();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Variable:uslack', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Variable:uslack', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = defeqn(self)
		try
			result = self.impl.defeqn();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Variable:defeqn', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Variable:defeqn', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = rc(self)
		try
			result = self.impl.rc();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Variable:rc', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Variable:rc', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = dual(self)
		try
			result = self.impl.dual();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Variable:dual', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Variable:dual', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function fix(self,varargin)
		try
			if size(varargin) > 0
				self.impl.fix(varargin);
			else
				self.impl.fix();
			end
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Variable:fix', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Variable:fix', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = value(self)
		try
			result = self.impl.value();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Variable:value', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Variable:value', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = slack(self)
		try
			result = self.impl.slack();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Variable:slack', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Variable:slack', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = getInstances(self)
		try
			result = self.impl.getInstances();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Variable:getInstances', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Variable:getInstances', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = status(self)
		try
			result = self.impl.status();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Variable:status', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Variable:status', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = init0(self)
		try
			result = self.impl.init0();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Variable:init0', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Variable:init0', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = urc(self)
		try
			result = self.impl.urc();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Variable:urc', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Variable:urc', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = integrality(self)
		try
			result = self.impl.integrality();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Variable:integrality', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Variable:integrality', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = init(self)
		try
			result = self.impl.init();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Variable:init', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Variable:init', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = ub2(self)
		try
			result = self.impl.ub2();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Variable:ub2', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Variable:ub2', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = ub1(self)
		try
			result = self.impl.ub1();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Variable:ub1', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Variable:ub1', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function setValue(self,tdouble1)
		try
			self.impl.setValue(tdouble1);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Variable:setValue', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Variable:setValue', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = ub0(self)
		try
			result = self.impl.ub0();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Variable:ub0', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Variable:ub0', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = astatus(self)
		try
			result = self.impl.astatus();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Variable:astatus', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Variable:astatus', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = lslack(self)
		try
			result = self.impl.lslack();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Variable:lslack', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Variable:lslack', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = lb2(self)
		try
			result = self.impl.lb2();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Variable:lb2', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Variable:lb2', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = lb0(self)
		try
			result = self.impl.lb0();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Variable:lb0', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Variable:lb0', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = lb1(self)
		try
			result = self.impl.lb1();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Variable:lb1', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Variable:lb1', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = ub(self)
		try
			result = self.impl.ub();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Variable:ub', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Variable:ub', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = lrc(self)
		try
			result = self.impl.lrc();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Variable:lrc', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Variable:lrc', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = lb(self)
		try
			result = self.impl.lb();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Variable:lb', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Variable:lb', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function unfix(self)
		try
			self.impl.unfix();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Variable:unfix', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Variable:unfix', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = sstatus(self)
		try
			result = self.impl.sstatus();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Variable:sstatus', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Variable:sstatus', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end
end
end
