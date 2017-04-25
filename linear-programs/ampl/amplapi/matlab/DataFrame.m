classdef DataFrame < DataFrameBase
	% A DataFrame object stores data in a tabular format, convenient to
	% interact with AMPL
methods(Access=private)
    function boolean  = isIndex(self, header)
        c=cell(self.impl.getHeaders);
        for i=1:length(c)
            if (strcmp(c{i}, header))
                index = i;
                break
            end
        end
        boolean = (index <= self.impl.getNumIndices);
    end
end
    
methods
	function obj = DataFrame(varargin)
        % Constructor; constructs a DataFrame Object
        % Two possibilities:
        % DataFrame(nindices, index1, ..., indexn) where index1..indexn are
        % the names of the n indexing  columns
        % or
        % DataFrame(entity1, ..., entityn) where entity1...entityn are AMPL
        % entities        
        if(nargin > 1)
            if(varargin{1} == -1)
                s=varargin{2};
            else
                if(isa(varargin{1}, 'Entity'))
                    fff  = toJava('com.ampl.Entity', varargin{1:end});   
                    s = com.ampl.DataFrame(fff);
                else
                    fff  = toJava('java.lang.String', varargin{2:end});
                    s = com.ampl.DataFrame(varargin{1}, fff);
                end
            end
        else
            s = com.ampl.DataFrame(varargin{:});
        end
        
        obj@DataFrameBase(s)
    end
    
    function display(self)
        % DISPLAY Show a textual representation of this entity
        % The specifis representation depends on the kind of entity.
        self.impl.toString()
    end
    
    function addColumn(self, header, values)
        % ADDCOLUMN add a column to this dataframe with the specified
        % header and values
        switch nargin
            case 2
                self.impl.addColumn(header);
            case 3
                if(self.impl.getNumCols < self.impl.getNumIndices)
                     v = com.ampl.MatlabUtil.matlabArrayToIndices(values);
                else
                     if(iscell(values))
                        v = com.ampl.MatlabUtil.matlabArrayToApiArray(values);
                     else
                        v = values;
                     end
                end
                self.impl.addColumn(header, v);
            otherwise error('error');
        end      
    end
    
     function setColumn(self, header, values)
         % SETCOLUMN set the values for a column 
        if(self.isIndex(header))
             v = com.ampl.MatlabUtil.matlabArrayToIndices(values);
         else
             if(iscell(values))
                v = com.ampl.MatlabUtil.matlabArrayToApiArray(values);
             else
                 v = values;
             end
         end
         
        self.impl.setColumn(header, v);   
     end
    
     function setMatrix(self, values, varargin)
         % SETMATRIX set the values of a matrix to this DataFrame.
         % The semantic is:
         % setMatrix(values, index1, ..., indexn), where values is an n-dimensional matrix and index1...indexn
         % are the name of the indexing column corresponding to each
         % dimension of the matrix
        self.impl.setMatrix(values, varargin);
     end
    
  
     function sizes = size(self, varargin)
        r = self.impl.getNumRows();
        c = self.impl.getNumCols();
        sizes(1) = r;
        sizes(2) = c;
     end
     
     
     function row = getRowByIndex(self, index)
        row = cell(self.impl.getRowByIndex(index -1)); 
     end
     
     function result = getColumnAsStrings(self,header)
         result = cell(getColumnAsStrings@DataFrameBase(self, header));
     end
     function result = getColumn(self,header)
         result = cell(getColumn@DataFrameBase(self, header));
     end
     function result = getRow(self,varargin)
            result = cell(getRow@DataFrameBase(self, varargin{1:end}));
     end
end
end
