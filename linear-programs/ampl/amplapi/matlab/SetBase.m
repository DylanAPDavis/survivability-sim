classdef SetBase < Entity
properties
end
methods
	function obj = SetBase(impl)
		obj@Entity(impl);
	end

	function result = arity(self)
		try
			result = self.impl.arity();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Set:arity', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Set:arity', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = isEmpty(self)
		try
			result = self.impl.isEmpty();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Set:isEmpty', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Set:isEmpty', char(ex.getMessage().replace('\', '/')));
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
					err = MException('AMPLAPI:Set:getInstances', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Set:getInstances', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = iterator(self)
		try
			result = self.impl.iterator();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Set:iterator', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Set:iterator', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = toArray(self,varargin)
		try
			if size(varargin) > 0
				result = self.impl.toArray(varargin);
			else
				result = self.impl.toArray();
			end
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Set:toArray', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Set:toArray', char(ex.getMessage().replace('\', '/')));
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
					err = MException('AMPLAPI:Set:setValues', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Set:setValues', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = containsAll(self,tCollection1)
		try
			result = self.impl.containsAll(tCollection1);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Set:containsAll', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Set:containsAll', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = contains(self,tObject1)
		try
			result = self.impl.contains(tObject1);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Set:contains', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Set:contains', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = size(self)
		try
			result = self.impl.size();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Set:size', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Set:size', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end
end
end
