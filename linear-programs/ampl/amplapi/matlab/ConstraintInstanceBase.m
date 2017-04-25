classdef ConstraintInstanceBase < Instance
properties
end
methods
	function obj = ConstraintInstanceBase(impl)
		obj@Instance(impl);
	end

	function result = val(self)
		try
			result = self.impl.val();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:ConstraintInstance:val', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:ConstraintInstance:val', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = body(self)
		try
			result = self.impl.body();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:ConstraintInstance:body', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:ConstraintInstance:body', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = uslack(self)
		try
			result = self.impl.uslack();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:ConstraintInstance:uslack', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:ConstraintInstance:uslack', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = ubs(self)
		try
			result = self.impl.ubs();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:ConstraintInstance:ubs', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:ConstraintInstance:ubs', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function setDual(self,tdouble1)
		try
			self.impl.setDual(tdouble1);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:ConstraintInstance:setDual', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:ConstraintInstance:setDual', char(ex.getMessage().replace('\', '/')));
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
					err = MException('AMPLAPI:ConstraintInstance:status', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:ConstraintInstance:status', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function drop(self)
		try
			self.impl.drop();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:ConstraintInstance:drop', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:ConstraintInstance:drop', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = udual(self)
		try
			result = self.impl.udual();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:ConstraintInstance:udual', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:ConstraintInstance:udual', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = dinit0(self)
		try
			result = self.impl.dinit0();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:ConstraintInstance:dinit0', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:ConstraintInstance:dinit0', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = defvar(self)
		try
			result = self.impl.defvar();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:ConstraintInstance:defvar', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:ConstraintInstance:defvar', char(ex.getMessage().replace('\', '/')));
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
					err = MException('AMPLAPI:ConstraintInstance:astatus', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:ConstraintInstance:astatus', char(ex.getMessage().replace('\', '/')));
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
					err = MException('AMPLAPI:ConstraintInstance:lslack', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:ConstraintInstance:lslack', char(ex.getMessage().replace('\', '/')));
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
					err = MException('AMPLAPI:ConstraintInstance:dual', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:ConstraintInstance:dual', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = ldual(self)
		try
			result = self.impl.ldual();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:ConstraintInstance:ldual', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:ConstraintInstance:ldual', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = dinit(self)
		try
			result = self.impl.dinit();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:ConstraintInstance:dinit', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:ConstraintInstance:dinit', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function restore(self)
		try
			self.impl.restore();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:ConstraintInstance:restore', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:ConstraintInstance:restore', char(ex.getMessage().replace('\', '/')));
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
					err = MException('AMPLAPI:ConstraintInstance:ub', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:ConstraintInstance:ub', char(ex.getMessage().replace('\', '/')));
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
					err = MException('AMPLAPI:ConstraintInstance:lb', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:ConstraintInstance:lb', char(ex.getMessage().replace('\', '/')));
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
					err = MException('AMPLAPI:ConstraintInstance:sstatus', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:ConstraintInstance:sstatus', char(ex.getMessage().replace('\', '/')));
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
					err = MException('AMPLAPI:ConstraintInstance:slack', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:ConstraintInstance:slack', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = lbs(self)
		try
			result = self.impl.lbs();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:ConstraintInstance:lbs', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:ConstraintInstance:lbs', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end
end
end
