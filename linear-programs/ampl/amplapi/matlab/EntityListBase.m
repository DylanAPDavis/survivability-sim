classdef EntityListBase < handle
properties
	impl
end
methods
	function obj = EntityListBase(impl)
		obj.impl = impl;
	end

	function result = isEmpty(self)
		try
			result = self.impl.isEmpty();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:EntityList:isEmpty', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:EntityList:isEmpty', char(ex.getMessage().replace('\', '/')));
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
					err = MException('AMPLAPI:EntityList:iterator', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:EntityList:iterator', char(ex.getMessage().replace('\', '/')));
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
					err = MException('AMPLAPI:EntityList:toArray', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:EntityList:toArray', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = listIterator(self,varargin)
		try
			if size(varargin) > 0
				result = self.impl.listIterator(varargin);
			else
				result = self.impl.listIterator();
			end
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:EntityList:listIterator', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:EntityList:listIterator', char(ex.getMessage().replace('\', '/')));
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
					err = MException('AMPLAPI:EntityList:containsAll', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:EntityList:containsAll', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = lastIndexOf(self,tObject1)
		try
			result = self.impl.lastIndexOf(tObject1);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:EntityList:lastIndexOf', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:EntityList:lastIndexOf', char(ex.getMessage().replace('\', '/')));
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
					err = MException('AMPLAPI:EntityList:contains', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:EntityList:contains', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = indexOf(self,tObject1)
		try
			result = self.impl.indexOf(tObject1);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:EntityList:indexOf', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:EntityList:indexOf', char(ex.getMessage().replace('\', '/')));
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
					err = MException('AMPLAPI:EntityList:size', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:EntityList:size', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end
end
end
