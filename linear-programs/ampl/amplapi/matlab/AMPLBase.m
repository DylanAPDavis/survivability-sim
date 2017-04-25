classdef AMPLBase < handle
properties
	impl
end
methods
	function obj = AMPLBase(impl)
		obj.impl = impl;
	end

	function result = getErrorHandler(self)
		try
			result = self.impl.getErrorHandler();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:getErrorHandler', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:getErrorHandler', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function readAsync(self,tString1,tRunnable2)
		try
			self.impl.readAsync(tString1, tRunnable2);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:readAsync', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:readAsync', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = getValue(self,tString1)
		try
			result = self.impl.getValue(tString1);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:getValue', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:getValue', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = getSets(self)
		try
			result = self.impl.getSets();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:getSets', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:getSets', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = getObjectives(self)
		try
			result = self.impl.getObjectives();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:getObjectives', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:getObjectives', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = getVariables(self)
		try
			result = self.impl.getVariables();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:getVariables', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:getVariables', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = getConstraint(self,tString1)
		try
			result = self.impl.getConstraint(tString1);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:getConstraint', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:getConstraint', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function setIntOption(self,tString1,tint2)
		try
			self.impl.setIntOption(tString1, tint2);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:setIntOption', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:setIntOption', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = getEntity(self,tString1)
		try
			result = self.impl.getEntity(tString1);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:getEntity', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:getEntity', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function evalAsync(self,tString1,tRunnable2)
		try
			self.impl.evalAsync(tString1, tRunnable2);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:evalAsync', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:evalAsync', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = isBusy(self)
		try
			result = self.impl.isBusy();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:isBusy', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:isBusy', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function show(self,varargin)
		try
			if size(varargin) > 0
				self.impl.show(varargin);
			else
				self.impl.show();
			end
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:show', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:show', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = getDblOption(self,tString1)
		try
			result = self.impl.getDblOption(tString1);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:getDblOption', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:getDblOption', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function close(self)
		try
			self.impl.close();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:close', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:close', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = getOption(self,tString1)
		try
			result = self.impl.getOption(tString1);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:getOption', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:getOption', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function solve(self)
		try
			self.impl.solve();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:solve', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:solve', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = getConstraints(self)
		try
			result = self.impl.getConstraints();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:getConstraints', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:getConstraints', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = getObjective(self,tString1)
		try
			result = self.impl.getObjective(tString1);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:getObjective', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:getObjective', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = getBoolOption(self,tString1)
		try
			result = self.impl.getBoolOption(tString1);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:getBoolOption', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:getBoolOption', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function setOption(self,tString1,tString2)
		try
			self.impl.setOption(tString1, tString2);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:setOption', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:setOption', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function read(self,tString1)
		try
			self.impl.read(tString1);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:read', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:read', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function reset(self)
		try
			self.impl.reset();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:reset', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:reset', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function expand(self,varargin)
		try
			if size(varargin) > 0
				self.impl.expand(varargin);
			else
				self.impl.expand();
			end
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:expand', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:expand', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = getSet(self,tString1)
		try
			result = self.impl.getSet(tString1);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:getSet', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:getSet', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function setDblOption(self,tString1,tdouble2)
		try
			self.impl.setDblOption(tString1, tdouble2);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:setDblOption', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:setDblOption', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = isRunning(self)
		try
			result = self.impl.isRunning();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:isRunning', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:isRunning', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = getParameter(self,tString1)
		try
			result = self.impl.getParameter(tString1);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:getParameter', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:getParameter', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function eval(self,tString1)
		try
			self.impl.eval(tString1);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:eval', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:eval', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function readData(self,tString1)
		try
			self.impl.readData(tString1);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:readData', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:readData', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function display(self,varargin)
		try
			if size(varargin) > 0
				self.impl.display(varargin);
			else
				self.impl.display();
			end
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:display', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:display', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function setData(self,varargin)
		try
			if size(varargin) > 0
				self.impl.setData(varargin);
			else
				self.impl.setData();
			end
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:setData', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:setData', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function setOutputHandler(self,tOutputHandler1)
		try
			self.impl.setOutputHandler(tOutputHandler1);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:setOutputHandler', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:setOutputHandler', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function readDataAsync(self,tString1,tRunnable2)
		try
			self.impl.readDataAsync(tString1, tRunnable2);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:readDataAsync', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:readDataAsync', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function interrupt(self)
		try
			self.impl.interrupt();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:interrupt', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:interrupt', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = getParameters(self)
		try
			result = self.impl.getParameters();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:getParameters', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:getParameters', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = getVariable(self,tString1)
		try
			result = self.impl.getVariable(tString1);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:getVariable', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:getVariable', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = getData(self,varargin)
		try
			if size(varargin) > 0
				result = self.impl.getData(varargin);
			else
				result = self.impl.getData();
			end
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:getData', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:getData', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function setBoolOption(self,tString1,tboolean2)
		try
			self.impl.setBoolOption(tString1, tboolean2);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:setBoolOption', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:setBoolOption', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function readTable(self,tString1)
		try
			self.impl.readTable(tString1);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:readTable', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:readTable', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function setErrorHandler(self,tErrorHandler1)
		try
			self.impl.setErrorHandler(tErrorHandler1);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:setErrorHandler', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:setErrorHandler', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function writeTable(self,tString1)
		try
			self.impl.writeTable(tString1);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:writeTable', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:writeTable', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = getOutputHandler(self)
		try
			result = self.impl.getOutputHandler();
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:getOutputHandler', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:getOutputHandler', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = getIntOption(self,tString1)
		try
			result = self.impl.getIntOption(tString1);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:getIntOption', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:getIntOption', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function solveAsync(self,tRunnable1)
		try
			self.impl.solveAsync(tRunnable1);
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:solveAsync', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:solveAsync', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end

	function result = cd(self,varargin)
		try
			if size(varargin) > 0
				result = self.impl.cd(varargin);
			else
				result = self.impl.cd();
			end
		catch e
			if(isa(e,'matlab.exception.JavaException'))
				ex = e.ExceptionObject;
				if(isa(ex, 'com.ampl.AMPLException'))
					err = MException('AMPLAPI:AMPL:cd', char(ex.toString().replace('\', '/')));
				else
					err = MException('AMPLAPI:AMPL:cd', char(ex.getMessage().replace('\', '/')));
				end
				throw (err)
			else
				throw (e)
			end
		end
	end
end
end
