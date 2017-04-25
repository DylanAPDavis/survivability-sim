classdef TupleBase < handle
properties
	impl
end
methods
	function obj = TupleBase(impl)
		obj.impl = impl;
	end

	function result = clone(self)
		try
			result = self.impl.clone();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Tuple:clone', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Tuple:clone', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = equals(self,tObject1)
		try
			result = self.impl.equals(tObject1);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Tuple:equals', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Tuple:equals', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = hashCode(self)
		try
			result = self.impl.hashCode();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Tuple:hashCode', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Tuple:hashCode', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = join(self,varargin)
		try
			if size(varargin) > 0
				result = self.impl.join(varargin);
			else
				result = self.impl.join();
			end
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Tuple:join', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Tuple:join', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = subtuple(self,tint1,tint2)
		try
			result = self.impl.subtuple(tint1, tint2);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Tuple:subtuple', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Tuple:subtuple', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function insert(self,array1,tint2)
		try
			self.impl.insert(array1, tint2);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Tuple:insert', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Tuple:insert', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = elements(self)
		try
			result = self.impl.elements();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:Tuple:elements', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Tuple:elements', char(ex.getMessage().replace('\', '/')));
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
					err = MException('AMPLAPI:Tuple:size', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:Tuple:size', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end
end
end
