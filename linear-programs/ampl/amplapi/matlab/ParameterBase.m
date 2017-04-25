classdef ParameterBase < Entity
properties
end
methods
	function obj = ParameterBase(impl)
		obj@Entity(impl);
	end

	function result = getInstances(self)
		try
			result = self.impl.getInstances();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Parameter:getInstances', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Parameter:getInstances', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = hasDefault(self)
		try
			result = self.impl.hasDefault();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Parameter:hasDefault', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Parameter:hasDefault', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function setValues(self,varargin)
		try
			if size(varargin) > 0
				self.impl.setValues(varargin);
			else
				self.impl.setValues();
			end
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Parameter:setValues', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Parameter:setValues', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function set(self,varargin)
		try
			if size(varargin) > 0
				self.impl.set(varargin);
			else
				self.impl.set();
			end
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Parameter:set', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Parameter:set', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = isSymbolic(self)
		try
			result = self.impl.isSymbolic();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Parameter:isSymbolic', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Parameter:isSymbolic', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end
end
end
