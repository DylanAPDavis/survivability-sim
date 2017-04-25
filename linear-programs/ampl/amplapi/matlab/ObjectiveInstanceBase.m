classdef ObjectiveInstanceBase < Instance
properties
end
methods
	function obj = ObjectiveInstanceBase(impl)
		obj@Instance(impl);
	end

	function result = message(self)
		try
			result = self.impl.message();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:ObjectiveInstance:message', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:ObjectiveInstance:message', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = result(self)
		try
			result = self.impl.result();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:ObjectiveInstance:result', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:ObjectiveInstance:result', char(ex.getMessage().replace('\', '/')));
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
					err = MException('AMPLAPI:ObjectiveInstance:drop', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:ObjectiveInstance:drop', char(ex.getMessage().replace('\', '/')));
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
					err = MException('AMPLAPI:ObjectiveInstance:restore', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:ObjectiveInstance:restore', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = exitcode(self)
		try
			result = self.impl.exitcode();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:ObjectiveInstance:exitcode', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:ObjectiveInstance:exitcode', char(ex.getMessage().replace('\', '/')));
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
					err = MException('AMPLAPI:ObjectiveInstance:value', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:ObjectiveInstance:value', char(ex.getMessage().replace('\', '/')));
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
					err = MException('AMPLAPI:ObjectiveInstance:astatus', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:ObjectiveInstance:astatus', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end
end
end
