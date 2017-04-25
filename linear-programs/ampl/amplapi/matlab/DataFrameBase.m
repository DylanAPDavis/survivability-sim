classdef DataFrameBase < handle
properties
	impl
end
methods
	function obj = DataFrameBase(impl)
		obj.impl = impl;
	end

	function result = getColumnAsDoubles(self,tObject1)
		try
			result = self.impl.getColumnAsDoubles(tObject1);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:DataFrame:getColumnAsDoubles', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:DataFrame:getColumnAsDoubles', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = getNumCols(self)
		try
			result = self.impl.getNumCols();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:DataFrame:getNumCols', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:DataFrame:getNumCols', char(ex.getMessage().replace('\', '/')));
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
					err = MException('AMPLAPI:DataFrame:iterator', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:DataFrame:iterator', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function setMatrix(self,tObject1,varargin)
		try
			if size(varargin) > 0
				self.impl.setMatrix(varargin);
			else
				self.impl.setMatrix();
			end
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:DataFrame:setMatrix', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:DataFrame:setMatrix', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function setValue(self,varargin)
		try
			if size(varargin) > 0
				self.impl.setValue(varargin);
			else
				self.impl.setValue();
			end
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:DataFrame:setValue', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:DataFrame:setValue', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = getRowByIndex(self,tint1)
		try
			result = self.impl.getRowByIndex(tint1);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:DataFrame:getRowByIndex', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:DataFrame:getRowByIndex', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = getColumnAsStrings(self,tObject1)
		try
			result = self.impl.getColumnAsStrings(tObject1);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:DataFrame:getColumnAsStrings', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:DataFrame:getColumnAsStrings', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = getNumIndices(self)
		try
			result = self.impl.getNumIndices();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:DataFrame:getNumIndices', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:DataFrame:getNumIndices', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function addColumn(self,varargin)
		try
			if size(varargin) > 0
				self.impl.addColumn(varargin);
			else
				self.impl.addColumn();
			end
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:DataFrame:addColumn', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:DataFrame:addColumn', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = getColumn(self,tObject1)
		try
			result = self.impl.getColumn(tObject1);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:DataFrame:getColumn', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:DataFrame:getColumn', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = getHeaders(self)
		try
			result = self.impl.getHeaders();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:DataFrame:getHeaders', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:DataFrame:getHeaders', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = getNumRows(self)
		try
			result = self.impl.getNumRows();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:DataFrame:getNumRows', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:DataFrame:getNumRows', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function addRow(self,varargin)
		try
			if size(varargin) > 0
				self.impl.addRow(varargin);
			else
				self.impl.addRow();
			end
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:DataFrame:addRow', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:DataFrame:addRow', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function setColumn(self,param0,param1)
		try
			self.impl.setColumn(param0, param1);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:DataFrame:setColumn', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:DataFrame:setColumn', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = getRow(self,varargin)
		try
			if size(varargin) > 0
				result = self.impl.getRow(varargin);
			else
				result = self.impl.getRow();
			end
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:DataFrame:getRow', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:DataFrame:getRow', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end
end
end
